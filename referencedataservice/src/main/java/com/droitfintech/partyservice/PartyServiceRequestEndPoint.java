package com.droitfintech.partyservice;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;


/**
 * The REST handler for this service
 */
@Path("/party")
public class PartyServiceRequestEndPoint {


    private static Logger logger = LoggerFactory.getLogger(PartyServiceRequestEndPoint.class);

    private ObjectMapper mapper = new ObjectMapper();


    public static String PARTY_QUERY_RESULT_COLUMN_HEADERS = "partyQueryResultHeaders";

    /**
     *  Get a Counter Party by ID
     *
     */
    @GET
    @Path("byid")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPartyById(@QueryParam(value = "id") final String partyId) {

        try {
            //Refactor to pass the fully intialized persister in to class on the ctor instead of static reference
            Map<String,Object> party = PartyServiceIOCTRL.persister.getParty(partyId);
            if(party != null) {
                String jsonRetValue = mapper.writeValueAsString(party);
                return Response.ok(jsonRetValue, MediaType.APPLICATION_JSON).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("Party not found for ID: " + partyId).build();
            }

        } catch (Exception e) {
            logger.error("Exception Occured -",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
        }
    }

    /**
     * Search CounterParties by parms
     *  of one or more parties
     *   url: 'cptyAutocomplete.do?term='+ encodeURIComponent($("#counterpartyName").val()) + "&partyIdSearch=" + $("#partyIdSearchDropDown").val() + "&frequentlyUsed=" + creditlineVal,
     */
    @GET
    @Path("query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchParties(@QueryParam(value = "term") final String partyName,
                                  @QueryParam(value = "contraparty") final String partyIdSearch,
                                  @QueryParam(value = "frequentlyUsed") final String frequentlyUsed ) {

            //Defensive check if nothing was passed on the search call
            //must specify at least one parameter return BAD_REQUEST
            if(partyName == null && partyIdSearch == null && frequentlyUsed == null ) {
                logger.warn("No selection criteria sent on the search call , return error");
                return Response.status(Response.Status.BAD_REQUEST).entity("No selection criteria sent on the search call , return error").build();

            }

            try {
                //Refactor to pass the fully intialized persister in to class on the ctor instead of static reference
                List<Map<String,Object>> partyies = PartyServiceIOCTRL.persister.getParties(partyName,partyIdSearch,frequentlyUsed);
                if(partyies.size() > 0 ) {
                    String jsonRetValue = mapper.writeValueAsString(partyies);
                    return Response.ok(jsonRetValue, MediaType.APPLICATION_JSON).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND).entity("Party not found for name: " + partyName).build();
                }

            } catch (Exception e) {
                logger.error("Exception Occured -",e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
            }





    }


    /**
     * Search CounterParties by parms
     *  of one or more parties
     *   url: 'cptyAutocomplete.do?term='+ encodeURIComponent($("#counterpartyName").val()) + "&partyIdSearch=" + $("#partyIdSearchDropDown").val() + "&frequentlyUsed=" + creditlineVal,
     */
    @GET
    @Path("queryWithParms")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPartiesWithParms(@QueryParam(value = "term") final String partyName,
                                           @QueryParam(value = "requestedColumns") final String requestedColumns,
                                           @QueryParam(value = "contraparty")    final String contraparty,
                                           @QueryParam(value = "frequentlyUsed")   final String frequentlyUsed ) {


        //BAD REQUEST
        if(requestedColumns == null) {
         return Response.status(Response.Status.BAD_REQUEST).entity("Requested Columns invalid" ).build();
        }


        //Defensive check if nothing was passed on the search call
        //must specify at least one parameter return BAD_REQUEST
        if(partyName == null && contraparty == null && frequentlyUsed == null ) {
            logger.warn("No selection criteria sent on the search call , return error");
            return Response.status(Response.Status.BAD_REQUEST).entity("No selection criteria sent on the search call , return error").build();

        }


        try {
            //Refactor to pass the fully intialized persister in to class on the ctor instead of static reference
            List<Map<String,Object>> parties = PartyServiceIOCTRL.persister.getParties(partyName,contraparty,frequentlyUsed);
            if(parties.size() > 0 ) {

                ArrayList<ArrayList> response = buildRequestedQueryResult(parties,requestedColumns);


                String jsonRetValue = mapper.writeValueAsString(response);
                return Response.ok(jsonRetValue, MediaType.APPLICATION_JSON).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("Party not found for name: " + partyName).build();
            }

        } catch (Exception e) {
            logger.error("Exception Occured -",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
        }





    }

    /**
     * Search CounterParties by parms
     *  of one or more parties
     */
    @GET
    @Path("queryWithHeaders")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPartiesAndHeaders(@QueryParam(value = "term") final String partyName,
                                           @QueryParam(value = "contraparty")    final String contraparty,
                                           @QueryParam(value = "frequentlyUsed")   final String frequentlyUsed ) {




        //Defensive check if nothing was passed on the search call
        //must specify at least one parameter return BAD_REQUEST
        if(partyName == null && contraparty == null && frequentlyUsed == null ) {
            logger.warn("No selection criteria sent on the search call , return error");
            return Response.status(Response.Status.BAD_REQUEST).entity("No selection criteria sent on the search call , return error").build();

        }

        try {

            HashMap queryResults = new HashMap();

            String requestedColumns = System.getProperty(PARTY_QUERY_RESULT_COLUMN_HEADERS);
            List headerList = Arrays.asList(requestedColumns.split(","));

            queryResults.put("headers",headerList);
            queryResults.put("results",new ArrayList());

            List<Map<String,Object>> parties = PartyServiceIOCTRL.persister.getParties(partyName,contraparty,frequentlyUsed);
            if(parties.size() > 0 ) {
                ArrayList<ArrayList> results = buildRequestedQueryResult(parties,requestedColumns);
                queryResults.put("results",results);

                //String jsonRetValue = mapper.writeValueAsString(queryResults);
                //return Response.ok(jsonRetValue, MediaType.APPLICATION_JSON).build();

            }

            String jsonRetValue = mapper.writeValueAsString(queryResults);
            return Response.ok(jsonRetValue, MediaType.APPLICATION_JSON).build();


        } catch (Exception e) {
            logger.error("Exception Occured -",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
        }


    }


    /****
     * Select the request columns and send it back return the listy of list
     * @param parties
     * @param requesstedColumns
     * @return
     */
    private ArrayList<ArrayList> buildRequestedQueryResult(List<Map<String,Object>> parties , String requesstedColumns ){

        ArrayList<ArrayList> response = new ArrayList<ArrayList>();

        String [] selectedPartyAttributes = requesstedColumns.split(",");
        ListIterator partyListIterator =  parties.listIterator();

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

    /***
     * Return the party cache size
     * @return
     */
    @GET
    @Path("cachesize")
    @Produces(MediaType.APPLICATION_JSON)
    public Response numberOfPartiesInTheCache()  {


        try {
            //Refactor to pass the fully intialized persister in to class on the ctor instead of static reference
            String numberOfPartiesInTheCache = PartyServiceIOCTRL.persister.getPartyCount();
            return Response.ok(numberOfPartiesInTheCache, MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            logger.error("Exception Occured -",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
        }


    }







}
