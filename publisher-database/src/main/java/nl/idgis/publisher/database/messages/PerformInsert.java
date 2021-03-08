package nl.idgis.publisher.database.messages;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.mysema.query.sql.RelationalPath;
import com.mysema.query.types.Path;

public class PerformInsert extends Query {

	private static final long serialVersionUID = -9193474668379032923L;

	private final RelationalPath<?> entity;
	
	private final List<PerformInsertBatch> batches;
	
	private final Path<?> key;
    
    public PerformInsert(RelationalPath<?> entity, List<PerformInsertBatch> batches, Optional<Path<?>> key) {
    	Objects.requireNonNull(entity);
    	
    	if(batches.isEmpty()) {
    		throw new IllegalArgumentException("batches list is empty");
    	}
    	
    	this.entity = entity;
    	this.batches = batches;
    	this.key = key.orElse(null);
    }

	public RelationalPath<?> getEntity() {
		return entity;
	}

	public List<PerformInsertBatch> getBatches() {
		return batches;
	}

	public Optional<Path<?>> getKey() {
		return Optional.ofNullable(key);
	}
	
	@Override
	public String toString() {
		return "PerformInsert [entity=" + entity + ", batches=" + batches
				+ ", key=" + key + "]";
	}
}
