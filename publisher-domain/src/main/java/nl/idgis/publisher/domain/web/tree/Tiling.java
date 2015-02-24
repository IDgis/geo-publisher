package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public interface Tiling {

	List<String> getMimeFormats();

	Integer getMetaWidth();

	Integer getMetaHeight();

	Integer getExpireCache();
	
	Integer getExpireClients();

	Integer getGutter();
}
