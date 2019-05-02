package com.droitfintech.auditservice;


import com.droitfintech.bootstrap.DroitReferenceDataService;
import com.droitfintech.bootstrap.AbstractReferenceDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by christopherwhinds on 4/10/17.
 */
public class AuditReferenceDataService  extends AbstractReferenceDataService implements DroitReferenceDataService {

    private static Logger logger = LoggerFactory.getLogger(AuditReferenceDataService.class);

    public static String AUDIT_STORE_LOCATION   = "auditStoreLocation";
    public static String AUDIT_STORE_AUTO_CREATE_ON_STARTUP = "auditStoreAutoCreateOnStartup";


    public static String SERVICE_HOST = "auditServiceHostname" ;
    public static String SERVICE_PORT = "auditServicePort";

    protected AuditServiceIOCTRL ioCtrl;


    @Override
    public void runService() {
        loadBootstrapConfiguration();
        createControlService();

    }

    @Override
    public void createControlService() {
        //Modify to use the
        ioCtrl = new AuditServiceIOCTRL(serviceConfig);
        ioCtrl.startServer();
    }






}
