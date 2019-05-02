package com.droitfintech.staticservice;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static com.droitfintech.staticservice.StaticDataReferenceDataService.PARTICIPANT_CACHE;


/**
 * The REST handler for this service
 */
@Path("/refdata")
public class StaticDataServiceRequestEndPoint {


    private static Logger logger = LoggerFactory.getLogger(StaticDataServiceRequestEndPoint.class);

    private ObjectMapper mapper = new ObjectMapper();

    public static String PARTICIPANT__RESULT_COLUMN_HEADERS = "participantResultHeaders";
    /**
     *  Retunn the User Credentials json file
     * @return a json a json stream
     */
    @GET
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserCredentials() {
        try {

            String serviceHomeLocation =   System.getProperty(StaticDataReferenceDataService.SERVICE_HOME);
            String userCredentialsFileLocation = serviceHomeLocation +  (String) StaticDataServiceIOCTRL.serviceProperties.get(StaticDataReferenceDataService.USERCREDENTIALS_LOCATION);
            File userCredentialsFile = new File(userCredentialsFileLocation);
            if (!userCredentialsFile.exists()) {
                return Response.status(Response.Status.NOT_FOUND).entity("User Credentials file not found: ").build();
            }
            byte[] docBuffer = new byte[(int)userCredentialsFile.length()];
            try {


                FileInputStream targetFileStream = new FileInputStream(userCredentialsFile);
                IOUtils.readFully(targetFileStream,docBuffer);
                return Response.ok(docBuffer, MediaType.APPLICATION_JSON).build();

            } catch (IOException e) {
                logger.error("Exception Occured:",e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
            }

        } catch (Exception e) {
            logger.error("Exception Occured:",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
        }


    }




    /**
     *  Retunn the list of FX Quotes as a json file

     * @return a json a json stream
     */
    @GET
    @Path("rates")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFxRates() {
        try {

            String serviceHomeLocation =   System.getProperty(StaticDataReferenceDataService.SERVICE_HOME);
            String ratesFileLocation =  serviceHomeLocation + (String) StaticDataServiceIOCTRL.serviceProperties.get(StaticDataReferenceDataService.FXRATES_LOCATION);
            File ratesFile = new File(ratesFileLocation);
            if (!ratesFile.exists()) {
                return Response.status(Response.Status.NOT_FOUND).entity("Rates File not found: ").build();
            }
            byte[] docBuffer = new byte[(int)ratesFile.length()];
            try {


                FileInputStream targetFileStream = new FileInputStream(ratesFile);
                IOUtils.readFully(targetFileStream,docBuffer);
                return Response.ok(docBuffer, MediaType.APPLICATION_JSON).build();

            } catch (IOException e) {
                logger.error("Exception Occured:",e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
            }

        } catch (Exception e) {
            logger.error("Exception Occured:",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
        }



    }

    /**
     *  Return the system paticipants as a json file

     * @return a json a json stream
     */
    @GET
    @Path("participants")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getParticipants() {
        try {

            try {

                String serviceHomeLocation =   System.getProperty(StaticDataReferenceDataService.SERVICE_HOME);
                String participantsFileLocation =  serviceHomeLocation +  (String) StaticDataServiceIOCTRL.serviceProperties.get(StaticDataReferenceDataService.PARTICIPANT_LOCATION);
                File participantsFile = new File(participantsFileLocation);
                if (!participantsFile.exists()) {
                    return Response.status(Response.Status.NOT_FOUND).entity("Participants File not found: ").build();
                }
                byte[] docBuffer = new byte[(int)participantsFile.length()];
                try {
                    // create a byte array of the file in correct format
                    //read the file into a byte array and set it in the content to send on the response

                    FileInputStream targetFileStream = new FileInputStream(participantsFile);
                    IOUtils.readFully(targetFileStream,docBuffer);
                    return Response.ok(docBuffer, MediaType.APPLICATION_JSON).build();

                } catch (IOException e) {
                    logger.error("Exception Occured:",e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
                }

            } catch (Exception e) {
                logger.error("Exception Occured:",e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
            }


        } catch (Exception e) {
            logger.error("Exception Occured -",e);
        }
        throw new WebApplicationException(500);

    }


    /**
     *  Return the system paticipants as a json file

     * @return a json a json stream
     */
    @GET
    @Path("participantsWithHeaders")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getParticipantsWithHeaders() {
        try {

            try {

                String serviceHomeLocation =   System.getProperty(StaticDataReferenceDataService.SERVICE_HOME);
                String participantsFileLocation =  serviceHomeLocation + (String)StaticDataServiceIOCTRL.serviceProperties.get(StaticDataReferenceDataService.PARTICIPANT_LOCATION);
                File participantsFile = new File(participantsFileLocation);
                if (!participantsFile.exists()) {
                    return Response.status(Response.Status.NOT_FOUND).entity("Participants File not found: ").build();
                }
                byte[] docBuffer = new byte[(int)participantsFile.length()];
                try {

                    HashMap queryResults = new HashMap();

                    String requestedColumns = System.getProperty(PARTICIPANT__RESULT_COLUMN_HEADERS);
                    List headerList = Arrays.asList(requestedColumns.split(","));

                    // create a byte array of the file in correct format
                    //read the file into a byte array and set it in the content to send on the response

                    FileInputStream targetFileStream = new FileInputStream(participantsFile);
                    IOUtils.readFully(targetFileStream,docBuffer);

                    ArrayList<Map<String,Object>> participants = mapper.readValue(docBuffer,ArrayList.class);
                    ArrayList<ArrayList> result =  buildRequestedResult(participants,requestedColumns);
                    queryResults.put("headers",headerList);
                    queryResults.put("results",result);

                    return Response.ok(mapper.writeValueAsString(queryResults), MediaType.APPLICATION_JSON).build();

                } catch (IOException e) {
                    logger.error("Exception Occured:",e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
                }

            } catch (Exception e) {
                logger.error("Exception Occured:",e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
            }


        } catch (Exception e) {
            logger.error("Exception Occured -",e);
        }
        throw new WebApplicationException(500);

    }


    /**
     *  Return the system paticipants as a json file

     * @return a json a json stream
     */
    @GET
    @Path("participantByIdWithHeaders")
    @Produces(MediaType.APPLICATION_JSON)
    public Response participantByIdWithHeaders(@QueryParam(value = "id") final String participantId) {
        try {

            try {

                HashMap queryResults = new HashMap();

                String requestedColumns = System.getProperty(PARTICIPANT__RESULT_COLUMN_HEADERS);
                List headerList = Arrays.asList(requestedColumns.split(","));
                queryResults.put("headers",headerList);
                ArrayList<ArrayList> results = new ArrayList<>();
                queryResults.put("results",results);
                if(StaticDataReferenceDataService.particpantCache.containsKey(participantId)) {
                    results.add(buildSingleResult(StaticDataReferenceDataService.particpantCache.get(participantId),requestedColumns));
                }
                return Response.ok(mapper.writeValueAsString(queryResults), MediaType.APPLICATION_JSON).build();

            } catch (IOException e) {
                logger.error("Exception Occured:",e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
            }



        } catch (Exception e) {
            logger.error("Exception Occured -",e);
        }
        throw new WebApplicationException(500);

    }





    /****
     * Select the request columns and send it back return the listy of list
     * @param participants
     * @param requesstedColumns
     * @return
     */
    private ArrayList<ArrayList> buildRequestedResult(List<Map<String,Object>> participants , String requesstedColumns ){

        ArrayList<ArrayList> response = new ArrayList<ArrayList>();

        String [] selectedPartyAttributes = requesstedColumns.split(",");
        ListIterator partyListIterator =  participants.listIterator();

        while(partyListIterator.hasNext()){

            Map party = (Map)partyListIterator.next();
            ArrayList selectedPartyFields = new ArrayList();

            //For each attributed selected add the value to the list

            for(String attributeKey: selectedPartyAttributes) {

                if(party.containsKey(attributeKey)) {

                    selectedPartyFields.add(party.get(attributeKey));
                }

            }

            response.add(selectedPartyFields);

        }
        return response;

    }


    /****
     * Select the request columns and send it back return the listy of list
     * @param participant
     * @param requestedColumns
     * @return
     */
    private ArrayList buildSingleResult(Map<String,Object> participant , String requestedColumns ){

        ArrayList response = new ArrayList();

        String [] selectedParticipantAttributes = requestedColumns.split(",");
        //For each attributed selected add the value to the list

        for(String attributeKey: selectedParticipantAttributes) {

            if(participant.containsKey(attributeKey)) {

                response.add(participant.get(attributeKey));
            }

        }

        return response;

    }





}
