package nl.idgis.publisher.utils;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.dispatch.Mapper;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import akka.util.Timeout;

import scala.Function1;
import scala.Function2;
import scala.Function3;
import scala.Function4;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction1;
import scala.runtime.AbstractFunction2;
import scala.runtime.AbstractFunction3;

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
		
		private Collector4(Collector3<T, U, V> parent, Future<W> future) {
			super(future);
			
			this.parent = parent;
		}
		
		public Result result(final Function4<T, U, V, W, Void> f) {
			return new Result(future, 
				parent.result(new AbstractFunction3<T, U, V, Void>() {
	
					@Override
					public Void apply(final T t, final U u, final V v) {
						future.onSuccess(new OnSuccess<W>() {
	
							@Override
							public void onSuccess(W w) throws Throwable {
								f.apply(t,  u,  v, w);
							}
							
						}, executionContext);
						
						return null;
					}
					
				}));
		}
	}
	
	public class Collector3<T, U, V> extends Collector<V> {
		
		private final Collector2<T, U> parent;
		
		private Collector3(Collector2<T, U> parent, Future<V> future) {
			super(future);
			
			this.parent = parent;
		}
		
		public Result result(final Function3<T, U, V, Void> f) {
			return new Result(future,
				parent.result(new AbstractFunction2<T, U, Void>() {
	
					@Override
					public Void apply(final T t, final U u) {
						future.onSuccess(new OnSuccess<V>() {
	
							@Override
							public void onSuccess(V v) throws Throwable {
								f.apply(t,  u,  v);
							}
							
						}, executionContext);
						
						return null;
					}
					
				}));
		}
		
		public <W> Collector4<T, U, V, W> collect(Future<W> future) {
			return new Collector4<>(this, future);
		}
	}
	
	public class Collector2<T, U> extends Collector<U> {
		
		private final Collector1<T> parent;		
		
		private Collector2(Collector1<T> parent, Future<U> future) {
			super(future);
			
			this.parent = parent;			
		}
		
		public Result result(final Function2<T, U, Void> f) {			
			return new Result(future,
				parent.result(new AbstractFunction1<T, Void>() {
	
					@Override
					public Void apply(final T t) { 
						future.onSuccess(new OnSuccess<U>() {
	
							@Override
							public void onSuccess(U u) throws Throwable {
								f.apply(t, u);
							}
							
						}, executionContext);
						
						return null;
					}
					
				}));
		}
		
		public <V> Collector3<T, U, V> collect(Future<V> future) {
			return new Collector3<>(this, future);
		}
	}
	
	public class Collector1<T> extends Collector<T> {
		
		private Collector1(Future<T> future) {
			super(future);
		}
		
		public Result result(final Function1<T, Void> f) {
			future.onSuccess(new OnSuccess<T>() {

				@Override
				public void onSuccess(T t) throws Throwable {
					f.apply(t);
				}
				
			}, executionContext);
			
			return new Result(future);
		}
		
		public <U> Collector2<T, U> collect(Future<U> future) {
			return new Collector2<>(this, future);
		}
	}
	
	public class Result {
		
		private Future<?> future;
		private Result parent;
		
		private Result(Future<?> future) {
			this(future, null);
		}

		public Result(Future<?> future, Result parent) {
			this.future = future;
			this.parent = parent;
		}
		
		public void failure(OnFailure onFailure) {
			future.onFailure(onFailure, executionContext);
			
			if(parent != null) {
				parent.failure(onFailure);
			}
		}
	}
	
	private abstract static class Collector<T> {
		
		protected final Future<T> future;
		
		private Collector(Future<T> future) {
			this.future = future;
		} 
	}
	
	public <T> Collector1<T> collect(Future<T> future) {
		return new Collector1<>(future);
	}
	
	public <T, U> Future<T> cast(Future<U> future, Class<T> targetClass) {
		return cast(future, targetClass, null);
	}
	
	public <T, U> Future<T> cast(Future<U> future, final Class<T> targetClass, final Object context) {
		return future.map(new Mapper<U, T>() {
			
			@Override			
			public T checkedApply(U u) throws Throwable {
				if(targetClass.isInstance(u)) {
					return targetClass.cast(u);
				} else {
					throw new WrongResultException(u, targetClass, context);
				}
			}
			
		}, executionContext);
	}
	
	public <T> Future<T> ask(ActorRef actor, Object message, Class<T> targetClass, Timeout timeout) {		 
		return cast(Patterns.ask(actor, message, timeout), targetClass, message);
	}
	
	public <T> Future<T> ask(ActorRef actor, Object message, Class<T> targetClass, long timeout) {		 
		return cast(Patterns.ask(actor, message, timeout), targetClass, message);
	}
	
	public <T> Future<T> ask(ActorRef actor, Object message, Class<T> targetClass) {		 
		return cast(Patterns.ask(actor, message, timeout), targetClass, message);
	}
	
	public <T> Future<T> ask(ActorSelection selection, Object message, Class<T> targetClass, Timeout timeout) {		 
		return cast(Patterns.ask(selection, message, timeout), targetClass, message);
	}
	
	public <T> Future<T> ask(ActorSelection selection, Object message, Class<T> targetClass, long timeout) {		 
		return cast(Patterns.ask(selection, message, timeout), targetClass, message);
	}
	
	public <T> Future<T> ask(ActorSelection selection, Object message, Class<T> targetClass) {		 
		return cast(Patterns.ask(selection, message, timeout), targetClass, message);
	}
}
