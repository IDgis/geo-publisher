package nl.idgis.publisher.service.geoserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.Recorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.RecordedMessage;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.service.geoserver.messages.EnsureLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureGroup;
import nl.idgis.publisher.service.geoserver.messages.EnsureWorkspace;
import nl.idgis.publisher.service.geoserver.messages.Ensured;
import nl.idgis.publisher.service.geoserver.messages.FinishEnsure;
import nl.idgis.publisher.service.manager.messages.DatasetLayer;
import nl.idgis.publisher.service.manager.messages.GroupLayer;
import nl.idgis.publisher.service.manager.messages.Layer;
import nl.idgis.publisher.service.manager.messages.Service;
import nl.idgis.publisher.utils.SyncAskHelper;
import nl.idgis.publisher.utils.UniqueNameGenerator;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProvisionServiceTest {
	
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
					
					if(msg instanceof EnsureGroup) {
						ensured();
						getContext().become(layers(initiator, depth + 1), false);
					} else if(msg instanceof EnsureLayer) {
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
					if(msg instanceof EnsureWorkspace) {
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
			if(msg instanceof Service) {
				ActorRef provisionService = getContext().actorOf(ProvisionService.props(), nameGenerator.getName(ProvisionService.class));
				provisionService.forward(msg, getContext());
				getContext().watch(provisionService);
				getContext().become(provisioning(getSender()), false);				
			} else {
				log.error("unexpected: {}", msg);
			}
		}
		
	}
	
	ActorRef recorder, geoServerService;
	
	SyncAskHelper sync;
	
	@Before
	public void setUp() {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
			
		ActorSystem actorSystem = ActorSystem.create("test", akkaConfig);
		
		recorder = actorSystem.actorOf(Recorder.props(), "recorder");		
		geoServerService = actorSystem.actorOf(GeoServerServiceMock.props(recorder), "service-mock");
		
		sync = new SyncAskHelper(actorSystem);
	}

	@Test
	public void testEmptyService() throws Exception {
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service0");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.emptyList());
		
		sync.ask(geoServerService, service, Ack.class);		
		
		sync.ask(recorder, new Wait(3), Waited.class);
		sync.ask(recorder, new GetRecording(), Recording.class)			
			.assertNext(EnsureWorkspace.class, workspace -> {
				assertEquals("service0", workspace.getWorkspaceId());
			})			
			.assertNext(FinishEnsure.class)
			.assertNext(Terminated.class)
			.assertNotHasNext();
	}
	
	@Test
	public void testSingleLayer() throws Exception {
		DatasetLayer datasetLayer = mock(DatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer0");
		when(datasetLayer.getTableName()).thenReturn("tableName0");
		when(datasetLayer.isGroup()).thenReturn(false);
		when(datasetLayer.asDataset()).thenReturn(datasetLayer);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service0");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(datasetLayer));
		
		sync.ask(geoServerService, service, Ack.class);
		
		sync.ask(recorder, new Wait(4), Waited.class);
		sync.ask(recorder, new GetRecording(), Recording.class)
			.assertNext(EnsureWorkspace.class, workspace -> {
				assertEquals("service0", workspace.getWorkspaceId());
			})
			.assertNext(EnsureLayer.class, featureType -> {
				assertEquals("layer0", featureType.getLayerId());
				assertEquals("tableName0", featureType.getTableName());
			})
			.assertNext(FinishEnsure.class)
			.assertNext(Terminated.class)
			.assertNotHasNext();		
	}
	
	@Test
	public void testGroup() throws Exception {
		final int numberOfLayers = 10;
		
		List<Layer> layers = new ArrayList<>();
		for(int i = 0; i < numberOfLayers; i++) {
			DatasetLayer layer = mock(DatasetLayer.class);
			when(layer.isGroup()).thenReturn(false);
			when(layer.asDataset()).thenReturn(layer);
			when(layer.getName()).thenReturn("layer" + i);
			when(layer.getTableName()).thenReturn("tableName" + i);
			
			layers.add(layer);
		}
		
		GroupLayer groupLayer = mock(GroupLayer.class);
		when(groupLayer.isGroup()).thenReturn(true);
		when(groupLayer.asGroup()).thenReturn(groupLayer);
		when(groupLayer.getName()).thenReturn("group0");
		when(groupLayer.getLayers()).thenReturn(layers);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service0");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(groupLayer));
		
		sync.ask(geoServerService, service, Ack.class);
		sync.ask(recorder, new Wait(5 + numberOfLayers), Waited.class);
		
		Recording recording = sync.ask(recorder, new GetRecording(), Recording.class)
			.assertNext(EnsureWorkspace.class, workspace -> {
				assertEquals("service0", workspace.getWorkspaceId());
			})
			.assertNext(EnsureGroup.class, group -> {
				assertEquals("group0", group.getGroupId());
			});
		
		for(int i = 0; i < numberOfLayers; i++) {
			String featureTypeId = "layer" + i;
			String tableName = "tableName" + i;
			recording.assertNext(EnsureLayer.class, featureType -> {
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
}
