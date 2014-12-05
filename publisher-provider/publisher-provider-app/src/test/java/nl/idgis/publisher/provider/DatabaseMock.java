package nl.idgis.publisher.provider;

import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;

import akka.actor.Props;
import akka.actor.UntypedActor;

public class DatabaseMock extends UntypedActor {
	
	private final Map<String, TableDescription> tableDescriptions;
	private final Map<String, Long> numberOfRecords;
	
	public DatabaseMock() {
		tableDescriptions = new HashMap<>();
		numberOfRecords = new HashMap<>();
	}
	
	public static Props props() {
		return Props.create(DatabaseMock.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof DescribeTable) {
			getSender().tell(tableDescriptions.get(((DescribeTable) msg).getTableName()), getSelf());
		} else if(msg instanceof PerformCount) {
			getSender().tell(numberOfRecords.get(((PerformCount) msg).getTableName()), getSelf());
		} else {
			unhandled(msg);
		}
	}
	
}