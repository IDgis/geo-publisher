package nl.idgis.publisher.xml.messages;

public class NotTextOnly extends QueryFailure {
	
	private static final long serialVersionUID = -3283628227073390161L;

	public NotTextOnly(Query<?> query) {
		super(query);
	}

	@Override
	public String toString() {
		return "NotTextOnly []";
	}

}
