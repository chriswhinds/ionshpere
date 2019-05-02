package com.droitfintech.bootstrap;


import com.droitfintech.auditservice.AuditReferenceDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * Service Bootstraper runs a given service using reflections to instanciate the class representing
 * the service implementattion
 */
public class ServiceRunner {

    private static Logger logger = LoggerFactory.getLogger(ServiceRunner.class);
    /**
     * Runtime  Bootstrap
     * @param args
     */
    public static void main(String[] args )
    {

        try {
            String serviceClassName = (String)args[0];
            logger.info("Sevice Runner -> About the run Service Class [ {} ]",serviceClassName);
            Class serviceClazz = (Class)Class.forName(serviceClassName);
            DroitReferenceDataService serviceToRun = (DroitReferenceDataService)serviceClazz.newInstance();
            serviceToRun.runService();

        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (InstantiationException e){
            System.out.println(e.getMessage());
        } catch (IllegalAccessException e){
            System.out.println(e.getMessage());
        }

    }
}
