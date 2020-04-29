package nl.idgis.publisher.database;

import java.util.concurrent.TimeUnit;

import org.junit.Before;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import nl.idgis.publisher.utils.FutureUtils;

public class AbstractDatabaseHelperTest extends AbstractDatabaseTest {

	protected FutureUtils f;
	
	protected AsyncDatabaseHelper db;
	
	@Before
	public void setup() {
		LoggingAdapter log = Logging.getLogger(system, this);
		
		f = new FutureUtils(system, Timeout.apply(1, TimeUnit.SECONDS));
		db = new AsyncDatabaseHelper(database, getClass().getName(), f, log);
	}
}
