package nl.idgis.publisher.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;

import nl.idgis.publisher.function.Function3;
import nl.idgis.publisher.function.Function4;

import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

/**
 * This utility class can be used to ease the utilization of Scala futures from Java. It removes the 
 * need to provide a {@link Timeout} and an {@link ExecutionContext} to various methods and converts 
 * Scala {@link Future} objects into {@link CompletableFuture} objects in order to be able to use lambda 
 * functions while dealing with futures. This class provides a collector concept that can be used to wrap 
 * multiple futures in order to easily transform the result of these futures into a single value, by providing
 * a single lambda function with a parameter for every future result:
 * 
 * FutureUtils f = new FutureUtils(...);
 * CompletableFuture<?> result = f
 * 		.collect(retrieveFromDatabase(...))
 * 		.collect(downloadFromNetwork(...))
 * 		.collect(f.ask(calculator, new PerformComputation()))
 * 		.thenApply((valueFromDatabase, downloadedItem, computationResult) -> {
 * 			...
 * 
 * 			return ...;
 * 		});
 * 
 */
public class FutureUtils {
	
	private final ActorRefFactory actorRefFactory;
	
	private final Timeout timeout;
	
	/**
	 * Construct a new FutureUtils object with a default ask timeout of 15 seconds.
	 * 
	 * @param actorRefFactory the {@link ActorRefFactory} to use
	 */
	public FutureUtils(ActorRefFactory actorRefFactory) {
		this(actorRefFactory, 15000);
	}
	
	/**
	 * Construct a new FutureUtils object.
	 * 
	 * @param actorRefFactory the {@link ActorRefFactory} to use
	 * @param timeout the ask timeout in milliseconds
	 */
	public FutureUtils(ActorRefFactory actorRefFactory, long timeout) {
		this(actorRefFactory, Timeout.longToTimeout(timeout));
	}
	
	/**
	 * Construct a new FutureUtils object.
	 * 
	 * @param actorRefFactory the {@link ActorRefFactory} to use
	 * @param timeout the ask timeout
	 */
	public FutureUtils(ActorRefFactory actorRefFactory, Timeout timeout) {
		this.actorRefFactory = actorRefFactory;
		this.timeout = timeout;
	}
	
	public class Collector4<T, U, V, W> extends AbstractCollector<W> {
		
		private final Collector3<T, U, V> parent;
		
		private Collector4(Collector3<T, U, V> parent, CompletableFuture<W> future) {
			super(future);
			
			this.parent = parent;
		}
		
		/**
		 * Returns a new CompletionStage that, when the futures wrapped by this collector 
		 * completes normally, is executed with the wrapped futures result as the argument to the supplied function.
		 * 
		 * @param f the function returning a new CompletableFuture
		 * @return
		 */
		public <R> CompletableFuture<R> thenCompose(final Function4<T, U, V, W, CompletableFuture<R>> f) {
			return future.thenCompose(w -> parent.thenCompose((t, u, v) -> f.apply(t, u, v, w)));
		}
		
		/**
		 * Returns a new CompletableFuture that, when the futures wrapped by this collector 
		 * completes normally, is executed with the wrapped futures result as the argument to the supplied function.
		 * 
		 * @param f the function to use to compute the value of the returned CompletableFuture
		 * @return the new CompletableFuture
		 */
		public <R> CompletableFuture<R> thenApply(final Function4<T, U, V, W, R> f) {
			return future.thenCompose(w -> parent.thenApply((t, u, v) -> f.apply(t, u, v, w)));
		}
	}
	
	public class Collector3<T, U, V> extends AbstractCollector<V> {
		
		private final Collector2<T, U> parent;
		
		private Collector3(Collector2<T, U> parent, CompletableFuture<V> future) {
			super(future);
			
			this.parent = parent;
		}
		
