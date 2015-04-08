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
 * Represents the metadata document of a service according to ISO 19119.
 * @author Rob
 *
 */
public class ServiceMetadataDocument extends MetadataDocument {
	
	public ServiceMetadataDocument(XMLDocument xmlDocument) {
		super(xmlDocument);	
	}	
	
	/*
	 * Resource Title
	 */
	protected String getTitlePath() {
		return 
			"/gmd:MD_Metadata" +
			"/gmd:identificationInfo" +
			"/gmd:MD_DataIdentification" +
			"/gmd:citation" +
			"/gmd:CI_Citation" +
			"/gmd:title" +
			"/gco:CharacterString";
	}
	
	public String getTitle() throws NotFound {
		return xmlDocument.getString(namespaces, getTitlePath());
	}
	
	public void setTitle (String title) throws QueryFailure {
		xmlDocument.updateString(namespaces, getTitlePath(), title);		
	}
	
	/*
	 * Resource Abstract
	 */
	protected String getAbstractPath() {
		return 
			"/gmd:MD_Metadata" +
			"/gmd:identificationInfo" +
			"/gmd:MD_DataIdentification" +
			"/gmd:citation" +
			"/gmd:CI_Citation" +
			"/gmd:Abstract" +
			"/gco:CharacterString";
	}
	
	public String getAbstract() throws NotFound {
		return xmlDocument.getString(namespaces, getAbstractPath());
	}
	
	public void setAbstract (String Abstract) throws QueryFailure {
		xmlDocument.updateString(namespaces, getAbstractPath(), Abstract);		
	}
	
	
	/*
	 * Resource Locator
	 * 
	 * @see public int removeServiceLinkage() throws Exception
	 * @see public void addServiceLinkage(String linkage, String protocol, String name) throws NotFound
	 */
	
	public int removeLocator() throws Exception{
		return removeServiceLinkage();
	}
	
	public void addLocator(String linkage, String protocol, String name) throws NotFound{
		addServiceLinkage(linkage, protocol, name);
	}
	
	/*
	 * srv:SV_OperationMetadata/srv:containsOperations
	 * 
	 * Operation name
	 * DCP
	 * Connect point linkage
	 * 
	 * (non-Javadoc)
	 * @see nl.idgis.publisher.metadata.MetadataDocument#removeServiceEndpoint()
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
	 * (non-Javadoc)
	 * @see nl.idgis.publisher.metadata.MetadataDocument#getRevisionDate()
	 */
	
	public int removeCoupledResource() throws NotFound {
		return removeOperatesOn();
	}

	public void addCoupledResource(String uuidref, String href) throws NotFound {
		addOperatesOn(uuidref, href); 		
	}
	
	
	/*
	 * keywords
	 *  
	 * (non-Javadoc)
	 * @see nl.idgis.publisher.metadata.MetadataDocument#getRevisionDate()
	 */
	
	protected String getKeywordPath() {
		return 
				"/gmd:MD_Metadata" +
				"/gmd:identificationInfo" +
				"/srv:SV_ServiceIdentification" +
				"/gmd:descriptiveKeywords" +
				"/<gmd:MD_Keywords>"
				;
	}

	public int removeKeywords() throws NotFound {
		return xmlDocument.removeNodes(namespaces, getKeywordPath());
	}
	
	public void addKeywords(List<String> keywords, String thesaurusTitle, String thesaurusDate, String thesaurusCodeList, String thesaurusCodeListValue) throws NotFound {	
		String parentPath = getKeywordPath();		
		for (String keyword : keywords) {
			xmlDocument.addNode(namespaces, parentPath, "gmd:keyword/gco:CharacterString", keyword);
		}
		String thesaurusPath = xmlDocument.addNode(namespaces, parentPath, "gmd:thesaurusName/gmd:CI_Citation");
		xmlDocument.addNode(namespaces, thesaurusPath, "gmd:title/gco:CharacterString", thesaurusTitle);
		
		String thesaurusDatePath = xmlDocument.addNode(namespaces, thesaurusPath, "gmd:date/gmd:CI_Date");

		xmlDocument.addNode(namespaces, thesaurusDatePath, "gmd:date/gco:Date", thesaurusDate);
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("codeList", thesaurusCodeList);
		attributes.put("codeListValue", thesaurusCodeListValue);		
		
		xmlDocument.addNode(namespaces, thesaurusDatePath, "gmd:dateType/gmd:CI_DateTypeCode", attributes);
	}
	
	/*
	 * Dates of publication, revision, creation
	 * 
	 * (non-Javadoc)
	 * @see nl.idgis.publisher.metadata.MetadataDocument#getDatePath(java.lang.String)
	 */
	
