package nl.idgis.publisher.provider;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import nl.idgis.publisher.provider.protocol.AttachmentType;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class DatasetInfoConverterTest {
	
	ActorRef datasetInfoConverter, target, database, metadata;
	
	public static class DatabaseMock extends UntypedActor {

		@Override
		public void onReceive(Object msg) throws Exception {
			unhandled(msg);
		}
		
	}
	
	public static class TargetMock extends UntypedActor {

		@Override
		public void onReceive(Object msg) throws Exception {
			unhandled(msg);
		}
		
	}
	
	public static class MetadataMock extends UntypedActor {

		@Override
		public void onReceive(Object msg) throws Exception {
			unhandled(msg);
		}
		
	}

	@Before
	public void actors() {
		ActorSystem actorSystem = ActorSystem.apply();
		
		database = actorSystem.actorOf(Props.create(DatabaseMock.class));		
		target = actorSystem.actorOf(Props.create(TargetMock.class));
		metadata = actorSystem.actorOf(Props.create(TargetMock.class));
		datasetInfoConverter = actorSystem.actorOf(DatasetInfoConverter.props(Collections.singleton(AttachmentType.METADATA), database, target));
	}
	
	@Test
	public void testDatasetInfoConverter() {
		
	}
	
}
