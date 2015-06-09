package nl.idgis.publisher.domain.response;

import java.util.Objects;

public class UniqueNameValidationResult implements ValidationResult {

	private static final long serialVersionUID = -3800481429856190153L;
	
	public enum ConflictType {
		LAYER,
		LAYERGROUP,
		SERVICE
	}
	
	private final ConflictType conflictType;
	
	private UniqueNameValidationResult(ConflictType conflictType) {
		this.conflictType = conflictType;
	}
	
	public static UniqueNameValidationResult valid() {
		return new UniqueNameValidationResult(null);
	}
	
	public static UniqueNameValidationResult conflict(ConflictType conflictType) {
		return new UniqueNameValidationResult(Objects.requireNonNull(conflictType, "conflict type missing"));		
	}

	public boolean isValid() {
		return conflictType == null;
	}
	
	public ConflictType conflictType() {
		if(conflictType == null) {
			throw new IllegalStateException("no conflict");
		}
		
		return conflictType;
	}

	@Override
	public String toString() {
		return "UniqueNameValidationResult [conflictType=" + conflictType + "]";
	}
}
