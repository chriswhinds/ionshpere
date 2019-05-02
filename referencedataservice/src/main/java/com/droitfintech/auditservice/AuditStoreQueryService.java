package com.droitfintech.auditservice;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 *
 */
public class AuditStoreQueryService {
    private static Logger logger = LoggerFactory.getLogger(AuditStoreQueryService.class);

    private Connection dbConnection;
    private String autoStoreFileLocation;

    private boolean autoCreateOnStartUp;

    private String fullyQualifiedAuditStoreDBName;
    private String auditStoreDBName;


    private ObjectMapper mapper = new ObjectMapper();

    //Db Prepared Statements
    private PreparedStatement createAuditTablePreparedStatement;
    private PreparedStatement createIndexByDecisionDatePreparedStatement;
    private PreparedStatement createIndexByGroupIdStatementPreparedStatement;
    private PreparedStatement createIndexBySubmissionDatePreparedStatement;
    private PreparedStatement instertDecsionsPreparedStatement;

    private PreparedStatement selectDecsionByGroupIdPreparedStatement;

    private String AUDIT_STORE_NAME_PREFIX = "auditstore";

    private int inserts = 0;
    private int updates = 0;
    private int maxCached = 50000;


    protected LinkedHashMap serviceConfig;

    protected DateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

    public enum AUDIT_COLUMN_POSITION{
        decisionID(1),
        applicationName(2),
        decisionDate(3),
        decisionSnapShot(4),
        groupId(5),
        groupTradeDecision(6),
        metRequired(7),
        midRequired(8),
        midValue(9),
        override(10),
        overrideApprover(11),
        overrideComments(12),
        overrideDate(13),
        overrideUser(14),
        scenarioAnalysisFile(15),
        scenarioAnalysisFileName(16),
        scenarioAnalysisFileType(17),
        submissionDate(18),
        tradeAssetClass(19),
        tradeBaseProduct(20),
        tradeContrapartyId(21),
        tradeCounterpartyId(22),
        tradeEffectiveDate(23),
        tradeGoldenSourceId(24),
        tradeGroupId(25),
        tradeNotional(26),
        tradeSalesPerson(27),
        tradeSubProduct(28),
        tradeTerm(29),
        tradeTerminationDate(30),
        tradeVenue(31),
        trader(32),
        userID(33);

        private final int value;

        AUDIT_COLUMN_POSITION(final int newValue) {
            value = newValue;
        }

        public int getValue() { return value; }
    }


    private String createAuditTableStatement =  " create table AuditRecord ( " +
                                                " decisionId varchar(255) PRIMARY KEY not null, " +
                                                " applicationName VARCHAR(255)," +
                                                " decisionDate DATETIME, " +
                                                " decisionSnapshot LONGTEXT, " +
                                                " groupId VARCHAR(255), " +
                                                " groupTradeDecision bit not null, " +
                                                " metRequired BIT not null, " +
                                                " midRequired BIT not null, " +
                                                " midValue VARCHAR(255), " +
                                                " override BIT not null, " +
                                                " overrideApprover VARCHAR(255), " +
                                                " overrideComments VARCHAR(255), " +
                                                " overrideDate DATETIME, " +
                                                " overrideUser VARCHAR(255), " +
                                                " scenarioAnalysisFile LONGBLOB, " +
                                                " scenarioAnalysisFileName VARCHAR(255), " +
                                                " scenarioAnalysisFileType VARCHAR(255), " +
                                                " submissionDate DATETIME, " +
                                                " tradeAssetClass VARCHAR(255), " +
                                                " tradeBaseProduct VARCHAR(255), " +
                                                " tradeContrapartyId VARCHAR(255), " +
                                                " tradeCounterpartyId VARCHAR(255), " +
                                                " tradeEffectiveDate DATE, " +
                                                " tradeGoldenSourceId VARCHAR(255), " +
                                                " tradeGroupId VARCHAR(255), " +
                                                " tradeNotional DECIMAL(19,2), " +
                                                " tradeSalesPerson VARCHAR(255), " +
                                                " tradeSubProduct VARCHAR(255), " +
                                                " tradeTerm VARCHAR(255), " +
                                                " tradeTerminationDate DATE, " +
                                                " tradeVenue VARCHAR(255), " +
                                                " trader VARCHAR(255), " +
                                                " userID VARCHAR(255) " +
                                                " )" ;


