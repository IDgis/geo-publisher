package nl.idgis.publisher.metadata;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.idgis.publisher.utils.SimpleDateFormatMapper;
import nl.idgis.publisher.xml.XMLDocument;
import nl.idgis.publisher.xml.exceptions.NotFound;
import nl.idgis.publisher.xml.exceptions.QueryFailure;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * 
 * Represents a metadata document for datasets and services. <br>
 * Metadata Standard for datasets according to ISO 19115 V 1.3.<br>
 * Metadata Standard for services according to ISO 19119 V 1.2.1<br>
 * <br>
 * The use of this MetataDocument assumes that content is already available i.e. read from a xml template.<br>
 * Use of getters and setters assumes that the xml elements are available and that only their content is read or updated. 
 * @author Rob
 *
 */
public class MetadataDocument {
	
	private static final String METADATA_DATE_PATTERN = "yyyy-MM-dd";
	private static final String CREATION = "creation";
	private static final String REVISION = "revision";
	
	private static enum Topic {DATASET, SERVICE};
	
	private static final String [] ROLE_CODES = {"owner","pointOfContact"}; 
	
	private final XMLDocument xmlDocument;
	private final BiMap<String, String> namespaces;
	
	public MetadataDocument(XMLDocument xmlDocument) {
		this.xmlDocument = xmlDocument;
		
		namespaces = HashBiMap.create();
		namespaces.put("gmd", "http://www.isotc211.org/2005/gmd");
		namespaces.put("gco", "http://www.isotc211.org/2005/gco");
		namespaces.put("srv", "http://www.isotc211.org/2005/srv");
		namespaces.put("xlink", "http://www.w3.org/1999/xlink");
	}
	
	@Override
	public MetadataDocument clone() {
		return new MetadataDocument(xmlDocument.clone());
	}

	/*
	 * generic methods
	 */
	
	public byte[] getContent() throws IOException {
		return xmlDocument.getContent();
	}
	
	private String dateToString(String pattern, Date date){		
		Format formatter = new SimpleDateFormat(pattern);
		return formatter.format(date);
	}
	
	/*
	 * shared methods for DATASET, SERVICE	
	 */

	private String getDatasetIdentificationPath(){
		return "/gmd:MD_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification";
	}
	
	private String getServiceIdentificationPath(){
		return "/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification";
	}
	
	private String getIdentificationPath(Topic topic){
		return (topic==Topic.DATASET?getDatasetIdentificationPath():getServiceIdentificationPath());
	}
	
	/*
	 * date
	 * 
	 */

	private String getDatePath(Topic topic, String codeListValue) {
		return getIdentificationPath(topic) +
			"/gmd:citation" +
			"/gmd:CI_Citation" +
			"/gmd:date" +
			"/gmd:CI_Date" +
				"[gmd:dateType" +
				"/gmd:CI_DateTypeCode" +
				"/@codeListValue" +
					"='" + codeListValue + "']" +
			"/gmd:date";
	}
		
	private Date getDate(Topic topic, String codeListValue) throws NotFound {
		String datePath = getDatePath(topic, codeListValue);		
		
		Date date = null; 
		
		try { 
			String dateString = xmlDocument.getString(namespaces, datePath + "/gco:DateTime");
			date = SimpleDateFormatMapper.isoDateTime().apply(dateString);
		} catch(NotFound nf) {}
		
		if(date == null) {		
			try { 
				String dateString = xmlDocument.getString(namespaces, datePath + "/gco:Date");		
				date = SimpleDateFormatMapper.isoDate().apply(dateString);
			} catch(NotFound nf) {}
		}
		
		return date;
	}

	private void setDate(Topic topic, String codeListValue, Date date) throws Exception{
		
		String datePath = getDatePath(topic, codeListValue);
		
		String dateString = dateToString(METADATA_DATE_PATTERN, date);
		
		xmlDocument.updateString(namespaces, datePath, dateString);
	}
	
	private void setDate(Topic topic,String codeListValue, Timestamp ts) throws Exception{
		Date date = new Date(ts.getTime());
		setDate(topic, codeListValue, date);
	}
	
