package nl.idgis.publisher.xml.messages;

import java.io.Serializable;

public abstract class QueryFailure implements Serializable {

	private static final long serialVersionUID = 3390984115494954018L;
	
	private final Query<?> query;	

	public QueryFailure(Query<?> query) {
		this.query = query;
	}

	public Query<?> getQuery() {
		return query;
	}	

}