		/**
		 * Returns a new CompletionStage that, when the futures wrapped by this collector 
		 * completes normally, is executed with the wrapped futures result as the argument to the supplied function.
		 * 
		 * @param f the function returning a new CompletableFuture
		 * @return
		 */
		public <R> CompletableFuture<R> thenCompose(final Function3<T, U, V, CompletableFuture<R>> f) {
			return future.thenCompose(v -> parent.thenCompose((t, u) -> f.apply(t, u, v)));
		}
		
		/**
		 * Returns a new CompletableFuture that, when the futures wrapped by this collector 
		 * completes normally, is executed with the wrapped futures result as the argument to the supplied function.
		 * 
		 * @param f the function to use to compute the value of the returned CompletableFuture
		 * @return the new CompletableFuture
		 */
		public <R> CompletableFuture<R> thenApply(final Function3<T, U, V, R> f) {
			return future.thenCompose(v -> parent.thenApply((t, u) -> f.apply(t, u, v)));
		}
		
		/**
		 * Wraps an additional future.
		 * 
		 * @param future the future
		 * @return the resulting collector
		 */
		public <W> Collector4<T, U, V, W> collect(CompletableFuture<W> future) {
			return new Collector4<>(this, future);
		}
	}
	
	public class Collector2<T, U> extends AbstractCollector<U> {
		
		private final Collector1<T> parent;		
		
		private Collector2(Collector1<T> parent, CompletableFuture<U> future) {
			super(future);
			
			this.parent = parent;			
		}
		
		/**
		 * Returns a new CompletionStage that, when the futures wrapped by this collector 
		 * completes normally, is executed with the wrapped futures result as the argument to the supplied function.
		 * 
		 * @param f the function returning a new CompletableFuture
		 * @return
		 */
		public <R> CompletableFuture<R> thenCompose(final BiFunction<T, U, CompletableFuture<R>> f) {			
			return future.thenCompose(u -> parent.thenCompose(t -> f.apply(t, u)));					
		}
		
		/**
		 * Returns a new CompletableFuture that, when the futures wrapped by this collector 
		 * completes normally, is executed with the wrapped futures result as the argument to the supplied function.
		 * 
		 * @param f the function to use to compute the value of the returned CompletableFuture
		 * @return the new CompletableFuture
		 */
		public <R> CompletableFuture<R> thenApply(final BiFunction<T, U, R> f) {			
			return future.thenCompose(u -> parent.thenApply(t -> f.apply(t, u)));
		}
		
		/**
		 * Wraps an additional future.
		 * 
		 * @param future the future
		 * @return the resulting collector
		 */
		public <V> Collector3<T, U, V> collect(CompletableFuture<V> future) {
			return new Collector3<>(this, future);
		}
	}
	
	public class Collector1<T> extends AbstractCollector<T> {
		
		private Collector1(CompletableFuture<T> future) {
			super(future);
		}
		
		/**
		 * Returns a new CompletionStage that, when the futures wrapped by this collector 
		 * completes normally, is executed with the wrapped futures result as the argument to the supplied function.
		 * 
		 * @param f the function returning a new CompletableFuture
		 * @return
		 */
		public <R> CompletableFuture<R> thenCompose(final Function<T, CompletableFuture<R>> f) {
			return future.thenCompose(f);
		}
		
		/**
		 * Returns a new CompletableFuture that, when the futures wrapped by this collector 
		 * completes normally, is executed with the wrapped futures result as the argument to the supplied function.
		 * 
		 * @param f the function to use to compute the value of the returned CompletableFuture
		 * @return the new CompletableFuture
		 */
		public <R> CompletableFuture<R> thenApply(final Function<T, R> f) {
			return future.thenApply(f);
		}
		
		/**
		 * Wraps an additional future.
		 * 
		 * @param future the future
		 * @return the resulting collector
		 */
		public <U> Collector2<T, U> collect(CompletableFuture<U> future) {
			return new Collector2<>(this, future);
		}
	}
	
	private abstract static class AbstractCollector<T> {
		
		protected final CompletableFuture<T> future;
		
