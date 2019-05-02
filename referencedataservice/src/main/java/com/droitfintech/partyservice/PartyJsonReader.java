package com.droitfintech.partyservice;


import com.droitfintech.auditservice.AuditReferenceDataService;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by chris hinds 02/03/2017.
 */
public class PartyJsonReader implements PartyReader {
    private static Logger log = LoggerFactory.getLogger(PartyJsonReader.class);
    private enum ObjectType { MAP, ARRAY }

    private String partyStartUpFileLocation;
    private String partyUpdateFileLocation;


    /**
     * Constructor
     * @param configuration
     */
    public PartyJsonReader(LinkedHashMap configuration){

        String serviceHomeLocation =   System.getProperty(PartyReferenceDataService.SERVICE_HOME);

        partyStartUpFileLocation = serviceHomeLocation + (String)configuration.get(PartyReferenceDataService.PARTY_STARTUP_LOCATION);
        partyUpdateFileLocation = serviceHomeLocation  + (String)configuration.get(PartyReferenceDataService.PARTY_UPDATES_LOCATION);




    }


    // Embeded JSON object either an array or a map.
    private static class JsonObject {
        String elementName;
        Map<String,Object> mapElement = null;
        ArrayList<Object> arrayElement = null;

        JsonObject(String name, ObjectType type) {
            if(type == ObjectType.ARRAY) {
                arrayElement = new ArrayList<>();
            } else {
                mapElement = new TreeMap<>();
            }
            if(StringUtils.isNotBlank(name)) {
                elementName = name;
            }
        }

        public void addString(String name, String value) {
            if(arrayElement != null) {
                arrayElement.add(value);
            }
            else if(mapElement != null) {
                mapElement.put(name, value);
            }
        }

        public void addBoolean(String name, Boolean value) {
            if(arrayElement != null) {
                arrayElement.add(value);
            }
            else if(mapElement != null) {
                mapElement.put(name, value);
            }
        }

        public void addNull(String name) {
            if(mapElement != null) {
                mapElement.put(name, null);
            }
        }

        public void addObject(JsonObject value) {
            if(arrayElement != null) {
                if(value.arrayElement != null)
                    arrayElement.add(value.arrayElement);
                else if(value.mapElement != null) {
                    arrayElement.add(value.mapElement);
                }
            }
            else if(mapElement != null) {
                if(value.arrayElement != null)
                    mapElement.put(value.elementName, value.arrayElement);
                else if(value.mapElement != null) {
                    mapElement.put(value.elementName, value.mapElement);
                }
            }
        }

        public Map<String,Object> getMap() {
            return mapElement;
        }
    }

    /**
     * Look up the list of initial party json files, read and persist them.
     * @param persister
     * @return
     */
    @Override
    public int parseAndPersistParties(PartyPersister persister) {
        int count = 0;
        for (String file : getStartupFileList()) {
            count += parseAndPersistParties(file, persister);
        }
        return count;
    }

    public int parseAndDeleteParties(String fileName, PartyPersister persister) {
        Integer numParties = 0;

        try {
            ObjectMapper mapper = new ObjectMapper();
            FileInputStream fs = new FileInputStream(fileName);
            HashMap<String, Object> obj = mapper.readValue(fs, HashMap.class);
            if(obj.containsKey("ids")) {
                Object idArrayObject  = obj.get("ids");
                if(idArrayObject != null && idArrayObject instanceof Collection) {
                    for(String id : (Collection<String>)idArrayObject) {
                        persister.delete(id);
                        ++numParties;
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error reading delete from file " + fileName, ex);
        }
        return numParties;
    }

    /**
     * Read an persist single party json file.
     * @param fileName      Full path name of file to read.
     * @param persister
     * @return
     */
    public int parseAndPersistParties(String fileName, PartyPersister persister) {
        Integer numParties = 0;
        try {

            JsonFactory jfactory = new JsonFactory();
            log.info("Reading and persisting parties from json file " + fileName);
            /*** read from file ***/
            JsonParser jParser = jfactory.createParser(new File(fileName));
            Stack<JsonObject> objectStack = new Stack<>();
            int objectDepth = 0;
            JsonObject curObject = null;
            JsonToken token;
            while ((token = jParser.nextToken()) != null) {
                String fieldname = jParser.getText();
                if(fieldname != null) {
                    switch(token) {
                        case START_OBJECT:
                            ++objectDepth;
                            if(objectDepth > 2) {
                                curObject = new JsonObject(jParser.getCurrentName(), ObjectType.MAP);
                                objectStack.push(curObject);
                            }
                            break;
                        case END_OBJECT:
                            --objectDepth;
                            if (objectDepth == 2) {
                                curObject = objectStack.pop();
                                if((numParties % 10000) == 0) {
                                    log.debug("end of party {} ", numParties);
                                }
                                persister.persist(curObject.getMap());
                                // free memory for current counterparty.
                                curObject = null;
                                ++numParties;
                            } else if(objectDepth > 2) {
                                JsonObject lastObject = objectStack.pop();
                                curObject = objectStack.peek();
                                curObject.addObject(lastObject);
                            }
                            break;
                        case START_ARRAY:
                            ++objectDepth;
                            if(objectDepth > 2) {
                                curObject = new JsonObject(jParser.getCurrentName(), ObjectType.ARRAY);
                                objectStack.push(curObject);
                            }
                            break;
                        case END_ARRAY:
                            --objectDepth;
                            if(objectDepth > 2) {
                                JsonObject lastObject = objectStack.pop();
                                curObject = objectStack.peek();
                                curObject.addObject(lastObject);
                            }
                            break;
                        case VALUE_STRING:
                            if(objectDepth >2 && curObject != null) {
                                curObject.addString(jParser.getCurrentName(), fieldname);
                            }
                            break;
                        case VALUE_TRUE:
                            if(objectDepth >2 && curObject != null) {
                                curObject.addBoolean(jParser.getCurrentName(), Boolean.TRUE);
                            }
                            break;
                        case VALUE_FALSE:
                            if(objectDepth >2 && curObject != null) {
                                curObject.addBoolean(jParser.getCurrentName(), Boolean.FALSE);
                            }
                            break;
                        case VALUE_NULL:
                            if(objectDepth >2 && curObject != null) {
                                curObject.addNull(jParser.getCurrentName());
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
            log.info("read {} parties from {}", numParties, fileName);
        } catch (Exception ex) {
            log.error("Error reading json party file :" + fileName, ex);
        }
        return numParties;
    }

    /**
     * Get the list of file to read at startup
     * @return
     */
    private Collection<String> getStartupFileList() {
        ArrayList<String> files = new ArrayList<>();

        File startupDir = new File(partyStartUpFileLocation);
        if(startupDir.exists() && startupDir.isDirectory()) {
            for (File file : startupDir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    files.add(file.getAbsoluteFile().getAbsolutePath());
                }
            }
        }
        return files;
    }

    public Path getUpdateDirectory() {

        File file = new File(partyUpdateFileLocation);
        if(file.exists() && file.isDirectory()) {
            return file.toPath();
        }
        return null;
    }
}
