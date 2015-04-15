package nl.idgis.publisher.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import nl.idgis.publisher.xml.exceptions.NotParseable;

public class MetadataDocumentTest {

	/**
	 * Get a MetadataDocument from test resources.
	 * @param name
	 * @return
	 * @throws Exception
	 */
	private MetadataDocument getDocument(String name) throws Exception {
		InputStream stream = MetadataDocumentTest.class.getResourceAsStream(name);
		assertNotNull("test metadata document not found", stream);
		
		byte[] content = IOUtils.toByteArray(stream);
		
		MetadataDocumentFactory factory = new MetadataDocumentFactory();
		
		return factory.parseDocument(content);
	}
	
	/**
	 * 
	 * Dataset metadata: read several items
	 */
	@Test
	public void testReadDataset() throws Exception {
		InputStream stream = MetadataDocumentTest.class.getResourceAsStream("dataset_metadata.xml");
		assertNotNull("test metadata document not found", stream);
		
		byte[] content = IOUtils.toByteArray(stream);
		
		MetadataDocumentFactory factory = new MetadataDocumentFactory();
		
		MetadataDocument document = factory.parseDocument(content);
		
		String result = document.getDatasetTitle();		
		assertEquals("wrong title", "Gemeentegrenzen Overijssel (vlakken)", result);
		
		result = document.getAlternateTitle();		
		assertEquals("wrong alternate title", "B1.gemgrens_polygon (b1/b14)", result);
		
		result = document.getDatasetAbstract();		
		assertTrue("wrong abstract: " + result, result.startsWith("De bestuurlijk vastgestelde gemeentegrenzen "));
		
		Date date = document.getDatasetRevisionDate();		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		assertEquals(2013, calendar.get(Calendar.YEAR));
		assertEquals(Calendar.NOVEMBER, calendar.get(Calendar.MONTH));
		assertEquals(19, calendar.get(Calendar.DAY_OF_MONTH));
		
		result = document.getDatasetResponsiblePartyName();
		assertTrue("wrong ResponsiblePartyName", result.startsWith("Provincie Overijssel"));
		result = document.getDatasetResponsiblePartyEmail();
		assertTrue("wrong ResponsiblePartyEmail", result.startsWith("beleidsinformatie@overijssel.nl"));
		
		result = document.getMetaDataIdentifier();
		assertTrue("wrong MetaDataIdentifier", result.startsWith("46647460-d8cf-4955-bcac-f1c192d57cc4"));
	}
	
	@Test(expected=NotParseable.class)
	public void testNotParseable() throws Exception {
		MetadataDocumentFactory factory = new MetadataDocumentFactory();
		factory.parseDocument("Not valid metadata!".getBytes("utf-8"));
	}
	
	/**
	 * Dataset metadata: Service Linkage
	 */	
	@Test
	public void testDatasetTransferOptions() throws Exception{
		MetadataDocument document = getDocument("dataset_metadata.xml");

		// remove all gmd:online child nodes
		int i = document.removeServiceLinkage();
		assertEquals("There should be one removed linkage", 1, i);
		String result ;
		// check gmd:MD_DigitalTransferOptions has no gmd:online child node anymore
		try {
			result = document.getServiceLinkageURL();		
			assertFalse("Still WMS link found", result.contains("overijssel.geo-hosting.nl"));
		} catch(Exception e) {}
		
		// add new gmd:online childnode
		document.addServiceLinkage("linkage", "protocol", "name");
		result = document.getServiceLinkageURL();		
		assertNotNull("No service linkage found", result);
		assertTrue("No name found", result.contains("linkage"));
		result = document.getServiceLinkageProtocol();		
		assertTrue("No protocol found", result.contains("protocol"));
		result = document.getServiceLinkageName();		
		assertTrue("No name found", result.contains("name"));
		
		// remove all gmd:online child nodes		
		i = document.removeServiceLinkage();
		assertEquals("There should be one removed linkage", 1, i);
		
		// check gmd:MD_DigitalTransferOptions has no gmd:online child node anymore
		try {
			result = document.getServiceLinkageName();		
			assertFalse("Unexpected name found", result.contains("name"));
		} catch(Exception e) {}
	}
	
	/**
	 * Dataset metadata: identifier
	 */	
	@Test
	public void testDatasetIdentifier() throws Exception {
		MetadataDocument document = getDocument("dataset_metadata.xml");

		// check the current id
		String result = document.getDatasetIdentifier();	
		assertEquals("Wrong dataset id found", "bc509f92-5d8c-4169-818b-49ff6a7576c3", result.trim());

		document.setDatasetIdentifier("aaaa-bbbb-cccc-dddd-eeee");
		
		// check the new dataset id is correct 
		result = document.getDatasetIdentifier();	
		assertEquals("Wrong dataset id found", "aaaa-bbbb-cccc-dddd-eeee", result.trim());		
	}
	
