package nl.idgis.publisher.harvester.sources;

import java.util.ArrayList;
import java.util.List;

import scala.concurrent.Future;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.harvester.sources.messages.Finished;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.TableDescription;
import nl.idgis.publisher.provider.protocol.database.TableNotFound;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.Ask;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnComplete;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class ProviderDataset extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef harvester, database;
	
	public ProviderDataset(ActorRef harvester, ActorRef database) {
		this.harvester = harvester;
		this.database = database;
	}
	
	public static Props props(ActorRef harvester, ActorRef database) {
		return Props.create(ProviderDataset.class, harvester, database);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof MetadataItem) {
			log.debug("metadata item received");
			
			final MetadataItem metadataItem = (MetadataItem)msg;			
			final ActorRef sender = getSender(), self = getSelf();			
			
			final String tableName = ProviderUtils.getTableName(metadataItem);
			if(tableName == null) {
				log.warning("couldn't determine table name: " + metadataItem);
				
				sender.tell(new NextItem(), self);
			} else {
				final String categoryId = ProviderUtils.getCategoryId(metadataItem);
				if(categoryId == null) {
					log.warning("couldn't determine category id: " + metadataItem);
					
					sender.tell(new NextItem(), self);
				} else {				
					Future<Object> tableDescriptionFuture = Ask.ask(getContext(), database, new DescribeTable(tableName), 15000);
					tableDescriptionFuture.onComplete(new OnComplete<Object>() {
		
						@Override
						public void onComplete(Throwable t, Object msg) throws Throwable {
							if(t != null) {
								log.error("couldn't fetch table description: " + t);
								sender.tell(new NextItem(), self);
							} else {
								if(msg instanceof TableNotFound) {
									log.error("table doesn't exist: " + tableName);
									sender.tell(new NextItem(), self);
								} else {								
									TableDescription tableDescription = (TableDescription)msg;
									
									log.debug("table description received");
									
									List<Column> columns = new ArrayList<Column>();
									for(nl.idgis.publisher.provider.protocol.database.Column column : tableDescription.getColumns()) {
										columns.add(new Column(column.getName(), column.getType()));
									}
									
									Table table = new Table(metadataItem.getTitle(), columns);
									
									Patterns.ask(harvester, new Dataset(metadataItem.getIdentification(), categoryId, table), 15000)
										.onSuccess(new OnSuccess<Object>() {

											@Override
											public void onSuccess(Object msg) throws Throwable {
												log.debug("dataset provided to harvester " + msg.toString());
												
												sender.tell(new NextItem(), self);
											}
										}, getContext().dispatcher());
								}
							}
						}
					}, getContext().dispatcher());
				}
			}			
		} else if(msg instanceof End) {	
			log.debug("dataset retrieval completed");
			
			finish();
		} else if(msg instanceof Failure) {
			log.error(msg.toString());
			
			finish();
		} else {
			unhandled(msg);
		}
	}
	
	private void finish() {
		harvester.tell(new Finished(), getSelf());
		getContext().stop(getSelf());
	}
}
