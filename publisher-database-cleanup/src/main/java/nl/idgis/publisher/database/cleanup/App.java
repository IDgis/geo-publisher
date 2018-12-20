package nl.idgis.publisher.database.cleanup;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
	
	private static final String DB_HOST = System.getenv("DB_HOST");
	private static final String DB_USER = System.getenv("DB_USER");
	private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
	
	public static void main(String[] args) {
		final Runnable cleanup = () -> {
			try {
				doCleanup();
			} catch(Exception e) {
				e.printStackTrace();
			}
		};
		
		LOGGER.info("scheduling with a " + getInitialDelay() + " seconds delay");
		
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(cleanup, getInitialDelay(), 24*60*60, TimeUnit.SECONDS);
	}
	
	public static long getInitialDelay() {
		ZonedDateTime zonedNow = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam"));
		ZonedDateTime zonedNext = zonedNow.withHour(3).withMinute(0).withSecond(0);
		if(zonedNow.compareTo(zonedNext) > 0) zonedNext = zonedNext.plusDays(1);
		return Duration.between(zonedNow, zonedNext).getSeconds();
	}
	
	public static void doCleanup() throws IOException, URISyntaxException {
		LOGGER.info("starting database cleanup");
		
		String sql = IOUtils.readLines(
				App.class.getResourceAsStream("cleanup.sql"), "utf-8").stream()
					.collect(Collectors.joining("\n"));
		
		try(Connection connection = DriverManager.getConnection(DB_HOST, DB_USER, DB_PASSWORD);
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.execute();
		} catch(SQLException se) {
			LOGGER.error("Something went wrong executing the query", se);
		}
		
		LOGGER.info("database cleanup was successful");
	}
}
