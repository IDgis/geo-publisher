package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.util.List;

public class InfoList<T> implements Serializable {
	
	private static final long serialVersionUID = 1363875513636367843L;
	
	private final List<T> list;
	private final Long count;
	
	public InfoList(List<T> list, Long count) {
		this.list = list;
		this.count = count;
	}

	public List<T> getList() {
		return list;
	}

	public Long getCount() {
		return count;
	}

	@Override
	public String toString() {
		return "InfoList [list=" + list + ", count=" + count + "]";
	}
	
}
