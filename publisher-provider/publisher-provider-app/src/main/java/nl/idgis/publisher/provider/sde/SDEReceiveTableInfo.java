package nl.idgis.publisher.provider.sde;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.service.DatabaseLog;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.ProviderUtils;
import nl.idgis.publisher.provider.database.messages.DatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.DatabaseTableInfo;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.database.messages.TableNotFound;
import nl.idgis.publisher.provider.protocol.ColumnInfo;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class SDEReceiveTableInfo extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final ActorRef target;
	
	private final String id;
	
	private final String tableName;
	
	private DatabaseTableInfo databaseTableInfo;
	
	public SDEReceiveTableInfo(ActorRef target, String id, String tableName) {
		this.target = target;
		this.id = id;
		this.tableName = tableName;
	}
	
	public static Props props(ActorRef target, String id, String tableName) {
		return Props.create(SDEReceiveTableInfo.class, target, id ,tableName);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Long) {
			long numberOfRecords = (Long)msg;
			log.debug("count received: {}", numberOfRecords);
			
			ArrayList<ColumnInfo> columns = new ArrayList<>();
			for(DatabaseColumnInfo columnInfo : databaseTableInfo.getColumns()) {
				Type type = columnInfo.getType();
				
				if(type == null) {
					log.debug("unknown data type: " + columnInfo.getTypeName());
				} else {
					columns.add(new ColumnInfo(columnInfo.getName(), type));
				}
			}
			
			TableInfo tableInfo = new TableInfo(columns.toArray(new ColumnInfo[columns.size()]));
			
			log.debug("sending vector dataset info");
			
			target.tell( 
				new VectorDatasetInfo(
					id, 
					"title: " + id, 
					"alternate title: " + id, 
					ProviderUtils.getCategoryId(tableName),
					new Date(),
					Collections.emptySet(),
					Collections.emptySet(),
					false,
					tableName,
					tableInfo,
					numberOfRecords),
				getSelf());
			
			getContext().stop(getSelf());
		} else if(msg instanceof DatabaseTableInfo) {
			databaseTableInfo = (DatabaseTableInfo)msg;
			log.debug("table info received: {}", databaseTableInfo);
			
			getSender().tell(new PerformCount(tableName), getSelf());
		} else if(msg instanceof TableNotFound) {
			log.debug("table not found -> sending unavailable dataset info");
			
			Set<Log> logs = new HashSet<>();
			logs.add(Log.create(LogLevel.ERROR, DatasetLogType.TABLE_NOT_FOUND, new DatabaseLog(tableName)));
			
			target.tell(
				new UnavailableDatasetInfo(id, 
						"title: " + id, 
						"alternate title: " + id, 
						ProviderUtils.getCategoryId(tableName),
						new Date(),
						Collections.emptySet(),
						logs,
						false /* confidential */),
				getSelf());
			
			getContext().stop(getSelf());
		}
	}
}
