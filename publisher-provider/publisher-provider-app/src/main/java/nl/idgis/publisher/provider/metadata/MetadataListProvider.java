package nl.idgis.publisher.provider.metadata;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import nl.idgis.publisher.provider.metadata.messages.GetAllMetadata;
import nl.idgis.publisher.stream.StreamProvider;

import akka.actor.ActorRef;
import akka.actor.Props;

public class MetadataListProvider extends StreamProvider<GetAllMetadata> {

	private final File metadataDirectory;
	
	private final ActorRef metadata;

	public MetadataListProvider(File metadataDirectory, ActorRef metadata) {
		this.metadataDirectory = metadataDirectory;
		this.metadata = metadata;
	}

	public static Props props(File metadataDirectory, ActorRef metadata) {
		return Props.create(MetadataListProvider.class, metadataDirectory, metadata);
	}
	
	@Override
	protected boolean isAvailable() {
		return metadataDirectory.isDirectory();
	}

	@Override
	protected Props start(GetAllMetadata msg) {
		return MetadataCursor.props(
			Arrays.asList(metadataDirectory.listFiles()).stream()
				.filter(File::isFile)
				.collect(Collectors.toList()).iterator(),
			metadata);
	}
}
