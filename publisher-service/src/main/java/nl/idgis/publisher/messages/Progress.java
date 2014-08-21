package nl.idgis.publisher.messages;

import java.io.Serializable;

public class Progress implements Serializable {

	private static final long serialVersionUID = 1867944423512780564L;
	
	private final long count, totalCount;
	
	public Progress(long count, long totalCount) {
		this.count = count;
		this.totalCount = totalCount;
	}

	public long getCount() {
		return count;
	}

	public long getTotalCount() {
		return totalCount;
	}

	@Override
	public String toString() {
		return "Progress [count=" + count + ", totalCount=" + totalCount + "]";
	}
}