    private String createIndexByDecisionDateStatement = "create index audit_dec_date_index on AuditRecord (decisionDate)";

    private String createIndexByGroupIdStatement      = "create index audit_droit_id_index on AuditRecord (groupId)";

    private String createIndexBySubmissionDateStatement = "create index audit_sub_date_index on AuditRecord (submissionDate)";

    private String insertAuditRecordStatement = "INSERT INTO AuditRecord VALUES (?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?," +
                                                                                 "?)";


    private String selectDecisionById         = "select * from AuditRecord where decisionId = ?";

    /**
     * Change audit record id to decisionId as a varchar(255)
     */
    /*  Audit table defintions

            create table AuditRecord (
                decisionId varchar(255) not null
                applicationName varchar(255),
                decisionDate datetime,
                decisionSnapshot longtext,
                groupId varchar(255),
                groupTradeDecision bit not null,
                metRequired bit not null,
                midRequired bit not null,
                midValue varchar(255),
                override bit not null,
                overrideApprover varchar(255),
                overrideComments varchar(255),
                overrideDate datetime,
                overrideUser varchar(255),
                scenarioAnalysisFile longblob,
                scenarioAnalysisFileName varchar(255),
                scenarioAnalysisFileType varchar(255),
                submissionDate datetime,
                tradeAssetClass varchar(255),
                tradeBaseProduct varchar(255),
                tradeContrapartyId varchar(255),
                tradeCounterpartyId varchar(255),
                tradeEffectiveDate date,
                tradeGoldenSourceId varchar(255),
                tradeGroupId varchar(255),
                tradeNotional decimal(19,2),
                tradeSalesPerson varchar(255),
                tradeSubProduct varchar(255),
                tradeTerm varchar(255),
                tradeTerminationDate date,
                tradeVenue varchar(255),
                trader varchar(255),
                userID varchar(255),
                primary key (decisionId)
            );

     */





    /**
     * Constutor
     * Create the reference Audit DB as an H2 Database
     * if it does not exist
     * @param config
     */
    public AuditStoreQueryService(LinkedHashMap config ) throws Exception {
        this.serviceConfig = config;
        autoStoreFileLocation = (String)serviceConfig.get(AuditReferenceDataService.AUDIT_STORE_LOCATION);

        String serviceHomeLocation =   System.getProperty(AuditReferenceDataService.SERVICE_HOME);
        //Add in the Service Home
        autoStoreFileLocation = serviceHomeLocation +  autoStoreFileLocation  ;

        this.createDbSchemaAndConnection();


    }


    /***
     * Get a H2 DB Connection
     * @return
     */
    private void createDbSchemaAndConnection() {

        //Check if the jdbc driver is in the class path
        try {

            Class.forName("org.h2.Driver");

        } catch (ClassNotFoundException e) {

            logger.error("Cant create DB connection:", e);
            return;
        }

        autoCreateOnStartUp = (Boolean)serviceConfig.get(AuditReferenceDataService.AUDIT_STORE_AUTO_CREATE_ON_STARTUP);
        fullyQualifiedAuditStoreDBName =  autoStoreFileLocation + File.separator + AUDIT_STORE_NAME_PREFIX + ".mv.db";
        auditStoreDBName =  autoStoreFileLocation + File.separator + AUDIT_STORE_NAME_PREFIX;

        logger.info("Using AuditStore H2 database  {}", fullyQualifiedAuditStoreDBName);


        //Always create the db if create on startup
        //if not create on startup and the db does not exist create it
        //NOTE: Should only exist on initial start for the first time.
        if(autoCreateOnStartUp)
           createDataBase();
        else {
            if(databaseExist()) {
                openDataBase();
            } else {
                createDataBase();
            }
        }

    }

