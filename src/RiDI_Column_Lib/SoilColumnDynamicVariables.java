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


public class SoilColumnDynamicVariables {

	//Suction variables
	public double[] SuctionOld;
	public double[] SuctionTemp; //Variable used to store the internal iterations to solve the nonlinear system
	public double[] SuctionNew; //Variable used to store the new value
	
	//Water content
	public double[] ThetaOld;
	public double[] ThetaNew;	
	
	public double WaterTable;
	
	//Hydraulic values
	public double[] Saturation;
	public double[] HydConductivity;
	public double[] Capacity;
	
	//Velocity
	public double[] velOld;
	public double[] velNew;
	
	//Safety factors
	public double[] FOS_unsat;
	public double[] FOS_sat;
	public double[] SuctionStress;
	
	//Boundary conditions
	public double topBoundaryCondition;
	public double botBoundaryCondition;
	public boolean topBoundCondInfiltration = true;
	public boolean botBoundCondInfiltration = true;
	
	//Logger
    private static Logger logger;
    
	
	public SoilColumnDynamicVariables() {
		
        //Create logger and log file
        logger = Logger.getLogger("RiDI.SoilColumnDynamicVariables");

	}

	
	public void CreateColumnNodes(int numNodes) {
		
		//Log
		logger.trace("Call to CreateColumnNodes(" + numNodes + ").");		

		//Dim variables and fill with 0's
		SuctionOld =  DoubleArray.fill(numNodes,0.0);
		ThetaOld = DoubleArray.fill(numNodes,0.0);
		Saturation = DoubleArray.fill(numNodes,0.0);
		HydConductivity = DoubleArray.fill(numNodes,0.0);
		Capacity = DoubleArray.fill(numNodes,0.0);
		velOld = DoubleArray.fill(numNodes, 0.0);
		FOS_unsat = DoubleArray.fill(numNodes,0.0);
		FOS_sat = DoubleArray.fill(numNodes, 0.0);
		SuctionStress = DoubleArray.fill(numNodes, 0.0);
		
		
	}
	
	public void InitializeColumnAuxVariables() {

		SuctionTemp = DoubleArray.copy(SuctionOld);
		SuctionNew = DoubleArray.copy(SuctionOld);
		ThetaNew = DoubleArray.copy(ThetaOld);
		velNew = DoubleArray.copy(velOld);

	}

	public void ReInitializeColumnVariables() {

		ThetaOld = DoubleArray.copy(ThetaNew);
		velOld = DoubleArray.copy(velNew);

	}
}
