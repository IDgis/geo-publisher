package nl.idgis.publisher.stream;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Start;
import nl.idgis.publisher.stream.messages.Unavailable;
import nl.idgis.publisher.utils.AskResponse;
import nl.idgis.publisher.utils.FutureUtils;

public class StreamConverterTest {
	
	ActorSystem actorSystem;
	
	FutureUtils f;

	@After
	public void shutdown() {
		actorSystem.shutdown();
	}

	@Before
	public void actorSystem() {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.create("test", akkaConfig);
		
		f = new FutureUtils(actorSystem);
	}
	
	static class ToStringStart<T> implements Start {
		
		private static final long serialVersionUID = 1669433909391275074L;
		
		private final List<T> list;
		
		ToStringStart(List<T> list) {
			this.list = list;
		}
		
		List<T> getList() {
			return list;
		}
	}
	
	static class ToStringConverter extends StreamConverter {
		
		static Props props() {
			return Props.create(ToStringConverter.class);
		}

		@Override
		protected void start(Start msg) throws Exception {
			if(msg instanceof ToStringStart) {
				ActorRef cursor = getContext().actorOf(IteratorCursor.props(((ToStringStart<?>)msg).getList().iterator()));
				cursor.tell(new NextItem(), getSelf());
			} else {
				unhandled(msg);
			}
		}

		@Override
		protected boolean convert(Object msg) throws Exception {
			getSelf().tell(msg.toString(), getSelf());
			
			return true;
		}
		
	}
	
	static class UnavailableStart implements Start {
		
		private static final long serialVersionUID = -1690154370787041106L;
		
	}
	
	static class UnavailableConverter extends StreamConverter {
		
		static Props props() {
			return Props.create(UnavailableConverter.class);
		}

		@Override
		protected void start(Start msg) throws Exception {
			if(msg instanceof UnavailableStart) {
				getSelf().tell(new Unavailable(), getSelf());
			} else {
				unhandled(msg);
			}
		}

		@Override
		protected boolean convert(Object msg) throws Exception {
			return false;
		}
		
	}
	
	@Test
	@SuppressWarnings("rawtypes")
	public void testConvert() throws Exception {
		ActorRef converter = actorSystem.actorOf(ToStringConverter.props(), "to-string-converter");
		
		AskResponse<Item> resp = f.askWithSender(converter, new ToStringStart<>(Arrays.asList(1, 2)), Item.class).get();
		assertEquals("1", resp.getMessage().getContent());
		
		resp = f.askWithSender(resp.getSender(), new NextItem(), Item.class).get();
		assertEquals("2", resp.getMessage().getContent());
		
		f.ask(resp.getSender(), new NextItem(), End.class).get();
	}
	
	@Test
	public void testUnavailable() throws Exception {
		ActorRef converter = actorSystem.actorOf(UnavailableConverter.props(), "unavailable-converter");
		f.ask(converter, new UnavailableStart(), Unavailable.class).get();
	}
}
