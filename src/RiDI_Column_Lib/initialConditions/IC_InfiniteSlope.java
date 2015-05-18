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
package RiDI_Column_Lib.initialConditions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import RiDI_Column_Lib.Basin;
import RiDI_Column_Lib.SoilColumn;
import RiDI_Column_Lib.interfaces.InterfaceInitialConditions;

import org.apache.log4j.Logger;


public class IC_InfiniteSlope implements InterfaceInitialConditions {

	//Logger
    private static Logger logger;

	//Pointer to the basin object
	Basin ProjectBasin;    
	
	//Variables
	double rainfallIntensity;
	
    
	public IC_InfiniteSlope(Basin AuxProjectBasin) {
		
		//Create logger and log file
        logger = Logger.getLogger("RiDI.initialConditions");

		//Point to the data
		ProjectBasin = AuxProjectBasin;

	}

	/* (non-Javadoc)
	 * @see RiDI_Column_Lib.InterfaceInitialConditions#ComputeInitialConditions()
	 */
	public void ComputeInitialConditions() {

		//Log
		logger.info("Call to ComputeInitialConditions().");

		
		//Loop over all the raster layer cells
		
		
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
                ComputeColumnInitialCondition(x, y);
            }
        }
		
	}
	
	
	private void ComputeColumnInitialCondition(int i, int j) {
		
		//Log
		logger.trace("Call to ComputeColumnInitilCondition(" + i + "," + j + "," + ").");
		
		//Column
		SoilColumn cellSoilColumn = ProjectBasin.BasinSoilColumns[i][j];
		
        //Cell size
		double XY = ProjectBasin.DepthRaster.getLayerCellSize();
		
		//Raster properties
        double Area = ProjectBasin.AccFlowRaster.getCellValueAsDouble(i, j);
        double Elevation = ProjectBasin.FillTopoRaster.getCellValueAsDouble(i, j);
        double Rainfall = ProjectBasin.RainIntensityRaster.getCellValueAsDouble(i, j);
        double Slope = ProjectBasin.SlopeRaster.getCellValueAsDouble(i, j);

        
        /**********************************************************
         * Compute infinite slope hydrological model
         * Set the rasters values
         * 		ProjectBasin.DepthRaster.setCellValue(i, j, XXX);
         * 		ProjectBasin.Discharge.setCellValue(i, j, XXX);
         **********************************************************/

        for (int k = 0; k < cellSoilColumn.ColumnDynamicVariables.SuctionOld.length; k++) {

        	//Set the suction, saturation and moisture values for the whole column
        	cellSoilColumn.ColumnDynamicVariables.SuctionOld[k] = 0.0;
        	cellSoilColumn.ColumnDynamicVariables.Saturation[k] = 0.0;
        	cellSoilColumn.ColumnDynamicVariables.ThetaNew[k] = 0.0;
        	
        	
        }
        

		//Update water table values
		ProjectBasin.BasinSoilColumns[i][j].ComputeWaterTable();
		
	}

	
	public void ReadInputFile(String fileName) {

		//Log
		logger.trace("Call to ReadInputFile(" + fileName + ").");
		
		//Open file and create reader
		File file = new File(fileName);
        BufferedReader reader = null;

        //Tokenizer
        String delims = "[ =\\t]+";

        //IO into exceptions catch
        try {
            reader = new BufferedReader(new FileReader(file));
            String text = null;

            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                rainfallIntensity = Double.parseDouble(tokens[2]); //(mm/hr)
            }
                    
        } catch (FileNotFoundException ex) {
            logger.error("Exception!",ex); //.printStackTrace();
        } catch (IOException ex) {
            logger.error("Exception!",ex);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                logger.error("Exception!",ex);            	
            }
        }

		
	}
	
	

}
