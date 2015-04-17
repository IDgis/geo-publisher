package nl.idgis.publisher.service.geoserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.GroupLayerRef;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.Service;
import nl.idgis.publisher.domain.web.tree.Tiling;
import nl.idgis.publisher.domain.web.tree.VectorDatasetLayer;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.Recorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.RecordedMessage;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.service.geoserver.messages.EnsureFeatureTypeLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureGroupLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureStyle;
import nl.idgis.publisher.service.geoserver.messages.EnsureWorkspace;
import nl.idgis.publisher.service.geoserver.messages.Ensured;
import nl.idgis.publisher.service.geoserver.messages.FinishEnsure;
import nl.idgis.publisher.service.manager.messages.Style;
import nl.idgis.publisher.service.style.TestStyle;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.UniqueNameGenerator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static java.util.Collections.singleton;

public class EnsureServiceTest {
	
	private static class Ensure {
		
		private final List<Object> messages;
		
		Ensure(Object... messages) {
			this.messages = Arrays.asList(messages);
		}
		
		public List<Object> getMessages() {
			return messages;
		}
	}
	
	static class GeoServerServiceMock extends UntypedActor {
		
		private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
		
		private final ActorRef recorder;
		
		public GeoServerServiceMock(ActorRef recorder) {
			this.recorder = recorder;
		}
		
		public static Props props(ActorRef recorder) {
			return Props.create(GeoServerServiceMock.class, recorder);
		}
		
		private void unexpected(Object msg) {
			if(msg instanceof Terminated) {
				record(msg);
			} else {			
				log.error("unhandled: {}", msg);
			
				unhandled(msg);
			}
		}
		
		private void record(Object msg) {
			log.debug("recorded: {}", msg);
			
			recorder.tell(new RecordedMessage(getSelf(), getSender(), msg), getSelf());
		}
		
		private void ensured() {
			log.debug("ensured");
			
			getSender().tell(new Ensured(), getSelf());
		}
		
		private Procedure<Object> layers(ActorRef initiator) {
			return layers(initiator, 0);
		}
		
		private Procedure<Object> layers(ActorRef initiator, int depth) {
			log.debug("group");
			
			return new Procedure<Object>() {

				@Override
				public void apply(Object msg) throws Exception {
					record(msg);
					
					if(msg instanceof EnsureGroupLayer) {
						ensured();
						getContext().become(layers(initiator, depth + 1), false);
					} else if(msg instanceof EnsureFeatureTypeLayer) {
						ensured();
					} else if(msg instanceof FinishEnsure) {
						ensured();
						
						if(depth == 0) {
							log.debug("ack");
							initiator.tell(new Ack(), getSelf());							
						} else {
							log.debug("unbecome {}", depth);	
						}
						
						getContext().unbecome();
					} else {
						unexpected(msg);
					}
				}				
			};
		}
		
