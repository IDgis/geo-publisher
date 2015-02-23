package nl.idgis.publisher.service.geoserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.EnsureServiceJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.Cleared;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.service.geoserver.rest.ServiceType;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.Logging;
import nl.idgis.publisher.utils.SyncAskHelper;

public class GeoServerServiceTest {
	
	static class PutService implements Serializable {

		private static final long serialVersionUID = 7974047966502087805L;

		private String serviceId;
		
		private Service service;
		
		public PutService(String serviceId, Service service) {
			this.serviceId = serviceId;
			this.service = service;
		} 
		
		public String getServiceId() {
			return serviceId;			
		}
		
		public Service getService() {
			return service;
		}
	}
	
	static class ServiceManagerMock extends UntypedActor {
		
		private Map<String, Service> services = new HashMap<>();
		
		public static Props props() {
			return Props.create(ServiceManagerMock.class);
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof GetService) {
				String serviceId = ((GetService)msg).getServiceId();
				if(services.containsKey(serviceId)) {
					getSender().tell(services.get(serviceId), getSelf());
				} else {
					getSender().tell(new NotFound(), getSelf());
				}
			} else if(msg instanceof PutService) {
				PutService putService = (PutService)msg;
				services.put(putService.getServiceId(), putService.getService());
				getSender().tell(new Ack(), getSelf());
			} else {
				unhandled(msg);
			}
		}
	}
	
	static LoggingAdapter log = Logging.getLogger();
	
	static GeoServerTestHelper h;
	
	ActorSystem actorSystem;
	
	FutureUtils f;
	
	ActorRef serviceManager, geoServerService, recorder;
	
	SyncAskHelper sync;
	
	@BeforeClass
	public static void testServers() throws Exception {
		h = new GeoServerTestHelper();
		h.start();
		
		Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + GeoServerTestHelper.PG_PORT + "/test", "postgres", "postgres");
		
		Statement stmt = connection.createStatement();
		stmt.execute("create schema \"staging_data\"");
		stmt.execute("create table \"staging_data\".\"myTable\"(\"id\" serial primary key, \"label\" text)");
		stmt.execute("select AddGeometryColumn ('staging_data', 'myTable', 'the_geom', 4326, 'GEOMETRY', 2)");
		stmt.execute("insert into \"staging_data\".\"myTable\"(\"label\", \"the_geom\") select 'Hello, world!', st_geomfromtext('POINT(42.0 47.0)', 4326)");
		
		stmt.close();
		
		connection.close();
	}
	
	@AfterClass
	public static void stopServers() throws Exception {
		h.stop();
	}
	
	@After
	public void clean() throws Exception {
		h.clean(f, log);
	}
	
	@Before
	public void actors() throws Exception {
		
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.create("test", akkaConfig);
		
		f = new FutureUtils(actorSystem.dispatcher());
		
		serviceManager = actorSystem.actorOf(ServiceManagerMock.props(), "service-manager");
		
		Config geoserverConfig = ConfigFactory.empty()
			.withValue("url", ConfigValueFactory.fromAnyRef("http://localhost:" + GeoServerTestHelper.JETTY_PORT + "/"))
			.withValue("user", ConfigValueFactory.fromAnyRef("admin"))
			.withValue("password", ConfigValueFactory.fromAnyRef("geoserver"))
			.withValue("schema", ConfigValueFactory.fromAnyRef("staging_data"));
		
		Config databaseConfig = ConfigFactory.empty()
			.withValue("url", ConfigValueFactory.fromAnyRef("jdbc:postgresql://localhost:" + GeoServerTestHelper.PG_PORT + "/test"))
			.withValue("user", ConfigValueFactory.fromAnyRef("postgres"))
			.withValue("password", ConfigValueFactory.fromAnyRef("postgres"));
		
		geoServerService = actorSystem.actorOf(GeoServerService.props(serviceManager, geoserverConfig, databaseConfig));
		
		sync = new SyncAskHelper(actorSystem, Timeout.apply(30, TimeUnit.SECONDS));
		
		recorder = actorSystem.actorOf(AnyRecorder.props(), "recorder");
	}
	
	private void assertSuccessful(Recording recording) throws Exception {
		recording
			.assertNext(UpdateJobState.class, updateJobState -> {
				assertEquals(JobState.STARTED, updateJobState.getState());
			})
			.assertNext(Ack.class)
			.assertNext(UpdateJobState.class, updateJobState -> {
				assertEquals(JobState.SUCCEEDED, updateJobState.getState());
			})
			.assertNotHasNext();
	}
	
	@Test
	public void testSingleLayer() throws Exception {
		DatasetLayer datasetLayer = mock(DatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer");
		when(datasetLayer.getTitle()).thenReturn("title");
		when(datasetLayer.getAbstract()).thenReturn("abstract");
		when(datasetLayer.getTableName()).thenReturn("myTable");
		when(datasetLayer.isGroup()).thenReturn(false);
		when(datasetLayer.asDataset()).thenReturn(datasetLayer);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(datasetLayer));
		
		sync.ask(serviceManager, new PutService("service", service), Ack.class);
		
		geoServerService.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		sync.ask(recorder, new Wait(3), Waited.class);
		assertSuccessful(sync.ask(recorder, new GetRecording(), Recording.class));
		
		Document features = h.getFeature("serviceName", "layer");		
		assertTrue(h.getText(features).contains("Hello, world!"));
		
		Document capabilities = h.getCapabilities("serviceName", ServiceType.WMS, "1.3.0");
		assertEquals("layer", h.getText("//wms:Layer/wms:Name", capabilities));
		assertEquals("title", h.getText("//wms:Layer[wms:Name = 'layer']/wms:Title", capabilities));
		assertEquals("abstract", h.getText("//wms:Layer[wms:Name = 'layer']/wms:Abstract", capabilities));
	}
	
	@Test
	public void testGroupLayer() throws Exception {
		final int numberOfLayers = 10;
		
		List<Layer> layers = new ArrayList<>();
		for(int i = 0; i < numberOfLayers; i++) {
			DatasetLayer layer = mock(DatasetLayer.class);
			when(layer.isGroup()).thenReturn(false);
			when(layer.asDataset()).thenReturn(layer);
			when(layer.getName()).thenReturn("layer" + i);
			when(layer.getTableName()).thenReturn("myTable");
			
			layers.add(layer);
		}
		
		GroupLayer groupLayer = mock(GroupLayer.class);
		when(groupLayer.isGroup()).thenReturn(true);
		when(groupLayer.asGroup()).thenReturn(groupLayer);
		when(groupLayer.getName()).thenReturn("group");
		when(groupLayer.getTitle()).thenReturn("groupTitle");
		when(groupLayer.getAbstract()).thenReturn("groupAbstract");
		when(groupLayer.getLayers()).thenReturn(layers);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(groupLayer));
		
		sync.ask(serviceManager, new PutService("service", service), Ack.class);
		
		geoServerService.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		sync.ask(recorder, new Wait(3), Waited.class);
		assertSuccessful(sync.ask(recorder, new GetRecording(), Recording.class));
		
		Document capabilities = h.getCapabilities("serviceName", ServiceType.WMS, "1.3.0");
		List<String> layerNames = h.getText(h.getNodeList("//wms:Layer/wms:Name", capabilities));
		for(int i = 0; i < numberOfLayers; i++) {
			assertTrue(layerNames.contains("serviceName:layer" + i)); // TODO: figure out how to remove the workspace prefix from the name
		}
		assertTrue(layerNames.contains("group"));
		
		layerNames = h.getText(h.getNodeList("//wms:Layer[wms:Name = 'group']/wms:Layer/wms:Name", capabilities));
		for(int i = 0; i < numberOfLayers; i++) {
			assertTrue(layerNames.contains("serviceName:layer" + i));
		}
		assertFalse(layerNames.contains("group"));
		
		assertEquals("groupTitle", h.getText("//wms:Layer[wms:Name = 'group']/wms:Title", capabilities));
		assertEquals("groupAbstract", h.getText("//wms:Layer[wms:Name = 'group']/wms:Abstract", capabilities));
	}
	
	@Test
	public void testRemoveLayer() throws Exception{
		DatasetLayer datasetLayer = mock(DatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer");		
		when(datasetLayer.getTableName()).thenReturn("myTable");
		when(datasetLayer.isGroup()).thenReturn(false);
		when(datasetLayer.asDataset()).thenReturn(datasetLayer);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(datasetLayer));
		
		sync.ask(serviceManager, new PutService("service", service), Ack.class);
		
		geoServerService.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		sync.ask(recorder, new Wait(3), Waited.class);
		assertSuccessful(sync.ask(recorder, new GetRecording(), Recording.class));
		sync.ask(recorder, new Clear(), Cleared.class);
		
		Document capabilities = h.getCapabilities("serviceName", ServiceType.WMS, "1.3.0");
		assertEquals("layer", h.getText("//wms:Layer/wms:Name", capabilities));
		
		service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.emptyList()); // layer removed
		
		sync.ask(serviceManager, new PutService("service", service), Ack.class);
		
		geoServerService.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		sync.ask(recorder, new Wait(3), Waited.class);
		assertSuccessful(sync.ask(recorder, new GetRecording(), Recording.class));
		
		capabilities = h.getCapabilities("serviceName", ServiceType.WMS, "1.3.0");		
		h.notExists("//wms:Layer/wms:Name", capabilities);
	}
	
	@Test
	public void testGroupInGroup() throws Exception {
		DatasetLayer datasetLayer = mock(DatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer");
		when(datasetLayer.getTitle()).thenReturn("title");
		when(datasetLayer.getAbstract()).thenReturn("abstract");
		when(datasetLayer.getTableName()).thenReturn("myTable");
		when(datasetLayer.isGroup()).thenReturn(false);
		when(datasetLayer.asDataset()).thenReturn(datasetLayer);
		
		GroupLayer group0 = mock(GroupLayer.class);
		when(group0.isGroup()).thenReturn(true);
		when(group0.asGroup()).thenReturn(group0);
		when(group0.getName()).thenReturn("group0");
		when(group0.getTitle()).thenReturn("groupTitle0");
		when(group0.getAbstract()).thenReturn("groupAbstract0");
		when(group0.getLayers()).thenReturn(Collections.singletonList(datasetLayer));
		
		GroupLayer group1 = mock(GroupLayer.class);
		when(group1.isGroup()).thenReturn(true);
		when(group1.asGroup()).thenReturn(group1);
		when(group1.getName()).thenReturn("group1");
		when(group1.getTitle()).thenReturn("groupTitle1");
		when(group1.getAbstract()).thenReturn("groupAbstract1");
		when(group1.getLayers()).thenReturn(Collections.singletonList(group0));
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(group1));
		
		sync.ask(serviceManager, new PutService("service", service), Ack.class);
		
		geoServerService.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		sync.ask(recorder, new Wait(3), Waited.class);
		assertSuccessful(sync.ask(recorder, new GetRecording(), Recording.class));
		
		Document capabilities = h.getCapabilities("serviceName", ServiceType.WMS, "1.3.0");		
		assertEquals("serviceName:layer", h.getText("//wms:Layer/wms:Layer/wms:Layer/wms:Layer/wms:Name", capabilities));
		assertEquals("serviceName:layer", h.getText("//wms:Layer/wms:Layer/wms:Layer[wms:Name = 'serviceName:group0']/wms:Layer/wms:Name", capabilities));
		assertEquals("serviceName:layer", h.getText("//wms:Layer/wms:Layer[wms:Name = 'group1']/wms:Layer[wms:Name = 'serviceName:group0']/wms:Layer/wms:Name", capabilities));
	}
	
	
}
