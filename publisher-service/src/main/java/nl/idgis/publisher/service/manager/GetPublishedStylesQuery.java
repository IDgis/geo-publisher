package nl.idgis.publisher.service.manager;

import org.xml.sax.InputSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;

import nl.idgis.publisher.service.manager.messages.Style;
import nl.idgis.publisher.utils.FutureUtils;

import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QPublishedServiceStyle.publishedServiceStyle;

public class GetPublishedStylesQuery extends AbstractQuery<List<Style>> {
	
	private final FutureUtils f;
	
	private final AsyncHelper tx;
	
	private final String serviceId;

	GetPublishedStylesQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, String serviceId) {
		super(log);
		
		this.f = f;
		this.tx = tx;		
		this.serviceId = serviceId;
	}

	@Override
	CompletableFuture<List<Style>> result() {
		return tx.query().from(publishedServiceStyle)
			.join(service).on(service.id.eq(publishedServiceStyle.serviceId))
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(serviceId))
			.list(
				publishedServiceStyle.name,
				publishedServiceStyle.definition).thenCompose(result -> {
					try {
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						dbf.setNamespaceAware(true);
						
						DocumentBuilder db = dbf.newDocumentBuilder();
						
						return f.successful(result.list().stream()
							.map(t -> {
								try {
									return new Style(
										t.get(publishedServiceStyle.name),
										db.parse(new InputSource(new StringReader(t.get(publishedServiceStyle.definition)))));
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