	public Date getServiceRevisionDate() throws NotFound {
		Date date = getDate(Topic.SERVICE, REVISION);
		if(date != null) {
			return date;
		}
		return getDate(Topic.SERVICE, CREATION);
	}
	
	public void setServiceRevisionDate(Timestamp ts) throws Exception {
		setDate(Topic.SERVICE, REVISION, ts);
	}
	
	
	public Date getDatasetRevisionDate() throws NotFound {
		Date date = getDate(Topic.DATASET, REVISION);
		if(date != null) {
			return date;
		}
		return getDate(Topic.DATASET, CREATION);
	}
	
	public void setDatasetRevisionDate(Timestamp ts) throws Exception {
		setDate(Topic.DATASET, REVISION, ts);
	}
	
	/*
	 * Title
	 */
	
	private String getTitlePath(Topic topic) {
		return getIdentificationPath(topic) +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:title" +
				"/gco:CharacterString";
	}
	
	public String getServiceTitle() throws NotFound {
		return xmlDocument.getString(namespaces, getTitlePath(Topic.SERVICE));
	}
	
	public void setServiceTitle (String title) throws QueryFailure {
		xmlDocument.updateString(namespaces, getTitlePath(Topic.SERVICE), title);		
	}
	
	public String getDatasetTitle() throws NotFound {
		return xmlDocument.getString(namespaces, getTitlePath(Topic.DATASET));
	}
	
	public void setDatasetTitle (String title) throws QueryFailure {
		xmlDocument.updateString(namespaces, getTitlePath(Topic.DATASET), title);		
	}
	
	/*
	 * Abstract
	 */

	private String getAbstractPath(Topic topic) {
		return getIdentificationPath(topic) +
			"/gmd:abstract" +
			"/gco:CharacterString";
	}
		
	public String getServiceAbstract() throws NotFound {
		return xmlDocument.getString(namespaces, getAbstractPath(Topic.SERVICE));
	}
	
	public void setServiceAbstract (String Abstract) throws QueryFailure {
		xmlDocument.updateString(namespaces, getAbstractPath(Topic.SERVICE), Abstract);		
	}
	
	public String getDatasetAbstract() throws NotFound {
		return xmlDocument.getString(namespaces, getAbstractPath(Topic.DATASET));
	}
	
	public void setDatasetAbstract (String Abstract) throws QueryFailure {
		xmlDocument.updateString(namespaces, getAbstractPath(Topic.DATASET), Abstract);		
	}
	
	/*
	 * keywords
	 *  
	 */
	
	private String getKeywordPath(Topic topic) {
		return getIdentificationPath(topic) +
				"/gmd:descriptiveKeywords"
				;
	}

	private int removeKeywords(Topic topic) throws NotFound {
		return xmlDocument.removeNodes(namespaces, getKeywordPath(topic));
	}
	
	private void addKeywords(Topic topic, List<String> keywords, String thesaurusTitle, String thesaurusDate, String thesaurusCodeList, String thesaurusCodeListValue) throws NotFound {		
		String keywordsPath = xmlDocument.addNode(namespaces, getIdentificationPath(topic), "gmd:descriptiveKeywords/gmd:MD_Keywords");
		
		for (String keyword : keywords) {
			xmlDocument.addNode(namespaces, keywordsPath, "gmd:keyword/gco:CharacterString", keyword);
		}
		String thesaurusPath = xmlDocument.addNode(namespaces, keywordsPath, "gmd:thesaurusName/gmd:CI_Citation");
		xmlDocument.addNode(namespaces, thesaurusPath, "gmd:title/gco:CharacterString", thesaurusTitle);
		
		String thesaurusDatePath = xmlDocument.addNode(namespaces, thesaurusPath, "gmd:date/gmd:CI_Date");

		xmlDocument.addNode(namespaces, thesaurusDatePath, "gmd:date/gco:Date", thesaurusDate);
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("codeList", thesaurusCodeList);
		attributes.put("codeListValue", thesaurusCodeListValue);		
		
		xmlDocument.addNode(namespaces, thesaurusDatePath, "gmd:dateType/gmd:CI_DateTypeCode", attributes);
	}
	
