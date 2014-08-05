package nl.idgis.publisher.domain.response;

import java.io.Serializable;

import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;

public class Response <T extends Serializable> implements Serializable{
	
	private static final long serialVersionUID = -9180605248639806306L;

	private CrudOperation operation;
	private CrudResponse operationresponse;
	private T value;
	
	public Response(CrudOperation operation, CrudResponse operationresponse, T value) {
		super();
		this.operation = operation;
		this.operationresponse = operationresponse;
		this.value = value;
	}
	
	public CrudOperation getOperation() {
		return operation;
	}
	public CrudResponse getOperationresponse() {
		return operationresponse;
	}
	public Object getValue(){
		return value;
	}
	
	@Override
	public String toString() {
		return "Response [operation=" + operation + ", operationresponse="
				+ operationresponse + ", value=" + value + "]";
	}
	
}
