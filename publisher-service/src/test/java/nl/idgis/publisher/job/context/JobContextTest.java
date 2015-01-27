package nl.idgis.publisher.job.context;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.HarvestJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.AnyAckRecorder;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.recorder.messages.Watch;
import nl.idgis.publisher.recorder.messages.Watching;
import nl.idgis.publisher.utils.FutureUtils;

public class JobContextTest {
	
	JobInfo jobInfo;
	
	ActorRef jobManager, jobContext, deadWatch;
	
	ActorSystem actorSystem;
	
	FutureUtils f;

	@Before
	public void actorSystem() {
		jobInfo = new HarvestJobInfo(0, "testDataSource");
		
		actorSystem = ActorSystem.create();
		
		jobManager = actorSystem.actorOf(AnyAckRecorder.props(new Ack()));
		jobContext = actorSystem.actorOf(JobContext.props(jobManager, jobInfo));
		deadWatch = actorSystem.actorOf(AnyRecorder.props());
		
		f = new FutureUtils(actorSystem.dispatcher());
		f.ask(deadWatch, new Watch(jobContext), Watching.class);
	}
	
	@Test
	public void testAck() throws Exception {
		jobContext.tell(new Ack(), ActorRef.noSender());
		
		f.ask(deadWatch, new Wait(1), Waited.class).get();
	}
	
	@Test
	public void testStartedAck() throws Exception {
		f.ask(jobContext, new UpdateJobState(JobState.STARTED), Ack.class);		
		
		jobContext.tell(new Ack(), ActorRef.noSender());		
		
		f.ask(jobContext, new UpdateJobState(JobState.SUCCEEDED), Ack.class);		
		f.ask(deadWatch, new Wait(1), Waited.class).get();
	}
}
