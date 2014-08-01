package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetColumn.sourceDatasetColumn;
import static nl.idgis.publisher.database.QVersion.version;
import static nl.idgis.publisher.database.QHarvestLog.harvestLog;

import java.sql.Timestamp;
import java.util.List;

import nl.idgis.publisher.database.messages.AlreadyRegistered;
import nl.idgis.publisher.database.messages.GetCategoryInfo;
import nl.idgis.publisher.database.messages.GetCategoryListInfo;
import nl.idgis.publisher.database.messages.GetDataSourceInfo;
import nl.idgis.publisher.database.messages.GetDatasetInfo;
import nl.idgis.publisher.database.messages.GetDatasetListInfo;
import nl.idgis.publisher.database.messages.GetHarvestLog;
import nl.idgis.publisher.database.messages.GetNextHarvestJob;
import nl.idgis.publisher.database.messages.GetSourceDatasetListInfo;
import nl.idgis.publisher.database.messages.GetVersion;
import nl.idgis.publisher.database.messages.HarvestJob;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.GetSourceDatasetColumns;
import nl.idgis.publisher.database.messages.ListQuery;
import nl.idgis.publisher.database.messages.NoJob;
import nl.idgis.publisher.database.messages.QCategoryInfo;
import nl.idgis.publisher.database.messages.QDataSourceInfo;
import nl.idgis.publisher.database.messages.QDatasetInfo;
import nl.idgis.publisher.database.messages.QSourceDatasetInfo;
import nl.idgis.publisher.database.messages.QStoredHarvestLogLine;
import nl.idgis.publisher.database.messages.QVersion;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.Registered;
import nl.idgis.publisher.database.messages.SourceDatasetInfo;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.database.projections.QColumn;
import nl.idgis.publisher.domain.log.Events;
import nl.idgis.publisher.domain.log.GenericEvent;
import nl.idgis.publisher.domain.log.HarvestLogLine;
import nl.idgis.publisher.domain.log.LogLine;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.Order;
import com.mysema.query.types.expr.ComparableExpressionBase;
import com.mysema.query.types.expr.DateTimeExpression;
import com.typesafe.config.Config;

public class PublisherDatabase extends QueryDSLDatabase {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public PublisherDatabase(Config config) {
		super(config);		
	}
	
	public static Props props(Config config) {
		return Props.create(PublisherDatabase.class, config);
	}
	
	private void insertSourceDatasetColumns(QueryDSLContext context, int sourceDatasetId, List<Column> columns) {
		int i = 0;
		for(Column column : columns) {			
			context.insert(sourceDatasetColumn)
				.set(sourceDatasetColumn.sourceDatasetId, sourceDatasetId)
				.set(sourceDatasetColumn.index, i++)
				.set(sourceDatasetColumn.name, column.getName())
				.set(sourceDatasetColumn.dataType, column.getDataType())
				.execute();
		}
	}
	
	private int getCategoryId(QueryDSLContext context, String identification) {
		Integer id = context.query().from(category)
			.where(category.identification.eq(identification))
			.singleResult(category.id);
		
		if(id == null) {
			context.insert(category)
				.set(category.identification, identification)
				.set(category.name, identification)
				.execute();
			
			return getCategoryId(context, identification);
		} else {
			return id;
		}
	}
	
	private <T extends Comparable<? super T>> SQLQuery applyListParams(SQLQuery query, ListQuery listQuery, ComparableExpressionBase<T> orderBy) {
		Order order = listQuery.getOrder();
		Long limit = listQuery.getLimit();
		Long offset = listQuery.getOffset();
		
		if(order != null) {
			if(order == Order.ASC) {
				query = query.orderBy(orderBy.asc());
			} else {
				query = query.orderBy(orderBy.desc());
			}
		}
		
		if(limit != null) {
			query = query.limit(limit);
		}
		
		if(offset != null) {
			query = query.offset(offset);
		}
		
		return query;
	}
	
