package nl.idgis.publisher.metadata.messages;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import akka.actor.ActorRef;

/**
 * Helper class to create a {@link GenerateMetadata} message.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class GenerateMetadataFactory {
	
	private final Set<GenerateMetadataEnvironment> environments;
	
	private GenerateMetadataFactory() {
		environments = new HashSet<>();
	}

	public static GenerateMetadataFactory start() {
		return new GenerateMetadataFactory();
	}
	
	public <T> GenerateMetadataFactory forEach(Stream<T> stream, BiConsumer<GenerateMetadataFactory, ? super T> consumer) {
		stream.forEach(t -> consumer.accept(this, t));
		return this;
	}
	
	public GenerateMetadataFactory environment(String environmentId, ActorRef target, String serviceLinkagePrefix, String datasetMetadataPrefix) {
		environments.add(new GenerateMetadataEnvironment(environmentId, target, serviceLinkagePrefix, datasetMetadataPrefix));
		return this;
	}
	
	public GenerateMetadata create() {
		if(environments.isEmpty()) {
			throw new IllegalStateException("no environments added");
		}
		
		return new GenerateMetadata(environments);
	}
}
