package nl.idgis.publisher.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import akka.dispatch.Mapper;

public class SimpleDateFormatMapper extends Mapper<String, Date> {
	
	private final String[] patterns;
	
	public SimpleDateFormatMapper(String... patterns) {
		this.patterns = patterns;
	}
	
	public static Mapper<String, Date> isoDateTime() {
		return 
			new SimpleDateFormatMapper(
				"yyyy-MM-dd'T'HH:mm:ss",
				"yyyyMMdd'T'HH:mm:ss",
				"yyyyMMdd'T'HHmmss");	
	}
	
	public static Mapper<String, Date> isoDate() {
		return 
			new SimpleDateFormatMapper(
				"yyyy-MM-dd",
				"yyyyMMdd");
	}

	@Override
	public Date apply(String s) {
		for(String pattern : patterns) {
			try {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
				return simpleDateFormat.parse(s);
			} catch(ParseException e) { }
		}
		
		return null;
	}

}
