package nl.idgis.publisher.xml.messages;

public class NotFound extends QueryFailure{
	
	private static final long serialVersionUID = 298975318219040192L;

	public NotFound(Query<?> query) {
		super(query);
	}

	@Override
	public String toString() {
		return "NotFound []";
	}

}
