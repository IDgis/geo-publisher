package nl.idgis.publisher.service.json;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;

import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.GroupLayerRef;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.RasterDatasetLayer;
import nl.idgis.publisher.domain.web.tree.Service;
import nl.idgis.publisher.domain.web.tree.StyleRef;
import nl.idgis.publisher.domain.web.tree.Tiling;
import nl.idgis.publisher.domain.web.tree.VectorDatasetLayer;

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
	
	// annotation mixins below are required to serialize Mockito objects
	
	@JsonSerialize(as=Service.class)
	private interface ServiceMixin {
		
	}
	
	@JsonSerialize(as=DatasetLayerRef.class)
	private interface DatasetLayerRefMixin {
		
	}
	
	@JsonSerialize(as=VectorDatasetLayer.class)
	private interface VectorDatasetLayerMixin {
		
		@JsonIgnore
		boolean isVectorLayer();
		
		@JsonIgnore
		boolean isRasterLayer();
	}
	
	@JsonSerialize(as=RasterDatasetLayer.class)
	private interface RasterDatasetLayerMixin {
		
		@JsonIgnore
		boolean isVectorLayer();
		
		@JsonIgnore
		boolean isRasterLayer();
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
	
	private static class ServiceSerializer extends BeanSerializerBase {

		ServiceSerializer(BeanSerializerBase src) {
			super(src);
		}
		
		ServiceSerializer(BeanSerializerBase  src, ObjectIdWriter objectIdWriter) {
            super(src, objectIdWriter);
        }
		
		ServiceSerializer(BeanSerializerBase src, String[] toIgnore) {
            super(src, toIgnore);
        }
		
		ServiceSerializer(BeanSerializerBase src, ObjectIdWriter objectIdWriter, Object filterId) {
			super(src, objectIdWriter, filterId);
		}

		@Override
		public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
			return new ServiceSerializer(this, objectIdWriter);
		}

		@Override
		protected BeanSerializerBase withIgnorals(String[] toIgnore) {
			return new ServiceSerializer(this, toIgnore);
		}

		@Override
		protected BeanSerializerBase asArraySerializer() {			
			return null;
		}

		@Override
		protected BeanSerializerBase withFilterId(Object filterId) {
			return new ServiceSerializer(this, _objectIdWriter, filterId);
		}

		@Override
		public void serialize(Object bean, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
			jgen.writeStartObject();
			jgen.writeNumberField("formatRevision", 1);
            serializeFields(bean, jgen, provider);             
            jgen.writeEndObject();
		}
		
	}
	
	private static class DatasetLayerSerializer extends BeanSerializerBase {

		DatasetLayerSerializer(BeanSerializerBase src) {
			super(src);
		}
		
		DatasetLayerSerializer(BeanSerializerBase  src, ObjectIdWriter objectIdWriter) {
            super(src, objectIdWriter);
        }
		
		DatasetLayerSerializer(BeanSerializerBase src, String[] toIgnore) {
            super(src, toIgnore);
        }
		
		DatasetLayerSerializer(BeanSerializerBase src, ObjectIdWriter objectIdWriter, Object filterId) {
			super(src, objectIdWriter, filterId);
		}

		@Override
		public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
			return new DatasetLayerSerializer(this, objectIdWriter);
		}

		@Override
		protected BeanSerializerBase withIgnorals(String[] toIgnore) {
			return new DatasetLayerSerializer(this, toIgnore);
		}

		@Override
		protected BeanSerializerBase asArraySerializer() {			
			return null;
		}

		@Override
		protected BeanSerializerBase withFilterId(Object filterId) {
			return new DatasetLayerSerializer(this, _objectIdWriter, filterId);
		}

		@Override
		public void serialize(Object bean, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
			jgen.writeStartObject();
            
            if(bean instanceof DatasetLayer) {            
            	DatasetLayer datasetLayer = (DatasetLayer)bean;            	
            	
            	String type;
            	if(datasetLayer.isVectorLayer()) {
            		type = "vector";
            	} else if(datasetLayer.isRasterLayer()) {
            		type = "raster";
            	} else {
            		throw new IllegalArgumentException("unknown layer type");
            	}
            	
            	jgen.writeStringField("type", type);
            } else {
            	throw new IllegalArgumentException("DatasetLayer bean expected");
            }
            
            serializeFields(bean, jgen, provider);
            
            jgen.writeEndObject();
		}
		
	}
	
	public static String toJson(Service service) {
		ObjectMapper objectMapper = new ObjectMapper();
		
		SimpleModule module = new SimpleModule("JsonService", Version.unknownVersion()) {
			
			private static final long serialVersionUID = -5062700192805231474L;
		
			@Override
			public void setupModule(SetupContext context) {
				context.addBeanSerializerModifier(new BeanSerializerModifier() {

					@Override
					public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
						Class<?> handledType = serializer.handledType();
						if(Optional.class.equals(handledType)) {
							return new OptionalJsonSerializer();
						}
						
						if(serializer instanceof BeanSerializerBase) {
							BeanSerializerBase beanSerializerBase = (BeanSerializerBase)serializer;
							
							if(Service.class.equals(handledType)) {
								return new ServiceSerializer(beanSerializerBase);
							}
							
							if(DatasetLayer.class.isAssignableFrom(handledType)) {
								return new DatasetLayerSerializer(beanSerializerBase);
							}
						}
						
						return serializer;
					}
				});
			}		
		};
				
		objectMapper.registerModule(module);
		
		objectMapper.setSerializationInclusion(Include.NON_EMPTY);
		
		objectMapper.addMixInAnnotations(Service.class, ServiceMixin.class);
		objectMapper.addMixInAnnotations(DatasetLayerRef.class, DatasetLayerRefMixin.class);
		objectMapper.addMixInAnnotations(VectorDatasetLayer.class, VectorDatasetLayerMixin.class);
		objectMapper.addMixInAnnotations(GroupLayerRef.class, GroupLayerRefMixin.class);
		objectMapper.addMixInAnnotations(GroupLayer.class, GroupLayerMixin.class);
		objectMapper.addMixInAnnotations(StyleRef.class, StyleRefMixin.class);		
		objectMapper.addMixInAnnotations(Tiling.class, TilingMixin.class);
		
		try {
			return objectMapper.writeValueAsString(service);
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
