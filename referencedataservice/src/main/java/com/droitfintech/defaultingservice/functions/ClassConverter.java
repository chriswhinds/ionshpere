package com.droitfintech.defaultingservice.functions;


import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.util.*;

public abstract class ClassConverter<T> {
	
	private static Logger logger = LoggerFactory.getLogger(ClassConverter.class);
	
	public static String NULL_SPECIAL_VALUE = "!NULL";
	
	private Class<T> clazz;
	
	protected ClassConverter(Class<T> clazz) {
		this.clazz = clazz;
	}
	
	public T convert(String val) {
		if (val == null || val.trim().isEmpty() || val.trim().equals(NULL_SPECIAL_VALUE)) {
			return null;
		}
		return convertInner(val);
	}
	
	protected abstract T convertInner(String val);
	
	public String toString(Object o){
		return o.toString();
	}
	
	public static String toStringStatic(Object o) {
		return getConverter(o.getClass()).toString(o);
	}
	
	@SuppressWarnings("unchecked")
	public static <E> ClassConverter<E> getConverter(Class<E> inClazz) {
		if (!CONVERTERS.containsKey(inClazz)) {

		    logger.error("Converter not found for class [ {} ]",inClazz);

		}
		return (ClassConverter<E>) CONVERTERS.get(inClazz);
	}
	
	private static Map<Class<?>, ClassConverter<?>> CONVERTERS = 
									new HashMap<Class<?>, ClassConverter<?>>(){{
		put(String.class, new StringClassConverter());
		put(Boolean.class, new BooleanClassConverter());
		put(LocalDate.class, new LocalDateClassConverter());
		put(Date.class, new DateClassConverter());
		put(XMLGregorianCalendar.class, new XMLGregorianCalendarClassConverter());
		put(Integer.class, new IntegerClassConverter());
		put(BigDecimal.class, new BigDecimalClassConverter());
		put(java.sql.Date.class, get(Date.class));
		put(Tenor.class, new TenorClassConverter());
		
	}};

	private static class StringClassConverter extends ClassConverter<String> {
		public StringClassConverter() {
			super(String.class);
		}

		@Override
        public String convertInner(String val) {
            return val;
        }
	}

	private static class BooleanClassConverter extends ClassConverter<Boolean> {
		public BooleanClassConverter() {
			super(Boolean.class);
		}

		@Override
        public Boolean convertInner(String val) {
            return Boolean.parseBoolean(val);
        }
	}

	private static class LocalDateClassConverter extends ClassConverter<LocalDate> {
		private DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");

		public LocalDateClassConverter() {
			super(LocalDate.class);
		}

		@Override
        public LocalDate convertInner(String o) {
            return fmt.parseLocalDate(o);
        }

		@Override
        public String toString(Object o){
            LocalDate d = (LocalDate) o;
            return fmt.print(d);
        }

		;
	}

	private static class DateClassConverter extends ClassConverter<Date> {

