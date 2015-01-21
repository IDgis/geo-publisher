package nl.idgis.publisher.harvester.sources;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.UnavailableDataset;
import nl.idgis.publisher.domain.service.VectorDataset;
import nl.idgis.publisher.domain.service.Table;

import nl.idgis.publisher.harvester.sources.messages.ListDatasets;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.stream.StreamConverter;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.Start;

public class ProviderDatasetConverter extends StreamConverter {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Set<AttachmentType> attachmentTypes;
	private final ActorRef provider; 
	
	public ProviderDatasetConverter(ActorRef provider) {
		this.provider = provider;
		
		attachmentTypes = new HashSet<>();
	}
	
	public static Props props(ActorRef provider) {
		return Props.create(ProviderDatasetConverter.class, provider);
	}

	@Override
	protected void start(Start msg) throws Exception {
		if(msg instanceof ListDatasets) {
			provider.tell(new ListDatasetInfo(attachmentTypes), getSelf());
		} else {
			unhandled(msg);
		}
	}

	@Override
	protected void convert(Item msg, ActorRef sender) throws Exception {
		if(msg instanceof DatasetInfo) {
			DatasetInfo datasetInfo = (DatasetInfo)msg;
			
			String identification = datasetInfo.getIdentification();
			String title = datasetInfo.getTitle();
			String categoryId = datasetInfo.getCategoryId();			
			Date revisionDate = datasetInfo.getRevisionDate();
			Set<Log> logs = datasetInfo.getLogs();
			
			final Dataset dataset;
			if(datasetInfo instanceof VectorDatasetInfo) {
				log.debug("vector dataset info: " + datasetInfo);
				
				VectorDatasetInfo vectorDatasetInfo = (VectorDatasetInfo)datasetInfo;				
				TableInfo tableInfo = vectorDatasetInfo.getTableInfo();
				
				List<Column> columns = Arrays.stream(tableInfo.getColumns())
					.map(column -> new Column(column.getName(), column.getType()))
					.collect(Collectors.toList());
				
				Table table = new Table(title, columns);
				
				dataset = new VectorDataset(identification, categoryId, revisionDate, logs, table);
			} else { 
				log.debug("unhandled dataset info type");
				
				dataset = new UnavailableDataset(identification, categoryId, revisionDate, logs);
			}
			
			sender.tell(dataset, getSelf());			
		} else {
			unhandled(msg);
		}
	}

	
}
