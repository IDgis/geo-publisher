package nl.idgis.publisher.database;

import java.util.Arrays;

import com.mysema.query.Tuple;

public abstract class AbstractTuple implements Tuple {

	@Override
	public int hashCode() {
		return Arrays.hashCode(toArray());
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Tuple) {
			return Arrays.equals(toArray(), ((Tuple)obj).toArray());
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return Arrays.toString(toArray());
	}
}
