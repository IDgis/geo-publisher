package nl.idgis.publisher.provider;

import java.util.ArrayList;
import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.service.DatabaseLog;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.provider.database.messages.DatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.DatabaseTableInfo;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.database.messages.TableNotFound;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.ColumnInfo;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;

import akka.actor.ActorRef;
import akka.actor.Props;

public class VectorDatasetInfoBuilder extends AbstractDatasetInfoBuilder {
	
	private final ActorRef database;
	
	private String tableName;
	
	private TableInfo tableInfo;
	
	private Long numberOfRecords;

	public VectorDatasetInfoBuilder(ActorRef sender, ActorRef database, Set<AttachmentType> requestedAttachmentTypes) {			
		super(sender, requestedAttachmentTypes);
		
		this.database = database;
	}
	
	public static DatasetInfoBuilderPropsFactory props(ActorRef database) {
		return (sender, requestedAttachmentTypes) ->		
			Props.create(VectorDatasetInfoBuilder.class, sender, database, requestedAttachmentTypes);
	}
	
	protected void onReceiveElse(Object msg) throws Exception {
		if(msg instanceof TableNotFound) {
			log.debug("table not found");
			
			logs.add(Log.create(LogLevel.ERROR, DatasetLogType.TABLE_NOT_FOUND, new DatabaseLog(tableName)));
			sendUnavailable();
		} else if(msg instanceof DatabaseTableInfo) {
			log.debug("table info");
			
			ArrayList<ColumnInfo> columns = new ArrayList<>();
			for(DatabaseColumnInfo columnInfo : ((DatabaseTableInfo)msg).getColumns()) {
				columns.add(new ColumnInfo(columnInfo.getName(), columnInfo.getType()));
			}
			
			tableInfo = new TableInfo(columns.toArray(new ColumnInfo[columns.size()]));
			sendResponse();
		} else if(msg instanceof Long) {
			log.debug("number of records");
			
			numberOfRecords = (Long)msg;
			sendResponse();
		} else {
			unhandled(msg);
		}
	}

	protected void processMetadata() {
		tableName = ProviderUtils.getTableName(alternateTitle);
		log.debug("tableName: {}", tableName);
		
		categoryId = ProviderUtils.getCategoryId(tableName);
		log.debug("categoryId: {}", categoryId);
		
		if(logs.isEmpty()) {		
			if(tableName != null && !tableName.trim().isEmpty()) {
				database.tell(new DescribeTable(tableName), getSelf());
				database.tell(new PerformCount(tableName), getSelf());
			} else {
				logs.add(Log.create(LogLevel.ERROR, DatasetLogType.UNKNOWN_TABLE));					
				sendUnavailable();
			}
		} else {
			sendUnavailable();
		}
	}

	private void sendResponse() {
		if(tableInfo != null && numberOfRecords != null) {
			tellTarget(new VectorDatasetInfo(identification, reportedTitle, alternateTitle, categoryId, revisionDate, attachments, logs, confidential, tableName, tableInfo, numberOfRecords));
		}
	}
}
