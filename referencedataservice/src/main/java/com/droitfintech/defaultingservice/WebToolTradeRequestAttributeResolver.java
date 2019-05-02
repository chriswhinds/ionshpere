package com.droitfintech.defaultingservice;

import com.droitfintech.defaultingservice.functions.BadArgumentException;
import com.droitfintech.defaultingservice.functions.ClassConverter;
import com.droitfintech.defaultingservice.functions.Tenor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Trade attribute resolver for Droit Webtool lite.
 * Created by christopherwhinds on 10/10/17.
 */
public class WebToolTradeRequestAttributeResolver implements RequestAttributeResolver {


    private static Logger logger = LoggerFactory.getLogger(WebToolTradeRequestAttributeResolver.class);

    private ObjectMapper mapper = new ObjectMapper();

    // Formatter for executionDate: e.g. 09-Apr-2014 16:21:32
    DateTimeFormatter executionDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    // Formatter for effective date: e.g. 17-Sep-2014
    DateTimeFormatter effectiveDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


    private DateTimeFormatter dateOnlyFormatter = DateTimeFormat.forPattern("dd-MMM-yyyy");


    @Override
    public Map buildResolvedTradeRequest(Map request) throws BadArgumentException {

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


        String assetClassTest  = (String)request.get("assetClass") ;
        String baseProductTest = (String)request.get("product") ;
        String subProductTest  = (String)request.get("subProduct");

        String assetClass      = (assetClassTest!=null) ? assetClassTest : "NotAssigned";
        String baseProduct     = (assetClassTest!=null) ? baseProductTest : "NotAssigned";
        String subProduct      =  (subProductTest!=null) ? subProductTest : "NotAssigned";



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
            if(indexName == null)  //For basis try floatindex1
                indexName  = (String)request.get("floatIndex1");

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
            } //else {
              //  logger.debug("==== >>> IGNORING Product  Attribute: [ {} ] no mapping in the API dicionary",productAttributeName);
              //  continue;
            //}

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

                //If signaled exclude this value from the results
                if(  adjustedValue!=null && !adjustedValue.equals("EXCLUDE-FROM_RESULTS"))
                     resolvedMap.put(productAttributeName,adjustedValue);
                else
                    logger.debug("==============> Attribute NOT SET was dropped for Computed Adjusted Value [ {} ] set to  [ {} ]",productAttributeName,adjustedValue);



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


