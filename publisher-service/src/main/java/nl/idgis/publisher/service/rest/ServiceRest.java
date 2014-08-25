package nl.idgis.publisher.service.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ServiceRest {
	
	private final String authorization;
	private final String serviceLocation;
	
	private final DocumentBuilder documentBuilder;
	private final XPath xpath;
	private final XMLOutputFactory outputFactory;
	
	public ServiceRest(String serviceLocation, String user, String password) throws Exception {		
		this.serviceLocation = serviceLocation;
		this.authorization = "Basic " + new String(Base64.encodeBase64((user + ":" + password).getBytes()));		
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		documentBuilder = dbf.newDocumentBuilder();
		
		xpath = XPathFactory.newInstance().newXPath();
		
		outputFactory = XMLOutputFactory.newInstance();
	}	
	
	private Document getDocument(String path) throws Exception {
		HttpURLConnection connection = getConnection(path + ".xml");
		
		InputStream stream = connection.getInputStream();
		Document document = documentBuilder.parse(stream);
		stream.close();
		
		return document;
	}

	private HttpURLConnection getConnection(String path) throws Exception {
		URL url = new URL(path);
		HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        urlConnection.setRequestProperty("Authorization", authorization);
		return urlConnection;
	}
	
	public boolean addWorkspace(Workspace workspace) throws Exception {
		HttpURLConnection connection = getConnection(getWorkspacesPath());
		
		OutputStream outputStream = getOutputStream(connection);
		
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(outputStream);
		streamWriter.writeStartDocument();
			streamWriter.writeStartElement("workspace");
				streamWriter.writeStartElement("name");
					streamWriter.writeCharacters(workspace.getName());
				streamWriter.writeEndElement();
			streamWriter.writeEndElement();
		streamWriter.writeEndDocument();
		streamWriter.close();
		
		outputStream.close();
		
		int responseCode = connection.getResponseCode();		
		return responseCode == HttpURLConnection.HTTP_CREATED;
	}

	private String getWorkspacesPath() {
		return serviceLocation + "workspaces";
	}

	private OutputStream getOutputStream(HttpURLConnection connection) throws IOException {
		connection.setRequestProperty("Content-type", "text/xml");
		connection.setDoOutput(true);
		
		return connection.getOutputStream();		
	}
	
	public List<Workspace> getWorkspaces() throws Exception {
		List<Workspace> retval = new ArrayList<>();		

		Document document = getDocument(getWorkspacesPath());
		
		NodeList result = (NodeList)xpath.evaluate("/workspaces/workspace/name/text()", document, XPathConstants.NODESET);
		for(int i = 0; i < result.getLength(); i++) {
			Node n = result.item(i);
			retval.add(new Workspace(n.getTextContent()));
		}
		
		return retval;
	}
	
	private DataStore getDataStore(Workspace workspace, String dataStoreName) throws Exception {
		Document document = getDocument(getDataStorePath(workspace, dataStoreName));
		
		String name = (String)xpath.evaluate("/dataStore/name/text()", document, XPathConstants.STRING);
		
		Map<String, String> connectionParameters = new HashMap<>();
		NodeList result = (NodeList)xpath.evaluate("/dataStore/connectionParameters/entry", document, XPathConstants.NODESET);
		for(int i = 0; i < result.getLength(); i++) {
			Node n = result.item(i);
			
			String key = n.getAttributes().getNamedItem("key").getNodeValue();
			String value = n.getTextContent();
			connectionParameters.put(key, value);			
		}
		
		return new DataStore(name, connectionParameters);
	}

	private String getDataStorePath(Workspace workspace, String dataStoreName) {
		return getDataStoresPath(workspace) + "/" + dataStoreName;
	}

	public List<DataStore> getDataStores(Workspace workspace) throws Exception {
		List<DataStore> retval = new ArrayList<>();
		
		Document document = getDocument(getDataStoresPath(workspace));
		
		NodeList result = (NodeList)xpath.evaluate("/dataStores/dataStore/name/text()", document, XPathConstants.NODESET);
		for(int i = 0; i < result.getLength(); i++) {
			Node n = result.item(i);
			retval.add(getDataStore(workspace, n.getTextContent()));
		}
		
		return retval;
	}

	public boolean addDataStore(Workspace workspace, DataStore dataStore) throws Exception {
		HttpURLConnection connection = getConnection(getDataStoresPath(workspace));
				
		OutputStream outputStream = getOutputStream(connection);
		
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(outputStream);
		streamWriter.writeStartDocument();
		streamWriter.writeStartElement("dataStore");
			streamWriter.writeStartElement("name");
				streamWriter.writeCharacters(dataStore.getName());
			streamWriter.writeEndElement();
			streamWriter.writeStartElement("connectionParameters");
				for(Map.Entry<String, String> connectionParameter : dataStore.getConnectionParameters().entrySet()) {
					streamWriter.writeStartElement(connectionParameter.getKey());
					streamWriter.writeCharacters(connectionParameter.getValue());
					streamWriter.writeEndElement();
				}
			streamWriter.writeEndElement();
		streamWriter.writeEndDocument();
		streamWriter.close();
		
		outputStream.close();
		
		int responseCode = connection.getResponseCode();
		return responseCode == HttpURLConnection.HTTP_CREATED;
	}

	private String getDataStoresPath(Workspace workspace) {
		return getWorkspacesPath() + "/" + workspace.getName() + "/datastores";
	}
	
	private String getFeatureTypePath(Workspace workspace, DataStore dataStore, String featureTypeName) {
		return getFeatureTypesPath(workspace, dataStore) + "/" + featureTypeName;
	}
	
	private FeatureType getFeatureType(Workspace workspace, DataStore dataStore, String featureTypeName) throws Exception {
		Document document = getDocument(getFeatureTypePath(workspace, dataStore, featureTypeName));
		
		String name = (String)xpath.evaluate("/featureType/name/text()", document, XPathConstants.STRING);
		String nativeName = (String)xpath.evaluate("/featureType/nativeName/text()", document, XPathConstants.STRING);
		
		List<Attribute> attributes = new ArrayList<>();
		NodeList result = (NodeList)xpath.evaluate("/featureType/attributes/attribute", document, XPathConstants.NODESET);
		for(int i = 0; i < result.getLength(); i++) {
			Node n = result.item(i);
			
			String attributeName = xpath.evaluate("name/text()", n);
			attributes.add(new Attribute(attributeName));
		}
		
		return new FeatureType(name, nativeName, Collections.unmodifiableList(attributes));
	}
	
	public List<FeatureType> getFeatureTypes(Workspace workspace, DataStore dataStore) throws Exception {
		List<FeatureType> retval = new ArrayList<>();
		
		Document document = getDocument(getFeatureTypesPath(workspace, dataStore));
		NodeList result = (NodeList)xpath.evaluate("/featureTypes/featureType/name/text()", document, XPathConstants.NODESET);
		for(int i = 0; i < result.getLength(); i++) {
			Node n = result.item(i);
			retval.add(getFeatureType(workspace, dataStore, n.getTextContent()));
		}
		
		return retval;
	}
	
	public boolean addFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType) throws Exception {
		HttpURLConnection connection = getConnection(getFeatureTypesPath(workspace, dataStore));
		
		OutputStream outputStream = getOutputStream(connection);
		
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(outputStream);
		streamWriter.writeStartDocument();
		streamWriter.writeStartElement("featureType");
			streamWriter.writeStartElement("name");
				streamWriter.writeCharacters(featureType.getName());
			streamWriter.writeEndElement();
			
			String nativeName = featureType.getNativeName();
			if(nativeName != null) {
				streamWriter.writeStartElement("nativeName");
					streamWriter.writeCharacters(nativeName);
				streamWriter.writeEndElement();
			}
		streamWriter.writeEndElement();
		streamWriter.writeEndDocument();
		streamWriter.close();
		
		outputStream.close();
		
		int responseCode = connection.getResponseCode();
		return responseCode == HttpURLConnection.HTTP_CREATED;
	}
	
	private String getFeatureTypesPath(Workspace workspace, DataStore dataStore) {
		return getDataStorePath(workspace, dataStore.getName()) + "/featuretypes";
	}

}
