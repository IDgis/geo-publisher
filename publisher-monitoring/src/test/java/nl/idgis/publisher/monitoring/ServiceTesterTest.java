package nl.idgis.publisher.monitoring;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;

import scala.concurrent.duration.FiniteDuration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import nl.idgis.publisher.monitoring.tester.ServiceTester;
import nl.idgis.publisher.monitoring.tester.messages.AddUrl;
import nl.idgis.publisher.monitoring.tester.messages.StartTesting;
import nl.idgis.publisher.monitoring.tester.messages.StatusReport;
import nl.idgis.publisher.recorder.PredicateRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.utils.FutureUtils;

public class ServiceTesterTest {
	
	static final int JETTY_PORT = 7000;
	
	ActorSystem actorSystem;
	
	ActorRef serviceTester;
	
	ActorRef recorder;
	
	FutureUtils f;
	
	Server server;
	
	@Before
	public void setUp() throws Exception {
		actorSystem = ActorSystem.create();
		 
		recorder = actorSystem.actorOf(PredicateRecorder.props(new Predicate<Object>() {
			
			boolean first = true;
			
			long currentCount, failureCount, totalCount;

			@Override
			public boolean test(Object msg) {
				if(msg instanceof StatusReport) {
					StatusReport statusReport = (StatusReport)msg;
					
					boolean retval = first
						|| statusReport.getCurrentCount() != currentCount
						|| statusReport.getFailureCount() != failureCount
						|| statusReport.getTotalCount() != totalCount;
					
					currentCount = statusReport.getCurrentCount();
					failureCount = statusReport.getFailureCount();
					totalCount = statusReport.getTotalCount();
					
					first = false;
					
					return retval;
				}
				
				return false;
			}
			
		}));
		serviceTester = actorSystem.actorOf(
			ServiceTester.props(
				new AsyncHttpClient(),
				FiniteDuration.create(1, TimeUnit.MILLISECONDS), 
				recorder));
		
		f = new FutureUtils(actorSystem);
		
		Map<String, byte[]> documents = new HashMap<>();
		documents.put("/success", "Hello, world!".getBytes("utf-8"));
		
		server = new Server(JETTY_PORT);
		server.setHandler(new AbstractHandler() {

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				if(documents.containsKey(target)) {
					ServletOutputStream stream = response.getOutputStream();
					stream.write(documents.get(target));
					stream.close();
				} else {				
					response.sendError(404);
				}
			}
			
		});
		server.start();
	}
	
	@After
	public void shutdown() throws Exception {
		actorSystem.shutdown();
		server.stop();
	}

	@Test
	public void testEmpty() throws Exception {
		f.ask(recorder, new Wait(1), Waited.class).get();
		
		f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(StatusReport.class, statusReport -> {
				assertEquals(0, statusReport.getCurrentCount());
				assertEquals(0, statusReport.getFailureCount());
				assertEquals(0, statusReport.getTotalCount());
			});			
	}
	
	@Test
	public void testSuccess() throws Exception {
		f.ask(recorder, new Wait(1), Waited.class).get();
		
		serviceTester.tell(new AddUrl(new URL("http://localhost:" + JETTY_PORT + "/success")), ActorRef.noSender());
		serviceTester.tell(new StartTesting(true), ActorRef.noSender());
		
		f.ask(recorder, new Wait(2), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()			
			.assertNext(StatusReport.class, statusReport -> {
				assertEquals(0, statusReport.getCurrentCount());
				assertEquals(0, statusReport.getFailureCount());
				assertEquals(0, statusReport.getTotalCount());
			})
			.assertNext(StatusReport.class, statusReport -> {
				assertEquals(1, statusReport.getCurrentCount());
				assertEquals(0, statusReport.getFailureCount());
				assertEquals(1, statusReport.getTotalCount());
			});
	}
	
	@Test
	public void testFailure() throws Exception {
		f.ask(recorder, new Wait(1), Waited.class).get();
		
		serviceTester.tell(new AddUrl(new URL("http://localhost:" + JETTY_PORT + "/failure")), ActorRef.noSender());
		serviceTester.tell(new StartTesting(true), ActorRef.noSender());
		
		f.ask(recorder, new Wait(2), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()			
			.assertNext(StatusReport.class, statusReport -> {
				assertEquals(0, statusReport.getCurrentCount());
				assertEquals(0, statusReport.getFailureCount());
				assertEquals(0, statusReport.getTotalCount());
			})
			.assertNext(StatusReport.class, statusReport -> {
				assertEquals(1, statusReport.getCurrentCount());
				assertEquals(1, statusReport.getFailureCount());
				assertEquals(1, statusReport.getTotalCount());
			});
	}
	
	@Test
	public void testMixed() throws Exception {		
		f.ask(recorder, new Wait(1), Waited.class).get();
		
		serviceTester.tell(new AddUrl(new URL("http://localhost:" + JETTY_PORT + "/success")), ActorRef.noSender());
		serviceTester.tell(new AddUrl(new URL("http://localhost:" + JETTY_PORT + "/failure")), ActorRef.noSender());		
		serviceTester.tell(new StartTesting(true), ActorRef.noSender());
		
		f.ask(recorder, new Wait(3), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()			
			.assertNext(StatusReport.class, statusReport -> {
				assertEquals(0, statusReport.getCurrentCount());
				assertEquals(0, statusReport.getFailureCount());
				assertEquals(0, statusReport.getTotalCount());
			})
			.assertNext(StatusReport.class, statusReport -> {
				assertEquals(1, statusReport.getCurrentCount());
				assertEquals(0, statusReport.getFailureCount());
				assertEquals(2, statusReport.getTotalCount());
			})
			.assertNext(StatusReport.class, statusReport -> {
				assertEquals(2, statusReport.getCurrentCount());
				assertEquals(1, statusReport.getFailureCount());
				assertEquals(2, statusReport.getTotalCount());
			});
	}
}