		List<DateTimeFormatter> noLongerSupportedFormats = new LinkedList<DateTimeFormatter>() {{
            add(DateTimeFormat.forPattern("MM/dd/yy"));
            add(DateTimeFormat.forPattern("MM/dd/yyyy"));
            add(DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm:ss"));
            add(DateTimeFormat.forPattern("dd-MMM-yyyy"));
            add(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mmZZ"));
        }};

		public DateClassConverter() {
			super(Date.class);
		}

		@Override
        public Date convertInner(String val) {
            val = val.trim();

            DateTime date = null;
            // First check for miliseconds since jan 1 1970 UTC
            if(StringUtils.isNumeric(val)) {
                date = new DateTime(Long.parseLong(val));
            } else if(val.length() == 10) {
                // ISO Complete date: YYYY-MM-DD (eg 1997-07-16)
                try {
                    date = ISODateTimeFormat.date().parseDateTime(val);
                } catch (IllegalArgumentException ignore) {
                }
            } else if(val.length() > 11 && val.charAt(10) == 'T') {
                // ISO Complete date plus hours, minutes, seconds and a decimal fraction of a second
                // YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
                try {
                    date = ISODateTimeFormat.dateTime().parseDateTime(val);
                } catch (IllegalArgumentException ignore) {
                }
                // ISO Complete date plus hours, minutes and seconds:
                // YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
                if(date == null) {
                    try {
                        date = ISODateTimeFormat.dateTimeNoMillis().parseDateTime(val);
                    } catch (IllegalArgumentException ignore) {
                    }
                }
            }

            if (date == null) {
                // fallback formats.  At some point these need to be removed as they're
                for (DateTimeFormatter fmt: noLongerSupportedFormats) {
                    try {
                        date = fmt.parseDateTime(val);
                        // XXX This needs to be at the warn level but we have so many dates in our system that break
                        // this rule that putting it to trace for now.  *Needs to be cleaned up!*
                        if (logger.isTraceEnabled()) {
                            logger.trace("Date {} is not in ISO format.  As some point this will be disabled; " +
                                    "change your date format to ISO standards.  Was parsed to {}.  Stacktrace for this call:", val, date.toString());
                        }
                        //ExceptionUtils.getStackTrace(new DroitException("Stacktrace output"));
                        break;
                    } catch (IllegalArgumentException e) {
                        ; // do nothing.
                    }
                }
            }

            return date.toDate();
        }

		@Override
        public String toString(Object o){
            Date d = (Date) o;
            return ISODateTimeFormat.dateTime().print(new LocalDate(d));
        }

		;
	}

	private static class XMLGregorianCalendarClassConverter extends ClassConverter<XMLGregorianCalendar> {
		DateTimeFormatter canonical = DateTimeFormat.forPattern("yyyy-MM-dd");
		List<DateTimeFormatter> formats = new LinkedList<DateTimeFormatter>() {{
            add(DateTimeFormat.forPattern("MM/dd/yy"));
            add(DateTimeFormat.forPattern("MM/dd/yyyy"));
            add(canonical);
        }};

		public XMLGregorianCalendarClassConverter() {
			super(XMLGregorianCalendar.class);
		}

		@Override
        public XMLGregorianCalendar convertInner(String val) {

            LocalDate date = null;
            for (DateTimeFormatter fmt: formats) {
                try {
                    date = fmt.parseLocalDate(val.trim());
                    break;
                } catch (IllegalArgumentException e) {
                    ; // do nothing.
                }
            }
            //DroitException.assertThat(date != null, "This date could not be parsed properly: " + val);
            try {
                return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
                        date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), DatatypeConstants.FIELD_UNDEFINED);

            } catch (DatatypeConfigurationException e) {

                logger.error("Exception Thrown:",e);

            }

            return null;
        }

	}

	private static class IntegerClassConverter extends ClassConverter<Integer> {
		public IntegerClassConverter() {
			super(Integer.class);
		}

		@Override
        public Integer convertInner(String o) {
            return Integer.parseInt(o);
        }
	}

	private static class BigDecimalClassConverter extends ClassConverter<BigDecimal> {
		public BigDecimalClassConverter() {
			super(BigDecimal.class);
		}

		@Override
        public BigDecimal convertInner(String o) {
            return new BigDecimal(o);
        }
	}

	/*
	private static class TradingWeekHoursSetClassConverter extends ClassConverter<TradingWeekHoursSet> {
		public TradingWeekHoursSetClassConverter() {
			super(TradingWeekHoursSet.class);
		}

		@Override
        public TradingWeekHoursSet convertInner(String o) {
            TradingWeekHoursSet res = new TradingWeekHoursSet();
            for(String weekHour: Splitter.on(";").split(o)) {
                List<String> typeTokens = Splitter.on(":::").splitToList(weekHour);
                TradingWeekHours h = new TradingWeekHours(typeTokens.get(1));
                if ("singleBc".equalsIgnoreCase(typeTokens.get(0))) {
                    res.setSingleBcTradingHours(h);
                } else {
                    res.setMultipleBcTradingHours(h);
                }
            }
            return res;
        }
	}

	private static class ProductMasterClassConverter extends ClassConverter<ProductMaster> {
		public ProductMasterClassConverter() {
			super(ProductMaster.class);
		}

		@Override
        public ProductMaster convertInner(String item) {
            String[] itemParts = item.split(":");
            String assetClass = itemParts.length > 0 ? itemParts[0] : null;
            String baseProduct = itemParts.length > 1 ? itemParts[1] : null;
            String subProduct = itemParts.length > 2 ? itemParts[2] : null;
            return new ProductMaster(assetClass, baseProduct, subProduct);
        }
	}
    */

	private static class TenorClassConverter extends ClassConverter<Tenor> {
		public TenorClassConverter() {
			super(Tenor.class);
		}

		@Override
        public Tenor convertInner(String item) {
            return Tenor.makeTenor(item);
        }

		@Override
        public String toString(Object o) {
            return ((Tenor)o).toDbValue();
        }
	}
}
