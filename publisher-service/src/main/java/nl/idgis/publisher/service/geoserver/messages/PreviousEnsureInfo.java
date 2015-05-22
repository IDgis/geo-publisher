package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

public abstract class PreviousEnsureInfo implements Serializable {
	
	private static final long serialVersionUID = -8716804313556041060L;
	
	public abstract boolean isEnsuredBefore();
	
	public abstract Timestamp getEnsuredTime();
	
	public static PreviousEnsureInfo neverEnsured() {
		return new PreviousEnsureInfo() {
			
			private static final long serialVersionUID = -1143119722794684681L;

			@Override
			public boolean isEnsuredBefore() {
				return false;
			}
			
			@Override
			public Timestamp getEnsuredTime() {
				throw new IllegalStateException("never ensured before");
			}
		};
	}
	
	public static PreviousEnsureInfo ensured(Timestamp ensuredTime) {
		Objects.requireNonNull(ensuredTime);
		
		return new PreviousEnsureInfo() {

			private static final long serialVersionUID = 5065771238769412913L;

			@Override
			public boolean isEnsuredBefore() {
				return true;
			}

			@Override
			public Timestamp getEnsuredTime() {
				return ensuredTime;
			}
			
		};
	}
}
