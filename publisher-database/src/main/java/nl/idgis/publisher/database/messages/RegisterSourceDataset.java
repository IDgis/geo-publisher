package nl.idgis.publisher.database.messages;

public class RegisterSourceDataset extends Query {
	
	private static final long serialVersionUID = -2882277132031271108L;
	
	private final String dataSource, id, name;
	
	public RegisterSourceDataset(String dataSource, String id, String name) {
		this.dataSource = dataSource;
		this.id = id;
		this.name = name;
	}

	public String getDataSource() {
		return dataSource;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "RegisterSourceDataset [dataSource="
				+ dataSource + ", id="
				+ id + ", name=" + name + "]";
	}
}
