package nl.idgis.publisher.metadata;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.utils.SimpleDateFormatMapper;
import nl.idgis.publisher.xml.XMLDocument;
import nl.idgis.publisher.xml.exceptions.NotFound;
import nl.idgis.publisher.xml.exceptions.QueryFailure;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class MetadataDocument {
	
	protected final XMLDocument xmlDocument;
	protected final BiMap<String, String> namespaces;
	
	public byte[] getContent() throws IOException {
		return xmlDocument.getContent();
	}
	
	public String getTitle() throws NotFound {
		return xmlDocument.getString(namespaces, 
				"/gmd:MD_Metadata" +
				"/gmd:identificationInfo" +
				"/gmd:MD_DataIdentification" +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:title" +
				"/gco:CharacterString");
	}
	
	public String getAlternateTitle() throws NotFound {
		return xmlDocument.getString(namespaces, 
				"/gmd:MD_Metadata" +
				"/gmd:identificationInfo" +
				"/gmd:MD_DataIdentification" +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:alternateTitle" +
				"/gco:CharacterString");
	}
	
	public Date getRevisionDate() throws NotFound {
		Date date = getDate("revision");
		if(date != null) {
			return date;
		}
		
		return getDate("creation");
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

	public MetadataDocument(XMLDocument xmlDocument) {
		this.xmlDocument = xmlDocument;
		
		namespaces = HashBiMap.create();
		namespaces.put("gmd", "http://www.isotc211.org/2005/gmd");
		namespaces.put("gco", "http://www.isotc211.org/2005/gco");
		namespaces.put("srv", "http://www.isotc211.org/2005/srv");
		namespaces.put("xlink", "http://www.w3.org/1999/xlink");
	}
	
	protected String getDatePath(String codeListValue) {
		return 
				"/gmd:MD_Metadata" +
				"/gmd:identificationInfo" +
				"/gmd:MD_DataIdentification" +
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
	
	/**
	 * Dataset metadata: Dataset Identifier
	 */	
	protected String getDatasetIdentifierCodePath() {
		return 
				"/gmd:MD_Metadata" +
				"/gmd:identificationInfo" +
				"/gmd:MD_DataIdentification" +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:identifier" +
				"/gmd:MD_Identifier" +
				"/gmd:code"
				;
	}
	
	protected String getDatasetIdentifierPath() {
		return 
				"/gmd:MD_Metadata" +
				"/gmd:identificationInfo" +
				"/gmd:MD_DataIdentification" +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:identifier" +
				"/gmd:MD_Identifier" +
				"/gmd:code" +
				"/gco:CharacterString"
				;
	}
	
	protected String getFileIdentifierPath() {
		return 
				"/gmd:MD_Metadata" +
				"/gmd:fileIdentifier" +				
				"/gco:CharacterString"
				;
	}
	
	public void setDatasetIdentifier(String datasetIdentifier) throws QueryFailure {
		xmlDocument.updateString(namespaces, getDatasetIdentifierPath(), datasetIdentifier);
	}
	
	public void setFileIdentifier(String fileIdentifier) throws QueryFailure {
		xmlDocument.updateString(namespaces, getFileIdentifierPath(), fileIdentifier);
	}
	
	/**
	 * Service metadata: serviceType
	 */	
	protected String getServiceIdentificationPath(){
		return "/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification";
	}
	
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
	
	/**
	 * Service metadata: BrowseGraphic
	 */	
	protected String getGraphicOverviewPath() {
		return "/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/gmd:graphicOverview";
	}
	
	protected String getBrowseGraphicPath() {
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
	
	/**
	 * Service metadata: Service Endpoint
	 */	
	protected String getOperationsPath(){
		return "/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/srv:containsOperations";
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
	
	/**
	 * Service metadata: CoupledResource (WMS layers c.q. WFS feature types) 
	 */
	protected String getCoupledResourcePath(){
		return "/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/srv:coupledResource";
	}
	
	protected String getSVCoupledResourcePath(){
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
	
	@Override
	public MetadataDocument clone() {
		return new MetadataDocument(xmlDocument.clone());
	}
	
}
