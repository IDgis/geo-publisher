/**
 *
 */
package nl.idgis.publisher.domain.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import akka.util.ByteString;
import akka.util.CompactByteString;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.query.annotations.QueryProjection;

/**
 * Representation of user defined style.
 * 
 * @author Rob
 *
 */
public class Style extends Identifiable {
	private static final long serialVersionUID = -103047524556298813L;
	private final String name;
	private final CompactByteString definition;
	private final StyleType styleType;
	private final Boolean inUse;

	@JsonCreator
	@QueryProjection
	public Style(
			final @JsonProperty("id") String id, 
			final @JsonProperty("name") String name,
			final @JsonProperty("definition") String definition,
			final @JsonProperty("styleType") String styleType,
			final @JsonProperty("inUse") Boolean inUse) {
		super(id);
		
		this.name = name;
		this.definition = encodeStyleBody (definition).compact ();
		this.styleType = StyleType.valueOf(styleType);
		this.inUse = inUse;
	}

	@JsonGetter
	public String name() {
		return name;
	}

	@JsonGetter
	public String definition() {
		return decodeStyleBody (definition);
	}
	
	@JsonGetter
	public StyleType styleType() {
		return styleType;
	}
	
	@JsonGetter
	public Boolean inUse() {
		return inUse;
	}
	
	private static ByteString encodeStyleBody (final String style) {
		if (style == null || style.isEmpty ()) {
			return ByteString.empty ();
		}
		
		try (final ByteArrayOutputStream output = new ByteArrayOutputStream ()) {
			try (final GZIPOutputStream gzipStream = new GZIPOutputStream (output)) {
				try (final Writer writer = new OutputStreamWriter (gzipStream, Charset.forName ("UTF-8"))) {
					writer.write (style);
				}
			}
			
			return ByteString.fromArray (output.toByteArray ());
		} catch (IOException e) {
			throw new RuntimeException (e);
		}
	} 
	
	private static String decodeStyleBody (final ByteString byteString) {
		if (byteString.isEmpty ()) {
			return "";
		}
		
		try (final ByteArrayInputStream input = new ByteArrayInputStream (byteString.toArray ())) {
			try (final GZIPInputStream gzipStream = new GZIPInputStream (input)) {
				try (final Reader reader = new InputStreamReader (gzipStream, Charset.forName ("UTF-8"))) {
					final StringBuilder builder = new StringBuilder ();
					final char[] buffer = new char[512];
					int n;
					
					while ((n = reader.read (buffer)) >= 0) {
						if (n == buffer.length) {
							builder.append (buffer);
						} else if (n > 0) {
							builder.append (Arrays.copyOf (buffer, n));
						}
					}
					
					return builder.toString ();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException (e);
		}
	}
}