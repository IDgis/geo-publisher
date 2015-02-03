package nl.idgis.publisher.job.context;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.load.ImportLogType;

import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.HarvestJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.AnyAckRecorder;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.Create;
import nl.idgis.publisher.recorder.messages.Created;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.recorder.messages.Watch;
import nl.idgis.publisher.recorder.messages.Watching;
import nl.idgis.publisher.utils.FutureUtils;

public class JobContextTest {
	
	JobInfo jobInfo;
	
	ActorRef jobManager, jobContextParent, jobContext, deadWatch;
	
	ActorSystem actorSystem;
	
	FutureUtils f;

	@Before
	public void actorSystem() throws Exception {
		jobInfo = new HarvestJobInfo(0, "testDataSource");
		
		Config akkaConfig = ConfigFactory.empty()
				.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
				.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.create("test", akkaConfig);
		f = new FutureUtils(actorSystem.dispatcher());
		
		jobManager = actorSystem.actorOf(AnyAckRecorder.props(new Ack()));
		
		jobContextParent = actorSystem.actorOf(AnyRecorder.props());
		jobContext = f.ask(jobContextParent, new Create(JobContext.props(jobManager, jobInfo)), Created.class).get().getActorRef();
		
		deadWatch = actorSystem.actorOf(AnyRecorder.props());		
		f.ask(deadWatch, new Watch(jobContext), Watching.class);
	}
	
	@Test
	public void testAck() throws Exception {
		jobContext.tell(new Ack(), ActorRef.noSender());
		
		f.ask(deadWatch, new Wait(1), Waited.class).get();
		
		f.ask(jobContextParent, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Ack.class)
			.assertNotHasNext();
	}
	
	@Test
	public void testStartedAck() throws Exception {
		f.ask(jobContext, new UpdateJobState(JobState.STARTED), Ack.class);		
		
		jobContext.tell(new Ack(), ActorRef.noSender());		
		
		f.ask(jobContext, new UpdateJobState(JobState.SUCCEEDED), Ack.class);		
		f.ask(deadWatch, new Wait(1), Waited.class).get();
		
		f.ask(jobContextParent, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Ack.class)
			.assertNotHasNext();
	}
	
	@Test
	public void testFailedAck() throws Exception {
		f.ask(jobContext, new UpdateJobState(JobState.FAILED), Ack.class);
		
		jobContext.tell(new Ack(), ActorRef.noSender());
		
		f.ask(deadWatch, new Wait(1), Waited.class).get();
		
		f.ask(jobContextParent, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Ack.class)
			.assertNotHasNext();
	}
	
	@Test
	public void testStartedFailedAck() throws Exception {
		f.ask(jobContext, new UpdateJobState(JobState.SUCCEEDED), Ack.class);
		f.ask(jobContext, new UpdateJobState(JobState.FAILED), Ack.class);
		
		jobContext.tell(new Ack(), ActorRef.noSender());
		
		f.ask(deadWatch, new Wait(1), Waited.class).get();
		
		f.ask(jobContextParent, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Ack.class)
			.assertNotHasNext();
	}
	
	@Test
	public void testStartedLogFailedAck() throws Exception {
		f.ask(jobContext, new UpdateJobState(JobState.STARTED), Ack.class);
		
		f.ask(jobContext, Log.create(LogLevel.WARNING, ImportLogType.MISSING_COLUMNS), Ack.class);
		f.ask(jobContext, Log.create(LogLevel.ERROR, ImportLogType.MISSING_FILTER_COLUMNS), Ack.class);
		f.ask(jobContext, new UpdateJobState(JobState.FAILED), Ack.class);		
		
		jobContext.tell(new Ack(), ActorRef.noSender());
		
		f.ask(deadWatch, new Wait(1), Waited.class).get();
		
		f.ask(jobContextParent, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Ack.class)
			.assertNotHasNext();
	}
}
