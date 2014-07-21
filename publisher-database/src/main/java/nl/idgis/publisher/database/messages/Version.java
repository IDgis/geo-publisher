package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.sql.Timestamp;

import com.mysema.query.annotations.QueryProjection;

public class Version implements Serializable {
	
	private static final long serialVersionUID = -4237772035266877963L;
	
	private final int id;
	private final Timestamp timestamp;
	
	@QueryProjection
	public Version(int id, Timestamp timestamp) {
		this.id = id;
		this.timestamp = timestamp;
	}

	public int getId() {
		return id;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return "Version [id=" + id + ", timestamp=" + timestamp + "]";
	}
}
