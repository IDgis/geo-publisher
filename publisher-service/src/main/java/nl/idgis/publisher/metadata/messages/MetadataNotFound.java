package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;

import nl.idgis.publisher.metadata.MetadataSource;

/**
 * Informs that {@link MetadataSource} could not find a specific metadata document.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class MetadataNotFound implements Serializable {

	private static final long serialVersionUID = -2360700755873799452L;

	@Override
	public String toString() {
		return "MetadataNotFound []";
	}

}
