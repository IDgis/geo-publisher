package nl.idgis.publisher.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;

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
		
		private Collector4(Collector3<T, U, V> parent, CompletableFuture<W> future) {
			super(future);
			
			this.parent = parent;
		}
		
		public <R> CompletableFuture<R> thenCompose(final Function4<T, U, V, W, CompletableFuture<R>> f) {
			return future.thenCompose(w -> parent.thenCompose((t, u, v) -> f.apply(t, u, v, w)));
		}
		
		public <R> CompletableFuture<R> thenApply(final Function4<T, U, V, W, R> f) {
			return future.thenCompose(w -> parent.thenApply((t, u, v) -> f.apply(t, u, v, w)));
		}
	}
	
	public class Collector3<T, U, V> extends Collector<V> {
		
		private final Collector2<T, U> parent;
		
		private Collector3(Collector2<T, U> parent, CompletableFuture<V> future) {
			super(future);
			
			this.parent = parent;
		}
		
		public <R> CompletableFuture<R> thenCompose(final Function3<T, U, V, CompletableFuture<R>> f) {
			return future.thenCompose(v -> parent.thenCompose((t, u) -> f.apply(t, u, v)));
		}
		
		public <R> CompletableFuture<R> thenApply(final Function3<T, U, V, R> f) {
			return future.thenCompose(v -> parent.thenApply((t, u) -> f.apply(t, u, v)));
		}
		
		public <W> Collector4<T, U, V, W> collect(CompletableFuture<W> future) {
			return new Collector4<>(this, future);
		}
	}
	
	public class Collector2<T, U> extends Collector<U> {
		
		private final Collector1<T> parent;		
		
		private Collector2(Collector1<T> parent, CompletableFuture<U> future) {
			super(future);
			
			this.parent = parent;			
		}
		
		public <R> CompletableFuture<R> thenCompose(final BiFunction<T, U, CompletableFuture<R>> f) {			
			return future.thenCompose(u -> parent.thenCompose(t -> f.apply(t, u)));					
		}
		
		public <R> CompletableFuture<R> thenApply(final BiFunction<T, U, R> f) {			
			return future.thenCompose(u -> parent.thenApply(t -> f.apply(t, u)));
		}
		
		public <V> Collector3<T, U, V> collect(CompletableFuture<V> future) {
			return new Collector3<>(this, future);
		}
	}
	
	public class Collector1<T> extends Collector<T> {
		
		private Collector1(CompletableFuture<T> future) {
			super(future);
		}
		
		public <R> CompletableFuture<R> thenCompose(final Function<T, CompletableFuture<R>> f) {
			return future.thenCompose(f);
		}
		
		public <R> CompletableFuture<R> thenApply(final Function<T, R> f) {
			return future.thenApply(f);
		}
		
		public <U> Collector2<T, U> collect(CompletableFuture<U> future) {
			return new Collector2<>(this, future);
		}
	}
	
	private abstract static class Collector<T> {
		
		protected final CompletableFuture<T> future;
		
		private Collector(CompletableFuture<T> future) {
			this.future = future;
		} 
	}
	
	public <T> Collector1<T> collect(CompletableFuture<T> future) {
		return new Collector1<>(future);
	}
	
	public CompletableFuture<Object> ask(ActorRef actor, Object message) {
		return ask(actor, message, timeout);
	}
	
	public CompletableFuture<Object> ask(ActorRef actor, Object message, long timeout) {
		return ask(actor, message, Timeout.apply(timeout));
	}
	
	public CompletableFuture<Object> ask(ActorRef actor, Object message, Timeout timeout) {
		return toCompletableFuture(Patterns.ask(actor, message, timeout));
	}
	
	public CompletableFuture<Object> ask(ActorSelection selection, Object message, Timeout timeout) {
		return toCompletableFuture(Patterns.ask(selection, message, timeout));
	}
	
	public <T> CompletableFuture<T> ask(ActorRef actor, Object message, Class<T> targetClass, Timeout timeout) {		 
		return cast(ask(actor, message, timeout), targetClass);
	}
	
	public <T> CompletableFuture<T> ask(ActorRef actor, Object message, Class<T> targetClass, long timeout) {		 
		return cast(ask(actor, message, Timeout.apply(timeout)), targetClass);
	}
	
	public <T> CompletableFuture<T> ask(ActorRef actor, Object message, Class<T> targetClass) {		 
		return cast(ask(actor, message, timeout), targetClass);
	}
	
	public <T> CompletableFuture<T> ask(ActorSelection selection, Object message, Class<T> targetClass, Timeout timeout) {		 
		return cast(ask(selection, message, timeout), targetClass);
	}
	
	public <T> CompletableFuture<T> ask(ActorSelection selection, Object message, Class<T> targetClass, long timeout) {		 
		return cast(ask(selection, message, Timeout.apply(timeout)), targetClass);
	}
	
	public <T> CompletableFuture<T> ask(ActorSelection selection, Object message, Class<T> targetClass) {		 
		return cast(ask(selection, message, timeout), targetClass);
	}
	
	public <U, T extends U> CompletableFuture<T> cast(CompletableFuture<U> future, Class<T> targetClass) {
		return future.thenCompose(u -> 
			targetClass.isInstance(u) 
				? successful(targetClass.cast(u)) 
				: failed(new WrongResultException(u, targetClass)));
	}
	
	public <T> CompletableFuture<Iterable<T>> sequence(Iterable<CompletableFuture<T>> sequence) {
		Iterator<CompletableFuture<T>> i = sequence.iterator();
		
		if(i.hasNext()) {
			Promise<Iterable<T>> promise = Futures.promise();
			
			i.next().whenComplete(new BiConsumer<T, Throwable>() {
				
				ArrayList<T> result = new ArrayList<>();

				@Override
				public void accept(T t, Throwable throwable) {
					if(throwable == null) {
						result.add(t);
						
						if(i.hasNext()) {
							i.next().whenComplete(this);
						} else {
							promise.success(result);
						}
					} else {
						promise.failure(throwable);
					}
				}
			});
			
			return toCompletableFuture(promise.future());
		} else {
			return successful(Collections.emptyList());
		}
	}
	
	public <K, V> CompletableFuture<Map<K, V>> map(Map<K, CompletableFuture<V>> map) {
		List<K> keys = new ArrayList<>();
		List<CompletableFuture<V>> futures = new ArrayList<>();
		
		for(Map.Entry<K, CompletableFuture<V>> entry : map.entrySet()) {
			keys.add(entry.getKey());
			futures.add(entry.getValue());
		}
		
		return sequence(futures).thenApply(values -> {
			Map<K, V> retval = new HashMap<K, V>();
			
			Iterator<K> keyItr = keys.iterator();
			Iterator<V> valueItr = values.iterator();
			
			while(keyItr.hasNext()) {
				retval.put(keyItr.next(), valueItr.next());
			}
			
			return retval;
		});
	}
	
	public <T> CompletableFuture<T> successful(T t) {
		return CompletableFuture.completedFuture(t);
	}
	
	public <T> CompletableFuture<T> failed(Throwable t) {
		CompletableFuture<T> completableFuture = new CompletableFuture<>();
		completableFuture.completeExceptionally(t);
		
		return completableFuture;
	}
	
	public <T> CompletableFuture<T> toCompletableFuture(Future<T> future) {
		CompletableFuture<T> completableFuture = new CompletableFuture<>();
		
		future.onComplete(new OnComplete<T>() {

			@Override
			public void onComplete(Throwable throwable, T t) throws Throwable {
				if(throwable == null) {
					completableFuture.complete(t);
				} else {
					completableFuture.completeExceptionally(throwable);
				}
			}
			
		}, executionContext);
		
		return completableFuture;
	}
}
