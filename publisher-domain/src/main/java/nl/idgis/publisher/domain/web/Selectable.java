package nl.idgis.publisher.domain.web;

public interface Selectable extends Nameable {
	
	boolean confidential ();
	
	boolean wmsOnly ();
}
