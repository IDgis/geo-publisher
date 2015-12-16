package nl.idgis.publisher.service.geoserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.DatabaseMock;
import nl.idgis.publisher.EmptyQueryResultTransactionMock;

import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.GroupLayerRef;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.RasterDatasetLayer;
import nl.idgis.publisher.domain.web.tree.Service;
import nl.idgis.publisher.domain.web.tree.StyleRef;
import nl.idgis.publisher.domain.web.tree.Tiling;
import nl.idgis.publisher.domain.web.tree.VectorDatasetLayer;

import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.EnsureServiceJobInfo;
import nl.idgis.publisher.job.manager.messages.VacuumServiceJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.Cleared;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.service.geoserver.rest.GeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.ServiceType;
import nl.idgis.publisher.service.geoserver.rest.Style;
import nl.idgis.publisher.service.geoserver.rest.TiledLayer;
import nl.idgis.publisher.service.geoserver.rest.Workspace;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.GetServiceIndex;
import nl.idgis.publisher.service.manager.messages.GetStyles;
import nl.idgis.publisher.service.manager.messages.ServiceIndex;
import nl.idgis.publisher.service.provisioning.ConnectionInfo;
import nl.idgis.publisher.service.provisioning.GeoServerProvisioningPropsFactory;
import nl.idgis.publisher.service.provisioning.ProvisioningManager;
import nl.idgis.publisher.service.provisioning.ServiceInfo;
import nl.idgis.publisher.service.provisioning.messages.AddStagingService;
import nl.idgis.publisher.service.provisioning.messages.GetEnvironments;
import nl.idgis.publisher.service.raster.TestRaster;
import nl.idgis.publisher.service.style.TestStyle;
import nl.idgis.publisher.stream.IteratorCursor;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.Logging;
import nl.idgis.publisher.utils.TypedList;

public class GeoServerServiceTest {
	
	private final static Config metadataEnvironmentConfig = ConfigFactory.parseString (
			"geoserver-public { "
				+ " serviceLinkagePrefix = \"http://public.example.com/geoserver/\", "
				+ " datasetMetadataPrefix = \"http://public.example.com/metadata/dataset/\", "
			+ "}, "
			+ " geoserver-secure { "
				+ " serviceLinkagePrefix = \"https://secure.example.com/geoserver/\", "
				+ " datasetMetadataPrefix = \"https://secure.example.com/metadata/dataset/\", "
			+ " }, "
			+ " geoserver-guaranteed { "
				+ " serviceLinkagePrefix = \"http://guaranteed.example.com/geoserver/\", "
				+ " datasetMetadataPrefix = \"http://guaranteed.example.com/metadata/dataset/\", "
			+ " }");
			
	static class PutServiceIndex implements Serializable {
		
		private static final long serialVersionUID = -5881906101843611427L;
		
		private final ServiceIndex serviceIndex;
		
		public PutServiceIndex(ServiceIndex serviceIndex) {
			this.serviceIndex = serviceIndex;
		}
		
		public ServiceIndex getServiceIndex() {
			return serviceIndex;
		}
	}
	
	static class PutService implements Serializable {

		private static final long serialVersionUID = 7974047966502087805L;

		private final String serviceId;
		
		private final Service service;
		
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
	
	static class PutStyle implements Serializable {	

		private static final long serialVersionUID = -8235556377963337675L;

		private final String styleName;
		
		private final Document sld;
		
		public PutStyle(String styleName, Document sld) {
			this.styleName = styleName;
			this.sld = sld;
		}

		public String getStyleName() {
			return styleName;
		}

		public Document getSld() {
			return sld;
		}
	}
	
	
	
	static class ServiceManagerMock extends UntypedActor {
		
		private Map<String, Service> services = new HashMap<>();
		
		private List<nl.idgis.publisher.service.manager.messages.Style> styles = new ArrayList<>();
		
