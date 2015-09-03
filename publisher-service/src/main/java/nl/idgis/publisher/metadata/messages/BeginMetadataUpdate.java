package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;

import nl.idgis.publisher.metadata.MetadataTarget;

/**
 * Request {@link MetadataTarget} to begin a new metadata update session.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class BeginMetadataUpdate implements Serializable {

	private static final long serialVersionUID = 3051668953948757311L;

	@Override
	public String toString() {
		return "BeginMetadataUpdate []";
	}
}
