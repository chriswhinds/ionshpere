package com.droitfintech.defaultingservice;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.Reader;
import java.util.*;

import static com.droitfintech.bootstrap.AbstractReferenceDataService.SERVICE_HOME;


public class ProductMasterUtils {
	
	private final static Logger logger = LoggerFactory.getLogger(ProductMasterUtils.class);
	private Map<String, Object> productTaxonomyMap = new HashMap<String, Object>();



    private Map<Integer, String> ccpMap = new TreeMap<Integer, String>();

    private Properties properties;



    private static String PRODUCT_MASTER_TABLE = "/defaults/ProductMasterTable.csv";


    private static String PRODUCT_MASTER_ID                = "IDPRODUCTMASTER";
    private static String PRODUCT_MASTER_ASSESTCLASS       = "ASSETCLASS";
    private static String PRODUCT_MASTER_BASEPRODUCT       = "BASEPRODUCT";
    private static String PRODUCT_MASTER_SUBPRODUCT        = "SUBPRODUCT";
    private static String PRODUCT_MASTER_ECONOMICSSUMMARY  = "ECONOMICSSUMMARY";


    private static String PRODUCT_MASTER_EXTENSIONS_TABLE = "/defaults/ProductMasterExtensionsTable.csv";
    private static String PRODUCT_MASTER_EXTENSIONS_ID                = "IDPRODUCTMASTEREXTENSION";
    private static String PRODUCT_MASTER_EXTENSIONS_PRODUCT_MASTER_ID = "IDPRODUCTMASTER";
    private static String PRODUCT_MASTER_EXTENSIONS_TRANSACTIONTYPE   = "TRANSACTIONTYPE";
    private static String PRODUCT_MASTER_EXTENSIONS_SETTLEMENTTYPE    = "SETTLEMENTTYPE";


    private static String FIM_MARKETS_MASTER_TABLE = "/defaults/FinMktInfraTable.csv";
    private static String FIN_MARKETS_ID                = "IDFINMKTINFRA";
    private static String FIN_MARKETS_TYPE              = "FINMKTINFRATYPE";
    private static String FIN_MARKETS_SHORTNAME         = "SHORTNAME";
    private static String FIN_MARKETS_LONGNAME          = "LONGNAME";
    private static String FIN_MARKETS_COUNTRY           = "COUNTRY";
    private static String FIN_MARKETS_REGION            = "REGION";



    private static String CREDIT_INDEX_MASTER_TABLE =    "/defaults/DefaultCreditIndexTable.csv";
    private static String CREDIT_INDEX_ID                   = "IDDEFAULTSCREDITINDEX";
    private static String CREDIT_INDEX_INDEXLABEL           = "INDEXLABEL";
    private static String CREDIT_INDEX_SERIES               = "SERIES";
    private static String CREDIT_INDEX_BASEPRODUCT          = "BASEPRODUCT";
    private static String CREDIT_INDEX_SUBPRODUCT           = "SUBPRODUCT";
    private static String CREDIT_INDEX_TRANSACTIONTYPE      = "TRANSACTIONTYPE";
    private static String CREDIT_INDEX_FAMILY               = "FAMILY";
    private static String CREDIT_INDEX_ONTHERUN             = "ONTHERUN";


