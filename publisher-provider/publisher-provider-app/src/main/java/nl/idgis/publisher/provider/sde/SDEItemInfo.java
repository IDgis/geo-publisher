package nl.idgis.publisher.provider.sde;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public class SDEItemInfo implements Serializable {

	private static final long serialVersionUID = -3424336595862989222L;

	private final String uuid;
	
	private final SDEItemInfoType type;

	private final String physicalname;
	
	private final String documentation;
	
	public SDEItemInfo(String uuid, SDEItemInfoType type, String physicalname, String documentation) {
		this.uuid = Objects.requireNonNull(uuid, "uuid should not be null");
		this.type = Objects.requireNonNull(type, "type should not be null");
		this.physicalname = Objects.requireNonNull(physicalname, "physicalname should not be null");
		this.documentation = documentation;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public SDEItemInfoType getType() {
		return type;
	}

	public String getPhysicalname() {

		String result;

		String[] schemaTableParts = physicalname.split("\\.");

		if (schemaTableParts.length > 2) { // includes db, don't return it.
			result = schemaTableParts[1] + "." + schemaTableParts[2];
		} else {
			result = physicalname;
		}

		return result;
	}
	
	public Optional<String> getDocumentation() {
		return Optional.ofNullable(documentation);
	}

	@Override
	public String toString() {
		return "SDEItemInfo [uuid=" + uuid + ", physicalname=" + physicalname + "]";
	}
}