package nl.idgis.publisher.monitoring.tester;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.ActorRef;
import akka.actor.AllForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.actor.SupervisorStrategy.Directive;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;

import nl.idgis.publisher.monitoring.tester.messages.AddUrl;
import nl.idgis.publisher.monitoring.tester.messages.Failure;
import nl.idgis.publisher.monitoring.tester.messages.NextUrl;
import nl.idgis.publisher.monitoring.tester.messages.Result;
import nl.idgis.publisher.monitoring.tester.messages.StartTesting;
import nl.idgis.publisher.monitoring.tester.messages.StatusReport;
import nl.idgis.publisher.utils.UniqueNameGenerator;

public class ServiceTester extends UntypedActor {
	
	private final static SupervisorStrategy supervisorStrategy = new AllForOneStrategy(10, Duration.create("1 minute"), 
		new Function<Throwable, Directive>() {

		@Override
		public Directive apply(Throwable t) throws Exception {			
			return AllForOneStrategy.escalate();
		}
		
	});
	
	private static final FiniteDuration DEFAULT_INTERVAL = FiniteDuration.create(10, TimeUnit.SECONDS);
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final FiniteDuration interval;
	
	private final ActorRef target;
	
	private List<URL> active, staging;
	
	private Iterator<URL> itr;
	
	private long lastTime, currentCount, failureCount, totalCount;
	
	public ServiceTester(FiniteDuration interval, ActorRef target) {
		this.interval = interval;
		this.target = target;
	}
	
	public final void preStart() {
		active = new ArrayList<>();
		staging = new ArrayList<>();
		
		createNewIterator();		
		getSelf().tell(new NextUrl(), getSelf());
	}
	
	public static Props props(ActorRef target) {
		return props(DEFAULT_INTERVAL, target);
	}
	
	public static Props props(FiniteDuration interval, ActorRef target) {
		return Props.create(ServiceTester.class, interval, target);
	}
	
	protected Props handlerProps(URL url) {
		return RequestHandler.props(url);
	}
	
	private void createNewIterator() {
		itr = active.iterator();
		currentCount = 0;
		failureCount = 0;
		totalCount = active.size();
	}
	
	private void reportAndScheduleNextRequest() {
		FiniteDuration scheduleInterval = interval.minus(
				Duration.create(
					System.currentTimeMillis() - lastTime, 
					TimeUnit.MILLISECONDS));
		
		log.debug("time to next request: {}", scheduleInterval);
		
		getContext().system().scheduler().scheduleOnce(
				scheduleInterval, getSelf(), new NextUrl(), 
				getContext().dispatcher(), getSelf());
		
		target.tell(new StatusReport(currentCount, failureCount, totalCount), getSelf());
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof AddUrl) {
			URL url = ((AddUrl)msg).getUrl();
			
			log.debug("adding url: {}", url);
			
			String protocol = url.getProtocol();
			if("http".equals(protocol) || "https".equals(protocol)) {		
				staging.add(url);
			} else {
				log.error("only http and https urls are supported");
			}
		} else if(msg instanceof StartTesting) {
			log.debug("testing started");
			
			active = staging;
			staging = new ArrayList<>();
			
			if(((StartTesting) msg).getDiscardPending()) {
				createNewIterator();
			}
		} else if(msg instanceof NextUrl) {
			lastTime = System.currentTimeMillis();
			
			if(!itr.hasNext()) {
				createNewIterator();
			}
			
			if(itr.hasNext()) { // set could be empty
				URL url = itr.next();
				
				log.debug("testing url: {}", url);
				
				getContext().actorOf(
					handlerProps(url),
					nameGenerator.getName("handler"));
			} else {
				log.warning("no test url available");
				
				reportAndScheduleNextRequest();
			}
		} else if(msg instanceof Result) {
			if(msg instanceof Failure) {
				Failure failure = (Failure)msg;
				
				failureCount++;
				log.error("failure, url: {}, message: {}", 
					failure.getUrl(), failure.getMessage());
			}
			
			currentCount++;
						
			reportAndScheduleNextRequest();
		} else {
			unhandled(msg);
		}
	}	
	
	@Override
	public final SupervisorStrategy supervisorStrategy() {
		return supervisorStrategy;
	}
}
