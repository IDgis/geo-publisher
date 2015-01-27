package nl.idgis.publisher.loader;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.InsertRecord;
import nl.idgis.publisher.database.messages.Rollback;

import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
import nl.idgis.publisher.loader.messages.SessionFinished;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
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
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;

public class LoaderSessionTest {
	
	ActorSystem actorSystem;
	
	ActorRef transaction, loader, loaderSession, jobContext;
	
	FutureUtils f;
	
	List<Column> columns;
	
	public static class LoaderRecorder extends AnyRecorder {
		
		public static Props props() {
			return Props.create(LoaderRecorder.class);
		}

		@Override
		protected void onRecord(Object msg, ActorRef sender) {
			if(msg instanceof SessionFinished) {
				sender.tell(new Ack(), getSelf());
			}
		}
	}

	@Before
	public void actorSystem() {
		actorSystem = ActorSystem.create();
		
		transaction = actorSystem.actorOf(AnyAckRecorder.props(new Ack()));
		
		loader = actorSystem.actorOf(LoaderRecorder.props());
		
		jobContext = actorSystem.actorOf(AnyAckRecorder.props(new Ack()));
		
		columns = new ArrayList<>();
		
		for(int i = 0; i < 5; i++) {
			columns.add(new Column("column" + i, Type.NUMERIC));
		}
		
		ImportJobInfo importJob = new ImportJobInfo(0, "categoryId", "dataSourceId", "sourceDatasetId", 
				"datasetId", "datasetName", null /* filterCondition */, columns, columns, Collections.emptyList());

		loaderSession = actorSystem.actorOf(LoaderSession.props(loader, importJob, null /* filterEvaluator */, transaction, jobContext));
		
		f = new FutureUtils(actorSystem.dispatcher());
	}
	
	@Test
	public void testSuccessfull() throws Exception {
		ActorRef initiator = actorSystem.actorOf(AnyRecorder.props());
		
		final int numberOfRecords = 10;
		
		f.ask(loaderSession, new StartImport(initiator, numberOfRecords), Ack.class).get();
		
		f.ask(initiator, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Ack.class)
			.assertNotHasNext();
		
		f.ask(loader, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Progress.class, progress -> {
				assertEquals(0, progress.getCount());
				assertEquals(numberOfRecords, progress.getTotalCount());
			})
			.assertNotHasNext();
		
		final int recordsSize = 5;		
		for(int i = 0; i < (numberOfRecords / recordsSize); i++) {
			
			List<Record> recordList = new ArrayList<>();
			for(int j = 0; j < recordsSize; j++) {
				
				List<Object> values = new ArrayList<>();
				for(int k = 0; k < columns.size(); k++) {
					values.add(k);
				}
				
				recordList.add(new Record(values));
			}
			
			f.ask(loaderSession, new Records(recordList), NextItem.class).get();
		}
		
		f.ask(transaction, new Wait(numberOfRecords), Waited.class).get();		
		
		Recording insertRecording = f.ask(transaction, new GetRecording(), Recording.class).get();
		for(int i = 0; i < numberOfRecords; i++) {
			insertRecording
				.assertHasNext()
				.assertNext(InsertRecord.class);
		}
		
		insertRecording.assertNotHasNext();
		
		f.ask(transaction, new Clear(), Cleared.class).get();		
		f.ask(loader, new Clear(), Cleared.class).get();
		
		ActorRef deadWatch = actorSystem.actorOf(AnyRecorder.props());
		f.ask(deadWatch, new Watch(loaderSession), Watching.class).get();
		
		loaderSession.tell(new End(), ActorRef.noSender());
		
		f.ask(transaction, new Wait(1), Waited.class).get();
		f.ask(transaction, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Commit.class)
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
	
	@Test
	public void testFailure() throws Exception {
		ActorRef initiator = actorSystem.actorOf(AnyRecorder.props());
		
		f.ask(loaderSession, new StartImport(initiator, 100), Ack.class).get();
		
		List<Object> values = new ArrayList<Object>();
		for(int i = 0; i < columns.size(); i++) {
			values.add(i);
		}
		
		f.ask(loaderSession, new Records(Arrays.asList(new Record(values))), NextItem.class).get();
		
		ActorRef deadWatch = actorSystem.actorOf(AnyRecorder.props());
		f.ask(deadWatch, new Watch(loaderSession), Watching.class).get();
		
		f.ask(transaction, new Clear(), Cleared.class);
		f.ask(jobContext, new Clear(), Cleared.class);
		f.ask(loader, new Clear(), Cleared.class);
		loaderSession.tell(new Failure(new IllegalStateException()), ActorRef.noSender());
		
		f.ask(transaction, new Wait(1), Waited.class).get();
		f.ask(transaction, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Rollback.class)
			.assertNotHasNext();
		
		f.ask(jobContext, new Wait(1), Waited.class).get();
		f.ask(jobContext, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(UpdateJobState.class, updateJobState -> {
				assertEquals(JobState.FAILED, updateJobState.getState());
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
				assertEquals(loaderSession, terminated.actor());
			})
			.assertNotHasNext();
	}
}
