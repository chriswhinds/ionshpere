package com.droitfintech.defaultingservice.functions;

/**
 * Created by christopherwhinds on 9/11/17.
 */
public interface AttributeAppyFunction {
    Object apply(Object... values) throws BadArgumentException;
}
