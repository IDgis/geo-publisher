package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QStyle.style;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.domain.query.GetStyleParentGroups;
import nl.idgis.publisher.domain.query.GetStyleParentLayers;
import nl.idgis.publisher.domain.query.ListStyles;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.QStyle;
import nl.idgis.publisher.domain.web.Style;
import akka.actor.ActorRef;
import akka.actor.Props;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.expr.BooleanExpression;

public class StyleAdmin extends AbstractAdmin {
	
	public StyleAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(StyleAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		doList(Style.class, this::handleListStyles);
		doGet(Style.class, this::handleGetStyle);
		doPut(Style.class, this::handlePutStyle);
		doDelete(Style.class, this::handleDeleteStyle);
		doQuery (ListStyles.class, this::handleListStylesWithQuery);
		doQuery (GetStyleParentLayers.class, this::handleGetStyleParentLayers);
		doQuery (GetStyleParentGroups.class, this::handleGetStyleParentGroups);
	}

	private CompletableFuture<Page<Style>> handleListStyles () {
		return handleListStylesWithQuery (new ListStyles (null, null));
	}
	
	private CompletableFuture<Page<Style>> handleListStylesWithQuery (final ListStyles listStyles) {
		final BooleanExpression styleInUse = new SQLSubQuery ()
			.from (layerStyle)
			.where (layerStyle.styleId.eq (style.id))
			.exists ();
		
		final AsyncSQLQuery baseQuery = db
			.query()
			.from(style)
			.orderBy (style.name.asc ());

		if (listStyles.getQuery () != null) {
			baseQuery.where (style.name.containsIgnoreCase (listStyles.getQuery ()));
		}
		
		final AsyncSQLQuery listQuery = baseQuery.clone ();
		
		singlePage (listQuery, listStyles.getPage ());
		
		return baseQuery
			.count ()
			.thenCompose ((count) -> {
				final Page.Builder<Style> builder = new Page.Builder<> ();
				
				addPageInfo (builder, listStyles.getPage (), count);
				
				return listQuery
					.list (new QStyle (style.identification, style.name, Expressions.constant(""), style.styleType, styleInUse))
					.thenApply ((styles) -> {
						builder.addAll (styles.list ());
						return builder.build ();
					});
			});
	}

	
	private CompletableFuture<Optional<Style>> handleGetStyle (String styleId) {
		log.debug ("handleGetStyle: " + styleId);
		
		return 
			db.query().from(style)
			.leftJoin(layerStyle).on(style.id.eq(layerStyle.styleId)).distinct()
			.where(style.identification.eq(styleId))
			.singleResult(new nl.idgis.publisher.domain.web.QStyle(style.identification, style.name, style.definition,style.styleType, layerStyle.styleId.isNotNull()));		
	}
	
	private CompletableFuture<Response<?>> handlePutStyle(Style theStyle) {
		String styleId = theStyle.id();
		String styleName = theStyle.name();
		log.debug ("handle update/create style: " + styleId);
		
		return db.transactional(tx ->
			// Check if there is another style with the same id
			tx.query().from(style)
			.where(style.identification.eq(styleId))
			.singleResult(style.identification)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// INSERT
					log.debug("Inserting new style with name: " + styleName);
					
					String newStyleId = UUID.randomUUID().toString();
					
					return tx.insert(style)
					.set(style.identification, newStyleId)
					.set(style.name, styleName)
					.set(style.definition, theStyle.definition())
					.set(style.styleType, theStyle.styleType().name())
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, newStyleId));
				} else {
					// UPDATE
					log.debug("Updating style with name: " + styleName);
					return tx.update(style)
					.set(style.name, styleName)
					.set(style.definition, theStyle.definition())
					.set(style.styleType, theStyle.styleType().name())
					.where(style.identification.eq(styleId))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, styleId));
				}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteStyle(String styleId) {
		log.debug ("handleDeleteStyle: " + styleId);
		return db.delete(style)
			.where(style.identification.eq(styleId))
			.execute()
			.thenApply(l -> new Response<String>(CrudOperation.DELETE, CrudResponse.OK, styleId));
	}
	
	private CompletableFuture<Page<Layer>> handleGetStyleParentLayers(final GetStyleParentLayers getStyleParentLayers){
		String styleId = getStyleParentLayers.getId();
		log.debug ("handleGetStyleParentLayers: " + styleId);
		final Page.Builder<Layer> builder = new Page.Builder<> ();
		
		final AsyncSQLQuery getLayersFromLayerStyleQuery =  
			db.query().from(genericLayer)
			.join(leafLayer).on(leafLayer.genericLayerId.eq(genericLayer.id))
			.join(layerStyle).on(layerStyle.layerId.eq(leafLayer.id))
			.join(style).on(style.id.eq(layerStyle.styleId))
			.where(style.identification.eq(styleId))
			.orderBy(genericLayer.name.asc());
		
			return getLayersFromLayerStyleQuery.list(
				genericLayer.identification,
				genericLayer.name
				).thenApply(styleLayers -> {
					for (Tuple layer : styleLayers.list()) {
						builder.add(new Layer(
							layer.get(genericLayer.identification),
							layer.get(genericLayer.name),
							null,
							null,
							null,
							null,
							null,
							null, null, null, false)
						);
					}
					return builder.build();
				});
	}
	
	private CompletableFuture<Page<LayerGroup>> handleGetStyleParentGroups(final GetStyleParentGroups getStyleParentGroups){
		String styleId = getStyleParentGroups.getId();
		log.debug ("handleGetStyleParentGroups: " + styleId);
		final Page.Builder<LayerGroup> builder = new Page.Builder<> ();
		
		final AsyncSQLQuery getLayersFromLayerStructureQuery =  
				db.query()
				.from(genericLayer)
				.where(genericLayer.id.in(
					new SQLSubQuery()
						.from(layerStructure)
						.join(genericLayer).on(genericLayer.id.eq(layerStructure.parentLayerId))
						.join(style).on(style.id.eq(layerStructure.styleId))
						.where(style.identification.eq(styleId))
						.list(layerStructure.parentLayerId)
				))
				.orderBy(genericLayer.name.asc());
		
			return getLayersFromLayerStructureQuery.list(
				genericLayer.identification,
				genericLayer.name
				).thenApply(structLayers -> {
					for (Tuple group : structLayers.list()) {
						builder.add(new LayerGroup(
								group.get(genericLayer.identification),
								group.get(genericLayer.name),
								null,
								null,
								null,
								null,
								false
								));
					}
					return builder.build();
				});
	}
	

}
