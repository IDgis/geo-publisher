package nl.idgis.publisher.provider.metadata;

import java.io.File;
import java.util.Arrays;

import nl.idgis.publisher.provider.protocol.metadata.GetAllMetadata;
import nl.idgis.publisher.stream.StreamProvider;

import akka.actor.Props;

public class MetadataListProvider extends StreamProvider<GetAllMetadata> {

	private final File metadataDirectory;

	public MetadataListProvider(File metadataDirectory) {
		this.metadataDirectory = metadataDirectory;
	}

	public static Props props(File metadataDirectory) {
		return Props.create(MetadataListProvider.class, metadataDirectory);
	}

	@Override
	protected Props start(GetAllMetadata msg) {
		return MetadataCursor.props(Arrays.asList(metadataDirectory.listFiles()).iterator());
	}
}
