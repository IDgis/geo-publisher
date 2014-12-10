package nl.idgis.publisher.harvester.sources.mock;

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.harvester.sources.mock.messages.PutDatasetInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.recorder.messages.RecordedMessage;
import nl.idgis.publisher.stream.ListCursor;
import nl.idgis.publisher.stream.messages.NextItem;

public class ProviderMock extends UntypedActor {
	
	private final List<DatasetInfo> datasetInfos;
	
	private final ActorRef recorder;
	
	public ProviderMock(ActorRef recorder) {
		this.recorder = recorder;
		
		datasetInfos = new ArrayList<>();
	}
	
	public static Props props(ActorRef recorder) {
		return Props.create(ProviderMock.class, recorder);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		recorder.tell(new RecordedMessage(getSelf(), getSender(), msg), getSelf());
		
		if(msg instanceof PutDatasetInfo) {
			datasetInfos.add(((PutDatasetInfo)msg).getDatasetInfo());
			getSender().tell(new Ack(), getSelf());
		} if(msg instanceof ListDatasetInfo) {
			ActorRef cursor = getContext().actorOf(ListCursor.props(datasetInfos.iterator()));
			cursor.tell(new NextItem(), getSender());
		} else {		
			unhandled(msg);
		}
	}
	
	public static Props props() {
		return Props.create(ProviderMock.class);
	}

}
