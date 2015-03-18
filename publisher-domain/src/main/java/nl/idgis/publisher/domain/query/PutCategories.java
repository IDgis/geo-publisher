package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.Identifiable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.query.annotations.QueryProjection;

public final class PutCategories implements DomainQuery<Response<?>> {
	
	private static final long serialVersionUID = -6169209588295181957L;

	private final List<Category> categories;
	
	public PutCategories (final List<Category> categories) {

		this.categories = categories;
	}

	public List<Category> categories () {
		return this.categories;
	}
}
