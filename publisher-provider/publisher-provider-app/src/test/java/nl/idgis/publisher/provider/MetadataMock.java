package nl.idgis.publisher.provider;

import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.provider.protocol.metadata.GetAllMetadata;
import nl.idgis.publisher.provider.protocol.metadata.GetMetadata;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class MetadataMock extends UntypedActor {
	
	private final Map<String, byte[]> metadataDocuments;
	
	private ActorRef metadataListProvider;
	
	public MetadataMock() {
		metadataDocuments = new HashMap<>();
	}
	
	public static Props props() {
		return Props.create(MetadataMock.class);
	}
	
	@Override
	public void preStart() throws Exception {
		metadataListProvider = getContext().actorOf(MetadataListProviderMock.props(metadataDocuments));
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetAllMetadata) {
			metadataListProvider.forward(msg, getContext());
		} else if(msg instanceof GetMetadata) {
			String identification = ((GetMetadata)msg).getIdentification();
			
			if(metadataDocuments.containsKey(identification)) {
				getSender().tell(new MetadataItem(identification, metadataDocuments.get(identification)), getSelf());
			}
		} else {
			unhandled(msg);
		}
	}
	
}