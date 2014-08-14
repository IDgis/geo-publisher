package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QHarvestJob.harvestJob;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QJobLog.jobLog;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetColumn.sourceDatasetColumn;
import static nl.idgis.publisher.database.QVersion.version;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import nl.idgis.publisher.database.messages.AlreadyRegistered;
import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.DeleteDataset;
import nl.idgis.publisher.database.messages.GetCategoryInfo;
import nl.idgis.publisher.database.messages.GetCategoryListInfo;
import nl.idgis.publisher.database.messages.GetDataSourceInfo;
import nl.idgis.publisher.database.messages.GetDatasetColumns;
import nl.idgis.publisher.database.messages.GetDatasetInfo;
import nl.idgis.publisher.database.messages.GetDatasetListInfo;
import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.GetSourceDatasetColumns;
import nl.idgis.publisher.database.messages.GetSourceDatasetInfo;
import nl.idgis.publisher.database.messages.GetSourceDatasetListInfo;
import nl.idgis.publisher.database.messages.GetVersion;
import nl.idgis.publisher.database.messages.HarvestJob;
import nl.idgis.publisher.database.messages.ImportJob;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.Job;
import nl.idgis.publisher.database.messages.ListQuery;
import nl.idgis.publisher.database.messages.QCategoryInfo;
import nl.idgis.publisher.database.messages.QDataSourceInfo;
import nl.idgis.publisher.database.messages.QDatasetInfo;
import nl.idgis.publisher.database.messages.QHarvestJob;
import nl.idgis.publisher.database.messages.QSourceDatasetInfo;
import nl.idgis.publisher.database.messages.QVersion;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.Registered;
import nl.idgis.publisher.database.messages.SourceDatasetInfo;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.database.messages.UpdateDataset;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.database.projections.QColumn;
import nl.idgis.publisher.domain.job.JobLog;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;

import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Order;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.ComparableExpressionBase;
import com.mysema.query.types.expr.DateTimeExpression;
import com.typesafe.config.Config;

public class PublisherTransaction extends QueryDSLTransaction {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	public PublisherTransaction(Config config, Connection connection) {
		super(config, connection);
	}
	
	private void insertSourceDatasetColumns(QueryDSLContext context, int sourceDatasetId, List<Column> columns) {
		int i = 0;
		for(Column column : columns) {			
			context.insert(sourceDatasetColumn)
				.set(sourceDatasetColumn.sourceDatasetId, sourceDatasetId)
				.set(sourceDatasetColumn.index, i++)
				.set(sourceDatasetColumn.name, column.getName())
				.set(sourceDatasetColumn.dataType, column.getDataType().toString())
				.execute();
		}
	}
	
	private void insertDatasetColumns(QueryDSLContext context, int datasetId, List<Column> columns) {
		int i = 0;
		for(Column column : columns) {			
			context.insert(datasetColumn)
				.set(datasetColumn.datasetId, datasetId)
				.set(datasetColumn.index, i++)
				.set(datasetColumn.name, column.getName())
				.set(datasetColumn.dataType, column.getDataType().toString())
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
			executeGetVersion(context);
		} else if(query instanceof RegisterSourceDataset) {
			executeRegisterSourceDataset(context, (RegisterSourceDataset)query);
		} else if(query instanceof GetCategoryListInfo) {
			executeGetCategoryListInfo(context);
		} else if(query instanceof GetCategoryInfo) {
			executeGetCategoryInfo(context, (GetCategoryInfo)query);			
		} else if(query instanceof GetDatasetListInfo) {
			executeGetDatasetListInfo(context, (GetDatasetListInfo)query);			
		} else if(query instanceof GetDataSourceInfo) {
			executeGetDataSourceInfo(context);
		} else if(query instanceof GetSourceDatasetInfo) {			
			executeGetSourceDatasetInfo(context, (GetSourceDatasetInfo)query);			
		} else if(query instanceof GetSourceDatasetListInfo) {			
			executeGetSourceDatasetListInfo(context, (GetSourceDatasetListInfo)query);			
		} else if(query instanceof StoreLog) {
			executeStoreLog(context, (StoreLog)query);
		} else if(query instanceof GetHarvestJobs){
			executeGetHarvestJobs(context);
		} else if(query instanceof GetSourceDatasetColumns) {
			executeGetSourceDatasetColumns(context, (GetSourceDatasetColumns)query);
		} else if(query instanceof GetDatasetColumns) {
			executeGetDatasetColumns(context, (GetDatasetColumns)query);
		} else if(query instanceof GetImportJobs) {				
			executeGetImportJobs(context);
		} else if(query instanceof CreateDataset) {
			executeCreateDataset(context, (CreateDataset)query);
		} else if(query instanceof GetDatasetInfo) {			
			executeGetDatasetInfo(context, (GetDatasetInfo)query);
		} else if(query instanceof UpdateDataset) {						
			executeUpdatedataset(context, (UpdateDataset)query);
		} else if(query instanceof DeleteDataset) {
			executeDeleteDataset(context, (DeleteDataset)query);
		} else if(query instanceof CreateHarvestJob) {
			executeCreateHarvestJob(context, (CreateHarvestJob)query);
		} else if(query instanceof CreateImportJob) {
			executeCreateImportJob(context, (CreateImportJob)query);
		} else if(query instanceof UpdateJobState) {
			executeUpdateJobState(context, (UpdateJobState)query);
		} else {
			throw new IllegalArgumentException("Unknown query");
		}
	}

