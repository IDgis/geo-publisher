package nl.idgis.publisher.loader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.domain.web.Filter;
import nl.idgis.publisher.provider.protocol.database.Record;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FilterEvaluatorTest {

	@Test
	public void testFilter() throws Exception {
		String filterCondition = "{\"expression\":{\"type\":\"operator\",\"operatorType\":\"OR\",\"inputs\":[{\"type\":\"operator\",\"operatorType\":\"AND\",\"inputs\":[{\"type\":\"operator\",\"operatorType\":\"LESS_THAN_EQUAL\",\"inputs\":[{\"type\":\"column-ref\",\"column\":{\"name\":\"OPPV\",\"dataType\":\"NUMERIC\"}},{\"type\":\"value\",\"valueType\":\"NUMERIC\",\"value\":\"10000\"}]}]}]}}";
		
		ObjectMapper objectMapper = new ObjectMapper();						
		Filter filter = objectMapper.reader(Filter.class).readValue(filterCondition);
		
		List<Column> columns = Arrays.asList(new Column("OPPV", Type.NUMERIC));
		FilterEvaluator evaluator = new FilterEvaluator(columns, filter.getExpression());		
		 
		assertFalse(evaluator.evaluate(new Record(Arrays.<Object>asList(5000))));
		assertTrue(evaluator.evaluate(new Record(Arrays.<Object>asList(15000))));
	}
}
