package nl.idgis.publisher.harvester.metadata.messages;

import nl.idgis.publisher.xml.messages.ParseDocument;

public class ParseMetadataDocument extends ParseDocument {
	
	private static final long serialVersionUID = 4822123133303526083L;

	public ParseMetadataDocument(byte[] content) {
		super(content);
	}

	@Override
	public String toString() {
		return "ParseMetadataDocument []";
	}
	
}
