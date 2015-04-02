package nl.idgis.publisher.provider;

import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.service.DatabaseLog;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.database.messages.TableNotFound;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;

import akka.actor.ActorRef;
import akka.actor.Props;

public class VectorDatasetInfoBuilder extends AbstractDatasetInfoBuilder {
	
	private final ActorRef database;
	
	private String tableName, reportedTitle;
	
	private TableInfo tableInfo;
	
	private Long numberOfRecords;

	public VectorDatasetInfoBuilder(ActorRef sender, ActorRef converter, ActorRef database, Set<AttachmentType> requestedAttachmentTypes) {			
		super(sender, converter, requestedAttachmentTypes);
		
		this.database = database;
	}
	
	public static DatasetInfoBuilderPropsFactory props(ActorRef database) {
		return (sender, converter, requestedAttachmentTypes) ->		
			Props.create(VectorDatasetInfoBuilder.class, sender, converter, database, requestedAttachmentTypes);
	}
	
	protected void onReceiveElse(Object msg) throws Exception {
		if(msg instanceof TableNotFound) {
			log.debug("table not found");
			
			logs.add(Log.create(LogLevel.ERROR, DatasetLogType.TABLE_NOT_FOUND, new DatabaseLog(tableName)));
			sendUnavailable();
		} else if(msg instanceof TableInfo) {
			log.debug("table info");
			
			tableInfo = (TableInfo)msg;
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
		
		categoryId = ProviderUtils.getCategoryId(tableName);
		
		if(title == null) {
			reportedTitle = alternateTitle;
		} else {
			reportedTitle = title;
		}
		
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
			tellTarget(new VectorDatasetInfo(identification, reportedTitle, alternateTitle, categoryId, revisionDate, attachments, logs, tableName, tableInfo, numberOfRecords));
		}
	}
}
