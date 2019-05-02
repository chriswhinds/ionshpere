package com.droitfintech.adeptapi;

import com.fasterxml.jackson.databind.ObjectMapper;




import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;



/**
 * Created by christopherwhinds on 5/3/17.
 */
public class DictionaryService {

    private static Logger logger = LoggerFactory.getLogger(DictionaryService.class);

    private ObjectMapper mapper = new ObjectMapper();

    private Map dictionay = new HashMap();

    private  ArrayList<Map> tradeAttributeDictionaryList;

    private HashMap tradeAttributeDictionary;

    public static String ADEPT_API_SERVICE_HOME   = "adeptAPIServiceHostname";
    public static String ADEPT_API_SERVICE_PORT   = "adeptAPIServicePort";
    public static String ADEPT_API_MODULES_URL    = "adeptGetModulesAPI";
    public static String ADEPT_API_DICTIONARY_URL = "adeptGetDictionaryAPI";

    public  enum AttributeType {
        BOOLEAN("Boolean"),
        DATE("DateType"),
        TENOR("TenorType"),
        STRING("String"),
        ENUMERATEDSTRING("EnumeratedString");

        private String type;

        AttributeType(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }

    }

    /***
     * Load the Data Dictionary
     * * by the API
     */
    public DictionaryService(){

    }

    /***
     * Build the service list
     */
    public void initialize(Properties properties){

        try {
            //Call the Adept Rest API to get the list of Service Available

            // Call the API to get the module list
            DecisionAPIService apiService = new DecisionAPIService(properties);
            apiService.executeGetDataDictionay();
            if(apiService.getStatusCode() == HttpStatus.SC_OK){

                dictionay = mapper.readValue( apiService.getResponse().toString().getBytes(),HashMap.class);
                //adjustDictionaryEnums();
                createTradeAttributeDictionary();

            } else {
                logger.error("Server Exception Occured retreiving Modules list");
            }

        } catch (Exception e) {
            logger.error("Exception Occured",e);

        }


    }

    /***
     * Fix up the map of enums so the map keys match the dictionary attibutes directly
     * Force all keys to lower case and remap the enum value set with
     */
    private void adjustDictionaryEnums(){

        Map enums = getEnums();
        Set<String> enumKeys = enums.keySet();
        for(String enumKey:enumKeys){
            Object enumValues = enums.get(enumKey);
            String adjustedKey = enumKey.toLowerCase();
            enums.put(adjustedKey,enumValues);
        }

    }

    /***
     * Create a map from the trade attributes list keyed by name
     * @return
     */
    public void createTradeAttributeDictionary(){

        ArrayList structureList = (ArrayList) dictionay.get("attributes");
        ArrayList tradeAttributeList = (ArrayList) structureList.get(1);
        tradeAttributeDictionaryList = (ArrayList<Map>)tradeAttributeList.get(1);

        tradeAttributeDictionary = new HashMap();
        ListIterator<Map> attributes = tradeAttributeDictionaryList.listIterator();
        while(attributes.hasNext()){

            Map attribute = attributes.next();
            String attributeName = (String)attribute.get("name");
            tradeAttributeDictionary.put(attributeName , attribute);

        }

    }


    /***
     * Return the Trade attribute Data Dictionary
     * @return
     */
    public Map getTradeAttributeDictionary(){
        return tradeAttributeDictionary;
    }


    /***
     * Return the Trade attribute Data Dictionary
     * @return
     */
    public Map getDataDictionary(){
        return dictionay;
    }

    /***
     * Return the service Structure
     * @return
     */
    public Map getEnums(){

        return (Map)dictionay.get("enums");
    }

    /***
     * Return the Trade Attributes Map of Objects
     * @return
     */
    public ArrayList getTradeAttributes(){

        ArrayList retAttributes = null;

        //ArrayList<ArrayList> attributes = (ArrayList) dictionay.get("attributes");


        //Optional<ArrayList> tradeAttributesValues =  attributes.stream().filter(atr-> atr.contains("trade")).findFirst();


        return retAttributes;
    }





}
