package nl.idgis.publisher.service.manager;

import static com.mysema.query.types.PathMetadataFactory.forVariable;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QStyle.style;
import nl.idgis.publisher.database.QGenericLayer;

import com.mysema.query.sql.SQLCommonQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.expr.SimpleExpression;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

public class QGroupStructure extends EntityPathBase<QGroupStructure> {		
	
	private static final long serialVersionUID = -9048925641878000032L;
	
	public final static QGroupStructure groupStructure = new QGroupStructure("group_structure");
	
	public StringPath groupLayerIdentification = createString("group_layer_identification");

	public NumberPath<Integer> parentLayerId = createNumber("parent_layer_id", Integer.class);
	
	public StringPath parentLayerIdentification = createString("parent_layer_identification");
	
	public NumberPath<Integer> childLayerId = createNumber("child_layer_id", Integer.class);
	
	public StringPath childLayerIdentification = createString("child_layer_identification");
	
	public NumberPath<Integer> layerOrder = createNumber("layer_order", Integer.class);
	
	public StringPath styleIdentification = createString("style_identification");
	
	public StringPath styleName = createString("style_name");
	
	public StringPath path = createString("path");
	
	public BooleanPath cycle = createBoolean("cycle");
	
	public QGroupStructure(String variable) {
        super(QGroupStructure.class, forVariable(variable));
        
        add(groupLayerIdentification);
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
	
	@SuppressWarnings("unchecked")
	public static <U extends SQLCommonQuery<U>> U withGroupStructure (final U query, final QGenericLayer parent, final QGenericLayer child) {
		final SimpleExpression<String> pathElement = Expressions.template(
				String.class, 
				"'(' || {0} || ',' || {1} || ')'", 
				child.id, 
				parent.id);
			
		return query.withRecursive(groupStructure, 
			groupStructure.groupLayerIdentification,
			groupStructure.childLayerId, 
			groupStructure.childLayerIdentification,
			groupStructure.parentLayerId,
			groupStructure.parentLayerIdentification,
			groupStructure.layerOrder,
			groupStructure.styleIdentification,
			groupStructure.styleName,
			groupStructure.path,
			groupStructure.cycle).as(
			new SQLSubQuery().unionAll( 
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(genericLayer).on(genericLayer.id.eq(layerStructure.parentLayerId))
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
					.join(groupStructure).on(groupStructure.childLayerId.eq(layerStructure.parentLayerId)
						.and(groupStructure.cycle.not()))
					.leftJoin(style).on(style.id.eq(layerStructure.styleId))
					.list(
						groupStructure.groupLayerIdentification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder,
						style.identification,
						style.name,
						groupStructure.path
							.concat(pathElement),
						Expressions.template(
							Boolean.class, 
							"{0} like '%(' || {1} || ',' || {2} || ')%'", 
							groupStructure.path, 
							child.id, 
							parent.id))));
	}
}