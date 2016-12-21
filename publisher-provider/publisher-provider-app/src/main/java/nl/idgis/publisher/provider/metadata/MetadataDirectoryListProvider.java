package nl.idgis.publisher.provider.metadata;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import nl.idgis.publisher.provider.metadata.messages.GetAllMetadata;
import nl.idgis.publisher.stream.StreamProvider;

import akka.actor.Props;

public class MetadataDirectoryListProvider extends StreamProvider<GetAllMetadata> {

	private final File metadataDirectory;

	public MetadataDirectoryListProvider(File metadataDirectory) {
		this.metadataDirectory = metadataDirectory;
	}

	public static Props props(File metadataDirectory) {
		return Props.create(MetadataDirectoryListProvider.class, metadataDirectory);
	}
	
	@Override
	protected boolean isAvailable() {
		return metadataDirectory.isDirectory();
	}

	@Override
	protected Props start(GetAllMetadata msg) {
		return MetadataFileCursor.props(
			Arrays.asList(metadataDirectory.listFiles()).stream()
				.filter(File::isFile)
				.collect(Collectors.toList()).iterator());
	}
}