	private void executeUpdateJobState(QueryDSLContext context, UpdateJobState query) {
		log.debug("updating job state: " + query);
		
		int jobId = getJobQuery(context,  query.getJob())
				.singleResult(job.id);
		
		context.insert(jobState)
			.set(jobState.jobId, jobId)
			.set(jobState.state, query.getState().name())
			.execute();
		
		context.ack();
	}

	private SQLQuery getJobQuery(QueryDSLContext context, Job job) {		
		if(job instanceof ImportJob) {
			return getJobQuery(context, (ImportJob)job);
		} else if(job instanceof HarvestJob) {
			return getJobQuery(context, (HarvestJob)job);
		} else {
			throw new IllegalArgumentException("unknown job type");
		}
	}	
	
	private <T extends Collection<? extends Enum<?>>> Collection<String> enumsToStrings(final T enums) {
		return new AbstractCollection<String>() {

			@Override
			public Iterator<String> iterator() {
				final Iterator<? extends Enum<?>> enumIterator = enums.iterator();
				
				return new Iterator<String>() {

					@Override
					public boolean hasNext() {
						return enumIterator.hasNext();
					}

					@Override
					public String next() {
						return enumIterator.next().name();
					}

					@Override
					public void remove() {
						enumIterator.remove();
					}					
				};	
			}

			@Override
			public int size() {				
				return enums.size();
			}
		};
	}

	private SQLQuery getJobQuery(QueryDSLContext context, ImportJob ij) {
		
		
		return context.query().from(job)
				.join(importJob).on(importJob.jobId.eq(job.id))
				.join(dataset).on(dataset.id.eq(importJob.datasetId))
				.where(dataset.identification.eq(ij.getDatasetId()))
				.where(unfinishedState());
	}

	private BooleanExpression unfinishedState() {
		QJobState jobStateSub = new QJobState("job_state_sub");
		
		return new SQLSubQuery().from(jobStateSub)
				.where(jobStateSub.jobId.eq(job.id)
					.and(jobStateSub.state.in(enumsToStrings(JobState.getFinished()))))
				.notExists();
	}	

	private SQLQuery getJobQuery(QueryDSLContext context, HarvestJob hj) {
		return context.query().from(job)
				.join(harvestJob).on(harvestJob.jobId.eq(job.id))
				.join(dataSource).on(dataSource.id.eq(harvestJob.dataSourceId))
				.where(dataSource.identification.eq(hj.getDataSourceId()))
				.where(unfinishedState());
	}

	private void executeCreateImportJob(QueryDSLContext context, CreateImportJob query) {
		log.debug("creating import job: " + query);
		
		if(context.query().from(job)
			.join(importJob).on(importJob.jobId.eq(job.id))
			.join(dataset).on(dataset.id.eq(importJob.datasetId))
			.where(dataset.identification.eq(query.getDatasetId()))
			.where(new SQLSubQuery().from(jobState)
					.where(jobState.jobId.eq(job.id))
					.notExists())
			.notExists()) {
			
			int jobId = context.insert(job)
					.set(job.type, "IMPORT")
					.executeWithKey(job.id);
			
			int datasetId = context.query().from(dataset)
				.where(dataset.identification.eq(query.getDatasetId()))
				.singleResult(dataset.id);
			
			context.insert(importJob)
				.set(importJob.jobId, jobId)
				.set(importJob.datasetId, datasetId)
				.execute();
			
			log.debug("import job created");
		} else {
			log.debug("already exist an import job for this dataset");
		}
		
		context.ack();
	}

