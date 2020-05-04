package nl.idgis.publisher.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.recorder.Recorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.RecordedMessage;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.UniqueNameGenerator;

public class AsyncDatabaseHelperTest {
	
	public static class TransactionMock extends UntypedActor {
		
		private final ActorRef recorder;
		
		private final Throwable commitFailureCause, rollbackFailureCause;
		
		public TransactionMock(ActorRef recorder, Throwable commitFailureCause, Throwable rollbackFailureCause) {
			this.recorder = recorder;
			this.commitFailureCause = commitFailureCause;
			this.rollbackFailureCause = rollbackFailureCause;
		}
		
		public static Props props(ActorRef recorder, Throwable commitFailureCause, Throwable rollbackFailureCause) {
			return Props.create(TransactionMock.class, recorder, commitFailureCause, rollbackFailureCause);
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			recorder.tell(new RecordedMessage(getSelf(), getSender(), msg), getSelf());
			
			if(msg instanceof Commit) {
				if(commitFailureCause != null) {
					getSender().tell(new Failure(commitFailureCause), getSelf());
				} else {
					getSender().tell(new Ack(), getSelf());
				}
				getContext().stop(getSelf());
			} else if(msg instanceof Rollback) {
				if(rollbackFailureCause != null) {
					getSender().tell(new Failure(rollbackFailureCause), getSelf());
				} else {
					getSender().tell(new Ack(), getSelf());
				}
			} else {
				unhandled(msg);
			}
		}
		
	}
	
	private static class DisableTransactions implements Serializable {

		private static final long serialVersionUID = -436706792667200834L;				
	}
	
	private static class CommitFailure implements Serializable {		
		
		private static final long serialVersionUID = -4303250152957731647L;
		
		private final Throwable cause;
		
		CommitFailure(Throwable cause) {
			this.cause = cause;
		}
		
		Throwable getCause() {
			return cause;
		}
	}
	
	private static class RollbackFailure implements Serializable {		
		
		private static final long serialVersionUID = -193627076505067926L;
		
		private final Throwable cause;
		
		RollbackFailure(Throwable cause) {
			this.cause = cause;
		}
		
		Throwable getCause() {
			return cause;
		}
	}
	
	public static class DatabaseMock extends UntypedActor {
		
		private final static UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
		
		private final ActorRef recorder;
		
		private boolean transactionsDisabled = false;
		
		private Throwable commitFailureCause = null, rollbackFailureCause = null;
		
		public DatabaseMock(ActorRef recorder) {
			this.recorder = recorder;
		}
		
		public static Props props(ActorRef recorder) {
			return Props.create(DatabaseMock.class, recorder);
		} 

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof CommitFailure) {
				commitFailureCause = ((CommitFailure)msg).getCause();
				
				getSender().tell(new Ack(), getSelf());
			} else if(msg instanceof RollbackFailure) {
				rollbackFailureCause = ((RollbackFailure)msg).getCause();
				
				getSender().tell(new Ack(), getSelf());
			} if(msg instanceof DisableTransactions) {
				transactionsDisabled = true;
				
				getSender().tell(new Ack(), getSelf());
			} else if(msg instanceof StartTransaction && !transactionsDisabled) {
				getSender().tell(new TransactionCreated(
					getContext().actorOf(
						TransactionMock.props(recorder, commitFailureCause, rollbackFailureCause), 
						nameGenerator.getName(TransactionMock.class))), 
						
					getSelf());
			} else {
				unhandled(msg);
			}
		}
		
	}
	
	FutureUtils f;
	
	AsyncDatabaseHelper db;
	
	ActorRef database, recorder;
	
	@Before
	public void setup() {
		ActorSystem actorSystem = ActorSystem.create();
		
		recorder = actorSystem.actorOf(Recorder.props(), "recorder");
		database = actorSystem.actorOf(DatabaseMock.props(recorder), "database");
		
		LoggingAdapter log = Logging.getLogger(actorSystem, this);
		
		f = new FutureUtils(actorSystem, Timeout.apply(1, TimeUnit.SECONDS));
		db = new AsyncDatabaseHelper(database, getClass().getName(), f, log);
	}
	
	private Recording playRecording() throws Exception {
		return f.ask(recorder, new GetRecording(), Recording.class).get();
	}
	
	@Test
	public void testTransaction() throws Exception {		
		assertNotNull(db.transaction().get());
	}

	@Test
	public void testTransactional() throws Exception {
		assertEquals("Hello, world!", db.transactional(tx -> f.successful("Hello, world!")).get());
		
		playRecording()
			.assertHasNext()
			.assertNext(Commit.class)
			.assertNotHasNext();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testTransactionalHandlerException() throws Throwable {
		CompletableFuture<Object> future = db.transactional(tx -> {
			throw new IllegalArgumentException();
		});
		
		try {
			future.get(1, TimeUnit.SECONDS);
		} catch(ExecutionException e) {
			
			playRecording()
				.assertHasNext()
				.assertNext(Rollback.class)
				.assertNotHasNext();
			
			throw e.getCause();
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testTransactionalResultException() throws Throwable {
		CompletableFuture<Object> future = db.transactional(tx -> f.failed(new IllegalArgumentException()));
		
		try {
			future.get(1, TimeUnit.SECONDS);
		} catch(ExecutionException e) {
			
			playRecording()
				.assertHasNext()
				.assertNext(Rollback.class)
				.assertNotHasNext();
			
			throw e.getCause();
		}
	}
	
	@Test(expected=AskTimeoutException.class)
	public void testTransactionFailure() throws Throwable {
		f.ask(database, new DisableTransactions(), Ack.class);
		
		try {
			db.transaction().get(2, TimeUnit.SECONDS);
		} catch(ExecutionException e) {
			throw e.getCause();
		}
	}
	
	@Test(expected=AskTimeoutException.class)
	public void testTransactionalFailure() throws Throwable {
		f.ask(database, new DisableTransactions(), Ack.class);
		
		try {
			db.transactional(tx -> f.successful("Hello, world!"))
				.get(2, TimeUnit.SECONDS);
		} catch(ExecutionException e) {
			throw e.getCause();
		}
	}

	@Test(expected=IllegalStateException.class)
	public void testTransactionalCommitFailure() throws Throwable {
		f.ask(database, new CommitFailure(new IllegalStateException()), Ack.class);
		
		try {
			db.transactional(tx -> f.successful("Hello, world!")).get(1, TimeUnit.SECONDS);
		} catch(ExecutionException e) {
			playRecording()
				.assertHasNext()
				.assertNext(Commit.class)
				.assertNotHasNext();
			
			throw e.getCause();
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testTransactionalRollbackFailure() throws Throwable {
		f.ask(database, new RollbackFailure(new IllegalStateException()), Ack.class);
		
		try {
			db.transactional(tx -> {
				throw new IllegalArgumentException();
			}).get(1, TimeUnit.SECONDS);
		} catch(ExecutionException e) {
			playRecording()
				.assertHasNext()
				.assertNext(Rollback.class)
				.assertNotHasNext();
			
			// should contain the exception raised by the transaction handler
			// and not the rollback exception.			
			throw e.getCause(); 
		}
	}
}
