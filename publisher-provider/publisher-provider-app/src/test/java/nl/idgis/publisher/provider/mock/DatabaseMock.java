package nl.idgis.publisher.provider.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.database.messages.TableNotFound;
import nl.idgis.publisher.provider.mock.messages.PutTable;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.recorder.messages.RecordedMessage;
import nl.idgis.publisher.stream.ListCursor;
import nl.idgis.publisher.stream.messages.NextItem;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class DatabaseMock extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private static class Table {
		
		private final TableInfo tableInfo;
		
		private final List<Record> records;
		
		public Table(TableInfo tableInfo, List<Record> records) {
			this.tableInfo = tableInfo;
			this.records = records;
		}

		public TableInfo getTableInfo() {
			return tableInfo;
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
				getSender().tell(tables.get(tableName).getTableInfo(), getSelf());
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
			
			tables.put(putTable.getTableName(), new Table(putTable.getTableInfo(), putTable.getRecords()));
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
				
				ActorRef cursor = getContext().actorOf(ListCursor.props(records.iterator()));
				cursor.tell(new NextItem(), getSender()); 
			} else {
				getSender().tell(new TableNotFound(), getSelf());
			}
		} else {
			unhandled(msg);
		}
	}
	
}