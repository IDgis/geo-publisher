package controllers;

import javax.inject.Inject;

import play.libs.Json;

public class DatasetMetadataDCAT {

	//private final DatasetQueryBuilder dqb;
	
	DCAT dcatResult = new DCAT()
	
	/**
	 @Inject
	 
	public DatasetMetadataDCAT(DatasetQueryBuilder dqb) throws Exception {
		this.dqb = dqb;
	}
	*//
	public Result index() {
		dcatResult.setName("M. Faber")
		return ok(Json.toJson(dcatResult))
	}
	
	
}
