package nl.idgis.publisher.provider;

import java.io.File;
import java.util.Arrays;

import nl.idgis.publisher.protocol.metadata.GetMetadata;
import nl.idgis.publisher.protocol.metadata.MetadataItem;
import nl.idgis.publisher.protocol.stream.StreamProvider;

import akka.actor.Props;

public class Metadata extends StreamProvider<GetMetadata, MetadataItem> {

	private final File metadataDirectory;

	public Metadata(File metadataDirectory) {
		if (!metadataDirectory.isDirectory()) {
			throw new IllegalArgumentException("metadataDirectory is not a directory");
		}

		this.metadataDirectory = metadataDirectory;
	}

	public static Props props(File metadataDirectory) {
		return Props.create(Metadata.class, metadataDirectory);
	}

	@Override
	protected Props start(GetMetadata msg) {
		return MetadataCursor.props(Arrays.asList(metadataDirectory.listFiles()).iterator());
	}
}