	protected String getDatePath(String codeListValue) {
		return 
			"/gmd:MD_Metadata" +
			"/gmd:identificationInfo" +
			"/srv:SV_ServiceIdentification" +
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
	

	public void setDate(String codeListValue, Date date) throws Exception{
		
		String datePath = getDatePath(codeListValue);
		
		String dateString = dateToString("yyyy-MM-dd", date);
		
		xmlDocument.updateString(namespaces, datePath, dateString);
	}
	
	public void setDate(String codeListValue, Timestamp ts) throws Exception{
		Date date = new Date(ts.getTime());
		setDate(codeListValue, date);
	}
	
	
	protected Date getDate(String codeListValue) throws NotFound {
		String datePath = getDatePath(codeListValue);		
		
		Date date = null; 
		
		try { 
			String dateString = xmlDocument.getString(namespaces, datePath + "/gco:DateTime");
			date = SimpleDateFormatMapper.isoDateTime().apply(dateString);
		} catch(NotFound nf) {}
		
		if(date == null) {		
			String dateString = xmlDocument.getString(namespaces, datePath + "/gco:Date");		
			date = SimpleDateFormatMapper.isoDate().apply(dateString);
		}
		
		return date;
	}

	/*
	 * Responsible Party
	 *  - name
	 *  - email
	 * 
	 * (non-Javadoc)
	 * @see nl.idgis.publisher.metadata.MetadataDocument#getDatePath(java.lang.String)
	 */
	protected String getResponsiblePartyPath() {
		return 
			"/gmd:MD_Metadata" +
			"/gmd:identificationInfo" +
			"/srv:SV_ServiceIdentification" +
			"/gmd:pointOfContact" +
			"/gmd:CI_ResponsibleParty" 
			;
	}
	
	protected String getResponsiblePartyNamePath() {
		return getResponsiblePartyPath() + 
			"/gmd:organisationName" +
			"/gco:CharacterString";
	}
	
	protected String getResponsiblePartyEmailPath() {
		return getResponsiblePartyPath() + 
			"/gmd:contactInfo" +
			"/gmd:CI_Contact" +
			"/gmd:address" +
			"/gmd:CI_Address" +
			"/gmd:electronicMailAddress" +
			"/gco:CharacterString";
	}
	
	public String getResponsiblePartyName() throws Exception{
		return xmlDocument.getString(namespaces, getResponsiblePartyNamePath());
	}
	
	public String getResponsiblePartyEmail() throws Exception{
		return xmlDocument.getString(namespaces, getResponsiblePartyEmailPath());
	}
	
	public void setResponsiblePartyName(String name) throws Exception{
		xmlDocument.updateString(namespaces, getResponsiblePartyNamePath(), name);
	}
	
	public void setResponsiblePartyEmail(String email) throws Exception{
		xmlDocument.updateString(namespaces, getResponsiblePartyEmailPath(), email);
	}
	
	
	/*
	 * Metadata
	 * 
	 */

	/*
	 * identifier
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
	 * PointOfContact
	 */
	protected String getMetaDataPointOfContactPath() {
		return 
			"/gmd:MD_Metadata" +
			"/gmd:contact" +
			"/gmd:CI_ResponsibleParty" 
			;
	}
	
	protected String getMetaDataPointOfContactNamePath() {
		return getMetaDataPointOfContactPath() + 
			"/gmd:organisationName" +
			"/gco:CharacterString";
	}
	
	protected String getMetaDataPointOfContactEmailPath() {
		return getMetaDataPointOfContactPath() + 
			"/gmd:contactInfo" +
			"/gmd:CI_Contact" +
			"/gmd:address" +
			"/gmd:CI_Address" +
			"/gmd:electronicMailAddress" +
			"/gco:CharacterString";
	}
	
	public String getMetaDataPointOfContactName() throws Exception{
		return xmlDocument.getString(namespaces, getMetaDataPointOfContactNamePath());
	}
	
	public String getMetaDataPointOfContactEmail() throws Exception{
		return xmlDocument.getString(namespaces, getMetaDataPointOfContactEmailPath());
	}
	
	public void setMetaDataPointOfContactName(String name) throws Exception{
		xmlDocument.updateString(namespaces, getMetaDataPointOfContactPath(), name);
	}
	
	public void setMetaDataPointOfContactEmail(String email) throws Exception{
		xmlDocument.updateString(namespaces, getMetaDataPointOfContactPath(), email);
	}
	

	/*
	 * creation date
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
		xmlDocument.updateString(namespaces, getMetaDataCreationDatePath(), dateToString("yyyy-MM-dd", date));
	}

	
}