    private static String INTEREST_RATES_MASTER_TABLE =  "/defaults/DefaultInterestRatesTable.csv";
    private static String INTEREST_RATES_ID                  = "IDDEFAULTSINTERESTRATE";
    private static String INTEREST_RATES_PRODUCT_MASTER_ID   = "IDPRODUCTMASTER";
    private static String INTEREST_RATES_CURRENCY            = "CURRENCY";
    private static String INTEREST_RATES_FLOATINDEX          = "FLOATINDEX";
    private static String INTEREST_RATES_INDEXTENOR          = "INDEXTENOR";
    private static String INTEREST_RATES_FLOATPAYFREQUENCY   = "FLOATPAYFREQUENCY";
    private static String INTEREST_RATES_FIXEDPAYFREQUENCY   = "FIXEDPAYFREQUENCY";
    private static String INTEREST_RATES_FLOATDAYCOUNT       = "FLOATDAYCOUNT";
    private static String INTEREST_RATES_FIXEDDAYCOUNT       = "FIXEDDAYCOUNT";
    private static String INTEREST_RATES_BUSINESSDAYCONVENTION  = "BUSINESSDAYCONVENTION";
    private static String INTEREST_RATES_HOLIDAYCALENDARS       = "HOLIDAYCALENDARS";
    private static String INTEREST_RATES_MATURITY            = "MATURITY";
    private static String INTEREST_RATES_NOTIONAL            = "NOTIONAL";
    private static String INTEREST_RATES_FIXEDCOUPON         = "FIXEDCOUPON";
    private static String INTEREST_RATES_FIXINGHOLIDAYCALENDARS = "FIXINGHOLIDAYCALENDERS";




    private ObjectMapper mapper = new ObjectMapper();

    private ArrayList<CSVRecord> productMasterTable = new ArrayList<CSVRecord>();

    private ArrayList<CSVRecord> productMasterExtensionsTable = new ArrayList<CSVRecord>();

    private ArrayList<CSVRecord> finMarketsMasterTable = new ArrayList<CSVRecord>();


    private ArrayList<CSVRecord> defaultsCreditIndicesTable = new ArrayList<CSVRecord>();

    private ArrayList<CSVRecord> defaultsIntrestRatesTable = new ArrayList<CSVRecord>();


	
	@PostConstruct
	public void init() throws Exception {

        loadProductMaster();
        loadProductMasterExtensions();
        loadCCPMaster();
        loadDefaultCreditIndexMaster();
        loadDefaultInterestRatesMaster();

	}

    /****
     * Load the Product Master
     * @throws Exception
     */
    private void loadProductMaster() throws Exception {

        String productMasterFileLocation =    System.getProperty(SERVICE_HOME) + PRODUCT_MASTER_TABLE;
        Reader in = new FileReader(productMasterFileLocation);
        CSVParser productMasterRecords = new CSVParser(in, CSVFormat.EXCEL.withHeader());


        for (CSVRecord productMasterRecord : productMasterRecords) {



            productMasterTable.add(productMasterRecord);

            insert(productTaxonomyMap,productMasterRecord);
        }

    }



    /****
     * Load the Product Master
     * @throws Exception
     */
    private void loadProductMasterExtensions() throws Exception {


        String csvFileLocation = System.getProperty(SERVICE_HOME) + PRODUCT_MASTER_EXTENSIONS_TABLE;
        Reader in = new FileReader(csvFileLocation);

        CSVParser productMasterRecords = new CSVParser(in, CSVFormat.EXCEL.withHeader());

        for (CSVRecord productMasterExtensionRecord : productMasterRecords) {
            //Check if reached end of file
            String id = productMasterExtensionRecord.get(PRODUCT_MASTER_EXTENSIONS_ID);
            if("".equalsIgnoreCase(id))
                break;



            productMasterExtensionsTable.add(productMasterExtensionRecord);

        }

    }



    /****
     * Load the CCP master and costruct the CCP Map For the GUI Services
     * @throws Exception
     */
    private void loadCCPMaster() throws Exception {

        String csvFileLocation = System.getProperty(SERVICE_HOME) + FIM_MARKETS_MASTER_TABLE;
        Reader in = new FileReader(csvFileLocation);


        CSVParser finMarketsMasterRecords = new CSVParser(in, CSVFormat.EXCEL.withHeader());

        for (CSVRecord finMarketMasterRecord : finMarketsMasterRecords) {


            finMarketsMasterTable.add(finMarketMasterRecord);

            String type = (String) finMarketMasterRecord.get(FIN_MARKETS_TYPE);
            if("CCP".equalsIgnoreCase(type)){
                ccpMap.put(Integer.parseInt(finMarketMasterRecord.get(FIN_MARKETS_ID)),finMarketMasterRecord.get(FIN_MARKETS_SHORTNAME));
            }
        }


    }

