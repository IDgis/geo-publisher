package nl.idgis.publisher.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Retry;
import nl.idgis.publisher.utils.AskResponse;
import nl.idgis.publisher.utils.FutureUtils;

public class StreamCursorTest {
	
	ActorSystem actorSystem;
	
	FutureUtils f;
	
	@Before
	public void start() {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.create("test", akkaConfig);
		f = new FutureUtils(actorSystem);
	}
	
	@After
	public void stop() {
		actorSystem.shutdown();
	}
	
	static class TestItem extends Item {		
	
		private static final long serialVersionUID = 3730672006015263341L;
		
		final int i;
		
		TestItem(int i) {
			this.i = i;
		}
		
		int getInt() {
			return i;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + i;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestItem other = (TestItem) obj;
			if (i != other.i)
				return false;
			return true;
		}
	}

	@Test
	public void testStream() throws Exception {
		ActorRef cursor = actorSystem.actorOf(
			IteratorCursor.props(
				IntStream.range(0, 100)
					.mapToObj(TestItem::new)
					.collect(Collectors.toList()).iterator()));		
		
		int i = 0;
		for(
			AskResponse<Object> j = f.askWithSender(cursor, new NextItem()).get();
			!(j.getMessage() instanceof End);
			j = f.askWithSender(j.getSender(), new NextItem()).get()) {
			
			Object msg = j.getMessage();
			assertTrue(msg instanceof TestItem);
			assertEquals(msg, f.ask(j.getSender(), new Retry()).get());			
			assertEquals(i++, ((TestItem)msg).getInt());
		}
	}
}
