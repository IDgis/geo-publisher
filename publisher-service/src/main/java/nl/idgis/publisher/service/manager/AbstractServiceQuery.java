package nl.idgis.publisher.service.manager;

import nl.idgis.publisher.utils.FutureUtils;
import akka.event.LoggingAdapter;

import com.mysema.query.sql.SQLCommonQuery;

public abstract class AbstractServiceQuery<T, U extends SQLCommonQuery<U>> extends AbstractQuery<T> {

	protected final FutureUtils f;
		
	protected final U withServiceStructure;
	
	protected AbstractServiceQuery(LoggingAdapter log, FutureUtils f, U query) {
		super(log);
		
		this.f = f;
		
		withServiceStructure = QServiceStructure.withServiceStructure (query, parent, child);
	}
}
