package nl.idgis.publisher.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.util.Timeout;

import nl.idgis.publisher.function.Function1;
import nl.idgis.publisher.function.Function2;
import nl.idgis.publisher.function.Function3;
import nl.idgis.publisher.function.Function4;

import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Promise;

public class FutureUtils {
	
	private final ExecutionContext executionContext;
	
	private final Timeout timeout;
	
	public FutureUtils(ExecutionContext executionContext) {
		this(executionContext, 15000);
	}
	
	public FutureUtils(ExecutionContext executionContext, long timeout) {
		this(executionContext, Timeout.longToTimeout(timeout));
	}
	
	public FutureUtils(ExecutionContext executionContext, Timeout timeout) {
		this.executionContext = executionContext;
		this.timeout = timeout;
	}
	
	public class Collector4<T, U, V, W> extends Collector<W> {
		
		private final Collector3<T, U, V> parent;
		
		private Collector4(Collector3<T, U, V> parent, SmartFuture<W> future) {
			super(future);
			
			this.parent = parent;
		}
		
		public <R> SmartFuture<R> flatMap(final Function4<T, U, V, W, SmartFuture<R>> f) {
			return future.flatMap(w -> parent.flatMap((T t, U u, V v) -> f.apply(t, u, v, w)));
		}
		
		public <R> SmartFuture<R> map(final Function4<T, U, V, W, R> f) {
			return future.flatMap(w -> parent.map((T t, U u, V v) -> f.apply(t, u, v, w)));
		}
	}
	
	public class Collector3<T, U, V> extends Collector<V> {
		
		private final Collector2<T, U> parent;
		
		private Collector3(Collector2<T, U> parent, SmartFuture<V> future) {
			super(future);
			
			this.parent = parent;
		}
		
		public <R> SmartFuture<R> flatMap(final Function3<T, U, V, SmartFuture<R>> f) {
			return future.flatMap(v -> parent.flatMap((T t, U u) -> f.apply(t, u, v)));
		}
		
		public <R> SmartFuture<R> map(final Function3<T, U, V, R> f) {
			return future.flatMap(v -> parent.map((T t, U u) -> f.apply(t, u, v)));
		}
		
		public <W> Collector4<T, U, V, W> collect(SmartFuture<W> future) {
			return new Collector4<>(this, future);
		}
	}
	
	public class Collector2<T, U> extends Collector<U> {
		
		private final Collector1<T> parent;		
		
		private Collector2(Collector1<T> parent, SmartFuture<U> future) {
			super(future);
			
			this.parent = parent;			
		}
		
		public <R> SmartFuture<R> flatMap(final Function2<T, U, SmartFuture<R>> f) {			
			return future.flatMap(u -> parent.flatMap(t -> f.apply(t, u)));					
		}
		
		public <R> SmartFuture<R> map(final Function2<T, U, R> f) {			
			return future.flatMap(u -> parent.map(t -> f.apply(t, u)));
		}
		
		public <V> Collector3<T, U, V> collect(SmartFuture<V> future) {
			return new Collector3<>(this, future);
		}
	}
	
	public class Collector1<T> extends Collector<T> {
		
		private Collector1(SmartFuture<T> future) {
			super(future);
		}
		
		public <R> SmartFuture<R> flatMap(final Function1<T, SmartFuture<R>> f) {
			return future.flatMap(f);
		}
		
		public <R> SmartFuture<R> map(final Function1<T, R> f) {
			return future.map(f);
		}
		
		public <U> Collector2<T, U> collect(SmartFuture<U> future) {
			return new Collector2<>(this, future);
		}
	}
	
	private abstract static class Collector<T> {
		
		protected final SmartFuture<T> future;
		
		private Collector(SmartFuture<T> future) {
			this.future = future;
		} 
	}
	
	public <T> Collector1<T> collect(SmartFuture<T> future) {
		return new Collector1<>(future);
	}
	
	public <T, U> SmartFuture<T> cast(SmartFuture<U> future, Class<T> targetClass) {
		return cast(future, targetClass, null);
	}
	