    /**
     * Create a Database from scratch
     */
    public void createDataBase(){

        try {

            if(autoCreateOnStartUp)
              logger.info("Audit DB AutoCreateOnStartUp -> [ {} ]", Boolean.toString(autoCreateOnStartUp));

            removeDBfile(fullyQualifiedAuditStoreDBName);
            logger.info("Creating Audit DB [ {} ]",auditStoreDBName);
            String dbConnectionURL = "jdbc:h2:file:" + auditStoreDBName;
            dbConnection = DriverManager.getConnection(dbConnectionURL, "", ""); // no login/password
            //Delete the file if it exist

            //Re-create the db
            createTables();
            createPreparedStatements();

        } catch (SQLException e) {
            logger.error("Error connecting to DB ", e);
        }


    }

    /**
     * Open an existing Audit DB If , Not AutoCreate and Db not created whish is usually the case
     * when starting service for the fiest time , create the db anyway.
     */
    private void openDataBase(){

        try {

            logger.info("Open Audit DB [ {} ]",auditStoreDBName);
            String dbConnectionURL = "jdbc:h2:file:" + auditStoreDBName;
            dbConnection = DriverManager.getConnection(dbConnectionURL, "", ""); // no login/password
            //Leave the DB inplace just create the prepared statments
            createPreparedStatements();

        } catch (SQLException e) {
            logger.error("Error connecting to DB ", e);
        }


    }

    /***
     * Check if the db file exist
     * @return
     */
    private boolean databaseExist(){

        File oldFile = new File(fullyQualifiedAuditStoreDBName);
        return oldFile.exists();
    }
    /***
     * Remove the DB File if it exsist
     * @param fileName
     */
    private void removeDBfile(String fileName){

        File oldFile = new File(fileName);
        //Recreate on start up if configured to do so else
        if(oldFile.exists()) {
            try {
                Files.delete(oldFile.toPath());
                logger.info("Deleted Audit DB file -> {}",fileName);
            } catch (Exception ex) {
                logger.error("Error deleting db file " + fileName , ex);
            }
        }
    }



    /***
     * Get a H2 DB Connection
     * @return
     */
    private Connection getDbConnection() {
        return dbConnection;
    }


    /**
     * insert a decision into the audit store store
     *
     * @param decsisionToSave
     */
    public  void saveDecision(Map decsisionToSave) throws Throwable{


        try {

            logger.debug("Saving Decsion for id[ {} ]",(String)decsisionToSave.get("decisionId"));

            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.decisionID.getValue(),(String)decsisionToSave.get("decisionId"));

            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.applicationName.getValue(),(String)decsisionToSave.get("applicationName"));
            instertDecsionsPreparedStatement.setDate(AUDIT_COLUMN_POSITION.decisionDate.getValue(), new java.sql.Date( ((Date)dateFormater.parse((String)decsisionToSave.get("decisionDate"))).getTime()  ) );

            //Get JSON decision Structure  STRANGE FIX
            //String decisionString = mapper.writeValueAsString((LinkedHashMap)decsisionToSave.get("droitDecision"));
            String decisionString = (String)decsisionToSave.get("droitDecision");

            ByteArrayInputStream inputStream = new ByteArrayInputStream(decisionString.getBytes());
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

