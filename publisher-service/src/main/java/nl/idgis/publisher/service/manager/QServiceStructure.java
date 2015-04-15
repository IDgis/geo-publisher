package nl.idgis.publisher.service.manager;

import static com.mysema.query.types.PathMetadataFactory.forVariable;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;

import nl.idgis.publisher.database.QGenericLayer;

import com.mysema.query.sql.SQLCommonQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.expr.SimpleExpression;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

public class QServiceStructure extends EntityPathBase<QServiceStructure> {		
	
	private static final long serialVersionUID = -9048925641878000032L;
	
	public final static QServiceStructure serviceStructure = new QServiceStructure("service_structure");
	
	public StringPath serviceIdentification = createString("service_identification");

	public NumberPath<Integer> parentLayerId = createNumber("parent_layer_id", Integer.class);
	
	public StringPath parentLayerIdentification = createString("parent_layer_identification");
	
	public NumberPath<Integer> childLayerId = createNumber("child_layer_id", Integer.class);
	
	public StringPath childLayerIdentification = createString("child_layer_identification");
	
	public NumberPath<Integer> layerOrder = createNumber("layer_order", Integer.class);
	
	public StringPath styleIdentification = createString("style_identification");
	
	public StringPath styleName = createString("style_name");
	
	public StringPath path = createString("path");
	
	public BooleanPath cycle = createBoolean("cycle");
	
	public NumberPath<Integer> datasetId = createNumber("dataset_id", Integer.class);
	
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
        add(datasetId);
    }
	
	@SuppressWarnings("unchecked")
	public static <U extends SQLCommonQuery<U>> U withServiceStructure (final U query, final QGenericLayer parent, final QGenericLayer child) {
		final SimpleExpression<String> pathElement = Expressions.template(
				String.class, 
				"'(' || {0} || ',' || {1} || ')'", 
				child.id, 
				parent.id);
		
		return query.withRecursive(serviceStructure,
				serviceStructure.serviceIdentification,
				serviceStructure.childLayerId, 
				serviceStructure.childLayerIdentification,
				serviceStructure.parentLayerId,
				serviceStructure.parentLayerIdentification,
				serviceStructure.layerOrder,
				serviceStructure.styleIdentification,
				serviceStructure.styleName,
				serviceStructure.path,
				serviceStructure.cycle,
				serviceStructure.datasetId).as(
				new SQLSubQuery().unionAll(
					new SQLSubQuery().from(layerStructure)
						.join(child).on(child.id.eq(layerStructure.childLayerId))
						.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
						.join(service).on(service.genericLayerId.eq(layerStructure.parentLayerId))
						.join(genericLayer).on(service.genericLayerId.eq(genericLayer.id))
						.leftJoin(leafLayer).on(leafLayer.genericLayerId.eq(child.id))
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
							Expressions.template(Boolean.class, "false"),
							leafLayer.datasetId),
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
								parent.id),
							serviceStructure.datasetId)));
	}
}