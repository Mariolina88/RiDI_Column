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

public class UnitType {
	
    //Logger
    private static Logger logger;
	
    //For stratigraphy units the variables are defined as arrays 
    public String SoilType[];
    public double Z_fin[]; 
    public double SoilDepth;

    
	public UnitType(int numberOfStrates) {
		
        //Create logger and log file
        logger = Logger.getLogger("RiDI.UnitType");
        
        SoilType = new String[numberOfStrates];
        Z_fin = new double[numberOfStrates];
        
	}
	
	public void computeSoilDepth() {

		//Log
		logger.trace("Call to computeSoilDepth().");
		
		SoilDepth = max(Z_fin);
		
	}
	
	
	private static double max(double[] array) {
	      
		// Validates input
	      if (array== null) {
	          throw new IllegalArgumentException("ERROR: The array in max() function must not be null");
	      } else if (array.length == 0) {
	          throw new IllegalArgumentException("ERROR: The array in the max() function cannot be empty.");
	      }
	  
	      // Finds and returns max
	      double max = array[0];
	      for (int j = 1; j < array.length; j++) {
	          if ((array[j] > max) && !(Double.isNaN(array[j]))) {
	              max = array[j];
	          }
	      }
	  
	      return max;
	  }
	
	public String getSoilTypeByZ( double Z) {

		String auxString = null;

		auxString = SoilType[0];

		for (int j = 0; j < Z_fin.length; j++) {
			if ((Z_fin[j] <= Z) && !(Double.isNaN(Z_fin[j]))) {
				auxString = SoilType[j];
			}
		}

		return auxString;

	}
	
	
	
}