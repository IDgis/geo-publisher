package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetColumn.sourceDatasetColumn;
import static nl.idgis.publisher.database.QVersion.version;
import static nl.idgis.publisher.database.QCategory.category;

import java.sql.Timestamp;
import java.util.List;

import nl.idgis.publisher.database.messages.GetDataSourceInfo;
import nl.idgis.publisher.database.messages.GetSourceDatasetInfo;
import nl.idgis.publisher.database.messages.GetVersion;
import nl.idgis.publisher.database.messages.QDataSourceInfo;
import nl.idgis.publisher.database.messages.QSourceDatasetInfo;
import nl.idgis.publisher.database.messages.QVersion;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.projections.QColumn;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
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
	
	private int getCatagoryId(QueryDSLContext context, String identification) {
		Integer id = context.query().from(category)
			.where(category.identification.eq(identification))
			.singleResult(category.id);
		
		if(id == null) {
			context.insert(category)
				.set(category.identification, identification)
				.set(category.name, identification)
				.execute();
			
			return getCatagoryId(context, identification);
		} else {
			return id;
		}
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
					log.debug("dataset already registered");
				} else {
					context.update(sourceDataset)
						.set(sourceDataset.name, table.getName())
						.set(sourceDataset.categoryId, getCatagoryId(context, dataset.getCategoryId()))
						.setNull(sourceDataset.deleteTime)						
						.set(sourceDataset.updateTime, DateTimeExpression.currentTimestamp(Timestamp.class))
						.where(sourceDataset.id.eq(id))
						.execute();
					
					context.delete(sourceDatasetColumn)
						.where(sourceDatasetColumn.sourceDatasetId.eq(id))
						.execute();
					
					insertSourceDatasetColumns(context, id, table.getColumns());
					
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
						.set(sourceDataset.categoryId, getCatagoryId(context, dataset.getCategoryId()))
						.execute();
					
					Integer id = context.query().from(sourceDataset)
						.where(sourceDataset.dataSourceId.eq(dataSourceId)
							.and(sourceDataset.identification.eq(dataset.getId())))
						.singleResult(sourceDataset.id);
					
					insertSourceDatasetColumns(context, id, table.getColumns());
				}
			}
		} else if(query instanceof GetDataSourceInfo) {
			context.answer(
				context.query().from(dataSource)
					.orderBy(dataSource.identification.asc())
					.list(new QDataSourceInfo(dataSource.identification, dataSource.name)));
		} else if(query instanceof GetSourceDatasetInfo) {
			GetSourceDatasetInfo sdi = (GetSourceDatasetInfo)query;
			log.debug(sdi.toString());
			
			String categoryId = sdi.getCategoryId();
			String dataSourceId = sdi.getDataSourceId();
			
			SQLQuery baseQuery = context.query().from(sourceDataset)
					.join (dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
					.join (category).on(sourceDataset.categoryId.eq(category.id))
					.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id));
			
			if(categoryId != null) {
				baseQuery = baseQuery.where(category.identification.eq(categoryId));
			}
			
			if(dataSourceId != null) {
				baseQuery = baseQuery.where(dataSource.identification.eq(dataSourceId));
			}
			
			context.answer(				
					baseQuery					
						.groupBy(dataSource.identification).groupBy(dataSource.name).groupBy(sourceDataset.identification).groupBy(sourceDataset.name)
						.orderBy(sourceDataset.identification.asc())
						.list(new QSourceDatasetInfo(dataSource.identification, dataSource.name, sourceDataset.identification, sourceDataset.name, dataset.count()))
			);
		} else {
			throw new IllegalArgumentException("Unknown query");
		}
	}
}
