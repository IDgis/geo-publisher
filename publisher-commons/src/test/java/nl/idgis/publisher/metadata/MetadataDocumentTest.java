package nl.idgis.publisher.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotEquals;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import nl.idgis.publisher.metadata.MetadataDocument.Keywords;
import nl.idgis.publisher.metadata.MetadataDocument.OperatesOn;
import nl.idgis.publisher.metadata.MetadataDocument.ServiceLinkage;
import nl.idgis.publisher.metadata.MetadataDocument.Topic;
import nl.idgis.publisher.utils.XMLUtils.XPathHelper;
import nl.idgis.publisher.xml.XMLDocument;
import nl.idgis.publisher.xml.exceptions.NotFound;
import nl.idgis.publisher.xml.exceptions.NotParseable;

public class MetadataDocumentTest {

	/**
	 * Get a MetadataDocument from test resources.
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public static MetadataDocument getDocument(String name) throws Exception {
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
	public void testReadDatasetMetadata() throws Exception {
		MetadataDocument document = getDocument("dataset_metadata.xml");
		
		Date date = document.getDatasetRevisionDate();		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		assertEquals(2013, calendar.get(Calendar.YEAR));
		assertEquals(Calendar.NOVEMBER, calendar.get(Calendar.MONTH));
		assertEquals(19, calendar.get(Calendar.DAY_OF_MONTH));
		
		String result = document.getDatasetResponsiblePartyName("custodian");
		assertTrue("wrong ResponsiblePartyName", result.startsWith("Provincie Overijssel"));
		result = document.getDatasetResponsiblePartyEmail("custodian");
		assertTrue("wrong ResponsiblePartyEmail", result.startsWith("beleidsinformatie@overijssel.nl"));
		
		result = document.getDatasetResponsiblePartyName("owner");
		assertTrue("wrong ResponsiblePartyName", result.startsWith("Provincie Overijssel:"));
		result = document.getDatasetResponsiblePartyEmail("owner");
		assertTrue("wrong ResponsiblePartyEmail", result.startsWith("NRJ.Eilers@overijssel.nl"));
		
		result = document.getMetaDataIdentifier();
		assertTrue("wrong MetaDataIdentifier", result.startsWith("46647460-d8cf-4955-bcac-f1c192d57cc4"));
		
		result = document.getMetaDataCreationDate();
		assertTrue("wrong MetaDataCreationDate", result.startsWith("2014-04-11"));
		
		List<String> results = document.getSupplementalInformation();
		assertNotNull(results);
		assertEquals("wrong SupplementalInformation", Arrays.asList("layerfile| http:\\\\localhost:7000\\GeoPortal\\MIS4GIS\\lyr\\gemgrens_polygon.lyr"), results);
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
		assertTrue("Still WMS link found", document.getServiceLinkage().isEmpty());
		
		// add new gmd:online childnode
		document.addServiceLinkage("linkage", "protocol", "name");
		List<ServiceLinkage> result = document.getServiceLinkage();		
		assertFalse("No service linkage found", result.isEmpty());
		
		ServiceLinkage serviceLinkage = result.get(0);		
		assertEquals("linkage", serviceLinkage.getURL());				
		assertEquals("protocol", serviceLinkage.getProtocol());				
		assertEquals("name", serviceLinkage.getName());
		
		// remove all gmd:online child nodes		
		i = document.removeServiceLinkage();
		assertEquals("There should be one removed linkage", 1, i);
		
		// check gmd:MD_DigitalTransferOptions has no gmd:online child node anymore
		result = document.getServiceLinkage();		
		assertTrue("No service linkage found", result.isEmpty());
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
		
		result = document.getDatasetAlternateTitle();		
		assertEquals("wrong alternate title", "B1.gemgrens_polygon (b1/b14)", result);
		
		result = document.getDatasetAbstract();		
		assertTrue("wrong abstract: " + result, result.startsWith("De bestuurlijk vastgestelde gemeentegrenzen "));
		

		// set new values
		document.setDatasetTitle("Overijssel Gemeentegrenzen");
		document.setDatasetAlternateTitle("Alternate ");
		document.setDatasetAbstract("De gemeentegrenzen, bestuurlijk vastgesteld ");

		
		// check new values
		result = document.getDatasetTitle();		
		assertEquals("wrong title", "Overijssel Gemeentegrenzen", result);
		
		result = document.getDatasetAlternateTitle();		
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
		List<MetadataDocument.Keywords> result = document.getDatasetKeywords();
		assertNotNull(result);
		assertEquals(1, result.size());
		
		Keywords keywords = result.get(0);
		assertNotNull(keywords);
		assertTrue("wrong keyword", keywords.getKeywords().contains("gemeenten"));
		assertEquals("wrong thesaurus", "Interprovinciale thesaurus", keywords.getThesaurusTitle());
		assertEquals("wrong date", "2013-09-11", keywords.getThesaurusDate());

		document.removeDatasetKeywords();
		 
		document.addDatasetKeywords(
			Stream.of("aaa-bbb", "ccc-ddd").collect(Collectors.toSet()), 
			"thesaurusTitle", "2015-01-01", "./resources/codeList.xml#etcetera", "publicatie") ;
		
		// check the new values 
		result = document.getDatasetKeywords();
		assertNotNull(result);
		assertEquals(1, result.size());
		
		keywords = result.get(0);
		assertNotNull(keywords);
		
		assertTrue("wrong keyword", keywords.getKeywords().contains("ccc-ddd"));		
		assertEquals("wrong thesaurus", "thesaurusTitle", keywords.getThesaurusTitle());
		assertEquals("wrong date", "2015-01-01", keywords.getThesaurusDate());
	}
	
	@Test
	public void testDatasetMetadataPointOfContact() throws Exception{
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
	public void testDatasetResponsibleParty() throws Exception{
		MetadataDocument document = getDocument("dataset_metadata.xml");

		String name = "contactname";
		String email = "emailaddress";
		
		// check current values
		String result = document.getDatasetResponsiblePartyName("owner");
		assertFalse("unexpected responsibleparty", result.indexOf(name) >= 0);
		result = document.getDatasetResponsiblePartyEmail("owner");
		assertFalse("unexpected email", result.indexOf(email) >= 0);
		
		// set new values
		document.setDatasetResponsiblePartyName("owner", name);
		document.setDatasetResponsiblePartyEmail("owner", email);
		
		// check new values again
		result = document.getDatasetResponsiblePartyName("owner");
		assertEquals("wrong responsibleparty", name, result);		
		result = document.getDatasetResponsiblePartyEmail("owner");
		assertEquals("wrong email", email, result);
	}
	
	
	
	
	@Test
	public void testServiceMetadataPointOfContact() throws Exception{
		MetadataDocument document = getDocument("service_metadata.xml");
		String name = "somename";
		String email = "someaddress";
		
		// check current values
		String result = document.getMetaDataPointOfContactName("pointOfContact");
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
	
	@Test
	public void testServerResponsibleParty() throws Exception{
		MetadataDocument document = getDocument("service_metadata.xml");

		String name = "contactname";
		String email = "emailaddress";
		
		// check current values
		String result = document.getServiceResponsiblePartyName("pointOfContact");
		assertFalse("unexpected responsibleparty", result.indexOf(name) >= 0);
		result = document.getServiceResponsiblePartyEmail("pointOfContact");
		assertFalse("unexpected email", result.indexOf(email) >= 0);
		
		// set new values
		document.setServiceResponsiblePartyName("pointOfContact", name);
		document.setServiceResponsiblePartyEmail("pointOfContact", email);
		
		// check new values again
		result = document.getServiceResponsiblePartyName("pointOfContact");
		assertEquals("wrong responsibleparty", name, result);		
		result = document.getServiceResponsiblePartyEmail("pointOfContact");
		assertEquals("wrong email", email, result);
	}
	
	
	
	/**
	 * Service metadata: read several items
	 */	
	@Test
	public void testReadServiceMetadata() throws Exception {
		MetadataDocument document = getDocument("service_metadata.xml");
		
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
		
		result = document.getServiceResponsiblePartyName("pointOfContact");
		assertTrue("wrong ResponsiblePartyName", result.startsWith("Interprovinciaal Overleg"));
		result = document.getServiceResponsiblePartyEmail("pointOfContact");
		assertTrue("wrong ResponsiblePartyEmail", result.startsWith("inspire@gbo-provincies.nl"));
		
		result = document.getMetaDataIdentifier();
		assertTrue("wrong MetaDataIdentifier", result.startsWith("5a69e9d5-611c-4818-a181-685ef4c81085"));

		result = document.getMetaDataCreationDate();
		assertTrue("wrong MetaDataCreationDate", result.startsWith("2013-12-03"));
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
		document.addServiceBrowseGraphic(fileName);
		
		List<String> result = document.getServiceBrowseGraphics();		
		assertFalse("No browse graphic found", result.isEmpty());
		assertEquals("No protocol found", fileName, result.get(0));
		
		// remove all child nodes		
		i = document.removeBrowseGraphic();
		assertEquals("There should be one removed linkage", 1, i);
		
		assertTrue(document.getServiceBrowseGraphics().isEmpty());
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
		
		OperatesOn operatesOn = document.getOperatesOn().get(0);
		
		String result = operatesOn.getUuidref();
		assertEquals("No uuid ref found", "bc509f92-5d8c-4169-818b-49ff6a7576c3", result);
		
		result = operatesOn.getHref();
		assertEquals("No href found", href, result);
		
		// remove all child nodes		
		i = document.removeOperatesOn();
		assertEquals("There should be one removed dataset link", 1, i);
		
		// check no node anymore
		assertTrue(document.getOperatesOn().isEmpty());
	}
	
