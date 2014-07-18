package nl.idgis.publisher.provider;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import javax.xml.namespace.QName;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;

import com.fasterxml.aalto.AsyncInputFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class MetadataParser extends UntypedActor {	
	
	public MetadataParser() {		
		
	}
	
	public static Props props() {
		return Props.create(MetadataParser.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof File) {
			getContext().stop(getSelf());
			
			final ActorRef sender = getSender();			
			final File file = (File)msg;
			final AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath());
			
			ByteBuffer bytes = ByteBuffer.allocate(8192);
			CompletionHandler<Integer, ByteBuffer> handler = new CompletionHandler<Integer, ByteBuffer>() {

				long fileLength = file.length();
				long filePosition = 0;

				final AsyncXMLInputFactory inputFactory = (AsyncXMLInputFactory) AsyncXMLInputFactory
						.newInstance();
				final AsyncXMLStreamReader reader = inputFactory
						.createAsyncXMLStreamReader();

				final StringBuilder title = new StringBuilder();
				final StringBuilder alternateTitle = new StringBuilder();
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
				
				final List<QName> alternateTitlePath = Arrays
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
										"alternateTitle"), new QName(
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
							} else if (reader.isCharacters()) {
								if(position.equals(titlePath)) {
									title.append(reader.getText());
								} else if(position.equals(alternateTitlePath)) {
									alternateTitle.append(reader.getText());
								}
							}
						}

						if (filePosition == fileLength) {
							sender.tell(new MetadataItem(file.getName(), title.toString(), alternateTitle.toString()), getSelf());							
						} else {
							attachment.position(0);
							channel.read(attachment, filePosition, attachment, this);
						}
					} catch (Exception e) {
						sender.tell(new Failure(e), getSelf());						
					}
				}

				@Override
				public void failed(Throwable t, ByteBuffer attachment) {
					sender.tell(new Failure(t), getSelf());					
				}
			};

			channel.read(bytes, 0, bytes, handler);
		} else {
			unhandled(msg);
		}
	}
}
