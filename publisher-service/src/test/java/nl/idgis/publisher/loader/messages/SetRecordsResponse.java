package nl.idgis.publisher.loader.messages;

import java.util.List;

import nl.idgis.publisher.provider.protocol.Records;

public class SetRecordsResponse {
	
	private final List<Records> response;

	public SetRecordsResponse(List<Records> response) {
		this.response = response;
	}
	
	public List<Records> getResponse() {
		return response;
	}
}
