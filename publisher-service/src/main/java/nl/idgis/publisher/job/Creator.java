package nl.idgis.publisher.job;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.GetDataSourceStatus;
import nl.idgis.publisher.database.messages.DataSourceStatus;
import nl.idgis.publisher.domain.job.JobState;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class Creator extends Scheduled {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	
	private static final FiniteDuration HARVEST_INTERVAL = Duration.create(15, TimeUnit.MINUTES);
	
	public Creator(ActorRef database) {
		this.database = database;
	}
	
	public static Props props(ActorRef database) {
		return Props.create(Creator.class, database);
	}

	@Override
	protected void doInitiate() {
		log.debug("creating jobs");
		
		Patterns.ask(database, new GetDataSourceStatus(), 15000)
			.onSuccess(new OnSuccess<Object>() {
				
				@Override
				@SuppressWarnings("unchecked")
				public void onSuccess(Object msg) throws Throwable {
					log.debug("data source status: " + msg);
					
					for(DataSourceStatus dataSourceStatus : (List<DataSourceStatus>)msg) {
						final String dataSourceId = dataSourceStatus.getDataSourceId();
						final JobState state = dataSourceStatus.getFinishedState();
						final Timestamp time = dataSourceStatus.getLastHarvested();
						
						final long timeDiff;
						if(time != null) {
							timeDiff = System.currentTimeMillis() - time.getTime();
						} else {
							timeDiff = Long.MAX_VALUE;
						}
						
						if(!JobState.SUCCEEDED.equals(state)
							|| timeDiff > HARVEST_INTERVAL.toMillis()) {
							
							log.debug("creating harvest job for: " + dataSourceId);
							
							Patterns.ask(database, new CreateHarvestJob(dataSourceId), 15000)
								.onSuccess(new OnSuccess<Object>() {

									@Override
									public void onSuccess(Object msg) throws Throwable {
										log.debug("job created for: " + dataSourceId);
									}
								}, getContext().dispatcher());
						} else {
							log.debug("not yet creating harvest job for: " + dataSourceId);
						}
					}
				}
			}, getContext().dispatcher());
	}

}