	private void executeCreateHarvestJob(QueryDSLContext context, CreateHarvestJob query) {
		log.debug("creating harvest job: " + query);
		
		if(context.query().from(job)
			.join(harvestJob).on(harvestJob.jobId.eq(job.id))
			.join(dataSource).on(dataSource.id.eq(harvestJob.dataSourceId))
			.where(dataSource.identification.eq(query.getDataSourceId()))
			.where(new SQLSubQuery().from(jobState)
					.where(jobState.jobId.eq(job.id))
					.notExists())
			.notExists()) {
			
			int jobId = context.insert(job)
				.set(job.type, "HARVEST")
				.executeWithKey(job.id);
			
			int dataSourceId = context.query().from(dataSource)
				.where(dataSource.identification.eq(query.getDataSourceId()))
				.singleResult(dataSource.id);
			
			context.insert(harvestJob)
				.set(harvestJob.jobId, jobId)				
				.set(harvestJob.dataSourceId, dataSourceId)
				.execute();
			
			log.debug("harvest job created");
		} else {
			log.debug("already exist a harvest job for this dataSource");
		}
		
		context.ack();
	}

	private void executeDeleteDataset(QueryDSLContext context, DeleteDataset dds) {
		Long nrOfDatasetColumnsDeleted = context.delete(datasetColumn)
				.where(
					new SQLSubQuery().from(dataset)
					.where(dataset.identification.eq(dds.getId())
					.and(dataset.id.eq(datasetColumn.datasetId))).exists())
				.execute();
		log.debug("nrOfDatasetColumnsDeleted: " + nrOfDatasetColumnsDeleted);
		
		Long nrOfDatasetsDeleted = context.delete(dataset)
			.where(dataset.identification.eq(dds.getId()))
			.execute();
		log.debug("nrOfDatasetsDeleted: " + nrOfDatasetsDeleted);
		
		if (nrOfDatasetsDeleted > 0 || nrOfDatasetColumnsDeleted >= 0){
			context.answer(new Response<Long>(CrudOperation.DELETE, CrudResponse.OK, nrOfDatasetColumnsDeleted));
		} else {
			context.answer(new Response<String>(CrudOperation.DELETE, CrudResponse.NOK, dds.getId()));
		}
	}

	private void executeUpdatedataset(QueryDSLContext context, UpdateDataset uds) {
		String sourceDatasetIdent = uds.getSourceDatasetIdentification();
		String datasetIdent = uds.getDatasetIdentification();
		String datasetName = uds.getDatasetName();
		log.debug("update dataset" + datasetIdent);
		
		Integer sourceDatasetId = context.query().from(sourceDataset)
				.where(sourceDataset.identification.eq(sourceDatasetIdent))
				.singleResult(sourceDataset.id);

		context.update(dataset)
			.set(dataset.name, datasetName)
			.set(dataset.sourceDatasetId, sourceDatasetId)
			.where(dataset.identification.eq(datasetIdent))
			.execute();
			
		Integer datasetId = context.query().from(dataset)
				.where(dataset.identification.eq(datasetIdent))
				.singleResult(dataset.id);
		
		context.delete(datasetColumn)
			.where(datasetColumn.datasetId.eq(datasetId))
			.execute();
		
		insertDatasetColumns(context, datasetId, uds.getColumnList());
		context.answer(new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, datasetIdent));
		
