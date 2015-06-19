package nl.idgis.publisher.recorder;

import java.util.function.Predicate;

import akka.actor.ActorRef;
import akka.actor.Props;

public class PredicateRecorder extends AnyRecorder {
	
	private Predicate<Object> predicate;
	
	public PredicateRecorder(Predicate<Object> predicate) {
		this.predicate = predicate;
	}

	public static Props props(Predicate<Object> predicate) {
		return Props.create(PredicateRecorder.class, predicate);
	}
	
	protected boolean doRecord(Object msg, ActorRef sender) {
		return predicate.test(msg);
	}
}