	public int removeDatasetKeywords() throws NotFound {
		return removeKeywords(Topic.DATASET);
	}
	
	public void addDatasetKeywords(List<String> keywords, String thesaurusTitle, String thesaurusDate, String thesaurusCodeList, String thesaurusCodeListValue) throws NotFound {
		addKeywords(Topic.DATASET, keywords, thesaurusTitle, thesaurusDate, thesaurusCodeList, thesaurusCodeListValue);
	}
	
	public String getDatasetKeywords() throws NotFound{
		return xmlDocument.getString(namespaces, getKeywordPath(Topic.DATASET));
	}
	
	public int removeServiceKeywords() throws NotFound {
		return removeKeywords(Topic.SERVICE);
	}
	
	public void addServiceKeywords(List<String> keywords, String thesaurusTitle, String thesaurusDate, String thesaurusCodeList, String thesaurusCodeListValue) throws NotFound {
		addKeywords(Topic.SERVICE, keywords, thesaurusTitle, thesaurusDate, thesaurusCodeList, thesaurusCodeListValue);
	}
	
	public String getServiceKeywords() throws NotFound{
		return xmlDocument.getString(namespaces, getKeywordPath(Topic.SERVICE));
	}
	
	/*
	 * Responsible Party
	 *  - name
	 *  - email
	 * 
	 */

	private String getResponsiblePartyPath(Topic topic) {
		return getIdentificationPath(topic) +
			"/gmd:pointOfContact" +
			"/gmd:CI_ResponsibleParty" 
			;
	}
	
	private String getResponsiblePartyNamePath(Topic topic) {
		return getResponsiblePartyPath(topic) + 
			"/gmd:organisationName" +
			"/gco:CharacterString";
	}
	
	private String getResponsiblePartyEmailPath(Topic topic) {
		return getResponsiblePartyPath(topic) + 
			"/gmd:contactInfo" +
			"/gmd:CI_Contact" +
			"/gmd:address" +
			"/gmd:CI_Address" +
			"/gmd:electronicMailAddress" +
			"/gco:CharacterString";
	}
	
	public String getServiceResponsiblePartyName() throws Exception{
		return xmlDocument.getString(namespaces, getResponsiblePartyNamePath(Topic.SERVICE));
	}
	
	public void setServiceResponsiblePartyName(String name) throws Exception{
		xmlDocument.updateString(namespaces, getResponsiblePartyNamePath(Topic.SERVICE), name);
	}
	
	public String getServiceResponsiblePartyEmail() throws Exception{
		return xmlDocument.getString(namespaces, getResponsiblePartyEmailPath(Topic.SERVICE));
	}
	
	public void setServiceResponsiblePartyEmail(String email) throws Exception{
		xmlDocument.updateString(namespaces, getResponsiblePartyEmailPath(Topic.SERVICE), email);
	}
	
	public String getDatasetResponsiblePartyName() throws Exception{
		return xmlDocument.getString(namespaces, getResponsiblePartyNamePath(Topic.DATASET));
	}
	
	public void setDatasetResponsiblePartyName(String name) throws Exception{
		xmlDocument.updateString(namespaces, getResponsiblePartyNamePath(Topic.DATASET), name);
	}
	
	public String getDatasetResponsiblePartyEmail() throws Exception{
		return xmlDocument.getString(namespaces, getResponsiblePartyEmailPath(Topic.DATASET));
	}
	
	public void setDatasetResponsiblePartyEmail(String email) throws Exception{
		xmlDocument.updateString(namespaces, getResponsiblePartyEmailPath(Topic.DATASET), email);
	}
	
	
	/*
	 * Metadata
	 * 
	 */

	/*
	 * Metadata file identifier
	 */

	private String getMetaDataIdentifierPath() {
		return 
				"/gmd:MD_Metadata" +
				"/gmd:fileIdentifier" +				
				"/gco:CharacterString"
				;
	}
	
