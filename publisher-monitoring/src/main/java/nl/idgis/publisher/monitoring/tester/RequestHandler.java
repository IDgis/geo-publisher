package nl.idgis.publisher.monitoring.tester;

import java.io.Serializable;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.AllForOneStrategy;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;

import nl.idgis.publisher.monitoring.tester.messages.Failure;
import nl.idgis.publisher.monitoring.tester.messages.Result;
import nl.idgis.publisher.monitoring.tester.messages.Success;
import nl.idgis.publisher.utils.Either;

public class RequestHandler extends UntypedActor {
	
	private final static SupervisorStrategy supervisorStrategy = new AllForOneStrategy(10, Duration.create("1 minute"), 
		new Function<Throwable, Directive>() {

		@Override
		public Directive apply(Throwable t) throws Exception {			
			return AllForOneStrategy.escalate();
		}
		
	});
	
	private static class RequestResult implements Serializable {
		
		private static final long serialVersionUID = -8798387539560274630L;
		
		private final Either<Response, Throwable> result;
		
		RequestResult(Response response) {
			this.result = Either.left(response);
		}
		
		RequestResult(Throwable t) {
			this.result = Either.right(t);
		}
		
		Either<Response, Throwable> getResult() {
			return result;
		}
	}
	
	private static Duration DEFAULT_TIMEOUT = Duration.create(30, TimeUnit.SECONDS);
	
	private final Duration timeout;
	
	private final URL url;
	
	private final AsyncHttpClient asyncHttpClient;
	
	public RequestHandler(AsyncHttpClient asyncHttpClient, URL url, Duration timeout) {
		this.url = url;
		this.timeout = timeout;
		this.asyncHttpClient = asyncHttpClient;
	}
	
	public static Props props(AsyncHttpClient asyncHttpClient, URL url) {
		return props(asyncHttpClient, url, DEFAULT_TIMEOUT);
	}
	
	public static Props props(AsyncHttpClient asyncHttpClient, URL url, Duration timeout) {
		return Props.create(RequestHandler.class, asyncHttpClient, url, timeout);
	}
	
	@Override
	public final void preStart() {
		asyncHttpClient
			.prepareGet(url.toExternalForm())
			.execute(new AsyncCompletionHandler<Response>() {
				
				final ActorRef self = getSelf();

				@Override
				public Response onCompleted(Response response) throws Exception {
					self.tell(new RequestResult(response), self);
					return response;
				}
				
				public void onThrowable(Throwable t) {
					self.tell(new RequestResult(t), self);
				}
			});
		
		getContext().setReceiveTimeout(timeout);
	}
	
	@Override
	public final void postStop() {
		asyncHttpClient.close();
	}
	
	protected Result getResult(Response response) {
		int statusCode = response.getStatusCode();
		
		if(statusCode / 100 == 2) {
			return new Success(url);			
		} else {
			return new Failure(url, "expected status code 2xx (received: " + statusCode +")");
		}
	}
	
	protected Result getResult(Throwable t) {
		return new Failure(url, t);
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		final Result result;
		
		if(msg instanceof ReceiveTimeout) {
			result = new Failure(url, "timeout");
		} else if(msg instanceof RequestResult) {
			result = ((RequestResult)msg).getResult().map(this::getResult, this::getResult);
		} else {
			result = new Failure(url, "unknown");
		}
		
		getContext().parent().tell(result, getSelf());		
		getContext().stop(getSelf());
	}
	
	@Override
	public final SupervisorStrategy supervisorStrategy() {
		return supervisorStrategy;
	}
}