		log.debug("dataset updated");
	}

	private void executeGetDatasetInfo(QueryDSLContext context, GetDatasetInfo gds) {
		String datasetIdent = gds.getId();
		log.debug("get dataset " + datasetIdent);
		
		context.answer(
				context.query().from(dataset)
				.join (sourceDataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
				.leftJoin (category).on(sourceDataset.categoryId.eq(category.id))
				.where(dataset.identification.eq( datasetIdent ))
				.singleResult(new QDatasetInfo(dataset.identification, dataset.name, 
						sourceDataset.identification, sourceDataset.name,
						category.identification,category.name)));
	}

	private void executeCreateDataset(QueryDSLContext context, CreateDataset cds) {
		String sourceDatasetIdent = cds.getSourceDatasetIdentification();
		String datasetIdent = cds.getDatasetIdentification();
		String datasetName = cds.getDatasetName();
		log.debug("create dataset " + datasetIdent);

		Integer sourceDatasetId = context.query().from(sourceDataset)
				.where(sourceDataset.identification.eq(sourceDatasetIdent))
				.singleResult(sourceDataset.id);
			if(sourceDatasetId == null) {
				log.error("sourceDataset not found: " + sourceDatasetIdent);
				context.answer(new Response<String>(CrudOperation.CREATE, CrudResponse.NOK, datasetIdent));
			} else {
				context.insert(dataset)
					.set(dataset.identification, datasetIdent)
					.set(dataset.name, datasetName)
					.set(dataset.sourceDatasetId, sourceDatasetId)
					.execute();
				
				Integer datasetId = context.query().from(dataset)
					.where(dataset.identification.eq(datasetIdent))
					.singleResult(dataset.id);
				
				insertDatasetColumns(context, datasetId, cds.getColumnList());					
				context.answer(new Response<String>(CrudOperation.CREATE, CrudResponse.OK, datasetIdent));
				
				log.debug("dataset inserted");
			}
	}

	private void executeGetImportJobs(QueryDSLContext context) {
		SQLQuery query = context.query().from(job)
			.join(importJob).on(importJob.jobId.eq(job.id))
			.join(dataset).on(dataset.id.eq(importJob.datasetId))
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
			.orderBy(job.createTime.asc())
			.where(new SQLSubQuery().from(jobState)
					.where(jobState.jobId.eq(job.id))
					.notExists());
		
		List<Tuple> baseList = query.clone()			
			.list(
					dataSource.identification,
					sourceDataset.identification,
					dataset.id,
					dataset.identification);
		
		List<Tuple> columnList = query.join(datasetColumn).on(datasetColumn.datasetId.eq(dataset.id))			
			.orderBy(datasetColumn.index.asc())
			.list(dataset.id, datasetColumn.name, datasetColumn.dataType);
		
		ListIterator<Tuple> columnIterator = columnList.listIterator();
		ArrayList<ImportJob> jobs = new ArrayList<>();
		for(Tuple t : baseList) {
			int datasetId = t.get(dataset.id);
			
			ArrayList<Column> columns = new ArrayList<>();
			for(; columnIterator.hasNext();) {
				Tuple tc = columnIterator.next();
				
				int columnDatasetId = tc.get(dataset.id);				
				if(columnDatasetId != datasetId) {
					columnIterator.previous();
					break;
				}
				
				columns.add(new Column(tc.get(datasetColumn.name), tc.get(datasetColumn.dataType)));
			}
			
			jobs.add(new ImportJob(
					t.get(dataSource.identification), 
					t.get(sourceDataset.identification),
					t.get(dataset.identification),
					columns));
		}
		
		context.answer(jobs);
	}

	private void executeGetDatasetColumns(QueryDSLContext context, GetDatasetColumns dc) {		
		log.debug("get columns for dataset: " + dc.getDatasetId());
		
		context.answer(
			context.query().from(datasetColumn)
			.join(dataset).on(dataset.id.eq(datasetColumn.datasetId))
			.where(dataset.identification.eq(dc.getDatasetId()))
			.list(new QColumn(datasetColumn.name, datasetColumn.dataType)));
	}

	private void executeGetSourceDatasetColumns(QueryDSLContext context, GetSourceDatasetColumns sdc) {
		log.debug("get columns for sourcedataset: " + sdc.getSourceDatasetId());

		context.answer(
			context.query().from(sourceDatasetColumn)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetColumn.sourceDatasetId))
			.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
			.where(sourceDataset.identification.eq(sdc.getSourceDatasetId())
				.and(dataSource.identification.eq(sdc.getDataSourceId())))
			.list(new QColumn(sourceDatasetColumn.name, sourceDatasetColumn.dataType)));
	}

	private void executeGetHarvestJobs(QueryDSLContext context) {
		context.answer(
			context.query().from(job)
				.join(harvestJob).on(harvestJob.jobId.eq(job.id))
				.join(dataSource).on(dataSource.id.eq(harvestJob.dataSourceId))
				.orderBy(job.createTime.asc())
				.where(new SQLSubQuery().from(jobState)
						.where(jobState.jobId.eq(job.id))
						.notExists())
				.list(new QHarvestJob(dataSource.identification)));
	}

	private void executeStoreLog(QueryDSLContext context, StoreLog query) throws Exception {
		log.debug("storing log line: " + query);
		
		int jobStateId = getJobQuery(context, query.getJob())
			.join(jobState).on(jobState.jobId.eq(job.id))
			.orderBy(jobState.id.desc())
			.limit(1)
			.singleResult(jobState.id);
		
		JobLog jl = query.getJobLog();
		
		context.insert(jobLog)
			.set(jobLog.jobStateId, jobStateId)
			.set(jobLog.level, jl.getLevel().name())
			.set(jobLog.type, jl.getType().name())
			.set(jobLog.content, toJson(jl.getContent()))
			.execute();
		
		context.ack();
	}	

	private String toJson(Object content) throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();
		return om.writeValueAsString(content);
	}

	private void executeGetSourceDatasetListInfo(QueryDSLContext context,
			GetSourceDatasetListInfo sdi) {
		log.debug(sdi.toString());
		
		String categoryId = sdi.getCategoryId();
		String dataSourceId = sdi.getDataSourceId();
		String searchStr = sdi.getSearchString();
		
		SQLQuery baseQuery = context.query().from(sourceDataset)
				.join (dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.join (category).on(sourceDataset.categoryId.eq(category.id));
		
		if(categoryId != null) {				
			baseQuery.where(category.identification.eq(categoryId));
		}
		
		if(dataSourceId != null) {				
			baseQuery.where(dataSource.identification.eq(dataSourceId));
		}
		
		if (!(searchStr == null || searchStr.isEmpty())){
			baseQuery.where(sourceDataset.name.containsIgnoreCase(searchStr)); 				
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
	}

	private void executeGetSourceDatasetInfo(QueryDSLContext context,
			GetSourceDatasetInfo sdi) {
		log.debug(sdi.toString());
		String sourceDatasetId = sdi.getId();
		
		SQLQuery baseQuery = context.query().from(sourceDataset)
				.join (dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.join (category).on(sourceDataset.categoryId.eq(category.id));
		
		if(sourceDatasetId != null) {				
			baseQuery.where(sourceDataset.identification.eq(sourceDatasetId));
		}
			
		SQLQuery listQuery = baseQuery.clone()					
				.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id));
		
		context.answer(
			listQuery					
				.groupBy(sourceDataset.identification).groupBy(sourceDataset.name)
				.groupBy(dataSource.identification).groupBy(dataSource.name)
				.groupBy(category.identification).groupBy(category.name)						
				.singleResult(new QSourceDatasetInfo(sourceDataset.identification, sourceDataset.name, 
						dataSource.identification, dataSource.name,
						category.identification,category.name,
						dataset.count())
			)
		);
	}

	private void executeGetDataSourceInfo(QueryDSLContext context) {
		context.answer(
			context.query().from(dataSource)
				.orderBy(dataSource.identification.asc())
				.list(new QDataSourceInfo(dataSource.identification, dataSource.name)));
	}

	private void executeGetDatasetListInfo(QueryDSLContext context, GetDatasetListInfo dli) {
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
				.list(new QDatasetInfo(dataset.identification, dataset.name, 
						sourceDataset.identification, sourceDataset.name,
						category.identification,category.name))
		);
	}

	private void executeGetCategoryInfo(QueryDSLContext context, GetCategoryInfo query) {
		context.answer(
				context.query().from(category)
				.where(category.identification.eq(query.getId()))
				.singleResult(new QCategoryInfo(category.identification,category.name)));
	}

	private void executeGetCategoryListInfo(QueryDSLContext context) {
		context.answer(
				context.query().from(category)
				.orderBy(category.identification.asc())
				.list(new QCategoryInfo(category.identification,category.name)));
	}

	private void executeRegisterSourceDataset(QueryDSLContext context, RegisterSourceDataset rsd) {
		log.debug("registering source dataset: " + rsd);
		
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
				
				log.debug("dataSource updated");
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
	}

	private void executeGetVersion(QueryDSLContext context) {
		log.debug("database version requested");
		
		context.answer(
			context.query().from(version)
				.orderBy(version.id.desc())
				.limit(1)
				.singleResult(new QVersion(version.id, version.createTime)));
	}
}
