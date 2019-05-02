package com.droitfintech.partyservice;



import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by barry on 9/20/16.
 */
public class MongoDbPartyReader implements PartyReader {

    private static Logger log = LoggerFactory.getLogger(MongoDbPartyReader.class);

    private String hostName = "192.168.2.20";
    private String databaseName = "jis";
    private String collectionName = "parties";
    private int port = 27017;
    public static String MOMGODB_HOSTNAME="mongoDBHostName";
    public static String MOMGODB_PORT = "mongoDBPort";
    public static String MOMGODB_DATABASE_NAME = "mongoDBDatabaseName";
    public static String MOMGODB_COLLECTION_NAME = "mongoDBCollectionName";

    private String partyUpdateFileLocation;

    /***
     * Constructor
     * @param serviceProperties
     */
    public MongoDbPartyReader(LinkedHashMap serviceProperties){
        hostName = (String)serviceProperties.get(MOMGODB_HOSTNAME);
        port     = (int)serviceProperties.get(MOMGODB_PORT);
        databaseName = (String)serviceProperties.get(MOMGODB_DATABASE_NAME);
        databaseName = (String)serviceProperties.get(MOMGODB_DATABASE_NAME);
        collectionName = (String)serviceProperties.get(MOMGODB_COLLECTION_NAME);

        String serviceHomeLocation =   System.getProperty(PartyReferenceDataService.SERVICE_HOME);
        partyUpdateFileLocation =  serviceHomeLocation +  (String)serviceProperties.get(PartyReferenceDataService.PARTY_UPDATES_LOCATION);
        
    }

    @Override
    public int parseAndPersistParties(PartyPersister persister) {
        int count = 0;
        try {

            MongoClient mongoClient = new MongoClient(hostName,port);
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            MongoCursor<Document> cursor = collection.find().iterator();

            try {
                while (cursor.hasNext()) {
                    Document partyDoc = cursor.next();
                    partyDoc.remove("_id"); // remove id added my mongoDB
                    persister.persist(partyDoc);
                    ++count;
                }
                log.debug("Loaded {} parties from db {} collection {}", count, databaseName, collectionName);
            } finally {
                cursor.close();
            }

            mongoClient.close();

        } catch (Exception ex) {
            log.error("Error connecting to mongoDB", ex);
        }
        return count;
    }

    @Override
    public int parseAndPersistParties(String fileName, PartyPersister persister) {

        return 0;
    }

    @Override
    public int parseAndDeleteParties(String fileName, PartyPersister persister) {

        return 0;
    }

    @Override
    public Path getUpdateDirectory() {

        File file = new File(partyUpdateFileLocation);
        if(file.exists() && file.isDirectory()) {
            return file.toPath();
        }

        return null;
    }

    public void persistParties(Collection<Map<String, Object>> parties, String collectionName) {
        try {
            MongoClient mongoClient = new MongoClient(hostName,port);;
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            for (Map<String, Object> p : parties) {
                Document partyDoc = new Document(p);
                collection.insertOne(partyDoc);
            }
        } catch (Exception ex) {
            log.error("Error connecting to mongoDB", ex);
        }
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
}
