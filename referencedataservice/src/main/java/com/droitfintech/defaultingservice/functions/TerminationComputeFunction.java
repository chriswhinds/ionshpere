package com.droitfintech.defaultingservice.functions;


import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by christopherwhinds on 9/11/17.
 */
public class TerminationComputeFunction implements AttributeAppyFunction {

    DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Override
    /***
     * This function will require four values
     * 1st parm trasform int a valid date function
     */
    public Object apply(Object... values) throws BadArgumentException {
       if ( values.length == 0 )
            throw new BadArgumentException("No Arguments passed on this call");
        String dateParm = (String)values[0];

        MultivaluedMap<String, String> request = (MultivaluedMap)values[1];
        String maturity = request.getFirst("maturity");
        return dateFormatter.print(new DateTime(	getTerminationDate(ClassConverter.getConverter(Date.class).convert(dateParm),Tenor.makeTenor(maturity))));
    }

    /****
     * Return a Date for adjusted by tenor
     * @param effectiveDate
     * @param maturity
     * @return
     */
    private Date getTerminationDate(Date effectiveDate, Tenor maturity) {

        Calendar c = Calendar.getInstance();
        c.setTime(effectiveDate);
        c.add(Calendar.DATE, maturity.getDays());
        return c.getTime();
    }

}
