package nl.idgis.publisher.loader;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
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
import akka.actor.Terminated;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.harvester.sources.messages.StartRasterImport;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.RasterImportJobInfo;
import nl.idgis.publisher.loader.VectorLoaderSessionTest.LoaderRecorder;
import nl.idgis.publisher.loader.messages.SessionFinished;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.AnyAckRecorder;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.Cleared;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.recorder.messages.Watch;
import nl.idgis.publisher.recorder.messages.Watching;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;

public class RasterLoaderSessionTest {
	
	ActorSystem actorSystem;
	
	ActorRef loader, jobContext, loaderSession, receiver;
	
	FutureUtils f;

	@After
	public void shutdown() {
		actorSystem.shutdown();
	}

	@Before
	public void actorSystem() {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.create("test", akkaConfig);
		
		loader = actorSystem.actorOf(LoaderRecorder.props());
		
		jobContext = actorSystem.actorOf(AnyAckRecorder.props(new Ack()));
		
		receiver = actorSystem.actorOf(AnyAckRecorder.props(new Ack()));
		
		RasterImportJobInfo importJob = new RasterImportJobInfo(0, "categoryId", "dataSourceId", UUID.randomUUID().toString(), 
			"sourceDatasetId", "datasetId", "datasetName", Collections.emptyList());
		
		loaderSession = actorSystem.actorOf(RasterLoaderSession.props(Duration.create(1, TimeUnit.SECONDS), 2, loader, importJob, jobContext, receiver));
		
		f = new FutureUtils(actorSystem);
	}
	
	@Test
	public void testSucessful() throws Exception {
		ActorRef initiator = actorSystem.actorOf(AnyRecorder.props());
		
		final long size = 1024;
		
		f.ask(loaderSession, new StartRasterImport(initiator, size), Ack.class).get();
		
		f.ask(initiator, new Wait(1), Waited.class).get();
		f.ask(initiator, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Ack.class)
			.assertNotHasNext();
		
		f.ask(loader, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Progress.class, progress -> {
				assertEquals(0, progress.getCount());
				assertEquals(size, progress.getTotalCount());
			})
			.assertNotHasNext();
		
		final int chunkSize = 512;		
		for(int i = 0; i < (size/ chunkSize); i++) {			
			f.ask(loaderSession, new Item<>(i, new byte[chunkSize]), NextItem.class).get();
		}
		
		f.ask(receiver, new Wait(2), Waited.class).get();		
		
		f.ask(receiver, new GetRecording(), Recording.class).get()
			.assertNext(byte[].class, bytes -> {
				assertEquals(chunkSize, bytes.length);
			})
			.assertNext(byte[].class, bytes -> {
				assertEquals(chunkSize, bytes.length);
			})
			.assertNotHasNext();
		
		f.ask(receiver, new Clear(), Cleared.class).get();		
		f.ask(loader, new Clear(), Cleared.class).get();
		
		ActorRef deadWatch = actorSystem.actorOf(AnyRecorder.props());
		f.ask(deadWatch, new Watch(loaderSession), Watching.class).get();
		
		loaderSession.tell(new End(), ActorRef.noSender());

		f.ask(receiver, new Wait(1), Waited.class).get();
		f.ask(receiver, new GetRecording(), Recording.class).get()			
			.assertNext(End.class)
			.assertNotHasNext();
		
		f.ask(jobContext, new Wait(1), Waited.class).get();
		f.ask(jobContext, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(UpdateJobState.class, updateJobState -> {
				assertEquals(JobState.SUCCEEDED, updateJobState.getState());
			})
			.assertNotHasNext();
		
		f.ask(loader, new Wait(1), Waited.class).get();
		f.ask(loader, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(SessionFinished.class)
			.assertNotHasNext();
		
		f.ask(deadWatch, new Wait(1), Waited.class).get();		
		f.ask(deadWatch, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Terminated.class, terminated -> {
				assertEquals(loaderSession, terminated.getActor());
			});
	}	
}
