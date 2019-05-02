package com.droitfintech.partyservice;

import java.nio.file.Path;

/**
 * Created by barry on 8/22/16.
 */
public interface PartyReader {

    int parseAndPersistParties(PartyPersister persister); // startup
    int parseAndPersistParties(String fileName, PartyPersister persister); // updates
    int parseAndDeleteParties(String fileName, PartyPersister persister); // deletes
    Path getUpdateDirectory();

}
