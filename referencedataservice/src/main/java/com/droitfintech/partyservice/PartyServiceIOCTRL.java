package com.droitfintech.partyservice;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;

/***
 * IO Control Bootstrap
 */
public class PartyServiceIOCTRL {
    private static Logger logger = LoggerFactory.getLogger(PartyServiceIOCTRL.class);
    private static String hostName;
    private static int port;
    private Server jettyServer;
    private ObjectMapper mapper = new ObjectMapper();
    public static PartyJsonPersister persister;
    private PartyReader reader;


    private PartyUpdatesEventFileWatcher partyUpdatesEventFileWatcher;

    private PartyEventFileListener partyEventFileListener;

    //System Configration Properties
    public static LinkedHashMap serviceProperties;

    public PartyServiceIOCTRL(LinkedHashMap serviceConfig) {

        serviceProperties = serviceConfig;
        hostName = (String)serviceProperties.get(PartyReferenceDataService.SERVICE_HOST);
        port     = (Integer)serviceProperties.get(PartyReferenceDataService.SERVICE_PORT);

        String serviceHomeLocation =   System.getProperty(PartyReferenceDataService.SERVICE_HOME);
        logger.debug("Party Service Home Location: { }" , serviceHomeLocation );

        String dbFilePath = serviceHomeLocation + (String)serviceProperties.get(PartyReferenceDataService.PARTY_CACHE_LOCATION);
        int maxSearchResults = (Integer)serviceProperties.get(PartyReferenceDataService.PARTY_MAX_SEARCH_RESULTS);

        persister = new PartyJsonPersister(dbFilePath,maxSearchResults);
    }

    /**
     * Start the IO Control Service Impl
     */
    public void startServer(PartyReader partyReader) {
        try {

            logger.info(" Party Reference Data Service Starting  ");


            // Load all of the Parties either from a local file System Source ot from
            // An MongoDb Json source
            /**  START LOADING PARTIES INTO THE CACHE **/


            logger.info("Loading party data from {}.", partyReader.getClass().getSimpleName());
            reader = partyReader;
            reader.parseAndPersistParties(persister);



            //Construct a events reader and wait for update events
            /**  START WATCHING for event file updates **/
            partyEventFileListener = new PartyEventFileListener(reader,persister);

            //partyUpdatesEventFileWatcher = new PartyUpdatesEventFileWatcher(reader,persister);

            //Wait for updates for ever
            //partyUpdatesEventFileWatcher.monitorForPartyUpdateEvents();
            partyEventFileListener.startMonitoringForPartyEvents();
            /**  START WATCHING for event file updates **/



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
            jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",PartyServiceRequestEndPoint.class.getCanonicalName());

            logger.info("Party ReferenceDataService successfully URI: {}",jettyServer.getURI());
            jettyServer.start();
            /**  END Jetty Endpoint Bindings **/

            //Block exit on jetty
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



}