	/**
	 * Dataset metadata: title, alternate title, abstract
	 */	
	@Test
	public void testDatasetTitleAbstract() throws Exception {
		MetadataDocument document = getDocument("dataset_metadata.xml");

		// check the current values 
		String result = document.getDatasetTitle();		
		assertEquals("wrong title", "Gemeentegrenzen Overijssel (vlakken)", result);
		
		result = document.getAlternateTitle();		
		assertEquals("wrong alternate title", "B1.gemgrens_polygon (b1/b14)", result);
		
		result = document.getDatasetAbstract();		
		assertTrue("wrong abstract: " + result, result.startsWith("De bestuurlijk vastgestelde gemeentegrenzen "));
		

		// set new values
		document.setDatasetTitle("Overijssel Gemeentegrenzen");
		document.setAlternateTitle("Alternate ");
		document.setDatasetAbstract("De gemeentegrenzen, bestuurlijk vastgesteld ");

		
		// check new values
		result = document.getDatasetTitle();		
		assertEquals("wrong title", "Overijssel Gemeentegrenzen", result);
		
		result = document.getAlternateTitle();		
		assertEquals("wrong alternate title", "Alternate ", result);
		
		result = document.getDatasetAbstract();		
		assertTrue("wrong abstract: " + result, result.startsWith("De gemeentegrenzen, bestuurlijk vastgesteld "));
		
	}
	
	/**
	 * Dataset: keywords 
	 */
	@Test
	public void testDatasetKeywords() throws Exception {
		MetadataDocument document = getDocument("dataset_metadata.xml");

		// check the current values 
		String result = document.getDatasetKeywords();
		System.out.println("1. result keywords: " + result);
		assertTrue("wrong keyword", result.indexOf("gemeenten") >= 0);
		assertTrue("wrong thesaurus", result.indexOf("Interprovinciale thesaurus") >= 0);
		assertTrue("wrong date", result.indexOf("2013-09-11") >= 0);

		assertFalse("unexpected keyword", result.indexOf("ccc-ddd") >= 0);
		assertFalse("unexpected thesaurus", result.indexOf("thesaurusTitle") >= 0);
		assertFalse("unexpected date", result.indexOf("2015-01-01") >= 0);

		document.removeDatasetKeywords();
		
		List<String> keywords = new ArrayList<String>();
		keywords.add("aaa-bbb");
		keywords.add("ccc-ddd");
		document.addDatasetKeywords(keywords, "thesaurusTitle", "2015-01-01", "./resources/codeList.xml#etcetera", "publicatie") ;
		
		// check the new values 
		result = document.getDatasetKeywords();		
		System.out.println("2. result keywords: " + result);
		assertTrue("wrong keyword", result.indexOf("ccc-ddd") >= 0);
		assertTrue("wrong thesaurus", result.indexOf("thesaurusTitle") >= 0);
		assertTrue("wrong date", result.indexOf("2015-01-01") >= 0);
		
	}
	
	@Test
	public void testDatasetMetadata() throws Exception{
		MetadataDocument document = getDocument("dataset_metadata.xml");

		String name = "contactname";
		String email = "emailaddress";
		
		// check current values
		String result = document.getMetaDataPointOfContactName("owner");
		assertFalse("unexpected responsibleparty", result.indexOf(name) >= 0);
		result = document.getMetaDataPointOfContactEmail("owner");
		assertFalse("unexpected email", result.indexOf(email) >= 0);
		
		// set new values
		document.setMetaDataPointOfContactName("owner", name);
		document.setMetaDataPointOfContactEmail("owner", email);
		
		// check new values again
		result = document.getMetaDataPointOfContactName("owner");
		assertEquals("wrong responsibleparty", name, result);		
		result = document.getMetaDataPointOfContactEmail("owner");
		assertEquals("wrong email", email, result);
	}
	
	@Test
	public void testServiceMetadata() throws Exception{
		MetadataDocument document = getDocument("service_metadata.xml");
		String name = "somename";
		String email = "someaddress";
		
		// check current values
		String result = document.getMetaDataPointOfContactName("pointOfContact");
		System.out.println("getMetaDataPointOfContactName: " + result);
		assertFalse("unexpected responsibleparty", result.indexOf(name) >= 0);
		result = document.getMetaDataPointOfContactEmail("pointOfContact");
		assertFalse("unexpected email", result.indexOf(email) >= 0);
		
		// set new values
		document.setMetaDataPointOfContactName("pointOfContact", name);
		document.setMetaDataPointOfContactEmail("pointOfContact", email);
		
		// check new values again
		result = document.getMetaDataPointOfContactName("pointOfContact");
		assertEquals("wrong responsibleparty", name, result);		
		result = document.getMetaDataPointOfContactEmail("pointOfContact");
		assertEquals("wrong email", email, result);
	}
	
	
	
