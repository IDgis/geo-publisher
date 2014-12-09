package nl.idgis.publisher.harvester.sources;

import org.junit.Before;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class ProviderDataSourceTest {
	
	ActorRef providerDataSource;
	
	@Before
	public void actors() {
		ActorSystem actorSystem = ActorSystem.create("test");
		
		//actorSystem.actorOf(ProviderDataSource.props(provider));
	}
}
