package nl.idgis.publisher.domain.web;

public interface Selectable extends Nameable {
	
	Boolean published();
	boolean confidential ();
}
