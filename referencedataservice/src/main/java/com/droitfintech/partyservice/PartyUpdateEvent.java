package com.droitfintech.partyservice;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by barry on 8/25/16.
 */
public class PartyUpdateEvent {
    public String service = "partyStaticDataService";
    public String method;
    public String msgType = "command";
    public String msgId;
    public Map<String,Object> arguments = new HashMap<>();

    public PartyUpdateEvent(String method, Collection<String> ids) {
        msgId = UUID.randomUUID().toString();
        this.method = method;
        arguments.put("ids", ids);
    }

}
