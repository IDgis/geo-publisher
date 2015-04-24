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

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
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
			AskResponse<Object> j = f.askWithSender(cursor, new NextItem(0)).get();
			j.getMessage() instanceof Item;) {
			
			Object msg = j.getMessage();
			assertTrue(msg instanceof Item);
			
			Item<Integer> item = (Item<Integer>)msg;
			
			long seq = item.getSequenceNumber();
			assertEquals(i, seq);
			
			Integer content = item.getContent();
			assertEquals(i++, content.intValue());
			
			ActorRef sender = j.getSender();
			Item<Integer> retriedItem = (Item<Integer>)f.ask(sender, new NextItem(seq), Item.class).get();			
			assertEquals(content, retriedItem.getContent());			
			assertEquals(seq, retriedItem.getSequenceNumber());
			
			// only requesting the same item again or requesting the next item is allowed
			f.ask(sender, new NextItem(seq - 1), Failure.class).get();
			f.ask(sender, new NextItem(seq + 2), Failure.class).get();
			
			j = f.askWithSender(sender, new NextItem(seq + 1)).get();			
		}
	}
}
