package nl.idgis.publisher.harvester.sources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import scala.concurrent.Future;
import scala.runtime.AbstractFunction3;
import nl.idgis.publisher.domain.job.JobLog;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.harvest.DatabaseLog;
import nl.idgis.publisher.domain.job.harvest.HarvestLogType;
import nl.idgis.publisher.domain.job.harvest.HarvestLog;
import nl.idgis.publisher.domain.job.harvest.MetadataLogType;
import nl.idgis.publisher.domain.job.harvest.MetadataField;
import nl.idgis.publisher.domain.job.harvest.MetadataLog;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.web.EntityType;
import nl.idgis.publisher.harvester.sources.messages.Finished;
import nl.idgis.publisher.metadata.messages.GetAlternateTitle;
import nl.idgis.publisher.metadata.messages.GetRevisionDate;
import nl.idgis.publisher.metadata.messages.GetTitle;
import nl.idgis.publisher.metadata.messages.MetadataFailure;
import nl.idgis.publisher.metadata.messages.NotValid;
import nl.idgis.publisher.metadata.messages.ParseMetadataDocument;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.TableDescription;
import nl.idgis.publisher.provider.protocol.database.TableNotFound;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.Ask;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.WrongResultException;
import nl.idgis.publisher.xml.messages.Close;
import nl.idgis.publisher.xml.messages.NotFound;
import nl.idgis.publisher.xml.messages.NotParseable;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnComplete;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class ProviderDatasetInfo extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef harvesterSession, harvester, providerDatabase;
	
	public ProviderDatasetInfo(ActorRef harvesterSession, ActorRef harvester, ActorRef providerDatabase) {
		this.harvesterSession = harvesterSession;
		this.harvester = harvester;
		this.providerDatabase = providerDatabase;		
	}
	
	public static Props props(ActorRef harvesterSession, ActorRef harvester, ActorRef providerDatabase) {
		return Props.create(ProviderDatasetInfo.class, harvesterSession, harvester, providerDatabase);
	}
	
	private OnSuccess<Object> processDocument(final ActorRef sender, final MetadataItem metadataItem) {
		final String identification = metadataItem.getIdentification();
		
		return new OnSuccess<Object>() {			

			@Override
			public void onSuccess(Object msg) throws Throwable {
				if(msg instanceof NotParseable) {
					log.debug("couldn't parse metadata document: " + msg);
					
					NotParseable notParsable = (NotParseable)msg;
					
					MetadataLog content = new MetadataLog (
							EntityType.SOURCE_DATASET,
							identification, 
							null, 
							null, 
							null, 
							null, 
							notParsable.getReason()
						);											
					JobLog jobLog = JobLog.create(LogLevel.ERROR, HarvestLogType.METADATA_PARSING_ERROR, content);
					
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
				
				final ActorRef metadataDocument = (ActorRef)msg;
				
				FutureUtils f = new FutureUtils(getContext().dispatcher());
				
				f
					.collect(f.ask(metadataDocument, new GetTitle(), String.class))
					.collect(f.ask(metadataDocument, new GetAlternateTitle(), String.class))
					.collect(f.ask(metadataDocument, new GetRevisionDate(), Date.class))
					.result(new AbstractFunction3<String, String, Date, Void>() {

						@Override
						public Void apply(String title, String alternateTitle, Date revisionDate) {
							log.debug("metadata title: " + title);
							log.debug("metadata alternate title: " + alternateTitle);
							log.debug("metadata revision date: " + revisionDate);
							
							processMetadata(sender, identification, title, alternateTitle, revisionDate);
							
							metadataDocument.tell(new Close(), getSelf());
							
							return null;
						}						
					})
					.failure(new OnFailure() {
						
						boolean nextRequested = false;

						@Override
						public void onFailure(Throwable t) throws Throwable {
							if(t instanceof WrongResultException) {
								WrongResultException wre = (WrongResultException)t;
								
								Object context = wre.getContext();
								Object result = wre.getResult();
								
								log.debug("metadata incorrect: " + context + " " + result);
								
								if(result instanceof MetadataFailure) {
									MetadataFailure failure = (MetadataFailure)result;
									List<NotValid<?>> notValid = failure.getNotValid();
									List<NotFound> notFound = failure.getNotFound();
									
									MetadataField field = null;
									if(context instanceof GetTitle) {
										field = MetadataField.TITLE;
									} else if(context instanceof GetAlternateTitle) {
										field = MetadataField.ALTERNATE_TITLE;
									} else if(context instanceof GetRevisionDate) {
										field = MetadataField.REVISION_DATE;
									}
									
									if(field != null) {
										
										Object value = null;
										MetadataLogType error = null;
										if(notValid.isEmpty()) {
											if(!notFound.isEmpty()) {
												error = MetadataLogType.NOT_FOUND;
											}
										} else {
											error = MetadataLogType.NOT_VALID;
											value = notValid.get(0).getValue();
										}
										
										if(error != null) {
											MetadataLog content = new MetadataLog(EntityType.SOURCE_DATASET, identification, null, null, field, error, value);											
											JobLog jobLog = JobLog.create(LogLevel.ERROR, HarvestLogType.METADATA_PARSING_ERROR, content);
											
											Patterns.ask(harvesterSession, jobLog, 15000)
												.onSuccess(new OnSuccess<Object>() {

													@Override
													public void onSuccess(Object msg) throws Throwable {
														log.debug("metadata parsing error saved");
													}
													
												}, getContext().dispatcher());
										}
										
									}									
									
								}
								
							} else {							
								log.error(t, "couldn't parse metadata");
							}
							
							if(!nextRequested) {
								metadataDocument.tell(new Close(), getSelf());
								
								sender.tell(new NextItem(), getSelf());
								nextRequested = true;
							}
						}
					});
			}
		};
	}
	
	private void processMetadata(final ActorRef sender, final String identification, final String title, final String alternateTitle, final Date revisionDate) {
		final String tableName = ProviderUtils.getTableName(alternateTitle);
		if(tableName == null) {
			log.warning("couldn't determine table name: " + alternateTitle);
			
			JobLog jobLog = JobLog.create (
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
								
								JobLog jobLog = JobLog.create(LogLevel.ERROR, HarvestLogType.TABLE_NOT_FOUND,
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
								for(nl.idgis.publisher.provider.protocol.database.Column column : tableDescription.getColumns()) {
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

	private void handleMetadataItem(MetadataItem metadataItem) {
		log.debug("metadata item received");
		
		Patterns.ask(harvester, new ParseMetadataDocument(metadataItem.getContent()), 15000)
			.onSuccess(processDocument(getSender(), metadataItem), getContext().dispatcher());
	}
	
	private void finish() {
		harvesterSession.tell(new Finished(), getSelf());
		getContext().stop(getSelf());
	}
}