	public String getMetaDataIdentifier() throws Exception{
		return xmlDocument.getString(namespaces, getMetaDataIdentifierPath());
	}
	
	public void setMetaDataIdentifier(String identifier) throws Exception{
		xmlDocument.updateString(namespaces, getMetaDataIdentifierPath(), identifier);
	}

	/*
	 * Alias
	 */
	public String getFileIdentifier() throws Exception{
		return getMetaDataIdentifier();
	}
	
	public void setFileIdentifier(String fileIdentifier) throws Exception {
		setMetaDataIdentifier(fileIdentifier);
	}
	
	/*
	 * Metadata PointOfContact
	 */
	
	private String getMetaDataPointOfContactPath(String role) {
		return 
			"/gmd:MD_Metadata" +
			"/gmd:contact" +
			"/gmd:CI_ResponsibleParty" +

			"[gmd:role" +
			"/gmd:CI_RoleCode" +
			"/@codeListValue" +
				"='" + role + "']"
			;
	}
	
	private String getMetaDataPointOfContactNamePath(String role) {
		return getMetaDataPointOfContactPath(role) + 
			"/gmd:organisationName" +
			"/gco:CharacterString";
	}
	
	private String getMetaDataPointOfContactEmailPath(String role) {
		return getMetaDataPointOfContactPath(role) + 
			"/gmd:contactInfo" +
			"/gmd:CI_Contact" +
			"/gmd:address" +
			"/gmd:CI_Address" +
			"/gmd:electronicMailAddress" +
			"/gco:CharacterString";
	}
	
	public String getMetaDataPointOfContactName(String role) throws NotFound{
		return xmlDocument.getString(namespaces, getMetaDataPointOfContactNamePath(role));
	}
	
	public String getMetaDataPointOfContactEmail(String role) throws Exception{
		return xmlDocument.getString(namespaces, getMetaDataPointOfContactEmailPath(role));
	}
	
	public void setMetaDataPointOfContactName(String role, String name) throws Exception{
		xmlDocument.updateString(namespaces, getMetaDataPointOfContactNamePath(role), name);
	}
	
	public void setMetaDataPointOfContactEmail(String role, String email) throws Exception{
		xmlDocument.updateString(namespaces, getMetaDataPointOfContactEmailPath(role), email);
	}
	

	/*
	 * Metadata creation date
	 */
	private String getMetaDataCreationDatePath() {
		return 
			"/gmd:MD_Metadata" +
			"/gmd:dateStamp" +
			"/gco:Date" 
			;
	}
	
	public String getMetaDataCreationDate() throws Exception{
		return xmlDocument.getString(namespaces, getMetaDataCreationDatePath());
	}
	
	public void setMetaDataCreationDate(Date date) throws Exception{
		xmlDocument.updateString(namespaces, getMetaDataCreationDatePath(), dateToString(METADATA_DATE_PATTERN, date));
	}

	
	/*
	 * DATASET
	 * 
	 */
	/*
	 * alternate title
	 */
	private String getAlternateTitlePath() {
		return getDatasetIdentificationPath() +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:alternateTitle" +
				"/gco:CharacterString";
	}
	
	public String getAlternateTitle() throws NotFound {
		return xmlDocument.getString(namespaces, getAlternateTitlePath());
	}

	public void setAlternateTitle(String newTitle) throws QueryFailure {
		xmlDocument.updateString(namespaces, getAlternateTitlePath(), newTitle);
	}
	
	/*
	 * other constraints
	 * 
	 */
	private String getOtherconstraintsPath() {
		return getDatasetIdentificationPath() + "/gmd:resourceConstraints"
				+ "/gmd:MD_LegalConstraints/gmd:otherConstraints/gco:CharacterString";
	}
	
	public String getOtherConstraints() throws NotFound {
		return xmlDocument.getString(namespaces, getOtherconstraintsPath());		
	}
	

	/**
	 * Dataset metadata: Service Linkage
	 */	
	private String getDigitalTransferOptionsPath(){
		return
				"/gmd:MD_Metadata" + 
				"/gmd:distributionInfo" + 
				"/gmd:MD_Distribution" + 
				"/gmd:transferOptions" + 
				"/gmd:MD_DigitalTransferOptions" ;
	}
	