	@Test
	public void testFileIdentifier() throws Exception {
		MetadataDocument document = getDocument("service_metadata.xml");
		
		assertEquals("5a69e9d5-611c-4818-a181-685ef4c81085", document.getFileIdentifier()); 
		document.setFileIdentifier("bc509f92-5d8c-4169-818b-49ff6a7576c3");
		assertEquals("bc509f92-5d8c-4169-818b-49ff6a7576c3", document.getFileIdentifier());
	}
	
	@Test(expected=NotFound.class)
	public void testGetDatasetRevisionDate() throws Exception {
		MetadataDocument document = getDocument("dataset_metadata.xml");
		
		// should return the revision date
		Date date = document.getDatasetRevisionDate(); 
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		assertEquals(2013, calendar.get(Calendar.YEAR));
		assertEquals(Calendar.NOVEMBER, calendar.get(Calendar.MONTH));
		assertEquals(19, calendar.get(Calendar.DAY_OF_MONTH));
		
		document.isoMetadata.removeNodes(document.namespaces,
			document.getDatePath(Topic.DATASET, MetadataDocument.REVISION));
		
		// should return the creation date
		date = document.getDatasetRevisionDate();		
		
		calendar = Calendar.getInstance();
		calendar.setTime(date);
		assertEquals(1996, calendar.get(Calendar.YEAR));
		assertEquals(Calendar.JUNE, calendar.get(Calendar.MONTH));
		assertEquals(9, calendar.get(Calendar.DAY_OF_MONTH));
		
		document.isoMetadata.removeNodes(document.namespaces,
				document.getDatePath(Topic.DATASET, MetadataDocument.CREATION));
		
		// should fail (raise NotFound exception)
		document.getDatasetRevisionDate();
	}
	
