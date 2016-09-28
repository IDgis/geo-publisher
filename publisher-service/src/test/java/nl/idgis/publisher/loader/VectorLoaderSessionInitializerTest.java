package nl.idgis.publisher.loader;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.messages.CreateIndices;
import nl.idgis.publisher.database.messages.CreateTable;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.InsertRecords;
import nl.idgis.publisher.database.messages.Query;

import nl.idgis.publisher.DatabaseMock;
import nl.idgis.publisher.TransactionMock;

import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Type;

import nl.idgis.publisher.harvester.sources.messages.FetchVectorDataset;
import nl.idgis.publisher.harvester.sources.messages.StartVectorImport;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.VectorImportJobInfo;
import nl.idgis.publisher.loader.messages.SessionFinished;
import nl.idgis.publisher.loader.messages.SessionStarted;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.recorder.AnyAckRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.stream.IteratorCursor;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;

public class VectorLoaderSessionInitializerTest {
	
	ActorSystem actorSystem;
	
	FutureUtils f;
	
	ActorRef database, jobContext, dataSource, loader, datasetManager;
	
	static class LoaderTransactionMock extends TransactionMock {
		
		Map<String, List<String>> tables = new HashMap<>(); 
		
		public static Props props() {
			return Props.create(LoaderTransactionMock.class);
		}
		
		@Override
		public void preStart() throws Exception {
			log.debug("test transaction started");
		}

		@Override
		public void handleQuery(Query query) throws Exception {
			log.debug("query received: {}", query);
			
			if(query instanceof GetDatasetStatus) {
				log.debug("get dataset info");
				
				GetDatasetStatus gds = (GetDatasetStatus)query;
				getSender().tell(
					new DatasetStatusInfo(gds.getDatasetId(), false, false, false, 
						false, false, false, false), 
					getSelf());
			} else if(query instanceof InsertRecords) {
				log.debug("insert records");
				
				getSender().tell(new Ack(), getSelf());
			} else if(query instanceof CreateIndices) {
				log.debug("create indices");
				
				getSender().tell(new Ack(), getSelf());
			} else if(query instanceof CreateTable) {
				log.debug("create table");
				
				getSender().tell(new Ack(), getSelf());
			} else {
				log.debug("unhandled: {}, from: {}", query, getSender());
				
				unhandled(query);
			}
		}
		
	}
	
	static class DataSourceMock extends UntypedActor {
		
		private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		private int sessions = 0, cursors = 0;
		
		static Props props() {
			return Props.create(DataSourceMock.class);
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof FetchVectorDataset) {
				log.debug("fetch vector dataset");
				
				FetchVectorDataset fetchVectorDataset = (FetchVectorDataset)msg;
				
				ActorRef receiver = getContext().actorOf(fetchVectorDataset.getReceiverProps(), "session-" + sessions++);
				receiver.tell(new StartVectorImport(getSender(), 1), getSelf());
			} else if(msg instanceof Ack) {
				log.debug("ack");
				
				ActorRef cursor = getContext().actorOf(
					IteratorCursor.props(Arrays.asList(
						new Records(Arrays.asList(
							new Record(Arrays.asList("test"))))).iterator()),
					"cursor" + cursors++);
				cursor.tell(new NextItem(), getSender());
			} else {
				log.debug("unhandled: {}, from: {}", msg, getSender());
				
				unhandled(msg);
			}			
		}
		
	}
	
	static class LoaderMock extends UntypedActor {
		
		private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		private final ActorRef jobContext, database, dataSource, datasetManager;
		
		LoaderMock(ActorRef jobContext, ActorRef database, ActorRef dataSource, ActorRef datasetManager) {
			this.jobContext = jobContext;
			this.database = database;
			this.dataSource = dataSource;
			this.datasetManager = datasetManager;
		}
		
		static Props props(ActorRef jobContext, ActorRef database, ActorRef dataSource, ActorRef datasetManager) {
			return Props.create(LoaderMock.class, jobContext, database, dataSource, datasetManager);
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof VectorImportJobInfo) {
				log.debug("vector import job info");
				
				VectorImportJobInfo importJob = (VectorImportJobInfo)msg;
				ActorRef initiator = getContext().actorOf(VectorLoaderSessionInitiator.props(importJob, jobContext, database, datasetManager, Duration.apply(1, TimeUnit.SECONDS)), "initiator");
				initiator.tell(dataSource, getSelf());
			} else if(msg instanceof SessionStarted){
				log.debug("session started");
				
				getSender().tell(new Ack(), getSender());
			} else if(msg instanceof SessionFinished) {
				log.debug("session finished");
				
				getSender().tell(new Ack(), getSender());
			} else if(msg instanceof Progress) {
				log.debug("progress");
			} else {
				log.debug("unhandled: {}, from: {}", msg, getSender());
				
				unhandled(msg);
			}
		}
		
	}

	@Before
	public void actorSystem() throws Exception {		
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
			
		actorSystem = ActorSystem.create("test", akkaConfig);
		
		database = actorSystem.actorOf(DatabaseMock.props(LoaderTransactionMock.props()), "database");
		
		jobContext = actorSystem.actorOf(AnyAckRecorder.props(new Ack()), "job-context");
		
		dataSource = actorSystem.actorOf(DataSourceMock.props(), "data-source");
		
		datasetManager = actorSystem.actorOf(AnyAckRecorder.props(new Ack()), "dataset-manager");
		
		loader = actorSystem.actorOf(LoaderMock.props(jobContext, database, dataSource, datasetManager));
		
		f = new FutureUtils(actorSystem);
	}
	
	@After
	public void shutdown() throws Exception {
		actorSystem.shutdown();
	}
	
	@Test
	public void testSuccessful() throws Exception {
		List<Column> columns = new ArrayList<>();
		
		for(int i = 0; i < 5; i++) {
			columns.add(new Column("column" + i, Type.NUMERIC));
		}
		
		VectorImportJobInfo importJob = new VectorImportJobInfo(0, "categoryId", "dataSourceId", UUID.randomUUID().toString(), "sourceDatasetId", 
				"datasetId", "datasetName", null /* filterCondition */, columns, columns, Collections.emptyList());
		
		loader.tell(importJob, ActorRef.noSender());
		
		f.ask(jobContext, new Wait(3), Waited.class).get();
		f.ask(jobContext, new GetRecording(), Recording.class).get()
			.assertNext(UpdateJobState.class, update -> {
				assertEquals(JobState.STARTED, update.getState());
			})
			.assertNext(Ack.class)
			.assertNext(UpdateJobState.class, update -> {
				assertEquals(JobState.SUCCEEDED, update.getState());
			})
			.assertNotHasNext();
	}
}
