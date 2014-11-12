package nl.idgis.publisher.metadata;

import java.util.Date;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import nl.idgis.publisher.utils.SimpleDateFormatMapper;
import nl.idgis.publisher.xml.XMLDocument;
import nl.idgis.publisher.xml.exceptions.NotFound;

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
	
}