	@Test
	public void testUpdateSupplementalInformation() throws Exception {
		MetadataDocument document = getDocument("dataset_metadata.xml");
		
		String current = document.getSupplementalInformation().get(0);
		String replacement = "Hello, ' world!";
		assertNotEquals(replacement, current);
		
		document.updateSupplementalInformation(current, replacement);
		String updated = document.getSupplementalInformation().get(0);
		assertEquals(replacement, updated);
		
		document.updateSupplementalInformation(replacement, current);
	}
	
	@Test
	public void testUpdateDatasetBrowseGraphics() throws Exception {
		MetadataDocument document = getDocument("dataset_metadata.xml");
		
		String current = document.getDatasetBrowseGraphics().get(0);
		String replacement = "Hello, ' world!";
		assertNotEquals(replacement, current);
		
		document.updateDatasetBrowseGraphic(current, replacement);
		String updated = document.getDatasetBrowseGraphics().get(0);
		assertEquals(replacement, updated);
		
		document.updateDatasetBrowseGraphic(replacement, current);
	}
	
	@Test
	public void testRemoveAdditionalPointOfContacts() throws Exception {
		MetadataDocument document = getDocument("dataset_metadata.xml");
		
		List<XPathHelper> pointOfContacts = document.isoMetadata
			.xpath(Optional.of(document.namespaces))
			.nodes("/gmd:MD_Metadata"
				+ "/gmd:identificationInfo"
				+ "/gmd:MD_DataIdentification"
				+ "/gmd:pointOfContact");
		
		assertEquals(2, pointOfContacts.size());
		
		String firstIndividualName = pointOfContacts.get(0).stringOrNull(
			"gmd:CI_ResponsibleParty"
			+ "/gmd:individualName"
			+ "/gco:CharacterString"
			+ "/text()");
		assertNotNull(firstIndividualName);
		
		document.removeAdditionalPointOfContacts();
		
		pointOfContacts = document.isoMetadata
			.xpath(Optional.of(document.namespaces))
			.nodes("/gmd:MD_Metadata"
				+ "/gmd:identificationInfo"
				+ "/gmd:MD_DataIdentification"
				+ "/gmd:pointOfContact");
		
		assertEquals(1, pointOfContacts.size());
		assertEquals(firstIndividualName, pointOfContacts.get(0).stringOrNull(
				"gmd:CI_ResponsibleParty"
				+ "/gmd:individualName"
				+ "/gco:CharacterString"
				+ "/text()"));
	}
	