		private AbstractCollector(CompletableFuture<T> future) {
			this.future = future;
		} 
	}
	
	/**
	 * Wraps a feature into a collector. This is the entry point to the future result collecting 
	 * functionality of {@link FutureUtils}.
	 * 
	 * @param future the future
	 * @return the collector
	 */
	public <T> Collector1<T> collect(CompletableFuture<T> future) {
		return new Collector1<>(future);
	}
	
	/**
	 * Sends a message asynchronously and returns a {@link CompletableFuture}
	 * holding the eventual reply message. It uses the default timeout of 
	 * this {@link FutureUtils} object.
	 * 
	 * @param actor the actor to send the message to
	 * @param message the message
	 * @return the future
	 */	
	public CompletableFuture<Object> ask(ActorRef actor, Object message) {
		return ask(actor, message, timeout);
	}
	
	/**
	 * Sends a message asynchronously and returns a {@link CompletableFuture}
	 * holding the eventual reply message.
	 * 
	 * @param actor the actor to send the message to
	 * @param message the message
	 * @param timeout the timeout
	 * @return the future
	 */
	public CompletableFuture<Object> ask(ActorRef actor, Object message, long timeout) {
		return ask(actor, message, Timeout.apply(timeout));
	}
	
	/**
	 * Sends a message asynchronously and returns a {@link CompletableFuture}
	 * holding the eventual reply message.
	 * 
	 * @param actor the actor to send the message to
	 * @param message the message
	 * @param timeout the timeout
	 * @return the future
	 */
	public CompletableFuture<Object> ask(ActorRef actor, Object message, Timeout timeout) {
		return toCompletableFuture(Patterns.ask(actor, message, timeout));
	}
	
	/**
	 * Sends a message asynchronously and returns a {@link CompletableFuture}
	 * holding the eventual reply message.
	 * 
	 * @param selection the actor(s) to send the message to
	 * @param message the message
	 * @param timeout the timeout
	 * @return the future
	 */
	public CompletableFuture<Object> ask(ActorSelection selection, Object message, Timeout timeout) {
		return toCompletableFuture(Patterns.ask(selection, message, timeout));
	}
	
	/**
	 * Sends a message asynchronously and returns a {@link CompletableFuture}
	 * holding the eventual reply message.
	 * 
	 * @param actor the actor to send the message to
	 * @param message the message
	 * @param targetClass the expected target class
	 * @param timeout the timeout
	 * @return the future
	 */
	public <T> CompletableFuture<T> ask(ActorRef actor, Object message, Class<T> targetClass, Timeout timeout) {		 
		return cast(ask(actor, message, timeout), targetClass);
	}
	
	/**
	 * Sends a message asynchronously and returns a {@link CompletableFuture}
	 * holding the eventual reply message.
	 * 
	 * @param actor the actor to send the message to
	 * @param message the message
	 * @param targetClass the expected target class
	 * @param timeout the timeout
	 * @return the future
	 */
	public <T> CompletableFuture<T> ask(ActorRef actor, Object message, Class<T> targetClass, long timeout) {		 
		return cast(ask(actor, message, Timeout.apply(timeout)), targetClass);
	}
	
	/**
	 * Sends a message asynchronously and returns a {@link CompletableFuture}
	 * holding the eventual reply message. It uses the default timeout of 
	 * this {@link FutureUtils} object.
	 * 
	 * @param actor the actor to send the message to
	 * @param message the message
	 * @param targetClass the expected target class
	 * @return the future
	 */
	public <T> CompletableFuture<T> ask(ActorRef actor, Object message, Class<T> targetClass) {		 
		return cast(ask(actor, message, timeout), targetClass);
	}
	
