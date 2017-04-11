package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public interface Service {

	String getId();
	
	String getName();
	
	String getTitle();
	
	String getAbstract();
	
	String getContact();

	String getOrganization();

	String getPosition();

	String getAddressType();

	String getAddress();

	String getCity();

	String getState();

	String getZipcode();

	String getCountry();

	String getTelephone();

	String getFax();

	String getEmail();
	
	List<String> getKeywords();	
	
	String getRootId();
	
	List<LayerRef<? extends Layer>> getLayers();
	
	boolean isConfidential ();
	
	boolean isWmsOnly();
}