	private String getServiceLinkagePath() {
		return getDigitalTransferOptionsPath() + "/gmd:onLine";
	}
	
	/**
	 * Remove all gmd:onLine childnodes of node MD_DigitalTransferOptions
	 * @return nr of removed nodes
	 * @throws Exception
	 */
	public int removeServiceLinkage() throws Exception{
		return xmlDocument.removeNodes(namespaces, getServiceLinkagePath());
	}
	
	/**
	 * Add a new /gmd:onLine/gmd:CI_OnlineResource node under gmd:MD_DigitalTransferOptions
	 * @param linkage content of the /gmd:linkage/gmd:URL node
	 * @param protocol content of the /gmd:protocol/gco:CharacterString node
	 * @param name content of the /gmd:name/gco:CharacterString node
	 * @throws NotFound 
	 */
	public void addServiceLinkage(String linkage, String protocol, String name) throws NotFound{
		String parentPath = xmlDocument.addNode(namespaces, getDigitalTransferOptionsPath(), new String[]{"gmd:offLine"}, "gmd:onLine/gmd:CI_OnlineResource");
		xmlDocument.addNode(namespaces, parentPath, "gmd:linkage/gmd:URL", linkage);
		xmlDocument.addNode(namespaces, parentPath, "gmd:protocol/gco:CharacterString", protocol);
		xmlDocument.addNode(namespaces, parentPath, "gmd:name/gco:CharacterString", name);
	}	
	
	public String getServiceLinkageURL() throws NotFound{
		return xmlDocument.getString(namespaces, getServiceLinkagePath() + "/gmd:CI_OnlineResource/gmd:linkage/gmd:URL");
	}
	
	public String getServiceLinkageProtocol() throws NotFound{
		return xmlDocument.getString(namespaces, getServiceLinkagePath() + "/gmd:CI_OnlineResource/gmd:protocol/gco:CharacterString");
	}
	
	public String getServiceLinkageName() throws NotFound{
		return xmlDocument.getString(namespaces, getServiceLinkagePath() + "/gmd:CI_OnlineResource/gmd:name/gco:CharacterString");
	}
	
	/**
	 * Dataset metadata: Dataset Identifier
	 */	
	private String getDatasetIdentifierCodePath() {
		return 
				getDatasetIdentificationPath() +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:identifier" +
				"/gmd:MD_Identifier" +
				"/gmd:code"
				;
	}
	
	private String getDatasetIdentifierPath() {
		return 
			getDatasetIdentifierCodePath() +
			"/gco:CharacterString"
			;
	}
	
	public void setDatasetIdentifier(String datasetIdentifier) throws QueryFailure {
		xmlDocument.updateString(namespaces, getDatasetIdentifierPath(), datasetIdentifier);
	}
	
	public String getDatasetIdentifier() throws QueryFailure {
		return xmlDocument.getString(namespaces, getDatasetIdentifierPath());
	}
	
	
	//
	// SERVICE
	//
	
	/**
	 * Service metadata: serviceType
	 */	
	private String getServiceTypePath(){
		return getServiceIdentificationPath() + "/srv:serviceType";
	}	
	
	public int removeServiceType() throws NotFound {
		return xmlDocument.removeNodes(namespaces, getServiceTypePath());		
	}
	
	/**
	 * Add a new /srv:serviceType/gco:LocalName node under srv:SV_ServiceIdentification
	 * @param serviceLocalName content of the /srv:serviceType/gco:LocalName node
	 * @throws NotFound 
	 */
	public void addServiceType(String serviceLocalName) throws NotFound{		
		xmlDocument.addNode(namespaces, getServiceIdentificationPath(), 
			new String[]{
				"srv:serviceTypeVersion",
				"srv:accessProperties",
				"srv:restrictions",
				"srv:keywords",
				"srv:extent",
				"srv:coupledResource",
				"srv:couplingType",
				"srv:containsOperations",
				"srv:operatesOn"
			}, 
			
			"srv:serviceType/gco:LocalName", serviceLocalName);
	}
	