	/**
	 * Sends a message asynchronously and returns a {@link CompletableFuture}
	 * holding the eventual reply message.
	 * 
	 * @param selection the actor(s) to send the message to
	 * @param message the message
	 * @param targetClass the expected target class
	 * @param timeout the timeout
	 * @return the future
	 */
	public <T> CompletableFuture<T> ask(ActorSelection selection, Object message, Class<T> targetClass, Timeout timeout) {		 
		return cast(ask(selection, message, timeout), targetClass);
	}
	
	/**
	 * Sends a message asynchronously and returns a {@link CompletableFuture}
	 * holding the eventual reply message.
	 * 
	 * @param selection the actor(s) to send the message to
	 * @param message the message
	 * @param targetClass the expected target class
	 * @param timeout the timeout
	 * @return the future
	 */
	public <T> CompletableFuture<T> ask(ActorSelection selection, Object message, Class<T> targetClass, long timeout) {		 
		return cast(ask(selection, message, Timeout.apply(timeout)), targetClass);
	}
	
	/**
	 * Sends a message asynchronously and returns a {@link CompletableFuture}
	 * holding the eventual reply message. It uses the default timeout of 
	 * this {@link FutureUtils} object.
	 * 
	 * @param selection the actor(s) to send the message to
	 * @param message the message
	 * @param targetClass the expected target class
	 * @return the future
	 */
	public <T> CompletableFuture<T> ask(ActorSelection selection, Object message, Class<T> targetClass) {
		return cast(ask(selection, message, timeout), targetClass);
	}
	
	/**
	 * Converts a {@link CompletableFuture} to another {@link CompletableFuture} by performing a type 
	 * cast. In case the cast fails the resulting future completes with {@link WrongResultException}.
	 * 
	 * @param future the future to cast
	 * @param targetClass the class to cast to
	 * @return the {@link CompletableFuture} for the casted result  
	 */
	public <U, T extends U> CompletableFuture<T> cast(CompletableFuture<U> future, Class<T> targetClass) {
		return future.thenCompose(u -> 
			targetClass.isInstance(u) 
				? successful(targetClass.cast(u)) 
				: failed(new WrongResultException(u, targetClass)));
	}
	
	/**
	 * Transforms an {@link Iterable} with {@link Supplier} objects generating {@link CompletableFuture} objects 
	 * into a single {@link CompletableFuture} providing an {@link List} with resulting values.
	 * 
	 * @param sequence the future suppliers
	 * @return a single future with the results
	 */
	public <T> CompletableFuture<List<T>> supplierSequence(Iterable<Supplier<CompletableFuture<T>>> sequence) {
		Iterator<Supplier<CompletableFuture<T>>> i = sequence.iterator();
		
		if(i.hasNext()) {
			CompletableFuture<List<T>> completableFuture = new CompletableFuture<>();
			
			i.next().get().whenComplete(new BiConsumer<T, Throwable>() {
				
				ArrayList<T> result = new ArrayList<>();

				@Override
				public void accept(T t, Throwable throwable) {
					if(throwable == null) {
						result.add(t);
						
						if(i.hasNext()) {
							i.next().get().whenComplete(this);
						} else {
							completableFuture.complete(result);							
						}
					} else {
						completableFuture.completeExceptionally(throwable);						
					}
				}
			});
			
			return completableFuture;
		} else {
			return successful(Collections.emptyList());
		}
	}
	
	/**
	 * Transforms an {@link Iterable} with {@link CompletableFuture} objects into a single {@link CompletableFuture} 
	 * providing an {@link List} with resulting values.
	 * 
	 * @param sequence the futures
	 * @return a single future with the results
	 */
	public <T> CompletableFuture<List<T>> sequence(final Iterable<CompletableFuture<T>> sequence) {
		return supplierSequence(new Iterable<Supplier<CompletableFuture<T>>>() {

			@Override
			public Iterator<Supplier<CompletableFuture<T>>> iterator() {
				
				return new Iterator<Supplier<CompletableFuture<T>>>() {
					
					Iterator<CompletableFuture<T>> i = sequence.iterator();

					@Override
					public boolean hasNext() {
						return i.hasNext();
					}

					@Override
					public Supplier<CompletableFuture<T>> next() {
						return () -> i.next();
					}
					
				};
			}
			
		});
	}
	
