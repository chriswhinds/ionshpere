package com.droitfintech.defaultingservice;

import com.droitfintech.defaultingservice.functions.BadArgumentException;

import java.util.Map;

/**
 * Created by christopherwhinds on 10/10/17.
 */
public interface RequestAttributeResolver {

    Map buildResolvedTradeRequest(Map request)  throws BadArgumentException;

}
