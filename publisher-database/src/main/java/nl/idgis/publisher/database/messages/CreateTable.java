package nl.idgis.publisher.database.messages;

import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class CreateTable extends Query {

	private static final long serialVersionUID = 4298412112357051184L;
	
	private final String name;
	private final List<Column> columns;
	
	public CreateTable(String name, List<Column> columns) {
		this.name = name;
		this.columns = columns;
	}

	public String getName() {
		return name;
	}

	public List<Column> getColumns() {
		return Collections.unmodifiableList(columns);
	}

	@Override
	public String toString() {
		return "CreateTable [name=" + name + ", columns=" + columns + "]";
	}
}
