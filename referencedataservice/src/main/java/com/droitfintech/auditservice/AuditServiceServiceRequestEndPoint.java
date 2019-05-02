package com.droitfintech.auditservice;


import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The REST handler for this service
 */
@Path("/audit")
public class AuditServiceServiceRequestEndPoint {


    private static Logger logger = LoggerFactory.getLogger(AuditServiceServiceRequestEndPoint.class);

    private ObjectMapper mapper = new ObjectMapper();



    /**
     * Store a Audit record to the database
     */
    @POST
    @Path("store")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response storeAuditRecord(String decisionJson) {

        String decisionId = "";
        try {

            Map decisionToSave = mapper.readValue(decisionJson, HashMap.class);
            if(logger.isDebugEnabled()){
                String queryJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(decisionToSave);
                logger.debug("About to save decsions:-> {}",queryJson);

            }

            decisionId = (String)decisionToSave.get("decisionId");
            AuditServiceIOCTRL.auditStoreQueryService.saveDecision(decisionToSave) ;

            return Response.status(Response.Status.OK).entity("Decision saved successfully [ " + decisionId + " ] ").build();

        } catch (Throwable e) {
            logger.error("Exception Occured while storing id [" + decisionId + "]->>",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception while storing id [" + decisionId + "]->>" + e).build();
        }


    }

    /**
     * Search CounterParties by parms
     *  of one or more parties
     *   url: 'cptyAutocomplete.do?term='+ encodeURIComponent($("#counterpartyName").val()) + "&partyIdSearch=" + $("#partyIdSearchDropDown").val() + "&frequentlyUsed=" + creditlineVal,
     *     private String      decisionId;
     *     DateTime    decisionDateStart = null;
     *      DateTime    decisionDateEnd = null;
     *      DateTime    submissionDateStart = null;
     *      DateTime    submissionDateEnd = null;
     *      Integer     pageNumber = 0;
     *      Integer     pageSize = 10;
     *      String		 externalTradeID;
     *      String	 	 groupID;
     *      String 	   assetClass;
     *      String 	 product;
     *      String 	 subProduct;
     *      String 	 contrapartyID;
     *      String 	 counterpartyID;
     *      String 	 trader;
     *      String 	 salesPerson;
     *      String		 userRole;
     *      String		 userID;
     *      String		 user;
     *      String		 overriden;
     *      String		 allowedToTrade;
     *      boolean	canSeeAllTrades;
     */
    @GET
    @Path("query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchAuditRecords(@QueryParam(value = "decisionId") final String decisionId,
                                       @QueryParam(value = "decisionDateStart") final String decisionDateStart,
                                       @QueryParam(value = "decisionDateEnd") final String decisionDateEnd,
                                       @QueryParam(value = "submissionDateStart") final String submissionDateStart,
                                       @QueryParam(value = "submissionDateEnd") final String submissionDateEnd,
                                       @QueryParam(value = "pageNumber") final String pageNumber,
                                       @QueryParam(value = "pageSize") final String pageSize,
                                       @QueryParam(value = "externalTradeID") final String externalTradeID,
                                       @QueryParam(value = "groupID") final String groupID,
                                       @QueryParam(value = "assetClass") final String assetClass,
                                       @QueryParam(value = "product") final String product,
                                       @QueryParam(value = "subProduct") final String subProduct,
                                       @QueryParam(value = "contrapartyID") final String contrapartyID,
                                       @QueryParam(value = "counterpartyID") final String counterpartyID,
                                       @QueryParam(value = "trader") final String trader,
                                       @QueryParam(value = "salesPerson") final String salesPerson,
                                       @QueryParam(value = "user") final String user,
                                       @QueryParam(value = "overriden") final String overriden,
                                       @QueryParam(value = "allowedToTrade") final String allowedToTrade
                                       ) {

            try {

                    StringBuilder queryParms = new StringBuilder();

                    queryParms.append("decisionId=").append(decisionId);
                    queryParms.append(" ,decisionDateStart=").append(decisionDateStart);
                    queryParms.append(" ,decisionDateEnd=").append(decisionDateEnd);
                    queryParms.append(" ,submissionDateStart=").append(submissionDateStart);
                    queryParms.append(" ,submissionDateEnd=").append(submissionDateEnd);
                    queryParms.append(" ,externalTradeID=").append(externalTradeID);
                    queryParms.append(" ,groupID=").append(groupID);
                    queryParms.append(" ,assetClass=").append(assetClass);
                    queryParms.append(" ,product=").append(product);
                    queryParms.append(" ,subProduct=").append(decisionId);
                    queryParms.append(" ,contrapartyID=").append(contrapartyID);
                    queryParms.append(" ,counterpartyID=").append(counterpartyID);
                    queryParms.append(" ,trader=").append(trader);
                    queryParms.append(" ,salesPerson=").append(salesPerson);
                    queryParms.append(" ,user=").append(decisionId);
                    queryParms.append(" ,overriden=").append(overriden);
                    queryParms.append(" ,allowedToTrade=").append(allowedToTrade);

                    logger.debug("================================================================================================================================= ");
                    logger.debug("RDS Recieved Audit Record  Query Parms-> [ {} ]",queryParms.toString());
                    logger.debug("================================================================================================================================= ");

                    List<Map<String,Object>> searchResults = null;

                    //If Decision ID is provided go direect
                    if( decisionId != null && !decisionId.isEmpty()) {

                        searchResults = AuditServiceIOCTRL.auditStoreQueryService.getDecision(decisionId);

                    } else {

                        searchResults = AuditServiceIOCTRL.auditStoreQueryService.getDecisions(decisionDateStart,
                                                                                          decisionDateEnd,
                                                                                          submissionDateStart,
                                                                                          submissionDateEnd,
                                                                                          externalTradeID,
                                                                                          assetClass,
                                                                                          product,
                                                                                          subProduct,
                                                                                          contrapartyID,
                                                                                          counterpartyID,
                                                                                          trader,
                                                                                          salesPerson,
                                                                                          user,
                                                                                          overriden,
                                                                                          allowedToTrade);

                    }

                    if(searchResults != null) {
                        String jsonRetValue = mapper.writeValueAsString(searchResults);
                        return Response.ok(jsonRetValue, MediaType.APPLICATION_JSON).build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND).entity("No records found: ").build();
                    }


            } catch (Exception e) {
                logger.error("Exception Occured -",e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
            }

    }





}
