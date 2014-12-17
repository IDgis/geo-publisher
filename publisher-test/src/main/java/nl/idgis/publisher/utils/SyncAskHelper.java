package nl.idgis.publisher.utils;

import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSelection;
import akka.util.Timeout;

import nl.idgis.publisher.utils.Ask.Response;

public class SyncAskHelper {
	
	private final ActorRefFactory refFactory;
	
	private final Timeout askTimeout;
	
	private final Duration awaitDuration;
	
	private ActorRef sender;
	
	public SyncAskHelper(ActorRefFactory refFactory) {
		this(refFactory, Timeout.apply(1, TimeUnit.SECONDS));
	}
	
	public SyncAskHelper(ActorRefFactory refFactory, Timeout askTimeout) {
		this(refFactory, askTimeout, askTimeout.duration());
	}

	public SyncAskHelper(ActorRefFactory refFactory, Timeout askTimeout, Duration awaitDuration) {
		this.refFactory = refFactory;
		this.askTimeout = askTimeout;
		this.awaitDuration = awaitDuration;
	}
	
	public Response askResponse(ActorRef actorRef, Object msg) throws Exception {
		Response response = SyncAsk.askResponse(refFactory, actorRef, msg, askTimeout, awaitDuration);
		sender = response.getSender();
		return response;
	}
	
	public Response askResponse(ActorSelection actorSelection, Object msg) throws Exception {
		Response response = SyncAsk.askResponse(refFactory, actorSelection, msg, askTimeout, awaitDuration);
		sender = response.getSender();
		return response;
	}
	
	public Object ask(ActorRef actorRef, Object msg) throws Exception {
		return ask(actorRef, msg, Object.class);
	}
	
	public <T> T ask(ActorRef actorRef, Object msg, Class<T> expected) throws Exception {
		return SyncAsk.result(expected, askResponse(actorRef, msg).getMessage());
	}
	
	public Object ask(ActorSelection actorSelection, Object msg) throws Exception {
		return ask(actorSelection, msg, Object.class);
	}
	
	public <T> T ask(ActorSelection actorSelection, Object msg, Class<T> expected) throws Exception {
		return SyncAsk.result(expected, askResponse(actorSelection, msg).getMessage());
	}
	
	public Response askSenderResponse(Object msg) throws Exception {
		return askResponse(sender, msg);
	}
	
	public <T> T askSender(Object msg, Class<T> expected) throws Exception {
		return ask(sender, msg, expected);
	}
}
