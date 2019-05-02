package com.droitfintech.partyservice;


import com.droitfintech.bootstrap.AbstractReferenceDataService;
import com.droitfintech.bootstrap.DroitReferenceDataService;
import com.droitfintech.partyservice.PartyJsonReader;
import com.droitfintech.partyservice.PartyReader;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by christopherwhinds on 4/10/17.
 */
public class PartyReferenceDataService  extends AbstractReferenceDataService implements DroitReferenceDataService {

    private static Logger logger = LoggerFactory.getLogger(PartyReferenceDataService.class);


    public static String PARTY_CACHE_LOCATION   = "partyCacheLocation";
    public static String PARTY_UPDATES_LOCATION = "partyEventsLocation";
    public static String PARTY_STARTUP_LOCATION = "partyStartUpLocation";
    public static String SERVICE_HOST = "partyServiceHostname" ;
    public static String SERVICE_PORT = "partyServicePort";

    public static String PARTY_MAX_SEARCH_RESULTS  = "maxPartiesReturnedOnSearch";
    public static String SERVICE_PARTY_JSON_SOURCE = "serviceSourceMode";

    /***
     * Party JSON Data Source
     */
    public enum SERVICE_PARTY_SOURCE {
        FileSystem,
        MongoDb
    }


    protected PartyServiceIOCTRL ioCtrl;


    @Override
    public void runService() {
        loadBootstrapConfiguration();
        createControlService();

    }

    @Override
    public void createControlService() {

        ioCtrl = new PartyServiceIOCTRL(serviceConfig);
        ioCtrl.startServer(createPartyReader());
    }


    /***
     * Create the Party Reader based on the Source
     * @return
     */
    private PartyReader createPartyReader(){

        PartyReader partyReader = null;

        PartyReferenceDataService.SERVICE_PARTY_SOURCE servicePartySource =   PartyReferenceDataService.SERVICE_PARTY_SOURCE.valueOf((String)serviceConfig.get(SERVICE_PARTY_JSON_SOURCE));
        switch(servicePartySource){
            case FileSystem:
                partyReader = new PartyJsonReader(serviceConfig);
                break;
            case MongoDb:
                partyReader = new MongoDbPartyReader(serviceConfig);
                break;

        }
        return partyReader;
    }

}