	@Override
	protected void executeQuery(QueryDSLContext context, Query query) throws Exception {
		if(query instanceof GetVersion) {
			log.debug("database version requested");
			
			context.answer(
				context.query().from(version)
					.orderBy(version.id.desc())
					.limit(1)
					.singleResult(new QVersion(version.id, version.createTime)));
		} else if(query instanceof RegisterSourceDataset) {
			log.debug("registering source dataset: " + query);
			
			RegisterSourceDataset rsd = (RegisterSourceDataset)query;
			Dataset dataset = rsd.getDataset();
			Table table = dataset.getTable();
			
			Tuple existing = 
				context.query().from(sourceDataset)
					.join(dataSource)
						.on(dataSource.id.eq(sourceDataset.dataSourceId))
					.join(category)
						.on(category.id.eq(sourceDataset.categoryId))
					.where(sourceDataset.identification.eq(dataset.getId())
						.and(dataSource.identification.eq(rsd.getDataSource())))
					.singleResult(sourceDataset.id, sourceDataset.name, category.identification, sourceDataset.deleteTime);
			
			if(existing != null) {
				Integer id = existing.get(sourceDataset.id);
				String existingName = existing.get(sourceDataset.name);
				String existingCategoryId = existing.get(category.identification);
				Timestamp existingDeleteTime = existing.get(sourceDataset.deleteTime);
				
				List<Column> existingColumns = context.query().from(sourceDatasetColumn)
					.where(sourceDatasetColumn.sourceDatasetId.eq(id))
					.orderBy(sourceDatasetColumn.index.asc())
					.list(new QColumn(sourceDatasetColumn.name, sourceDatasetColumn.dataType));
				
				if(existingName.equals(table.getName())
						&& existingCategoryId.equals(dataset.getCategoryId())
						&& existingDeleteTime == null
						&& existingColumns.equals(table.getColumns())) {
					context.answer(new AlreadyRegistered());
					log.debug("dataset already registered");
				} else {
					context.update(sourceDataset)
						.set(sourceDataset.name, table.getName())
						.set(sourceDataset.categoryId, getCategoryId(context, dataset.getCategoryId()))
						.setNull(sourceDataset.deleteTime)						
						.set(sourceDataset.updateTime, DateTimeExpression.currentTimestamp(Timestamp.class))
						.where(sourceDataset.id.eq(id))
						.execute();
					
					context.delete(sourceDatasetColumn)
						.where(sourceDatasetColumn.sourceDatasetId.eq(id))
						.execute();
					
					insertSourceDatasetColumns(context, id, table.getColumns());
					context.answer(new Registered());
					
					log.debug("dataset updated");
				}
			} else {
				Integer dataSourceId = context.query().from(dataSource)
					.where(dataSource.identification.eq(rsd.getDataSource()))
					.singleResult(dataSource.id);
				
				if(dataSourceId == null) {
					log.error("dataSource not found: " + dataSourceId);
				} else {
					context.insert(sourceDataset)
						.set(sourceDataset.dataSourceId, dataSourceId)
						.set(sourceDataset.identification, dataset.getId())
						.set(sourceDataset.name, table.getName())
						.set(sourceDataset.categoryId, getCategoryId(context, dataset.getCategoryId()))
						.execute();
					
					Integer id = context.query().from(sourceDataset)
						.where(sourceDataset.dataSourceId.eq(dataSourceId)
							.and(sourceDataset.identification.eq(dataset.getId())))
						.singleResult(sourceDataset.id);
					
					insertSourceDatasetColumns(context, id, table.getColumns());					
					context.answer(new Registered());
					
					log.debug("dataSource inserted");
				}
			}
		} else if(query instanceof GetCategoryListInfo) {
			context.answer(
					context.query().from(category)
					.orderBy(category.identification.asc())
					.list(new QCategoryInfo(category.identification,category.name)));
		} else if(query instanceof GetCategoryInfo) {
			context.answer(
					context.query().from(category)
					.where(category.identification.eq( ((GetCategoryInfo)query).getId() ))
					.singleResult(new QCategoryInfo(category.identification,category.name)));
			
		} else if(query instanceof GetDatasetListInfo) {
			GetDatasetListInfo dli = (GetDatasetListInfo)query;
			String categoryId = dli.getCategoryId();
			
			SQLQuery baseQuery = context.query().from(dataset)
				.join (sourceDataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
				.leftJoin (category).on(sourceDataset.categoryId.eq(category.id));
			
			if(categoryId != null) {
				baseQuery.where(category.identification.eq(categoryId));
			}
			
			context.answer(
					baseQuery
					.orderBy(dataset.identification.asc())
					.list(new QDatasetInfo(dataset.identification, dataset.identification, 
							sourceDataset.identification, sourceDataset.name,
							category.identification,category.name))
			);
		} else if(query instanceof GetDatasetInfo) {
			context.answer(
					context.query().from(dataset)
					.join (sourceDataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
					.leftJoin (category).on(sourceDataset.categoryId.eq(category.id))
					.where(dataset.identification.eq( ((GetDatasetInfo)query).getId() ))
					.singleResult(new QDatasetInfo(dataset.identification, dataset.identification, 
							sourceDataset.identification, sourceDataset.name,
							category.identification,category.name)));
			
		} else if(query instanceof GetDataSourceInfo) {
			context.answer(
				context.query().from(dataSource)
					.orderBy(dataSource.identification.asc())
					.list(new QDataSourceInfo(dataSource.identification, dataSource.name)));
		} else if(query instanceof GetSourceDatasetListInfo) {
			GetSourceDatasetListInfo sdi = (GetSourceDatasetListInfo)query;
			log.debug(sdi.toString());
			
			String categoryId = sdi.getCategoryId();
			String dataSourceId = sdi.getDataSourceId();
			
			SQLQuery baseQuery = context.query().from(sourceDataset)
					.join (dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
					.join (category).on(sourceDataset.categoryId.eq(category.id));
			
			if(categoryId != null) {				
				baseQuery.where(category.identification.eq(categoryId));
			}
			
			if(dataSourceId != null) {				
				baseQuery.where(dataSource.identification.eq(dataSourceId));
			}
			
			SQLQuery listQuery = baseQuery.clone()					
					.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id));
			
			applyListParams(listQuery, sdi, sourceDataset.name);
			
			context.answer(
				new InfoList<SourceDatasetInfo>(			
					listQuery					
						.groupBy(sourceDataset.identification).groupBy(sourceDataset.name)
						.groupBy(dataSource.identification).groupBy(dataSource.name)
						.groupBy(category.identification).groupBy(category.name)						
						.list(new QSourceDatasetInfo(sourceDataset.identification, sourceDataset.name, 
								dataSource.identification, dataSource.name,
								category.identification,category.name,
								dataset.count())),
								
					baseQuery.count()
				)
			);
			
		} else if (query instanceof StoreLog) {
			log.debug("storing log line: " + query);
			
			LogLine logLine = ((StoreLog) query).getLogLine();
			
			if(logLine instanceof HarvestLogLine) {
				String dataSourceId = ((HarvestLogLine) logLine).getDataSourceId();
				
				if(context.insert(harvestLog)
					.columns(harvestLog.datasourceId, harvestLog.event)
					.select(new SQLSubQuery().from(dataSource)							
							.where(dataSource.identification.eq(dataSourceId))
							.list(dataSource.id, Expressions.constant(Events.toString(logLine.getEvent()))))					
					.execute() == 0) {
					log.error("couldn't store log line");
				} else {
					log.debug("log line stored");
					context.ack();
				}
			} else {
				log.error("unknown log line type");
			}
		} else if(query instanceof GetHarvestLog) {
			GetHarvestLog ghl = (GetHarvestLog)query;
			
			String dataSourceId = ghl.getDataSourceId();
			
			
			SQLQuery baseQuery = context.query().from(harvestLog)
				.join(dataSource)
					.on(dataSource.id.eq(harvestLog.datasourceId));
			
			if(dataSourceId != null) {
				baseQuery = baseQuery.where(dataSource.identification.eq(dataSourceId));
			}
			
			baseQuery = applyListParams(baseQuery, ghl, harvestLog.createTime);
				
			context.answer(
					baseQuery.list(new QStoredHarvestLogLine(
						harvestLog.event,
						dataSource.identification, 
						harvestLog.createTime)));
		} else if (query instanceof GetNextHarvestJob){
			QHarvestLog harvestLogSub = new QHarvestLog("subHarvestLog");
			
			String dataSourceName = 
				context.query().from(harvestLog)
					.join(dataSource)
						.on(dataSource.id.eq(harvestLog.datasourceId))
					.orderBy(harvestLog.createTime.asc())
					.where(
						harvestLog.event.eq(Events.toString(GenericEvent.REQUESTED))
						.and(new SQLSubQuery().from(harvestLogSub)
								.where(
									harvestLogSub.datasourceId.eq(harvestLog.datasourceId)
									.and(harvestLogSub.createTime.after(harvestLog.createTime))
									.and(harvestLogSub.event.eq(Events.toString(GenericEvent.STARTED))))										
								.notExists()))
					.limit(1)
					.singleResult(dataSource.identification);
			
			if(dataSourceName == null) {
				context.answer(new NoJob());
			} else {
				context.answer(new HarvestJob(dataSourceName)); 
			}
		} else if(query instanceof GetSourceDatasetColumns) {
			GetSourceDatasetColumns di = (GetSourceDatasetColumns)query;
			
			context.answer(
				context.query().from(sourceDatasetColumn)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetColumn.sourceDatasetId))
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(sourceDataset.identification.eq(di.getSourceDatasetId())
					.and(dataSource.identification.eq(di.getDataSourceId())))
				.list(new QColumn(sourceDatasetColumn.name, sourceDatasetColumn.dataType)));
		} else {
			throw new IllegalArgumentException("Unknown query");
		}
	}
}
