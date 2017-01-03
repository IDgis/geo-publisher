package nl.idgis.publisher.provider.sde;

public enum SDEItemInfoType {

	TABLE("{CD06BC3B-789D-4C51-AAFA-A467912B8965}"),
	FEATURE_CLASS("{70737809-852C-4A03-9E22-2CECEA5B9BFA}"),
	RASTER_DATASET("{5ED667A3-9CA9-44A2-8029-D95BF23704B9}");
	
	private final String uuid;
	
	SDEItemInfoType(String uuid) {
		this.uuid = uuid;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public static SDEItemInfoType fromUuid(String uuid) {
		for(SDEItemInfoType itemInfoType : values()) {
			if(itemInfoType.getUuid().equals(uuid)) {
				return itemInfoType;
			}
		}
		
		return null;
	}
}
