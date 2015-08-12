package nl.idgis.publisher.metadata;

import java.util.HashMap;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.metadata.messages.AddDataSource;
import nl.idgis.publisher.protocol.messages.Ack;

public class HarvesterMock extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private Map<String, ActorRef> dataSources;
	
	public static Props props() {
		return Props.create(HarvesterMock.class);
	}
	
	@Override
	public void preStart() {
		dataSources = new HashMap<>();
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof AddDataSource) {
			log.debug("dataSource added");
			
			AddDataSource addDataSource = (AddDataSource)msg;
			dataSources.put(addDataSource.getDataSourceId(), addDataSource.getActorRef());
			
			getSender().tell(new Ack(), getSelf());
		} else if(msg instanceof GetDataSource) {
			log.debug("dataSource requested");
			
			String dataSourceId = ((GetDataSource) msg).getDataSourceId();
			if(dataSources.containsKey(dataSourceId)) {
				getSender().tell(dataSources.get(dataSourceId), getSelf());				
			} else {
				getSender().tell(new NotConnected(), getSelf());
			}
		} else {
			unhandled(msg);
		}
	}

}