    /***
     * Load th
     */
    private void loadDefaultCreditIndexMaster() throws Exception {


        String csvFileLocation = System.getProperty(SERVICE_HOME) + CREDIT_INDEX_MASTER_TABLE;
        Reader in = new FileReader(csvFileLocation);

        CSVParser creditIndexMasterRecords = new CSVParser(in, CSVFormat.EXCEL.withHeader());

        for (CSVRecord creditIndexMasterRecord : creditIndexMasterRecords) {



            defaultsCreditIndicesTable.add(creditIndexMasterRecord);
        }


    }

    /***
     * Load th
     */
    private void loadDefaultInterestRatesMaster() throws Exception {


        String csvFileLocation = System.getProperty(SERVICE_HOME) + INTEREST_RATES_MASTER_TABLE;
        Reader in = new FileReader(csvFileLocation);


        CSVParser interestRatesMasterRecords = new CSVParser(in, CSVFormat.EXCEL.withHeader());

        for (CSVRecord interestRatesMasterRecord : interestRatesMasterRecords) {


            defaultsIntrestRatesTable.add(interestRatesMasterRecord);
        }


    }

    protected void insert(Map<String, Object> productTaxonomy, CSVRecord productMasterRecord) {



        Map<String, Object> baseProductMap = (Map<String, Object>) productTaxonomy.get( productMasterRecord.get(PRODUCT_MASTER_ASSESTCLASS) );
        if (baseProductMap == null) baseProductMap = new HashMap<String, Object>();

        Map<String, Object> subProductMap = (Map<String, Object>) baseProductMap.get(   productMasterRecord.get(PRODUCT_MASTER_BASEPRODUCT));
        if (subProductMap == null) subProductMap = new HashMap<String, Object>();

        subProductMap.put(  productMasterRecord.get(PRODUCT_MASTER_SUBPRODUCT), productMasterRecord.get(PRODUCT_MASTER_SUBPRODUCT)); 	// Includes {null:null}
        baseProductMap.put(  productMasterRecord.get(PRODUCT_MASTER_BASEPRODUCT), subProductMap);
        productTaxonomy.put(  productMasterRecord.get(PRODUCT_MASTER_ASSESTCLASS) , baseProductMap);
    }



    /****
     * Get the CCP master list
     * @return
     */
    public Map<Integer, String> getCcpMap() {
        return ccpMap;
    }

    public Map<String, Object> getProductTaxonomyMap() {
    	return productTaxonomyMap;
    }
        
    /**
     * description : Generalized method to fetch properties depending upon the requested
     * key
     * 
     * @param option
     * @return
     */
    public Map<String, String> getOptions(String option)
    {

        //Quick Fix to remove extra " from the option string
        option = option.replace('"',' ').trim();

        Map<String, String> map = new TreeMap<String, String>();
        StringTokenizer st = new StringTokenizer(option, ".");
        int tokenCount = st.countTokens();
        if (productTaxonomyMap.isEmpty())
        {
        	productTaxonomyMap = this.getProductTaxonomyMap();
        }
        String optionClass = st.nextToken();
        String key = st.nextToken();
        String selectedAssetClass = (tokenCount>2) ? st.nextToken() : "";
        String selectedProduct = (tokenCount>3) ? st.nextToken() : "";
        
        if (optionClass.equals("assetClass"))
        {
            Map<String, Object> productMap = (Map<String, Object>)productTaxonomyMap.get(selectedAssetClass);
            Set<String> productSet = productMap.keySet();
            for (Iterator iterator = productSet.iterator(); iterator.hasNext();)
            {
            	String product = (String) iterator.next();
                map.put("pretrade.input.product." + (StringUtils.lowerCase(selectedAssetClass)) + "."
                		+ (StringUtils.lowerCase(product)), product);
            }
        }
        else if (optionClass.equals("product"))
        {
            Map<String, String> subProductMap = (Map<String, String>)((Map<String, Map<String,String>>)productTaxonomyMap.get(selectedAssetClass)).get(selectedProduct);
            Set<String> subProductKeySet = subProductMap.keySet();
            for (Iterator iteretorSubProduct = subProductKeySet.iterator(); iteretorSubProduct.hasNext();)
            {
            	String subProductKey = (String) iteretorSubProduct.next();
                if (subProductKey != null)
                {
                	map.put(("pretrade.input.subproduct." + StringUtils.lowerCase(selectedProduct) + "." + StringUtils
                			.lowerCase(subProductKey)), (String) subProductMap.get(subProductKey));
                }
            }
        }
        return map;
    }
    
    

