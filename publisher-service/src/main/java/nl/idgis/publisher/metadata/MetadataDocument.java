package nl.idgis.publisher.metadata;

import java.io.IOException;
import java.util.Date;

import nl.idgis.publisher.utils.SimpleDateFormatMapper;
import nl.idgis.publisher.xml.XMLDocument;
import nl.idgis.publisher.xml.exceptions.NotFound;

import org.w3c.dom.Node;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class MetadataDocument {
	
	protected final XMLDocument xmlDocument;
	protected final BiMap<String, String> namespaces;
	
	byte[] getContent() throws IOException {
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
		Node dtopNode = xmlDocument.getNode(namespaces, getDigitalTransferOptionsPath());
		if (dtopNode==null){
			throw new NotFound(namespaces, getDigitalTransferOptionsPath());
		}else{
			String gmd = namespaces.get("gmd");
			String gco = namespaces.get("gco");

			Node onlineNode = xmlDocument.addNode(dtopNode, gmd, "gmd:onLine", null);
	        Node onlineResourceNode = xmlDocument.addNode(onlineNode, gmd, "gmd:CI_OnlineResource", null);
			
	        Node linkageNode = xmlDocument.addNode(onlineResourceNode, gmd, "gmd:linkage", null);			
	        xmlDocument.addNode(linkageNode, gmd, "gmd:URL", linkage);
	        
	        Node protocolNode = xmlDocument.addNode(onlineResourceNode, gmd, "gmd:protocol", null);			
	        xmlDocument.addNode(protocolNode, gco, "gco:CharacterString", protocol);
	        
	        Node nameNode = xmlDocument.addNode(onlineResourceNode, gmd, "gmd:name", null);			
	        xmlDocument.addNode(nameNode, gco, "gco:CharacterString", name);
		}		
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
	
	public int removeDatasetIdentifier(){
		return xmlDocument.removeNodes(namespaces, getDatasetIdentifierPath());		
	}
	
	public void setDatasetIdentifier(String datasetIdentifier) throws NotFound{
		removeDatasetIdentifier();
		Node codeNode = xmlDocument.getNode(namespaces, getDatasetIdentifierCodePath());
		if (codeNode==null){
			throw new NotFound(namespaces, getDatasetIdentifierCodePath());
		}else{
			String gco = namespaces.get("gco");

	        xmlDocument.addNode(codeNode, gco, "gco:CharacterString", datasetIdentifier);
		}
	}
	
	/**
	 * Service metadata: serviceType
	 */	
	protected String getServiceIdentificationPath(){
		return "/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification";
	}
	
	protected String getServiceTypePath(){
		return "/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/srv:serviceType";
	}	
	
	public int removeServiceType(){
		return xmlDocument.removeNodes(namespaces, getServiceTypePath());		
	}
	
	/**
	 * Add a new /srv:serviceType/gco:LocalName node under srv:SV_ServiceIdentification
	 * @param serviceLocalName content of the /srv:serviceType/gco:LocalName node
	 * @throws NotFound 
	 */
	public void addServiceType(String serviceLocalName) throws NotFound{
		Node siNode = xmlDocument.getNode(namespaces, getServiceIdentificationPath());
		if (siNode==null){
			throw new NotFound(namespaces, getServiceIdentificationPath());
		}else{
			String srv = namespaces.get("srv");
			String gco = namespaces.get("gco");

			Node serviceTypeNode = xmlDocument.addNode(siNode, srv, "srv:serviceType", null);
	        xmlDocument.addNode(serviceTypeNode, gco, "gco:LocalName", serviceLocalName);
		}
	}
	
	/**
	 * Service metadata: BrowseGraphic
	 */	
	protected String getGraphicOverviewPath(){
		return "/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/gmd:graphicOverview";
	}
	
	protected String getBrowseGraphicPath(){
		return "/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/gmd:graphicOverview/gmd:MD_BrowseGraphic";
	}
	
	public int removeBrowseGraphic(){
		return xmlDocument.removeNodes(namespaces, getBrowseGraphicPath());		
	}
	
	/**
	 * Add a new /gmd:MD_BrowseGraphic/gco:CharacterString node under gmd:graphicOverview
	 * @param fileName content of the /gmd:MD_BrowseGraphic/gco:CharacterString node
	 * @throws NotFound 
	 */
	public void addBrowseGraphic(String fileName) throws NotFound{
		Node goNode = xmlDocument.getNode(namespaces, getGraphicOverviewPath());
		if (goNode==null){
			throw new NotFound(namespaces, getGraphicOverviewPath());
		}else{
			String gmd = namespaces.get("gmd");
			String gco = namespaces.get("gco");

			Node browseGraphicNode = xmlDocument.addNode(goNode, gmd, "gmd:MD_BrowseGraphic", null);
	        xmlDocument.addNode(browseGraphicNode, gco, "gco:CharacterString", fileName);
		}
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
	
	
	public int removeServiceEndpoint() {
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
		Node opNode = xmlDocument.getNode(namespaces, getOperationsPath());
		if (opNode==null){
			throw new NotFound(namespaces, getOperationsPath());
		}else{
			String gmd = namespaces.get("gmd");
			String gco = namespaces.get("gco");
			String srv = namespaces.get("srv");

			Node operationMetadataNode = xmlDocument.addNode(opNode, srv, "srv:SV_OperationMetadata", null);

			Node operationNameNode = xmlDocument.addNode(operationMetadataNode, srv, "srv:operationName", null);
	        xmlDocument.addNode(operationNameNode, gco, "gco:CharacterString", operationName);

	        Node dcpNode = xmlDocument.addNode(operationMetadataNode, srv, "srv:DCP", null);
	        xmlDocument.addNodeWithAttributes(dcpNode, srv, "srv:DCPList", new String[] {"codeList", codeList, "codeListValue", codeListValue});
	        
			Node connectPointNode = xmlDocument.addNode(operationMetadataNode, srv, "srv:connectPoint", null);
			Node onlineResourceNode = xmlDocument.addNode(connectPointNode, gmd, "gmd:CI_OnlineResource", null);
			Node linkageNode = xmlDocument.addNode(onlineResourceNode, gmd, "gmd:linkage", null);
	        xmlDocument.addNode(linkageNode, gmd, "gmd:URL", linkage);
	        
		}
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
	
	public int removeSVCoupledResource() {
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
		Node crNode = xmlDocument.getNode(namespaces, getCoupledResourcePath());
		if (crNode==null){
			throw new NotFound(namespaces, getCoupledResourcePath());
		}else{
			String srv = namespaces.get("srv");
			String gco = namespaces.get("gco");

			Node svcrNode = xmlDocument.addNode(crNode, srv, "srv:SV_CoupledResource", null);
			
	        Node linkageNode = xmlDocument.addNode(svcrNode, srv, "srv:operationName", null);			
	        xmlDocument.addNode(linkageNode, gco, "gco:CharacterString", operationName);
	        
	        Node protocolNode = xmlDocument.addNode(svcrNode, srv, "srv:identifier", null);			
	        xmlDocument.addNode(protocolNode, gco, "gco:CharacterString", identifier);
	        
	        Node nameNode = xmlDocument.addNode(svcrNode, gco, "gco:ScopedName", null);			
	        xmlDocument.addNode(nameNode, gco, "gco:ScopedName", scopedName);
		}
	}
	
	/**
	 * Service metadata: Link to dataset 
	 */	
	protected String getOperatesOnPath(){
		return getServiceIdentificationPath() + "/srv:operatesOn";
	}
	
	public int removeOperatesOn() {
		return xmlDocument.removeNodes(namespaces, getOperatesOnPath());
	}

	/**
	 * Add link to dataset as attributes in /srv:operatesOn
	 * @param uuidref dataset reference
	 * @param href link to dataset metadata
	 * @throws NotFound
	 */
	public void addOperatesOn(String uuidref, String href) throws NotFound {
		Node siNode = xmlDocument.getNode(namespaces, getServiceIdentificationPath());
		if (siNode==null){
			throw new NotFound(namespaces, getServiceIdentificationPath());
		}else{
			String srv = namespaces.get("srv");

	       xmlDocument.addNodeWithAttributes(siNode, srv, "srv:operatesOn", new String[] {"uuidref", uuidref, "href", href});
		}
	}
	
}
