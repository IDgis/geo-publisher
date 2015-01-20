package nl.idgis.publisher.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;

public class FutureTest {
	
	ActorSystem actorSystem;
	
	@Before
	public void actorSystem() {
		actorSystem = ActorSystem.create();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCompletableFuture() throws Throwable {
		CompletableFuture<String> future =
		
		CompletableFuture.completedFuture("Hello, world!").thenApply(s -> {
			try {
				return Integer.parseInt(s);
			} catch(Exception e) {
				throw new IllegalArgumentException(e);
			}
		}).thenApply(i -> i.toString());
		
		try {
			future.get(5, TimeUnit.SECONDS);
		} catch(ExecutionException e) {
			throw e.getCause();
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testScalaFuture() throws Exception {
		Future<String> future = 
		
		Futures.successful("Hello, world!").map(new Mapper<String, Integer>() {
			
			@Override
			public Integer apply(String s) {
				try {
					return Integer.parseInt(s);
				} catch(Exception e) {
					throw new IllegalArgumentException("Not an integer", e);
				}
			}
			
		}, actorSystem.dispatcher()).map(new Mapper<Integer, String>() {
			
			@Override
			public String apply(Integer i) {
				return i.toString();
			}
		}, actorSystem.dispatcher());
		
		Await.result(future, Duration.apply(5, TimeUnit.SECONDS));		
	}
}
