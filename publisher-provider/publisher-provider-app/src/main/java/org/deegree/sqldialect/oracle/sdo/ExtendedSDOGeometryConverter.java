package org.deegree.sqldialect.oracle.sdo;

import java.sql.SQLException;

import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.geometry.Geometry;
import org.deegree.sqldialect.oracle.sdo.OracleObjectTools;
import org.deegree.sqldialect.oracle.sdo.SDOGeometryConverter;

import oracle.sql.ARRAY;
import oracle.sql.Datum;
import oracle.sql.STRUCT;

@SuppressWarnings("deprecation")
public class ExtendedSDOGeometryConverter extends SDOGeometryConverter {

	@Override
	public Geometry toGeometry( STRUCT sdoStruct, ICRS crs ) throws SQLException {
		Datum data[] = sdoStruct.getOracleAttributes();

		int gtype = OracleObjectTools.fromInteger( data[0], 0 );
		int srid = OracleObjectTools.fromInteger( data[1], -1 );
		double[] point = OracleObjectTools.fromDoubleArray( (STRUCT) data[2], Double.NaN );
		int[] elemInfo = OracleObjectTools.fromIntegerArray( (ARRAY) data[3] );
		double[] ordinates = OracleObjectTools.fromDoubleArray( (ARRAY) data[4], Double.NaN );			
		
		int gtype_d, tdims = gtype / 1000;
        if ( tdims < 2 || tdims > 4 )
            gtype_d = 2;
        else
            gtype_d = tdims;
        
		int gtype_l = ( gtype % 1000 ) / 100;
		if(gtype_l > 0) {
			double[] newOrdinates = new double[(ordinates.length / gtype_d) * (gtype_d - 1)];
			
			int j = 0;
			for(int i = 0; i < ordinates.length; i++) {
				if(i % gtype_d != (gtype_l - 1)) {
					newOrdinates[j++] = ordinates[i];
				}
			}
			
			ordinates = newOrdinates;
			
			int[] newElemInfo = new int[elemInfo.length];
			for(int i = 0; i < newElemInfo.length; i++) {
				if(i % 3 == 0) {
					newElemInfo[i] = ((elemInfo[i] - 1) / gtype_d) * (gtype_d - 1) + 1;
				} else {
					newElemInfo[i] = elemInfo[i];
				}
			}
			
			elemInfo = newElemInfo;
			
			gtype = gtype - 1000 - gtype_l * 100;
		}
		
		return toGeometry(new SDOGeometryConverter.GeomHolder(gtype, srid, point, elemInfo, ordinates, crs), crs);
	}
}
