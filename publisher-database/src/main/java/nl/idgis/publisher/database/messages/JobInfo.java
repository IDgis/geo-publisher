package nl.idgis.publisher.database.messages;

import java.io.Serializable;

public abstract class JobInfo implements Serializable {

	private static final long serialVersionUID = -6901131165015329634L;
	
	protected final int id;
	
	protected JobInfo(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
}