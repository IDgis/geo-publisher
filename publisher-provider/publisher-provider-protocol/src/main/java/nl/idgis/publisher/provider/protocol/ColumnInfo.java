package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import nl.idgis.publisher.domain.service.Type;

/**
 * A column belonging to a {@link TableInfo} of a {@link VectorDatasetInfo}.
 * 
 * @author copierrj
 *
 */
public class ColumnInfo implements Serializable {

	private static final long serialVersionUID = 5239530430444929445L;

	private final String name;
	
	private final Type type;
	
	private final String alias;
	
	/**
	 * Creates a column.
	 * @param name the column name.
	 * @param type the data type.
	 * @param alias an arbitrary string to denote the meaning of this column. May be null.
	 */
	public ColumnInfo(String name, Type type, String alias) {
		this.name = Objects.requireNonNull(name, "name should not be null");
		this.type = Objects.requireNonNull(type, "type should not be null");
		this.alias = alias;
	}

	/**
	 * 
	 * @return the column name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return the data type
	 */
	public Type getType() {
		return type;
	}
	
	/**
	 * 
	 * @return the column alias
	 */
	public Optional<String> getAlias() {
		return Optional.ofNullable(alias);
	}

	@Override
	public String toString() {
		return "ColumnInfo [name=" + name + ", type=" + type + ", alias=" + alias + "]";
	}
}
