package nl.idgis.publisher;

import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QVersion.version;
import static nl.idgis.publisher.utils.EnumUtils.enumsToStrings;

import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.admin.Admin;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.GeometryDatabase;
import nl.idgis.publisher.database.PublisherDatabase;
import nl.idgis.publisher.database.QJobState;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.dataset.DatasetManager;
import nl.idgis.publisher.harvester.Harvester;
import nl.idgis.publisher.job.JobSystem;
import nl.idgis.publisher.loader.Loader;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.metadata.FileMetadataStore;
import nl.idgis.publisher.metadata.MetadataGenerator;
import nl.idgis.publisher.metadata.MetadataStore;
import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.monitor.messages.Tree;
import nl.idgis.publisher.service.Service;
import nl.idgis.publisher.utils.Boot;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.JdbcUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import com.mysema.query.sql.SQLSubQuery;
import com.typesafe.config.Config;

public class ServiceApp extends UntypedActor {
	
	private static class StartService implements Serializable {
		
		private static final long serialVersionUID = 2801573884157962370L;

		@Override
		public String toString() {
			return "StartService []";
		}
		
	}
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final QJobState jobState1 = new QJobState("js1"), jobState2 = new QJobState("js2");
	
	private final Config config;
	
	private ActorRef database;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	public ServiceApp(Config config) {
		this.config = config;
	}
	
	public static Props props(Config config) {
		return Props.create(ServiceApp.class, config);
	}
	
	@Override
	public void preStart() throws Exception {
		log.info("starting service application");
		
		Config databaseConfig = config.getConfig("database");
		database = getContext().actorOf(PublisherDatabase.props(databaseConfig), "database");
		
		f = new FutureUtils(getContext().dispatcher());
		db = new AsyncDatabaseHelper(database, f, log);
		
		db.query().from(version)
			.orderBy(version.id.desc())
			.limit(1)
			.singleResult(version.id, version.createTime).thenCompose(versionInfo -> {
				int versionId = versionInfo.get(version.id);
				Timestamp createTime = versionInfo.get(version.createTime);
				
				log.info("database version id: {}, createTime: {}", versionId, createTime);
				
				int lastVersionId = JdbcUtils.maxRev("nl/idgis/publisher/database", versionId);
				
				if(versionId != lastVersionId) {
					log.error("database obsolete, expected version id: " + lastVersionId);
					
					return f.<Object>successful(PoisonPill.getInstance());
				} else {
					// mark running all jobs (started but not finished) as failed 
					
					return db.insert(jobState)
						.columns(
							jobState.jobId,
							jobState.state)
						.select(
							new SQLSubQuery().from(jobState1)
								.where(jobState1.state.eq(JobState.STARTED.name())
									.and(new SQLSubQuery().from(jobState2)
											.where(jobState1.jobId.eq(jobState2.jobId)
													.and(jobState2.state.in(
														enumsToStrings(JobState.getFinished()))))
											.notExists()))
							.list(
								jobState1.jobId, 
								JobState.FAILED.name()))
						.execute().thenApply(jobsTerminated -> {
							log.debug("jobs terminated: {}",  + jobsTerminated);
							
							return new StartService();
						});
				}
			}).whenComplete((msg, t) -> {
				if(t != null) {
					log.error("service initialization failed: {}", t);
					
					getSelf().tell(PoisonPill.getInstance(), getSelf());
				} else {
					log.debug("sending start message: {}", msg);
					
					getSelf().tell(msg, getSelf());
				}
			});
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof StartService) {
			getContext().become(running());
		} else {
			unhandled(msg);
		}
	}
	
	private Procedure<Object> running() throws Exception {
		Config geometryDatabaseConfig = config.getConfig("geometry-database");
		final ActorRef geometryDatabase = getContext().actorOf(GeometryDatabase.props(geometryDatabaseConfig), "geometryDatabase");
		
		final ActorRef datasetManager = getContext().actorOf(DatasetManager.props(database));
		
		Config harvesterConfig = config.getConfig("harvester");
		final ActorRef harvester = getContext().actorOf(Harvester.props(datasetManager, harvesterConfig), "harvester");
		
		final ActorRef loader = getContext().actorOf(Loader.props(geometryDatabase, database, harvester), "loader");
		
		Config geoserverConfig = config.getConfig("geoserver");
		
		final ActorRef service = getContext().actorOf(Service.props(geoserverConfig, geometryDatabaseConfig), "service");
		
		ActorRef jobSystem = getContext().actorOf(JobSystem.props(database, harvester, loader, service), "jobs");
		
		getContext().actorOf(Admin.props(database, harvester, loader, service, jobSystem), "admin");
		
		Config metadataConfig = config.getConfig("metadata");
		
		MetadataStore serviceMetadataSource = new FileMetadataStore(new File(metadataConfig.getString("serviceSource")));
		MetadataStore datasetMetadataTarget = new FileMetadataStore(new File(metadataConfig.getString("datasetTarget")));
		MetadataStore serviceMetadataTarget = new FileMetadataStore(new File(metadataConfig.getString("serviceTarget")));		
		
		ActorRef metadataGenerator = getContext().actorOf(MetadataGenerator.props(database, service, harvester, serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget, metadataConfig.getConfig("generator-constants")), "metadata-generator");
		getContext().system().scheduler().schedule(
				Duration.create(10, TimeUnit.SECONDS), 
				Duration.create(10, TimeUnit.SECONDS),
				metadataGenerator, new GenerateMetadata(), 
				getContext().dispatcher(), getSelf());
		
		if(log.isDebugEnabled()) {
			ActorSystem system = getContext().system();
			system.scheduler().schedule(
					Duration.Zero(), 
					Duration.create(5, TimeUnit.SECONDS), 
					getSelf(), 
					new GetActiveJobs(), 
					getContext().dispatcher(), 
					getSelf());
		}
		
		log.info("service application started");
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Tree) {
					log.debug(msg.toString());
				} else if(msg instanceof GetActiveJobs) {
					harvester.tell(msg, getSelf());
					loader.tell(msg, getSelf());
				} else if(msg instanceof ActiveJobs) {
					log.debug("active jobs: " + msg);
				} else {
					unhandled(msg);
				}
			}
		};
	}

	public static void main(String[] args) {
		Boot boot = Boot.init("service");
		boot.startApplication(ServiceApp.props(boot.getConfig()));
	}
}
