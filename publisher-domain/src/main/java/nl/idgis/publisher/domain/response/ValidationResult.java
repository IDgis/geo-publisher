package nl.idgis.publisher.domain.response;

import java.io.Serializable;

public interface ValidationResult extends Serializable {

	boolean isValid();
}
