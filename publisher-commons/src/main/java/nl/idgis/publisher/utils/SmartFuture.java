package nl.idgis.publisher.utils;

import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;

import nl.idgis.publisher.function.Function1;
import nl.idgis.publisher.function.Procedure1;
import nl.idgis.publisher.function.Procedure2;

public class SmartFuture<T> {
	
	private final Future<T> future;
	
	private final ExecutionContext executionContext;

	public SmartFuture(Future<T> future, ExecutionContext executionContext) {
		this.future = future;
		this.executionContext = executionContext;
	}
	
	public <U> SmartFuture<U> map(Function1<? super T,? extends U> mapper) {
		return new SmartFuture<U>(future.map(new Mapper<T, U>() {
			
			@Override
			public U checkedApply(T t) throws Throwable {
				return mapper.apply(t);
			}
			
		}, executionContext), executionContext);
	}
	
	public <U> SmartFuture<U> flatMap(Function1<? super T, SmartFuture<U>> mapper) {
		return new SmartFuture<U>(future.flatMap(new Mapper<T, Future<U>>() {
			
			@Override
			public Future<U> checkedApply(T t) throws Throwable {
				SmartFuture<U> smartFuture = mapper.apply(t);
				
				if(smartFuture.executionContext != executionContext) {
					throw new IllegalArgumentException("mapper returned a future bound to a different execution context");
				}
				
				return smartFuture.future;
			}
			
		}, executionContext), executionContext);
	}
	
	public <U> SmartFuture<U> mapValue(U u) {
		return map(t -> u);
	}
	
	public SmartFuture<Void> mapNull() {
		return mapValue(null);
	}
	
	public T get(Duration atMost) throws Exception {
		return Await.result(future, atMost);
	}

	public void onFailure(Procedure1<Throwable> failureHandler) {
		future.onFailure(new OnFailure() {

			@Override
			public void onFailure(Throwable t) throws Throwable {
				failureHandler.apply(t);
			}
			
		}, executionContext);
	}
	
	public void onSuccess(Procedure1<T> successHandler) {
		future.onSuccess(new OnSuccess<T>() {

			@Override
			public void onSuccess(T t) throws Throwable {
				successHandler.apply(t);
			}
			
		}, executionContext);
	}
	
	public void onComplete(Procedure2<Throwable, T> completeHandler) {
		future.onComplete(new OnComplete<T>() {

			@Override
			public void onComplete(Throwable throwable, T t) throws Throwable {
				completeHandler.apply(throwable, t);
			}
			
		}, executionContext);
	}
	
	public void pipeTo(ActorRef recipient, ActorRef sender) {
		Patterns.pipe(future, executionContext).pipeTo(recipient, sender);
	}
	
	public <U> SmartFuture<U> cast(Class<U> targetClass) {
		return cast(targetClass, null);
	}
	
	public <U> SmartFuture<U> cast(Class<U> targetClass, Object context) {
		return map(u -> {
			if(targetClass.isInstance(u)) {
				return targetClass.cast(u);
			} else {
				throw new WrongResultException(u, targetClass, context);
			}
		});
	}
}
