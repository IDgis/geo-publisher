package nl.idgis.publisher.service.json;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
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
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

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
	
	private final Map<String, Optional<String>> metadataFileIdentifications;
	
	public JsonService(JsonNode jsonNode, Map<String, Optional<String>> metadataFileIdentifications) {
		this.jsonNode = jsonNode;
		this.metadataFileIdentifications = metadataFileIdentifications;
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
		return asTextWithDefault (jsonNode.path ("title"), null);
	}

	@Override
	public String getAbstract() {
		return asTextWithDefault (jsonNode.path ("abstract"), null);
	}

	@Override
	public String getContact() {
		return asTextWithDefault (jsonNode.path ("contact"), null);
	}

	@Override
	public String getOrganization() {
		return asTextWithDefault (jsonNode.path ("organization"), null);
	}

	@Override
	public String getPosition() {
		return asTextWithDefault (jsonNode.path ("position"), null);
	}

	@Override
	public String getAddressType() {
		return asTextWithDefault (jsonNode.path ("addressType"), null);
	}

	@Override
	public String getAddress() {
		return asTextWithDefault (jsonNode.path ("address"), null);
	}

	@Override
	public String getCity() {
		return asTextWithDefault (jsonNode.path ("city"), null);
	}

	@Override
	public String getState() {
		return asTextWithDefault (jsonNode.path ("state"), null);
	}

	@Override
	public String getZipcode() {
		return asTextWithDefault (jsonNode.path ("zipcode"), null);
	}

	@Override
	public String getCountry() {
		return asTextWithDefault (jsonNode.path ("country"), null);
	}

	@Override
	public String getTelephone() {
		return asTextWithDefault (jsonNode.path ("telephone"), null);
	}

	@Override
	public String getFax() {
		return asTextWithDefault (jsonNode.path ("fax"), null);
	}

	@Override
	public String getEmail() {
		return asTextWithDefault (jsonNode.path ("email"), null);
	}

	@Override
	public List<String> getKeywords() {
		return 
			getStream(jsonNode, "keywords")
				.map(JsonNode::asText)
				.collect(toList());
	}
	
	@Override
	public List<String> getUserGroups() {
		return 
			getStream(jsonNode, "userGroups")
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
				.map(jsonNode -> AbstractJsonLayerRef.fromJson(jsonNode, metadataFileIdentifications))
				.collect(toList());
	}
	
	@Override
	public boolean isConfidential () {
		return jsonNode.path ("confidential").asBoolean ();
	}
	
	@Override
	public final boolean isWmsOnly () {
		return jsonNode.path ("wmsOnly").asBoolean ();
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
	
	private static class ServiceSerializer extends BeanSerializerBase {
		
		private static final long serialVersionUID = 7685190819553841569L;

		ServiceSerializer(BeanSerializerBase src) {
			super(src);
		}
		
		ServiceSerializer(BeanSerializerBase  src, ObjectIdWriter objectIdWriter) {
            super(src, objectIdWriter);
        }
		
		ServiceSerializer(BeanSerializerBase src, ObjectIdWriter objectIdWriter, Object filterId) {
			super(src, objectIdWriter, filterId);
		}

		@Override
		public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
			return new ServiceSerializer(this, objectIdWriter);
		}		

		@Override
		protected BeanSerializerBase asArraySerializer() {			
			return null;
		}

		@Override
		public BeanSerializerBase withFilterId(Object filterId) {
			return new ServiceSerializer(this, _objectIdWriter, filterId);
		}

		@Override
		public void serialize(Object bean, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
			jgen.writeStartObject();
			jgen.writeNumberField("formatRevision", 1);
            serializeFields(bean, jgen, provider);             
            jgen.writeEndObject();
		}

		@Override
		protected BeanSerializerBase withIgnorals(Set<String> toIgnore) {
			return new ServiceSerializer(this, _objectIdWriter, toIgnore);
		}
		
	}
	
	private static class DatasetLayerSerializer extends BeanSerializerBase {
		
		private static final long serialVersionUID = 7837502906419222795L;

		DatasetLayerSerializer(BeanSerializerBase src) {
			super(src);
		}
		
		DatasetLayerSerializer(BeanSerializerBase  src, ObjectIdWriter objectIdWriter) {
            super(src, objectIdWriter);
        }
		
		DatasetLayerSerializer(BeanSerializerBase src, ObjectIdWriter objectIdWriter, Object filterId) {
			super(src, objectIdWriter, filterId);
		}

		@Override
		public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
			return new DatasetLayerSerializer(this, objectIdWriter);
		}		

		@Override
		protected BeanSerializerBase asArraySerializer() {			
			return null;
		}

		@Override
		public BeanSerializerBase withFilterId(Object filterId) {
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

		@Override
		protected BeanSerializerBase withIgnorals(Set<String> toIgnore) {
			return new DatasetLayerSerializer(this, _objectIdWriter, toIgnore);
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
			
		objectMapper.registerModule(new Jdk8Module());
		objectMapper.registerModule(module);
		
		objectMapper.setSerializationInclusion(Include.NON_EMPTY);
		
		objectMapper.addMixIn(Service.class, ServiceMixin.class);
		objectMapper.addMixIn(DatasetLayerRef.class, DatasetLayerRefMixin.class);
		objectMapper.addMixIn(VectorDatasetLayer.class, VectorDatasetLayerMixin.class);
		objectMapper.addMixIn(GroupLayerRef.class, GroupLayerRefMixin.class);
		objectMapper.addMixIn(GroupLayer.class, GroupLayerMixin.class);
		objectMapper.addMixIn(StyleRef.class, StyleRefMixin.class);		
		objectMapper.addMixIn(Tiling.class, TilingMixin.class);
		
		try {
			return objectMapper.writeValueAsString(service);
		} catch(Exception e) {
			throw new RuntimeException("Couldn't generate json from service", e);
		}
	}

	public static Service fromJson(String json, Map<String, Optional<String>> metadataFileIdentifications) {
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			JsonNode jsonNode = objectMapper.readValue(json, JsonNode.class);
			int formatRevision = jsonNode.get("formatRevision").asInt();
			if(formatRevision != 1) {
				throw new IllegalArgumentException("unsupported format revision: " + formatRevision);
			}
			
			return new JsonService(jsonNode, metadataFileIdentifications);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't construct service from json", e);
		}
	}
	
	static Stream<JsonNode> getStream(JsonNode jsonNode, String fieldName) {
		JsonNode array = jsonNode.path(fieldName);
		
		return 
			range(0, array.size())
				.mapToObj(array::path);
	}
	
	static Optional<JsonNode> getOptional(JsonNode jsonNode, String fieldName) {
		if(jsonNode.has(fieldName)) {
			return Optional.of(jsonNode.get(fieldName));
		} else {
			return Optional.empty();
		}
	}

	static String asTextWithDefault (final JsonNode jsonNode, final String defaultValue) {
		if (jsonNode.isMissingNode () || !jsonNode.isValueNode ()) {
			return defaultValue;
		} else {
			return jsonNode.asText ();
		}
	}
}
