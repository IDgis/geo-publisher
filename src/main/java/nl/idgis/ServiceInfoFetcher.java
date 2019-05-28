package nl.idgis;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ServiceInfoFetcher {
	
	private final DataSource dataSource;
	
	private final String serviceQuery;
	
	private final ObjectMapper om = new ObjectMapper();
	
	public ServiceInfoFetcher(DataSource dataSource) throws Exception {
		this.dataSource = dataSource;
		
		final String resourceName = "service.sql";
		final InputStream is = ServiceInfoFetcher.class.getResourceAsStream(resourceName);		
		if (is == null) {
			throw new IllegalStateException("resource is missing: " + resourceName);
		}
		serviceQuery = IOUtils.toString(is, "utf-8");
	}
	
	private static JsonNode findChildLayerById(JsonNode layer, int id) {
		JsonNode layers = layer.get("layers");
		if (layers == null) {
			throw new IllegalStateException("layers field is missing");
		}
		
		for (JsonNode childLayer : layers) {
			JsonNode idNode = childLayer.get("id");
			if (idNode == null) {
				throw new IllegalStateException("id field is missing");
			}
		
			if (idNode.asInt() == id) {
				return childLayer;
			}
		}
		
		return null;
	}
	
	private static JsonNode removeAllLayerIds(JsonNode layer) {
		((ObjectNode)layer).remove("id");
		JsonNode layers = layer.get("layers");
		if (layers != null) {
			for (JsonNode childLayer : layers) {
				removeAllLayerIds(childLayer);
			}
		}
		
		return layer;
	}
	
	private static JsonNode ensureUniqueLayerNames(JsonNode layer) {
		return makeLayerNamesUnique(layer, new HashSet<>());
	}
	
	private static JsonNode makeLayerNamesUnique(JsonNode layer, Set<String> layerNames) {
		JsonNode name = layer.get("name");
		if (name == null) {
			throw new IllegalStateException("name field is missing");
		}
		
		String nameText = name.asText();				
		if (layerNames.contains(nameText)) {
			int postFix = 2;
			String newName;
			
			do {
				newName = nameText + "-" + postFix;
				postFix++;
			} while (layerNames.contains(newName));
			
			layerNames.add(newName);
			((ObjectNode)layer).put("name", newName);
		} else {
			layerNames.add(nameText);
		}
		
		JsonNode layers = layer.get("layers");
		if (layers != null) {
			for (JsonNode childLayer : layers) {
				makeLayerNamesUnique(childLayer, layerNames);
			}
		}
		
		return layer;
	}
	
	public JsonNode fetchServiceInfo(int serviceId) throws SQLException, IOException {
		JsonNode root = fetchServiceInfo(serviceId, (ObjectNode serviceTree, JsonNode layers, Integer[] anchestors) -> {
			if (anchestors.length == 0) {
				serviceTree = om.createObjectNode();
				serviceTree.set("layers", layers);
			} else {
				if (serviceTree == null) {
					throw new IllegalStateException("no service tree available");
				}
				
				ObjectNode currentLayer = serviceTree;
				for (int anchestor : anchestors) {
					JsonNode child = findChildLayerById(currentLayer, anchestor);
					if (child == null) {
						throw new IllegalStateException("failed to traverse anchestors");
					}
					currentLayer = (ObjectNode)child;
				}
				
				currentLayer.set("layers", layers);
			}
			
			return serviceTree;
		});
		
		if (root == null) {
			return null;
		}
		
		JsonNode layers = root.get("layers");
		if (layers == null) {
			throw new IllegalStateException("layers field is missing at root level");
		}
		
		if (layers.size() != 1) {
			throw new IllegalStateException("Unexpected number (!= 1) of root layers: " + layers.size());
		}
		
		return StreamSupport.stream(layers.spliterator(), false)
				.map(ServiceInfoFetcher::removeAllLayerIds)
				.map(ServiceInfoFetcher::ensureUniqueLayerNames)
				.findFirst()
				.get();
	}
	
	@FunctionalInterface
	private interface ServiceInfoBuilder<T> {

		T accept(T serviceInfo, JsonNode layers, Integer[] anchestors);
	}
	
	private <T> T fetchServiceInfo(int serviceId, ServiceInfoBuilder<T> builder) throws SQLException, IOException {
		T serviceInfo = null;
		
		try (Connection c = DataSourceUtils.getConnection(dataSource); 
				PreparedStatement stmt = c.prepareStatement(serviceQuery)) {
			
			stmt.setInt(1, serviceId);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					Integer[] anchestors = (Integer[])rs.getArray(1).getArray();
					JsonNode layers = om.readTree(rs.getBinaryStream(2));
					serviceInfo = builder.accept(serviceInfo, layers, anchestors);
				}
			}
		}
		
		return serviceInfo;
	}
	
	public List<Integer> fetchAllServiceIds() throws SQLException {
		List<Integer> retval = new ArrayList<>();
		
		try (Connection c = DataSourceUtils.getConnection(dataSource); 
				Statement stmt = c.createStatement();
				ResultSet rs = stmt.executeQuery("select id from publisher.service order by 1")) {
			
			while (rs.next()) {
				retval.add(rs.getInt(1));
			}
		}
		
		return retval;
	}
	
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
		dataSource.setDriverClass(org.postgresql.Driver.class);
		dataSource.setUrl("jdbc:postgresql://192.168.99.100:5432/publisher");
		dataSource.setUsername("postgres");
		dataSource.setPassword("postgres");
		
		ServiceInfoFetcher sif = new ServiceInfoFetcher(dataSource);
		
		try(BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream("services.json"))) {
			JsonFactory jf = new JsonFactory();
			
			try(JsonGenerator jg = jf.createGenerator(os, JsonEncoding.UTF8)
				.useDefaultPrettyPrinter()
				.setCodec(sif.om)) {
			
				jg.writeStartObject();
				jg.writeArrayFieldStart("services");
				
				List<Integer> serviceIds = sif.fetchAllServiceIds();
				for (int serviceId : serviceIds) {
					JsonNode serviceInfo = sif.fetchServiceInfo(serviceId);
					jg.writeTree(serviceInfo);
				}
				
				jg.writeEndArray();
				jg.writeEndObject();
			}
		}
		
		System.out.println("done: " + (System.currentTimeMillis() - startTime) +" ms");
	}
}
