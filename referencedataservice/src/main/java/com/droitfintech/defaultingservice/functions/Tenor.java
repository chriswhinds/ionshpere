package com.droitfintech.defaultingservice.functions;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tenor.java
 * COPYRIGHT (C) 2013 Droit Financial Technologies, LLC
 *
 * @author jisoo
 *
 */
public class Tenor implements Comparable<Tenor>, Serializable {
	
	private static final long serialVersionUID = 1L;
	// This is the rudimentary equivalence table we are using to convert each tenor period to days.
	// TODO: there problems with this approach, not the least of which is that
	// tenors like 1T mean nothing without the trade context.
	// Updated values to take into account leap years and 30/31 days in months
	private static Map<String, Double> periodInDaysMap = new HashMap<String, Double>() {{
		put("D", 1.00);
		put("W", 7.00);
		put("M", 30.4375);
		put("Y", 365.25);
		put("T", -1.00);
	}};

	private static Double convertTenorToDouble(int multiplier, String period) {
		Double periodInDays = periodInDaysMap.get(period);
		return periodInDays * multiplier;
	}

	private static int convertTenorToDays(int multiplier, String period) {
		Double periodInDays = periodInDaysMap.get(period);
		Double days = periodInDays * multiplier;
		return (int) Math.round(days);
	}
	
	protected String period;
	protected Integer multiplier;
	
	public Tenor() {
		period = "D";
		multiplier = 0;
	}
	
	public Tenor(int multiplier, String period) {
		setMultiplier(multiplier);
		setPeriod(period);
	}
	
	@Override
    public String toString()
    {
		return toDbValue();
    }
	
	public String toDbValue() {
		return this.multiplier + this.period;
	}
	
	@Override
    public Object clone()
    {
		Tenor _tenor = new Tenor();
		_tenor.setMultiplier(this.multiplier);
		_tenor.setPeriod(this.period);
		return _tenor;
    }
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(getDouble()).toHashCode();
	}

	/**
	 * We will consider two tenors equal if they amount to the equivalent number of days
	 * within
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		Tenor rhs = (Tenor) obj;
		return (Math.abs(getDouble() - rhs.getDouble()) < 1);
	}
	
	public String getPeriod() {
		return period;
	}
	public void setPeriod(String period) {
		this.period = period;
	}
	public Integer getMultiplier() {
		return multiplier;
	}
	public void setMultiplier(Integer multiplier) {
		this.multiplier = multiplier;
	}
	
	@JsonIgnore
	public Integer getDays() {
		return convertTenorToDays(multiplier, period);
	}

	@JsonIgnore
	public Double getDouble() {
		return convertTenorToDouble(multiplier, period);
	}
	
	public static Tenor makeTenor(String val) {
		if (val == null || val.trim().isEmpty()) {
			return null;
		}
		Pattern pattern = Pattern.compile("(-?[0-9]{1,7})([DWMYT])");
		Matcher matcher = pattern.matcher(val.toUpperCase());
		if (matcher.find()) {
			int multiplier = Integer.parseInt(matcher.group(1));
			String period = matcher.group(2);
			return new Tenor(multiplier, period);
		}
		//throw new Exception("Invalid tenor literal: "+val);
		return null;
	}

	@Override
	public int compareTo(Tenor o) {
		if (this.equals(o)) {
			return 0;
		} else if (this.getDouble() > o.getDouble()) {
			return 1;
		} else { 
			return -1;
		}
	}

	
}
