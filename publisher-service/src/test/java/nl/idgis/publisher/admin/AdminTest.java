package nl.idgis.publisher.admin;

import nl.idgis.publisher.database.AbstractDatabaseTest;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.Props;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class AdminTest extends AbstractDatabaseTest {
	
	protected ActorRef admin;
	private Props adminProps;
	
	@Before
	public void admin(){
//		Config config = ConfigFactory.empty()
//			.withValue("database.actorRef", ConfigValueFactory.fromAnyRef("akka.tcp://service@127.0.0.1:2552/user/app/admin"));
//			
//		adminProps = Props.create(Admin.class, config);
//		admin = actorOf(adminProps, "admin");
	}
	
	@Test
	public void testHandleListCategories() {
		
		
		
		
	}

}
