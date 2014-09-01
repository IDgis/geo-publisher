package nl.idgis.publisher.loader;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.database.AbstractDatabaseTest;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.database.Record;
import nl.idgis.publisher.provider.protocol.database.Records;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;

public class LoaderTest extends AbstractDatabaseTest {
	
	static class TransactionMock extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("received: " + msg);
			
			getSender().tell(new Ack(), getSelf());
		}
		
	}
	
	static class GeometryDatabaseMock extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("received: " + msg);
			
			if(msg instanceof StartTransaction) {
				ActorRef transaction = getContext().actorOf(Props.create(TransactionMock.class));				
				getSender().tell(new TransactionCreated(transaction), getSelf());
			} else {
				unhandled(msg);
			}
		}		
	}
	
	static class DataSourceMock extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("received: " + msg);
			
			if(msg instanceof GetDataset) {
				GetDataset gd = (GetDataset)msg;				
				ActorRef receiver = getContext().actorOf(gd.getReceiverProps(), "receiver");
				receiver.tell(new StartImport(10), getSelf());
				getContext().become(waitingForStart());
			} else {
				unhandled(msg);
			}
		}
		
		private Procedure<Object> sendingRecords(Iterable<Records> records) {
			final Iterator<Records> itr = records.iterator();			
			
			return new Procedure<Object>() {
				
				{
					sendResponse();
				}
				
				void sendResponse() {
					if(itr.hasNext()) {
						getSender().tell(itr.next(), getSelf());
					} else {
						getSender().tell(new End(), getSelf());						
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
						unhandled(msg);
					}
				}
				
			};
		}
		
	}
	
	static class HarvesterMock extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		final ActorRef dataSource;
		
		HarvesterMock(ActorRef dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("received: " + msg);
			
			if(msg instanceof GetDataSource) {
				getSender().tell(dataSource, getSelf());
			} else {
				unhandled(msg);
			}
		}		
	}
	
	static class WaitForSucceeded {
		
	}
	
	static class DatabaseAdapter extends UntypedActor {
		
		final ActorRef database;
		
		ActorRef sender = null;
		boolean succeeded = false;
		
		DatabaseAdapter(ActorRef database) {
			this.database = database;
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof WaitForSucceeded) {
				sender = getSender();
				sendWaitResponse();
			} else {			
				if(msg instanceof UpdateJobState) {
					UpdateJobState ujs = (UpdateJobState)msg;
					
					if(ujs.getState() == JobState.SUCCEEDED) {
						succeeded = true;
						sendWaitResponse();
					}
				}
				
				database.tell(msg, getSender());
			}
		}
		
		void sendWaitResponse() {
			if(sender != null && succeeded) {
				sender.tell(new Ack(), getSelf());
			}
		}
		
	}
	
	ActorRef loader;
	ActorRef databaseAdapter;
	
	@Before
	public void actors() {
		ActorRef geometryDatabaseMock = actorOf(Props.create(GeometryDatabaseMock.class), "geometryDatabaseMock");
		ActorRef dataSourceMock = actorOf(Props.create(DataSourceMock.class), "dataSourceMock");
		ActorRef harvesterMock = actorOf(Props.create(HarvesterMock.class, dataSourceMock), "harvesterMock");		
		databaseAdapter = actorOf(Props.create(DatabaseAdapter.class, database), "databaseAdapter");
		
		loader = actorOf(Loader.props(geometryDatabaseMock, databaseAdapter, harvesterMock), "loader");
	}

	@Test
	public void testImportJob() throws Exception {
		insertDataset();
		ask(database, new CreateImportJob("testDataset"));
		
		List<?> list = askAssert(database, new GetImportJobs(), List.class);
		assertEquals(1, list.size());
		for(Object o : list) {
			askAssert(loader, o, Ack.class);
		}
		
		askAssert(databaseAdapter, new WaitForSucceeded(), Ack.class);
	}
}
