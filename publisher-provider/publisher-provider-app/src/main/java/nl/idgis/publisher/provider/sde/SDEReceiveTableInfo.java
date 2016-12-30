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
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.ProviderUtils;
import nl.idgis.publisher.provider.database.messages.DatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.DatabaseTableInfo;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.database.messages.TableNotFound;
import nl.idgis.publisher.provider.protocol.ColumnInfo;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.xml.exceptions.NotFound;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class SDEReceiveTableInfo extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final ActorRef target;
	
	private final SDEItemInfo itemInfo;
	
	private String identification;
	
	private String title;
	
	private String alternateTitle;
	
	private String tableName;
	
	private String categoryId;
	
	private DatabaseTableInfo databaseTableInfo;
	
	public SDEReceiveTableInfo(ActorRef target, SDEItemInfo itemInfo) {
		this.target = target;
		this.itemInfo = itemInfo;
	}
	
	public static Props props(ActorRef target, SDEItemInfo itemInfo) {
		return Props.create(SDEReceiveTableInfo.class, target, itemInfo);
	}
	
	@Override
	public void preStart() {
		identification = itemInfo.getUuid();
		tableName = itemInfo.getPhysicalname();
		categoryId = ProviderUtils.getCategoryId(tableName);
		
		title = tableName;
		
		itemInfo.getDocumentation().ifPresent(documentation -> {
			try {
				MetadataDocumentFactory mdf = new MetadataDocumentFactory();
				MetadataDocument md = mdf.parseDocument(documentation.getBytes("utf-8"));
			
				try {
					title = md.getDatasetTitle();
				} catch(NotFound nf) {}
				try {
					alternateTitle = md.getDatasetAlternateTitle();
				} catch(NotFound nf) {}
			} catch(Exception e) {
				log.error(e, "couldn't process documentation content");
			}
		});
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
					identification, 
					title, 
					alternateTitle,
					categoryId,
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
				new UnavailableDatasetInfo(
					identification,
					title, 
					alternateTitle,
					categoryId,
					new Date(),
					Collections.emptySet(),
					logs,
					false /* confidential */),
				getSelf());
			
			getContext().stop(getSelf());
		}
	}
}