        if (type.equals("Integer")) {

            return 1;

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




        //Set Function to known Value to not trip NullPointer exception
        if(function == null)
            function="NOOP";

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

            if( function.equals("FixedLegRollConventionFunction") ||  function.equals("RollConventionAllFunction")  )  {

                String subProduct = (String)guiUnresolvedMap.get("subProduct");
                String fixedRollConvention = (String)guiUnresolvedMap.get("fixedRollConvention");
                String fixedPayFrequency = (String)guiUnresolvedMap.get("fixedPayFrequency");
                if("OIS".equals(subProduct)) {
                    if(fixedPayFrequency.equals("1T")) //ZERO COUPON
                       return "NONE";
                }

                //Not OIS just return the roll Convention  as is or not Zero Coupon
                return fixedRollConvention;

            }


            if( function.equals("FloatLegRollConventionFunction")  )  {

                String subProduct = (String)guiUnresolvedMap.get("subProduct");
                String floatRollConvention = (String)guiUnresolvedMap.get("floatRollConvention");
                String floatPayFrequency = (String)guiUnresolvedMap.get("floatPayFrequency");
                if("OIS".equals(subProduct)) {
                    if(floatPayFrequency.equals("1T")) //ZERO COUPON
                        return "NONE";
                }

                //Not OIS just return the roll Convention  as is or not Zero Coupon
                return floatRollConvention;

            }

            if( function.equals("CompondingNoneFunction")  )  {

                String compounding = (String)guiUnresolvedMap.get("compounding");


                if( compounding !=null &&  compounding.equalsIgnoreCase("None"))   // If none was sent just signal exclusion
                    return "EXCLUDE-FROM_RESULTS";

                return compounding;


            }


            if( function.equals("CompondingOneNoneFunction")  )  {

                String compounding = (String)guiUnresolvedMap.get("compounding1");


                if( compounding !=null &&  compounding.equalsIgnoreCase("None"))   // If none was sent just signal exclusion
                    return "EXCLUDE-FROM_RESULTS";

                return compounding;


            }

            if( function.equals("CompondingTwoNoneFunction")  )  {

                String compounding = (String)guiUnresolvedMap.get("compounding2");


                if( compounding !=null &&  compounding.equalsIgnoreCase("None"))   // If none was sent just signal exclusion
                    return "EXCLUDE-FROM_RESULTS";

                return compounding;


            }

            if( function.equals("BasisPaymentFrequencyLegOneFunction") || function.equals("BasisPaymentFrequencyLegTwoFunction") )  {

                String ccy = (String)guiUnresolvedMap.get("currency");
                String index1 = (String)guiUnresolvedMap.get("floatIndex1");
                String index2 = (String)guiUnresolvedMap.get("floatIndex2");
                String indextenor1 = (String)guiUnresolvedMap.get("indexTenor1");
                String indextenor2 = (String)guiUnresolvedMap.get("indexTenor2");
                if(ccy.equals("AUD") && index1.equals("AUD-BBR-BBSW") && index2.equals("AUD-AONIA-OIS-COMPOUND"))
                    return indextenor1;
                if(ccy.equals("AUD") && index1.equals("AUD-AONIA-OIS-COMPOUND") && index2.equals("AUD-BBR-BBSW"))
                    return indextenor2;

                if(ccy.equals("EUR") && index1.equals("EUR-EURIBOR-Reuters") && index2.equals("EUR-EONIA-OIS-COMPOUND"))
                    return indextenor1;
                if(ccy.equals("EUR") && index1.equals("EUR-EONIA-OIS-COMPOUND") && index2.equals("EUR-EURIBOR-Reuters"))
                    return indextenor2;

                if(ccy.equals("EUR") && index1.equals("EUR-EURIBOR-Telerate") && index2.equals("EUR-EONIA-OIS-COMPOUND"))
                    return indextenor1;
                if(ccy.equals("EUR") && index1.equals("EUR-EONIA-OIS-COMPOUND") && index2.equals("EUR-EURIBOR-Telerate"))
                    return indextenor2;

                if(ccy.equals("GPB") && index1.equals("GBP-LIBOR-BBA") && index2.equals("GBP-WMBA-SONIA-COMPOUND"))
                    return indextenor1;
                if(ccy.equals("GPB") && index1.equals("GBP-WMBA-SONIA-COMPOUND") && index2.equals("GBP-LIBOR-BBA"))
                    return indextenor2;

                if(ccy.equals("USD") && index1.equals("USD-LIBOR-BBA") && index2.equals("USD-Federal Funds-H.15-OIS-COMPOUND"))
                    return indextenor1;
                if(ccy.equals("USD") && index1.equals("USD-Federal Funds-H.15-OIS-COMPOUND") && index2.equals("USD-LIBOR-BBA"))
                    return indextenor2;

                //None of the above so just return the value as is
                if(function.equals("BasisPaymentFrequencyLegOneFunction"))
                    return indextenor1;

                if(function.equals("BasisPaymentFrequencyLegTwoFunction"))
                    return indextenor2;




            }



            return "NoComputationMethodFound";

        }


        if(  function.equals("UseCCYPairOneFunction") || function.equals("UseCCYPairTwoFunction")) {
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

        if(function.equals("CheckBoxToBooleanFunction") ) {

            if("Yes".equals(guiValue))
                return true;
            else
                return false;

        }



            //ADJUST ALL other fields

        //Simple conversion  "Yes" ot "Y"
        if("Yes".equals(guiValue)  || "Y".equals(guiValue)  )
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

                    retArrayValue.add(value.trim());
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

        //If this is a BigDecimal field convert
        if(type.equals("Integer") &&    ( function != null &&  function.equals("IntegerFunction")    ) ) {
            return applyIntegerFunction(guiValue);
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
     * Convert value to BigDecimal to api service standard
     * @return
     */
    private Object applyBigDecimalFunction( String decimalvalue){

        if(logger.isDebugEnabled())
            logger.debug("BigDecimal Fundtion apply transform on value-> [ {} ]", decimalvalue );

        return new BigDecimal(decimalvalue);


    }

    /****
     * Convert value to date to api service standard
     * @return
     */
    private Object applyIntegerFunction( String integerValue){

        if(logger.isDebugEnabled())
            logger.debug("Integer Fundtion apply transform on value-> [ {} ]", integerValue );

        return new Integer(integerValue);


    }


    /***
     * This method calculates number of days between 2 dates without including extra day from leap years
     * This is needed because when converting a tenor in years to days, it always uses 365 days
     *
     * @param from
     * @param to
     * @return
     */
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










}
