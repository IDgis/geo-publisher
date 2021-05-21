package nl.idgis.publisher.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.Props;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.AsyncTransactionHelper;
import nl.idgis.publisher.database.messages.CreateIndices;
import nl.idgis.publisher.database.messages.InsertRecords;

import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Type;

import nl.idgis.publisher.dataset.messages.PrepareTable;
import nl.idgis.publisher.harvester.sources.messages.StartVectorImport;
import nl.idgis.publisher.job.manager.messages.VectorImportJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Stop;

public class VectorLoaderSession extends AbstractLoaderSession<VectorImportJobInfo, StartVectorImport> {
	
	private final String tmpTable;
	
	private final List<Column> importColumns;
	
	private final AsyncTransactionHelper tx;
	
	private final ActorRef datasetManager;
	
	private final FilterEvaluator filterEvaluator;	
	
	private long insertCount = 0, filteredCount = 0;
	
	public VectorLoaderSession(Duration receiveTimeout, int maxRetries, ActorRef loader, VectorImportJobInfo importJob, String tmpTable, List<Column> importColumns, ActorRef datasetManager, FilterEvaluator filterEvaluator, AsyncTransactionHelper tx, ActorRef jobContext) throws IOException {		
		super(receiveTimeout, maxRetries, loader, importJob, jobContext);
		
		this.tmpTable = tmpTable;
		this.importColumns = importColumns;
		this.datasetManager = datasetManager;
		this.filterEvaluator = filterEvaluator;
		this.tx = tx;
	}
	
	public static Props props(Duration receiveTimeout, int maxRetries, ActorRef loader, VectorImportJobInfo importJob, String tmpTable, List<Column> importColumns, ActorRef datasetManager, FilterEvaluator filterEvaluator, AsyncTransactionHelper tx, ActorRef jobContext) {
		return Props.create(VectorLoaderSession.class, receiveTimeout, maxRetries, loader, importJob, tmpTable, importColumns, datasetManager, filterEvaluator, tx, jobContext);
	}
	
	public static Props props(ActorRef loader, VectorImportJobInfo importJob, String tmpTable, List<Column> importColumns, ActorRef datasetManager, FilterEvaluator filterEvaluator, AsyncTransactionHelper tx, ActorRef jobContext) {
		return props(DEFAULT_RECEIVE_TIMEOUT, DEFAULT_MAX_RETRIES, loader, importJob, tmpTable, importColumns, datasetManager, filterEvaluator, tx, jobContext);
	}	
	
	@Override
	protected void handleItemContent(Object content) throws Exception {
		if(content instanceof Records) {			 			
			handleRecords((Records)content);
		} else  {
			log.error("unknown item content: {}" + content);
		}
	}
	
	@Override
	protected CompletableFuture<Object> importSucceeded() {
		List<Column> geometryColumns = importJob.getColumns().stream()
			.filter(column -> column.getDataType().equals(Type.GEOMETRY))
			.collect(Collectors.toList());
		
		long timeout = insertCount * 3 + 15000;
		log.debug("Creating indices on table {} with a timeout of: {} ms", tmpTable, timeout);
		
		return tx.ask(new CreateIndices("staging_data", tmpTable, geometryColumns), timeout).thenCompose(createIndicesMsg -> {
			log.debug("indices created");
			
			if(createIndicesMsg instanceof Ack) {
				return f.ask(datasetManager, new PrepareTable(
					Optional.of(tx.getTransactionRef()), 
					tmpTable,
					importJob.getDatasetId(),
					importColumns)).thenCompose(prepareTableMsg -> {
					
					log.debug("table prepared");
					
					if(prepareTableMsg instanceof Ack) {
						return tx.commit().thenApply(commitMsg -> {
							log.debug("transaction committed");
							
							return commitMsg;
						});
					} else {
						return f.successful(prepareTableMsg);
					}
				});
			} else {
				return f.successful(createIndicesMsg);
			}
		});
	}
	
	@Override
	protected CompletableFuture<Object> importFailed() {
		return tx.rollback().thenApply(msg -> {
			log.debug("transaction rolled back");
			
			return msg;
		});
	}	
	
	private void handleRecords(Records msg) {
		List<Record> records = msg.getRecords();
		
		log.debug("records received: {}", records.size());
		
		List<List<Object>> processedRecords = new ArrayList<>();
		for(Record record : records) {
			log.debug("record received: {} {}/{} (filtered:{})", record, (insertCount + filteredCount), progressTarget,  filteredCount);		
			
			if(filterEvaluator != null && !filterEvaluator.evaluate(record)) {
				filteredCount++;
			} else {
				insertCount++;
				
				List<Object> recordValues = record.getValues();
				
				List<Object> values;
				if(recordValues.size() > importColumns.size()) {
					log.debug("creating smaller value list");
					
					values = new ArrayList<>(importColumns.size());
					
					Iterator<Object> valueItr = recordValues.iterator();
					for(int i = 0; i< importColumns.size(); i++) {
						values.add(valueItr.next());
					}
				} else {
					log.debug("use value list from source record");
					
					values = recordValues;
				}
				
				processedRecords.add(values);
			}
		}	
		
		log.debug("records processed");
		
		updateProgress();
		
		ActorRef sender = getSender(), self = getSelf();
		tx.ask(new InsertRecords(
			"staging_data",
			tmpTable, 
			importColumns, 
			processedRecords))
				.exceptionally(t -> new Failure(t))
				.thenAccept(resp -> {
					if(resp instanceof Failure) {
						log.error("failed to insert records: {}", records);
						
						sender.tell(new Stop(), getSelf());
						self.tell(new FinalizeSession(JobState.FAILED), self);
					} else {
						sender.tell(new NextItem(), self);
					}
				});
	}
	
	@Override
	protected long progressTarget(StartVectorImport startImport) {
		return startImport.getCount();
	}

	@Override
	protected long progress() {
		return insertCount + filteredCount;
	}
}
