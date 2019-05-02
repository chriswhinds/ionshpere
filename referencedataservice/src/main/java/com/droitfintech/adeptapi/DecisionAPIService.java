package com.droitfintech.adeptapi;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Created by christopherwhinds on 5/2/17.
 */
public class DecisionAPIService {

    private static Logger logger = LoggerFactory.getLogger(DecisionAPIService.class);

    public static final String DECISION_API_HOSTNAME            = "decision.api.hostName";

    public static final String DECISION_API_PORT                   = "decision.api.port";

    public static final String DECISION_API_REQUEST_DECISION    = "decision.request.api.url";
    public static final String DECISION_API_REQUEST_REFRESH     = "decision.refresh.api.url";
    public static final String DECISION_API_REQUEST_MODULE_LIST = "decision.modules.api.url";

    public static final String DECISION_API_REQUEST_DICTIONARY = "decision.dictionary.api.url";



    public String getErrorMsg() {
        return errorMsg;
    }

    private String errorMsg;


    private String hostname ;
    private int port;

    private String requestApi;
    private String refreshApi;
    private String muduleListApi;
    private String dictionaryListApi;
    private String requestUrl;
    private String refreshUrl;
    private String moduleRequestUrl;
    private String dictionaryRequestUrl;






    protected Object responseObject;
    protected CloseableHttpClient client;
    protected CloseableHttpResponse response;

    protected Properties serviceProperties;

    protected int httpResponseStatusCode = 0;




    private String requestMessage;


    /**
     * Ctor
     * @param properties
     * @param decisionRequest
     */
    public DecisionAPIService ( Properties properties ,String decisionRequest ){
        this.requestMessage = decisionRequest;
        this.serviceProperties = properties;
        this.initialize();
    }

    /**
     * Ctor
     * @param properties
     */
    public DecisionAPIService ( Properties properties  ){

        this.serviceProperties = properties;
        this.initialize();
    }


    /**
     * Execute the operration abstract must be implemented by the derived class2
     */
     public void executeRequest() {

         try {

             HttpPost post = new HttpPost(requestUrl);
             post.addHeader("Content-Type", "application/json");
             HttpEntity entity = new ByteArrayEntity(requestMessage.getBytes("UTF-8"));
             post.setEntity(entity);
             executePost(post);

         } catch (Exception e){
             logger.error("Exception has occured",e);
         }

     };

    /**
     * Execute the operration abstract must be implemented by the derived class2
     */
    public void executeGetModules() {

        try {

            HttpGet get = new HttpGet(moduleRequestUrl);
            get.addHeader("Content-Type", "application/json");
            executeGet(get);

        } catch (Exception e){
            logger.error("Exception has occured",e);
        }

    };


    /**
     * Execute the operration abstract must be implemented by the derived class2
     */
    public void executeGetDataDictionay() {

        try {

            HttpGet get = new HttpGet(dictionaryRequestUrl);
            get.addHeader("Content-Type", "application/json");
            executeGet(get);

        } catch (Exception e){
            logger.error("Exception has occured",e);
        }

    };

    /**
     * Execute the operration abstract must be implemented by the derived class2
     */
    public void executeRefresh() {

        try {

            HttpPost post = new HttpPost(refreshUrl);
            post.addHeader("Content-Type", "application/json");
            HttpEntity entity = new ByteArrayEntity(requestMessage.getBytes("UTF-8"));
            post.setEntity(entity);
            executePost(post);

        } catch (Exception e){
            logger.error("Exception has occured",e);
        }

    };

    /**
     * Execute the operration abstract must be implemented by the derived class2
     */
    public void executePost(HttpPost httpPost) {

        try {

            response = client.execute(httpPost);
            this.httpResponseStatusCode = response.getStatusLine().getStatusCode();
            if(httpResponseStatusCode == HttpStatus.SC_OK)
                responseObject = EntityUtils.toString(response.getEntity());
            else
                errorMsg = EntityUtils.toString(response.getEntity());

        } catch (Exception e){
            logger.error("Exception has occured",e);
        } finally {

            try {
                response.close();
            } catch (IOException e) {
                logger.error("Exception occured while closing response: " + e.getMessage());
            }


        }

    };


    /**
     * Execute the operration abstract must be implemented by the derived class2
     */
    public void executeGet(HttpGet httpGet) {

        try {

            response = client.execute(httpGet);
            this.httpResponseStatusCode = response.getStatusLine().getStatusCode();
            if(httpResponseStatusCode == HttpStatus.SC_OK)
                responseObject = EntityUtils.toString(response.getEntity());
            else
                errorMsg = EntityUtils.toString(response.getEntity());

        } catch (Exception e){
            logger.error("Exception has occured",e);
        } finally {

            try {
                response.close();
            } catch (IOException e) {
                logger.error("Exception occured while closing response: " + e.getMessage());
            }


        }

    };




