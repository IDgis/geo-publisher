package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CompoundFilter implements Filter, Serializable {
	
	private static final long serialVersionUID = -4366437853827127531L;

	private final Filter[] filters;
	
	private final String operator;
	
	public CompoundFilter(String operator, Filter... filters) {
		if(filters.length < 1) {
			throw new IllegalArgumentException("at least 2 filter elements should be provided");
		}
		
		this.filters = filters;
		this.operator = Objects.requireNonNull(operator, "operator should not be null");
	}

	public String getOperator() {
		return operator;
	}	

	public List<Filter> getFilters() {
		return Arrays.asList(filters);
	}

	@Override
	public String toString() {
		return "CompoundFilter [filters=" + Arrays.toString(filters) + ", operator=" + operator + "]";
	}
}
