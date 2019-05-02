package com.droitfintech.auditservice;


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
public class AuditServiceIOCTRL {
    private static Logger logger = LoggerFactory.getLogger(AuditServiceIOCTRL.class);
    private static String hostName;
    private static int port;
    private Server jettyServer;
    private ObjectMapper mapper = new ObjectMapper();


    public static AuditStoreQueryService auditStoreQueryService;


    //System Configration Properties
    public static LinkedHashMap serviceProperties;

    public AuditServiceIOCTRL(LinkedHashMap serviceConfig) {

        serviceProperties = serviceConfig;
        hostName = (String)serviceProperties.get(AuditReferenceDataService.SERVICE_HOST);
        port     = (Integer)serviceProperties.get(AuditReferenceDataService.SERVICE_PORT);


    }

    /**
     * Start the IO Control Service Impl
     */
    public void startServer() {
        try {

            logger.info(" Audit Service Reference Data Service Starting");


            /**
             * Create the Audtit Store and Query Service
             */
            auditStoreQueryService = new AuditStoreQueryService(serviceProperties);


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
            jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",AuditServiceServiceRequestEndPoint.class.getCanonicalName());

            logger.info("AbstractReferenceDataService successfully URI: {}",jettyServer.getURI());
            jettyServer.start();
            /**  END Jetty Endpoint Bindings **/

            logger.info("Audit Service IOCTRL initialized and started");

            //Block Main Bootstrap from exiting *forever* or until kill by shutdown script
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
