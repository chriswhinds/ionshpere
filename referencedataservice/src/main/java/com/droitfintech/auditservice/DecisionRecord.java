package com.droitfintech.auditservice;



import java.math.BigDecimal;
import java.util.Date;


public class DecisionRecord {

    private String      decisionId;
    private Date decisionDate;       // in UTC
    private String      userId;             // user who created decision from GUI or external source
    private String      applicationName;    // application that created decision.
    private Date    submissionDate;
    private String      traderId;
    private String      tradeSalesPersionId;
    private String      tradeVenue;
    private String      tradeExternalId;
    private Date   tradeEffectiveDate;
    private Date   tradeTerminationDate;
    private String      droitDecision;          // JSON decision
    private String      tradeAssetClass;
    private String      tradeBaseProduct;
    private String      tradeSubProduct;
    private String      tradeCounterpartyId;
    private String      tradeContrapartyId;
    private String      tradeTerm;
    private BigDecimal  tradeNotional;
    private Boolean     groupTradeDecision;
    private Boolean     midRequired;            // supplement
    private String      midValue;               // supplement
    private Boolean     metRequired;
    private String      scenarioAnalysisFileName;  // supplement
    private String      scenarioAnalysisFileType;  // supplement
    private String      scenarioAnalysisFile;      // supplement
    private Boolean     override;               // supplement along with other override fields
    private String      overrideUser;           // GUI login that applied the override.
    private Date     overrideDate;           // time the override was done in UTC
    private String      overrideComments;
    private String      overrideApproverId;
    private String      tradeGoldenSourceId;
    private String      tradeGroupId;

    public DecisionRecord() {

    }

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public Date getDecisionDate() {
        return decisionDate;
    }

    public void setDecisionDate(Date decisionDate) {
        this.decisionDate = decisionDate;
    }

    public Date getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(Date submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getTraderId() {
        return traderId;
    }

    public void setTraderId(String traderId) {
        this.traderId = traderId;
    }

    public String getTradeSalesPersionId() {
        return tradeSalesPersionId;
    }

    public void setTradeSalesPersionId(String tradeSalesPersionId) {
        this.tradeSalesPersionId = tradeSalesPersionId;
    }

    public String getTradeVenue() {
        return tradeVenue;
    }

    public void setTradeVenue(String tradeVenue) {
        this.tradeVenue = tradeVenue;
    }

    public String getTradeExternalId() {
        return tradeExternalId;
    }

    public void setTradeExternalId(String tradeExternalId) {
        this.tradeExternalId = tradeExternalId;
    }

    public Date getTradeEffectiveDate() {
        return tradeEffectiveDate;
    }

    public void setTradeEffectiveDate(Date tradeEffectiveDate) {
        this.tradeEffectiveDate = tradeEffectiveDate;
    }

    public Date getTradeTerminationDate() {
        return tradeTerminationDate;
    }

    public void setTradeTerminationDate(Date tradeTerminationDate) {
        this.tradeTerminationDate = tradeTerminationDate;
    }

    public String getDroitDecision() {
        return droitDecision;
    }

    public void setDroitDecision(String droitDecision) {
        this.droitDecision = droitDecision;
    }

    public String getTradeAssetClass() {
        return tradeAssetClass;
    }

    public void setTradeAssetClass(String tradeAssetClass) {
        this.tradeAssetClass = tradeAssetClass;
    }

    public String getTradeBaseProduct() {
        return tradeBaseProduct;
    }

    public void setTradeBaseProduct(String tradeBaseProduct) {
        this.tradeBaseProduct = tradeBaseProduct;
    }

    public String getTradeSubProduct() {
        return tradeSubProduct;
    }

    public void setTradeSubProduct(String tradeSubProduct) {
        this.tradeSubProduct = tradeSubProduct;
    }

    public String getTradeCounterpartyId() {
        return tradeCounterpartyId;
    }

    public void setTradeCounterpartyId(String tradeCounterpartyId) {
        this.tradeCounterpartyId = tradeCounterpartyId;
    }

    public String getTradeContrapartyId() {
        return tradeContrapartyId;
    }

    public void setTradeContrapartyId(String tradeContrapartyId) {
        this.tradeContrapartyId = tradeContrapartyId;
    }

    public String getTradeTerm() {
        return tradeTerm;
    }

    public void setTradeTerm(String tradeTerm) {
        this.tradeTerm = tradeTerm;
    }

    public BigDecimal getTradeNotional() {
        return tradeNotional;
    }

    public void setTradeNotional(BigDecimal tradeNotional) {
        this.tradeNotional = tradeNotional;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Boolean isGroupTradeDecision() {
        return groupTradeDecision != null ? groupTradeDecision : Boolean.FALSE;
    }

    public void setGroupTradeDecision(Boolean groupTradeDecision) {
        this.groupTradeDecision = groupTradeDecision;
    }

    public Boolean isMidRequired() {
        return midRequired != null ? midRequired : Boolean.FALSE;
    }

    public void setMidRequired(Boolean midRequired) {
        this.midRequired = midRequired;
    }

    public String getMidValue() {
        return midValue;
    }

    public void setMidValue(String midValue) {
        this.midValue = midValue;
    }

    public Boolean isMetRequired() {
        return metRequired != null ? metRequired : Boolean.FALSE;
    }

    public void setMetRequired(Boolean metRequired) {
        this.metRequired = metRequired;
    }

    public String getScenarioAnalysisFileName() {
        return scenarioAnalysisFileName;
    }

    public void setScenarioAnalysisFileName(String scenarioAnalysisFileName) {
        this.scenarioAnalysisFileName = scenarioAnalysisFileName;
    }

    public String getScenarioAnalysisFileType() {
        return scenarioAnalysisFileType;
    }

    public void setScenarioAnalysisFileType(String scenarioAnalysisFileType) {
        this.scenarioAnalysisFileType = scenarioAnalysisFileType;
    }

    public String getScenarioAnalysisFile() {
        return scenarioAnalysisFile;
    }

    public void setScenarioAnalysisFile(String scenarioAnalysisFile) {
        this.scenarioAnalysisFile = scenarioAnalysisFile;
    }

    public Boolean hasOverride() {
        return override != null ? override : Boolean.FALSE;
    }

    public void setOverride(Boolean override) {
        this.override = override;
    }

    public String getOverrideUser() {
        return overrideUser;
    }

    public void setOverrideUser(String overrideUser) {
        this.overrideUser = overrideUser;
    }

    public Date getOverrideDate() {
        return overrideDate;
    }

    public void setOverrideDate(Date overrideDate) {
        this.overrideDate = overrideDate;
    }

    public String getOverrideComments() {
        return overrideComments;
    }

    public void setOverrideComments(String overrideComments) {
        this.overrideComments = overrideComments;
    }

    public String getOverrideApproverId() {
        return overrideApproverId;
    }

    public void setOverrideApproverId(String overrideApproverId) {
        this.overrideApproverId = overrideApproverId;
    }
    
    public String getTradeGoldenSourceId() {
        return tradeGoldenSourceId;
    }

    public void setTradeGoldenSourceId(String tradeGoldenSourceId) {
        this.tradeGoldenSourceId = tradeGoldenSourceId;
    }

    public String getTradeGroupId() {
        return tradeGroupId;
    }

    public void setTradeGroupId(String tradeGroupId) {
        this.tradeGroupId = tradeGroupId;
    }

}
