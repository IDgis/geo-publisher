package nl.idgis.publisher.provider;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.xml.namespace.QName;

import com.fasterxml.aalto.AsyncInputFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import nl.idgis.publisher.protocol.metadata.EndOfList;
import nl.idgis.publisher.protocol.metadata.GetList;
import nl.idgis.publisher.protocol.metadata.Item;
import nl.idgis.publisher.protocol.metadata.Failure;
import nl.idgis.publisher.protocol.metadata.NextItem;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Metadata extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(),
			this);

	private final File metadataDirectory;
	private final ActorRef harvester;

	private Iterator<File> fileIterator;

	public Metadata(File metadataDirectory, ActorRef harvester) {
		if (!metadataDirectory.isDirectory()) {
			throw new IllegalArgumentException(
					"metadataDirectory is not a directory");
		}

		this.metadataDirectory = metadataDirectory;
		this.harvester = harvester;
	}

	public static Props props(File metadataDirectory, ActorRef harvester) {
		return Props.create(Metadata.class, metadataDirectory, harvester);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof GetList) {
			log.debug("metadata list requested");

			if (fileIterator != null) {
				throw new IllegalStateException("fileIterator != null");
			}

			fileIterator = Arrays.asList(metadataDirectory.listFiles())
					.iterator();
			nextItem();
		} else if (msg instanceof NextItem) {
			log.debug("next metadata list item requested");

			nextItem();
		} else {
			unhandled(msg);
		}
	}

	private void nextItem() throws IOException {
		if (fileIterator == null) {
			throw new IllegalStateException("fileIterator == null");
		}

		if (fileIterator.hasNext()) {
			File file = fileIterator.next();
			AsynchronousFileChannel channel = AsynchronousFileChannel.open(file
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

				final List<QName> titlePath = Arrays.asList(new QName(
						"http://www.isotc211.org/2005/gmd", "MD_Metadata"),
						new QName("http://www.isotc211.org/2005/gmd",
								"identificationInfo"), new QName(
								"http://www.isotc211.org/2005/gmd",
								"MD_DataIdentification"),
						new QName("http://www.isotc211.org/2005/gmd",
								"citation"), new QName(
								"http://www.isotc211.org/2005/gmd",
								"CI_Citation"), new QName(
								"http://www.isotc211.org/2005/gmd", "title"),
						new QName("http://www.isotc211.org/2005/gco",
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
							harvester.tell(
									new Item(file.getName(), title.toString()),
									getSelf());
						} else {
							attachment.position(0);
							channel.read(attachment, filePosition, attachment,
									this);
						}
					} catch (Exception e) {
						harvester.tell(new Failure(e.getMessage()), getSelf());
						fileIterator = null;
					}
				}

				@Override
				public void failed(Throwable t, ByteBuffer attachment) {
					harvester.tell(new Failure(t.getMessage()), getSelf());
					fileIterator = null;
				}
			};

			channel.read(bytes, 0, bytes, handler);
		} else {
			harvester.tell(new EndOfList(), getSelf());
			fileIterator = null;
		}
	}
}
