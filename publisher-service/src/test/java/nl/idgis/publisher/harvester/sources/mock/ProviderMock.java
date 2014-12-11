package nl.idgis.publisher.harvester.sources.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.harvester.sources.mock.messages.PutDataset;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.GetDatasetInfo;
import nl.idgis.publisher.provider.protocol.GetVectorDataset;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.recorder.messages.RecordedMessage;
import nl.idgis.publisher.stream.ListCursor;
import nl.idgis.publisher.stream.messages.NextItem;

public class ProviderMock extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Map<String, Dataset> datasets;
	
	private final ActorRef recorder;
	
	public ProviderMock(ActorRef recorder) {
		this.recorder = recorder;
		
		datasets = new HashMap<>();
	}
	
	public static Props props(ActorRef recorder) {
		return Props.create(ProviderMock.class, recorder);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		recorder.tell(new RecordedMessage(getSelf(), getSender(), msg), getSelf());
		
		if(msg instanceof PutDataset) {
			log.debug("put dataset");
			
			PutDataset putDataset = ((PutDataset)msg);
			Dataset dataset = new Dataset(putDataset.getDatasetInfo(), putDataset.getRecords());			
			
			datasets.put(dataset.getDatasetInfo().getIdentification(), dataset);			
			getSender().tell(new Ack(), getSelf());
		} if(msg instanceof ListDatasetInfo) {
			log.debug("list dataset info");
			
			List<DatasetInfo> datasetInfos = new ArrayList<>();
			for(Dataset dataset : datasets.values()) {
				datasetInfos.add(dataset.getDatasetInfo());
			}
			
			ActorRef cursor = getContext().actorOf(ListCursor.props(datasetInfos.iterator()));
			cursor.tell(new NextItem(), getSender());
		} else if(msg instanceof GetDatasetInfo) {
			log.debug("get dataset info");
			
			String identification = ((GetDatasetInfo)msg).getIdentification();
			
			if(datasets.containsKey(identification)) {
				getSender().tell(datasets.get(identification).getDatasetInfo(), getSelf());
			}
		} else if(msg instanceof GetVectorDataset) {
			log.debug("get vector dataset");
			
			GetVectorDataset getVectorDataset = (GetVectorDataset)msg;			
			
			String identification = getVectorDataset.getIdentification();			
			if(datasets.containsKey(identification)) {
				int messageSize = getVectorDataset.getMessageSize();
				
				List<Record> records = new ArrayList<>();
				List<Records> recordPacks = new ArrayList<>();
				for(Record record : datasets.get(identification).getRecords()) {
					if(records.size() == messageSize) {
						recordPacks.add(new Records(records));
						records = new ArrayList<>();
					}
					
					records.add(record);
				}
				
				if(!records.isEmpty()) {
					recordPacks.add(new Records(records));					
				}
				
				getContext().actorOf(ListCursor.props(recordPacks.iterator())).tell(new NextItem(), getSender());
			}
		} else {
			unhandled(msg);
		}
	}	
	
	public static Props props() {
		return Props.create(ProviderMock.class);
	}

}
