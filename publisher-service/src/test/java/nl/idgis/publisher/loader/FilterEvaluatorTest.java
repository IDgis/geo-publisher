package nl.idgis.publisher.loader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.domain.web.Filter;
import nl.idgis.publisher.domain.web.Filter.FilterExpression;
import nl.idgis.publisher.domain.web.Filter.OperatorType;
import nl.idgis.publisher.domain.web.Filter.OperatorExpression;
import nl.idgis.publisher.domain.web.Filter.ValueExpression;
import nl.idgis.publisher.domain.web.Filter.ColumnReferenceExpression;
import nl.idgis.publisher.loader.FilterEvaluator.BooleanValue;
import nl.idgis.publisher.loader.FilterEvaluator.DateValue;
import nl.idgis.publisher.loader.FilterEvaluator.Value;
import nl.idgis.publisher.provider.protocol.database.Record;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FilterEvaluatorTest {

	@Test
	public void testFilter() throws Exception {
		String filterCondition = "{\"expression\":{\"type\":\"operator\",\"operatorType\":\"OR\",\"inputs\":[{\"type\":\"operator\",\"operatorType\":\"AND\",\"inputs\":[{\"type\":\"operator\",\"operatorType\":\"LESS_THAN_EQUAL\",\"inputs\":[{\"type\":\"column-ref\",\"column\":{\"name\":\"OPPV\",\"dataType\":\"NUMERIC\"}},{\"type\":\"value\",\"valueType\":\"NUMERIC\",\"value\":\"10000\"}]}]}]}}";
		
		ObjectMapper objectMapper = new ObjectMapper();						
		
		Filter filter = objectMapper.reader(Filter.class).readValue(filterCondition);
		assertNotNull(filter);
		
		FilterExpression expression = filter.getExpression();
		assertNotNull(expression);
		
		Set<Column> requiredColumns =  FilterEvaluator.getRequiredColumns(expression);
		assertNotNull(requiredColumns);
		assertEquals(1, requiredColumns.size());
		assertTrue(requiredColumns.contains(new Column("OPPV", Type.NUMERIC)));
		
		List<Column> columns = Arrays.asList(new Column("OPPV", Type.NUMERIC));
		FilterEvaluator evaluator = new FilterEvaluator(columns, expression);
		 
		assertFalse(evaluator.evaluate(new Record(Arrays.<Object>asList(5000))));
		assertTrue(evaluator.evaluate(new Record(Arrays.<Object>asList(15000))));
	}
	
	@Test
	public void testAnd() {
		FilterEvaluator evaluator = new FilterEvaluator(Collections.<Column>emptyList(), null);
		
		assertEquals(
			BooleanValue.TRUE,
				
			evaluator.evaluate(null, 
				new OperatorExpression(
						OperatorType.AND, 
						Arrays.<FilterExpression>asList(
								new ValueExpression(Type.BOOLEAN, "true"),
								new ValueExpression(Type.BOOLEAN, "true")))));
		
		assertEquals(
			BooleanValue.FALSE,
				
			evaluator.evaluate(null, 
				new OperatorExpression(
						OperatorType.AND, 
						Arrays.<FilterExpression>asList(
								new ValueExpression(Type.BOOLEAN, "true"),
								new ValueExpression(Type.BOOLEAN, "false")))));
	}
	
	@Test
	public void testOr() {
		FilterEvaluator evaluator = new FilterEvaluator(Collections.<Column>emptyList(), null);
		
		assertEquals(
			BooleanValue.TRUE,
				
			evaluator.evaluate(null, 
				new OperatorExpression(
						OperatorType.OR, 
						Arrays.<FilterExpression>asList(
								new ValueExpression(Type.BOOLEAN, "true"),
								new ValueExpression(Type.BOOLEAN, "true")))));
		
		assertEquals(
			BooleanValue.TRUE,
				
			evaluator.evaluate(null, 
				new OperatorExpression(
						OperatorType.OR, 
						Arrays.<FilterExpression>asList(
								new ValueExpression(Type.BOOLEAN, "true"),
								new ValueExpression(Type.BOOLEAN, "false")))));
	}
	
	@Test
	public void testEquals() {
		FilterEvaluator evaluator = new FilterEvaluator(Collections.<Column>emptyList(), null);
		
		assertEquals(
				BooleanValue.TRUE,
					
				evaluator.evaluate(null, 
					new OperatorExpression(
							OperatorType.EQUALS, 
							Arrays.<FilterExpression>asList(
									new ValueExpression(Type.TEXT, "Hello world!"),
									new ValueExpression(Type.TEXT, "Hello world!")))));
		
		assertEquals(
				BooleanValue.FALSE,
					
				evaluator.evaluate(null, 
					new OperatorExpression(
							OperatorType.EQUALS, 
							Arrays.<FilterExpression>asList(
									new ValueExpression(Type.TEXT, "Hello world!"),
									new ValueExpression(Type.TEXT, "Another String!")))));
		
		assertEquals(
				BooleanValue.TRUE,
					
				evaluator.evaluate(null, 
					new OperatorExpression(
							OperatorType.EQUALS, 
							Arrays.<FilterExpression>asList(
									new ValueExpression(Type.NUMERIC, "42"),
									new ValueExpression(Type.NUMERIC, "42")))));
		
		assertEquals(
				BooleanValue.FALSE,
					
				evaluator.evaluate(null, 
					new OperatorExpression(
							OperatorType.EQUALS, 
							Arrays.<FilterExpression>asList(
									new ValueExpression(Type.NUMERIC, "42"),
									new ValueExpression(Type.NUMERIC, "47")))));
	}
	
	@Test
	public void testLike() {
		FilterEvaluator evaluator = new FilterEvaluator(Collections.<Column>emptyList(), null);
		
		assertEquals(
				BooleanValue.TRUE,
					
				evaluator.evaluate(null, 
					new OperatorExpression(
							OperatorType.LIKE, 
							Arrays.<FilterExpression>asList(
									new ValueExpression(Type.TEXT, "Hello world!"),
									new ValueExpression(Type.TEXT, "Hello%")))));
		
		assertEquals(
				BooleanValue.TRUE,
					
				evaluator.evaluate(null, 
					new OperatorExpression(
							OperatorType.LIKE, 
							Arrays.<FilterExpression>asList(
									new ValueExpression(Type.TEXT, "Hello world!"),
									new ValueExpression(Type.TEXT, "?ello%")))));
		
		assertEquals(
				BooleanValue.FALSE,
					
				evaluator.evaluate(null, 
					new OperatorExpression(
							OperatorType.LIKE, 
							Arrays.<FilterExpression>asList(
									new ValueExpression(Type.TEXT, "Another String!"),
									new ValueExpression(Type.TEXT, "Hello%")))));
	}
	
	@Test
	public void testIn() {
		FilterEvaluator evaluator = new FilterEvaluator(Collections.<Column>emptyList(), null);
		
		assertEquals(
				BooleanValue.TRUE,
					
				evaluator.evaluate(null, 
					new OperatorExpression(
							OperatorType.IN, 
							Arrays.<FilterExpression>asList(
									new ValueExpression(Type.TEXT, "a"),
									new ValueExpression(Type.TEXT, "a, b, c")))));
		
		assertEquals(
				BooleanValue.TRUE,
					
				evaluator.evaluate(null, 
					new OperatorExpression(
							OperatorType.IN, 
							Arrays.<FilterExpression>asList(
									new ValueExpression(Type.TEXT, "c"),
									new ValueExpression(Type.TEXT, "a, b, c")))));
		
		assertEquals(
				BooleanValue.FALSE,
					
				evaluator.evaluate(null, 
					new OperatorExpression(
							OperatorType.IN, 
							Arrays.<FilterExpression>asList(
									new ValueExpression(Type.TEXT, "z"),
									new ValueExpression(Type.TEXT, "a, b, c")))));
		
		// the different parts of the expression are to be converted 
		// to the same type as the value before comparison
		assertEquals(
				BooleanValue.TRUE,
					
				evaluator.evaluate(null, 
					new OperatorExpression(
							OperatorType.IN, 
							Arrays.<FilterExpression>asList(
									new ValueExpression(Type.NUMERIC, "42"),
									new ValueExpression(Type.TEXT, "010, 020, 042")))));
	}
	
	@Test
	public void testDateValue() {
		DateValue dateValue = (DateValue)Value.toValue(Type.DATE, "20140826T15:07:00");
		assertNotNull(dateValue);
		
		Date date = dateValue.getValue();
		assertNotNull(date);
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		
		assertEquals(2014, calendar.get(Calendar.YEAR));
		assertEquals(Calendar.AUGUST, calendar.get(Calendar.MONTH));
		assertEquals(26, calendar.get(Calendar.DATE));		
		assertEquals(15, calendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(7, calendar.get(Calendar.MINUTE));
		
		String stringValue = dateValue.getStringValue();
		assertNotNull(stringValue);
		
		dateValue = (DateValue)Value.toValue(Type.DATE, stringValue);
		assertNotNull(dateValue);
		
		date = dateValue.getValue();		
		assertNotNull(date);
		
		calendar = Calendar.getInstance();
		calendar.setTime(date);
		
		assertEquals(2014, calendar.get(Calendar.YEAR));
		assertEquals(Calendar.AUGUST, calendar.get(Calendar.MONTH));
		assertEquals(26, calendar.get(Calendar.DATE));		
		assertEquals(15, calendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(7, calendar.get(Calendar.MINUTE));
	}
	
	@Test
	public void testNotNull() {
		Column testColumn = new Column("col0", Type.TEXT);		
		FilterEvaluator evaluator = new FilterEvaluator(Arrays.asList(testColumn), null);
		
		assertEquals(
				BooleanValue.TRUE,
					
				evaluator.evaluate(new Record(Arrays.<Object>asList("Hello world!")), 
					new OperatorExpression(
							OperatorType.NOT_NULL,
							Arrays.<FilterExpression>asList(
									new ColumnReferenceExpression(testColumn)))));
		
		assertEquals(
				BooleanValue.FALSE,

				evaluator.evaluate(new Record(Arrays.<Object>asList(new Object[]{null})), 
					new OperatorExpression(
							OperatorType.NOT_NULL,
							Arrays.<FilterExpression>asList(
									new ColumnReferenceExpression(testColumn)))));
	}
	
	@Test
	public void testNullColumnValueEquals() {
		Column testColumn = new Column("col0", Type.TEXT);		
		FilterEvaluator evaluator = new FilterEvaluator(Arrays.asList(testColumn), null);
		
		assertEquals(
				BooleanValue.FALSE,
					
				evaluator.evaluate(new Record(Arrays.asList(new Object[]{null})), 
					new OperatorExpression(
							OperatorType.EQUALS, 
							Arrays.<FilterExpression>asList(
									new ColumnReferenceExpression(testColumn),
									new ValueExpression(Type.TEXT, "testString")))));
	}
}
