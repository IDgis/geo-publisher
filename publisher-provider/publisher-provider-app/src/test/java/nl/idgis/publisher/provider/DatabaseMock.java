package nl.idgis.publisher.provider;

import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.provider.messages.Record;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
import nl.idgis.publisher.provider.protocol.database.TableNotFound;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class DatabaseMock extends UntypedActor {
	
	private static class TableInfo {
		
		private final TableDescription tableDescription;
		private final long numberOfRecords;
		
		public TableInfo(TableDescription tableDescription, long numberOfRecords) {
			this.tableDescription = tableDescription;
			this.numberOfRecords = numberOfRecords;
		}

		public TableDescription getTableDescription() {
			return tableDescription;
		}

		public long getNumberOfRecords() {
			return numberOfRecords;
		}
	}
	
	private final Map<String, TableInfo> tableInfo;	
	
	private final ActorRef recorder;
	
	public DatabaseMock(ActorRef recorder) {
		tableInfo = new HashMap<>();
		
		this.recorder = recorder;
	}
	
	public static Props props(ActorRef recorder) {
		return Props.create(DatabaseMock.class, recorder);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		recorder.tell(new Record(getSelf(), getSender(), msg), getSelf());
		
		if(msg instanceof DescribeTable) {			
			String tableName = ((DescribeTable)msg).getTableName();
			
			if(tableInfo.containsKey(tableName)) {
				getSender().tell(tableInfo.get(tableName).getTableDescription(), getSelf());
			} else {
				getSender().tell(new TableNotFound(), getSelf());
			}
		} else if(msg instanceof PerformCount) {
			String tableName = ((PerformCount)msg).getTableName();
			
			if(tableInfo.containsKey(tableName)) {
				getSender().tell(tableInfo.get(tableName), getSelf());
			} else {
				getSender().tell(new TableNotFound(), getSelf());
			}
		} else {
			unhandled(msg);
		}
	}
	
}