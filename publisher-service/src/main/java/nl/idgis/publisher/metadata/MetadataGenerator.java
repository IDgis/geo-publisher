package nl.idgis.publisher.metadata;

import java.util.concurrent.TimeUnit;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import nl.idgis.publisher.database.DatabaseRef;

import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.TypedList;

import nl.idgis.publisher.database.QServiceJob;
import nl.idgis.publisher.database.QJobState;

import nl.idgis.publisher.domain.job.JobState;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QServiceJob.serviceJob;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QCategory.category;

public class MetadataGenerator extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef service;
	
	private final DatabaseRef database;
	
	public MetadataGenerator(ActorRef database, ActorRef service) {
		this.database = new DatabaseRef(database, Timeout.apply(15, TimeUnit.SECONDS), getContext().dispatcher(), log);
		this.service = service;
	}
	
	public static Props props(ActorRef database, ActorRef service) {
		return Props.create(MetadataGenerator.class, database, service);
	}
	
	@Override
	public void preStart() throws Exception {		
		
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GenerateMetadata) {
			generateMetadata();
		} else {
			unhandled(msg);
		}
	}

	private void generateMetadata() {		
		log.debug("generating metadata");
		
		final ActorRef sender = getSender();
		
		QServiceJob otherServiceJob = new QServiceJob("otherServiceJob");
		QJobState otherJobState = new QJobState("otherJobState");
		
		database.query()
			.from(serviceJob)
			.join(dataset).on(dataset.id.eq(serviceJob.datasetId))
			.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(serviceJob.sourceDatasetVersionId))
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
			.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
			.join(category).on(category.id.eq(sourceDatasetVersion.categoryId))			
			.join(jobState).on(
					jobState.jobId.eq(serviceJob.jobId)
				.and(
					jobState.state.eq(JobState.SUCCEEDED.name())))			
			.where(new SQLSubQuery()
				.from(otherServiceJob)
				.join(otherJobState).on(
						otherJobState.jobId.eq(otherServiceJob.jobId)
					.and(
						otherJobState.state.eq(JobState.SUCCEEDED.name())))
				.where(
						otherServiceJob.datasetId.eq(serviceJob.datasetId)
					.and(
						otherJobState.createTime.after(jobState.createTime)))
				.notExists())
			.list(
					dataSource.identification,
					sourceDataset.identification,
					category.identification, 
					dataset.identification)
			.onComplete(new OnComplete<TypedList<Tuple>>() {

				@Override
				public void onComplete(Throwable t, TypedList<Tuple> result) throws Throwable {
					if(t != null) {
						log.error(t, "query failed");
					} else {
						log.debug("query result received");
					
						for(Tuple item : result) {
							log.debug(item.toString());
						}
						
						sender.tell(new Ack(), getSelf());
					}
				}
				
			}, getContext().dispatcher());
			
	}

}