    public String resolveProductTaxonomy(String propertykey) {

    	if (propertykey==null || "".equals(propertykey)) {
    		return "UnAssigned";
    	}

    	if (properties.containsKey(propertykey)) {
    		return ((String) properties.get(propertykey)).trim();
    	}
    	return null;
    }


    /***
     * Get the Product Master Record for the the AssetClass, BaseProduct , SubProduct vars
     * @param assetClass
     * @param baseProduct
     * @param subProduct
     * @return
     */
    public CSVRecord getProductMaster(String assetClass, String baseProduct, String subProduct) {
        Iterator<CSVRecord> productMasterList = productMasterTable.listIterator();
        while(productMasterList.hasNext()){

            //If subProduct not specifed then match on AssetClass and Product Only
            //Other wise if provided must
            CSVRecord target = productMasterList.next();

            if(subProduct != null) {

                if(findProductMatchingByFull(target , assetClass, baseProduct, subProduct ))
                    return target;

            } else {

                if(findProductMatchingByAssetAndBase(target , assetClass, baseProduct ))
                    return target;

            }

        }
        return null;
    }

    /****
     * Test of found a match on the Product master object
     * @param target
     * @param assetClass
     * @param baseProduct
     * @param subProduct
     * @return
     */
    private boolean findProductMatchingByFull(CSVRecord target , String assetClass, String baseProduct, String subProduct ) {

         if (assetClass.equals(target.get(PRODUCT_MASTER_ASSESTCLASS)) &&
                baseProduct.equals(target.get(PRODUCT_MASTER_BASEPRODUCT)) &&
                subProduct.equals(target.get(PRODUCT_MASTER_SUBPRODUCT))) {
                    return true;
         }

        return false;
    }

    /****
     * Test of found a match on the Product master object
     * @param target
     * @param assetClass
     * @param baseProduct
     * @return
     */
    private boolean findProductMatchingByAssetAndBase(CSVRecord target , String assetClass, String baseProduct ) {

       if (assetClass.equals(target.get(PRODUCT_MASTER_ASSESTCLASS)) &&
           baseProduct.equals(target.get(PRODUCT_MASTER_BASEPRODUCT))) {
             return true;
       }
       return false;
    }