	/**
	 * Service metadata: read several items
	 */	
	@Test
	public void testReadService() throws Exception {
		InputStream stream = MetadataDocumentTest.class.getResourceAsStream("service_metadata.xml");
		assertNotNull("test metadata document not found", stream);
		
		byte[] content = IOUtils.toByteArray(stream);
		
		MetadataDocumentFactory factory = new MetadataDocumentFactory();
		
		MetadataDocument document = factory.parseDocument(content);
		
		String result = document.getServiceTitle();		
		assertEquals("wrong title", "INSPIRE View service Beschermde gebieden", result);
		
		result = document.getServiceAbstract();		
		assertTrue("wrong abstract", result.startsWith("Deze View service"));
		
		Date date = document.getServiceRevisionDate();		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		assertEquals(2011, calendar.get(Calendar.YEAR));
		assertEquals(Calendar.APRIL, calendar.get(Calendar.MONTH));
		assertEquals(20, calendar.get(Calendar.DAY_OF_MONTH));
		
		result = document.getServiceResponsiblePartyName();
		assertTrue("wrong ResponsiblePartyName", result.startsWith("Interprovinciaal Overleg"));
		result = document.getServiceResponsiblePartyEmail();
		assertTrue("wrong ResponsiblePartyEmail", result.startsWith("inspire@gbo-provincies.nl"));
		
		result = document.getMetaDataIdentifier();
		assertTrue("wrong MetaDataIdentifier", result.startsWith("5a69e9d5-611c-4818-a181-685ef4c81085"));
	}
	
	
	/**
	 * Service metadata: serviceType
	 */	
	@Test
	public void testServiceServiceType() throws Exception {
		MetadataDocument document = getDocument("service_metadata.xml");

		// remove all srv:serviceType child nodes
		int i = document.removeServiceType();
		
		// add new srv:serviceType childnode
		document.addServiceType("OGC:WMS");
		String result = document.getServiceType();		
		assertEquals("No service type found", "OGC:WMS", result);
		
		// remove all srv:serviceType child nodes		
		i = document.removeServiceType();
		assertEquals("There should be one removed linkage", 1, i);
		
		// check srv:SV_ServiceIdentification has no srv:serviceType child node anymore
		try {
			document.getServiceType();
			fail("Unexpected service type found");
		} catch(Exception e) {}
	}
	
	/**
	 * Service metadata: BrowseGraphic
	 */
	@Test
	public void testServiceBrowseGraphic() throws Exception {
		MetadataDocument document = getDocument("service_metadata.xml");

		// remove all child nodes
		int i = document.removeBrowseGraphic();
		
		// add new childnode
		String fileName = 
				"https://overijssel.geo-hosting.nl/geoserver/wms?request=GetMap&Service=WMS"+
				"&SRS=EPSG:28992&CRS=EPSG:28992&Bbox=180000,459000,270000,540000&Width=600"+
				"&Height=662&Layers=b1:grenzen&Format=image/png&Styles=";
		document.addBrowseGraphic(fileName);
		
		String result = document.getBrowseGraphic();		
		assertNotNull("No browse graphic found", result);
		assertEquals("No protocol found", fileName, result);
		
		// remove all child nodes		
		i = document.removeBrowseGraphic();
		assertEquals("There should be one removed linkage", 1, i);
		
		// check no node anymore
		try {
			document.getBrowseGraphic();
			fail("Unexpected layer found");
		} catch(Exception e) {}
	}
	
	
	/**
	 * Service metadata: Service Endpoint
	 */
	@Test
	public void testServiceEndpoint() throws Exception{
		MetadataDocument document = getDocument("service_metadata.xml");

		// remove all child nodes
		int i = document.removeServiceEndpoint();
		
		// add new childnode
		document.addServiceEndpoint("GetCapabilities",
				"http://www.isotc211.org/2005/iso19119/resources/Codelist/gmxCodelists.xml#DCPList",
				"WebServices", 
				"https://overijssel.geo-hosting.nl/geoserver/wms");
		
		String result = document.getServiceEndpointOperationName();
		assertNotNull("No endpoint found", result);
		assertTrue("No operationName found", result.contains("GetCapabilities"));
		result = document.getServiceEndpointCodeList();
		assertTrue("No codelist found", result.contains("gmxCodelists"));
		result = document.getServiceEndpointCodeListValue();
		assertTrue("No codelist value found", result.contains("WebServices"));
		result = document.getServiceEndpointUrl();
		assertTrue("No url found", result.contains("overijssel.geo-hosting.nl"));
		
		// remove all child nodes		
		i = document.removeServiceEndpoint();
		assertEquals("There should be one removed linkage", 1, i);
		
		// check no node anymore
		try {
			document.getServiceEndpointUrl();
			fail("Unexpected url found");
		} catch(Exception e) {}
	}
	
