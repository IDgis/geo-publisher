package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;

import nl.idgis.publisher.metadata.MetadataTarget;

/**
 * Request {@link MetadataTarget} to commit active update session.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class CommitMetadata implements Serializable {
	
	private static final long serialVersionUID = -9076307209191913724L;

	@Override
	public String toString() {
		return "CommitMetadata []";
	}

}
