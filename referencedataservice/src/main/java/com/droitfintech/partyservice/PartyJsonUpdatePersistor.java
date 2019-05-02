package com.droitfintech.partyservice;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by barry on 8/25/16.
 * NOTE: This class servers no purpose right now will remove shortly
 */
public class PartyJsonUpdatePersistor implements PartyPersister {
    PartyJsonPersister persistor;
    TreeMap<String,Object> updates = new TreeMap<>();
    ArrayList<String> deletes = new ArrayList<>();

    public PartyJsonUpdatePersistor(PartyJsonPersister persistor) {
        this.persistor = persistor;
    }

    @Override
    public void persist(Map<String, Object> party) {

        persistor.persist(party);


    }

    @Override
    public void delete(String partyId) {
        persistor.delete(partyId);

    }

    public Map<String,Object> getUpdates() {
        return updates;
    }

    public ArrayList<String> getDeletes() {
        return deletes;
    }
}