	public String getServiceType() throws QueryFailure {
		return xmlDocument.getString(namespaces, getServiceTypePath() + "/gco:LocalName");
	}
	
	/**
	 * Service metadata: BrowseGraphic
	 */	
	private String getGraphicOverviewPath() {
		return getServiceIdentificationPath() + "/gmd:graphicOverview";
	}
	
	private String getBrowseGraphicPath() {
		return getGraphicOverviewPath() + "/gmd:MD_BrowseGraphic";
	}
	
	public int removeBrowseGraphic() throws NotFound {
		return xmlDocument.removeNodes(namespaces, getBrowseGraphicPath());		
	}
	
	/**
	 * Add a new /gmd:MD_BrowseGraphic/gco:CharacterString node under gmd:graphicOverview
	 * @param fileName content of the /gmd:MD_BrowseGraphic/gco:CharacterString node
	 * @throws NotFound 
	 */
	public void addBrowseGraphic(String fileName) throws NotFound {		
		xmlDocument.addNode(namespaces, getGraphicOverviewPath(), "gmd:MD_BrowseGraphic/gmd:fileName/gco:CharacterString", fileName);		
	}
	
	public String getBrowseGraphic() throws QueryFailure {
		return xmlDocument.getString(namespaces, getBrowseGraphicPath() + "/gmd:fileName/gco:CharacterString");
	}
	

	/**
	 * Service metadata: Service Endpoint
	 */	
	private String getOperationsPath(){
		return getServiceIdentificationPath() + "/srv:containsOperations";
	}
	
	private String getOperationMetadataPath(){
		return getOperationsPath() + "/srv:SV_OperationMetadata";
	}
	
	
	public int removeServiceEndpoint() throws NotFound {
		return xmlDocument.removeNodes(namespaces, getOperationMetadataPath());
	}

	/**
	 * Add a service endpoint to /srv:SV_OperationMetadata
	 * @param operationName name of service operation e.g. GetMap
	 * @param codeList attribute
	 * @param codeListValue attribute
	 * @param linkage url to service
	 * @throws NotFound
	 */
	public void addServiceEndpoint(String operationName, String codeList, String codeListValue, String linkage) throws NotFound {
		String parentPath = xmlDocument.addNode(namespaces, getOperationsPath(), "srv:SV_OperationMetadata");		
		
		xmlDocument.addNode(namespaces, parentPath, "srv:operationName/gco:CharacterString", operationName);
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("codeList", codeList);
		attributes.put("codeListValue", codeListValue);
		xmlDocument.addNode(namespaces, parentPath, "srv:DCP/srv:DCPList", attributes);
		
		xmlDocument.addNode(namespaces, parentPath, "srv:connectPoint/gmd:CI_OnlineResource/gmd:linkage/gmd:URL", linkage);
	}
	
	public String getServiceEndpointOperationName() throws QueryFailure {
		return xmlDocument.getString(namespaces, getOperationsPath() + "/srv:SV_OperationMetadata/srv:operationName/gco:CharacterString");
	}
	
	public String getServiceEndpointCodeList() throws QueryFailure {
		return xmlDocument.getString(namespaces, getOperationsPath() + "/srv:SV_OperationMetadata/srv:DCP/srv:DCPList/@codeList");
	}
	
	public String getServiceEndpointCodeListValue() throws QueryFailure {
		return xmlDocument.getString(namespaces, getOperationsPath() + "/srv:SV_OperationMetadata/srv:DCP/srv:DCPList/@codeListValue");
	}
	
	public String getServiceEndpointUrl() throws QueryFailure {
		return xmlDocument.getString(namespaces, getOperationsPath() + "/srv:SV_OperationMetadata/srv:connectPoint/gmd:CI_OnlineResource/gmd:linkage/gmd:URL");
	}
	

	/**
	 * Service metadata: CoupledResource (WMS layers c.q. WFS feature types) 
	 */
	private String getCoupledResourcePath(){
		return getServiceIdentificationPath() + "/srv:coupledResource";
	}
	
