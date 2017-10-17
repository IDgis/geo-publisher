package nl.idgis.publisher.metadata;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	protected static final String PUBLICATION = "publication";
	
	public static enum Topic {DATASET, SERVICE};
	
	protected static final String [] ROLE_CODES = {"owner","pointOfContact"}; 
	
	protected final XMLDocument isoMetadata;
	
	protected final XMLDocument featureCatalogue;
	
	protected final BiMap<String, String> namespaces;
	
	public MetadataDocument(XMLDocument xmlDocument) {
		namespaces = HashBiMap.create();
		namespaces.put("gmd", "http://www.isotc211.org/2005/gmd");
		namespaces.put("gco", "http://www.isotc211.org/2005/gco");
		namespaces.put("srv", "http://www.isotc211.org/2005/srv");
		namespaces.put("xlink", "http://www.w3.org/1999/xlink");
		
		String rootNode = "/gmd:MD_Metadata";
		if(xmlDocument.xpath(Optional.of(namespaces)).node(rootNode).isPresent()) {
			this.isoMetadata = xmlDocument;
		} else {
			this.isoMetadata = xmlDocument.clone(namespaces, "/" + rootNode);
		}
		
		String featureCataloguePath = "//FC_FeatureCatalogue";
		if(xmlDocument.xpath(Optional.empty()).node(featureCataloguePath).isPresent()) {
			this.featureCatalogue = xmlDocument.clone(null, featureCataloguePath); 
		} else {
			this.featureCatalogue = null;
		}
	}
	
	@Override
	public MetadataDocument clone() {
		return new MetadataDocument(isoMetadata.clone());
	}

	/*
	 * generic methods
	 */
	
	public byte[] getContent() throws IOException {
		return isoMetadata.getContent();
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
			String dateString = isoMetadata.getString(namespaces, datePath + "/gco:DateTime");
			return SimpleDateFormatMapper.isoDateTime().apply(dateString);
		} catch(NotFound nf) {
			String dateString = isoMetadata.getString(namespaces, datePath + "/gco:Date");
			return SimpleDateFormatMapper.isoDate().apply(dateString);
		}
	}

	public void setDate(Topic topic, String codeListValue, Date date) throws Exception{
		
		String datePath = getDatePath(topic, codeListValue);
		
		String dateString = dateToString(METADATA_DATE_PATTERN, date);
		
		isoMetadata.updateString(namespaces, datePath + "/gco:Date", dateString);
	}
	
	public void setDate(Topic topic,String codeListValue, Timestamp ts) throws Exception{
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
	
	public Date getServicePublicationDate() throws NotFound {
		try { 
			return getDate(Topic.SERVICE, PUBLICATION);
		} catch(NotFound nf) {
			return getDate(Topic.SERVICE, PUBLICATION);
		}
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
	
	public Date getDatasetPublicationDate() throws NotFound {
		try { 
			return getDate(Topic.DATASET, PUBLICATION);
		} catch(NotFound nf) {
			return getDate(Topic.DATASET, PUBLICATION);
		}
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
		return isoMetadata.getString(namespaces, getTitlePath(Topic.SERVICE));
	}
	
	public void setServiceTitle (String title) throws QueryFailure {
		isoMetadata.updateString(namespaces, getTitlePath(Topic.SERVICE), title);		
	}
	
	public void setServiceAlternateTitle(String alternateTitle) throws QueryFailure {		
		isoMetadata.updateString(namespaces, getAlternateTitlePath(Topic.SERVICE), alternateTitle);
	}
	
	public String getDatasetTitle() throws NotFound {
		return isoMetadata.getString(namespaces, getTitlePath(Topic.DATASET));
	}
	
	public void setDatasetTitle (String title) throws QueryFailure {
		isoMetadata.updateString(namespaces, getTitlePath(Topic.DATASET), title);		
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
		return isoMetadata.getString(namespaces, getAbstractPath(Topic.SERVICE));
	}
	
	public void setServiceAbstract (String Abstract) throws QueryFailure {
		isoMetadata.updateString(namespaces, getAbstractPath(Topic.SERVICE), Abstract);		
	}
	
	public String getDatasetAbstract() throws NotFound {
		return isoMetadata.getString(namespaces, getAbstractPath(Topic.DATASET));
	}
	
	public String getServiceAlternateTitle() throws NotFound {
		return isoMetadata.getString(namespaces, getAlternateTitlePath(Topic.SERVICE));
	}
	
	public void setDatasetAbstract (String Abstract) throws QueryFailure {
		isoMetadata.updateString(namespaces, getAbstractPath(Topic.DATASET), Abstract);		
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
		return isoMetadata.removeNodes(namespaces, getKeywordPath(topic));
	}
	
	protected void addKeywords(Topic topic, Collection<String> keywords, String thesaurusTitle, String thesaurusDate, String thesaurusCodeList, String thesaurusCodeListValue) throws NotFound {		
		String keywordsPath = isoMetadata.addNode(
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
			isoMetadata.addNode(namespaces, keywordsPath, "gmd:keyword/gco:CharacterString", keyword);
		}
		String thesaurusPath = isoMetadata.addNode(namespaces, keywordsPath, "gmd:thesaurusName/gmd:CI_Citation");
		isoMetadata.addNode(namespaces, thesaurusPath, "gmd:title/gco:CharacterString", thesaurusTitle);
		
		String thesaurusDatePath = isoMetadata.addNode(namespaces, thesaurusPath, "gmd:date/gmd:CI_Date");

		isoMetadata.addNode(namespaces, thesaurusDatePath, "gmd:date/gco:Date", thesaurusDate);
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("codeList", thesaurusCodeList);
		attributes.put("codeListValue", thesaurusCodeListValue);		
		
		isoMetadata.addNode(namespaces, thesaurusDatePath, "gmd:dateType/gmd:CI_DateTypeCode", attributes);
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
		return isoMetadata
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
	 * Topic
	 */
	
	public List<String> getTopicCategories() throws NotFound {
		return isoMetadata
			.xpath(Optional.of(namespaces))
			.strings("/gmd:MD_Metadata" +
					"/gmd:identificationInfo" +	
					"/gmd:MD_DataIdentification" +
					"/gmd:topicCategory" +
					"/gmd:MD_TopicCategoryCode");
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
		return isoMetadata.getString(namespaces, getResponsiblePartyNamePath(Topic.SERVICE, role));
	}
	
	public void setServiceResponsiblePartyName(String role, String name) throws Exception{
		isoMetadata.updateString(namespaces, getResponsiblePartyNamePath(Topic.SERVICE, role), name);
	}
	
	public String getServiceResponsiblePartyEmail(String role) throws Exception{
		return isoMetadata.getString(namespaces, getResponsiblePartyEmailPath(Topic.SERVICE, role));
	}
	
	public void setServiceResponsiblePartyEmail(String role, String email) throws Exception{
		isoMetadata.updateString(namespaces, getResponsiblePartyEmailPath(Topic.SERVICE, role), email);
	}
	
	public String getDatasetResponsiblePartyName(String role) throws Exception{
		return isoMetadata.getString(namespaces, getResponsiblePartyNamePath(Topic.DATASET, role));
	}
	
	public void setDatasetResponsiblePartyName(String role, String name) throws Exception{
		isoMetadata.updateString(namespaces, getResponsiblePartyNamePath(Topic.DATASET, role), name);
	}
	
	public String getDatasetResponsiblePartyEmail(String role) throws Exception{
		return isoMetadata.getString(namespaces, getResponsiblePartyEmailPath(Topic.DATASET, role));
	}
	
	public void setDatasetResponsiblePartyEmail(String role, String email) throws Exception{
		isoMetadata.updateString(namespaces, getResponsiblePartyEmailPath(Topic.DATASET, role), email);
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
	
	public String getMetadataStandardName() throws NotFound {
		return isoMetadata.getString(namespaces, 
				"/gmd:MD_Metadata/gmd:metadataStandardName/gco:CharacterString");
	}
	
	public String getMetaDataIdentifier() throws Exception{
		return isoMetadata.getString(namespaces, getMetaDataIdentifierPath());
	}
	
	public void setMetaDataIdentifier(String identifier) throws Exception{
		isoMetadata.updateString(namespaces, getMetaDataIdentifierPath(), identifier);
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
		return isoMetadata.getString(namespaces, getMetaDataPointOfContactNamePath(role));
	}
	
	public String getMetaDataPointOfContactEmail(String role) throws Exception{
		return isoMetadata.getString(namespaces, getMetaDataPointOfContactEmailPath(role));
	}
	
	public void setMetaDataPointOfContactName(String role, String name) throws Exception{
		isoMetadata.updateString(namespaces, getMetaDataPointOfContactNamePath(role), name);
	}
	
	public void setMetaDataPointOfContactEmail(String role, String email) throws Exception{
		isoMetadata.updateString(namespaces, getMetaDataPointOfContactEmailPath(role), email);
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
		return isoMetadata.getString(namespaces, getMetaDataCreationDatePath());
	}
	
	public void setMetaDataCreationDate(Date date) throws Exception{
		isoMetadata.updateString(namespaces, getMetaDataCreationDatePath(), dateToString(METADATA_DATE_PATTERN, date));
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
		return isoMetadata.getString(namespaces, getAlternateTitlePath(Topic.DATASET));
	}

	public void setDatasetAlternateTitle(String newTitle) throws QueryFailure {
		isoMetadata.updateString(namespaces, getAlternateTitlePath(Topic.DATASET), newTitle);
	}

	public void addDatasetAlternateTitle(String name) throws NotFound{
		if(name != null && !"".equals(name.trim())) {
			isoMetadata.addNode(namespaces, 
					getDatasetIdentificationPath() + "/gmd:citation/gmd:CI_Citation", 
					"gmd:alternateTitle/gco:CharacterString", 
					name);
		}
	}
	
	public String getMaintenanceFrequencyCodeListValue() throws NotFound {
		return isoMetadata.getString(namespaces, getDatasetIdentificationPath() + 
				"/gmd:resourceMaintenance"
				+ "/gmd:MD_MaintenanceInformation"
				+ "/gmd:maintenanceAndUpdateFrequency"
				+ "/gmd:MD_MaintenanceFrequencyCode/"
				+ "@codeListValue");
	}
	
	protected String getUseLimitationsPath() {
		return getDatasetIdentificationPath() + "/gmd:resourceConstraints"
				+ "/gmd:MD_Constraints/gmd:useLimitation/gco:CharacterString";
	}
	
	public List<String> getUseLimitations() throws NotFound {
		return isoMetadata
			.xpath(Optional.of(namespaces))
			.strings(getUseLimitationsPath());
	}
	
	/*
	 * Get values of path from argument
	 */
	public List<String> getMetadataElements(String path) throws NotFound {
		return isoMetadata
			.xpath(Optional.of(namespaces))
			.strings(path);
	}
	
	/*
	 * other constraints
	 * 
	 */
	protected String getOtherconstraintsPath() {
		return getDatasetIdentificationPath() + "/gmd:resourceConstraints"
				+ "/gmd:MD_LegalConstraints/gmd:otherConstraints/gco:CharacterString";
	}
	
	public List<String> getOtherConstraints() throws NotFound {
		return isoMetadata
			.xpath(Optional.of(namespaces))
			.strings(getOtherconstraintsPath());
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
	
	/**
	 * Dataset Distribution
	 * : distributor
	 */	
	protected String getDistributionResponsiblePartyPath(String role){
		return
				"/gmd:MD_Metadata" + 
				"/gmd:distributionInfo" + 
				"/gmd:MD_Distribution" + 
				"/gmd:distributor" + 
				"/gmd:MD_Distributor" + 
				"/gmd:distributorContact" +
				"/gmd:CI_ResponsibleParty" + 

			"[gmd:role" +
			"/gmd:CI_RoleCode" +
			"/@codeListValue" +
				"='" + role + "']"
			;
	}
	
	protected String getDistributionResponsiblePartyEmailPath(String role) {
		return getDistributionResponsiblePartyPath(role) + 
				"/gmd:contactInfo" +
				"/gmd:CI_Contact" +
				"/gmd:address" +
				"/gmd:CI_Address" +
				"/gmd:electronicMailAddress" +
				"/gco:CharacterString";
	}
	
	protected String getDistributionResponsiblePartyNamePath(String role) {
		return getDistributionResponsiblePartyPath(role) + 
			"/gmd:organisationName" +
			"/gco:CharacterString";
	}
	
	public String getDistributionResponsiblePartyEmail(String role) throws Exception{
		return isoMetadata.getString(namespaces, getDistributionResponsiblePartyEmailPath(role));
	}
	
	public String getDistributionResponsiblePartyName(String role) throws Exception{
		return isoMetadata.getString(namespaces, getDistributionResponsiblePartyNamePath(role));
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
		return isoMetadata.removeNodes(namespaces, getServiceLinkagePath());
	}
	
	/**
	 * Add a new /gmd:onLine/gmd:CI_OnlineResource node under gmd:MD_DigitalTransferOptions
	 * @param linkage content of the /gmd:linkage/gmd:URL node
	 * @param protocol content of the /gmd:protocol/gco:CharacterString node
	 * @param name content of the /gmd:name/gco:CharacterString node
	 * @throws NotFound 
	 */
	public void addServiceLinkage(String linkage, String protocol, String name) throws NotFound{
		String parentPath = isoMetadata.addNode(namespaces, getDigitalTransferOptionsPath(), new String[]{"gmd:offLine"}, "gmd:onLine/gmd:CI_OnlineResource");
		isoMetadata.addNode(namespaces, parentPath, "gmd:linkage/gmd:URL", linkage);
		isoMetadata.addNode(namespaces, parentPath, "gmd:protocol/gco:CharacterString", protocol);
		if(name != null) {
			isoMetadata.addNode(namespaces, parentPath, "gmd:name/gco:CharacterString", name);
		}
	}
	
	protected XPathHelper xpath() {
		return isoMetadata.xpath(Optional.of(namespaces));
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
		isoMetadata.updateString(namespaces, getDatasetIdentifierPath(), datasetIdentifier);
	}
	
	public String getDatasetIdentifier() throws QueryFailure {
		return isoMetadata.getString(namespaces, getDatasetIdentifierPath());
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
		return isoMetadata.removeNodes(namespaces, getServiceTypePath());		
	}
	
	/**
	 * Add a new /srv:serviceType/gco:LocalName node under srv:SV_ServiceIdentification
	 * @param serviceLocalName content of the /srv:serviceType/gco:LocalName node
	 * @throws NotFound 
	 */
	public void addServiceType(String serviceLocalName) throws NotFound{		
		isoMetadata.addNode(namespaces, getServiceIdentificationPath(), 
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
		return isoMetadata.getString(namespaces, getServiceTypePath() + "/gco:LocalName");
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
		return isoMetadata.removeNodes(namespaces, getServiceGraphicOverviewPath());		
	}
	
	/**
	 * Add a new /gmd:MD_BrowseGraphic/gco:CharacterString node under gmd:graphicOverview
	 * @param fileName content of the /gmd:MD_BrowseGraphic/gco:CharacterString node
	 * @throws NotFound 
	 */
	public void addServiceBrowseGraphic(String fileName) throws NotFound {		
		isoMetadata.addNode(
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
		isoMetadata.addNode(
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
	
	public void updateDatasetBrowseGraphic(String currentBrowseGraphic, String newBrowseGraphic) {
		xpath()
			.nodes(
				getDatasetGraphicOverviewPath()
				+ "/gmd:MD_BrowseGraphic"
				+ "/gmd:fileName"
				+ "/gco:CharacterString")
			.stream()
			.filter(node -> node.string().map(str -> str.equals(currentBrowseGraphic)).orElse(false))
			.findAny()
			.ifPresent(node -> node.setTextContent(newBrowseGraphic));
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
		return isoMetadata.removeNodes(namespaces, getOperationMetadataPath());
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
		String parentPath = isoMetadata.addNode(namespaces, getOperationsPath(), "srv:SV_OperationMetadata");		
		
		isoMetadata.addNode(namespaces, parentPath, "srv:operationName/gco:CharacterString", operationName);
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("codeList", codeList);
		attributes.put("codeListValue", codeListValue);
		isoMetadata.addNode(namespaces, parentPath, "srv:DCP/srv:DCPList", attributes);
		
		isoMetadata.addNode(namespaces, parentPath, "srv:connectPoint/gmd:CI_OnlineResource/gmd:linkage/gmd:URL", linkage);
	}
	
	public String getServiceEndpointOperationName() throws QueryFailure {
		return isoMetadata.getString(namespaces, getOperationsPath() + "/srv:SV_OperationMetadata/srv:operationName/gco:CharacterString");
	}
	
	public String getServiceEndpointCodeList() throws QueryFailure {
		return isoMetadata.getString(namespaces, getOperationsPath() + "/srv:SV_OperationMetadata/srv:DCP/srv:DCPList/@codeList");
	}
	
	public String getServiceEndpointCodeListValue() throws QueryFailure {
		return isoMetadata.getString(namespaces, getOperationsPath() + "/srv:SV_OperationMetadata/srv:DCP/srv:DCPList/@codeListValue");
	}
	
	public String getServiceEndpointUrl() throws QueryFailure {
		return isoMetadata.getString(namespaces, getOperationsPath() + "/srv:SV_OperationMetadata/srv:connectPoint/gmd:CI_OnlineResource/gmd:linkage/gmd:URL");
	}
	

	/**
	 * Service metadata: CoupledResource (WMS layers c.q. WFS feature types) 
	 */
	protected String getCoupledResourcePath(){
		return getServiceIdentificationPath() + "/srv:coupledResource";
	}
	
	public int removeSVCoupledResource() throws NotFound {
		return isoMetadata.removeNodes(namespaces, getCoupledResourcePath());
	}

	/**
	 * Add info about WMS layers c.q. WFS feature types to /srv:SV_CoupledResource.
	 * @param operationName name of the service operation
	 * @param identifier identifier of dataset
	 * @param scopedName name of dataset
	 * @throws NotFound
	 */
	public void addSVCoupledResource(String operationName, String identifier, String scopedName) throws NotFound {
		String parentPath = isoMetadata.addNode (
				namespaces, 
				getServiceIdentificationPath (),
				new String[] { 
					"srv:couplingType",
					"srv:containsOperations",
					"srv:operatesOn"
				}, 
				"srv:coupledResource/srv:SV_CoupledResource"
			);
		isoMetadata.addNode(namespaces, parentPath, "srv:operationName/gco:CharacterString", operationName);
		isoMetadata.addNode(namespaces, parentPath, "srv:identifier/gco:CharacterString", identifier);
		isoMetadata.addNode(namespaces, parentPath, "gco:ScopedName", scopedName);
	}
	
	
	public String getServiceCoupledResourceOperationName() throws QueryFailure {
		return isoMetadata.getString(namespaces, getCoupledResourcePath() + "/srv:SV_CoupledResource/srv:operationName/gco:CharacterString");
	}
	
	public String getServiceCoupledResourceIdentifier() throws QueryFailure {
		return isoMetadata.getString(namespaces, getCoupledResourcePath() + "/srv:SV_CoupledResource/srv:identifier/gco:CharacterString");
	}
	
	public String getServiceCoupledResourceScopedName() throws QueryFailure {
		return isoMetadata.getString(namespaces, getCoupledResourcePath() + "/srv:SV_CoupledResource/gco:ScopedName");
	}
	
	

	
	/**
	 * Service metadata: Link to dataset 
	 */	
	protected String getOperatesOnPath(){
		return getServiceIdentificationPath() + "/srv:operatesOn";
	}
	
	public int removeOperatesOn() throws NotFound {
		return isoMetadata.removeNodes(namespaces, getOperatesOnPath());
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
		
		isoMetadata.addNode(namespaces, getServiceIdentificationPath(), "srv:operatesOn", attributes);
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
		isoMetadata.removeStylesheet();
	}
	
	public void setStylesheet(String stylesheet) {
		isoMetadata.setStylesheet(stylesheet);
	}
	
	public List<String> getSpatialSchema() {
		return xpath().strings(
			"/gmd:MD_Metadata"
			+ "/gmd:identificationInfo"
			+ "/gmd:MD_DataIdentification"
			+ "/gmd:spatialRepresentationType"
			+ "/gmd:MD_SpatialRepresentationTypeCode"
			+ "/@codeListValue");
	}
	
	public List<String> getSupplementalInformation() {
		return xpath().strings(
			"/gmd:MD_Metadata"
			+ "/gmd:identificationInfo"
			+ "/gmd:MD_DataIdentification"
			+ "/gmd:supplementalInformation"
			+ "/gco:CharacterString");
	}
	
	public void removeSupplementalInformation(String supplementalInformation) {
		xpath()
			.nodes(
				"/gmd:MD_Metadata"
				+ "/gmd:identificationInfo"
				+ "/gmd:MD_DataIdentification"
				+ "/gmd:supplementalInformation"
				+ "/gco:CharacterString")
			.stream()
			.filter(node -> node.string().map(str -> str.equals(supplementalInformation)).orElse(false))
			.forEach(XPathHelper::remove);
	}
	
	public void updateSupplementalInformation(String existingSupplementalInformation, String newSupplementalInformation) {
		xpath()
			.nodes(
				"/gmd:MD_Metadata"
				+ "/gmd:identificationInfo"
				+ "/gmd:MD_DataIdentification"
				+ "/gmd:supplementalInformation"
				+ "/gco:CharacterString")
			.stream()
			.filter(node -> node.string().map(str -> str.equals(existingSupplementalInformation)).orElse(false))
			.findAny()
			.ifPresent(node -> node.setTextContent(newSupplementalInformation));
	}
	
	public void removeAdditionalPointOfContacts() {
		xpath()
			.nodes(
				"/gmd:MD_Metadata"
				+ "/gmd:identificationInfo"
				+ "/gmd:MD_DataIdentification"
				+ "/gmd:pointOfContact")
			.stream()
			.skip(1)
			.forEach(XPathHelper::remove);
	}
	
	private void addLineage(String path, String description) throws NotFound {
		String lineagePath = path + 
				"/gmd:lineage"
				+ "/gmd:LI_Lineage";
		
		if(!xpath()
			.node(lineagePath)
			.isPresent()) {
			isoMetadata.addNode(
					namespaces, 
					path,
					"gmd:lineage"
						+ "/gmd:LI_Lineage");
		}
		
		String processStepDescriptionPath = isoMetadata.addNode(
			namespaces, 
			lineagePath,
			new String[]{
				"gmd:source"
			},
			"gmd:processStep"
				+ "/gmd:LI_ProcessStep"
				+ "/gmd:description"
				+ "/gco:CharacterString",
			description);
		
		Map<String, String> roleCodeAttr = new HashMap<>();
		roleCodeAttr.put("codeList", "./resources/codeList.xml#CI_RoleCode");
		roleCodeAttr.put("codeListValue", "processor");
		
		String processStepPath = processStepDescriptionPath 
			+ "/ancestor::gmd:LI_ProcessStep";
		
		isoMetadata.addNode(
			namespaces, 
			processStepPath,
			"gmd:processor"
				+ "/gmd:CI_ResponsibleParty"
				+ "/gmd:role"
				+ "/gmd:CI_RoleCode",
			roleCodeAttr);
	}
	
	public void addProcessStep(String description) throws NotFound {
		String dataQualityPath = "/gmd:MD_Metadata"
		+ "/gmd:dataQualityInfo"
		+ "/gmd:DQ_DataQuality"
			+ "[gmd:scope"
				+ "/gmd:DQ_Scope"
				+ "/gmd:level"
				+ "/gmd:MD_ScopeCode"
					+ "[text() = 'dataset'"
					+ "	and @codeList='./resources/codeList.xml#MD_ScopeCode'"
					+ " and @codeListValue='dataset']]";
		
		if(xpath().nodes(dataQualityPath).isEmpty()) {
			String dataQualityInfoPath = isoMetadata.addNode(
					namespaces,
					"/gmd:MD_Metadata",
					new String[]{
							"gmd:portrayalCatalogueInfo",
							"gmd:metadataConstraints",
							"gmd:applicationSchemaInfo",
							"gmd:metadataMaintenance",
							"gmd:series",
							"gmd:describes",
							"gmd:propertyType",
							"gmd:featureType",
							"gmd:featureAttribute"},
					"gmd:dataQualityInfo");
			
			Map<String, String> scopeCodeAttr = new HashMap<>();
			scopeCodeAttr.put("codeList", "./resources/codeList.xml#MD_ScopeCode");
			scopeCodeAttr.put("codeListValue", "dataset");
			
			isoMetadata.addNode(
				namespaces, 
				dataQualityInfoPath, 
				"gmd:DQ_DataQuality"
					+ "/gmd:scope"
					+ "/gmd:DQ_Scope"
					+ "/gmd:level"
					+ "/gmd:MD_ScopeCode", 
					"dataset",
					scopeCodeAttr);
			
			addLineage(
				dataQualityInfoPath 
					+ "/gmd:DQ_DataQuality",
				description);
		} else {
			addLineage(dataQualityPath, description);
		}
	}

	public Map<String, String> getAttributeAliases() {
		if(featureCatalogue == null) {
			return Collections.emptyMap();
		} else {
			Map<String, String> retval = new HashMap<>();
			
			featureCatalogue
				.xpath(Optional.empty())
				.nodes("FC_FeatureCatalogue/featureType/featureAttribute")
				.stream()
				.forEach(attr ->
					attr.string("name").ifPresent(name ->
					attr.string("definition").ifPresent(definition -> 
						retval.put(name, definition))));
			
			return Collections.unmodifiableMap(retval);
		}
	}
	
	public String getDatasetSpatialRepresentationType() {
		return xpath()
			.string("/gmd:MD_Metadata"
					+ "/gmd:identificationInfo"
					+ "/gmd:MD_DataIdentification"
					+ "/gmd:spatialRepresentationType"
					+ "/gmd:MD_SpatialRepresentationTypeCode"
					+ "/@codeListValue")
			.get();
	}
	
	public Double getDatasetSpatialExtentSide(String side) throws NotFound {
		// side must be west, east, north or south
		return Double.valueOf(xpath()
				.string("/gmd:MD_Metadata"
						+ "/gmd:identificationInfo"
						+ "/gmd:MD_DataIdentification"
						+ "/gmd:extent"
						+ "/gmd:EX_Extent"
						+ "/gmd:geographicElement"
						+ "/gmd:EX_GeographicBoundingBox"
						+ "/gmd:"+side+"Bound"+((side == "west" || side == "east") ? "Longitude" : "Latitude") 
						+ "/gco:Decimal")
				.get());
	}
	
	public String getDatasetSpatialExtent() throws NotFound {
		String west = String.valueOf(getDatasetSpatialExtentSide("west"));
		String east = String.valueOf(getDatasetSpatialExtentSide("east"));
		String north = String.valueOf(getDatasetSpatialExtentSide("north"));
		String south = String.valueOf(getDatasetSpatialExtentSide("south"));
		
		return west+","+south+","+east+","+north;
	}
}
