package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.xml.messages.NotFound;

public class MetadataFailure implements Serializable {

	private static final long serialVersionUID = -1020834725658321707L;
	
	private final List<NotValid<?>> notValid;
	private final List<NotFound> notFound;
	
	public MetadataFailure(List<NotValid<?>> notValid, List<NotFound> notFound) {
		this.notValid = notValid;
		this.notFound = notFound;
	}

	public List<NotValid<?>> getNotValid() {
		return Collections.unmodifiableList(notValid);
	}

	public List<NotFound> getNotFound() {
		return Collections.unmodifiableList(notFound);
	}

	@Override
	public String toString() {
		return "Failure [notValid=" + notValid + ", notFound=" + notFound + "]";
	}
}
