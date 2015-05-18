/*******************************************************************************
 * Copyright (C) 2015 Fabio Cervo, Vicente Medina
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package RiDI_Column_Lib;

import org.apache.log4j.Logger;
import org.math.array.DoubleArray;



public class StabilityFactor {

	//Pointer to the basin object
	static Basin ProjectBasin;
	
	//Logger
    private static Logger logger;
		
	public StabilityFactor(Basin AuxProjectBasin) {
		
		
        //Create logger and log file
        logger = Logger.getLogger("RiDI.SoilColumn");
		
		//Point to the data
		ProjectBasin = AuxProjectBasin;	
		
	}
	
	
	public void TimeStep(double dt) {
		
		//Loop over all the raster layer cells

		//Logger
		logger.info("Call to TimeStep(" + dt + ").");

		
        /*
         * Number of cells and columns
         */
        int iNX = ProjectBasin.FillTopoRaster.getNX();
        int iNY = ProjectBasin.FillTopoRaster.getNY();

        /*
         * Loop for all grid cells
         */
        for (int y = 0; y < iNY; y++) {
            for (int x = 0; x < iNX; x++) {
            	for (int z = 0; z < ProjectBasin.numNodes; z++){
            		ComputeStability(x, y, z);
            	}
            	
            	//Find column min
            	ComputeMinStability(x, y);

            }
        }
	}

	private void ComputeStability(int i, int j, int k) {
       
	
		//Log
		logger.trace("Call to ComputeStability(" + i + "," + j + "," + k + ").");

		

		// call Static properties
		double phi0 = ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties.nodeSoilType[k].phi0;
		double dphi = ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties.nodeSoilType[k].dphi;
		double gw = ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties.nodeSoilType[k].gw;
		double gsat = ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties.nodeSoilType[k].gsat;
		double Gs = ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties.nodeSoilType[k].Gs;
		double e = ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties.nodeSoilType[k].e;
		double c = ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties.nodeSoilType[k].c;
		double c_root = ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties.nodeSoilType[k].c_root;
		
		// call depth nodes
		double z = ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties.nodeZ[k];
		
		// get slope
		double s = ProjectBasin.SlopeRaster.getCellValueAsDouble(i,j);
		
		//Control slope minimum value to avoid NaN
		s = Math.max(s, 0.000001);
		
		// call Dynamic variables
		double uw = ProjectBasin.BasinSoilColumns[i][j].ColumnDynamicVariables.SuctionOld[k];
		double Se = ProjectBasin.BasinSoilColumns[i][j].ColumnDynamicVariables.Saturation[k];
		double Hwt = ProjectBasin.BasinSoilColumns[i][j].ColumnDynamicVariables.WaterTable;
       
		
		//UNSATURATED
		// calculate some variable properties
		double zw = z;
		double phi = phi0 + (dphi/(1+(zw/(Hwt - z))));
		double g = (gw/1000)*((Gs+(e*Se))/(1+e));
		double ru = ((gw/1000)*uw*Se)/(g*(Hwt - z));
				
		// calculate FOS's components and FOS (Lu & Godt 2012, pg 326) "UNSATURATED"
		double FOS_f = Math.tan(phi)/Math.tan(s);
		double FOS_c = (2*(c+c_root))/(g*(Hwt - z)*Math.sin(2*s));
		double FOS_uw = -ru*(Math.tan(s)+(1/Math.tan(s)))*Math.tan(phi);
		
		double FOS = FOS_f + FOS_c + FOS_uw;
		
		
		//SATURATED (classic)
		// calculate some variable properties;
		phi = phi0;
		double g2 = gsat; // gamma sat
		double ru2 = (uw*(gw/1000))/(gsat*(Hwt - z));
		
		// calculate FOS's components and FOS "SATURATED"
		double FOS_f2 = Math.tan(phi)/Math.tan(s);
		double FOS_c2 = (2*(c+c_root))/(g2*(Hwt - z)*Math.sin(2*s));
		double FOS_uw2 = -(ru2*(Math.tan(s)+(1/Math.tan(s)))*Math.tan(phi));
		
		double FOS2 = FOS_f2 + FOS_c2 + FOS_uw2;
		
		//Store FOS results as dynamic variable
		ProjectBasin.BasinSoilColumns[i][j].ColumnDynamicVariables.FOS_unsat[k] = Math.min(FOS,10.0);
		ProjectBasin.BasinSoilColumns[i][j].ColumnDynamicVariables.FOS_sat[k]   = Math.min(FOS2,10.0);
		
		//Store suction stress values defined by Lu (2010)
		ProjectBasin.BasinSoilColumns[i][j].ColumnDynamicVariables.SuctionStress[k] = -(gw/1000)*uw*Se;
				
	}
			

	
	private void ComputeMinStability(int i, int j) {
	       
		
		//Log
		logger.trace("Call to ComputeMinStability(" + i + "," + j + ").");

		//Find min
		double min_FOS_Sat = DoubleArray.max(ProjectBasin.BasinSoilColumns[i][j].ColumnDynamicVariables.FOS_sat);
		double min_FOS_Unsat = DoubleArray.max(ProjectBasin.BasinSoilColumns[i][j].ColumnDynamicVariables.FOS_unsat);

		//Set min
		ProjectBasin.FactorStabilitySatRaster.setCellValue(i, j, min_FOS_Sat);
		ProjectBasin.FactorStabilityUnsatRaster.setCellValue(i,j,min_FOS_Unsat);
		
	}


}