		private Procedure<Object> provisioning(ActorRef initiator) {
			log.debug("provisioning");
			
			return new Procedure<Object>() {

				@Override
				public void apply(Object msg) throws Exception {
					if(msg instanceof EnsureStyle) {
						record(msg);
						ensured();
					} else if(msg instanceof EnsureWorkspace) {
						record(msg);
						ensured();
						getContext().become(layers(initiator), false);
					} else {
						unexpected(msg);
					}
				}
			};
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof Ensure) {
				ActorRef infoCollector = getContext().actorOf(
					InfoCollector.props(singleton(getSelf())), 
					nameGenerator.getName(EnsureService.class));
				
				Ensure ensure = (Ensure)msg;
				ensure.getMessages().stream()
					.forEach(item -> infoCollector.tell(item, getSelf()));
				
				getContext().watch(infoCollector);
				getContext().become(provisioning(getSender()), false);
			} else {
				log.error("unexpected: {}", msg);
			}
		}
		
	}
	
	ActorRef recorder, geoServerService;
	
	FutureUtils f;
	
	@Before
	public void setUp() {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
			
		ActorSystem actorSystem = ActorSystem.create("test", akkaConfig);
		
		recorder = actorSystem.actorOf(Recorder.props(), "recorder");		
		geoServerService = actorSystem.actorOf(GeoServerServiceMock.props(recorder), "service-mock");
		
		f = new FutureUtils(actorSystem);
	}

	@Test
	public void testEmptyService() throws Exception {
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service0");
		when(service.getName()).thenReturn("serviceName0");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.emptyList());
		
		f.ask(geoServerService, new Ensure(service, new End()), Ack.class).get();		
		
		f.ask(recorder, new Wait(3), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()			
			.assertNext(EnsureWorkspace.class, workspace -> {
				assertEquals("serviceName0", workspace.getWorkspaceId());
			})			
			.assertNext(FinishEnsure.class)
			.assertNext(Terminated.class)
			.assertNotHasNext();
	}
	
	@Test
	public void testSingleLayer() throws Exception {
		Tiling tilingSettings = mock(Tiling.class);
		
		VectorDatasetLayer datasetLayer = mock(VectorDatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer0");
		when(datasetLayer.getTitle()).thenReturn("title0");
		when(datasetLayer.getAbstract()).thenReturn("abstract0");
		when(datasetLayer.isVectorLayer()).thenReturn(true);
		when(datasetLayer.asVectorLayer()).thenReturn(datasetLayer);
		when(datasetLayer.getTableName()).thenReturn("tableName0");		
		when(datasetLayer.getTiling()).thenReturn(Optional.of(tilingSettings));
		when(datasetLayer.getKeywords()).thenReturn(Arrays.asList("keyword0", "keyword1"));
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service0");
		when(service.getName()).thenReturn("serviceName0");
		when(service.getTitle()).thenReturn("serviceTitle0");
		when(service.getAbstract()).thenReturn("serviceAbstract0");
		when(service.getKeywords()).thenReturn(Arrays.asList("keyword0", "keyword1", "keyword2"));
		when(service.getTelephone()).thenReturn("serviceTelephone0");
		
		DatasetLayerRef datasetLayerRef = mock(DatasetLayerRef.class);
		when(datasetLayerRef.isGroupRef()).thenReturn(false);
		when(datasetLayerRef.asDatasetRef()).thenReturn(datasetLayerRef);
		when(datasetLayerRef.getLayer()).thenReturn(datasetLayer);
		when(datasetLayerRef.getStyleRef()).thenReturn(Optional.empty());
		
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(datasetLayerRef));
		
		f.ask(geoServerService, new Ensure(
			service, 
			new Item<>(new Style("style0", TestStyle.getGreenSld())),
			new Item<>(new Style("style1", TestStyle.getGreenSld())),
			new End()), Ack.class).get();
		
		f.ask(recorder, new Wait(6), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(EnsureStyle.class, style -> {
				assertEquals("style0", style.getName());
			})
			.assertNext(EnsureStyle.class, style -> {
				assertEquals("style1", style.getName());
			})
			.assertNext(EnsureWorkspace.class, workspace -> {
				assertEquals("serviceName0", workspace.getWorkspaceId());
				assertEquals("serviceTitle0", workspace.getTitle());
				assertEquals("serviceAbstract0", workspace.getAbstract());
				assertEquals(Arrays.asList("keyword0", "keyword1", "keyword2"), workspace.getKeywords());
				
			})
			.assertNext(EnsureFeatureTypeLayer.class, featureType -> {
				assertEquals("layer0", featureType.getLayerId());
				assertEquals("title0", featureType.getTitle());
				assertEquals("abstract0", featureType.getAbstract());
				assertEquals("tableName0", featureType.getTableName());
				assertTrue(featureType.getTiledLayer().isPresent());
				
				List<String> keywords = featureType.getKeywords();
				assertNotNull(keywords);
				assertTrue(keywords.contains("keyword0"));
				assertTrue(keywords.contains("keyword1"));
			})
			.assertNext(FinishEnsure.class)
			.assertNext(Terminated.class)
			.assertNotHasNext();		
	}
	
	@Test
	public void testGroup() throws Exception {
		final int numberOfLayers = 10;
		
		List<LayerRef<?>> layers = new ArrayList<>();
		for(int i = 0; i < numberOfLayers; i++) {
			VectorDatasetLayer layer = mock(VectorDatasetLayer.class);			
			when(layer.getName()).thenReturn("layer" + i);
			when(layer.isVectorLayer()).thenReturn(true);
			when(layer.asVectorLayer()).thenReturn(layer);
			when(layer.getTableName()).thenReturn("tableName" + i);
			when(layer.getTiling()).thenReturn(Optional.empty());
			
			DatasetLayerRef layerRef = mock(DatasetLayerRef.class);
			when(layerRef.isGroupRef()).thenReturn(false);
			when(layerRef.asDatasetRef()).thenReturn(layerRef);
			when(layerRef.getLayer()).thenReturn(layer);
			when(layerRef.getStyleRef()).thenReturn(Optional.empty());
			
			layers.add(layerRef);
		}
		
		GroupLayer groupLayer = mock(GroupLayer.class);		
		when(groupLayer.getName()).thenReturn("group0");
		when(groupLayer.getTitle()).thenReturn("groupTitle0");
		when(groupLayer.getAbstract()).thenReturn("groupAbstract0");
		when(groupLayer.getLayers()).thenReturn(layers);
		when(groupLayer.getTiling()).thenReturn(Optional.empty());
		
		GroupLayerRef groupLayerRef = mock(GroupLayerRef.class);
		when(groupLayerRef.isGroupRef()).thenReturn(true);
		when(groupLayerRef.asGroupRef()).thenReturn(groupLayerRef);
		when(groupLayerRef.getLayer()).thenReturn(groupLayer);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service0");
		when(service.getName()).thenReturn("serviceName0");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(groupLayerRef));
		
		f.ask(geoServerService, new Ensure(service, new End()), Ack.class).get();
		f.ask(recorder, new Wait(5 + numberOfLayers), Waited.class).get();
		
		Recording recording = f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(EnsureWorkspace.class, workspace -> {
				assertEquals("serviceName0", workspace.getWorkspaceId());
			})
			.assertNext(EnsureGroupLayer.class, group -> {
				assertEquals("group0", group.getLayerId());
				assertEquals("groupTitle0", group.getTitle());
				assertEquals("groupAbstract0", group.getAbstract());
			});
		
		for(int i = 0; i < numberOfLayers; i++) {
			String featureTypeId = "layer" + i;
			String tableName = "tableName" + i;
			recording.assertNext(EnsureFeatureTypeLayer.class, featureType -> {
				assertEquals(featureTypeId, featureType.getLayerId());
				assertEquals(tableName, featureType.getTableName());
			});
		}
			
		recording
			.assertNext(FinishEnsure.class)
			.assertNext(FinishEnsure.class)
			.assertNext(Terminated.class)
			.assertNotHasNext();
	}
	
	@Test
	public void testEmptyGroup () throws Throwable {
		GroupLayer groupLayer = mock(GroupLayer.class);		
		when(groupLayer.getName()).thenReturn("group0");
		when(groupLayer.getTitle()).thenReturn("groupTitle0");
		when(groupLayer.getAbstract()).thenReturn("groupAbstract0");
		when(groupLayer.getLayers()).thenReturn(Collections.<LayerRef<?>>emptyList ());
		when(groupLayer.getTiling()).thenReturn(Optional.empty());
		
		GroupLayerRef groupLayerRef = mock(GroupLayerRef.class);
		when(groupLayerRef.isGroupRef()).thenReturn(true);
		when(groupLayerRef.asGroupRef()).thenReturn(groupLayerRef);
		when(groupLayerRef.getLayer()).thenReturn(groupLayer);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service0");
		when(service.getName()).thenReturn("serviceName0");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(groupLayerRef));
		
		f.ask(geoServerService, new Ensure(service, new End()), Ack.class).get();
		f.ask(recorder, new Wait(3), Waited.class).get();
		
		Recording recording = f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(EnsureWorkspace.class, workspace -> {
				assertEquals("serviceName0", workspace.getWorkspaceId());
			});
		
		recording
			.assertNext(FinishEnsure.class)
			.assertNext(Terminated.class)
			.assertNotHasNext();
	}
	
	@Test
	public void testDuplicateLayer() throws Exception {
		final int numberOfDuplicates = 10;
		
		VectorDatasetLayer datasetLayer = mock(VectorDatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer0");
		when(datasetLayer.getTitle()).thenReturn("title0");
		when(datasetLayer.getAbstract()).thenReturn("abstract0");
		when(datasetLayer.isVectorLayer()).thenReturn(true);
		when(datasetLayer.asVectorLayer()).thenReturn(datasetLayer);
		when(datasetLayer.getTableName()).thenReturn("tableName0");		
		when(datasetLayer.getTiling()).thenReturn(Optional.empty());
		when(datasetLayer.getKeywords()).thenReturn(Arrays.asList("keyword0", "keyword1"));
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service0");
		when(service.getName()).thenReturn("serviceName0");
		when(service.getTitle()).thenReturn("serviceTitle0");
		when(service.getAbstract()).thenReturn("serviceAbstract0");
		when(service.getKeywords()).thenReturn(Arrays.asList("keyword0", "keyword1", "keyword2"));
		when(service.getTelephone()).thenReturn("serviceTelephone0");
		
		List<LayerRef<?>> layers = new ArrayList<>();
		
		for(int i = 0; i < numberOfDuplicates; i++) {
			DatasetLayerRef datasetLayerRef = mock(DatasetLayerRef.class);
			when(datasetLayerRef.isGroupRef()).thenReturn(false);
			when(datasetLayerRef.asDatasetRef()).thenReturn(datasetLayerRef);
			when(datasetLayerRef.getLayer()).thenReturn(datasetLayer);
			when(datasetLayerRef.getStyleRef()).thenReturn(Optional.empty());
			
			layers.add(datasetLayerRef);
		}
		
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(layers);
		
		f.ask(geoServerService, new Ensure(
			service,
			new End()), Ack.class).get();
		
		f.ask(recorder, new Wait(3 + numberOfDuplicates), Waited.class).get();
		
		Recording recording = f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(EnsureWorkspace.class, workspace -> {
				assertEquals("serviceName0", workspace.getWorkspaceId());
			});
		
		for(int i = 0; i < numberOfDuplicates; i++) {
			String layerId = "layer0" + (i != 0 ? "-" + (i + 1) : "");			
			
			recording.assertNext(EnsureFeatureTypeLayer.class, featureType -> {
				assertEquals(layerId, featureType.getLayerId());
				assertEquals("tableName0", featureType.getTableName());
			});
		}
		
		recording.assertNext(FinishEnsure.class)
			.assertNext(Terminated.class)
			.assertNotHasNext();
	}
}
