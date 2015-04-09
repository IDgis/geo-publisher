package nl.idgis.publisher.service.json;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;

import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.GroupLayerRef;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.Service;
import nl.idgis.publisher.domain.web.tree.StyleRef;
import nl.idgis.publisher.domain.web.tree.Tiling;

public class JsonService implements Service {
	
	private final JsonNode jsonNode;
	
	public JsonService(JsonNode jsonNode) {
		this.jsonNode = jsonNode;
	}

	@Override
	public String getId() {
		return jsonNode.get("id").asText();
	}

	@Override
	public String getName() {
		return jsonNode.get("name").asText();
	}

	@Override
	public String getTitle() {
		return jsonNode.get("title").asText();
	}

	@Override
	public String getAbstract() {
		return jsonNode.get("abstract").asText();
	}

	@Override
	public String getContact() {
		return jsonNode.get("contact").asText();
	}

	@Override
	public String getOrganization() {
		return jsonNode.get("organization").asText();
	}

	@Override
	public String getPosition() {
		return jsonNode.get("position").asText();
	}

	@Override
	public String getAddressType() {
		return jsonNode.get("addressType").asText();
	}

	@Override
	public String getAddress() {
		return jsonNode.get("address").asText();
	}

	@Override
	public String getCity() {
		return jsonNode.get("city").asText();
	}

	@Override
	public String getState() {
		return jsonNode.get("state").asText();
	}

	@Override
	public String getZipcode() {
		return jsonNode.get("zipcode").asText();
	}

	@Override
	public String getCountry() {
		return jsonNode.get("country").asText();
	}

	@Override
	public String getTelephone() {
		return jsonNode.get("telephone").asText();
	}

	@Override
	public String getFax() {
		return jsonNode.get("fax").asText();
	}

	@Override
	public String getEmail() {
		return jsonNode.get("email").asText();
	}

	@Override
	public List<String> getKeywords() {
		return 
			getStream(jsonNode, "keywords")
				.map(JsonNode::asText)
				.collect(toList());
	}

	@Override
	public String getRootId() {
		return jsonNode.get("rootId").asText();
	}

	@Override
	public List<LayerRef<? extends Layer>> getLayers() {
		return 
			getStream(jsonNode, "layers")
				.map(AbstractJsonLayerRef::fromJson)
				.collect(toList());
	}
	
	@Override
	public boolean isConfidential () {
		return jsonNode.path ("confidential").asBoolean ();
	}
	
	private interface EnrichedService extends Service {
		
		String getFormatRevision();
	}
	
	// annotation mixins below are required to serialize Mockito objects
	
	@JsonSerialize(as=DatasetLayerRef.class)
	private interface DatasetLayerRefMixin {
		
	}
	
	@JsonSerialize(as=DatasetLayer.class)
	private interface DatasetLayerMixin {
		
	}
	
	@JsonSerialize(as=GroupLayerRef.class)
	private interface GroupLayerRefMixin {
		
	}
	
	@JsonSerialize(as=GroupLayer.class)
	private interface GroupLayerMixin {
		
	}
	
	@JsonSerialize(as=StyleRef.class)
	private interface StyleRefMixin {
		
	}
	
	@JsonSerialize(as=Tiling.class)
	private interface TilingMixin {
		
	}
	
	@SuppressWarnings("rawtypes")
	private static class OptionalJsonSerializer extends JsonSerializer<Optional> {		

		@Override
		public boolean isEmpty(Optional value) {
			return !value.isPresent();
		}

		@Override
		public Class<Optional> handledType() {
			return Optional.class;
		}		

		@Override
		public void serialize(Optional value, JsonGenerator jgen, SerializerProvider provider) 
			throws IOException, JsonProcessingException {
			
			jgen.writeObject(value.get());
		}
		
	}
	
	public static String toJson(Service service) {
		ObjectMapper objectMapper = new ObjectMapper();
		
		SimpleModule module = new SimpleModule("JsonService", Version.unknownVersion());
		module.addSerializer(new OptionalJsonSerializer());
		
		objectMapper.registerModule(module);
		
		objectMapper.setSerializationInclusion(Include.NON_EMPTY);
		
		objectMapper.addMixInAnnotations(DatasetLayerRef.class, DatasetLayerRefMixin.class);
		objectMapper.addMixInAnnotations(DatasetLayer.class, DatasetLayerMixin.class);
		objectMapper.addMixInAnnotations(GroupLayerRef.class, GroupLayerRefMixin.class);
		objectMapper.addMixInAnnotations(GroupLayer.class, GroupLayerMixin.class);
		objectMapper.addMixInAnnotations(StyleRef.class, StyleRefMixin.class);		
		objectMapper.addMixInAnnotations(Tiling.class, TilingMixin.class);
		
		try {
			return objectMapper.writeValueAsString(
				Proxy.newProxyInstance(
					JsonService.class.getClassLoader(), 
					new Class[]{EnrichedService.class},
					new InvocationHandler() {
						
						Method getFormatRevision = EnrichedService.class.getMethod("getFormatRevision");
						
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if(method.equals(getFormatRevision)) {
								return "1";
							} else {
								return method.invoke(service, args);
							}
						}
					})); 
		} catch(Exception e) {
			throw new RuntimeException("Couldn't generate json from service", e);
		}
	}

	public static Service fromJson(String json) {
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			JsonNode jsonNode = objectMapper.readValue(json, JsonNode.class);
			int formatRevision = jsonNode.get("formatRevision").asInt();
			if(formatRevision != 1) {
				throw new IllegalArgumentException("unsupported format revision: " + formatRevision);
			}
			
			return new JsonService(jsonNode);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't construct service from json", e);
		}
	}
	
	static Stream<JsonNode> getStream(JsonNode jsonNode, String fieldName) {
		JsonNode array = jsonNode.get(fieldName);
		
		return 
			range(0, array.size())
				.mapToObj(array::get);
	}
	
	static Optional<JsonNode> getOptional(JsonNode jsonNode, String fieldName) {
		if(jsonNode.has(fieldName)) {
			return Optional.of(jsonNode.get("tiling"));
		} else {
			return Optional.empty();
		}
	}

}
