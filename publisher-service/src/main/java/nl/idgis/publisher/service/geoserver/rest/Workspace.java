package nl.idgis.publisher.service.geoserver.rest;

public class Workspace {	
	
	private final String name;	

	public Workspace(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Workspace [name=" + name + "]";
	}
	
}
