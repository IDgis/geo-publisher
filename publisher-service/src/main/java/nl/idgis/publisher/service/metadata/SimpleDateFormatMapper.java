package nl.idgis.publisher.service.metadata;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import akka.dispatch.Mapper;

public class SimpleDateFormatMapper extends Mapper<String, Date> {
	
	private final String[] patterns;
	
	public SimpleDateFormatMapper(String... patterns) {
		this.patterns = patterns;
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
