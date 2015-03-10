package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

import nl.idgis.publisher.domain.query.DeleteEntity;

public class DeleteEvent implements Serializable {	

	private static final long serialVersionUID = 6099425285900749222L;
	
	private final DeleteEntity<?> msg;
	
	public DeleteEvent(DeleteEntity<?> msg) {
		this.msg = msg;
	}
	
	public DeleteEntity<?> getMessage() {
		return msg;
	}

	@Override
	public String toString() {
		return "DeleteEvent [msg=" + msg + "]";
	}
}