	/**
	 * Service metadata: transfer options: (same as Dataset metadata: Service Linkage)
	 */	
	public void testServiceTransferOptions() throws Exception{
		MetadataDocument document = getDocument("service_metadata.xml");

		// get gmd:MD_DigitalTransferOptions content
		String result = document.getServiceLinkageName();		
		assertTrue("No WMS link found", result.contains("OGC:WMS"));

		// remove all gmd:online child nodes
		int i = document.removeServiceLinkage();
		assertEquals("There should be two removed linkages", 2, i);
		
		// check gmd:MD_DigitalTransferOptions has no gmd:online child node anymore
		result = document.getServiceLinkageURL();		
		assertFalse("Still WMS link found", result.contains("overijssel.geo-hosting.nl"));
		
		// add new gmd:online childnode
		document.addServiceLinkage("linkage", "protocol", "name");
		result = document.getServiceLinkageURL();		
		assertNotNull("No service linkage found", result);
		assertTrue("No name found", result.contains("linkage"));
		result = document.getServiceLinkageProtocol();		
		assertTrue("No protocol found", result.contains("protocol"));
		result = document.getServiceLinkageName();		
		assertTrue("No name found", result.contains("name"));
		
		// remove all gmd:online child nodes		
		i = document.removeServiceLinkage();
		assertEquals("There should be one removed linkage", 1, i);
		
		// check gmd:MD_DigitalTransferOptions has no gmd:online child node anymore
		result = document.getServiceLinkageName();		
		assertFalse("Unexpected name found", result.contains("name"));
	}
	
	
	/**
	 * Service metadata: Coupled Resource
	 */
	@Test
	public void testServiceCoupledResource() throws Exception {
		MetadataDocument document = getDocument("service_metadata.xml");

		// remove all child nodes
		int i = document.removeSVCoupledResource();
		
		// add new childnode
		document.addSVCoupledResource("GetMap", "bc509f92-5d8c-4169-818b-49ff6a7576c3", "PS.ProtectedSiteStilteGebieden");
		String result = document.getServiceCoupledResourceOperationName();		
		assertNotNull("No coupled resource found", result);
		assertTrue("No operationName found", result.contains("GetMap"));
		result = document.getServiceCoupledResourceIdentifier();		
		assertTrue("No identifier found", result.contains("bc509f92-5d8c-4169-818b-49ff6a7576c3"));
		result = document.getServiceCoupledResourceScopedName();		
		assertTrue("No name found", result.contains("PS.ProtectedSiteStilteGebieden"));
		
		// remove all child nodes		
		i = document.removeSVCoupledResource();
		assertEquals("There should be one removed coupled resource", 1, i);
		
		// check no node anymore
		try {
			document.getServiceCoupledResourceOperationName();
			fail("Unexpected operationName found");
		} catch(Exception e) {}
	}
	
	/**
	 * Service metadata: Link to dataset 
	 */
	@Test
	public void testServiceLinkToDataset() throws Exception{
		MetadataDocument document = getDocument("service_metadata.xml");		

		// remove all child nodes
		int i = document.removeOperatesOn();
		
		final String href = 
				"http://gisopenbaar.overijssel.nl/geoportal_extern/csw?Service=CSW&Request=GetRecordById&Version=2.0.2"+
				"&id=46647460-d8cf-4955-bcac-f1c192d57cc4&outputSchema=http://www.isotc211.org/2005/gmd&elementSetName=full";
		
		// add new childnode
		document.addOperatesOn("bc509f92-5d8c-4169-818b-49ff6a7576c3", href);
		
		String result = document.getOperatesOnUuid();
		assertEquals("No uuid ref found", "bc509f92-5d8c-4169-818b-49ff6a7576c3", result);
		
		result = document.getOperatesOnHref();
		assertEquals("No href found", href, result);
		
		// remove all child nodes		
		i = document.removeOperatesOn();
		assertEquals("There should be one removed dataset link", 1, i);
		
		// check no node anymore
		try {
			document.getOperatesOnHref();
			fail("Unexpected uuid ref found");
		} catch(Exception e) {}
	}
	
	@Test
	public void testFileIdentifier() throws Exception {
		MetadataDocument document = getDocument("service_metadata.xml");
		
		assertEquals("5a69e9d5-611c-4818-a181-685ef4c81085", document.getFileIdentifier()); 
		document.setFileIdentifier("bc509f92-5d8c-4169-818b-49ff6a7576c3");
		assertEquals("bc509f92-5d8c-4169-818b-49ff6a7576c3", document.getFileIdentifier());
	}
}
