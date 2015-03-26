package nl.idgis.publisher.service.manager;

import org.xml.sax.InputSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.mysema.query.sql.SQLSubQuery;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;

import nl.idgis.publisher.service.manager.messages.Style;
import nl.idgis.publisher.utils.FutureUtils;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;

public class GetStylesQuery extends AbstractServiceQuery<List<Style>, AsyncSQLQuery> {
	
	private final String serviceId;

	GetStylesQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, String serviceId) {
		super(log, f, tx.query());
		
		this.serviceId = serviceId;
	}

	@Override
	CompletableFuture<List<Style>> result() {
		return withServiceStructure.from(style)
			.where(new SQLSubQuery().from(serviceStructure)
				.join(leafLayer).on(leafLayer.genericLayerId.eq(serviceStructure.childLayerId))
				.join(layerStyle).on(layerStyle.layerId.eq(leafLayer.id))
				.where(serviceStructure.serviceIdentification.eq(serviceId))
				.exists())
			.list(
				style.name,
				style.definition).thenCompose(result -> {
					try {
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						dbf.setNamespaceAware(true);
						
						DocumentBuilder db = dbf.newDocumentBuilder();
						
						return f.successful(result.list().stream()
							.map(t -> {
								try {
									return new Style(
										t.get(style.name),
										db.parse(new InputSource(new StringReader(t.get(style.definition)))));
								} catch(Exception e) {
									throw new IllegalArgumentException("couldn't parse style definition");
								}
							})
							.collect(Collectors.toList()));
					} catch(Exception e) {
						return f.failed(e);
					}
				});
	}

}
