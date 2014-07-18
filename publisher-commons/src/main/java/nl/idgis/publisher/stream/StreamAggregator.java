package nl.idgis.publisher.stream;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.protocol.Failure;
import nl.idgis.publisher.stream.messages.AggregateStream;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Start;
import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
import akka.japi.Procedure;
import akka.pattern.Patterns;
import akka.util.Timeout;

public final class StreamAggregator<T extends Item, U extends Collection<T>> extends UntypedActor {
	
	protected U collection;
	
	public StreamAggregator(U collection) {
		this.collection = collection;
	}
	
	public static <T extends Item, U extends Collection<T>> Future<U> ask(ActorRefFactory actorRefFactory, ActorRef actorRef, Start startMessage, U collection, long timeout) {
		return ask(actorRefFactory, actorRef, startMessage, collection, new Timeout(timeout, TimeUnit.MILLISECONDS));
	}
	
	public static <T extends Item, U extends Collection<T>> Future<U> ask(ActorRefFactory actorRefFactory, ActorRef actorRef, Start startMessage, U collection, Timeout timeout) {
		ActorRef aggregator = actorRefFactory.actorOf(Props.create(StreamAggregator.class, collection));
		return Patterns.ask(aggregator, new AggregateStream(actorRef, startMessage), timeout).map(new Mapper<Object, U>() {
			
			@Override
			@SuppressWarnings("unchecked")
			public U checkedApply(Object parameter) throws Exception {
				if(parameter instanceof Failure) {
					throw new Exception(((Failure) parameter).getCause());
				}
				
				return (U)parameter;
			}
			
		}, actorRefFactory.dispatcher());
	}
	
	@Override	
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof AggregateStream) {
			AggregateStream ag = (AggregateStream)msg;
			ag.getStreamProvider().tell(ag.getStart(), getSelf());
			getContext().become(consuming(getSender()));
		} else {
			unhandled(msg);
		}
	}
	
	private Procedure<Object> consuming(final ActorRef sender) {
		return new Procedure<Object>() {
			
			@Override
			@SuppressWarnings("unchecked")
			public void apply(Object msg) throws Exception {
				if(msg instanceof Item) {
					collection.add((T)msg);
					getSender().tell(new NextItem(), getSelf());
				} else if(msg instanceof End) {
					sender.tell(collection, getSelf());
					getContext().stop(getSelf());
				} else if(msg instanceof Failure) {
					sender.tell(msg, getSelf());
					getContext().stop(getSelf());
				} else {
					unhandled(msg);
				}
			}
		};
	}
}