	/**
	 * Transforms a {@link Map} with {@link CompletableFuture} objects as values into a {@link CompletableFuture} 
	 * providing a {@link Map} with resulting values.
	 * 
	 * @param map the futures
	 * @return a future with a results map
	 */
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
	
	/**
	 * Creates an already completed {@link CompletableFuture} with the specified value.
	 * 
	 * @param t the value
	 * @return the resulting {@link CompletableFuture}
	 */
	public <T> CompletableFuture<T> successful(T t) {
		return CompletableFuture.completedFuture(t);
	}
	
	/**
	 * Creates an already completed {@link CompletableFuture} with the specified exception.
	 * 
	 * @param t the exception
	 * @return the resulting {@link CompletableFuture}
	 */
	public <T> CompletableFuture<T> failed(Throwable t) {
		CompletableFuture<T> completableFuture = new CompletableFuture<>();
		completableFuture.completeExceptionally(t);
		
		return completableFuture;
	}
	
	/**
	 * Converts a Scala {@link Future} object into an equivalent {@link CompletableFuture}
	 * 
	 * @param future the Scala {@link Future} object to convert
	 * @return the resulting {@link CompletableFuture}
	 */
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
			
		}, actorRefFactory.dispatcher());
		
		return completableFuture;
	}
	
	/**
	 * Converts a {@link CompletableFuture} with an {@link AskResponse} to another {@link CompletableFuture} 
	 * with another {@link AskResponse} by performing a type cast. In case the cast fails the resulting future 
	 * completes with {@link WrongResultException}.
	 * 
	 * @param future the future to cast
	 * @param targetClass the class to cast to
	 * @return the {@link CompletableFuture} for the casted result
	 */
	public <U, T extends U> CompletableFuture<AskResponse<T>> castWithSender(CompletableFuture<AskResponse<U>> future, Class<T> targetClass) {
		return future.thenCompose(u -> {
			U msg = u.getMessage();
			return targetClass.isInstance(msg) 
				? successful(new AskResponse<>(targetClass.cast(msg), u.getSender())) 
				: failed(new WrongResultException(msg, targetClass));
		});
	}
	
	public <T> CompletableFuture<AskResponse<Object>> askWithSender(ActorRef actor, Object message) {
		return toCompletableFuture(Ask.askWithSender(actorRefFactory, actor, message, timeout));
	}
	
	public <T> CompletableFuture<AskResponse<T>> askWithSender(ActorRef actor, Object message, Class<T> targetClass) {
		return castWithSender(askWithSender(actor, message), targetClass);
	}
	
	/**
	 * Returns a {@link Collector} that aggregates {@link CompletableFuture} objects into a 
	 * single {@link CompletableFuture} containing a {@link Stream}.
	 * 
	 * @return the {@link Collector}
	 */
	public <T> Collector<CompletableFuture<T>, ?, CompletableFuture<Stream<T>>> collect() {
		return Collector.of(
			() -> new ArrayList<CompletableFuture<T>>(), 
			(ArrayList<CompletableFuture<T>> list, CompletableFuture<T> future) -> { 
				list.add(future); 
			}, 
			(ArrayList<CompletableFuture<T>> left, ArrayList<CompletableFuture<T>> right) -> {
				ArrayList<CompletableFuture<T>> retval = new ArrayList<>();
				retval.addAll(left);
				retval.addAll(right);
				
				return retval;
			},
			(ArrayList<CompletableFuture<T>> list) -> sequence(list).thenApply(Collection::stream));
	}
	
	@SafeVarargs
	public final <T> CompletableFuture<Stream<T>> concat(CompletableFuture<Stream<T>>... futures) {
		return Stream.of(futures)
			.collect(collect())
			.thenApply(result ->
				result.flatMap(Function.identity()));
	}
}