    /**
     * Return a Collection of Product master Records
     * @return
     */
    public Collection<CSVRecord> getProductMasterCollection() {

        return productMasterTable.subList(0,productMasterTable.size());

    }
    /***
     * Reture the Default Credit Index based on the parms sent on the call
     * if the series is a -1 do search by On the Run set to true.
     * @param indexLabel
     * @param series
     * @return
     */
    public CSVRecord getCreditIndexDefaultsByIndexSeriesTerm(String indexLabel, Integer series) {

        Iterator<CSVRecord> defaultsCreditIndecMasterList = defaultsCreditIndicesTable.listIterator();
        while(defaultsCreditIndecMasterList.hasNext()){

            CSVRecord defaultsCreditIndex = defaultsCreditIndecMasterList.next();
            if ( indexLabel != null && series!=-1 ) {
                if( indexLabel.equals( defaultsCreditIndex.get(CREDIT_INDEX_INDEXLABEL) )  &&
                        Integer.parseInt(defaultsCreditIndex.get(CREDIT_INDEX_SERIES))  == series  ) {

                    return defaultsCreditIndex;
                }
            } else {

                if( indexLabel.equals( defaultsCreditIndex.get(CREDIT_INDEX_INDEXLABEL) )  &&
                        Boolean.parseBoolean(defaultsCreditIndex.get(CREDIT_INDEX_ONTHERUN))  ) {
                    return defaultsCreditIndex;
                }
            }

        }
        return null;
    }




    /****
     * Get a list of series sorted for a given credit index by name
     * @param index
     * @return
     */
    public List<String> getSeriesListByCreditIndex(String index) {


        //first fetch by filter index then sort the list
        ArrayList<String> selectedSortedList = new ArrayList<String>();
        Iterator<CSVRecord> defaultsCreditIndecMasterList = defaultsCreditIndicesTable.listIterator();
        while(defaultsCreditIndecMasterList.hasNext()){
            CSVRecord target = defaultsCreditIndecMasterList.next();

            if(index.equals( target.get(CREDIT_INDEX_INDEXLABEL) ) ) {

                selectedSortedList.add(target.get(CREDIT_INDEX_SERIES));
            }

        }

        // Sorting list Acesnding order
        Collections.sort(selectedSortedList);
        //Reverse list in Decending order
        Collections.sort(selectedSortedList,Collections.reverseOrder());

        return selectedSortedList;
    }

    /*
    ---> Comparator om the fly
    // Sorting list Acesnding order
        Collections.sort(selectedSortedList, new Comparator<String>() {
            @Override
            public int compare(String creditIndexSeries1, String creditIndexSeries2)
            {
                return  creditIndexSeries1.compareTo(creditIndexSeries2);
            }
          });

     */

    /***
     * For a given Product Object , currency and floatInder return the  object

     * @param currency
     * @param floatIndex
     * @return
     */
    public CSVRecord getIrDefaultsByCurrencyIndex( String assetClass, String baseProduct, String subProduct , String currency, String floatIndex) {
        if(assetClass.equals("InterestRate") && baseProduct.equals("IRSwap") ) {
            CSVRecord  productMaster =  getProductMaster(assetClass, baseProduct, subProduct);
            return getIrDefaultsByCurrencyIndex(productMaster, currency, floatIndex);

        }
        return null;
    }



    /***
     * For a given Product Object , currency and floatInder return the  object

     * @param currency
     * @param floatIndex
     * @return
     */
    public CSVRecord getIrDefaultsByCurrencyIndex(CSVRecord productMaster, String currency, String floatIndex) {

        Iterator<CSVRecord>  defaultInterestRatesList  = defaultsIntrestRatesTable.listIterator();

        while(defaultInterestRatesList.hasNext()){

            CSVRecord target = defaultInterestRatesList.next();

            if( productMaster.get(PRODUCT_MASTER_ID).trim().equals(target.get(INTEREST_RATES_PRODUCT_MASTER_ID).trim())  &&
                target.get(INTEREST_RATES_CURRENCY).equals(currency)                       &&
                target.get(INTEREST_RATES_FLOATINDEX).equals(floatIndex)) {
                return target;
            }

        }


        return null;
    }

    /****
     * Get the default InterestRate for a given product anc currency

     * @param currency
     * @return
     */
    public CSVRecord getIrDefaultsByCurrency(CSVRecord productMaster, String currency){

        Iterator<CSVRecord>  defaultInterestRatesList  = defaultsIntrestRatesTable.listIterator();
        while(defaultInterestRatesList.hasNext()){

            CSVRecord target = defaultInterestRatesList.next();
            if(productMaster.get(PRODUCT_MASTER_ID).trim().equals(target.get(INTEREST_RATES_PRODUCT_MASTER_ID).trim()) &&
               target.get(INTEREST_RATES_CURRENCY).equals(currency)   )   {
               return target;
            }

        }
        return null;
    }



