package com.droitfintech.defaultingservice.functions;



import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

/**
 * Created by christopherwhinds on 9/11/17.
 */
public class DateFunction implements AttributeAppyFunction {

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
        return dateFormatter.print(new DateTime(ClassConverter.getConverter(Date.class).convert(dateParm)));
    }
}
