package nl.idgis.publisher.recorder.messages;

import java.util.Optional;

public class Clear extends RecorderCommand {	

	private static final long serialVersionUID = 5149790923729597701L;
	
	private final Integer count;
	
	public Clear() {
		this.count = null;
	}
	
	public Clear(int count) {
		this.count = count;
	}

	public Optional<Integer> getCount() {
		return Optional.ofNullable(count);
	}

	@Override
	public String toString() {
		return "Clear [count=" + count + "]";
	}

}
