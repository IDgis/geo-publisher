package nl.idgis.publisher.domain.query;

import java.util.Objects;

import nl.idgis.publisher.domain.response.UniqueNameValidationResult;

public class ValidateUniqueName implements DomainQuery<UniqueNameValidationResult> {	

	private static final long serialVersionUID = -8948822157181419535L;
	
	private final String name;

	public ValidateUniqueName(String name) {
		this.name = Objects.requireNonNull(name);
	}
	
	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return "ValidateUniqueName [name=" + name + "]";
	}
}