    /***
     * Return a list of Currencies for a given product

     * @return
     */
    public List<String> getCurrencyListByProduct(CSVRecord productMaster) {

        ArrayList<String> currencyList = new ArrayList<String>();
        Iterator<CSVRecord>  defaultInterestRatesList  = defaultsIntrestRatesTable.listIterator();
        while(defaultInterestRatesList.hasNext()){

            CSVRecord target = defaultInterestRatesList.next();
            if(productMaster.get(PRODUCT_MASTER_ID).trim().equals(target.get(INTEREST_RATES_PRODUCT_MASTER_ID).trim()) ) {

                if(!currencyList.contains(target.get(INTEREST_RATES_CURRENCY))) {
                    currencyList.add(target.get(INTEREST_RATES_CURRENCY));
                }
            }

        }

        return currencyList;
    }

    /***
     * Return a list of FLoating Indexes for a given product

     * @return
     */
    public List<String> getIndexListByProduct(CSVRecord productMaster) {


        ArrayList<String> indexList = new ArrayList<String>();

        Iterator<CSVRecord>  defaultInterestRatesList  = defaultsIntrestRatesTable.listIterator();


        while(defaultInterestRatesList.hasNext()){

            CSVRecord target = defaultInterestRatesList.next();
            if(productMaster.get(PRODUCT_MASTER_ID).trim().equals(target.get(INTEREST_RATES_PRODUCT_MASTER_ID).trim())) {

                if(!indexList.contains( target.get(INTEREST_RATES_FLOATINDEX))) {
                    indexList.add(target.get(INTEREST_RATES_FLOATINDEX));
                }
            }

        }
       return indexList;
    }

    /***
     * Return a list of Currencies for a given Product and Index

     * @param index
     * @return
     */
    public List<String> getCurrencyListByProductAndIndex(CSVRecord productMaster, String index) {

        ArrayList<String> currencyList = new ArrayList<String>();
        Iterator<CSVRecord>  defaultInterestRatesList  = defaultsIntrestRatesTable.listIterator();

        while(defaultInterestRatesList.hasNext()){

            CSVRecord target = defaultInterestRatesList.next();

            if(productMaster.get(PRODUCT_MASTER_ID).trim().equals(target.get(INTEREST_RATES_PRODUCT_MASTER_ID).trim()) &&
               target.get(INTEREST_RATES_FLOATINDEX).equals(index)  ) {

                if(!currencyList.contains(target.get(INTEREST_RATES_CURRENCY))) {
                    currencyList.add(target.get(INTEREST_RATES_CURRENCY));
                }
            }

        }
        return currencyList;
    }


    /***
     *
     * @param transactionType
     * @param family
     * @return
     */
    public String getCreditIndexByTransactionTypeAndFamily(String transactionType, String family) {

        Iterator<CSVRecord> defaultsCreditIndecMasterList = defaultsCreditIndicesTable.listIterator();

         while(defaultsCreditIndecMasterList.hasNext()){
             CSVRecord target = defaultsCreditIndecMasterList.next();
            if(family!=null && !family.isEmpty()){
                //Must me an exact match
                if(transactionType.equals(target.get(CREDIT_INDEX_TRANSACTIONTYPE)) && family.equals(target.get(CREDIT_INDEX_FAMILY)) ){
                    return target.get(CREDIT_INDEX_INDEXLABEL);
                }
            } else if(transactionType.equals(target.get(CREDIT_INDEX_TRANSACTIONTYPE))) {
                // Partial match on first tran type match
                return target.get(CREDIT_INDEX_INDEXLABEL);
            }
        }
        return null;
    }

