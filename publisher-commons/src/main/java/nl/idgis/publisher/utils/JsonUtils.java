package nl.idgis.publisher.utils;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

	public static <T> T fromJson(Class<T> clazz, String json) throws JsonProcessingException, IOException {
		ObjectMapper om = new ObjectMapper();
		return om.reader(clazz).readValue(json);
	}

	public static String toJson(Object content) throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();
		om.setSerializationInclusion(Include.NON_NULL);
		return om.writeValueAsString(content);
	}
}
