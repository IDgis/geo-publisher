package nl.idgis.publisher.metadata;

import java.util.Date;

import nl.idgis.publisher.utils.SimpleDateFormatMapper;
import nl.idgis.publisher.xml.XMLDocument;
import nl.idgis.publisher.xml.exceptions.NotFound;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class MetadataDocument {
	
	private final XMLDocument xmlDocument;
	private final BiMap<String, String> namespaces;
	
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

	private Date getDate(String codeListValue) throws NotFound {
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
	}
	
	private String getDatePath(String codeListValue) {
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
	
	
	public String getString(String path) throws NotFound{
		return xmlDocument.getString(namespaces, path);
	}
	
	/*
	 * Service Linkage
	 */
	
	private String getDigitalTransferOptionsPath(){
		return
				"/gmd:MD_Metadata" + 
				"/gmd:distributionInfo" + 
				"/gmd:MD_Distribution" + 
				"/gmd:transferOptions" + 
				"/gmd:MD_DigitalTransferOptions" ;
	}
	
	public String getDigitalTransferOptions() throws NotFound{
		return xmlDocument.getString(namespaces, getDigitalTransferOptionsPath());
	}
	
	private String getServiceLinkagePath(){
		return
				"/gmd:MD_Metadata" + 
				"/gmd:distributionInfo" + 
				"/gmd:MD_Distribution" + 
				"/gmd:transferOptions" + 
				"/gmd:MD_DigitalTransferOptions" + 
				"/gmd:onLine";
	}
	
	public String getServiceLinkages() throws NotFound{
		return xmlDocument.getString(namespaces, getServiceLinkagePath());
	}
	
	/**
	 * Remove all gmd:onLine childnodes of node MD_DigitalTransferOptions
	 * @return nr of removed nodes
	 * @throws Exception
	 */
	public int removeServiceLinkages() throws Exception{
		return xmlDocument.removeNodes(namespaces, getServiceLinkagePath());
	}
	
	/**
	 * Add a new /gmd:onLine/gmd:CI_OnlineResource node under gmd:MD_DigitalTransferOptions
	 * @param linkage content of the /gmd:linkage/gmd:URL node
	 * @param protocol content of the /gmd:protocol/gco:CharacterString node
	 * @param name content of the /gmd:name/gco:CharacterString node
	 */
	public void setServiceLinkage(String linkage, String protocol, String name){
		// build a gmd:online node
		Node dtopNode = xmlDocument.getNode(namespaces, getDigitalTransferOptionsPath());
		if (dtopNode==null){
			// exception?
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
//		try {
//			xmlDocument.prettyPrint(dtopNode.getParentNode());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}
	
}