    /**
     * Get the response object from the operation
     * @return
     */
    public Object getResponse() {
        return responseObject;
    }


    /**
     * Get the error response if api call failed
     * @return
     */
    public Object getErrorResponse() {
        return errorMsg;
    }


    /**
     * INitialize the HTTP endpoint
     * Note Must contain BASE URL , SERVICE CONTEXT[ Service Name, method ] , GET QueryParms value pairs or POST value pairs  ,
     */
    public void initialize() {


        hostname   = (String)serviceProperties.get(DECISION_API_HOSTNAME);
        port       = (int)serviceProperties.get(DECISION_API_PORT);
        requestApi = (String)serviceProperties.get(DECISION_API_REQUEST_DECISION);
        refreshApi = (String)serviceProperties.get(DECISION_API_REQUEST_REFRESH);
        muduleListApi = (String)serviceProperties.get(DECISION_API_REQUEST_MODULE_LIST);
        dictionaryListApi = (String)serviceProperties.get(DECISION_API_REQUEST_DICTIONARY);


        //Build the api urls ( Decsion Request and Refresh )
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://").append(hostname).append(":").append(port);
        urlBuilder.append(requestApi);
        requestUrl = urlBuilder.toString();
        logger.info("Configured  Decision Engine API Url target : " + urlBuilder.toString() );

        urlBuilder.setLength(0);
        urlBuilder.append("http://").append(hostname).append(":").append(port);
        urlBuilder.append(refreshApi);
        refreshUrl = urlBuilder.toString();
        logger.info("Configured Decision Engine API Url target : " + urlBuilder.toString() );

        urlBuilder.setLength(0);
        urlBuilder.append("http://").append(hostname).append(":").append(port);
        urlBuilder.append(muduleListApi);
        moduleRequestUrl = urlBuilder.toString();


        urlBuilder.setLength(0);
        urlBuilder.append("http://").append(hostname).append(":").append(port);
        urlBuilder.append(dictionaryListApi);
        dictionaryRequestUrl = urlBuilder.toString();


        logger.info("Configured Decision Engine API Url target : " + urlBuilder.toString() );


        try {
            client = configureHttpClient();
        } catch (Exception e) {
            logger.error("Exception Occured",e);
        }

    }

    /***
     * Set the request message for the Call
     * @param requestMessage
     */
    public void setRequestMessage(String requestMessage) {
        this.requestMessage = requestMessage;
    }



    /**
     * Get the last call http status
     * @return
     */
    public int getStatusCode() {
        return httpResponseStatusCode;
    }

    /**
     * Direct Http Request No SSL
     */
    private CloseableHttpClient configureHttpClient() throws Exception {

        CloseableHttpClient newclient = null;
        newclient = HttpClientBuilder.create().build();
        return newclient;

    }

    /***
     * Test harnest
     * @param args
     */
    public static void main(String[] args){

        ObjectMapper mapper = new ObjectMapper();

        Properties properties = new Properties();
        properties.put(DECISION_API_HOSTNAME,"192.168.2.109");
        properties.put(DECISION_API_PORT,"3000");
        properties.put(DECISION_API_REQUEST_DECISION,"/api/decision/");
        properties.put(DECISION_API_REQUEST_REFRESH,"/api/refresh/");
        properties.put(DECISION_API_REQUEST_MODULE_LIST,"/api/");




        try {

            //Create a Module service
            ModulesService modulesService = new ModulesService();
            modulesService.initialize(properties);


            File requestMsgFile = new File("/Users/christopherwhinds/github/webtool-lite/config/api_sample_request_full.json");
            byte[] msgBuff = new byte[(int)requestMsgFile.length()];

            IOUtils.readFully(new FileInputStream(requestMsgFile),msgBuff);
            HashMap reqMsg = mapper.readValue(msgBuff,HashMap.class);
            //Add the service list to the request
            reqMsg.put("services",modulesService.getServices());

            String requestMessage = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reqMsg);

            //String requestMessage = new String(msgBuff);

            System.out.println("Sending Request");
            System.out.println(requestMessage);

            DecisionAPIService apiService = new DecisionAPIService(properties,requestMessage);
            apiService.executeRequest();
            if(apiService.getStatusCode() == HttpStatus.SC_OK) {


                String response = (String)apiService.getResponse();

                Map msgMap = (HashMap)mapper.readValue(response.getBytes(), HashMap.class );
                String prettyOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msgMap);
                System.out.println("Recv Out put");
                System.out.println(prettyOutput);


            } else {

                String response = (String)apiService.getErrorResponse();
                System.out.println("Recv An Error");
                System.out.println(response);
            }




        } catch (Exception e) {
            e.printStackTrace();
        }


    }



}
