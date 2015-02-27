package nl.idgis.publisher.service.manager;

import static com.mysema.query.types.PathMetadataFactory.forVariable;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QStyle.style;

import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;

import nl.idgis.publisher.utils.FutureUtils;

public abstract class AbstractServiceQuery<T> extends AbstractQuery<T> {

	protected final static QServiceStructure serviceStructure = new QServiceStructure("service_structure");
	
	protected static class QServiceStructure extends EntityPathBase<QServiceStructure> {		
		
		private static final long serialVersionUID = -9048925641878000032L;
		
		StringPath serviceIdentification = createString("service_identification");

		NumberPath<Integer> parentLayerId = createNumber("parent_layer_id", Integer.class);
		
		StringPath parentLayerIdentification = createString("parent_layer_identification");
		
		NumberPath<Integer> childLayerId = createNumber("child_layer_id", Integer.class);
		
		StringPath childLayerIdentification = createString("child_layer_identification");
		
		NumberPath<Integer> layerOrder = createNumber("layer_order", Integer.class);
		
		StringPath styleIdentification = createString("style_identification");
		
		QServiceStructure(String variable) {
	        super(QServiceStructure.class, forVariable(variable));
	        
	        add(serviceIdentification);
	        add(parentLayerId);
	        add(parentLayerIdentification);
	        add(childLayerId);
	        add(childLayerIdentification);
	        add(layerOrder);
	        add(styleIdentification);
	    }
	}
	
	protected final FutureUtils f;
	
	protected final AsyncHelper tx;
	
	protected final AsyncSQLQuery withServiceStructure;
	
	@SuppressWarnings("unchecked")
	protected AbstractServiceQuery(FutureUtils f, AsyncHelper tx) {
		this.f = f;
		this.tx = tx;
		
		withServiceStructure = tx.query().withRecursive(serviceStructure,
			serviceStructure.serviceIdentification,
			serviceStructure.childLayerId, 
			serviceStructure.childLayerIdentification,
			serviceStructure.parentLayerId,
			serviceStructure.parentLayerIdentification,
			serviceStructure.layerOrder,
			serviceStructure.styleIdentification).as(
			new SQLSubQuery().unionAll(
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(service).on(service.genericLayerId.eq(layerStructure.parentLayerId))
					.leftJoin(style).on(style.id.eq(layerStructure.styleId))
					.list(
						service.identification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder,
						style.identification),
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(serviceStructure).on(serviceStructure.childLayerId.eq(layerStructure.parentLayerId))
					.leftJoin(style).on(style.id.eq(layerStructure.styleId))
					.list(
						serviceStructure.serviceIdentification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder,
						style.identification)));
	}
}
