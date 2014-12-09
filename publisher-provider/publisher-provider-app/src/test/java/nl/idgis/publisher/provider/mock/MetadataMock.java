package nl.idgis.publisher.provider.mock;

import java.util.Map;
import java.util.TreeMap;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.mock.messages.PutMetadata;
import nl.idgis.publisher.provider.protocol.metadata.GetAllMetadata;
import nl.idgis.publisher.provider.protocol.metadata.GetMetadata;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.provider.protocol.metadata.MetadataNotFound;
import nl.idgis.publisher.recorder.messages.Record;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class MetadataMock extends UntypedActor {
	
	private final Map<String, byte[]> metadataDocuments;
	
	private final ActorRef recorder;
	
	private ActorRef metadataListProvider;
	
	public MetadataMock(ActorRef recorder) {
		metadataDocuments = new TreeMap<>();
		
		this.recorder = recorder;
	}
	
	public static Props props(ActorRef recorder) {
		return Props.create(MetadataMock.class, recorder);
	}
	
	@Override
	public void preStart() throws Exception {
		metadataListProvider = getContext().actorOf(MetadataListProviderMock.props(metadataDocuments));
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		recorder.tell(new Record(getSelf(), getSender(), msg), getSelf());
		
		if(msg instanceof PutMetadata) {
			PutMetadata putMetadata = (PutMetadata)msg;
			metadataDocuments.put(putMetadata.getIdentification(), putMetadata.getContent());
			getSender().tell(new Ack(), getSelf());
		} else if(msg instanceof GetAllMetadata) {
			metadataListProvider.forward(msg, getContext());
		} else if(msg instanceof GetMetadata) {
			String identification = ((GetMetadata)msg).getIdentification();
			
			if(metadataDocuments.containsKey(identification)) {
				getSender().tell(new MetadataItem(identification, metadataDocuments.get(identification)), getSelf());
			} else {
				getSender().tell(new MetadataNotFound(identification), getSelf());
			}
		} else {
			unhandled(msg);
		}
	}
	
}