package nl.idgis.publisher.provider.mock;

import java.util.Map;

import nl.idgis.publisher.provider.metadata.messages.GetAllMetadata;
import nl.idgis.publisher.stream.StreamProvider;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MetadataListProviderMock extends StreamProvider<GetAllMetadata> {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Map<String, byte[]> metadataDocuments;
	
	public MetadataListProviderMock(Map<String, byte[]> metadataDocuments) {
		this.metadataDocuments = metadataDocuments;
	}
	
	public static Props props(Map<String, byte[]> metadataDocuments) {
		return Props.create(MetadataListProviderMock.class, metadataDocuments);
	}

	@Override
	protected Props start(GetAllMetadata msg) throws Exception {
		log.debug("start");
		
		return MetadataCursorMock.props(metadataDocuments);
	}
	
}