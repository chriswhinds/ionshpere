package com.droitfintech.partyservice;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.*;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by christopherwhinds on 3/2/17.
 */
public class PartyUpdatesEventFileWatcher {
    private static Logger logger = LoggerFactory.getLogger(PartyUpdatesEventFileWatcher.class);

    private PartyReader reader ;
    private WatchService fileWatcher;
    private ObjectMapper mapper = new ObjectMapper();
    private PartyJsonPersister persister;


    /**
     * Constructor
     * @param reader
     * @param persister
     */
    public PartyUpdatesEventFileWatcher(PartyReader reader,
                                        PartyJsonPersister persister){
        this.reader = reader;
        this.persister = persister;

    }

    public void monitorForPartyUpdateEvents() throws Exception{

        // if there is an update directory start scanning it for file drops.
        // only process files ending in .json
        if(reader.getUpdateDirectory() != null) {

            Path dirPath = reader.getUpdateDirectory();
            logger.info("IOCTRL starting file watcher on {}.", dirPath.toAbsolutePath().toString());
            fileWatcher = FileSystems.getDefault().newWatchService();
            dirPath.register(fileWatcher, StandardWatchEventKinds.ENTRY_CREATE);
            while (true) {
                try {
                    WatchKey key = fileWatcher.poll(1, TimeUnit.SECONDS);
                    if(key != null) {
                        logger.debug("Got file event " + key.toString());
                        for (WatchEvent<?> event: key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }
                            WatchEvent<Path> ev = (WatchEvent<Path>)event;
                            Path filename = ev.context();
                            if(filename.toString().endsWith(".json")) {
                                if(filename.toString().contains("delete")) {
                                    logger.info("Got party delete " + filename.toString());
                                    String fileFullPath = dirPath.toString() + File.separator + filename.toString();
                                    PartyJsonUpdatePersistor up = new PartyJsonUpdatePersistor(persister);
                                    reader.parseAndDeleteParties(fileFullPath, up);
                                    Collection<String> updates = up.getDeletes();
                                    logger.info("File deleted {} parties", (updates != null ? updates.size() : 0) );

                                }else {
                                    logger.info("Got party update " + filename.toString());
                                    String fileFullPath = dirPath.toString() + File.separator + filename.toString();
                                    PartyJsonUpdatePersistor up = new PartyJsonUpdatePersistor(persister);
                                    reader.parseAndPersistParties(fileFullPath, up);
                                    Map<String, Object> updates = up.getUpdates();
                                    logger.info("File updated {} parties", (updates != null ? updates.size() : 0) );


                                }
                            }
                        }
                        boolean valid = key.reset();
                        if (!valid) {
                            logger.warn("Could not reset file watch key exiting file scanning.");
                            break;
                        }
                    }

                } catch (Exception ex ) {
                    logger.warn("Exception from file watcher: ", ex);
                    break;
                }
            }
        }



    }



}
