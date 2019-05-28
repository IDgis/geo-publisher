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
	
	private static void removeAllLayerIds(JsonNode layer) {
		((ObjectNode)layer).remove("id");
		JsonNode layers = layer.get("layers");
		if (layers != null) {
			for (JsonNode childLayer : layers) {
				removeAllLayerIds(childLayer);
			}
		}
	}
	
	private static void makeLayerNamesUnique(JsonNode layer) {
		makeLayerNamesUnique(layer, new HashSet<>());
	}
	
	private static void makeLayerNamesUnique(JsonNode layer, Set<String> layerNames) {
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
	}
	
	private void fetchAllServiceInfos(DataSourceConsumer<JsonNode> consumer) throws SQLException, IOException {
		fetchAllServiceInfos(defaultServiceInfoBuilder, root -> {
			consumer.accept(postProcessServiceInfo(root));
		});
	}
	
	public interface DataSourceConsumer<T> {
		
		void accept(T t) throws SQLException, IOException;
	}
	
	public <T> void fetchAllServiceInfos(ServiceInfoBuilder<T> builder, DataSourceConsumer<? super T> consumer) throws SQLException, IOException {
		try (Connection c = DataSourceUtils.getConnection(dataSource); 
				PreparedStatement stmt = c.prepareStatement(serviceQuery)) {
			
			T serviceInfo = null;
			int lastServiceId = -1;
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					int serviceId = rs.getInt(1);
					Integer[] anchestors = (Integer[])rs.getArray(2).getArray();
					JsonNode layers = om.readTree(rs.getBinaryStream(3));
					
					if (serviceId != lastServiceId) {
						if (serviceInfo != null) {
							consumer.accept(serviceInfo);
						}
						
						lastServiceId = serviceId;
						serviceInfo = null;
					}
					
					serviceInfo = builder.accept(serviceInfo, layers, anchestors);
				}
				
				if (serviceInfo != null) {
					consumer.accept(serviceInfo);
				}
			}
		}
	}
	
	private static JsonNode postProcessServiceInfo(JsonNode root) {
		JsonNode layers = root.get("layers");
		if (layers == null) {
			throw new IllegalStateException("layers field is missing at root level");
		}
		
		if (layers.size() != 1) {
			throw new IllegalStateException("Unexpected number (!= 1) of root layers: " + layers.size());
		}
		
		JsonNode rootLayer = layers.get(0);
		removeAllLayerIds(rootLayer);
		makeLayerNamesUnique(rootLayer);
		
		return rootLayer;
	}
	
	public JsonNode fetchServiceInfo(int serviceId) throws SQLException, IOException {
		JsonNode root = fetchServiceInfo(serviceId, defaultServiceInfoBuilder);
		
		if (root == null) {
			return null;
		}
		
		return postProcessServiceInfo(root);
	}
	
	@FunctionalInterface
	private interface ServiceInfoBuilder<T> {

		T accept(T serviceInfo, JsonNode layers, Integer[] anchestors);
	}
	
	private final ServiceInfoBuilder<ObjectNode> defaultServiceInfoBuilder = (ObjectNode serviceInfo, JsonNode layers, Integer[] anchestors) -> {
		if (anchestors.length == 0) {
			serviceInfo = om.createObjectNode();
			serviceInfo.set("layers", layers);
		} else {
			if (serviceInfo == null) {
				throw new IllegalStateException("no service info available");
			}
			
			ObjectNode currentLayer = serviceInfo;
			for (int anchestor : anchestors) {
				JsonNode child = findChildLayerById(currentLayer, anchestor);
				if (child == null) {
					throw new IllegalStateException("failed to traverse anchestors");
				}
				currentLayer = (ObjectNode)child;
			}
			
			currentLayer.set("layers", layers);
		}
		
		return serviceInfo;
	};
	
	private <T> T fetchServiceInfo(int serviceId, ServiceInfoBuilder<T> builder) throws SQLException, IOException {
		T serviceInfo = null;
		
		try (Connection c = DataSourceUtils.getConnection(dataSource); 
				PreparedStatement stmt = c.prepareStatement("select * from (" + serviceQuery + ") s where s.service_id = ?")) {
			
			stmt.setInt(1, serviceId);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					Integer[] anchestors = (Integer[])rs.getArray(2).getArray();
					JsonNode layers = om.readTree(rs.getBinaryStream(3));
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
		System.out.println("fetching all services:");
		fetchAllWithSingleQuery(sif);
		fetchAllWithMultipleQueries(sif);
		
		System.out.println("done: " + (System.currentTimeMillis() - startTime) +" ms");
	}
	
	public static void fetchAllWithSingleQuery(ServiceInfoFetcher sif) throws Exception {		
		long startTime = System.currentTimeMillis();
		
		System.out.print("- with a single query... ");
		System.out.flush();
		
		try(BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream("services-single.json"))) {
			JsonFactory jf = new JsonFactory();
			
			try(JsonGenerator jg = jf.createGenerator(os, JsonEncoding.UTF8)
				.useDefaultPrettyPrinter()
				.setCodec(sif.om)) {
			
				jg.writeStartObject();
				jg.writeArrayFieldStart("services");
				
				sif.fetchAllServiceInfos(jg::writeTree);
				
				jg.writeEndArray();
				jg.writeEndObject();
			}
		}
		
		System.out.println("done: " + (System.currentTimeMillis() - startTime) +" ms");
	}
	
	
	public static void fetchAllWithMultipleQueries(ServiceInfoFetcher sif) throws Exception {
		long startTime = System.currentTimeMillis();
		
		System.out.print("- with multiple queries... ");
		System.out.flush();
		
		try(BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream("services-multiple.json"))) {
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
