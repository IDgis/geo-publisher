package nl.idgis.publisher.metadata;

import akka.actor.Props;

import nl.idgis.publisher.metadata.messages.DatasetInfo;

public class DatasetMetadataGenerator extends AbstractMetadataItemGenerator<DatasetInfo> {

	public DatasetMetadataGenerator(DatasetInfo datasetInfo) {
		super(datasetInfo);
	}
	
	public static Props props(DatasetInfo datasetInfo) {
		return Props.create(DatasetMetadataGenerator.class, datasetInfo);
	}

	@Override
	protected void generateMetadata(MetadataDocument metadataDocument) {
		
	}

}
