package nl.idgis.publisher.service.geoserver.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.StreamUtils;
import nl.idgis.publisher.utils.XMLUtils.XPathHelper;
import static nl.idgis.publisher.utils.XMLUtils.xpath;

public class DefaultGeoServerRest implements GeoServerRest {
	
	private static final String RECURSE = "?recurse=true";
	
	private static final Set<String> DEFAULT_STYLE_NAMES = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(
			"generic",
			"line",
			"point",
			"polygon",
			"raster")));
	
	private final LoggingAdapter log;

	private final String authorization;
	
	private final String restLocation;
	
	private final String serviceLocation;
	
	private final FutureUtils f;
	
	private final AsyncHttpClient asyncHttpClient;
	
	public DefaultGeoServerRest(FutureUtils f, LoggingAdapter log, String serviceLocation, String user, String password) throws Exception {
		this.f = f;
		this.log = log;
		this.restLocation = serviceLocation + "rest/";
		this.serviceLocation = serviceLocation;
		this.authorization = "Basic " + new String(Base64.encodeBase64((user + ":" + password).getBytes()));
		
		asyncHttpClient = new AsyncHttpClient();
	}
	
	private CompletableFuture<Optional<Document>> get(String path) {
		return get(path, true, false);
	}
	
	private CompletableFuture<Optional<Document>> get(String path, boolean appendSuffix, boolean namespaceAware) {
		String url = path + (appendSuffix ? ".xml" : "");
		log.debug("fetching {}", url);
		
		CompletableFuture<Optional<Document>> future = new CompletableFuture<>();
		
		asyncHttpClient.prepareGet(url)
			.addHeader("Authorization", authorization)
			.execute(new AsyncCompletionHandler<Response>() {

			@Override
			public Response onCompleted(Response response) throws Exception {
				try {
					int responseCode = response.getStatusCode();
					if(responseCode == HttpURLConnection.HTTP_OK) {
						byte[] content = response.getResponseBodyAsBytes();
						
						try {
							Document document = parse(new ByteArrayInputStream(content), namespaceAware);							
							future.complete(Optional.of(document));
						} catch(Exception e) {
							log.error("xml repair was required in order to parse broken api response: {}", path);
							
							XMLInputFactory xif = XMLInputFactory.newInstance();
							XMLEventReader reader = xif.createXMLEventReader(new ByteArrayInputStream(content));
							
							ByteArrayOutputStream fixedDocument = new ByteArrayOutputStream();
							
							XMLOutputFactory xof = XMLOutputFactory.newInstance();
							XMLEventWriter writer = xof.createXMLEventWriter(fixedDocument);
							
							try {
								while(reader.hasNext()) {
									writer.add(reader.nextEvent());
								}
							} catch(Exception se) {}
							
							writer.close();
							
							Document document = parse(new ByteArrayInputStream(fixedDocument.toByteArray()), namespaceAware);
							future.complete(Optional.of(document));
						}
					} else if(responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
						future.complete(Optional.empty());
					} else {
						future.completeExceptionally(new GeoServerException(path, responseCode));
					}
				} catch(Exception e) {
					future.completeExceptionally(new GeoServerException(path, e));
				}
				
				return response;
			}			
			
			@Override
		    public void onThrowable(Throwable t){
				future.completeExceptionally(t);
		    }
			
		});
		
		return future;
	}
	
	private CompletableFuture<Void> delete(String path) {
		log.debug("deleting {}", path);
		
		CompletableFuture<Void> future = new CompletableFuture<>();
		
		asyncHttpClient.prepareDelete(path)
			.addHeader("Authorization", authorization)
			.execute(new AsyncCompletionHandler<Response>() {

				@Override
				public Response onCompleted(Response response) throws Exception {
					int responseCode = response.getStatusCode();					
					if(responseCode != HttpURLConnection.HTTP_OK) {	
						future.completeExceptionally(new GeoServerException(path, responseCode));
					} else {
						future.complete(null);
					}
					
					return response;
				}
				
				@Override
			    public void onThrowable(Throwable t){
					future.completeExceptionally(new GeoServerException(path, t));
			    }
				
			});
		
		return future;
	}
	
	private CompletableFuture<Void> put(String path, byte[] document) {
		return put(path, document, "text/xml");
	}
	
	private CompletableFuture<Void> put(String path, byte[] document, String contentType) {		
		log.debug("put {}", path);
		
		CompletableFuture<Void> future = new CompletableFuture<>();
		
		asyncHttpClient.preparePut(path)
			.addHeader("Authorization", authorization)
			.addHeader("Content-type", contentType)
			.setBody(document)
			.execute(new AsyncCompletionHandler<Response>() {

				@Override
				public Response onCompleted(Response response) throws Exception {
					int responseCode = response.getStatusCode();
					if(responseCode != HttpURLConnection.HTTP_OK) {
						future.completeExceptionally(new GeoServerException(path, responseCode));
					} else {
						future.complete(null);
					}

					return response;
				}
				
				@Override
			    public void onThrowable(Throwable t){
					future.completeExceptionally(new GeoServerException(path, t));
			    }
			});
		
		return future;
	}
	
	private CompletableFuture<Void> post(String path, byte[] document, int expectedResponseCode) {
		return post(path, document, "text/xml", expectedResponseCode);
	}
	
	private CompletableFuture<Void> post(String path, byte[] document) {
		return post(path, document, "text/xml");
	}
	
	private CompletableFuture<Void> post(String path, byte[] document, String contentType) {
		return post(path, document, contentType, HttpURLConnection.HTTP_CREATED);
	}
	
	private CompletableFuture<Void> post(String path, byte[] document, String contentType, int expectedResponseCode) {		
		log.debug("posting {}", path);
		
		CompletableFuture<Void> future = new CompletableFuture<>();
		
		asyncHttpClient.preparePost(path)
			.addHeader("Authorization", authorization)
			.addHeader("Content-type", contentType)
			.setBody(document)
			.execute(new AsyncCompletionHandler<Response>() {

				@Override
				public Response onCompleted(Response response) throws Exception {
					int responseCode = response.getStatusCode();
					if(responseCode != expectedResponseCode) {	
						future.completeExceptionally(new GeoServerException(path, responseCode));
					} else {
						future.complete(null);
					}

					return response;
				}
				
				@Override
			    public void onThrowable(Throwable t){
					future.completeExceptionally(new GeoServerException(path, t));
			    }
			});
		
		return future;
	}
	
	private String getServiceSettingsPath(Workspace workspace, ServiceType serviceType) {
		return restLocation + "services/" + serviceType.name().toLowerCase() + "/workspaces/" 
			+ workspace.getName() + "/settings";
	}
	
	@Override
	public CompletableFuture<Void> putServiceSettings(Workspace workspace, ServiceType serviceType, ServiceSettings serviceSettings) {
		try {
			String serviceTypeName = serviceType.name().toLowerCase();
			
			// we have to use the default settings as a template otherwise all kind of 
			// essential settings are set to null, causing NPE when using the service.
			InputStream defaultServiceSettings =
				getClass().getResourceAsStream(
					"default-" + serviceTypeName + "-settings.xml");
			
			Objects.requireNonNull(defaultServiceSettings);			
			
			Document document = parse(defaultServiceSettings);
			
			XPath xpath = XPathFactory.newInstance().newXPath();
					
			String title = serviceSettings.getTitle();
			if(title != null) {
				Node titleNode = (Node)xpath.evaluate(serviceTypeName + "/title", document, XPathConstants.NODE);
				titleNode.setTextContent(title);
			}
			
			String abstr = serviceSettings.getAbstract();
			if(abstr != null) {
				Node abstractNode = (Node)xpath.evaluate(serviceTypeName + "/abstrct", document, XPathConstants.NODE);
				abstractNode.setTextContent(abstr);
			}
			
			List<String> keywords = serviceSettings.getKeywords();
			if(keywords != null) {
				Node keywordsNode = (Node)xpath.evaluate(serviceTypeName + "/keywords", document, XPathConstants.NODE);
				
				for(String keyword : keywords) {
					Node keywordNode = document.createElement("keyword");
					keywordNode.appendChild(document.createTextNode(keyword));					
					keywordsNode.appendChild(keywordNode);
				}
			}
			
			return put(getServiceSettingsPath(workspace, serviceType), serialize(document));
		} catch(Exception e) {
			return f.failed(e);
		}
	}
	
	@Override
	public CompletableFuture<Void> postWorkspace(Workspace workspace) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			
			XMLOutputFactory of = XMLOutputFactory.newInstance();
			XMLStreamWriter sw = of.createXMLStreamWriter(os);
			sw.writeStartDocument();
				sw.writeStartElement("workspace");
					sw.writeStartElement("name");
						sw.writeCharacters(workspace.getName());
					sw.writeEndElement();
				sw.writeEndElement();
			sw.writeEndDocument();
			sw.close();
			
			os.close();
			
			return post(getWorkspacesPath(), os.toByteArray());
		} catch(Exception e) {
			return f.failed(e);
		}
	}	
	
	private String getWorkspacePath(String workspaceId) {
		return getWorkspacesPath() + "/" + workspaceId.replace(".", "%2E");
	}
	
	private String getWorkspacePath(Workspace workspace) {
		return getWorkspacePath(workspace.getName());
	}

	private String getWorkspacesPath() {
		return restLocation + "workspaces";
	}
	
	@Override
	public CompletableFuture<List<Workspace>> getWorkspaces() {
		return get(getWorkspacesPath()).thenApply(optionalDocument ->
			xpath(optionalDocument.get())
				.map("/workspaces/workspace/name", name -> new Workspace(name.string().get())));
	}
	
	@Override
	public CompletableFuture<Optional<DataStore>> getDataStore(Workspace workspace, String dataStoreName) {
		return get(getDataStorePath(workspace, dataStoreName)).thenApply(optionalDocument ->
			optionalDocument.map(document -> {
				XPathHelper dataStore = xpath(document).node("dataStore").get();
				String name = dataStore.string("name").get();
				
				Map<String, String> connectionParameters = 
					dataStore.nodes("connectionParameters/entry").stream()
						.collect(Collectors.toMap(
							entry -> entry.string("@key").get(),
							entry -> entry.string().get()));
				
				return new DataStore(name, connectionParameters);
			}));		
	}

	private String getDataStorePath(Workspace workspace, String dataStoreName) {
		return getDataStoresPath(workspace) + "/" + dataStoreName;
	}
	
	private <T> T optionalPresent(Optional<T> optional) {
		return optional.get();
	}

	@Override
	public CompletableFuture<List<DataStore>> getDataStores(Workspace workspace) {
		return get(getDataStoresPath(workspace)).thenCompose(optionalDocument ->
			f.sequence(
				xpath(optionalDocument.get()).map("/dataStores/dataStore/name", 
					name -> getDataStore(workspace, name.string().get())
						.thenApply(this::optionalPresent))));
	}
	
	private CompletableFuture<Optional<CoverageStore>> getCoverageStore(Workspace workspace, String coverageStoreName) {
		return get(getCoverageStorePath(workspace, coverageStoreName)).thenApply(optionalDocument ->
			optionalDocument.map(document -> {
				XPathHelper coverageStore = xpath(document).node("coverageStore").get();
				try {
					return new CoverageStore(
						coverageStore.string("name").get(),
						coverageStore.string("url").get());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}));
	}
	
	public CompletableFuture<List<CoverageStore>> getCoverageStores(Workspace workspace) {
		return get(getCoverageStoresPath(workspace)).thenCompose(optionalDocument ->
			f.sequence(
				xpath(optionalDocument.get()).map("/coverageStores/coverageStore/name",
					name -> getCoverageStore(workspace, name.string().get())
						.thenApply(this::optionalPresent))));
	}
	
	private String getCoverageStoresPath(Workspace workspace) {
		return getWorkspacesPath() + "/" + workspace.getName() + "/coveragestores";
	}
	
	@Override
	public CompletableFuture<Void> postCoverageStore(Workspace workspace, CoverageStore coverageStore) {
		try {
			return post(getCoverageStoresPath(workspace), getCoverageStoreDocument(workspace, coverageStore));
		} catch(Exception e) {
			return f.failed(e);
		}
	}
	
	@Override
	public CompletableFuture<Void> putCoverageStore(Workspace workspace, CoverageStore coverageStore) {
		try {
			return put(getCoverageStoresPath(workspace), getCoverageStoreDocument(workspace, coverageStore));
		} catch(Exception e) {
			return f.failed(e);
		}
	}

	private byte[] getCoverageStoreDocument(Workspace workspace, CoverageStore coverageStore) throws FactoryConfigurationError, XMLStreamException, IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		XMLOutputFactory of = XMLOutputFactory.newInstance();
		XMLStreamWriter sw = of.createXMLStreamWriter(os);
		sw.writeStartDocument();
		sw.writeStartElement("coverageStore");
			sw.writeStartElement("name");
				sw.writeCharacters(coverageStore.getName());
			sw.writeEndElement();
			sw.writeStartElement("type");
				sw.writeCharacters("GeoTIFF");
			sw.writeEndElement();
			// a coverage store is disabled by default
			sw.writeStartElement("enabled");
				sw.writeCharacters("true");
			sw.writeEndElement();
			// workspace name in url is apparently not enough: 
			// omitting the workspace here results in an exception
			sw.writeStartElement("workspace");
				sw.writeStartElement("name");
					sw.writeCharacters(workspace.getName());
				sw.writeEndElement();
			sw.writeEndElement();
			sw.writeStartElement("url");
				sw.writeCharacters(coverageStore.getUrl());
			sw.writeEndElement();
		sw.writeEndElement();
		sw.writeEndDocument();
		sw.close();
		
		os.close();
		
		return os.toByteArray();
	}

	@Override
	public CompletableFuture<Void> postDataStore(Workspace workspace, DataStore dataStore) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			
			XMLOutputFactory of = XMLOutputFactory.newInstance();
			XMLStreamWriter sw = of.createXMLStreamWriter(os);
			sw.writeStartDocument();
			sw.writeStartElement("dataStore");
				sw.writeStartElement("name");
					sw.writeCharacters(dataStore.getName());
				sw.writeEndElement();
				sw.writeStartElement("connectionParameters");
					for(Map.Entry<String, String> connectionParameter : dataStore.getConnectionParameters().entrySet()) {
						sw.writeStartElement("entry");
						sw.writeAttribute("key", connectionParameter.getKey());
						sw.writeCharacters(connectionParameter.getValue());
						sw.writeEndElement();
					}
				sw.writeEndElement();
			sw.writeEndDocument();
			sw.close();
			
			os.close();
			
			return post(getDataStoresPath(workspace), os.toByteArray());			
		} catch(Exception e) {
			return f.failed(e);
		}
	}

	private String getDataStoresPath(Workspace workspace) {
		return getWorkspacesPath() + "/" + workspace.getName() + "/datastores";
	}
	
	private String getFeatureTypePath(Workspace workspace, DataStore dataStore, FeatureType featureType) {
		return getFeatureTypePath(workspace, dataStore, featureType.getName());
	}
	
	private String getFeatureTypePath(Workspace workspace, DataStore dataStore, String featureTypeName) {
		return getFeatureTypesPath(workspace, dataStore) + "/" + featureTypeName;
	}
	
	private CompletableFuture<Optional<FeatureType>> getFeatureType(Workspace workspace, DataStore dataStore, String featureTypeName) {
		return get(getFeatureTypePath(workspace, dataStore, featureTypeName)).thenApply(optionalDocument ->
			optionalDocument.map(document -> {										
				XPathHelper featureType = xpath(document).node("featureType").get();
				return new FeatureType(
					featureType.string("name").get(),
					featureType.string("nativeName").get(),
					featureType.stringOrNull("title"),
					featureType.stringOrNull("abstract"),
					featureType.strings("keywords/string"),
					Collections.unmodifiableList(
						featureType.map("metadataLinks/metadataLink",
							metadataLink -> new MetadataLink(
								metadataLink.string("type").get(), 
								metadataLink.string("metadataType").get(), 
								metadataLink.string("content").get()))),
					Collections.unmodifiableList(
						featureType.map("attributes/attribute/name", 
							name -> new Attribute(name.string().get()))));
			}));
	}
	
	@Override
	public CompletableFuture<List<FeatureType>> getFeatureTypes(Workspace workspace, DataStore dataStore) {
		return get(getFeatureTypesPath(workspace, dataStore)).thenCompose(optionalDocument ->
			f.sequence(
				xpath(optionalDocument.get()).map("/featureTypes/featureType/name", name ->
					getFeatureType(workspace, dataStore, name.string().get()).thenApply(this::optionalPresent))));
	}
	
	private CompletableFuture<Optional<Coverage>> getCoverage(Workspace workspace, CoverageStore coverageStore, String coverageName) {
		return get(getCoveragePath(workspace, coverageStore, coverageName)).thenApply(optionalDocument ->
			optionalDocument.map(document -> {
				XPathHelper coverage = xpath(document).node("coverage").get();				
				return new Coverage(
					coverage.string("name").get(),
					coverage.string("nativeName").get(),
					coverage.string("title").orElse(null),
					coverage.string("abstract").orElse(null),
					coverage.strings("keywords/string"),
					Collections.unmodifiableList(
						coverage.map("metadataLinks/metadataLink",
							metadataLink -> new MetadataLink(
								metadataLink.string("type").get(), 
								metadataLink.string("metadataType").get(), 
								metadataLink.string("content").get()))));
			}));
	}
	
	@Override
	public CompletableFuture<List<Coverage>> getCoverages(Workspace workspace, CoverageStore coverageStore) {
		return get(getCoveragesPath(workspace, coverageStore)).thenCompose(optionalDocument ->
			f.sequence(
				xpath(optionalDocument.get()).map("/coverages/coverage/name", name ->
					getCoverage(workspace, coverageStore, name.string().get()).thenApply(this::optionalPresent))));
	}
	
	@Override
	public CompletableFuture<Map<CoverageStore, List<Coverage>>> getCoverages(Workspace workspace) {
		return getCoverageStores(workspace).thenCompose(coverageStores ->
			f.sequence(coverageStores.stream()
				.map(coverageStore -> getCoverages(workspace, coverageStore))
				.collect(Collectors.toList())).thenApply(coverages ->
					StreamUtils.zipToMap(
						coverageStores.stream(), 
						coverages.stream())));
	}
	
	@Override
	public CompletableFuture<Void> putFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType) {
		try {
			return put(getFeatureTypePath(workspace, dataStore, featureType), getFeatureTypeDocument(featureType));
		} catch(Exception e) {			
			return f.failed(e);
		}
	}

	private byte[] getFeatureTypeDocument(FeatureType featureType) throws XMLStreamException, IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		XMLOutputFactory of = XMLOutputFactory.newInstance();
		XMLStreamWriter sw = of.createXMLStreamWriter(os);
		sw.writeStartDocument();
		sw.writeStartElement("featureType");
			writeDatasetInfo(featureType, sw);
			
			List<Attribute> attributes = featureType.getAttributes();				
			
			sw.writeStartElement("attributes");
				for(Attribute attribute : attributes) {
					sw.writeStartElement("attribute");
						sw.writeStartElement("name");
							sw.writeCharacters(attribute.getName());
						sw.writeEndElement();
					sw.writeEndElement();
				}
			sw.writeEndElement();
		sw.writeEndElement();
		sw.writeEndDocument();
		sw.close();
		
		os.close();
		
		return os.toByteArray();		
	}

	private void writeDatasetInfo(Dataset dataset, XMLStreamWriter sw) throws XMLStreamException {
		sw.writeStartElement("name");
			sw.writeCharacters(dataset.getName());
		sw.writeEndElement();
		
		String nativeName = dataset.getNativeName();				
		sw.writeStartElement("nativeName");
			sw.writeCharacters(nativeName);
		sw.writeEndElement();				
		
		String title = dataset.getTitle();
		if(title != null) {
			sw.writeStartElement("title");
				sw.writeCharacters(title);
			sw.writeEndElement();
		}
		
		String abstr = dataset.getAbstract();
		if(abstr != null) {
			sw.writeStartElement("abstract");
				sw.writeCharacters(abstr);
			sw.writeEndElement();
		}
		
		sw.writeStartElement("keywords");
		List<String> keywords = dataset.getKeywords();
		for(String keyword : keywords) {
			sw.writeStartElement("strings");
				sw.writeCharacters(keyword);
			sw.writeEndElement();
		}
		sw.writeEndElement();
		
		sw.writeStartElement("metadataLinks");
		List<MetadataLink> metadataLinks = dataset.getMetadataLinks();
		for(MetadataLink metadataLink : metadataLinks) {
			sw.writeStartElement("metadataLink");
				sw.writeStartElement("type");
					sw.writeCharacters(metadataLink.getType());
				sw.writeEndElement();
				sw.writeStartElement("metadataType");
					sw.writeCharacters(metadataLink.getMetadataType());
				sw.writeEndElement();
				sw.writeStartElement("content");
					sw.writeCharacters(metadataLink.getContent());
				sw.writeEndElement();
			sw.writeEndElement();
		}
		sw.writeEndElement();
		
		sw.writeStartElement("enabled");
			sw.writeCharacters("true");
		sw.writeEndElement();
	}
	
	private String getCoverageStorePath(Workspace workspace, String coverageStoreName) {
		return getCoverageStoresPath(workspace) + "/" + coverageStoreName;
	}
	
	private String getCoverageStorePath(Workspace workspace, CoverageStore coverageStore) {
		return getCoverageStorePath(workspace, coverageStore.getName());
	}
	
	private String getCoveragesPath(Workspace workspace, CoverageStore coverageStore) {
		return getCoverageStorePath(workspace, coverageStore) + "/coverages";
	}
	
	private String getCoveragePath(Workspace workspace, CoverageStore coverageStore, Coverage coverage) {
		return getCoveragePath(workspace, coverageStore, coverage.getName());
	}
	
	private String getCoveragePath(Workspace workspace, CoverageStore coverageStore, String coverageName) {
		return getCoveragesPath(workspace, coverageStore) + "/" + coverageName;
	}
	
	private byte[] getCoverageDocument(Coverage coverage) throws XMLStreamException, IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		XMLOutputFactory of = XMLOutputFactory.newInstance();
		XMLStreamWriter sw = of.createXMLStreamWriter(os);
		sw.writeStartDocument();
		sw.writeStartElement("coverage");
			writeDatasetInfo(coverage, sw);
		sw.writeEndElement();
		sw.writeEndDocument();
		sw.close();
		
		os.close();
		
		return os.toByteArray();
	}
	
	@Override
	public CompletableFuture<Void> postCoverage(Workspace workspace, CoverageStore coverageStore, Coverage coverage) {
		try {
			return post(getCoveragesPath(workspace, coverageStore), getCoverageDocument(coverage));
		} catch(Exception e) {
			return f.failed(e);
		}
	}
	
	@Override
	public CompletableFuture<Void> putCoverage(Workspace workspace, CoverageStore coverageStore, Coverage coverage) {
		try {
			return put(getCoveragePath(workspace, coverageStore, coverage), getCoverageDocument(coverage));
		} catch(Exception e) {
			return f.failed(e);
		}
	}
	
	@Override
	public CompletableFuture<Void> postFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType) {
		try {
			return post(getFeatureTypesPath(workspace, dataStore), getFeatureTypeDocument(featureType));
		} catch(Exception e) {			
			return f.failed(e);
		}
	}
	
	private String getFeatureTypesPath(Workspace workspace, DataStore dataStore) {
		return getDataStorePath(workspace, dataStore.getName()) + "/featuretypes";
	}

	@Override
	public void close() throws IOException {
		asyncHttpClient.close();
	}
	
	private String getLayerGroupPath(Workspace workspace, LayerGroup layerGroup) { 
		return getLayerGroupPath(workspace, layerGroup.getName());
	}
	
	private String getLayerGroupPath(Workspace workspace, String layerGroupName) {
		return getLayerGroupsPath(workspace) + "/" + layerGroupName;
	}
	
	private CompletableFuture<Optional<LayerGroup>> getLayerGroup(Workspace workspace, String layerGroupName) {
		return get(getLayerGroupPath(workspace, layerGroupName)).thenApply(optionalDocument ->
			optionalDocument.map(document -> {
				XPathHelper layerGroup = xpath(document).node("layerGroup").get();
				
				List<PublishedRef> layers = StreamUtils.zip(
						layerGroup.nodes("publishables/published").stream(),
						layerGroup.nodes("styles/style").stream().map(style -> style.string("name")))
							.map(entry -> {
							
							XPathHelper published = entry.getFirst();
							Optional<String> style = entry.getSecond();
							
							String name = published.string("name").get();
							String type = published.string("@type").get();
							
							log.debug("type: {}", type);

							switch(type) {
								case "layer":
									if(style.isPresent()) {
										return new LayerRef(name, style.get());
									} else {
										return new LayerRef(name);
									}
								case "layerGroup":
									return new GroupRef(name);
								default:
									throw new IllegalArgumentException("unknown published type: " + type + ", name: " + name);
							}
					}).collect(Collectors.toList());
				
				String title = layerGroup.stringOrNull("title");
				String abstr = layerGroup.stringOrNull("abstractTxt");
				
				return new LayerGroup(layerGroupName, title, abstr, Collections.unmodifiableList(layers));
			}));
	}
	
	private String getLayerGroupsPath(Workspace workspace) {
		return getWorkspacesPath() + "/" + workspace.getName() + "/layergroups";
	}

	@Override
	public CompletableFuture<List<LayerGroup>> getLayerGroups(Workspace workspace) {
		return get(getLayerGroupsPath(workspace)).thenCompose(optionalDocument ->			
			f.sequence(
				xpath(optionalDocument.get()).map("layerGroups/layerGroup/name", name ->
					getLayerGroup(workspace, name.string().get())
						.thenApply(this::optionalPresent))));
	}
	
	@Override
	public CompletableFuture<Void> putLayerGroup(Workspace workspace, LayerGroup layerGroup) {
		try {
			return put(getLayerGroupPath(workspace, layerGroup), getLayerGroupDocument(workspace, layerGroup));
		} catch(Exception e) {
			return f.failed(e);
		}
	}
	
	@Override
	public CompletableFuture<Void> postLayerGroup(Workspace workspace, LayerGroup layerGroup) {
		try {
			return post(getLayerGroupsPath(workspace), getLayerGroupDocument(workspace, layerGroup));
		} catch(Exception e) {
			return f.failed(e);
		}
	}

	private byte[] getLayerGroupDocument(Workspace workspace, LayerGroup layerGroup) throws FactoryConfigurationError, XMLStreamException, IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		XMLOutputFactory of = XMLOutputFactory.newInstance();
		XMLStreamWriter sw = of.createXMLStreamWriter(os);
		sw.writeStartDocument();
		sw.writeStartElement("layerGroup");
			sw.writeStartElement("name");
				sw.writeCharacters(layerGroup.getName());
			sw.writeEndElement();
			
			sw.writeStartElement("mode");
				sw.writeCharacters("NAMED");
			sw.writeEndElement();
			
			String title = layerGroup.getTitle();
			if(title != null) {
				sw.writeStartElement("title");
					sw.writeCharacters(title);
				sw.writeEndElement();
			}
			
			String abstr = layerGroup.getAbstract();
			if(abstr != null) {
				sw.writeStartElement("abstractTxt");
					sw.writeCharacters(abstr);
				sw.writeEndElement();
			}
			
			sw.writeStartElement("publishables");
			for(PublishedRef publishedRef : layerGroup.getLayers()) {
				sw.writeStartElement("published");
				sw.writeAttribute("type", publishedRef.isGroup() ? "layerGroup" : "layer");
					sw.writeStartElement("name");
						// layerGroup references without workspace prefix are not correctly resolved
						sw.writeCharacters(workspace.getName() + ":" + publishedRef.getLayerName());
					sw.writeEndElement();
				sw.writeEndElement();
			}
			sw.writeEndElement();
			
			sw.writeStartElement("styles");
			for(PublishedRef publishedRef : layerGroup.getLayers()) {
				sw.writeStartElement("styles");
					if(!publishedRef.isGroup()) { 
						Optional<String> styleName = publishedRef.asLayerRef().getStyleName();
						if(styleName.isPresent()) {
							sw.writeStartElement("name");
								sw.writeCharacters(styleName.get());
							sw.writeEndElement();
						}
					}
				sw.writeEndElement();
			}
			sw.writeEndElement();
			
		sw.writeEndElement();
		sw.writeEndDocument();
		sw.close();
		
		os.close();
		
		return os.toByteArray();		
	}

	@Override
	public CompletableFuture<Void> deleteDataStore(Workspace workspace, DataStore dataStore) {		
		return delete(getDataStorePath(workspace, dataStore.getName()) + RECURSE);
	}
	
	@Override
	public CompletableFuture<Void> deleteCoverageStore(Workspace workspace, CoverageStore coverageStore) {		
		return delete(getCoverageStorePath(workspace, coverageStore) + RECURSE);
	}

	@Override
	public CompletableFuture<Void> deleteFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType) {
		return delete(getFeatureTypePath(workspace, dataStore, featureType.getName()) + RECURSE);
	}

	@Override
	public CompletableFuture<Void> deleteLayerGroup(Workspace workspace, LayerGroup layerGroup) {
		return delete(getLayerGroupPath(workspace, layerGroup.getName()));
	}

	@Override
	public CompletableFuture<Void> deleteWorkspace(Workspace workspace) { 
		return delete(getWorkspacePath(workspace) + RECURSE);
	}

	@Override
	public CompletableFuture<Optional<ServiceSettings>> getServiceSettings(Workspace workspace, ServiceType serviceType) {
		return get(getServiceSettingsPath(workspace, serviceType)).thenApply(optionalDocument ->
			optionalDocument.map(document -> {
				XPathHelper service = xpath(optionalDocument.get()).node(serviceType.name().toLowerCase()).get();
				
				return	new ServiceSettings(
						service.stringOrNull("title"),
						service.stringOrNull("abstrct"),
						service.strings("keywords/string"));
			}));
	}
	
	private String getWorkspaceSettingsPath(Workspace workspace) {
		return getWorkspacePath(workspace) + "/settings";
	}
	
	@Override
	public CompletableFuture<WorkspaceSettings> getWorkspaceSettings(Workspace workspace) {
		return get(getWorkspaceSettingsPath(workspace)).thenApply(optionalDocument -> {
			XPathHelper contactInfo = xpath(optionalDocument.get()).node("settings/contact").get();
			
			return
				new WorkspaceSettings(
					contactInfo.stringOrNull("contactPerson"),
					contactInfo.stringOrNull("contactOrganization"),
					contactInfo.stringOrNull("contactPosition"),
					contactInfo.stringOrNull("addressType"),
					contactInfo.stringOrNull("address"),
					contactInfo.stringOrNull("addressCity"),
					contactInfo.stringOrNull("addressState"),
					contactInfo.stringOrNull("addressPostalCode"),
					contactInfo.stringOrNull("addressCountry"),
					contactInfo.stringOrNull("contactVoice"),
					contactInfo.stringOrNull("contactFacsimile"), 
					contactInfo.stringOrNull("contactEmail"));
		});
	}

	@Override
	public CompletableFuture<Void> putWorkspaceSettings(Workspace workspace, WorkspaceSettings workspaceSettings) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			
			XMLOutputFactory of = XMLOutputFactory.newInstance();
			XMLStreamWriter sw = of.createXMLStreamWriter(os);			
			sw.writeStartDocument();
			
			sw.writeStartElement("settings");
				sw.writeStartElement("contact");
					sw.writeStartElement("id");						
						sw.writeCharacters("contact");
					sw.writeEndElement();
						
					String address = workspaceSettings.getAddress();
					if(address != null) {
						sw.writeStartElement("address");
							sw.writeCharacters(address);
						sw.writeEndElement();
					}
					
					String city = workspaceSettings.getCity();
					if(city != null) {
						sw.writeStartElement("addressCity");
							sw.writeCharacters(city);
						sw.writeEndElement();
					}
					
					String country = workspaceSettings.getCountry();
					if(country != null) {
						sw.writeStartElement("addressCountry");
							sw.writeCharacters(country);
						sw.writeEndElement();
					}
					
					String zipcode = workspaceSettings.getZipcode();
					if(zipcode != null) {
						sw.writeStartElement("addressPostalCode");
							sw.writeCharacters(zipcode);
						sw.writeEndElement();
					}
					
					String state = workspaceSettings.getState();
					if(state != null) {
						sw.writeStartElement("addressState");
							sw.writeCharacters(state);
						sw.writeEndElement();
					}
					
					String addressType = workspaceSettings.getAddressType();
					if(addressType != null) {
						sw.writeStartElement("addressType");
							sw.writeCharacters(addressType);
						sw.writeEndElement();
					}
					
					String email = workspaceSettings.getEmail();
					if(email != null) {
						sw.writeStartElement("contactEmail");
							sw.writeCharacters(email);
						sw.writeEndElement();
					}
					
					String fax = workspaceSettings.getFax();
					if(fax != null) {
						sw.writeStartElement("contactFacsimile");
							sw.writeCharacters(fax);
						sw.writeEndElement();
					}
					
					String organization = workspaceSettings.getOrganization();
					if(organization != null) {
						sw.writeStartElement("contactOrganization");
							sw.writeCharacters(organization);
						sw.writeEndElement();
					}
					
					String contact = workspaceSettings.getContact();
					if(contact != null) {
						sw.writeStartElement("contactPerson");
							sw.writeCharacters(contact);
						sw.writeEndElement();
					}
					
					String position = workspaceSettings.getPosition();
					if(position != null) {
						sw.writeStartElement("contactPosition");
							sw.writeCharacters(position);
						sw.writeEndElement();
					}
					
					String telephone = workspaceSettings.getTelephone();
					if(telephone != null) {
						sw.writeStartElement("contactVoice");
							sw.writeCharacters(telephone);
						sw.writeEndElement();
					}						
				sw.writeEndElement();
				
				sw.writeStartElement("charset");
					sw.writeCharacters("UTF-8");
				sw.writeEndElement();
				
				sw.writeStartElement("numDecimals");
					sw.writeCharacters("5");
				sw.writeEndElement();
				
			sw.writeEndElement();
			
			sw.writeEndDocument();
			sw.close();
			
			return put(getWorkspaceSettingsPath(workspace), os.toByteArray());
		} catch(Exception e) {
			return f.failed(e);
		}
	}
	
	@Override
	public CompletableFuture<Optional<Workspace>> getWorkspace(String workspaceId) {		
		return get(getWorkspacePath(workspaceId)).thenApply(optionalDocument -> 
			optionalDocument.map(document -> new Workspace(workspaceId)));
	}
	
	private String getStylePath(String styleId) {
		return getStylesPath() + "/" + styleId;
	}

	private String getStylesPath() {
		return restLocation + "styles";
	}

	@Override
	public CompletableFuture<Optional<Style>> getStyle(String styleId) {
		CompletableFuture<Optional<Style>> future = new CompletableFuture<>();
		
		CompletableFuture<String> fileNameFuture = new CompletableFuture<>();
		fileNameFuture.thenAccept(fileName -> {
			// we use an alternative end-point here because the one in /rest
			// wrongly raises 404 in some cases. 
			get(serviceLocation + "styles/" + fileName, false, true).whenComplete((optionalDocument, t) -> {
				if(t != null) {
					future.completeExceptionally(t);
				} else {
					try {
						future.complete(Optional.of(new Style(styleId, optionalDocument.get())));
					} catch(Exception e) {
						future.completeExceptionally(e);	
					}
				}
			});
		});
		
		get(getStylePath(styleId)).whenComplete((optionalDocument, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					if(optionalDocument.isPresent()) {
						fileNameFuture.complete(xpath(optionalDocument.get()).string("style/filename").get());						
					} else {
						future.complete(Optional.empty());
					}
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			}
		});
		
		return future;
	}
	
	@Override
	public CompletableFuture<List<String>> getStyleNames() {
		return get(getStylesPath()).thenApply(optionalDocument ->
				xpath(optionalDocument.get()).strings("styles/style/name"));
	}
	
	private byte[] serializeStyle(Document sld) throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		return serialize(sld);
	}
	
	@Override
	public CompletableFuture<Void> postStyle(Style style) {
		try {
			Document sld = style.getSld();
			String styleSldName = String.format("<style><name>%s</name><filename>%s</filename></style>", style.getName(), style.getName());
			byte[] serializedStyle = serializeStyle(sld);
			
			return post(getStylesPath(), styleSldName.getBytes(), "text/xml").thenRun(() -> put(getStylesPath() + "/" + style.getName(), serializedStyle, getStyleContentType(sld)));
		} catch(Exception e) {
			return f.failed(e);
		}
	}
	
	@Override
	public CompletableFuture<Void> putStyle(Style style) {
		try {
			Document sld = style.getSld();
			return put(getStylePath(style.getName()), serializeStyle(sld), getStyleContentType(sld));
		} catch(Exception e) {
			return f.failed(e);
		}
	}

	private String getStyleContentType(Document sld) {
		String contentType;
		Element root = sld.getDocumentElement();
		if(root.getLocalName().equals("StyledLayerDescriptor")) {
			String version = root.getAttribute("version");
			if("1.0.0".equals(version)) {
				contentType = "application/vnd.ogc.sld+xml";
			} else if("1.1.0".equals(version)) {
				contentType = "application/vnd.ogc.se+xml";
			} else {
				throw new IllegalStateException("expected: StyledLayerDescriptor[@version = '1.0.0' or @version = '1.1.0']");
			}
		} else {
			throw new IllegalStateException("expected: StyledLayerDescriptor");
		}
		return contentType;
	}

	private byte[] serialize(Document sld) throws TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		
		t.transform(new DOMSource(sld), new StreamResult(os));
		
		return os.toByteArray();		
	}
	
	private Document parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
		return parse(is, false);
	}
	
	private Document parse(InputStream is, boolean namespaceAware) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(namespaceAware);
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db.parse(is);
	}
	
	private String getLayerPath(Workspace workspace, Layer layer) {
		return getLayerPath(workspace, layer.getName());
	}
	
	private String getLayerPath(Workspace workspace, Coverage coverage) {
		return getLayerPath(workspace, coverage.getName());
	}
	
	private String getLayerPath(Workspace workspace, FeatureType featureType) {
		return getLayerPath(workspace, featureType.getName());
	}
	
	private String getLayerPath(Workspace workspace, String name) {
		// there isn't a layers end-point in workspaces/ but it
		// appeared possible to request a layer using ${workspaceName}:${layerName} as layer name,
		// the layer index is not usable because of name collisions (same layer name in different workspaces)
		// but the layer name seems always identical to the feature type name
		return restLocation + "layers/" + workspace.getName() + ":" + name;
	}
	
	public CompletableFuture<Layer> getLayer(Workspace workspace, Coverage coverage) {
		return get(getLayerPath(workspace, coverage)).thenApply(optionalDocument -> {			
			XPathHelper layer = xpath(optionalDocument.get()).node("layer").get();
			
			String defaultStyleName = layer.string("defaultStyle/name").get();
			StyleRef defaultStyle = new StyleRef(defaultStyleName);
			
			List<StyleRef> additionalStyles = layer.map("styles/style/name", 
				name -> new StyleRef(name.string().get())); 
			
			boolean queryable = Boolean.parseBoolean(layer.stringOrNull("queryable"));
			
			return new Layer(coverage.getName(), defaultStyle, additionalStyles, queryable);
		});
	}
	
	public CompletableFuture<Layer> getLayer(Workspace workspace, FeatureType featureType) {
		return get(getLayerPath(workspace, featureType)).thenApply(optionalDocument -> {			
			XPathHelper layer = xpath(optionalDocument.get()).node("layer").get();
			
			String defaultStyleName = layer.string("defaultStyle/name").get();
			StyleRef defaultStyle = new StyleRef(defaultStyleName);
			
			List<StyleRef> additionalStyles = layer.map("styles/style/name", 
				name -> new StyleRef(name.string().get())); 
			
			boolean queryable = Boolean.parseBoolean(layer.stringOrNull("queryable"));
			
			return new Layer(featureType.getName(), defaultStyle, additionalStyles, queryable);
		});
	}
	
	public CompletableFuture<Void> putLayer(Workspace workspace, Layer layer) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			
			XMLOutputFactory of = XMLOutputFactory.newInstance();
			XMLStreamWriter sw = of.createXMLStreamWriter(os);			
			sw.writeStartDocument();
			
			sw.writeStartElement("layer");
				sw.writeStartElement("name");
					sw.writeCharacters(layer.getName());
				sw.writeEndElement();
				
				sw.writeStartElement("queryable");
					sw.writeCharacters(String.valueOf(layer.isQueryable()));
				sw.writeEndElement();
				
				String defaultStyleName = layer.getDefaultStyle().getStyleName();
				if(defaultStyleName != null) {				
					sw.writeStartElement("defaultStyle");
						sw.writeStartElement("name");
							sw.writeCharacters(defaultStyleName);
						sw.writeEndElement();
					sw.writeEndElement();
				}
				
				sw.writeStartElement("styles");
				for(StyleRef style : layer.getAdditionalStyles()) {
					String styleName = style.getStyleName();					
					if(styleName != null) {
						sw.writeStartElement("style");
							sw.writeStartElement("name");
								sw.writeCharacters(styleName);
							sw.writeEndElement();
						sw.writeEndElement();
					}
				}
				sw.writeEndElement();
			sw.writeEndElement();
			
			sw.writeEndDocument();
			
			sw.close();
			
			return put(getLayerPath(workspace, layer), os.toByteArray());
		} catch(Exception e) {
			return f.failed(e);
		}
	}

	@Override
	public CompletableFuture<Void> deleteStyle(Style style) {
		return deleteStyle(style.getName());
	}
		
	@Override
	public CompletableFuture<Void> deleteStyle(String styleName) {		
		if(DEFAULT_STYLE_NAMES.contains(styleName)) {
			// GeoServer doesn't seem to like removal of a default style
			log.debug("ignoring delete request for default style: {}", styleName);
			return f.successful(null);
		} else {
			return delete(getStylePath(styleName) + "?purge=true");
		}
	}
	
	private String getTiledLayerSeedPath(String tiledLayerName) { 
		return getTiledLayersSeedPath() + "/" + tiledLayerName;
	}
	
	private String getTiledLayerPath(String tiledLayerName) {
		return getTiledLayersPath() + "/" + tiledLayerName;
	}
	
	private String getTiledLayersPath() {
		return getGWCServiceLocation() + "/layers";
	}
	
	private String getTiledLayersSeedPath() {
		return getGWCServiceLocation() + "/seed";
	}
	
	private String getGWCServiceLocation() {
		return serviceLocation + "gwc/rest" ;
	}
	
	private String getTiledLayerSeedPath(Workspace workspace, String layerName) {
		return getTiledLayerSeedPath(workspace.getName() + ":" + layerName);
	}
	
	private String getTiledLayerPath(Workspace workspace, String layerName) {
		return getTiledLayerPath(workspace.getName() + ":" + layerName);
	}
	
	@Override
	public CompletableFuture<Optional<TiledLayer>> getTiledLayer(Workspace workspace, String layerName) {
		return get(getTiledLayerPath(workspace, layerName)).thenApply(optionalDocument ->
			optionalDocument.map(document -> {						
				XPathHelper layer = xpath(optionalDocument.get()).node("GeoServerLayer").get();
				
				Integer metaWidth;
				Integer metaHeight;						
				Optional<XPathHelper> metaWidthHeight = layer.node("metaWidthHeight");
				if(metaWidthHeight.isPresent()) {
					List<Integer> values = metaWidthHeight.get().integers("int");
					
					metaWidth = values.get(0);
					metaHeight = values.get(1);
				} else {
					metaWidth = null;
					metaHeight = null;
				}
				
				return
					new TiledLayer(
						layer.strings("mimeFormats/string"),
						layer.nodes("gridSubsets/gridSubset").stream()
							.map(gridSubset -> new GridSubset(
								gridSubset.string("gridSetName").get(),
								gridSubset.integer("minCachedLevel"),
								gridSubset.integer("maxCachedLevel")))
							.collect(Collectors.toList()),
						metaWidth,
						metaHeight,
						layer.integerOrNull("expireCache"),
						layer.integerOrNull("expireClients"), 
						layer.integerOrNull("gutter"));
			}));
	}
	
	public CompletableFuture<Optional<TiledLayer>> getTiledLayer(Workspace workspace, FeatureType featureType) {
		return getTiledLayer(workspace, featureType.getName());
	}
	
	public CompletableFuture<Optional<TiledLayer>> getTiledLayer(Workspace workspace, LayerGroup layerGroup) {
		return getTiledLayer(workspace, layerGroup.getName());
	}
	
	@Override
	public CompletableFuture<Void> deleteTiledLayer(Workspace workspace, String layerName) {
		// the trailing .xml is required, results in a 400 otherwise
		return delete(getTiledLayerPath(workspace, layerName) + ".xml");
	}
	
	@Override
	public CompletableFuture<Void> deleteTiledLayer(Workspace workspace, FeatureType featureType) {
		return deleteTiledLayer(workspace, featureType.getName());
	}
	
	@Override
	public CompletableFuture<Void> deleteTiledLayer(Workspace workspace, LayerGroup layerGroup) {
		return deleteTiledLayer(workspace, layerGroup.getName());
	}
	
	@Override
	public CompletableFuture<List<String>> getTiledLayerNames(Workspace workspace) {
		return get(getTiledLayersPath()).thenApply(optionalDocument ->			
			xpath(optionalDocument.get()).strings("layers/layer/name").stream()
				.map(name -> name.split(":"))
				.filter(name -> name[0].equals(workspace.getName()))
				.map(name -> name[1])
				.collect(Collectors.toList()));
	}
	
	
	private byte[] getTiledLayerDocument(String tiledLayerName, TiledLayer tiledLayer) throws IOException, XMLStreamException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		XMLOutputFactory of = XMLOutputFactory.newInstance();
		XMLStreamWriter sw = of.createXMLStreamWriter(os);
		
		sw.writeStartDocument();
		
		sw.writeStartElement("GeoServerLayer");
			sw.writeStartElement("enabled");
				sw.writeCharacters("true");
			sw.writeEndElement();
			
			sw.writeStartElement("name");
				sw.writeCharacters(tiledLayerName);
			sw.writeEndElement();
			
			sw.writeStartElement("mimeFormats");
			for(String mimeFormat : tiledLayer.getMimeFormats()) {
				sw.writeStartElement("string");
					sw.writeCharacters(mimeFormat);
				sw.writeEndElement();
			}			
			sw.writeEndElement();
						
			sw.writeStartElement("gridSubsets");
				for(GridSubset gridSubset : tiledLayer.getGridSubsets()) {
					sw.writeStartElement("gridSubset");
						sw.writeStartElement("gridSetName");
							sw.writeCharacters(gridSubset.getGridSetName());
						sw.writeEndElement();
						
						Optional<Integer> minCachedLevel = gridSubset.getMinCachedLevel();
						if(minCachedLevel.isPresent()) {
							sw.writeStartElement("minCachedLevel");
								sw.writeCharacters("" + minCachedLevel.get());
							sw.writeEndElement();
						}
						
						Optional<Integer> maxCachedLevel = gridSubset.getMaxCachedLevel();
						if(maxCachedLevel.isPresent()) {
							sw.writeStartElement("maxCachedLevel");
								sw.writeCharacters("" + maxCachedLevel.get());
							sw.writeEndElement();
						}
					sw.writeEndElement();
				}
			sw.writeEndElement();
			
			sw.writeStartElement("metaWidthHeight");
				sw.writeStartElement("int");
					sw.writeCharacters("" + tiledLayer.getMetaWidth());
				sw.writeEndElement();
				sw.writeStartElement("int");
					sw.writeCharacters("" + tiledLayer.getMetaHeight());
				sw.writeEndElement();
			sw.writeEndElement();
			
			sw.writeStartElement("expireCache");
				sw.writeCharacters("" + tiledLayer.getExpireCache());
			sw.writeEndElement();
			
			sw.writeStartElement("expireClients");
				sw.writeCharacters("" + tiledLayer.getExpireClients());
			sw.writeEndElement();
			
			sw.writeStartElement("gutter");
				sw.writeCharacters("" + tiledLayer.getGutter());
			sw.writeEndElement();
		sw.writeEndElement();
		
		sw.writeEndDocument();
		
		sw.close();		
		os.close();
		
		return os.toByteArray();
	}
	
	@Override
	public CompletableFuture<Void> putTiledLayer(Workspace workspace, String layerName, TiledLayer tiledLayer) {
		try {
			return put(getTiledLayerPath(workspace, layerName) + ".xml", 
				getTiledLayerDocument(workspace.getName() + ":" + layerName, tiledLayer));
		} catch(Exception e) {
			return f.failed(e);
		}	
	}
	
	@Override
	public CompletableFuture<Void> postTiledLayer(Workspace workspace, String layerName, TiledLayer tiledLayer) {
		try {
			return post(
				getTiledLayerPath(workspace, layerName) + ".xml", 
				getTiledLayerDocument(workspace.getName() + ":" + layerName, tiledLayer),
				HttpURLConnection.HTTP_OK);
		} catch(Exception e) {
			return f.failed(e);
		}	
	}
	
	@Override
	public CompletableFuture<Void> seedTiledLayer(Workspace workspace, String layerName, TiledLayer tiledLayer, int zoomStart, int zoomStop) {
		return seedTiledLayer(workspace, layerName, tiledLayer, zoomStart, zoomStop, "seed");
	}
	
	@Override
	public CompletableFuture<Void> reseedTiledLayer(Workspace workspace, String layerName, TiledLayer tiledLayer, int zoomStart, int zoomStop) {
		return seedTiledLayer(workspace, layerName, tiledLayer, zoomStart, zoomStop, "reseed");
	}
	
	@Override
	public CompletableFuture<Void> truncateTiledLayer(Workspace workspace, String layerName, TiledLayer tiledLayer, int zoomStart, int zoomStop) {
		return seedTiledLayer(workspace, layerName, tiledLayer, zoomStart, zoomStop, "truncate");
	}
	
	private CompletableFuture<Void> seedTiledLayer(Workspace workspace, String layerName, TiledLayer tiledLayer, int zoomStart, int zoomStop, String type) {
		ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
		for(GridSubset gridSubset : tiledLayer.getGridSubsets()) {
			for(String mimeFormat : tiledLayer.getMimeFormats()) {
				try {
					futures.add(
						post(
							getTiledLayerSeedPath(workspace, layerName) + ".xml",
							getSeedRequestDocument(
								workspace.getName() + ":" + layerName, 
								gridSubset.getGridSetName(),
								zoomStart,
								zoomStop,
								mimeFormat,
								type),
							HttpURLConnection.HTTP_OK));
				} catch(Exception e) {
					return f.failed(e);
				}
			}
		}
		
		return f.sequence(futures).thenApply(result -> null);
	}	

	private byte[] getSeedRequestDocument(String tiledLayerName, String gridSetId, int zoomStart, int zoomStop, String format, String type) throws IOException, XMLStreamException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		XMLOutputFactory of = XMLOutputFactory.newInstance();
		XMLStreamWriter sw = of.createXMLStreamWriter(os);
		
		sw.writeStartDocument();
		
		sw.writeStartElement("seedRequest");
			sw.writeStartElement("name");
				sw.writeCharacters(tiledLayerName);				
			sw.writeEndElement();
			
			sw.writeStartElement("gridSetId");
				sw.writeCharacters(gridSetId);
			sw.writeEndElement();
			
			sw.writeStartElement("zoomStart");
				sw.writeCharacters("" + zoomStart);
			sw.writeEndElement();
			
			sw.writeStartElement("zoomStop");
				sw.writeCharacters("" + zoomStop);
			sw.writeEndElement();
			
			sw.writeStartElement("format");
				sw.writeCharacters(format);
			sw.writeEndElement();
			
			sw.writeStartElement("threadCount");
				sw.writeCharacters("1");
			sw.writeEndElement();
			
		sw.writeEndElement();
		
		sw.writeEndDocument();
		
		sw.close();		
		os.close();
		
		return os.toByteArray();
	}

	@Override
	public CompletableFuture<Void> reload() {
		return post(restLocation + "reload", new byte[]{}, "text/xml", HttpURLConnection.HTTP_OK);
	}
	
	@Override
	public CompletableFuture<Void> reset() {
		return post(restLocation + "reset", new byte[]{}, "text/xml", HttpURLConnection.HTTP_OK);
	}
}
