package com.droitfintech.partyservice;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PartyJsonPersister implements PartyPersister {
    private static Logger logger = LoggerFactory.getLogger(PartyJsonPersister.class);

    Connection dbConnection;
    String dbFilePath;
    private String createTable = ""; // built in constructor
    private String insertParty = ""; // built in constructor
    private String updateParty = ""; // built in constructor
    private String queryObject = "SELECT objMap from PARTY WHERE accountID = ?";
    private String queryPartiesyNameObject = "SELECT objMap from PARTY WHERE accountName like ? AND  contraparty = ? AND frequentlyUsedCounterparty = ? LIMIT 100  ";

    private String queryPartyCount = "SELECT count(*) from PARTY ";

    private String deleteParty = "DELETE from PARTY WHERE accountID = ?";
    private PreparedStatement insertPreparedStatement;
    private PreparedStatement updatePreparedStatement;
    private PreparedStatement queryObjectPreparedStatement;
    private PreparedStatement queryPartyCountPreparedStatement;
    private PreparedStatement deleteObjectPreparedStatement;



    private int inserts = 0;
    private int updates = 0;
    private int maxCached = 50000;
    private ArrayList<ExtractedField> fieldToExtract = new ArrayList<>();
    private boolean cacheAllParties = false;

    private String partyCacheFileName = "partyCache";
    private int searchResultsLimit = 100;

    /**
     * Constutor
     * @param dbFilePath
     */
    public PartyJsonPersister(String dbFilePath,int queryResultlimit) {
        this.dbFilePath = dbFilePath ;
        searchResultsLimit = queryResultlimit;
        fieldToExtract.add(new ExtractedField("accountID", ExtractedField.FieldType.STRING));
        fieldToExtract.add(new ExtractedField("accountName", ExtractedField.FieldType.STRING));

        fieldToExtract.add(new ExtractedField("creditline", ExtractedField.FieldType.BOOLEAN));
        fieldToExtract.add(new ExtractedField("frequentlyUsedContraparty", ExtractedField.FieldType.BOOLEAN));
        fieldToExtract.add(new ExtractedField("frequentlyUsedCounterparty", ExtractedField.FieldType.BOOLEAN));


        fieldToExtract.add(new ExtractedField("customCptyIdentifiers", ExtractedField.FieldType.OBJECT));

        fieldToExtract.add(new ExtractedField("parentid", ExtractedField.FieldType.STRING));

        fieldToExtract.add(new ExtractedField("cftcFinalUSPerson", ExtractedField.FieldType.BOOLEAN));      // USP column in dropdown
        fieldToExtract.add(new ExtractedField("internalPartyAffiliate", ExtractedField.FieldType.BOOLEAN)); // Affiliate column in dropdown
        fieldToExtract.add(new ExtractedField("foreignBranchUSPerson", ExtractedField.FieldType.BOOLEAN)); // Branch col in dropdown

        // so non cached contraparties get added to the adept search cache.
        fieldToExtract.add(new ExtractedField("contraparty", ExtractedField.FieldType.BOOLEAN));

        createTable = "CREATE TABLE PARTY(";
        insertParty = "INSERT INTO PARTY (";
        updateParty = "UPDATE PARTY set ";
        boolean firstField = true;
        int fieldCount = 0;
        for(ExtractedField field : fieldToExtract) {
            ++fieldCount;
            if(field.getFieldType() == ExtractedField.FieldType.BOOLEAN) {
                createTable += field.getFieldName() + " BOOLEAN";
            } else if(field.getFieldType() == ExtractedField.FieldType.STRING) {
                createTable += field.getFieldName() + " varchar(255)";
            } else {
                --fieldCount;
                continue;
            }
            insertParty += field.getFieldName();
            if(firstField) {
                createTable += " primary key";
            } else {
                updateParty += field.getFieldName() + " = ?, ";
            }
            createTable += ", ";
            insertParty += ", ";
            firstField = false;
        }
        createTable += " objMap OTHER)";
        insertParty += " objMap) VALUES (";
        updateParty += " objMap = ? WHERE accountID = ?";
        while(fieldCount > 0) {
            insertParty += "?,";
            --fieldCount;
        }
        insertParty += "?)";
        fieldCount = 0;
    }

    /***
     * Delete a Party
     * @param partyId
     */
    public void delete(String partyId) {

        getDbConnection();
        try {
            deleteObjectPreparedStatement.setString(1, partyId);
            deleteObjectPreparedStatement.executeUpdate();
        } catch (SQLException ex) {
            logger.warn("Error deleting " + partyId + " from db", ex);
        }

    }

    /**
     * Presist a party on add or update to the H2 Cache
     *
     * @param party
     */
    public void persist(Map<String,Object> party) {

        getDbConnection();
        String partyId = (String)party.get("accountID");

        Map<String,Object> foundParty = this.getParty(partyId);
        if(foundParty != null){
            //Update
            try {
                int paramIndex = 0;
                for(ExtractedField field : fieldToExtract) {
                    if(paramIndex == 0) {
                        ++paramIndex;
                        continue;
                    }
                    if(field.getFieldType() == ExtractedField.FieldType.BOOLEAN) {
                        Boolean fValue = (Boolean)party.get(field.getFieldName());
                        if(fValue == null) {
                            fValue = Boolean.FALSE;
                        }
                        updatePreparedStatement.setBoolean(paramIndex, fValue);
                    } else if(field.getFieldType() == ExtractedField.FieldType.STRING) {
                        String fValue = (String)party.get(field.getFieldName());
                        if(fValue == null) {
                            fValue = "";
                        }
                        updatePreparedStatement.setString(paramIndex, fValue);
                    } else {
                        continue; // skip unknown types.
                    }
                    ++paramIndex;
                }
                updatePreparedStatement.setObject(paramIndex, party);
                ++paramIndex;
                updatePreparedStatement.setString(paramIndex, partyId);
                updatePreparedStatement.executeUpdate();
                logger.debug("Updated Party for id [{}] to H2 Cache",partyId);
                ++updates;
            } catch (SQLException ex) {
                logger.error("Exception updateing party [" + partyId +" ]", ex);
            }

        } else {
            //Add
            try {
                int paramIndex = 1;
                for(ExtractedField field : fieldToExtract) {
                    if(field.getFieldType() == ExtractedField.FieldType.BOOLEAN) {
                        Boolean fValue = (Boolean)party.get(field.getFieldName());
                        if(fValue == null) {
                            fValue = Boolean.FALSE;
                        }
                        insertPreparedStatement.setBoolean(paramIndex, fValue);
                    } else if(field.getFieldType() == ExtractedField.FieldType.STRING) {
                        String fValue = (String)party.get(field.getFieldName());
                        if(fValue == null) {
                            fValue = "";
                        }
                        insertPreparedStatement.setString(paramIndex, fValue);
                    } else {
                        continue; // skip field os unknown type.
                    }
                    ++paramIndex;
                }
                insertPreparedStatement.setObject(paramIndex, party);
                insertPreparedStatement.executeUpdate();
                ++inserts;
                logger.debug("Added Party for id [{}] to H2 Cache",partyId);

            } catch (SQLException ex) {
                logger.error("Exception Adding party [" + partyId +" ]", ex);

            }

        }


    }

    /***
     *
     * @param fullParty
     * @return
     */
    private Map<String,Object> buildPartialParty(Map<String,Object> fullParty) {
        Map partial = new HashMap();
        for(ExtractedField field : fieldToExtract) {
            if(fullParty.containsKey(field.getFieldName())) {
                partial.put(field.getFieldName(), fullParty.get(field.getFieldName()));
            }
        }
        return partial;
    }




    /***
     * Get a Single party BY ID , NOTE remove all notion o cache in the Heap
     * H2 is already a off heap cache !
     * @param partyid
     * @return
     */
    public Map<String,Object> getParty(String partyid) {

        try {
            //StringBuilder partyToSearchFor  = new StringBuilder();
            //partyToSearchFor.append("'").append(partyid).append("'");
            queryObjectPreparedStatement.setString(1, partyid);
            ResultSet rs = queryObjectPreparedStatement.executeQuery();
            if (rs.next()) {
                Object map = rs.getObject("objMap");
                return (Map<String, Object>) map;
            }
        } catch (SQLException ex) {
            logger.error("Error querying object for " + partyid, ex);
        }

        logger.debug("Unable to find data for party ID " + partyid);
        return null;
    }


    /***
     * Get the count of parties in the H2 cache
     *
     * @return
     */
    public String getPartyCount() {

        try {
            StringBuilder retValue = new StringBuilder();
            ResultSet rs = queryPartyCountPreparedStatement.executeQuery();
            if (rs.next()) {
                return Integer.toString(rs.getInt(1));
            }
        } catch (SQLException ex) {
            logger.error("Error getting party cache count " , ex);
        }
        return "0";
    }


    /***
     * Retrieve a list of parties based on Partial Name , if the party search is for contraparties and
     * if the party is for frequently used.
     * @param partyName
     * @param isContraParty
     * @param isfrequentlyUsed
     * @return
     */
    public List<Map<String,Object>> getParties(String partyName, String isContraParty, String isfrequentlyUsed) {


        ArrayList<Map<String,Object>> foundParties = new ArrayList<Map<String,Object>>();
        try {

            String query = this.buildPartyQueryStatement(partyName,isContraParty,isfrequentlyUsed);
            Statement queryStatement = dbConnection.createStatement();
            logger.debug("About the search party cache query {}", query);
            ResultSet rs = queryStatement.executeQuery(query);
            while(rs.next()) {
                Object map = rs.getObject("objMap");
                foundParties.add((Map<String, Object>) map);
            }




        } catch (SQLException ex) {
            logger.error("Error querying object for partial name: " + partyName, ex);
        }

        if(foundParties.size() == 0)
            logger.warn("Unable to find data for partial name " + partyName);
        return foundParties;

    }


    /**
     * Build the party search query
     * @param partyName
     * @param partyIdSearch
     * @param frequentlyUsed
     * @return
     */
    private String buildPartyQueryStatement(String partyName,String partyIdSearch, String frequentlyUsed ){


        StringBuilder queryStatement = new StringBuilder();

        queryStatement.append("SELECT objMap from PARTY WHERE ");


        boolean addAndToQuery = false;

        // Name Like
        if ( partyName != null && !partyName.isEmpty() ) {

            queryStatement.append(" lower(accountName) like ").append("'").append(partyName.toLowerCase()).append("%' ");
            addAndToQuery = true;

        }

        //Is ContraParty search other wise CounterPartys
        //Dount like this will have to change the gui
        if ( partyIdSearch != null && !partyIdSearch.isEmpty() && !partyIdSearch.equalsIgnoreCase("true") ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }

            queryStatement.append(" contraparty  ");

            addAndToQuery = true;

        }

        //Frequently Used Party
        if ( frequentlyUsed != null && !frequentlyUsed.isEmpty() ) {


            //Tis is a freq used counterparty check
            if(frequentlyUsed.equalsIgnoreCase("Y")  )
            {

                if(addAndToQuery) {
                    queryStatement.append(" AND ");
                }

                queryStatement.append(" frequentlyUsedCounterparty  ");
            }

        }

        //Add the limit
        //queryStatement.append(" LIMIT ").append(searchResultsLimit);


        return queryStatement.toString();

    }

    /***
     * Get a H2 DB Connection
     * @return
     */
    private Connection getDbConnection() {
        if(dbConnection != null) {
            return dbConnection;
        }
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("Cant create DB connection:", e);
        }
        String fullyQualifiedDbName = dbFilePath + File.separator +  partyCacheFileName + ".mv.db";
        String fullyDbName = dbFilePath + File.separator +  partyCacheFileName;
        logger.info("Using H2 database  {}", fullyQualifiedDbName );
        File oldFile = new File(fullyQualifiedDbName);
        if(oldFile.exists()) {
            try {
                Files.delete(oldFile.toPath());
            } catch (Exception ex) {
                logger.error("Error deleting db file " + fullyQualifiedDbName , ex);
            }
        }
        try {
            String dbConnectionURL = "jdbc:h2:file:" + fullyDbName;
            dbConnection = DriverManager.getConnection(dbConnectionURL, "", ""); // no login/password
            createTables(dbConnection);
            return dbConnection;
        } catch (SQLException e) {
            logger.error("Error connecting to DB ", e);
        }
        return dbConnection;
    }

    /***
     * Clear the cache
     */
    public void clearCache() {
        try {
            if (dbConnection != null) {
                dbConnection.close();
                dbConnection = null;
            }
        } catch (SQLException e) {
                logger.warn("Error closing db connection ", e);
        }


    }

    /***
     * Create the H2 store and all query objects
     * @param conn
     */
    private void createTables(Connection conn) {
        try {
            PreparedStatement createPreparedStatement = conn.prepareStatement(createTable);
            createPreparedStatement.executeUpdate();
            createPreparedStatement.close();
            insertPreparedStatement = conn.prepareStatement(insertParty);
            updatePreparedStatement = conn.prepareStatement(updateParty);
            queryObjectPreparedStatement = conn.prepareStatement(queryObject);
            deleteObjectPreparedStatement = conn.prepareStatement(deleteParty);
            //Add the Parties Search Prepared Statement
            queryPartyCountPreparedStatement = conn.prepareStatement(queryPartyCount);

        } catch (Exception ex) {
            logger.error("Error creating DB ", ex);
        }


    }


}
