package nl.idgis.publisher.provider.mock;

import java.util.Map;

import nl.idgis.publisher.provider.protocol.metadata.GetAllMetadata;
import nl.idgis.publisher.stream.StreamProvider;

import akka.actor.Props;

public class MetadataListProviderMock extends StreamProvider<GetAllMetadata> {
	
	private final Map<String, byte[]> metadataDocuments;
	
	public MetadataListProviderMock(Map<String, byte[]> metadataDocuments) {
		this.metadataDocuments = metadataDocuments;
	}
	
	public static Props props(Map<String, byte[]> metadataDocuments) {
		return Props.create(MetadataListProviderMock.class, metadataDocuments);
	}

	@Override
	protected Props start(GetAllMetadata msg) throws Exception {
		return MetadataCursorMock.props(metadataDocuments);
	}
	
}