	public <T, U> SmartFuture<T> cast(SmartFuture<U> future, final Class<T> targetClass, final Object context) {
		return future.map(u -> {
			if(targetClass.isInstance(u)) {
				return targetClass.cast(u);
			} else {
				throw new WrongResultException(u, targetClass, context);
			}
		});
	}
	
	public SmartFuture<Object> ask(ActorRef actor, Object message) {
		return ask(actor, message, timeout);
	}
	
	public SmartFuture<Object> ask(ActorRef actor, Object message, long timeout) {
		return ask(actor, message, Timeout.apply(timeout));
	}
	
	public SmartFuture<Object> ask(ActorRef actor, Object message, Timeout timeout) {
		return new SmartFuture<>(Patterns.ask(actor, message, timeout), executionContext);
	}
	
	public SmartFuture<Object> ask(ActorSelection selection, Object message, Timeout timeout) {
		return new SmartFuture<>(Patterns.ask(selection, message, timeout), executionContext);
	}
	
	public <T> SmartFuture<T> ask(ActorRef actor, Object message, Class<T> targetClass, Timeout timeout) {		 
		return cast(ask(actor, message, timeout), targetClass, message);
	}
	
	public <T> SmartFuture<T> ask(ActorRef actor, Object message, Class<T> targetClass, long timeout) {		 
		return cast(ask(actor, message, Timeout.apply(timeout)), targetClass, message);
	}
	
	public <T> SmartFuture<T> ask(ActorRef actor, Object message, Class<T> targetClass) {		 
		return cast(ask(actor, message, timeout), targetClass, message);
	}
	
	public <T> SmartFuture<T> ask(ActorSelection selection, Object message, Class<T> targetClass, Timeout timeout) {		 
		return cast(ask(selection, message, timeout), targetClass, message);
	}
	
	public <T> SmartFuture<T> ask(ActorSelection selection, Object message, Class<T> targetClass, long timeout) {		 
		return cast(ask(selection, message, Timeout.apply(timeout)), targetClass, message);
	}
	
	public <T> SmartFuture<T> ask(ActorSelection selection, Object message, Class<T> targetClass) {		 
		return cast(ask(selection, message, timeout), targetClass, message);
	}
	
	public <T> SmartFuture<Iterable<T>> sequence(Iterable<SmartFuture<T>> sequence) {
		Iterator<SmartFuture<T>> i = sequence.iterator();
		
		if(i.hasNext()) {
			Promise<Iterable<T>> promise = Futures.promise();
			
			i.next().success(new Consumer<T>() {
				
				ArrayList<T> result = new ArrayList<>();

				@Override
				public void accept(T t) {
					result.add(t);
					
					if(i.hasNext()) {
						i.next().success(this);
					} else {
						promise.success(result);
					}
				}
				
			});
			
			return new SmartFuture<>(promise.future(), executionContext);
		} else {
			return new SmartFuture<>(Futures.successful(Collections.emptyList()), executionContext);
		}
	}
	
	public <K, V> SmartFuture<Map<K, V>> map(Map<K, SmartFuture<V>> input) {
		final List<K> keys = new ArrayList<K>();
		List<SmartFuture<V>> values = new ArrayList<SmartFuture<V>>();
		
		for(Map.Entry<K, SmartFuture<V>> entry : input.entrySet()) {
			keys.add(entry.getKey());
			values.add(entry.getValue());
		}
		
		return sequence(values).map(i -> {
			Map<K, V> retval = new HashMap<K, V>();
			
			Iterator<K> keyItr = keys.iterator();
			Iterator<V> valueItr = i.iterator();
			
			while(keyItr.hasNext()) {
				retval.put(keyItr.next(), valueItr.next());
			}
			
			return retval;
		});
	}
	
	public <T> SmartFuture<T> successful(T t) {
		return new SmartFuture<>(Futures.successful(t), executionContext);
	}
	
	public <T> SmartFuture<T> failed(Throwable t) {
		return new SmartFuture<>(Futures.failed(t), executionContext);
	}
	
	public <T> SmartFuture<T> smart(Future<T> future) {
		return new SmartFuture<>(future, executionContext);
	}
}
