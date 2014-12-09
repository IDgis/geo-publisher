package nl.idgis.publisher.provider.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.mock.messages.PutTable;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.FetchTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
import nl.idgis.publisher.provider.protocol.database.TableNotFound;
import nl.idgis.publisher.recorder.messages.RecordedMessage;
import nl.idgis.publisher.stream.messages.NextItem;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class DatabaseMock extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private static class Table {
		
		private final TableDescription tableDescription;
		
		private final List<Record> records;
		
		public Table(TableDescription tableDescription, List<Record> records) {
			this.tableDescription = tableDescription;
			this.records = records;
		}

		public TableDescription getTableDescription() {
			return tableDescription;
		}

		public List<Record> getRecords() {
			return records;
		}
	}
	
	private final Map<String, Table> tables;	
	
	private final ActorRef recorder;
	
	public DatabaseMock(ActorRef recorder) {
		tables = new HashMap<>();
		
		this.recorder = recorder;
	}
	
	public static Props props(ActorRef recorder) {
		return Props.create(DatabaseMock.class, recorder);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		recorder.tell(new RecordedMessage(getSelf(), getSender(), msg), getSelf());
		
		if(msg instanceof DescribeTable) {
			log.debug("describe table");
			
			String tableName = ((DescribeTable)msg).getTableName();
			
			if(tables.containsKey(tableName)) {
				getSender().tell(tables.get(tableName).getTableDescription(), getSelf());
			} else {
				getSender().tell(new TableNotFound(), getSelf());
			}
		} else if(msg instanceof PerformCount) {
			log.debug("perform count");
			
			String tableName = ((PerformCount)msg).getTableName();
			
			if(tables.containsKey(tableName)) {
				getSender().tell((long)tables.get(tableName).getRecords().size(), getSelf());
			} else {
				getSender().tell(new TableNotFound(), getSelf());
			}
		} else if(msg instanceof PutTable) {
			log.debug("put table");
			
			PutTable putTable = (PutTable)msg;
			
			tables.put(putTable.getTableName(), new Table(putTable.getTableDescription(), putTable.getRecords()));
			getSender().tell(new Ack(), getSelf());
		} else if(msg instanceof FetchTable) {
			log.debug("fetch table");
			
			String tableName = ((FetchTable)msg).getTableName();
			
			if(tables.containsKey(tableName)) {				
				List<Records> records = new ArrayList<>();
				
				int messageSize = ((FetchTable) msg).getMessageSize();
				List<Record> currentRecords = new ArrayList<>();
				for(Record record : tables.get(tableName).getRecords()) {
					if(currentRecords.size() == messageSize) {
						records.add(new Records(currentRecords));
						currentRecords = new ArrayList<>();
					}
					
					currentRecords.add(record);
				}
				
				if(!currentRecords.isEmpty()) {
					records.add(new Records(currentRecords));
				}
				
				ActorRef cursor = getContext().actorOf(DatabaseCursorMock.props(records.iterator()));
				cursor.tell(new NextItem(), getSender()); 
			} else {
				getSender().tell(new TableNotFound(), getSelf());
			}
		} else {
			unhandled(msg);
		}
	}
	
}