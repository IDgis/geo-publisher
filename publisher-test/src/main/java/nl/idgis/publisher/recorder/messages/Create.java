package nl.idgis.publisher.recorder.messages;

import java.io.Serializable;

import akka.actor.Props;

public class Create implements Serializable {
	
	private static final long serialVersionUID = 4232830489454301619L;
	
	private final Props props;
	
	public Create(Props props) {
		this.props = props;
	}

	public Props getProps() {
		return props;
	}

	@Override
	public String toString() {
		return "Create [props=" + props + "]";
	}
}
