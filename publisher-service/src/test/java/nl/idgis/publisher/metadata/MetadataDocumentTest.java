package nl.idgis.publisher.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.metadata.messages.ParseMetadataDocument;
import nl.idgis.publisher.xml.exceptions.NotFound;
import nl.idgis.publisher.xml.messages.NotParseable;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.Element;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;

public class MetadataDocumentTest {

	private static final FiniteDuration AWAIT_DURATION = Duration.create(15, TimeUnit.SECONDS);

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
		
		ActorSystem system = ActorSystem.create();
		
		ActorRef factory = system.actorOf(MetadataDocumentFactory.props());
		Future<Object> future = Patterns.ask(factory, new ParseMetadataDocument(content), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		assertTrue("didn't receive a MetadataDocument", result instanceof MetadataDocument);
		
		MetadataDocument document = (MetadataDocument)result;
		
		return document;
	}
	
	@Test
	public void testRead() throws Exception {
		InputStream stream = MetadataDocumentTest.class.getResourceAsStream("metadata.xml");
		assertNotNull("test metadata document not found", stream);
		
		byte[] content = IOUtils.toByteArray(stream);
		
		ActorSystem system = ActorSystem.create();
		
		ActorRef factory = system.actorOf(MetadataDocumentFactory.props());
		Future<Object> future = Patterns.ask(factory, new ParseMetadataDocument(content), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		assertTrue("didn't receive a MetadataDocument", result instanceof MetadataDocument);
		
		MetadataDocument document = (MetadataDocument)result;
		
		result = document.getTitle();		
		assertEquals("wrong title", "Zeer kwetsbare gebieden", result);
		
		result = document.getAlternateTitle();		
		assertEquals("wrong alternate title", "B4.wav_polygon (b4\\b46)", result);
		
		result = document.getRevisionDate();		
		assertTrue("wrong GetRivisionDate response type", result instanceof Date);
		
		Date date = (Date)result;
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		
		assertEquals(2009, calendar.get(Calendar.YEAR));
		assertEquals(Calendar.AUGUST, calendar.get(Calendar.MONTH));
		assertEquals(14, calendar.get(Calendar.DAY_OF_MONTH));
	}
	
	@Test
	public void testNotParseable() throws Exception {
		ActorSystem system = ActorSystem.create();
		
		ActorRef factory = system.actorOf(MetadataDocumentFactory.props());
		Future<Object> future = Patterns.ask(factory, new ParseMetadataDocument("Not valid metadata!".getBytes("utf-8")), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		
		assertTrue(result instanceof NotParseable);
		
	}
	
	/*
	 * Dataset metadata: Service Linkage
	 */
	
	@Test
	public void testDatasetServiceLinkage() throws Exception{
		MetadataDocument document = getDocument("dataset_metadata.xml");

		// remove all gmd:online child nodes
		int i = document.removeServiceLinkage();
		
		// check gmd:MD_DigitalTransferOptions has no gmd:online child node anymore
		String result = document.getDigitalTransferOptions();		
		assertTrue("Still WMS link found", result==null?true:result.indexOf("OGC:WMS") == -1);
		
		// add new gmd:online childnode
		document.addServiceLinkage("linkage", "protocol", "name");
		result = document.getDigitalTransferOptions();		
		assertNotNull("No service linkage found", result);
		assertTrue("No protocol found", result.indexOf("protocol") > -1);
		
		// remove all gmd:online child nodes		
		i = document.removeServiceLinkage();
		assertTrue("There should be one removed linkage", i==1);
		
		// check gmd:MD_DigitalTransferOptions has no gmd:online child node anymore
		result = document.getDigitalTransferOptions();		
		assertTrue("Unexpected protocol found", result==null?true:result.indexOf("protocol") == -1);
	}
	
	/*
	 * Dataset metadata: Service Linkage
	 */
	
	@Test
	public void testDatasetIdentifier() throws Exception{
		MetadataDocument document = getDocument("dataset_metadata.xml");

		// check the current id
		String result = document.getDatasetIdentifierCode();	
		assertTrue("No dataset id found", result.indexOf("bc509f92-5d8c-4169-818b-49ff6a7576c3") > -1);

		document.setDatasetIdentifier("aaaa-bbbb-cccc-dddd-eeee");
		
		// check the new dataset id is correct 
		result = document.getDatasetIdentifierCode();		
		assertTrue("No dataset id found", result.indexOf("aaaa-bbbb-cccc-dddd-eeee") > -1);
		
	}
	
	
	/*
	 * Service metadata: serviceType
	 */
	
	@Test(expected=NotFound.class)
	public void testServiceServiceType() throws Exception{
		MetadataDocument document = getDocument("service_metadata.xml");

		// remove all srv:serviceType child nodes
		int i = document.removeServiceType();
		
		// add new srv:serviceType childnode
		document.addServiceType("OGC:WMS");
		String result = document.xmlDocument.getString(document.namespaces, document.getServiceTypePath());
		assertNotNull("No service type found", result);
		assertTrue("No service type found", result.indexOf("OGC:WMS") > -1);
		
		// remove all srv:serviceType child nodes		
		i = document.removeServiceType();
		assertTrue("There should be one removed linkage", i==1);
		
		// check srv:SV_ServiceIdentification has no srv:serviceType child node anymore
		result = document.xmlDocument.getString(document.namespaces, document.getServiceTypePath());		
	}
	
	/*
	 * Service metadata: BrowseGraphic
	 */
	@Test
	public void testServiceBrowseGraphic() throws Exception{
		MetadataDocument document = getDocument("service_metadata.xml");

		// remove all child nodes
		int i = document.removeBrowseGraphic();
		
		// add new childnode
		document.addBrowseGraphic(
				"https://overijssel.geo-hosting.nl/geoserver/wms?request=GetMap&Service=WMS"+
				"&SRS=EPSG:28992&CRS=EPSG:28992&Bbox=180000,459000,270000,540000&Width=600"+
				"&Height=662&Layers=b1:grenzen&Format=image/png&Styles=");
		String result = document.getBrowseGraphic();		
		assertNotNull("No browse graphic found", result);
		assertTrue("No protocol found", result.indexOf("b1:grenzen") > -1);
		
		// remove all child nodes		
		i = document.removeBrowseGraphic();
		assertTrue("There should be one removed linkage", i==1);
		
		// check no node anymore
		result = document.getBrowseGraphic();		
		assertTrue("Unexpected layer found", result==null?true:result.indexOf("b1:grenzen") == -1);
	}
	
	
	/*
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
		String result = document.getServiceEndpoint();
		assertNotNull("No endpoint found", result);
		assertTrue("No operationName found", result.indexOf("GetCapabilities") > -1);
		
		// remove all child nodes		
		i = document.removeServiceEndpoint();
		assertTrue("There should be one removed linkage", i==1);
		
		// check no node anymore
		result = document.getServiceEndpoint();		
		assertTrue("Unexpected operationName found", result==null?true:result.indexOf("GetCapabilities") == -1);
	}
	
	/*
	 * Service metadata: transfer options: (same as Dataset metadata: Service Linkage)
	 */
	@Test
	public void testServiceServiceTransferOptions() throws Exception{
		MetadataDocument document = getDocument("service_metadata.xml");

		// get gmd:MD_DigitalTransferOptions content
		String result = document.getDigitalTransferOptions();		
		assertTrue("No WMS link found", result.indexOf("OGC:WMS") > -1);

		// remove all gmd:online child nodes
		int i = document.removeServiceLinkage();
		assertTrue("There should be two removed linkages", i==2);
		
		// check gmd:MD_DigitalTransferOptions has no gmd:online child node anymore
		result = document.getDigitalTransferOptions();		
		assertTrue("Still WMS link found", result==null?true:result.indexOf("OGC:WMS") == -1);
		
		// add new gmd:online childnode
		document.addServiceLinkage("linkage", "protocol", "name");
		result = document.getDigitalTransferOptions();		
		assertNotNull("No transfer options found", result);
		assertTrue("No protocol found", result.indexOf("protocol") > -1);
		
		// remove all gmd:online child nodes		
		i = document.removeServiceLinkage();
		assertTrue("There should be one removed linkage", i==1);
		
		// check gmd:MD_DigitalTransferOptions has no gmd:online child node anymore
		result = document.getDigitalTransferOptions();		
		assertTrue("Unexpected protocol found", result==null?true:result.indexOf("protocol") == -1);
	}
	
	
	/*
	 * Service metadata: Coupled Resource
	 */
	@Test
	public void testServiceCoupledResource() throws Exception{
		MetadataDocument document = getDocument("service_metadata.xml");

		// remove all child nodes
		int i = document.removeSVCoupledResource();
		
		// add new childnode
		document.addSVCoupledResource("GetMap", "bc509f92-5d8c-4169-818b-49ff6a7576c3", "PS.ProtectedSiteStilteGebieden");
		String result = document.getSVCoupledResource();		
		assertNotNull("No coupled resource found", result);
		assertTrue("No operationName found", result.indexOf("GetMap") > -1);
		
		// remove all child nodes		
		i = document.removeSVCoupledResource();
		assertTrue("There should be one removed coupled resource", i==1);
		
		// check no node anymore
		result = document.getSVCoupledResource();		
		assertTrue("Unexpected operationName found", result==null?true:result.indexOf("GetMap") == -1);
	}
	
	/*
	 * Service metadata: Link to dataset 
	 */
	@Test
	public void testServiceLinkToDataset() throws Exception{
		MetadataDocument document = getDocument("service_metadata.xml");

		Element result = (Element) document.getOperatesOn();		
		

		// remove all child nodes
		int i = document.removeOperatesOn();
		
		// add new childnode
		document.addOperatesOn("bc509f92-5d8c-4169-818b-49ff6a7576c3",
				"http://gisopenbaar.overijssel.nl/geoportal_extern/csw?Service=CSW&Request=GetRecordById&Version=2.0.2"+
				"&id=46647460-d8cf-4955-bcac-f1c192d57cc4&outputSchema=http://www.isotc211.org/2005/gmd&elementSetName=full");
		result = (Element) document.getOperatesOn();		
		assertNotNull("No link to dataset found", result);
		assertTrue("No uuid ref found", result.getAttribute("uuidref").indexOf("bc509f92-5d8c-4169-818b-49ff6a7576c3") > -1);
		
		// remove all child nodes		
		i = document.removeOperatesOn();
		assertTrue("There should be one removed dataset link", i==1);
		
		// check no node anymore
		result = (Element) document.getOperatesOn();		
		assertTrue("Unexpected uuid ref found", result==null?true:result.getAttribute("uuidref").indexOf("bc509f92-5d8c-4169-818b-49ff6a7576c3") == -1);
	}
	

}
