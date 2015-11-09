package nl.idgis.publisher.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.harvester.sources.messages.FetchVectorDataset;
import nl.idgis.publisher.harvester.sources.messages.StartVectorImport;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;

class DataSourceMock extends UntypedActor {
	
	final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	ActorRef sender = null;
	List<String> columns = null;
	
	static Props props() {
		return Props.create(DataSourceMock.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("received: " + msg);
		
		if(msg instanceof FetchVectorDataset) {
			FetchVectorDataset gd = (FetchVectorDataset)msg;
			
			columns = gd.getColumns();
			sendColumns();
			
			ActorRef receiver = getContext().actorOf(gd.getReceiverProps(), "receiver");
			receiver.tell(new StartVectorImport(getSender(), 10), getSelf());
			getContext().become(waitingForStart(), true);
		} else {
			onReceiveElse(msg);
		}
	}
	
	private void onReceiveElse(Object msg) {
		if(msg instanceof GetColumns) {
			sender = getSender();
			sendColumns();
		} else {
			unhandled(msg);
		}
	}
	
	private void sendColumns() {
		if(sender != null && columns != null) {
			sender.tell(columns, getSelf());
			
			sender = null;
			columns = null;
		}
	}
	
	private Procedure<Object> sendingRecords(Iterable<Records> records) {
		final Iterator<Records> itr = records.iterator();			
		
		return new Procedure<Object>() {
			
			long seq = 0;
			
			{
				sendResponse();
			}
			
			void sendResponse() {
				if(itr.hasNext()) {
					getSender().tell(new Item<>(seq++, itr.next()), getSelf());
				} else {
					getSender().tell(new End(), getSelf());
					getContext().unbecome();
				}
			}
			
			@Override
			public void apply(Object msg) throws Exception {
				log.debug("received: " + msg);
				
				if(msg instanceof NextItem) {
					sendResponse();
				} else {
					onReceiveElse(msg);
				}
			}
		};
	}

	private Procedure<Object> waitingForStart() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("received: " + msg);
				
				if(msg instanceof Ack) {
					List<Records> records = new ArrayList<>();
					for(int i = 0; i < 2; i++) {
						
						List<Record> currentRecords = new ArrayList<>();
						for(int j = 0; j < 5; j++) {
							int value = i * 5 + j;
							
							currentRecords.add(
								new Record(
									Arrays.<Object>asList(
											"value: " + j, 
											value)));
						}
						
						records.add(new Records(currentRecords));
					}
					
					getContext().become(sendingRecords(records));
				} else {
					onReceiveElse(msg);
				}
			}
			
		};
	}
	
}