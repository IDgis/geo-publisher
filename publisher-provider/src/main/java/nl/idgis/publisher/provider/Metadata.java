package nl.idgis.publisher.provider;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

import nl.idgis.publisher.protocol.metadata.GetMetadata;
import nl.idgis.publisher.protocol.metadata.MetadataItem;
import nl.idgis.publisher.protocol.stream.StreamProvider;

import scala.concurrent.Future;

import akka.actor.Props;

public class Metadata extends StreamProvider<Iterator<File>, GetMetadata, MetadataItem> {

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
	protected Iterator<File> start(GetMetadata msg) {
		return Arrays.asList(metadataDirectory.listFiles()).iterator();
	}

	@Override
	protected Future<MetadataItem> next(Iterator<File> i) {
		return askActor(getContext().actorOf(MetadataParser.props()), i.next(), 1000);		
	}

	@Override
	protected boolean hasNext(Iterator<File> u) { 
		return u.hasNext();
	}
}
