package nl.idgis.publisher.provider;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.xml.namespace.QName;

import nl.idgis.publisher.protocol.metadata.GetMetadata;
import nl.idgis.publisher.protocol.metadata.MetadataItem;
import nl.idgis.publisher.protocol.stream.StreamHandle;
import nl.idgis.publisher.protocol.stream.StreamProvider;

import akka.actor.Props;

import com.fasterxml.aalto.AsyncInputFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;

public class Metadata extends
		StreamProvider<Iterator<File>, GetMetadata, MetadataItem> {

	private final File metadataDirectory;

	public Metadata(File metadataDirectory) {
		if (!metadataDirectory.isDirectory()) {
			throw new IllegalArgumentException(
					"metadataDirectory is not a directory");
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
	protected void next(Iterator<File> i, final StreamHandle<MetadataItem> handle) throws Exception {
		final File file = i.next();		
		final AsynchronousFileChannel channel = AsynchronousFileChannel.open(file
				.toPath());

		ByteBuffer bytes = ByteBuffer.allocate(8192);
		CompletionHandler<Integer, ByteBuffer> handler = new CompletionHandler<Integer, ByteBuffer>() {

			long fileLength = file.length();
			long filePosition = 0;

			final AsyncXMLInputFactory inputFactory = (AsyncXMLInputFactory) AsyncXMLInputFactory
					.newInstance();
			final AsyncXMLStreamReader reader = inputFactory
					.createAsyncXMLStreamReader();

			final StringBuilder title = new StringBuilder();
			final Stack<QName> position = new Stack<QName>();

			final List<QName> titlePath = Arrays
					.asList(new QName("http://www.isotc211.org/2005/gmd",
							"MD_Metadata"), new QName(
							"http://www.isotc211.org/2005/gmd",
							"identificationInfo"), new QName(
							"http://www.isotc211.org/2005/gmd",
							"MD_DataIdentification"), new QName(
							"http://www.isotc211.org/2005/gmd", "citation"),
							new QName("http://www.isotc211.org/2005/gmd",
									"CI_Citation"),
							new QName("http://www.isotc211.org/2005/gmd",
									"title"), new QName(
									"http://www.isotc211.org/2005/gco",
									"CharacterString"));

			@Override
			public void completed(Integer result, ByteBuffer attachment) {
				try {
					filePosition += result;

					AsyncInputFeeder feeder = reader.getInputFeeder();
					feeder.feedInput(attachment.array(), 0, result);

					if (filePosition == fileLength) {
						feeder.endOfInput();
					}

					while (reader.hasNext()) {
						if (reader.next() == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
							break;
						}

						if (reader.isStartElement()) {
							position.push(reader.getName());
						} else if (reader.isEndElement()) {
							position.pop();
						} else if (reader.isCharacters()
								&& position.equals(titlePath)) {
							title.append(reader.getText());
						}
					}

					if (filePosition == fileLength) {
						handle.item(new MetadataItem(file.getName(), title
								.toString()));
					} else {
						attachment.position(0);
						channel.read(attachment, filePosition, attachment, this);
					}
				} catch (Exception e) {
					handle.failure(e.getMessage());
				}
			}

			@Override
			public void failed(Throwable t, ByteBuffer attachment) {
				handle.failure(t.getMessage());
			}
		};

		channel.read(bytes, 0, bytes, handler);
	}

	@Override
	protected boolean hasNext(Iterator<File> u) { 
		return u.hasNext();
	}
}
