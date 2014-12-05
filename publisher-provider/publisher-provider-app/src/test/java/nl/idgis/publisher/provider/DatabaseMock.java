package nl.idgis.publisher.provider;

import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.provider.messages.Record;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class DatabaseMock extends UntypedActor {
	
	private final Map<String, TableDescription> tableDescriptions;
	private final Map<String, Long> numberOfRecords;
	
	private final ActorRef recorder;
	
	public DatabaseMock(ActorRef recorder) {
		tableDescriptions = new HashMap<>();
		numberOfRecords = new HashMap<>();
		
		this.recorder = recorder;
	}
	
	public static Props props(ActorRef recorder) {
		return Props.create(DatabaseMock.class, recorder);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		recorder.tell(new Record(getSelf(), getSender(), msg), getSelf());
		
		if(msg instanceof DescribeTable) {
			getSender().tell(tableDescriptions.get(((DescribeTable) msg).getTableName()), getSelf());
		} else if(msg instanceof PerformCount) {
			getSender().tell(numberOfRecords.get(((PerformCount) msg).getTableName()), getSelf());
		} else {
			unhandled(msg);
		}
	}
	
}