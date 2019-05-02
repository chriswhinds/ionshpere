package com.droitfintech.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Client service to read party \s from a json file and supply them back to Adept.
 */

public class AbstractReferenceDataService implements DroitReferenceDataService {

    private static Logger logger = LoggerFactory.getLogger(AbstractReferenceDataService.class);

    public LinkedHashMap getServiceConfig() {
        return serviceConfig;
    }

    protected LinkedHashMap serviceConfig;


    /**
     * System Propeties Keys
     */
    public static String SERVICE_CONFIGFILE_LOCATION = "service.configurationFile";

    public static String SERVICE_HOME = "service.home";


    /***
     * Refactor here
     */
    public void runService() {
    }

    /***
     * Refactor here
     */
    public void createControlService() {
    }


    /**
     * Load the Service Bootstrap config
     */
    public void loadBootstrapConfiguration(){

        if(System.getProperties().containsKey(SERVICE_CONFIGFILE_LOCATION)){
            String bootstrapConfigLocation = System.getProperty(SERVICE_CONFIGFILE_LOCATION);
            Yaml yaml = new Yaml();
            try {
                serviceConfig = (LinkedHashMap) yaml.load(new FileInputStream(bootstrapConfigLocation));

            } catch (FileNotFoundException e) {

                logger.error("AbstractReferenceDataService shutting down , Could not load configuration  .....",e);
                System.exit(100);
            }

            exportToSystemProperties();

        } else {
            logger.error(" AbstractReferenceDataService , Could not locate configuration file .....");
            System.exit(100);
        }

    }

    /****
     * Export the service config to system properties
     */
    private void exportToSystemProperties(){
        logger.info("Adding Service Properties to JVM System  Properties...started");

        Set<Map.Entry> mapEntries = serviceConfig.entrySet();
        for(Map.Entry mapEntry : mapEntries){
            logger.info("Adding Property to system properties-> {}",mapEntry);

            System.getProperties().put(mapEntry.getKey(),mapEntry.getValue());
        }

        logger.info("Adding Service Properties to JVM System  Properties...completed");
    }




}
