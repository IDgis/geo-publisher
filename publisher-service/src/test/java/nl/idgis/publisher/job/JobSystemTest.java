package nl.idgis.publisher.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.CronExpression;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorSystem;
import akka.testkit.TestActorRef;

import scala.concurrent.duration.FiniteDuration;

public class JobSystemTest {
	
	ActorSystem actorSystem;
	
	@Before
	public void actorSystem() {
		Config akkaConfig = ConfigFactory.empty()
				.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
				.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
			
		actorSystem = ActorSystem.create("test", akkaConfig);
	}
	
	@After
	public void shutdown() {
		actorSystem.shutdown();
	}

	@Test
	public void testCronExpression() throws Exception {
		CronExpression cronExpression = new CronExpression(JobSystem.ON_THE_HOUR);
		
		TestActorRef<JobSystem> ref = TestActorRef.create(actorSystem, JobSystem.props(null, null, null, null, null));
		JobSystem jobSystem = ref.underlyingActor();
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR, 23);
		calendar.set(Calendar.MINUTE, 55);
		calendar.set(Calendar.SECOND, 1);
		calendar.set(Calendar.MILLISECOND, 1);				
		Date now = calendar.getTime();
		
		FiniteDuration interval = jobSystem.toInterval(cronExpression, now);
		assertNotNull(interval);
		assertEquals(TimeUnit.MILLISECONDS, interval.unit());
		assertEquals(
			5 * 60 * 1000 // five minutes 
			- 1000 // minus one second
			- 1, // minus one millisecond
			interval.length());
		
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 1);
		calendar.set(Calendar.MILLISECOND, 1);
		now = calendar.getTime();
		
		interval = jobSystem.toInterval(cronExpression, now);
		assertNotNull(interval);
		assertEquals(TimeUnit.MILLISECONDS, interval.unit());
		assertEquals(
			60 * 60 * 1000 // one hour
			- 60 * 1000 // minus one minute
			- 1000 // minus one second
			- 1, // minus one millisecond
			interval.length());
	}
}
