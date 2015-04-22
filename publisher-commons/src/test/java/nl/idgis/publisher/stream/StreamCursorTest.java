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

	@SuppressWarnings("unchecked")
	@Test
	public void testStream() throws Exception {
		ActorRef cursor = actorSystem.actorOf(
			IteratorCursor.props(
				IntStream.range(0, 100)
					.mapToObj(Integer::valueOf)
					.collect(Collectors.toList()).iterator()));		
		
		int i = 0;
		for(
			AskResponse<Object> j = f.askWithSender(cursor, new NextItem()).get();
			j.getMessage() instanceof Item;
			j = f.askWithSender(j.getSender(), new NextItem()).get()) {
			
			Object msg = j.getMessage();
			assertTrue(msg instanceof Item);
			
			Item<Integer> item = (Item<Integer>)msg;
			
			long seq = item.getSequenceNumber();
			assertEquals(i, seq);
			
			Integer content = item.getContent();
			assertEquals(i++, content.intValue());
			
			Item<Integer> retriedItem = (Item<Integer>)f.ask(j.getSender(), new Retry(), Item.class).get();
			assertEquals(content, retriedItem.getContent());			
			assertEquals(seq, retriedItem.getSequenceNumber());
		}
	}
}
