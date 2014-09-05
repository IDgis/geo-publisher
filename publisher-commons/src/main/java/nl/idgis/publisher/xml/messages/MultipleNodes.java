package nl.idgis.publisher.xml.messages;

public class MultipleNodes extends QueryFailure {
	
	private static final long serialVersionUID = -7178164810383525060L;

	public MultipleNodes(Query<?> query) {
		super(query);
	}

	@Override
	public String toString() {
		return "MultipleNodes []";
	}

}
