package nl.idgis.publisher.service.manager;

import static com.mysema.query.types.PathMetadataFactory.forVariable;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QStyle.style;

import com.mysema.query.sql.SQLCommonQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.expr.SimpleExpression;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.utils.FutureUtils;

public abstract class AbstractServiceQuery<T, U extends SQLCommonQuery<U>> extends AbstractQuery<T> {

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
		
		StringPath styleName = createString("style_name");
		
		StringPath path = createString("path");
		
		BooleanPath cycle = createBoolean("cycle");
		
		QServiceStructure(String variable) {
	        super(QServiceStructure.class, forVariable(variable));
	        
	        add(serviceIdentification);
	        add(parentLayerId);
	        add(parentLayerIdentification);
	        add(childLayerId);
	        add(childLayerIdentification);
	        add(layerOrder);
	        add(styleIdentification);
	        add(styleName);
	        add(path);
	        add(cycle);
	    }
	}
	
	protected final FutureUtils f;
		
	protected final U withServiceStructure;
	
	@SuppressWarnings("unchecked")
	protected AbstractServiceQuery(LoggingAdapter log, FutureUtils f, U query) {
		super(log);
		
		this.f = f;
		
		SimpleExpression<String> pathElement = Expressions.template(
			String.class, 
			"'(' || {0} || ',' || {1} || ')'", 
			child.id, 
			parent.id);
		
		withServiceStructure = query.withRecursive(serviceStructure,
			serviceStructure.serviceIdentification,
			serviceStructure.childLayerId, 
			serviceStructure.childLayerIdentification,
			serviceStructure.parentLayerId,
			serviceStructure.parentLayerIdentification,
			serviceStructure.layerOrder,
			serviceStructure.styleIdentification,
			serviceStructure.styleName,
			serviceStructure.path,
			serviceStructure.cycle).as(
			new SQLSubQuery().unionAll(
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(service).on(service.genericLayerId.eq(layerStructure.parentLayerId))
					.join(genericLayer).on(service.genericLayerId.eq(genericLayer.id))
					.leftJoin(style).on(style.id.eq(layerStructure.styleId))
					.list(
						genericLayer.identification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder,
						style.identification,
						style.name,
						pathElement,
						Expressions.template(Boolean.class, "false")),
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(serviceStructure).on(
						serviceStructure.childLayerId.eq(layerStructure.parentLayerId)
						.and(serviceStructure.cycle.not()))
					.leftJoin(style).on(style.id.eq(layerStructure.styleId))
					.list(
						serviceStructure.serviceIdentification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder,
						style.identification,
						style.name,
						serviceStructure.path
							.concat(pathElement),
						Expressions.template(
							Boolean.class, 
							"{0} like '%(' || {1} || ',' || {2} || ')%'", 
							serviceStructure.path, 
							child.id, 
							parent.id))));
	}
}
