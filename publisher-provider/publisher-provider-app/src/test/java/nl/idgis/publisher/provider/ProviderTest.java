package nl.idgis.publisher.provider;

import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.stream.messages.End;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class ProviderTest {
	
	private static final FiniteDuration duration = FiniteDuration.create(10, TimeUnit.SECONDS);
	
	private static final Timeout timeout = new Timeout(duration);
	
	private ActorRef provider;
	
	@Before
	public void actors() {		
		provider = ActorSystem.create().actorOf(Provider.props(DatabaseMock.props(), MetadataMock.props()));
	}
	
	private <T> T ask(Object msg, Class<T> expected) throws Exception {
		Future<Object> future = Patterns.ask(provider, msg, timeout);
		
		Object result = Await.result(future, duration);
		if(expected.isInstance(result)) {
			return expected.cast(result);
		} else {
			fail("expected: " + expected.getCanonicalName() + " received: " + result.getClass().getCanonicalName());
			return null;
		}
	}

	@Test
	public void testListDatasetInfo() throws Exception {
		Set<AttachmentType> attachmentTypes = new HashSet<>();
		attachmentTypes.add(AttachmentType.METADATA);
		
		ask(new ListDatasetInfo(attachmentTypes), End.class);
	}
}