            instertDecsionsPreparedStatement.setClob(AUDIT_COLUMN_POSITION.decisionSnapShot.getValue(),inputStreamReader);
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.groupId.getValue(),(String)decsisionToSave.get("groupId"));
            instertDecsionsPreparedStatement.setBoolean(AUDIT_COLUMN_POSITION.groupTradeDecision.getValue(), (boolean)decsisionToSave.get("groupTradeDecision"));
            instertDecsionsPreparedStatement.setBoolean(AUDIT_COLUMN_POSITION.metRequired.getValue(), (boolean)decsisionToSave.get("metRequired"));
            instertDecsionsPreparedStatement.setBoolean(AUDIT_COLUMN_POSITION.midRequired.getValue(), (boolean)decsisionToSave.get("midRequired"));
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.midValue.getValue(),(String)decsisionToSave.get("midValue"));
            instertDecsionsPreparedStatement.setBoolean(AUDIT_COLUMN_POSITION.override.getValue(), (boolean)decsisionToSave.get("override"));
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.overrideApprover.getValue(),(String)decsisionToSave.get("overrideApprover"));
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.overrideComments.getValue(),(String)decsisionToSave.get("overrideComments"));


            instertDecsionsPreparedStatement.setDate(AUDIT_COLUMN_POSITION.overrideDate.getValue(), new java.sql.Date( ((Date)dateFormater.parse((String)decsisionToSave.get("overrideDate"))).getTime()  ) );

            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.overrideUser.getValue(),(String)decsisionToSave.get("overrideUser"));
            inputStream = new ByteArrayInputStream("NA".getBytes());
            instertDecsionsPreparedStatement.setBlob( AUDIT_COLUMN_POSITION.scenarioAnalysisFile.getValue() , inputStream );
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.scenarioAnalysisFileName.getValue(),(String)decsisionToSave.get("scenarioAnalysisFileName"));
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.scenarioAnalysisFileType.getValue(),(String)decsisionToSave.get("scenarioAnalysisFileType"));

            instertDecsionsPreparedStatement.setDate(AUDIT_COLUMN_POSITION.submissionDate.getValue(), new java.sql.Date( ((Date)dateFormater.parse((String)decsisionToSave.get("submissionDate"))).getTime()  ) );

            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.tradeAssetClass.getValue(),(String)decsisionToSave.get("tradeAssetClass"));
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.tradeBaseProduct.getValue(),(String)decsisionToSave.get("tradeBaseProduct"));
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.tradeContrapartyId.getValue(),(String)decsisionToSave.get("tradeContrapartyId"));
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.tradeCounterpartyId.getValue(),(String)decsisionToSave.get("tradeCounterpartyId"));
            instertDecsionsPreparedStatement.setDate(AUDIT_COLUMN_POSITION.tradeEffectiveDate.getValue(), new java.sql.Date( ((Date)dateFormater.parse((String)decsisionToSave.get("tradeEffectiveDate"))).getTime()  ) );


            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.tradeGoldenSourceId.getValue(),(String)decsisionToSave.get("tradeGoldenSourceId"));
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.tradeGroupId.getValue(),(String)decsisionToSave.get("tradeGroupId"));


            //Stuff a default value in notional column
            instertDecsionsPreparedStatement.setBigDecimal(AUDIT_COLUMN_POSITION.tradeNotional.getValue(), new  BigDecimal(0.0));

            //double  tradeNotionalObject = decsisionToSave.get("tradeNotional");


            //instertDecsionsPreparedStatement.setBigDecimal(AUDIT_COLUMN_POSITION.tradeNotional.getValue(), new  BigDecimal((double)decsisionToSave.get("tradeNotional")));


            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.tradeSalesPerson.getValue(),(String)decsisionToSave.get("tradeSalesPerson"));
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.tradeSubProduct.getValue(),(String)decsisionToSave.get("tradeSubProduct"));
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.tradeTerm.getValue(),(String)decsisionToSave.get("tradeTerm"));

            //instertDecsionsPreparedStatement.setDate(AUDIT_COLUMN_POSITION.tradeTerminationDate.getValue(), new java.sql.Date( ((Date)dateFormater.parse((String)decsisionToSave.get("tradeTerminationDate"))).getTime()  ) );

            instertDecsionsPreparedStatement.setDate(AUDIT_COLUMN_POSITION.tradeTerminationDate.getValue(), new java.sql.Date( ((Date)dateFormater.parse((String)decsisionToSave.get("tradeEffectiveDate"))).getTime()  ) );



            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.tradeVenue.getValue(),(String)decsisionToSave.get("tradeVenue"));

            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.trader.getValue(),(String)decsisionToSave.get("trader"));
            instertDecsionsPreparedStatement.setString(AUDIT_COLUMN_POSITION.userID.getValue(),(String)decsisionToSave.get("userID"));
            instertDecsionsPreparedStatement.execute();
            dbConnection.commit();

            logger.debug("Saved Decsion for id[ {} ]",(String)decsisionToSave.get("decisionId"));


        } catch (Throwable e) {
            logger.error("Error saving decision object" , e);
            throw new Throwable(e);
        }






    }



    /***
     * Get a Single party BY ID , NOTE remove all notion o cache in the Heap
     * H2 is already a off heap cache !
     * @param id
     * @return
     */
    public List<Map<String,Object>> getDecision(String id) {

        ArrayList<Map<String,Object>> foundDecisions = new ArrayList();
        try {
            selectDecsionByGroupIdPreparedStatement.setString(1, id);
            ResultSet rs = selectDecsionByGroupIdPreparedStatement.executeQuery();


            if (rs.next()) {

                HashMap auditMap = createQueryResultMap(rs);
                foundDecisions.add(auditMap);
            }

         } catch (Throwable ex) {
            logger.error("Error querying object for " + id, ex);
        }

        logger.debug("Unable to find data for Decision ID " + id);
        return foundDecisions;
    }

    /**
     * Query the AuditRecord Store with the parameters and produce as list of one or more
     * Decisions
     *
     * @param decisionDateStart
     * @param decisionDateEnd
     * @param submissionDateStart
     * @param submissionDateEnd
     * @param externalTradeID
     * @param assetClass
     * @param product
     * @param subProduct
     * @param contrapartyID
     * @param counterpartyID
     * @param trader
     * @param salesPerson
     * @param user
     * @param overriden
     * @param allowedToTrade
     * @return
     */
    public List<Map<String,Object>> getDecisions( String decisionDateStart,
                                      String decisionDateEnd,
                                      String submissionDateStart,
                                      String submissionDateEnd,
                                      String externalTradeID,
                                      String assetClass,
                                      String product,
                                      String subProduct,
                                      String contrapartyID,
                                      String counterpartyID,
                                      String trader,
                                      String salesPerson,
                                      String user,
                                      String overriden,
                                      String allowedToTrade) {


        ArrayList<Map<String,Object>> foundDecisions = new ArrayList();

        //Create a dynamic query for the


       try {

             String query = this.createQueryStatement(decisionDateStart,
                                                     decisionDateEnd,
                                                     submissionDateStart,
                                                     submissionDateEnd,
                                                     externalTradeID,
                                                     assetClass,
                                                     product,
                                                     subProduct,
                                                     contrapartyID,
                                                     counterpartyID,
                                                     trader,
                                                     salesPerson,
                                                     user,
                                                     overriden,
                                                     allowedToTrade);

            Statement queryStatement = dbConnection.createStatement();
            ResultSet rs = queryStatement.executeQuery(query);

            while(rs.next()) {

                HashMap auditMap = createQueryResultMap(rs);
                foundDecisions.add(auditMap);
            }


        } catch (Throwable ex) {
            logger.error("Error querying decisions from AuditRecords " ,ex);
        }


        return foundDecisions;

    }

    /*
  decisionId varchar(255) not null
  applicationName varchar(255),
  decisionDate datetime,
  decisionSnapshot longtext,
  groupId varchar(255),
  groupTradeDecision bit not null,
  metRequired bit not null,
  midRequired bit not null,
  midValue varchar(255),
  override bit not null,
  overrideApprover varchar(255),
  overrideComments varchar(255),
  overrideDate datetime,
  overrideUser varchar(255),
  scenarioAnalysisFile longblob,
  scenarioAnalysisFileName varchar(255),
  scenarioAnalysisFileType varchar(255),
  submissionDate datetime,
  tradeAssetClass varchar(255),
  tradeBaseProduct varchar(255),
  tradeContrapartyId varchar(255),
  tradeCounterpartyId varchar(255),
  tradeEffectiveDate date,
  tradeGoldenSourceId varchar(255),
  tradeGroupId varchar(255),
  tradeNotional decimal(19,2),
  tradeSalesPerson varchar(255),
  tradeSubProduct varchar(255),
  tradeTerm varchar(255),
  tradeTerminationDate date,
  tradeVenue varchar(255),
  trader varchar(255),
  userID varchar(255),
  */

    /***
     * Create a hashmap of the database tuple returned
     * @param rs
     * @return
     */
    private HashMap<String,Object> createQueryResultMap( ResultSet rs){

        HashMap<String,Object> auditMap = new HashMap();


        try {

            auditMap.put("decisionId", rs.getString("decisionId"));
            auditMap.put("applicationName", rs.getString("applicationName"));
            auditMap.put("decisionDate", rs.getDate("decisionDate"));

            Clob decisionData = rs.getClob("decisionSnapshot");
            String decisionJson = decisionData.getSubString(1,(int)decisionData.length());
            Map decisionMap = mapper.readValue(decisionJson,HashMap.class);
            auditMap.put("droitDecision",decisionMap);


            auditMap.put("groupId", rs.getString("groupId"));
            auditMap.put("groupTradeDecision", rs.getBoolean("groupTradeDecision"));
            auditMap.put("metRequired", rs.getBoolean("metRequired"));
            auditMap.put("midRequired", rs.getBoolean("midRequired"));

            auditMap.put("midValue", rs.getString("midValue"));
            auditMap.put("override", rs.getBoolean("override"));
            auditMap.put("overrideApproverId", rs.getString("overrideApprover"));
            auditMap.put("overrideComments", rs.getString("overrideComments"));
            auditMap.put("overrideDate", rs.getDate("overrideDate"));
            auditMap.put("overrideUser", rs.getString("overrideUser"));
            auditMap.put("submissionDate", rs.getDate("submissionDate"));
            auditMap.put("tradeAssetClass", rs.getString("tradeAssetClass"));
            auditMap.put("tradeBaseProduct", rs.getString("tradeBaseProduct"));
            auditMap.put("tradeContrapartyId ", rs.getString("tradeContrapartyId"));
            auditMap.put("tradeCounterpartyId", rs.getString("tradeCounterpartyId"));
            auditMap.put("tradeEffectiveDate", rs.getDate("tradeEffectiveDate"));
            auditMap.put("tradeGoldenSourceId", rs.getString("tradeGoldenSourceId"));
            auditMap.put("tradeGroupId", rs.getString("tradeGroupId"));
            auditMap.put("tradeNotional", rs.getBigDecimal("tradeNotional"));
            auditMap.put("tradeSalesPerson", rs.getString("tradeSalesPerson"));
            auditMap.put("tradeSubProduct", rs.getString("tradeSubProduct"));
            auditMap.put("tradeTerm", rs.getString("tradeTerm"));
            auditMap.put("tradeTerminationDate", rs.getDate("tradeTerminationDate"));
            auditMap.put("tradeVenue", rs.getString("tradeVenue"));
            auditMap.put("traderId", rs.getString("trader"));
            auditMap.put("userID", rs.getString("userID"));



        } catch (SQLException e) {
             logger.error("Exception Occured :",e);

        } catch (IOException e) {
            logger.error("Exception Occured :",e);
        }

        return auditMap;

    }

    /**
     * Create a sql select statement for querying the AuditStore
     * @param decisionDateStart
     * @param decisionDateEnd
     * @param submissionDateStart
     * @param submissionDateEnd
     * @param externalTradeID
     * @param assetClass
     * @param product
     * @param subProduct
     * @param contrapartyID
     * @param counterpartyID
     * @param trader
     * @param salesPerson
     * @param user
     * @param overriden
     * @param allowedToTrade
     * @return
     */
    private String createQueryStatement( String decisionDateStart,
                                         String decisionDateEnd,
                                         String submissionDateStart,
                                         String submissionDateEnd,
                                         String externalTradeID,
                                         String assetClass,
                                         String product,
                                         String subProduct,
                                         String contrapartyID,
                                         String counterpartyID,
                                         String trader,
                                         String salesPerson,
                                         String user,
                                         String overriden,
                                         String allowedToTrade) {


        StringBuilder queryStatement = new StringBuilder();

        queryStatement.append("SELECT * FROM AuditRecord WHERE ");

        boolean addAndToQuery = false;

        //Decision Start greater that or equal to
        if (  decisionDateStart != null &&  !decisionDateStart.isEmpty() ) {

            queryStatement.append(" decisionDate >=").append("'").append(decisionDateStart).append("' ");
            addAndToQuery = true;

        }

        //Decision End greater that or equal to
        if ( decisionDateEnd != null && !decisionDateEnd.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            queryStatement.append(" decisionDate <=").append("'").append(decisionDateEnd).append("' ");
            addAndToQuery = true;

        }

        //Submission Start greater than or equal to
        if ( submissionDateStart != null &&  !submissionDateStart.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            queryStatement.append(" submissionDate >=").append("'").append(submissionDateStart).append("' ");
            addAndToQuery = true;

        }

        //Sunmission End less than or equal to
        if ( submissionDateEnd != null &&  !submissionDateEnd.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            queryStatement.append(" submissionDate <=").append("'").append(submissionDateEnd).append("' ");
            addAndToQuery = true;
        }

        //External Trade id equal
        if ( externalTradeID != null && !externalTradeID.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");

            }
            queryStatement.append(" tradeGoldenSourceId =").append("'").append(externalTradeID).append("' ");
            addAndToQuery = true;

        }



        if ( assetClass != null && !assetClass.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            queryStatement.append(" tradeAssetClass =").append("'").append(assetClass).append("' ");
            addAndToQuery = true;

        }


        if ( product != null && !product.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            queryStatement.append(" tradeBaseProduct =").append("'").append(product).append("' ");
            addAndToQuery = true;


        }


        if ( subProduct != null && !subProduct.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            queryStatement.append(" tradeSubProduct =").append("'").append(subProduct).append("' ");
            addAndToQuery = true;

        }


        if ( contrapartyID != null &&   !contrapartyID.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            queryStatement.append(" tradeContrapartyId =").append("'").append(contrapartyID).append("' ");
            addAndToQuery = true;

        }

        if ( counterpartyID != null &&!counterpartyID.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            queryStatement.append(" tradeCounterpartyId =").append("'").append(counterpartyID).append("' ");
            addAndToQuery = true;

        }

        if ( trader != null && !trader.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            queryStatement.append(" trader =").append("'").append(trader).append("' ");
            addAndToQuery = true;

        }

        if ( salesPerson != null && !salesPerson.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            queryStatement.append(" tradeSalesPerson =").append("'").append(salesPerson).append("' ");
            addAndToQuery = true;

        }


        if ( user != null && !user.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            queryStatement.append(" userID =").append("'").append(user).append("' ");
            addAndToQuery = true;

        }


        if ( overriden != null && !overriden.isEmpty() ) {

            if(addAndToQuery) {
                queryStatement.append(" AND ");
            }
            if(overriden.equalsIgnoreCase("TRUE"))
                queryStatement.append(" override ");
            else
                queryStatement.append(" NOTE override ");
            addAndToQuery = true;

        }

        return queryStatement.toString();

    }



    /***
     * Create the H2 store and all query objects
     */
    private void createTables() {
        try {

            logger.info("Creating the Audit DB Schema  ..");
            //Create AuditRecord table
            createAuditTablePreparedStatement = dbConnection.prepareStatement(createAuditTableStatement);
            createAuditTablePreparedStatement.executeUpdate();
            createAuditTablePreparedStatement.close();


            //Create the search indexes
            createIndexByDecisionDatePreparedStatement = dbConnection.prepareStatement(createIndexByDecisionDateStatement);
            createIndexByDecisionDatePreparedStatement.executeUpdate();
            createIndexByDecisionDatePreparedStatement.close();

            createIndexByGroupIdStatementPreparedStatement = dbConnection.prepareStatement(createIndexByGroupIdStatement);
            createIndexByGroupIdStatementPreparedStatement.executeUpdate();
            createIndexByGroupIdStatementPreparedStatement.close();

            createIndexBySubmissionDatePreparedStatement = dbConnection.prepareStatement(createIndexBySubmissionDateStatement);
            createIndexBySubmissionDatePreparedStatement.executeUpdate();
            createIndexBySubmissionDatePreparedStatement.close();

            logger.info("Created the Audit DB Schema ..");

        } catch (Exception ex) {
            logger.error("Error creating DB ", ex);
        }


    }

    /***
     * Create the query and save prepared Statements
     */
    private void createPreparedStatements(){
        try {

            logger.info("Creating the Audit DB Sql Prepared Statements  ..");

            //create the insert prepared statement
            instertDecsionsPreparedStatement =  dbConnection.prepareStatement(insertAuditRecordStatement);

            //create preparead select statement by id
            selectDecsionByGroupIdPreparedStatement = dbConnection.prepareStatement(this.selectDecisionById);

            logger.info("Created the Audit DB Sql Prepared Statements ..");

        } catch (Exception ex) {
            logger.error("Error creating DB ", ex);
        }
    }


}