    /****
     * Return a list of Transaction Types for a given product

     * @return
     */
    public List<String> getTransactionTypeListByProduct(CSVRecord productMaster) {
        //first fetch by filter index then sort the list
        ArrayList<String> selectedSortedList = new ArrayList<String>();

        Iterator<CSVRecord> productMasterExtensionsList = productMasterExtensionsTable.listIterator();

        while(productMasterExtensionsList.hasNext()){
            CSVRecord target = productMasterExtensionsList.next();

            if(productMaster.get(PRODUCT_MASTER_ID).trim().equals(target.get(PRODUCT_MASTER_EXTENSIONS_PRODUCT_MASTER_ID).trim())) {

                selectedSortedList.add(target.get(PRODUCT_MASTER_EXTENSIONS_TRANSACTIONTYPE));
            }

        }
        // Sorting list Acesnding order
        Collections.sort(selectedSortedList);
        //Reverse list in Decending order
        Collections.sort(selectedSortedList,Collections.reverseOrder());

        return selectedSortedList;


    }

    /***
     *
     * @return
     */
    public List<String> getCreditIndexListByProduct(CSVRecord productMaster) {
        //first fetch by filter index then sort the list
        ArrayList<String> selectedSortedList = new ArrayList<String>();

        Iterator<CSVRecord> defaultsCreditIndesMasterList = defaultsCreditIndicesTable.listIterator();

        while(defaultsCreditIndesMasterList.hasNext()){
            CSVRecord target = defaultsCreditIndesMasterList.next();

            if(productMaster.get(PRODUCT_MASTER_SUBPRODUCT).equals(target.get(CREDIT_INDEX_SUBPRODUCT))) {
                logger.debug("CredixIndexName[ {} ]",target.get(CREDIT_INDEX_INDEXLABEL));
                if(!selectedSortedList.contains(target.get(CREDIT_INDEX_INDEXLABEL))){
                    logger.debug("   add to list CredixIndexName[ {} ]",target.get(CREDIT_INDEX_INDEXLABEL));
                    selectedSortedList.add(target.get(CREDIT_INDEX_INDEXLABEL));
                }

            }
        }
        // Sorting list Acesnding order
        Collections.sort(selectedSortedList);
        //Reverse list in Decending order
        Collections.sort(selectedSortedList,Collections.reverseOrder());
        return selectedSortedList;
    }



    /***
     *

     * @param currency
     * @return
     */
    public List<String> getIndexListByProductAndCurrency(CSVRecord productMaster, String currency){

        ArrayList<String> indexList = new ArrayList<String>();
        if(productMaster != null) {

            Iterator<CSVRecord>  defaultInterestRatesList  = defaultsIntrestRatesTable.listIterator();
            while(defaultInterestRatesList.hasNext()){

                CSVRecord target = defaultInterestRatesList.next();
                if(productMaster.get(PRODUCT_MASTER_ID).trim().equals(target.get(  INTEREST_RATES_PRODUCT_MASTER_ID).trim())
                      && currency.equals(target.get(INTEREST_RATES_CURRENCY) ) ) {


                    if(!indexList.contains(target.get(INTEREST_RATES_FLOATINDEX))) {
                        indexList.add(target.get(INTEREST_RATES_FLOATINDEX));
                    }
                }

            }
        }

        return indexList;

    }

    /***
     *
     * @param transactionType
     * @return
     */
    public String getCreditIndexByTransactionType(String transactionType){


        Iterator<CSVRecord> defaultsCreditIndecMasterList = defaultsCreditIndicesTable.listIterator();

        while(defaultsCreditIndecMasterList.hasNext()){

            CSVRecord target = defaultsCreditIndecMasterList.next();
            if(transactionType.equals(target.get(CREDIT_INDEX_TRANSACTIONTYPE)) ){
                    return target.get(CREDIT_INDEX_INDEXLABEL);
            }

        }
        return null;


    }

}