	@Test
	public void testAddProcessStep() throws Exception {
		String dataQualityInfoPath = 
				"/gmd:MD_Metadata"
				+ "/gmd:dataQualityInfo";
		String scopeCodePath = 
				dataQualityInfoPath
				+ "/gmd:DQ_DataQuality"
				+ "/gmd:scope"
				+ "/gmd:DQ_Scope"
				+ "/gmd:level"
				+ "/gmd:MD_ScopeCode["
					+ "@codeList = './resources/codeList.xml#MD_ScopeCode' "
					+ "and text() = 'dataset']";
		String processStepDescriptionPath = 
				dataQualityInfoPath
				+ "/gmd:DQ_DataQuality"
				+ "/gmd:lineage"
				+ "/gmd:LI_Lineage"
				+ "/gmd:processStep"
				+ "/gmd:LI_ProcessStep"
				+ "/gmd:description"
				+ "/gco:CharacterString";
		
		String firstDescription = "first test description";
		String secondDescription = "second test description";
		
		MetadataDocument document = getDocument("dataset_metadata.xml");
		assertFalse(document.xpath().node(dataQualityInfoPath).isPresent());
		assertFalse(document.xpath().node(scopeCodePath).isPresent());
		assertFalse(document.xpath().node(processStepDescriptionPath + "[text() = '" + firstDescription + "']").isPresent());
		assertFalse(document.xpath().node(processStepDescriptionPath + "[text() = '" + secondDescription + "']").isPresent());
		
		document.addProcessStep(firstDescription);
		assertTrue(document.xpath().node(dataQualityInfoPath).isPresent());
		assertTrue(document.xpath().node(scopeCodePath).isPresent());
		assertTrue(document.xpath().node(processStepDescriptionPath + "[text() = '" + firstDescription + "']").isPresent());
		assertFalse(document.xpath().node(processStepDescriptionPath + "[text() = '" + secondDescription + "']").isPresent());
		
		document.addProcessStep(secondDescription);
		assertTrue(document.xpath().node(processStepDescriptionPath + "[text() = '" + secondDescription + "']").isPresent());
	}
	
	@Test
	public void testEncapsulatingDocument() throws Exception {
		InputStream stream = MetadataDocumentTest.class.getResourceAsStream("dataset_metadata.xml");
		assertNotNull(stream);
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		Document datasetMetadata = db.parse(stream);
		
		Document encapsulatingDocument = db.newDocument();
		Element rootElement = encapsulatingDocument.createElement("metadata");
		encapsulatingDocument.appendChild(rootElement);
		rootElement.appendChild(encapsulatingDocument.adoptNode(datasetMetadata.getDocumentElement()));
		
		MetadataDocument metadataDocument = new MetadataDocument(new XMLDocument(encapsulatingDocument));
		assertNotNull(metadataDocument.getDatasetTitle());
	}
	
	@Test
	public void testAttributeAliases() throws Exception {
		MetadataDocument metadataDocument = getDocument("sde_metadata.xml");
		assertNotNull(metadataDocument);
		
		Map<String, String> attributeAliases = metadataDocument.getAttributeAliases();
		assertNotNull(attributeAliases);
		assertTrue(attributeAliases.containsKey("NAAM"));
		assertEquals("gemeentenaam te gebruiken voor cartografische doeleinden", attributeAliases.get("NAAM"));
	}
	
	@Test
	public void testDatasetSpatialRepresentationType() throws Exception {
		MetadataDocument metadataDocument = getDocument("dataset_metadata.xml");
		assertNotNull(metadataDocument);
		assertEquals("vector", metadataDocument.getDatasetSpatialRepresentationType());
	}
}
