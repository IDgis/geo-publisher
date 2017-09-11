package controllers;

import play.mvc.Result;
import play.libs.Json;
import javax.inject.Inject;

public class DCAT {

	private String name;
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}

}
