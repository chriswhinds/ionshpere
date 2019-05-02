package com.droitfintech.partyservice;

import java.util.Map;

/**
 * Created by barry on 8/25/16.
 */
public interface PartyPersister {

    void persist(Map<String, Object> party);
    void delete(String partyId);
}
