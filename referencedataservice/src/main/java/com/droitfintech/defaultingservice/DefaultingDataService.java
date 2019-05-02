package com.droitfintech.defaultingservice;

import com.droitfintech.bootstrap.AbstractReferenceDataService;
import com.droitfintech.bootstrap.DroitReferenceDataService;

/***
 * Created Chris Hinds 08/10/2017
 */
public class DefaultingDataService extends AbstractReferenceDataService implements DroitReferenceDataService {

    public static String SERVICE_HOST = "defaultingServicehostname" ;
    public static String SERVICE_PORT = "defaultingServicePort";

    public static String REQUEST_DICTIONARY_FILE_LOCATION = "defaultRequestDictionary";



    protected DefaultingDataServiceIOCTRL ioCtrl;

    @Override
    public void runService() {
        loadBootstrapConfiguration();
        createControlService();

    }

    /***
     * Create the service , Load the Dictionary from the API
     * Start the Embedded Jetty service
     */
    @Override
    public void createControlService() {

        ioCtrl = new DefaultingDataServiceIOCTRL(serviceConfig);
        ioCtrl.startServer();
    }

}
