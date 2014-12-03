package nl.idgis.publisher.harvester.sources;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import scala.concurrent.Future;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.harvest.DatabaseLog;
import nl.idgis.publisher.domain.job.harvest.HarvestLogType;
import nl.idgis.publisher.domain.job.harvest.HarvestLog;
import nl.idgis.publisher.domain.job.harvest.MetadataField;
import nl.idgis.publisher.domain.job.harvest.MetadataLog;
import nl.idgis.publisher.domain.job.harvest.MetadataLogType;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.web.EntityType;

import nl.idgis.publisher.harvester.sources.messages.Finished;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.TableNotFound;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.Ask;
import nl.idgis.publisher.xml.exceptions.NotFound;
import nl.idgis.publisher.xml.exceptions.NotParseable;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class ProviderDatasetInfo extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef harvesterSession, providerDatabase;
	
	private MetadataDocumentFactory metadataDocumentFactory;
	
	public ProviderDatasetInfo(ActorRef harvesterSession, ActorRef providerDatabase) {
		this.harvesterSession = harvesterSession;		
		this.providerDatabase = providerDatabase;		
	}
	
	public static Props props(ActorRef harvesterSession, ActorRef providerDatabase) {
		return Props.create(ProviderDatasetInfo.class, harvesterSession, providerDatabase);
	}
	
	@Override
	public void preStart() throws Exception {
		metadataDocumentFactory = new MetadataDocumentFactory();
	}
	
	private void processDocument(final ActorRef sender, String identification, MetadataDocument metadataDocument) {
				
		String title; 
		String alternateTitle; 
		Date revisionDate;
		
		List<MetadataLogType> errors = new ArrayList<>();
		List<MetadataField> fields = new ArrayList<>();					
		List<Object> values = new ArrayList<>();
		
		try {
			title = metadataDocument.getTitle();
			log.debug("metadata title: " + title);
		} catch(NotFound nf) {
			title = null;
			
			errors.add(MetadataLogType.NOT_FOUND);
			fields.add(MetadataField.TITLE);						
			values.add(null);
		}
		
		try {
			alternateTitle = metadataDocument.getAlternateTitle();
			log.debug("metadata alternate title: " + alternateTitle);
		} catch(NotFound nf) {
			alternateTitle = null;
			
			errors.add(MetadataLogType.NOT_FOUND);
			fields.add(MetadataField.ALTERNATE_TITLE);						
			values.add(null);
		}
		
		try {
			revisionDate = metadataDocument.getRevisionDate();
			log.debug("metadata revision date: " + revisionDate);
		} catch(NotFound nf) {
			revisionDate = null;
			
			errors.add(MetadataLogType.NOT_FOUND);
			fields.add(MetadataField.REVISION_DATE);						
			values.add(null);
		}
		
		if(errors.isEmpty()) {
			processMetadata(sender, identification, title, alternateTitle, revisionDate);
		} else {
			Iterator<MetadataLogType> errorsItr = errors.iterator();
			Iterator<MetadataField> fieldsItr = fields.iterator();
			Iterator<Object> valuesItr = values.iterator();
			
			List<Future<Object>> futures = new ArrayList<>();
			for(;errorsItr.hasNext();) {
				MetadataLogType error = errorsItr.next();
				MetadataField field = fieldsItr.next();
				Object value = valuesItr.next();
				
				MetadataLog content = new MetadataLog(EntityType.SOURCE_DATASET, identification, title, alternateTitle, field, error, value);
				Log jobLog = Log.create(LogLevel.ERROR, HarvestLogType.METADATA_PARSING_ERROR, content);
				
				futures.add(Patterns.ask(harvesterSession, jobLog, 15000));
			}
			
			Futures.sequence(futures, getContext().dispatcher())
				.onComplete(new OnComplete<Iterable<Object>>() {

					@Override
					public void onComplete(Throwable t, Iterable<Object> msg) throws Throwable {
						if(t != null) {
							log.error("couldn't store parsing error(s): {}", t);
						} else {
							log.debug("metadata parsing error(s) saved");
						}
						
						sender.tell(new NextItem(), getSelf());
					}
					
				}, getContext().dispatcher());
		}
	}
				
	
	private void processMetadata(final ActorRef sender, final String identification, final String title, final String alternateTitle, final Date revisionDate) {
		final String tableName = ProviderUtils.getTableName(alternateTitle);
		if(tableName == null) {
			log.warning("couldn't determine table name: " + alternateTitle);
			
			Log jobLog = Log.create (
					LogLevel.ERROR, 
					HarvestLogType.UNKNOWN_TABLE, 
					new HarvestLog(EntityType.SOURCE_DATASET, identification, title, alternateTitle));
			
			Patterns.ask(harvesterSession, jobLog, 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {						
						sender.tell(new NextItem(), getSelf());
					}
					
				}, getContext().dispatcher());
		} else {
			final String categoryId = ProviderUtils.getCategoryId(alternateTitle);
			if(categoryId == null) {
				log.warning("couldn't determine category id: " + alternateTitle);
				
				sender.tell(new NextItem(), getSelf());
			} else {				
				Future<Object> tableDescriptionFuture = Ask.ask(getContext(), providerDatabase, new DescribeTable(tableName), 15000);
				tableDescriptionFuture.onComplete(new OnComplete<Object>() {
	
					@Override
					public void onComplete(Throwable t, Object msg) throws Throwable {
						if(t != null) {
							log.error("couldn't fetch table description: " + t);
							sender.tell(new NextItem(), getSelf());
						} else {
							if(msg instanceof TableNotFound) {
								log.error("table doesn't exist: " + tableName);
								
								Log jobLog = Log.create(LogLevel.ERROR, HarvestLogType.TABLE_NOT_FOUND,
									new DatabaseLog(EntityType.SOURCE_DATASET, identification, title, alternateTitle, tableName));
								
								Patterns.ask(harvesterSession, jobLog, 15000)
									.onSuccess(new OnSuccess<Object>() {

										@Override
										public void onSuccess(Object msg) throws Throwable {
											sender.tell(new NextItem(), getSelf());
										}
										
									}, getContext().dispatcher());
							} else {								
								TableDescription tableDescription = (TableDescription)msg;
								
								log.debug("table description received");
								
								List<Column> columns = new ArrayList<Column>();
								for(nl.idgis.publisher.provider.protocol.Column column : tableDescription.getColumns()) {
									columns.add(new Column(column.getName(), column.getType()));
								}
								
								Table table = new Table(title, columns);
								
								Patterns.ask(harvesterSession, new Dataset(identification, categoryId, table, revisionDate), 15000)
									.onSuccess(new OnSuccess<Object>() {

										@Override
										public void onSuccess(Object msg) throws Throwable {
											log.debug("dataset provided to harvester " + msg.toString());
											
											sender.tell(new NextItem(), getSelf());
										}
									}, getContext().dispatcher());
							}
						}
					}
				}, getContext().dispatcher());
			}
		}
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof MetadataItem) {
			handleMetadataItem((MetadataItem)msg);
		} else if(msg instanceof End) {	
			handleEnd();
		} else if(msg instanceof Failure) {
			handleFailure(msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleFailure(Object msg) {
		log.error(msg.toString());
		
		finish();
	}

	private void handleEnd() {
		log.debug("dataset retrieval completed");
		
		finish();
	}

	private void handleMetadataItem(MetadataItem metadataItem) throws Exception {
		log.debug("metadata item received");
		
		final ActorRef sender = getSender();
		final String identification = metadataItem.getIdentification();
		
		try {
			MetadataDocument document = metadataDocumentFactory.parseDocument(metadataItem.getContent());
			processDocument(sender, identification, document);
		} catch(NotParseable notParsable) {
			log.debug("couldn't parse metadata document: " + notParsable);
			
			MetadataLog content = new MetadataLog (
					EntityType.SOURCE_DATASET,
					identification, 
					null, 
					null, 
					null, 
					null, 
					notParsable.getCause().getMessage()
				);											
			Log jobLog = Log.create(LogLevel.ERROR, HarvestLogType.METADATA_PARSING_ERROR, content);
			
			Patterns.ask(harvesterSession, jobLog, 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						log.debug("metadata parsing error saved");
						
						sender.tell(new NextItem(), getSelf());
					}
					
				}, getContext().dispatcher());
			
			return;
		}
	}
	
	private void finish() {
		harvesterSession.tell(new Finished(), getSelf());
		getContext().stop(getSelf());
	}
}
