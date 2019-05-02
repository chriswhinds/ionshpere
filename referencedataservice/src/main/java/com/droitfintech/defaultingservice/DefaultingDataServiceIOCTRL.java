package com.droitfintech.defaultingservice;


import com.droitfintech.adeptapi.DecisionAPIService;
import com.droitfintech.adeptapi.DictionaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

import static com.droitfintech.adeptapi.DictionaryService.ADEPT_API_DICTIONARY_URL;
import static com.droitfintech.adeptapi.DictionaryService.ADEPT_API_SERVICE_HOME;
import static com.droitfintech.adeptapi.DictionaryService.ADEPT_API_SERVICE_PORT;
import static com.droitfintech.bootstrap.AbstractReferenceDataService.SERVICE_HOME;

/***
 * IO Control Bootstrap
 */
public class DefaultingDataServiceIOCTRL {
    private static Logger logger = LoggerFactory.getLogger(DefaultingDataServiceIOCTRL.class);
    private static String hostName;
    private static int port;
    private Server jettyServer;

    public static HashMap<String,Map> requestByTaxonomyMap = new HashMap<String,Map>();

    public static HashMap<String,Map> dictionaryMap = new HashMap<String,Map>();


    private ObjectMapper mapper = new ObjectMapper();


    public static ProductMasterUtils productMasterUtils = new ProductMasterUtils();


    public static DictionaryService dictionaryService = new DictionaryService();

    public static RequestAttributeResolver getAttributeResolver() {
        return attributeResolver;
    }

    private static RequestAttributeResolver attributeResolver;



    //System Configration Properties
    public static LinkedHashMap serviceProperties;

    public DefaultingDataServiceIOCTRL(LinkedHashMap serviceConfig) {

        serviceProperties = serviceConfig;
        hostName = (String) serviceProperties.get(DefaultingDataService.SERVICE_HOST);
        port = (Integer) serviceProperties.get(DefaultingDataService.SERVICE_PORT);

    }


    /***
     * Get the translation map from the loaded set of all maps by taxonomy point
     */
    public static Map findTradeTaxonymyMap( String productType ,String assetClass,String baseProduct , String subProduct){
        String key = productType +  ":" +  assetClass + ":" + baseProduct + ":" + subProduct;
        return requestByTaxonomyMap.get(key);
    }


    public static String getTaxonomyAdjusted(String productTaxonomyPoint){

        Map productTaxonomyPointMap = dictionaryMap.get("isda-product-transfomers");
        return (String) productTaxonomyPointMap.get(productTaxonomyPoint);

    }

    /**
     * Start the IO Control Service Impl
     */
    public void startServer() {
        try {

            logger.info(" Defaulting Data Service Starting  ");


            //Load the trade Request Taxonomy map cache
            DefaultingDataServiceIOCTRL.LoadRequestTaxonomyMap();

            //INitialize all of the Product Master lookup tables
            DefaultingDataServiceIOCTRL.productMasterUtils.init();

            Properties properties = new Properties();

            properties.put(DecisionAPIService.DECISION_API_HOSTNAME,            serviceProperties.get(ADEPT_API_SERVICE_HOME));
            properties.put(DecisionAPIService.DECISION_API_PORT,                serviceProperties.get(ADEPT_API_SERVICE_PORT));
            properties.put(DecisionAPIService.DECISION_API_REQUEST_DICTIONARY,  serviceProperties.get(ADEPT_API_DICTIONARY_URL));

            //Initalize the Dictionary serivce
            DefaultingDataServiceIOCTRL.dictionaryService.initialize(properties);


            //Load the default
            String attributeResolverClassName = System.getProperties().getProperty("defaultRequestAttributeReslover");
            Class atrributeResolverClass = Class.forName(attributeResolverClassName);
            attributeResolver = (RequestAttributeResolver)atrributeResolverClass.newInstance();


            /**
             * Construct the Jetty Container and all of the Jersey Servlet and bind the rest end point
             * Then start the server
             */
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            context.setContextPath("/");

            jettyServer = new Server(getControlHostaddress());;
            jettyServer.setHandler(context);

            ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
            jerseyServlet.setInitOrder(0);

            // Bind the jersey REST endpoint handler to jetty.
            jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",DefaultingServiceRequestEndPoint.class.getCanonicalName());

            logger.info(" Static ReferenceDataService successfully URI: {}",jettyServer.getURI());
            jettyServer.start();
            /**  END Jetty Endpoint Bindings **/

            logger.info("Static Service IOCTRL initialized and started");

            //Block to cloes of the main thread by joining to jetty thread , wait for kill signal from shutdown script
            jettyServer.join();




        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        finally{
            logger.info("Shutting down server");
            System.exit(0);
        }
    }


    /**
     * Construct a InetSocketAddress
     * @return InetSocketAddress
     */
    private InetSocketAddress getControlHostaddress() {

        InetSocketAddress hostAddress = new InetSocketAddress(hostName, port );
        return hostAddress;
    }


    /****
     * Function to load the taxonomy map on the fly
     */
    public static synchronized void  LoadRequestTaxonomyMap() throws Exception{

        ObjectMapper mapper = new ObjectMapper();

        try {

            String serviceHomeLocation =   System.getProperty(SERVICE_HOME);
            String requestDictopnaryFile =   serviceHomeLocation  +  (String)serviceProperties.get(DefaultingDataService.REQUEST_DICTIONARY_FILE_LOCATION);

            File requestDictionaryFile = new File(requestDictopnaryFile);
            if (!requestDictionaryFile.exists()) {

                logger.error("Error RequestDictionary not fond at location [ {} ] service shutting down",requestDictopnaryFile);
                System.exit(1);

            }
            byte[] docBuffer = new byte[(int)requestDictionaryFile.length()];
            try {


                FileInputStream targetFileStream = new FileInputStream(requestDictionaryFile);
                IOUtils.readFully(targetFileStream,docBuffer);

                //Convert the file into json Map
                dictionaryMap = mapper.readValue(docBuffer,HashMap.class);

                ArrayList dictionaryList = (ArrayList)dictionaryMap.get("trade-attribute-maps-list");

                ListIterator<Map> dictionaryEntries = dictionaryList.listIterator();
                while(dictionaryEntries.hasNext()){
                    Map dictionayEntry = dictionaryEntries.next();
                    String isdaToxonomyPoint = (String)dictionayEntry.get("trade-taxonomy");
                    DefaultingDataServiceIOCTRL.requestByTaxonomyMap.put(isdaToxonomyPoint,dictionayEntry);

                }



            } catch (IOException e) {
                logger.error("Exception Occured:",e);
                throw new Exception("Exception Occured while loadlin or Reloading trade attribute meta-data cache",e);
            }

        } catch (Exception e) {
            logger.error("Exception Occured:",e);
            throw new Exception("Exception Occured while loadlin or Reloading trade attribute meta-data cache",e);
        }





    }






}
