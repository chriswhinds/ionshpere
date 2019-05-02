package com.droitfintech.staticservice;


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
public class StaticDataServiceIOCTRL {
    private static Logger logger = LoggerFactory.getLogger(StaticDataServiceIOCTRL.class);
    private static String hostName;
    private static int port;
    private Server jettyServer;


    //System Configration Properties
    public static LinkedHashMap serviceProperties;

    public StaticDataServiceIOCTRL(LinkedHashMap serviceConfig) {

        serviceProperties = serviceConfig;
        hostName = (String)serviceProperties.get(StaticDataReferenceDataService.SERVICE_HOST);
        port     = (Integer)serviceProperties.get(StaticDataReferenceDataService.SERVICE_PORT);

    }

    /**
     * Start the IO Control Service Impl
     */
    public void startServer() {
        try {

            logger.info(" Static Data Service Starting  ");



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
            jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",StaticDataServiceRequestEndPoint.class.getCanonicalName());

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



}
