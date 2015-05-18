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

public class SoilColumnStaticProperties {

	//Basin unit where the column belongs
	private static int columnUnit;
	
	//Logger
    private static Logger logger;

	
	//Soil type of the node
	private String[] nodeSoilTypeName;
	public SoilType[] nodeSoilType;
	public double[] nodeZ;
	public double columnDepth;
	public double maxCondHyd = 0.0;
	
	public SoilColumnStaticProperties(int UnitType) {

        //Create logger and log file
        logger = Logger.getLogger("RiDI.SoilColumnStaticProperties");
		
		//Set the unit of the column
		columnUnit = UnitType;
	}
	
	public void CreateColumnNodes(int numNodes, UnitsCatalog BasinUnitsCatalog, SoilsCatalog BasinSoilsCatalog) {

		//Log
		logger.info("Call to CreateColumnNodes(" + numNodes + "," + BasinUnitsCatalog + "," + BasinSoilsCatalog + ").");
		
		
		//Dim variables
		nodeSoilTypeName = new String[numNodes];
		nodeSoilType = new SoilType[numNodes];
		nodeZ = new double[numNodes];
		columnDepth = BasinUnitsCatalog.getUnitDepth(columnUnit);

		//Loop to identify node soil type
        for (int i = 0; i < nodeSoilType.length; i++) {
        	nodeZ[i] = UnitsCatalog.getNodeZ(columnUnit,numNodes,i);
        	nodeSoilTypeName[i] = UnitsCatalog.getNodeSoilName(columnUnit,numNodes,i);
        	nodeSoilType[i] = BasinSoilsCatalog.getSoilType(nodeSoilTypeName[i]);
        	
        	maxCondHyd = Math.max(maxCondHyd, nodeSoilType[i].Ks);
        }
		
	}
	
    public static int getColumnUnit() {

    	//Create logger and log file
		logger.info("Call to getColumnUnit().");
    	
    	return columnUnit;
	}

	
}
