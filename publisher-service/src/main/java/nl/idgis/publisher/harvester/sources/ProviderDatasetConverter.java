package nl.idgis.publisher.harvester.sources;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;

import nl.idgis.publisher.harvester.sources.messages.ListDatasets;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.Column;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.stream.StreamConverter;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Start;

public class ProviderDatasetConverter extends StreamConverter {
	
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
			if(msg instanceof VectorDatasetInfo) {
				VectorDatasetInfo vectorDatasetInfo = (VectorDatasetInfo)msg;
				
				List<nl.idgis.publisher.domain.service.Column> columns = new ArrayList<>();
				TableDescription tableDescription = vectorDatasetInfo.getTableDescription();
				for(Column column : tableDescription.getColumns()) {
					columns.add(new nl.idgis.publisher.domain.service.Column(column.getName(), column.getType()));
				}
				
				sender.tell(new Dataset(vectorDatasetInfo.getIdentification(), "category", new Table(vectorDatasetInfo.getTableName(), columns), new Date()), getSelf());
			} else { // Unhandled DatasetInfo type, ask for the next one
				getSender().tell(new NextItem(), getSelf());
			}
		} else {
			unhandled(msg);
		}
	}

	
}
