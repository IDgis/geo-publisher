package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;
import java.util.Set;

import nl.idgis.publisher.metadata.MetadataGenerator;

/**
 * Request {@link MetadataGenerator} to begin updating metadata.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class GenerateMetadata implements Serializable {

	private static final long serialVersionUID = 6475005346002672426L;
	
	private final Set<GenerateMetadataEnvironment> environments;
	
	GenerateMetadata(Set<GenerateMetadataEnvironment> environments) {
		this.environments = environments;
	}

	public Set<GenerateMetadataEnvironment> getEnvironments() {
		return environments;
	}

	@Override
	public String toString() {
		return "GenerateMetadata [environments=" + environments + "]";
	}
}
