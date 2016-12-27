package nl.idgis.publisher.provider.sde;

import java.io.Serializable;
import java.util.Objects;

public class SDEItemInfo implements Serializable {

	private static final long serialVersionUID = -3424336595862989222L;

	private final String uuid;

	private final String physicalname;
	
	public SDEItemInfo(String uuid, String physicalname) {
		this.uuid = Objects.requireNonNull(uuid, "uuid should not be null");
		this.physicalname = Objects.requireNonNull(physicalname, "physicalname should not be null");
	}

	public String getUuid() {
		return uuid;
	}

	public String getPhysicalname() {
		return physicalname;
	}

	@Override
	public String toString() {
		return "SDEItemInfo [uuid=" + uuid + ", physicalname=" + physicalname + "]";
	}
}