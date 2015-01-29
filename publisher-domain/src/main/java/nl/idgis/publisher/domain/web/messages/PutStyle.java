/**
 * 
 */
package nl.idgis.publisher.domain.web.messages;

import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.Identifiable;
import nl.idgis.publisher.domain.web.Style;

/**
 * PutStyle combines Style with a crud operation (create or update).
 * @author Rob
 *
 */
public class PutStyle extends Identifiable {

	private static final long serialVersionUID = 1087907574578499548L;
	
	private final CrudOperation operation;
	private final Style style;
	
	public PutStyle(CrudOperation operation, Style style) {
		super(style.id());
		this.operation = operation;
		this.style = style;
	}

	public CrudOperation getOperation() {
		return operation;
	}

	public Style getStyle() {
		return style;
	}
	
}
