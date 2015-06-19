package nl.idgis.publisher.monitoring.tester.messages;

import java.io.Serializable;

public class StatusReport implements Serializable {

	private static final long serialVersionUID = 2753057491491759482L;
	
	private final long currentCount, failureCount, totalCount;
	
	public StatusReport(long currentCount, long failureCount, long totalCount) {
		this.currentCount = currentCount;
		this.failureCount = failureCount;
		this.totalCount = totalCount;
	}

	public long getCurrentCount() {
		return currentCount;
	}

	public long getFailureCount() {
		return failureCount;
	}

	public long getTotalCount() {
		return totalCount;
	}

	@Override
	public String toString() {
		return "StatusReport [currentCount=" + currentCount + ", failureCount="
				+ failureCount + ", totalCount=" + totalCount + "]";
	}
}
