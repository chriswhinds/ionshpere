package com.droitfintech.defaultingservice;


import com.droitfintech.adeptapi.DictionaryService;
import com.droitfintech.defaultingservice.functions.BadArgumentException;
import com.droitfintech.defaultingservice.functions.ClassConverter;
import com.droitfintech.defaultingservice.functions.Tenor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * The REST handler for this service
 */
@Path("/defaults")
public class DefaultingServiceRequestEndPoint {


    private static Logger logger = LoggerFactory.getLogger(DefaultingServiceRequestEndPoint.class);

    private ObjectMapper mapper = new ObjectMapper();

    //private DictionaryService dictionaryService = new DictionaryService();


    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


    private DateTimeFormatter dateOnlyFormatter = DateTimeFormat.forPattern("dd-MMM-yyyy");


    // Formatter for executionDate: e.g. 09-Apr-2014 16:21:32
    DateTimeFormatter executionDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    // Formatter for effective date: e.g. 17-Sep-2014
    DateTimeFormatter effectiveDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");



    /**
     *  Return the defaulted set of attributes from the request
     *  @return a json a json stream
     */
    @POST
    @Path("request")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolveTradeRequest(String requestBody) {
        try {

            Map unresolvedRequest = mapper.readValue(requestBody.getBytes(),HashMap.class);

            Map resulvedMap = DefaultingDataServiceIOCTRL.getAttributeResolver().buildResolvedTradeRequest(unresolvedRequest);

            //Map resulvedMap = buildResolvedWTLTradeRequest(unresolvedRequest);

            String retValue = mapper.writeValueAsString(resulvedMap);

            return Response.ok(retValue, MediaType.APPLICATION_JSON).build();


        } catch (Exception e) {
            logger.error("Exception Occured:",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
        }


    }

    /**
     *  Refresh the service MetaData
     *  @return a json a json stream
     */
    @POST
    @Path("refresh")
    @Produces(MediaType.APPLICATION_JSON)
    public Response refresh() {

        try {

            //Reload the cache
            DefaultingDataServiceIOCTRL.LoadRequestTaxonomyMap();

            return Response.ok("refresh completed successfully", MediaType.APPLICATION_JSON).build();


        } catch (Exception e) {
            logger.error("Exception Occured:",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
        }


    }

    /***
     *
     * @return
     */
    private Map buildResolvedWTLTradeRequest(Map request)  throws BadArgumentException {

        //Dump the Inbound
        if(logger.isDebugEnabled()){
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                logger.debug("Inbound Raw Request attributes -> {}",mapper.writeValueAsString(request));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        HashMap resolvedTradeRequest = new HashMap();

        String productType =  (String)request.get("productTypeDropDown");

        String assetClass =   (String)request.get("assetClass") ;
        String baseProduct =  (String)request.get("product") ;
        String subProduct  = (String)request.get("subProduct");




        Map tradetoxonymyMap = DefaultingDataServiceIOCTRL.findTradeTaxonymyMap(productType,assetClass,baseProduct,subProduct);
        if(tradetoxonymyMap == null){
            throw new BadArgumentException("No Trade request Mapping Found for Taxonomy points ->  Product Type [" + productType  +"]  AssetClass [" + assetClass+ "] Product [" + baseProduct + "] SubProduct [" + subProduct + "]");
        }

        String maturity    = (String)request.get("maturity");

        //Fetch the defaults record for Rates and Credit , will have to refactor for Credit
        CSVRecord defaults = null;

        //Find the rates defaults entry
        if(assetClass.equals("InterestRate")) {
            String currency   = (String)request.get("currency");
            String indexName  = (String)request.get("floatIndex");

            defaults = DefaultingDataServiceIOCTRL.productMasterUtils.getIrDefaultsByCurrencyIndex(assetClass,baseProduct,subProduct,currency,indexName);
        }

        //Find the Credit defaults
        if(assetClass.equals("Credit")) {
            String index   = (String)request.get("index");
            String series       = (String)request.get("series");

            //Trt resolve if you have a indexlabe and a series
            if(index!=null && series !=null )
                defaults = DefaultingDataServiceIOCTRL.productMasterUtils.getCreditIndexDefaultsByIndexSeriesTerm(index, new Integer(series) )  ;
        }



        Set<String> sectionKeys = tradetoxonymyMap.keySet();

        for(String sectionKey : sectionKeys) {

            if(sectionKey.equals("trade-taxonomy")) continue;  //BYPass the index name

            if(logger.isDebugEnabled())
                logger.debug("Processing Map Section [ {} ]",sectionKey);

            //Fetch the three sections by key
            ArrayList tradeProducCommomAttributes = (ArrayList)tradetoxonymyMap.get(sectionKey);
            processAttributes(resolvedTradeRequest,request,tradeProducCommomAttributes,defaults,maturity);



        }
        //Dump the output
        if(logger.isDebugEnabled()){
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                logger.debug("Outbound Resolved Request attributes -> {}",mapper.writeValueAsString(resolvedTradeRequest));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        }
        return resolvedTradeRequest;


    }

    /***
     * fetch the last value form a ISDA product specification adjust when nessary
     * @return
     *   /*
    if(retValue.equals("interestrate"))
    return "InterestRate";

    if(retValue.equals("irswap"))
    return "IRSwap";


    if(retValue.equals("crosscurrency"))
    return "CrossCurrency";


    if(retValue.equals("fixedfloat"))
    return "FixedFloat";
     */

    private String extractLastProductPartValue(String isdaProductPart) {
        if ( isdaProductPart != null  ){
            String [] productParts = isdaProductPart.split("\\.");

            String retValue = productParts[productParts.length - 1];

            return DefaultingDataServiceIOCTRL.getTaxonomyAdjusted(retValue);

        }
        return null;
    }

    /***
     * Precess a set of product attributes resolving the values from the inbound map from the UI ,  explict setDaultValues of Industry defauls
     * @param resolvedMap
     * @param guiUnresolvedMap
     * @param attributeDictionaryList
     */
    private void processAttributes(Map resolvedMap, Map guiUnresolvedMap , ArrayList attributeDictionaryList , CSVRecord defaults,String maturityTenor){


        ListIterator<Map> attributes = attributeDictionaryList.listIterator();
        while(attributes.hasNext()){

            Map productAttribute = attributes.next();
            String productAttributeName = (String)productAttribute.get("product-attribute");
            String applyFunction         = "NotAvailable";
            applyFunction                = (String)productAttribute.get("applyfunction");

            logger.debug("Processing Prod Attribute: [ {} ] with Function [ {} ]",productAttributeName,applyFunction);

            //We may not find and entry in the trade attribute map as it may be transiant
            String type = "NotAvailable";
            String cardinality  = "NotAvailable";
            Map tradeAttributeDictionaryEntry = (Map)DefaultingDataServiceIOCTRL.dictionaryService.getTradeAttributeDictionary().get(productAttributeName);

            if(tradeAttributeDictionaryEntry!=null){
                type = (String)tradeAttributeDictionaryEntry.get("type");
                cardinality = (String)tradeAttributeDictionaryEntry.get("cardinality");
            }

            //Pick the setToValue first
            if(productAttribute.containsKey("setToValue") ) {
                Object attributeObject  = productAttribute.get("setToValue");
                Object adjustedValue = adjustSetValue(attributeObject,type,cardinality,applyFunction,maturityTenor);
                if(logger.isDebugEnabled())
                    logger.debug("----> Set to Adjusted Assigned value  Attribute  [ {} ] set to  [ {} ]",productAttributeName,adjustedValue);
                resolvedMap.put(productAttributeName,adjustedValue);
                continue;
            }
            //Pick the GUI value if not null
            String guiValueKey = (String)productAttribute.get("gui-attribute");
            String guiValue = (String)guiUnresolvedMap.get(guiValueKey);
            if(guiValue!=null){

                Object adjustedValue = adjustValue(guiValue,type,cardinality,applyFunction,maturityTenor,guiUnresolvedMap);
                if(logger.isDebugEnabled())
                    logger.debug("----> Attribute Set to Adjusted GUI Value [ {} ] set to  [ {} ]",productAttributeName,adjustedValue);

                resolvedMap.put(productAttributeName,adjustedValue);
                continue;
            }

            //Check for INLINE Computed fields
            if(guiValueKey.equals("COMPUTE-INLINE")){

                Object adjustedValue = adjustValue(guiValueKey,type,cardinality,applyFunction,maturityTenor,guiUnresolvedMap);
                if(logger.isDebugEnabled())
                    logger.debug("----> Attribute Set to Computed Adjusted Value [ {} ] set to  [ {} ]",productAttributeName,adjustedValue);

                resolvedMap.put(productAttributeName,adjustedValue);
                continue;


            }

            if(defaults != null  && productAttribute.containsKey("defaultToStatndard") ) {

                //Pick the default value from the defaults record
                String defaultField = (String)productAttribute.get("defaultToStatndard");
                String defaultValue = defaults.get(defaultField);

                if(defaultValue!=null) {
                    Object adjustedValue = adjustValue(defaultValue,type,cardinality,applyFunction,maturityTenor,guiUnresolvedMap);
                    if(logger.isDebugEnabled())
                        logger.debug("----> Defaulted to Standard value  Attribute  [ {} ] set to  [ {} ]",productAttributeName,adjustedValue);
                    resolvedMap.put(productAttributeName,adjustedValue);
                    continue;
                }

            }
            //No Value Available Stuff "default value based on decitionary
            Object defauleValue = defualtFromType(type,cardinality);
            resolvedMap.put(productAttributeName,defauleValue);
            if(logger.isDebugEnabled())
                logger.debug("----> Defaulted to Product Attribute  [ {} ] set to  [ {} ]",productAttributeName,defauleValue);


        }


    }

    /***
     * Adjust Value if
     * otherwise just return actaual value
     * @param guiValue
     * @return
     */
    private Object adjustSetValue(Object guiValue , String type , String cardinality , String function,String maturityTenor){

        //Now Type Enumerated String  check cardinality and One or Many
        //Convert to Arraylist
        if(type.equals("EnumeratedString")) {

            if(cardinality.equals("One")) {
                //return just the string
                return guiValue;
            }

            if(cardinality.equals("Many")) {

                ArrayList retArrayValue = new ArrayList();
                retArrayValue.add(guiValue);
                return retArrayValue;

            }

        }

        if(type.equals("Boolean") || type.equals("String") || type.equals("TenorType")  || type.equals("CreditIndexNameType") ) {

            return guiValue;
        }

        if(type.equals("BigDecimal") ) {

            return guiValue;
        }

        //Raise not defaulted
        return "SetValueUnknown";

    }

    /***
     * Default based on
     * @param type
     * @param cardinality
     * @return
     */
    private Object defualtFromType(String type , String cardinality) {

        if (type.equals("Boolean")) {

            return false;
        }

        if (type.equals("String")) {

            return "NotApplicable";
        }

        if (type.equals("EnumeratedString")) {

            if (cardinality.equals("One")) {
                //return just the string
                return "NotApplicable";
            }

            if (cardinality.equals("Many")) {
                ArrayList retArrayValue = new ArrayList();
                return retArrayValue;
            }

        }

        if (type.equals("DateType")) {

            Date dateValue = new Date();
            return simpleDateFormat.format(dateValue);

        }

        if (type.equals("TenotType")) {

            return "1D";

        }

        if (type.equals("BigDecimal")) {

            return 1.0;

        }

        //Raise not defaulted
        return "DefaultToTypeUnknown";


    }

    /***
     * Adjust Value if Yes/No set the boolean true or false
     * otherwise just return actaual value
     * @param guiValue
     * @return
     */
    private Object adjustValue(String guiValue , String type , String cardinality , String function,String maturityTenor , Map guiUnresolvedMap){


        //Adjust computed fields
        if(guiValue.equals("COMPUTE-INLINE")) {


            if( function.equals("ComputeTermFromSubmissionDateToMaturity")    )  {
                Date submissionDateUI = executionDateFormatter.parseDateTime((String)guiUnresolvedMap.get("executionDate")).toDate();
                Date maturityDateUI = dateOnlyFormatter.parseDateTime((String)guiUnresolvedMap.get("maturityDateUI")).toDate();
                return calculateNumberOfDays(submissionDateUI, maturityDateUI);
            }

            if( function.equals("ComputeTermFromEffectiveDateToMaturity")    )  {

                //Date effectiveDateUI = effectiveDateFormatter.parseDateTime((String)guiUnresolvedMap.get("effectiveDateUI")).toDate();
                Date effectiveDateUI = dateOnlyFormatter.parseDateTime((String)guiUnresolvedMap.get("effectiveDateUI")).toDate();
                Date maturityDateUI = dateOnlyFormatter.parseDateTime((String)guiUnresolvedMap.get("maturityDateUI")).toDate();
                return calculateNumberOfDays(effectiveDateUI, maturityDateUI);
            }

            if( function.equals("ComputeTermFromSubmissionDateToSettlementDate")    )  {

                Date submissionDateUI = executionDateFormatter.parseDateTime((String)guiUnresolvedMap.get("executionDate")).toDate();
                Date maturityDateUI = dateOnlyFormatter.parseDateTime((String)guiUnresolvedMap.get("settlementDateUI")).toDate();
                return calculateNumberOfDays(submissionDateUI, maturityDateUI);

            }

            if( function.equals("ComputeTermFromExecutionDateToTerminationDate")    )  {

                Date submissionDateUI = executionDateFormatter.parseDateTime((String)guiUnresolvedMap.get("executionDate")).toDate();
                Date maturityDateUI = dateOnlyFormatter.parseDateTime((String)guiUnresolvedMap.get("terminationDateNoTerms")).toDate();
                return calculateNumberOfDays(submissionDateUI, maturityDateUI);

            }



            return "NoComputationMethodFound";

        }


        if(function.equals("UseCCYPairOneFunction") || function.equals("UseCCYPairOneFunction")) {
            String[] ccyTokens = guiValue.split("/", -1);
            String baseCurrency = ccyTokens[0];
            String counterCurrency = ccyTokens[1];

            if(function.equals("UseCCYPairOneFunction")){
                return baseCurrency;
            }
            if(function.equals("UseCCYPairTwoFunction")){
                return counterCurrency;
            }

        }

        if(function.equals("CounterPartyCurrencyPayerFunction") || function.equals("ContraPartyCurrencyPayerFunction")) {

            if (function.equals("CounterPartyCurrencyPayerFunction")) {

                if(guiValue.equals("Counterparty"))
                    return "Counterparty";
                else
                    return "Contraparty";
            }

            if (function.equals("ContraPartyCurrencyPayerFunction")) {
                if(guiValue.equals("Counterparty"))
                    return "Contraparty";
                else
                    return "Counterparty";
            }


        }

        //ADJUST ALL other fields

        //Simple conversion
        if("Yes".equals(guiValue))
            return true;

        if("No".equals(guiValue))
            return false;

        //Now Type Enumerated String  check cardinality and One or Many
        //Convert to Arraylist
        if(type.equals("EnumeratedString")) {

            if(cardinality.equals("One")) {
                //return just the string
                return guiValue;
            }

            if(cardinality.equals("Many")) {

                ArrayList retArrayValue = new ArrayList();
                String[] values = guiValue.split(",");
                for(String value:values) {

                    retArrayValue.add(value);
                }

                return retArrayValue;

            }

        }


        //If this is a Date field convert
        if(type.equals("DateType") &&  ( function != null &&  function.equals("DateFunction")    ) ) {
            return applyDateFuntion(guiValue);
        }

        //If this is a Date field convert
        if(type.equals("DateType") &&  ( function != null &&  function.equals("DateOnlyFunction")    ) ) {
            return applyDateOnlyFuntion(guiValue);
        }

        //If this is a Date field convert
        if(type.equals("DateType") &&    ( function != null &&  function.equals("TerminationDateFunction")    ) ) {
            return appyComputeTermination(guiValue,maturityTenor);
        }

        //If this is a BigDecimal field convert
        if(type.equals("BigDecimal") &&    ( function != null &&  function.equals("BigDecimalFunction")    ) ) {
            return applyBigDecimalFunction(guiValue);
        }



        return guiValue;

    }

    /****
     * Convert value to date to api service standard
     * @return
     */
    private Object applyDateFuntion( String datevalue){

        if(logger.isDebugEnabled())
            logger.debug("Date Fundtion apply transform on value-> [ {} ]", datevalue );

        return dateFormatter.print(new DateTime(ClassConverter.getConverter(Date.class).convert(datevalue)));


    }

    /****
     * Convert value to date to api service standard
     * @return
     */
    private Object applyDateOnlyFuntion( String datevalue){

        if(logger.isDebugEnabled())
            logger.debug("Date Only Fundtion apply transform on value-> [ {} ]", datevalue );

        return dateOnlyFormatter.print(new DateTime(ClassConverter.getConverter(Date.class).convert(datevalue)));


    }

    /****
     * Convert value to date to api service standard
     * @return
     */
    private Object applyBigDecimalFunction( String decimalvalue){

        if(logger.isDebugEnabled())
            logger.debug("BigDecimal Fundtion apply transform on value-> [ {} ]", decimalvalue );

        return new BigDecimal(decimalvalue);


    }

    // This method calculates number of days between 2 dates without including extra day from leap years
    // This is needed because when converting a tenor in years to days, it always uses 365 days
    private String calculateNumberOfDays(Date from, Date to) {
        GregorianCalendar start = new GregorianCalendar();
        start.setTime(from);
        GregorianCalendar end = new GregorianCalendar();
        end.setTime(to);
        int days = (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 365 +
                (end.get(Calendar.DAY_OF_YEAR) - start.get(Calendar.DAY_OF_YEAR));
        return days + "D";
    }


    /****
     * Convert Value to a Termination Date based on effectiveDate and Maturity
     * @return
     */
    private Object appyComputeTermination( String datevalue , String maturity){
        return dateFormatter.print(new DateTime(computeTerminationDate(ClassConverter.getConverter(Date.class).convert(datevalue), Tenor.makeTenor(maturity))));
    }

    /****
     * Return a Date for adjusted by tenor
     * @param effectiveDate
     * @param maturity
     * @return
     */
    private Date computeTerminationDate(Date effectiveDate, Tenor maturity) {

        Calendar c = Calendar.getInstance();
        c.setTime(effectiveDate);
        c.add(Calendar.DATE, maturity.getDays());
        return c.getTime();
    }


    /**
     *  Return the defaulted set of values for a given index and ccy
     *  @return a json a json stream
     */
    @POST
    @Path("irddefaults")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRatesDefaults(@QueryParam(value = "ccy") final String ccy,
                                     @QueryParam(value = "indexname")    final String indexname ) {

        try {

            //String defaultdedValues =  applyDefaultsFromAttributes(requestBody);
            return Response.ok("", MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            logger.error("Exception Occured:",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
        }


    }


    /**
     *  Return the trade attributes required for an API call by ISDA Product Taxonomy key
     *  @return a json a json stream
     */
    @POST
    @Path("requestattributes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRequestAttributes(@QueryParam(value = "isdaproduct") final String isdaproduct ) {


        try {




            //String defaultdedValues =  applyDefaultsFromAttributes(requestBody);
            return Response.ok("", MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            logger.error("Exception Occured:",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service Exception: " + e).build();
        }


    }





    /***
     *
     * @param attribute
     * @return
     */
    private Map resolveStrinAttribute(Map attribute){

        HashMap retValue = new HashMap();
        retValue.put(attribute.get("name"),"NotApplicable");
        return retValue;

    }

    /***
     * resolve Enumerated Strings by
     * @param attribute
     * @return
     */
    private Map resolveEnumeratedAttribute(Map attribute){

        HashMap retValue = new HashMap();
        String enumSearchKey = ((String)attribute.get("name")).toLowerCase();


        ArrayList enumsList = (ArrayList)DefaultingDataServiceIOCTRL.dictionaryService.getEnums().get(enumSearchKey);
        retValue.put(enumSearchKey,enumsList);
        return retValue;

    }

    /**
     *
     * @param attribute
     * @return
     */
    private Map resolveDateAttribute(Map attribute){

        //HashMap retValue = new HashMap();
        //retValue.put(attribute.get("name"), dataFormat.format(new Date()));
        //return retValue;

        return null;

    }

    /***
     *
     * @param attribute
     * @return
     */
    private Map resolveBooleanAttribute(Map attribute){

        HashMap retValue = new HashMap();
        retValue.put(attribute.get("name"),false);
        return retValue;
    }

    /***
     *
     * @param attribute
     * @return
     */
    private Map resolveTenorAttribute(Map attribute){

        HashMap retValue = new HashMap();
        retValue.put(attribute.get("name"),"1D");
        return retValue;

    }





}
