package nl.idgis.publisher.metadata;

import java.util.Objects;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.metadata.messages.DatasetInfo;
import nl.idgis.publisher.metadata.messages.PutDatasetMetadata;

public class DatasetMetadataGenerator extends AbstractMetadataItemGenerator<DatasetInfo, PutDatasetMetadata> {

	public DatasetMetadataGenerator(ActorRef metadataTarget, DatasetInfo datasetInfo) {
		super(metadataTarget, datasetInfo);
	}
	
	public static Props props(ActorRef metadataTarget, DatasetInfo datasetInfo) {
		return Props.create(
			DatasetMetadataGenerator.class, 
			Objects.requireNonNull(metadataTarget, "metadataTarget must not be null"), 
			Objects.requireNonNull(datasetInfo, "datasetInfo must not be null"));
	}

	@Override
	protected PutDatasetMetadata generateMetadata(MetadataDocument metadataDocument) {
		return new PutDatasetMetadata(itemInfo.getId(), metadataDocument);
	}

}
