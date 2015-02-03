package nl.idgis.publisher.recorder.messages;

public class Wait extends RecorderCommand {

	private static final long serialVersionUID = 7681078230165498677L;
	
	private final int count;

	public Wait(int count) {
		this.count = count;
	}
	
	public int getCount() {
		return count;
	}

	@Override
	public String toString() {
		return "Wait [count=" + count + "]";
	}
}
