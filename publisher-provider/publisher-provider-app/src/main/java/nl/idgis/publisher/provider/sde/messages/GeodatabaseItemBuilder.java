package nl.idgis.publisher.provider.sde.messages;

import java.util.Iterator;
import java.util.List;

import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class GeodatabaseItemBuilder extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef transaction, sender;
	
	public GeodatabaseItemBuilder(ActorRef transaction, ActorRef sender) {
		this.transaction = transaction;
		this.sender = sender;
	}
	
	public static Props props(ActorRef transaction, ActorRef sender) {
		return Props.create(GeodatabaseItemBuilder.class, transaction, sender);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Records) {
			handleRecords((Records)msg);
		} else if(msg instanceof End) {
			sender.tell(msg, getSelf());
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
		
	}

	private void handleRecords(Records msg) {
		// a single record is expected
		Iterator<Record> itr = msg.getRecords().iterator();
		if(itr.hasNext()) {
			Record record = itr.next();
			log.debug("record received");
			if(itr.hasNext()) {
				log.error("more than one records received");
				getContext().stop(getSelf());
			} else {
				List<Object> values = record.getValues();
				int valueCount = values.size();
				if(valueCount == 2) {
					Iterator<String> valuesItr = values.stream()
						.map(Object::toString)
						.iterator();
					
					sender.tell(
						new GeodatabaseItem(
							valuesItr.next(),
							valuesItr.next(),
							//valuesItr.next(),
							//valuesItr.next()),
							null,
							null),
						getSelf());
					
					getSender().tell(new NextItem(), getSelf());
					
					log.debug("record handled");
				} else {
					log.error("unexpected value count: " + valueCount);
				}
			}
		} else {
			log.error("no record found");
		}
	}

}
