package nl.idgis.publisher.loader;

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
import nl.idgis.publisher.loader.messages.GetRequestedColumns;
import nl.idgis.publisher.loader.messages.SetRecordsResponse;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;

class DataSourceMock extends UntypedActor { 
	
	final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	public static class FetchSession extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		final List<Records> response;
		
		FetchSession(List<Records> response) {
			this.response = response;
		}
		
		static Props props(List<Records> response) {
			return Props.create(FetchSession.class, response);
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("received: " + msg);
			
			if(msg instanceof Ack) {
				getContext().become(sendingRecords(response));
			} else {
				unhandled(msg);
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
						getContext().stop(getSelf());
					}
				}
				
				@Override
				public void apply(Object msg) throws Exception {
					log.debug("received: " + msg);
					
					if(msg instanceof NextItem) {
						sendResponse();
					} else {
						unhandled(msg);
					}
				}
			};
		}
		
	}
	
	ActorRef sender = null;
	List<String> requestedColumns = null;
	List<Records> response = null;
	
	static Props props() {
		return Props.create(DataSourceMock.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("received: " + msg);
		
		if(msg instanceof FetchVectorDataset) {
			FetchVectorDataset gd = (FetchVectorDataset)msg;
			
			requestedColumns = gd.getColumns();
			
			ActorRef session = getContext().actorOf(FetchSession.props(response));
			
			ActorRef receiver = getContext().actorOf(gd.getReceiverProps(), "receiver");
			receiver.tell(new StartVectorImport(getSender(), 10), session);
		} else {
			onReceiveElse(msg);
		}
	}
	
	private void onReceiveElse(Object msg) {
		if(msg instanceof GetRequestedColumns) {
			sender = getSender();
			sendColumns();
		} else if(msg instanceof SetRecordsResponse) {
			response = ((SetRecordsResponse)msg).getResponse();
			getSender().tell(new Ack(), getSelf());
		} else {
			unhandled(msg);
		}
	}
	
	private void sendColumns() {
		if(sender != null && requestedColumns != null) {
			sender.tell(requestedColumns, getSelf());
			
			sender = null;
			requestedColumns = null;
		}
	}	
}