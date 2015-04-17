package nl.idgis.publisher.provider;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.database.messages.TableNotFound;
import nl.idgis.publisher.provider.protocol.DatasetNotAvailable;
import nl.idgis.publisher.provider.protocol.GetVectorDataset;
import nl.idgis.publisher.stream.messages.Item;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.Terminated;
import akka.japi.Procedure;

public class VectorDatasetFetcher extends AbstractDatasetFetcher<GetVectorDataset> {
	
	private final ActorRef database;
		
	public VectorDatasetFetcher(ActorRef sender, ActorRef database, GetVectorDataset request) {
		super(sender, request);
		
		this.database = database;
	}
	
	public static Props props(ActorRef sender, ActorRef database, GetVectorDataset request) {
		return Props.create(VectorDatasetFetcher.class, sender, database, request);
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(15, TimeUnit.SECONDS));
	}
	
	private Procedure<Object> waitingForAck() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("transaction committed");
					
					getContext().stop(getSelf());
				} else if(msg instanceof Failure) {
					log.error("failed to commit: {}", msg);
					
					sender.tell(msg, getSelf());
					getContext().stop(getSelf());
				} else if(msg instanceof ReceiveTimeout) {
					log.error("timout while committing");
					
					sender.tell(new Failure(new TimeoutException("while commiting")), getSelf());
					getContext().stop(getSelf());
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private Procedure<Object> fetchingData(ActorRef transaction) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof ReceiveTimeout) {
					log.error("timeout while fetching data");
					
					sender.tell(new Failure(new TimeoutException("while fetching data")), getSelf());
					getContext().stop(getSelf());
				} else if(msg instanceof TableNotFound) {
					log.debug("table not found");
					
					sender.tell(new DatasetNotAvailable(request.getIdentification()), getSelf());
					getContext().stop(getSelf());
				} else if(msg instanceof Item) {
					log.debug("item");
					
					sender.tell(msg, getSender());					
					
					// we assume that the the database is still
					// producing records as long as the cursor actor
					// is still alive.
					getContext().watch(getSender());
					
					// disable the receive timeout as we don't get to see
					// additional records objects as these are send directly
					// to the consumer. 
					getContext().setReceiveTimeout(Duration.Inf());  
				} else if(msg instanceof Terminated) {
					log.debug("cursor terminated");
					
					transaction.tell(new Commit(), getSelf());
					getContext().become(waitingForAck());
				} else {
					unhandled(msg);
				}
			}			
		};
	}
	
	private Procedure<Object> startingTransaction(String tableName) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof ReceiveTimeout) {
					log.error("timeout while starting transaction");
					
					sender.tell(new Failure(new TimeoutException("while starting transaction")), getSelf());
					getContext().stop(getSelf());
				} else if(msg instanceof Failure) {
					log.error("database failure: {}", msg);
					
					sender.tell(msg, getSelf());
					getContext().stop(getSelf());
				} else if(msg instanceof TransactionCreated) {
					log.debug("transaction created");
					
					ActorRef transaction = ((TransactionCreated)msg).getActor();					
					transaction.tell(new FetchTable(tableName, request.getColumnNames(), request.getMessageSize()), getSelf());
					getContext().become(fetchingData(transaction));
				} else {
					unhandled(msg);
				}				
			}
			
		};
	}
	
	protected void handleMetadataDocument(MetadataDocument metadataDocument) throws Exception {
		String tableName = ProviderUtils.getTableName(metadataDocument.getAlternateTitle());
		
		database.tell(new StartTransaction(), getSelf());
		getContext().become(startingTransaction(tableName));
	}
}
