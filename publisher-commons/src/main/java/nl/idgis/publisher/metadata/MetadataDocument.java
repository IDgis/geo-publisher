package nl.idgis.publisher.metadata;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import nl.idgis.publisher.utils.SimpleDateFormatMapper;
import nl.idgis.publisher.utils.XMLUtils.XPathHelper;
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
	
	protected static final String METADATA_DATE_PATTERN = "yyyy-MM-dd";
	protected static final String CREATION = "creation";
	protected static final String REVISION = "revision";
	
	protected static enum Topic {DATASET, SERVICE};
	
	protected static final String [] ROLE_CODES = {"owner","pointOfContact"}; 
	
	protected final XMLDocument xmlDocument;
	protected final BiMap<String, String> namespaces;
	
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
	
	protected String dateToString(String pattern, Date date){		
		Format formatter = new SimpleDateFormat(pattern);
		return formatter.format(date);
	}
	
	/*
	 * shared methods for DATASET, SERVICE	
	 */

	protected String getDatasetIdentificationPath(){
		return "/gmd:MD_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification";
	}
	
	protected String getServiceIdentificationPath(){
		return "/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification";
	}
	
	protected String getIdentificationPath(Topic topic){
		return (topic==Topic.DATASET?getDatasetIdentificationPath():getServiceIdentificationPath());
	}
	
	/*
	 * date
	 * 
	 */

	protected String getDatePath(Topic topic, String codeListValue) {
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
		
	protected Date getDate(Topic topic, String codeListValue) throws NotFound {
		String datePath = getDatePath(topic, codeListValue); 
		
		try { 
			String dateString = xmlDocument.getString(namespaces, datePath + "/gco:DateTime");
			return SimpleDateFormatMapper.isoDateTime().apply(dateString);
		} catch(NotFound nf) {
			String dateString = xmlDocument.getString(namespaces, datePath + "/gco:Date");
			return SimpleDateFormatMapper.isoDate().apply(dateString);
		}
	}

	protected void setDate(Topic topic, String codeListValue, Date date) throws Exception{
		
		String datePath = getDatePath(topic, codeListValue);
		
		String dateString = dateToString(METADATA_DATE_PATTERN, date);
		
		xmlDocument.updateString(namespaces, datePath, dateString);
	}
	
	protected void setDate(Topic topic,String codeListValue, Timestamp ts) throws Exception{
		Date date = new Date(ts.getTime());
		setDate(topic, codeListValue, date);
	}
	
	public Date getServiceRevisionDate() throws NotFound {
		try {
			return getDate(Topic.SERVICE, REVISION);
		} catch(NotFound nf) {
			return getDate(Topic.SERVICE, CREATION);
		}
	}
	
	public void setServiceRevisionDate(Timestamp ts) throws Exception {
		setDate(Topic.SERVICE, REVISION, ts);
	}
	
	
	public Date getDatasetRevisionDate() throws NotFound {
		try { 
			return getDate(Topic.DATASET, REVISION);
		} catch(NotFound nf) {
			return getDate(Topic.DATASET, CREATION);
		}
	}
	
	public void setDatasetRevisionDate(Timestamp ts) throws Exception {
		setDate(Topic.DATASET, REVISION, ts);
	}
	
	/*
	 * Title
	 */
	
	protected String getTitlePath(Topic topic) {
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
	
	public void setServiceAlternateTitle(String alternateTitle) throws QueryFailure {		
		xmlDocument.updateString(namespaces, getAlternateTitlePath(Topic.SERVICE), alternateTitle);
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

	protected String getAbstractPath(Topic topic) {
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
	
	public String getServiceAlternateTitle() throws NotFound {
		return xmlDocument.getString(namespaces, getAlternateTitlePath(Topic.SERVICE));
	}
	
	public void setDatasetAbstract (String Abstract) throws QueryFailure {
		xmlDocument.updateString(namespaces, getAbstractPath(Topic.DATASET), Abstract);		
	}
	
	/*
	 * keywords
	 *  
	 */
	
	protected String getKeywordPath(Topic topic) {
		return getIdentificationPath(topic) +
				"/gmd:descriptiveKeywords"
				;
	}

	protected int removeKeywords(Topic topic) throws NotFound {
		return xmlDocument.removeNodes(namespaces, getKeywordPath(topic));
	}
	
	protected void addKeywords(Topic topic, Collection<String> keywords, String thesaurusTitle, String thesaurusDate, String thesaurusCodeList, String thesaurusCodeListValue) throws NotFound {		
		String keywordsPath = xmlDocument.addNode(
				namespaces, 
				getIdentificationPath(topic), 
				new String[] { 
					"gmd:resourceSpecificUsage",
					"gmd:resourceConstraints",
					"gmd:aggregationInfo",
					"srv:serviceType",
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
				"gmd:descriptiveKeywords/gmd:MD_Keywords"
			);
		
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
	
	public void addDatasetKeywords(Set<String> keywords, String thesaurusTitle, String thesaurusDate, String thesaurusCodeList, String thesaurusCodeListValue) throws NotFound {
		addKeywords(Topic.DATASET, keywords, thesaurusTitle, thesaurusDate, thesaurusCodeList, thesaurusCodeListValue);
	}
	
	public List<Keywords> getDatasetKeywords() throws NotFound{
		return getKeywords(Topic.DATASET);
	}
	
	public int removeServiceKeywords() throws NotFound {
		return removeKeywords(Topic.SERVICE);
	}
	
	public void addServiceKeywords(Collection<String> keywords, String thesaurusTitle, String thesaurusDate, String thesaurusCodeList, String thesaurusCodeListValue) throws NotFound {
		addKeywords(Topic.SERVICE, keywords, thesaurusTitle, thesaurusDate, thesaurusCodeList, thesaurusCodeListValue);
	}
	
	public interface Keywords {
		
		List<String> getKeywords();
		
		String getThesaurusTitle();
		
		String getThesaurusDate();
		
		String getThesaurusCodeList();
		
		String getThesaurusCodeListValue();
	}
	
	protected List<Keywords> getKeywords(Topic topic) {
		return xmlDocument
			.xpath(Optional.of(namespaces))
			.nodes(getKeywordPath(topic) + "/gmd:MD_Keywords").stream()
				.map(node -> (Keywords)new Keywords() {

					@Override
					public List<String> getKeywords() {
						return node.strings("gmd:keyword/gco:CharacterString");
					}
					
					private XPathHelper getThesaurusNode() {
						return node.node("gmd:thesaurusName/gmd:CI_Citation").get();
					}
					
					@Override
					public String getThesaurusTitle() {						
						return getThesaurusNode().string("gmd:title/gco:CharacterString").get();
					}
					
					private XPathHelper getThesaurusDateNode() {
						return getThesaurusNode().node("gmd:date/gmd:CI_Date").get();
					}

					@Override
					public String getThesaurusDate() {
						return getThesaurusDateNode().string("gmd:date/gco:Date").get();
					}
					
					private XPathHelper getThesaurusCodeNode() {
						return getThesaurusDateNode().node("gmd:dateType/gmd:CI_DateTypeCode").get();
					}

					@Override
					public String getThesaurusCodeList() {
						return getThesaurusCodeNode().string("@codeList").get();
					}

					@Override
					public String getThesaurusCodeListValue() {
						return getThesaurusCodeNode().string("@codeListValue").get();
					}
				})
				.collect(Collectors.toList());
	}
	
	public List<Keywords> getServiceKeywords() throws NotFound{
		return getKeywords(Topic.SERVICE);
	}
	
	/*
	 * Responsible Party
	 *  - name
	 *  - email
	 * 
	 */

	protected String getResponsiblePartyPath(Topic topic, String role) {
		return getIdentificationPath(topic) +
			"/gmd:pointOfContact" +
			"/gmd:CI_ResponsibleParty" + 

			"[gmd:role" +
			"/gmd:CI_RoleCode" +
			"/@codeListValue" +
				"='" + role + "']"
			;
	}
	
	protected String getResponsiblePartyNamePath(Topic topic, String role) {
		return getResponsiblePartyPath(topic, role) + 
			"/gmd:organisationName" +
			"/gco:CharacterString";
	}
	
	protected String getResponsiblePartyEmailPath(Topic topic, String role) {
		return getResponsiblePartyPath(topic, role) + 
			"/gmd:contactInfo" +
			"/gmd:CI_Contact" +
			"/gmd:address" +
			"/gmd:CI_Address" +
			"/gmd:electronicMailAddress" +
			"/gco:CharacterString";
	}
	
	public String getServiceResponsiblePartyName(String role) throws Exception{
		return xmlDocument.getString(namespaces, getResponsiblePartyNamePath(Topic.SERVICE, role));
	}
	
	public void setServiceResponsiblePartyName(String role, String name) throws Exception{
		xmlDocument.updateString(namespaces, getResponsiblePartyNamePath(Topic.SERVICE, role), name);
	}
	
	public String getServiceResponsiblePartyEmail(String role) throws Exception{
		return xmlDocument.getString(namespaces, getResponsiblePartyEmailPath(Topic.SERVICE, role));
	}
	
	public void setServiceResponsiblePartyEmail(String role, String email) throws Exception{
		xmlDocument.updateString(namespaces, getResponsiblePartyEmailPath(Topic.SERVICE, role), email);
	}
	
	public String getDatasetResponsiblePartyName(String role) throws Exception{
		return xmlDocument.getString(namespaces, getResponsiblePartyNamePath(Topic.DATASET, role));
	}
	
	public void setDatasetResponsiblePartyName(String role, String name) throws Exception{
		xmlDocument.updateString(namespaces, getResponsiblePartyNamePath(Topic.DATASET, role), name);
	}
	
	public String getDatasetResponsiblePartyEmail(String role) throws Exception{
		return xmlDocument.getString(namespaces, getResponsiblePartyEmailPath(Topic.DATASET, role));
	}
	
	public void setDatasetResponsiblePartyEmail(String role, String email) throws Exception{
		xmlDocument.updateString(namespaces, getResponsiblePartyEmailPath(Topic.DATASET, role), email);
	}
	
	
	/*
	 * Metadata
	 * 
	 */

	/*
	 * Metadata file identifier
	 */

	protected String getMetaDataIdentifierPath() {
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
	
	protected String getMetaDataPointOfContactPath(String role) {
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
	
	protected String getMetaDataPointOfContactNamePath(String role) {
		return getMetaDataPointOfContactPath(role) + 
			"/gmd:organisationName" +
			"/gco:CharacterString";
	}
	
	protected String getMetaDataPointOfContactEmailPath(String role) {
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
	protected String getMetaDataCreationDatePath() {
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
	protected String getAlternateTitlePath(Topic topic) {
		return getIdentificationPath(topic) +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:alternateTitle" +
				"/gco:CharacterString";
	}
	
	public String getDatasetAlternateTitle() throws NotFound {
		return xmlDocument.getString(namespaces, getAlternateTitlePath(Topic.DATASET));
	}

	public void setDatasetAlternateTitle(String newTitle) throws QueryFailure {
		xmlDocument.updateString(namespaces, getAlternateTitlePath(Topic.DATASET), newTitle);
	}
	
	/*
	 * other constraints
	 * 
	 */
	protected String getOtherconstraintsPath() {
		return getDatasetIdentificationPath() + "/gmd:resourceConstraints"
				+ "/gmd:MD_LegalConstraints/gmd:otherConstraints/gco:CharacterString";
	}
	
	public String getOtherConstraints() throws NotFound {
		return xmlDocument.getString(namespaces, getOtherconstraintsPath());		
	}
	

	/**
	 * Dataset metadata: Service Linkage
	 */	
	protected String getDigitalTransferOptionsPath(){
		return
				"/gmd:MD_Metadata" + 
				"/gmd:distributionInfo" + 
				"/gmd:MD_Distribution" + 
				"/gmd:transferOptions" + 
				"/gmd:MD_DigitalTransferOptions" ;
	}
	
	protected String getServiceLinkagePath() {
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
	
	private XPathHelper xpath() {
		return xmlDocument.xpath(Optional.of(namespaces));
	}
	
	public interface ServiceLinkage {
		
		String getURL();
		
		String getProtocol();
		
		String getName();
	}
	
	public List<ServiceLinkage> getServiceLinkage() {
		return xpath()
			.nodes(getServiceLinkagePath() + "/gmd:CI_OnlineResource").stream()
				.map(node -> (ServiceLinkage)new ServiceLinkage() {

					@Override
					public String getURL() {
						return node.string("gmd:linkage/gmd:URL").get();
					}

					@Override
					public String getProtocol() {
						return node.string("gmd:protocol/gco:CharacterString").get();
					}

					@Override
					public String getName() {
						return node.string("gmd:name/gco:CharacterString").get();
					}
					
				})
				.collect(Collectors.toList());
	}
	
	/**
	 * Dataset metadata: Dataset Identifier
	 */	
	protected String getDatasetIdentifierCodePath() {
		return 
				getDatasetIdentificationPath() +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:identifier" +
				"/gmd:MD_Identifier" +
				"/gmd:code"
				;
	}
	
	protected String getDatasetIdentifierPath() {
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
	protected String getServiceTypePath(){
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
	protected String getServiceGraphicOverviewPath() {
		return getServiceIdentificationPath() + "/gmd:graphicOverview";
	}
	
	protected String getDatasetGraphicOverviewPath() {
		return getDatasetIdentificationPath() + "/gmd:graphicOverview";
	}
	
	public int removeBrowseGraphic() throws NotFound {
		return xmlDocument.removeNodes(namespaces, getServiceGraphicOverviewPath());		
	}
	
	/**
	 * Add a new /gmd:MD_BrowseGraphic/gco:CharacterString node under gmd:graphicOverview
	 * @param fileName content of the /gmd:MD_BrowseGraphic/gco:CharacterString node
	 * @throws NotFound 
	 */
	public void addServiceBrowseGraphic(String fileName) throws NotFound {		
		xmlDocument.addNode(
				namespaces, 
				getServiceIdentificationPath (), 
				new String[] {
					"gmd:resourceFormat",
					"gmd:descriptiveKeywords",
					"gmd:resourceSpecificUsage",
					"gmd:resourceConstraints",
					"gmd:aggregationInfo",
					"srv:serviceType",
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
				"gmd:graphicOverview/gmd:MD_BrowseGraphic/gmd:fileName/gco:CharacterString", 
				fileName
			);		
	}
	
	/**
	 * Add a new /gmd:MD_BrowseGraphic/gco:CharacterString node under gmd:graphicOverview
	 * @param fileName content of the /gmd:MD_BrowseGraphic/gco:CharacterString node
	 * @throws NotFound 
	 */
	public void addDatasetBrowseGraphic(String fileName) throws NotFound {		
		xmlDocument.addNode(
				namespaces, 
				getDatasetIdentificationPath (), 
				new String[] {
					"gmd:resourceFormat",
					"gmd:descriptiveKeywords",
					"gmd:resourceSpecificUsage",
					"gmd:resourceConstraints",
					"gmd:aggregationInfo"
				},
				"gmd:graphicOverview/gmd:MD_BrowseGraphic/gmd:fileName/gco:CharacterString", 
				fileName
			);		
	}
	
	public List<String> getServiceBrowseGraphics() {
		return xpath().strings(getServiceGraphicOverviewPath () + "/gmd:MD_BrowseGraphic/gmd:fileName/gco:CharacterString");
	}
	
	public List<String> getDatasetBrowseGraphics() {
		return xpath().strings(getDatasetGraphicOverviewPath () + "/gmd:MD_BrowseGraphic/gmd:fileName/gco:CharacterString");
	}

	/**
	 * Service metadata: Service Endpoint
	 */	
	protected String getOperationsPath(){
		return getServiceIdentificationPath() + "/srv:containsOperations";
	}
	
	protected String getOperationMetadataPath(){
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
	protected String getCoupledResourcePath(){
		return getServiceIdentificationPath() + "/srv:coupledResource";
	}
	
	public int removeSVCoupledResource() throws NotFound {
		return xmlDocument.removeNodes(namespaces, getCoupledResourcePath());
	}

	/**
	 * Add info about WMS layers c.q. WFS feature types to /srv:SV_CoupledResource.
	 * @param operationName name of the service operation
	 * @param identifier identifier of dataset
	 * @param scopedName name of dataset
	 * @throws NotFound
	 */
	public void addSVCoupledResource(String operationName, String identifier, String scopedName) throws NotFound {
		String parentPath = xmlDocument.addNode (
				namespaces, 
				getServiceIdentificationPath (),
				new String[] { 
					"srv:couplingType",
					"srv:containsOperations",
					"srv:operatesOn"
				}, 
				"srv:coupledResource/srv:SV_CoupledResource"
			);
		xmlDocument.addNode(namespaces, parentPath, "srv:operationName/gco:CharacterString", operationName);
		xmlDocument.addNode(namespaces, parentPath, "srv:identifier/gco:CharacterString", identifier);
		xmlDocument.addNode(namespaces, parentPath, "gco:ScopedName", scopedName);
	}
	
	
	public String getServiceCoupledResourceOperationName() throws QueryFailure {
		return xmlDocument.getString(namespaces, getCoupledResourcePath() + "/srv:SV_CoupledResource/srv:operationName/gco:CharacterString");
	}
	
	public String getServiceCoupledResourceIdentifier() throws QueryFailure {
		return xmlDocument.getString(namespaces, getCoupledResourcePath() + "/srv:SV_CoupledResource/srv:identifier/gco:CharacterString");
	}
	
	public String getServiceCoupledResourceScopedName() throws QueryFailure {
		return xmlDocument.getString(namespaces, getCoupledResourcePath() + "/srv:SV_CoupledResource/gco:ScopedName");
	}
	
	

	
	/**
	 * Service metadata: Link to dataset 
	 */	
	protected String getOperatesOnPath(){
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
	
	public interface OperatesOn {
		
		String getUuidref();
		
		String getHref();
	}
	
	public List<OperatesOn> getOperatesOn() throws NotFound{
		return xpath()
			.nodes(getOperatesOnPath()).stream()
				.map(node -> (OperatesOn)new OperatesOn() {

					@Override
					public String getUuidref() {
						return node.string("@uuidref").get();
					}

					@Override
					public String getHref() {
						return node.string("@xlink:href").get();
					}
					
				})
				.collect(Collectors.toList());
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
	
	public void removeStylesheet() {
		xmlDocument.removeStylesheet();
	}
	
	public void setStylesheet(String stylesheet) {
		xmlDocument.setStylesheet(stylesheet);
	}
	
	public List<String> getSupplementalInformation() {
		return xpath().strings(
			"/gmd:MD_Metadata"
			+ "/gmd:identificationInfo"
			+ "/gmd:MD_DataIdentification"
			+ "/gmd:supplementalInformation"
			+ "/gco:CharacterString");
	}
	
	public void updateSupplementalInformation(String existingSupplementalInformation, String newSupplementalInformation) {
		xpath()
			.node(
				"/gmd:MD_Metadata"
				+ "/gmd:identificationInfo"
				+ "/gmd:MD_DataIdentification"
				+ "/gmd:supplementalInformation"
				+ "/gco:CharacterString[text() = '" + existingSupplementalInformation + "']")
			.ifPresent(node -> node.setTextContent(newSupplementalInformation));
	}
	
}
