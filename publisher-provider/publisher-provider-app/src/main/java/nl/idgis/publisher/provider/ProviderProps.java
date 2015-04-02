package nl.idgis.publisher.provider;

import akka.actor.Props;

public class ProviderProps {

	private final String name;
	
	private final Props props;
	
	public ProviderProps(String name, Props props) {
		this.name = name;
		this.props = props;
	}

	public String getName() {
		return name;
	}

	public Props getProps() {
		return props;
	}

	@Override
	public String toString() {
		return "ProviderProps [name=" + name + ", props=" + props + "]";
	}
}