		private ServiceIndex serviceIndex;
		
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
			} else if(msg instanceof GetServiceIndex) {
				if(serviceIndex != null) {
					getSender().tell(serviceIndex, getSelf());
				}
			} else if(msg instanceof GetStyles) {
				getContext().actorOf(IteratorCursor.props(styles.iterator())).tell(new NextItem(), getSender());
			} else if(msg instanceof PutService) {
				PutService putService = (PutService)msg;
				services.put(putService.getServiceId(), putService.getService());
				getSender().tell(new Ack(), getSelf());
			} else if(msg instanceof PutServiceIndex) {
				serviceIndex = ((PutServiceIndex)msg).getServiceIndex();				
				getSender().tell(new Ack(), getSelf());
			} else if(msg instanceof PutStyle) {
				PutStyle putStyle = (PutStyle)msg;
				styles.add(new nl.idgis.publisher.service.manager.messages.Style(putStyle.getStyleName(), putStyle.getSld()));
				getSender().tell(new Ack(), getSelf());
			} else {
				unhandled(msg);
			}
		}
	}
	
	public static class EnvironmentInfoProviderMock extends UntypedActor {
		
		private final LoggingAdapter log = Logging.getLogger();
		
		private final ActorRef database;
		
		private FutureUtils f;
		
		private AsyncDatabaseHelper db;
		
		public EnvironmentInfoProviderMock(ActorRef database) {
			this.database = database;
		}
		
		public static Props props(ActorRef database) {
			return Props.create(EnvironmentInfoProviderMock.class, database);
		}
		
		@Override
		public void preStart() throws Exception {
			f = new FutureUtils(getContext());
			db = new AsyncDatabaseHelper(database, f, log);
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof GetEnvironments) {
				ActorRef sender = getSender();
				db.transactional((GetEnvironments)msg, tx ->
					f.successful(new TypedList<>(String.class, Arrays.asList("environmentId")))).thenAccept(resp ->
						sender.tell(resp, getSelf()));
			} else {
				unhandled(msg);
			}
		}
		
	}
	
	static LoggingAdapter log = Logging.getLogger();
	
	static GeoServerTestHelper h;
	
	ActorSystem actorSystem;
	
	FutureUtils f;
	
	ActorRef serviceManager, provisioningManager, recorder;
		
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
		
		f = new FutureUtils(actorSystem, Timeout.apply(30, TimeUnit.SECONDS));
		
		serviceManager = actorSystem.actorOf(ServiceManagerMock.props(), "service-manager");
		
		ActorRef database = actorSystem.actorOf(DatabaseMock.props(EmptyQueryResultTransactionMock.props()), "database");
		
		provisioningManager = actorSystem.actorOf(ProvisioningManager.props(database, serviceManager, new GeoServerProvisioningPropsFactory() {

			@Override
			public Props environmentInfoProviderProps(ActorRef database) {				
				return EnvironmentInfoProviderMock.props(database);
			}
			
		}, metadataEnvironmentConfig), "provisioning-manager");
		
		ActorRef updateServiceInfoRecorder = actorSystem.actorOf(AnyRecorder.props(), "update-service-info-recorder");
		
		String rasterFolder = TestRaster.getRasterFolder();
		
		provisioningManager.tell(new AddStagingService(new ServiceInfo(
			new ConnectionInfo(
					"http://localhost:" + GeoServerTestHelper.JETTY_PORT + "/",
					"admin",
					"geoserver"),
			rasterFolder)), 
					
			updateServiceInfoRecorder);
		
		// wait for update acknowledgement
		f.ask(updateServiceInfoRecorder, new Wait(1), Waited.class).get();
		
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
		String[] styleNames = {"style0", "style1"};
		
		for(String styleName : styleNames)  {
			f.ask(serviceManager, new PutStyle(styleName, TestStyle.getGreenSld())).get();
		}
		
		VectorDatasetLayer datasetLayer = mock(VectorDatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer");
		when(datasetLayer.getTitle()).thenReturn("title");
		when(datasetLayer.getAbstract()).thenReturn("abstract");
		when(datasetLayer.getKeywords()).thenReturn(Arrays.asList("vector", "layer"));
		when(datasetLayer.isVectorLayer()).thenReturn(true);
		when(datasetLayer.isRasterLayer()).thenReturn(false);
		when(datasetLayer.asVectorLayer()).thenReturn(datasetLayer);
		when(datasetLayer.getTableName()).thenReturn("myTable");		
		when(datasetLayer.getTiling()).thenReturn(Optional.empty());
		when(datasetLayer.getImportTime()).thenReturn(Optional.empty());
		
		List<StyleRef> styleRefs = Arrays.asList(styleNames).stream()
			.map(styleName -> {
				StyleRef styleRef = mock(StyleRef.class);					
				when(styleRef.getName()).thenReturn(styleName);
				
				return styleRef;
			})
			.collect(Collectors.toList());
		
		when(datasetLayer.getStyleRefs()).thenReturn(styleRefs);
		
		DatasetLayerRef datasetLayerRef = mock(DatasetLayerRef.class);
		when(datasetLayerRef.isGroupRef()).thenReturn(false);
		when(datasetLayerRef.asDatasetRef()).thenReturn(datasetLayerRef);
		when(datasetLayerRef.getLayer()).thenReturn(datasetLayer);
		when(datasetLayerRef.getStyleRef()).thenReturn(Optional.empty());
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(datasetLayerRef));
		
		f.ask(serviceManager, new PutService("service", service), Ack.class).get();
		
		provisioningManager.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		f.ask(recorder, new Wait(3), Waited.class).get();
		assertSuccessful(f.ask(recorder, new GetRecording(), Recording.class).get());
		
		Document features = h.getFeature("serviceName", "layer");		
		assertTrue(h.getText(features).contains("Hello, world!"));
		
		Document capabilities = h.getCapabilities("serviceName", ServiceType.WMS, "1.3.0");
		assertEquals("layer", h.getText("//wms:Layer/wms:Name", capabilities));
		assertEquals("title", h.getText("//wms:Layer[wms:Name = 'layer']/wms:Title", capabilities));
		assertEquals("abstract", h.getText("//wms:Layer[wms:Name = 'layer']/wms:Abstract", capabilities));		
		assertEquals(Arrays.asList("vector", "layer"), h.getText(h.getNodeList("//wms:Layer[wms:Name = 'layer']/wms:KeywordList/wms:Keyword", capabilities)));
		
		NodeList styles = h.getNodeList("//wms:Layer/wms:Style/wms:Name", capabilities);
		assertEquals(styleNames.length, styles.getLength());
		for(int i = 0; i < styleNames.length; i++) {
			assertEquals(styleNames[i], styles.item(i).getTextContent());
		}
		
		assertTrue(h.rest(f, log).getTiledLayerNames(new Workspace("serviceName")).get().isEmpty());
	}
	
	@Test
	public void testGroupLayer() throws Exception {
		final int numberOfLayers = 10;
		
		List<LayerRef<?>> layers = new ArrayList<>();
		for(int i = 0; i < numberOfLayers; i++) {
			Tiling tilingSettings = mock(Tiling.class);
			when(tilingSettings.getMimeFormats()).thenReturn(Arrays.asList("image/png"));
			when(tilingSettings.getExpireCache()).thenReturn(0);
			when(tilingSettings.getExpireClients()).thenReturn(0);
			when(tilingSettings.getMetaHeight()).thenReturn(4);
			when(tilingSettings.getMetaWidth()).thenReturn(4);
			when(tilingSettings.getGutter()).thenReturn(0);
			
			VectorDatasetLayer layer = mock(VectorDatasetLayer.class);			
			when(layer.getName()).thenReturn("layer" + i);
			when(layer.isVectorLayer()).thenReturn(true);
			when(layer.asVectorLayer()).thenReturn(layer);
			when(layer.getTableName()).thenReturn("myTable");			
			when(layer.getTiling()).thenReturn(Optional.of(tilingSettings));
			when(layer.getImportTime()).thenReturn(Optional.empty());
			
			DatasetLayerRef layerRef = mock(DatasetLayerRef.class);
			when(layerRef.isGroupRef()).thenReturn(false);
			when(layerRef.asDatasetRef()).thenReturn(layerRef);
			when(layerRef.getLayer()).thenReturn(layer);
			when(layerRef.getStyleRef()).thenReturn(Optional.empty());
			
			layers.add(layerRef);
		}
		
		GroupLayer groupLayer = mock(GroupLayer.class);		
		when(groupLayer.getName()).thenReturn("group");
		when(groupLayer.getTitle()).thenReturn("groupTitle");
		when(groupLayer.getAbstract()).thenReturn("groupAbstract");
		when(groupLayer.getLayers()).thenReturn(layers);
		when(groupLayer.getTiling()).thenReturn(Optional.empty());
		when(groupLayer.getImportTime()).thenReturn(Optional.empty());
		
		GroupLayerRef groupLayerRef = mock(GroupLayerRef.class);
		when(groupLayerRef.isGroupRef()).thenReturn(true);
		when(groupLayerRef.asGroupRef()).thenReturn(groupLayerRef);
		when(groupLayerRef.getLayer()).thenReturn(groupLayer);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(groupLayerRef));
		
		f.ask(serviceManager, new PutService("service", service), Ack.class).get();
		
		provisioningManager.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		f.ask(recorder, new Wait(3), Waited.class).get();
		assertSuccessful(f.ask(recorder, new GetRecording(), Recording.class).get());
		
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
		
		GeoServerRest rest = h.rest(f, log);
		Workspace workspace = new Workspace("serviceName");
		Set<String> tiledLayers = rest.getTiledLayerNames(workspace).get()
			.stream().collect(Collectors.toSet());
		
		for(int i = 0; i < numberOfLayers; i++) {
			String tiledLayerName = "layer" + i;
			assertTrue(tiledLayers.contains(tiledLayerName));
			
			Optional<TiledLayer> tiledLayerOptional = rest.getTiledLayer(workspace, tiledLayerName).get();
			assertTrue(tiledLayerOptional.isPresent());
			TiledLayer tiledLayer = tiledLayerOptional.get();
			assertEquals(Arrays.asList("image/png"), tiledLayer.getMimeFormats());
		}
		
		// remove group layer and its content
		service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.emptyList());
		
		f.ask(serviceManager, new PutService("service", service), Ack.class).get();
		
		f.ask(recorder, new Clear(), Cleared.class).get();
		provisioningManager.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		f.ask(recorder, new Wait(3), Waited.class).get();
		assertSuccessful(f.ask(recorder, new GetRecording(), Recording.class).get());
	}
	
	@Test
	public void testRemoveLayer() throws Exception{
		VectorDatasetLayer datasetLayer = mock(VectorDatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer");
		when(datasetLayer.isVectorLayer()).thenReturn(true);
		when(datasetLayer.asVectorLayer()).thenReturn(datasetLayer);
		when(datasetLayer.getTableName()).thenReturn("myTable");		
		when(datasetLayer.getTiling()).thenReturn(Optional.empty());
		when(datasetLayer.getImportTime()).thenReturn(Optional.empty());
		
		DatasetLayerRef datasetLayerRef = mock(DatasetLayerRef.class);
		when(datasetLayerRef.isGroupRef()).thenReturn(false);
		when(datasetLayerRef.asDatasetRef()).thenReturn(datasetLayerRef);
		when(datasetLayerRef.getLayer()).thenReturn(datasetLayer);
		when(datasetLayerRef.getStyleRef()).thenReturn(Optional.empty());
		when(datasetLayerRef.getStyleRef()).thenReturn(Optional.empty());
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(datasetLayerRef));
		
		f.ask(serviceManager, new PutService("service", service), Ack.class).get();
		
		provisioningManager.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		f.ask(recorder, new Wait(3), Waited.class).get();
		assertSuccessful(f.ask(recorder, new GetRecording(), Recording.class).get());
		f.ask(recorder, new Clear(), Cleared.class).get();
		
		Document capabilities = h.getCapabilities("serviceName", ServiceType.WMS, "1.3.0");
		assertEquals("layer", h.getText("//wms:Layer/wms:Name", capabilities));
		
		service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.emptyList()); // layer removed
		
		f.ask(serviceManager, new PutService("service", service), Ack.class).get();
		
		provisioningManager.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		f.ask(recorder, new Wait(3), Waited.class).get();
		assertSuccessful(f.ask(recorder, new GetRecording(), Recording.class).get());
		
		capabilities = h.getCapabilities("serviceName", ServiceType.WMS, "1.3.0");		
		h.notExists("//wms:Layer/wms:Name", capabilities);
	}
	
	@Test
	public void testGroupInGroup() throws Exception {
		VectorDatasetLayer datasetLayer = mock(VectorDatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer");
		when(datasetLayer.getTitle()).thenReturn("title");
		when(datasetLayer.getAbstract()).thenReturn("abstract");
		when(datasetLayer.isVectorLayer()).thenReturn(true);
		when(datasetLayer.asVectorLayer()).thenReturn(datasetLayer);
		when(datasetLayer.getTableName()).thenReturn("myTable");		
		when(datasetLayer.getTiling()).thenReturn(Optional.empty());
		when(datasetLayer.getImportTime()).thenReturn(Optional.empty());
		
		DatasetLayerRef datasetLayerRef = mock(DatasetLayerRef.class);
		when(datasetLayerRef.isGroupRef()).thenReturn(false);
		when(datasetLayerRef.asDatasetRef()).thenReturn(datasetLayerRef);
		when(datasetLayerRef.getLayer()).thenReturn(datasetLayer);
		when(datasetLayerRef.getStyleRef()).thenReturn(Optional.empty());
		
		GroupLayer group0 = mock(GroupLayer.class);		
		when(group0.getName()).thenReturn("group0");
		when(group0.getTitle()).thenReturn("groupTitle0");
		when(group0.getAbstract()).thenReturn("groupAbstract0");
		when(group0.getLayers()).thenReturn(Collections.singletonList(datasetLayerRef));
		when(group0.getTiling()).thenReturn(Optional.empty());
		when(group0.getImportTime()).thenReturn(Optional.empty());
		
		GroupLayerRef group0Ref = mock(GroupLayerRef.class);
		when(group0Ref.isGroupRef()).thenReturn(true);
		when(group0Ref.asGroupRef()).thenReturn(group0Ref);
		when(group0Ref.getLayer()).thenReturn(group0);
		
		GroupLayer group1 = mock(GroupLayer.class);		
		when(group1.getName()).thenReturn("group1");
		when(group1.getTitle()).thenReturn("groupTitle1");
		when(group1.getAbstract()).thenReturn("groupAbstract1");
		when(group1.getLayers()).thenReturn(Collections.singletonList(group0Ref));
		when(group1.getTiling()).thenReturn(Optional.empty());
		when(group1.getImportTime()).thenReturn(Optional.empty());
		
		GroupLayerRef group1Ref = mock(GroupLayerRef.class);
		when(group1Ref.isGroupRef()).thenReturn(true);
		when(group1Ref.asGroupRef()).thenReturn(group1Ref);
		when(group1Ref.getLayer()).thenReturn(group1);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(group1Ref));
		
		f.ask(serviceManager, new PutService("service", service), Ack.class).get();
		
		provisioningManager.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		f.ask(recorder, new Wait(3), Waited.class).get();
		assertSuccessful(f.ask(recorder, new GetRecording(), Recording.class).get());
		
		Document capabilities = h.getCapabilities("serviceName", ServiceType.WMS, "1.3.0");		
		assertEquals("serviceName:layer", h.getText("//wms:Layer/wms:Layer/wms:Layer/wms:Layer/wms:Name", capabilities));
		assertEquals("serviceName:layer", h.getText("//wms:Layer/wms:Layer/wms:Layer[wms:Name = 'serviceName:group0']/wms:Layer/wms:Name", capabilities));
		assertEquals("serviceName:layer", h.getText("//wms:Layer/wms:Layer[wms:Name = 'group1']/wms:Layer[wms:Name = 'serviceName:group0']/wms:Layer/wms:Name", capabilities));
		
		
	}
	
	@Test
	public void testVacuum() throws Exception {
		GeoServerRest rest = h.rest(f, log);
		
		rest.postWorkspace(new Workspace("workspace")).get();
		rest.postStyle(new Style("style", TestStyle.getGreenSld())).get();
		
		assertTrue(
			rest.getWorkspaces().get().stream()
				.map(workspace -> workspace.getName())
				.collect(Collectors.toSet())
					.contains("workspace"));		
		assertTrue(
			rest.getStyleNames().get().stream()				
				.collect(Collectors.toSet())
					.contains("style"));
				
		f.ask(serviceManager, new PutServiceIndex(new ServiceIndex(
			Arrays.asList("workspace"),
			// includes the default styles to prevent other tests from running properly
			Arrays.asList("point", "line", "polygon", "raster", "style"))), Ack.class).get();
		
		provisioningManager.tell(new VacuumServiceJobInfo(0), recorder);
		f.ask(recorder, new Wait(3), Waited.class).get();
		assertSuccessful(f.ask(recorder, new GetRecording(), Recording.class).get());
		
		assertTrue(
			rest.getWorkspaces().get().stream()
				.map(workspace -> workspace.getName())
				.collect(Collectors.toSet())
					.contains("workspace"));
		assertTrue(
			rest.getStyleNames().get().stream()				
				.collect(Collectors.toSet())
					.contains("style"));
		
		f.ask(serviceManager, new PutServiceIndex(new ServiceIndex(
			Collections.emptyList(),
			Arrays.asList("point", "line", "polygon", "raster", "style"))), Ack.class).get();
		
		f.ask(recorder, new Clear(), Cleared.class).get();
		
		provisioningManager.tell(new VacuumServiceJobInfo(0), recorder);
		f.ask(recorder, new Wait(3), Waited.class).get();
		assertSuccessful(f.ask(recorder, new GetRecording(), Recording.class).get());
		
		assertTrue(rest.getWorkspaces().get().isEmpty());
		assertTrue(
			rest.getStyleNames().get().stream()				
				.collect(Collectors.toSet())
					.contains("style"));
		
		f.ask(serviceManager, new PutServiceIndex(new ServiceIndex(
			Collections.emptyList(),
			Arrays.asList("point", "line", "polygon", "raster"))), Ack.class).get();
			
		f.ask(recorder, new Clear(), Cleared.class).get();
		
		provisioningManager.tell(new VacuumServiceJobInfo(0), recorder);
		f.ask(recorder, new Wait(3), Waited.class).get();
		assertSuccessful(f.ask(recorder, new GetRecording(), Recording.class).get());
		
		assertTrue(rest.getWorkspaces().get().isEmpty());
		assertFalse(
			rest.getStyleNames().get().stream()				
				.collect(Collectors.toSet())
					.contains("style"));
	}
	
	@Test
	public void testRasterLayer() throws Exception {
		URL testRasterUrl = TestRaster.getRasterUrl();
		assertEquals("file", testRasterUrl.getProtocol());
		
		File testRasterFile = new File(testRasterUrl.toURI ().getPath ());
		assertTrue(testRasterFile.exists());
		
		String[] styleNames = {"style0", "style1"};
		
		for(String styleName : styleNames)  {
			f.ask(serviceManager, new PutStyle(styleName, TestStyle.getRasterSld())).get();
		}
		
		RasterDatasetLayer datasetLayer = mock(RasterDatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("raster-layer");
		when(datasetLayer.getTitle()).thenReturn("raster-title");
		when(datasetLayer.getAbstract()).thenReturn("raster-abstract");
		when(datasetLayer.getKeywords()).thenReturn(Arrays.asList("raster", "layer"));
		when(datasetLayer.isVectorLayer()).thenReturn(false);
		when(datasetLayer.isRasterLayer()).thenReturn(true);
		when(datasetLayer.asRasterLayer()).thenReturn(datasetLayer);
		when(datasetLayer.getFileName()).thenReturn(testRasterFile.getName());
		when(datasetLayer.getTiling()).thenReturn(Optional.empty());
		when(datasetLayer.getImportTime()).thenReturn(Optional.empty());
		
		List<StyleRef> styleRefs = Arrays.asList(styleNames).stream()
			.map(styleName -> {
				StyleRef styleRef = mock(StyleRef.class);					
				when(styleRef.getName()).thenReturn(styleName);
				
				return styleRef;
			})
			.collect(Collectors.toList());
		
		when(datasetLayer.getStyleRefs()).thenReturn(styleRefs);
		
		DatasetLayerRef datasetLayerRef = mock(DatasetLayerRef.class);
		when(datasetLayerRef.isGroupRef()).thenReturn(false);
		when(datasetLayerRef.asDatasetRef()).thenReturn(datasetLayerRef);
		when(datasetLayerRef.getLayer()).thenReturn(datasetLayer);
		when(datasetLayerRef.getStyleRef()).thenReturn(Optional.empty());
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getName()).thenReturn("serviceName");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(datasetLayerRef));
		
		f.ask(serviceManager, new PutService("service", service), Ack.class).get();
		
		provisioningManager.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		f.ask(recorder, new Wait(3), Waited.class).get();
		assertSuccessful(f.ask(recorder, new GetRecording(), Recording.class).get());
		
		Document capabilities = h.getCapabilities("serviceName", ServiceType.WMS, "1.3.0");
		assertEquals("raster-layer", h.getText("//wms:Layer/wms:Name", capabilities));
		assertEquals("raster-title", h.getText("//wms:Layer[wms:Name = 'raster-layer']/wms:Title", capabilities));
		assertEquals("raster-abstract", h.getText("//wms:Layer[wms:Name = 'raster-layer']/wms:Abstract", capabilities));
		assertEquals(Arrays.asList("raster", "layer"), h.getText(h.getNodeList("//wms:Layer[wms:Name = 'raster-layer']/wms:KeywordList/wms:Keyword", capabilities)));
		
		// remove raster layer
		Service emptyService = mock(Service.class);
		when(emptyService.getId()).thenReturn("service");
		when(emptyService.getName()).thenReturn("serviceName");
		when(emptyService.getRootId()).thenReturn("root");
		when(emptyService.getLayers()).thenReturn(Collections.emptyList());
		
		f.ask(serviceManager, new PutService("service", emptyService), Ack.class).get();
		
		f.ask(recorder, new Clear(), Cleared.class);
		
		provisioningManager.tell(new EnsureServiceJobInfo(0, "service"), recorder);
		f.ask(recorder, new Wait(3), Waited.class).get();
		assertSuccessful(f.ask(recorder, new GetRecording(), Recording.class).get());
		
		capabilities = h.getCapabilities("serviceName", ServiceType.WMS, "1.3.0");
		assertEquals(0, h.getNodeList("//wms:Layer/wms:Name", capabilities).getLength());
	}
}
