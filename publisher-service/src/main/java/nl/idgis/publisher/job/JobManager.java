package nl.idgis.publisher.job;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QHarvestJob.harvestJob;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QJobState.jobState;

import java.util.concurrent.TimeUnit;

import scala.concurrent.Future;

import com.mysema.query.sql.SQLSubQuery;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.CreateServiceJob;
import nl.idgis.publisher.database.messages.GetDataSourceStatus;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.GetServiceJobs;
import nl.idgis.publisher.database.messages.QHarvestJobInfo;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.pattern.PipeToSupport.PipeableFuture;
import akka.util.Timeout;

public class JobManager extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	
	public JobManager(ActorRef database) {
		this.database = database;	
	}
	
	public static Props props(ActorRef database) {
		return Props.create(JobManager.class, database);
	}
	
	private AsyncSQLQuery query() {
		return new AsyncSQLQuery(database, new Timeout(15, TimeUnit.SECONDS), getContext().dispatcher());
	}
	
	private <T> PipeableFuture<T> pipe(Future<T> future) {
		return Patterns.pipe(future, getContext().dispatcher());
	}
	
	@Override
	public void preStart() throws Exception {
		
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("jobs: " + msg);
		
		if(msg instanceof GetImportJobs) {
			database.forward(msg, getContext());
		} else if(msg instanceof GetHarvestJobs) {
			handleGetHarvestJobs();
		} else if(msg instanceof GetServiceJobs) {
			database.forward(msg, getContext());
		} else if(msg instanceof CreateHarvestJob) {
			database.forward(msg, getContext());
		} else if(msg instanceof CreateImportJob) {
			database.forward(msg, getContext());
		} else if(msg instanceof CreateServiceJob) {
			database.forward(msg, getContext());
		} else if(msg instanceof GetDataSourceStatus) {
			database.forward(msg, getContext());
		} else if(msg instanceof GetDatasetStatus) {
			database.forward(msg, getContext());
		} else {
			unhandled(msg);
		}
	}
	
	private void handleGetHarvestJobs() {
		log.debug("fetching harvest jobs");
		
		pipe(
			query().from(job)
				.join(harvestJob).on(harvestJob.jobId.eq(job.id))
				.join(dataSource).on(dataSource.id.eq(harvestJob.dataSourceId))
				.orderBy(job.createTime.asc())
				.where(new SQLSubQuery().from(jobState)
						.where(jobState.jobId.eq(job.id))
						.notExists())
				.list(new QHarvestJobInfo(job.id, dataSource.identification)))
			
			.pipeTo(getSender(), getSelf());
	}
}