	private String getSVCoupledResourcePath(){
		return getCoupledResourcePath() + "/srv:SV_CoupledResource";
	}
	
	public int removeSVCoupledResource() throws NotFound {
		return xmlDocument.removeNodes(namespaces, getSVCoupledResourcePath());
	}

	/**
	 * Add info about WMS layers c.q. WFS feature types to /srv:SV_CoupledResource.
	 * @param operationName name of the service operation
	 * @param identifier identifier of dataset
	 * @param scopedName name of dataset
	 * @throws NotFound
	 */
	public void addSVCoupledResource(String operationName, String identifier, String scopedName) throws NotFound {
		String parentPath = xmlDocument.addNode(namespaces, getCoupledResourcePath(), "srv:SV_CoupledResource");
		xmlDocument.addNode(namespaces, parentPath, "srv:operationName/gco:CharacterString", operationName);
		xmlDocument.addNode(namespaces, parentPath, "srv:identifier/gco:CharacterString", identifier);
		xmlDocument.addNode(namespaces, parentPath, "gco:ScopedName", scopedName);
	}
	
	
	public String getServiceCoupledResourceOperationName() throws QueryFailure {
		return xmlDocument.getString(namespaces, getSVCoupledResourcePath() + "/srv:operationName/gco:CharacterString");
	}
	
	public String getServiceCoupledResourceIdentifier() throws QueryFailure {
		return xmlDocument.getString(namespaces, getSVCoupledResourcePath() + "/srv:identifier/gco:CharacterString");
	}
	
	public String getServiceCoupledResourceScopedName() throws QueryFailure {
		return xmlDocument.getString(namespaces, getSVCoupledResourcePath() + "/gco:ScopedName");
	}
	
	

	
	/**
	 * Service metadata: Link to dataset 
	 */	
	private String getOperatesOnPath(){
		return getServiceIdentificationPath() + "/srv:operatesOn";
	}
	
	public int removeOperatesOn() throws NotFound {
		return xmlDocument.removeNodes(namespaces, getOperatesOnPath());
	}

	/**
	 * Add link to dataset as attributes in /srv:operatesOn
	 * @param uuidref dataset reference
	 * @param href link to dataset metadata
	 * @throws NotFound
	 */
	public void addOperatesOn(String uuidref, String href) throws NotFound {		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("uuidref", uuidref);
		attributes.put("xlink:href", href);
		
		xmlDocument.addNode(namespaces, getServiceIdentificationPath(), "srv:operatesOn", attributes);
	}
	
	public String getOperatesOn() throws NotFound{
		return xmlDocument.getString(namespaces, getOperatesOnPath());
	}
	
	public String getOperatesOnUuid() throws NotFound{
		return xmlDocument.getString(namespaces, getOperatesOnPath() + "/@uuidref");
	}
	
	public String getOperatesOnHref() throws NotFound{
		return xmlDocument.getString(namespaces, getOperatesOnPath() + "/@xlink:href");
	}
	
	
	
	 
	  
	
	/*
	 * Service method aliases
	 */
	
	/*
	 * Resource Locator
	 * 
	 */
	
	public int removeLocator() throws Exception{
		return removeServiceLinkage();
	}
	
	public void addLocator(String linkage, String protocol, String name) throws NotFound{
		addServiceLinkage(linkage, protocol, name);
	}
	
	
	/*
	 * Operation name
	 * DCP List
	 * Connect point linkage
	 * 
	 */
	
	public int removeOperationMetadata()  throws Exception {
		return removeServiceEndpoint();
	}
	
	
	public void addOperationMetadata(String operationName, String codeList, String codeListValue, String linkage) throws NotFound {
		addServiceEndpoint(operationName, codeList, codeListValue, linkage);		
	}
	

	/*
	 * Coupled Resource
	 * 
	 */
	
	public int removeCoupledResource() throws NotFound {
		return removeOperatesOn();
	}

	public void addCoupledResource(String uuidref, String href) throws NotFound {
		addOperatesOn(uuidref, href); 		
	}
	
}
