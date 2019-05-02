package com.droitfintech.adeptapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

/**
 * Created by christopherwhinds on 5/3/17.
 */
public class ModulesService {

    private static Logger logger = LoggerFactory.getLogger(ModulesService.class);
    private ArrayList modules;
    private ObjectMapper mapper = new ObjectMapper();

    private ArrayList<Map> services = new ArrayList();
    /***
     * Load the Moduals list from a JSON string represetnting the Modules currently supported
     * by the API
     */
    public ModulesService(){


    }

    /***
     * Build the service list
     */
    public void initialize(Properties properties){

        try {
            //Call the Adept Rest API to get the list of Service Available

            // Call the API to get the module list
            DecisionAPIService apiService = new DecisionAPIService(properties);
            apiService.executeGetModules();
            if(apiService.getStatusCode() == HttpStatus.SC_OK){
                services = mapper.readValue( apiService.getResponse().toString().getBytes(),ArrayList.class);
            } else {
                logger.error("Server Exception Occured retreiving Modules list");
            }

        } catch (Exception e) {
            logger.error("Exception Occured",e);

        }


    }

    /***
     * Return the Modules map
     * @return
     */
    public ArrayList getModules(){
        return modules;
    }

    /***
     * Return the service Structure
     * @return
     */
    public ArrayList<Map> getServices(){

        return services;
    }


}
