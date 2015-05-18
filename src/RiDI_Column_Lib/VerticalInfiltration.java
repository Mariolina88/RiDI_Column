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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.math.array.DoubleArray;

public class VerticalInfiltration {

	//Pointer to the basin object
	private Basin ProjectBasin;
	
	//Richards 1D solver
	private Richards1D richardsSolver;
	
    //Logger
    private static Logger logger;
    
    //Bottom boundary conditions
    BottomBoundaryCondition ProjectBottomBoundaryCondition = new BottomBoundaryCondition();


	public VerticalInfiltration(Basin AuxProjectBasin) {
		
        //Create logger and log file
        logger = Logger.getLogger("RiDI.VerticalInfiltration");

		//Point to the data
		ProjectBasin = AuxProjectBasin;

	}
	
	
	public void TimeStep(double dt, double dtMin, double TolTh, double TolH, int MaxIt) {
		
		//Log
		logger.info("Call to TimeStep(" + dt + ").");

		
		//Loop over all the raster layer cells
		int iNX = ProjectBasin.FillTopoRaster.getNX();
        int iNY = ProjectBasin.FillTopoRaster.getNY();

        for (int y = 0; y < iNY; y++) {
            for (int x = 0; x < iNX; x++) {
            	
        		//Local time step
        		double dtLocal = 0.0;
            	
        		//The infiltration algorithm reduces time step, so a internal control and iteration is necessary
        		// to guarantee cells synchronization
        		while(dtLocal < dt) {
                	dtLocal = dtLocal + ComputeInfiltration(x, y, (dt - dtLocal), dtMin, TolTh, TolH, MaxIt);  
                	
                	//Advance values for the next step (linear extrapolation
                	VariablesExtrapolation(x, y, dt, (dt - dtLocal));
        		}
            }
        }
		
		
	}
	
	private double ComputeInfiltration(int i, int j, double dt, double dtMin, double TolTh, double TolH, int MaxIt) {
		
		//Log
		logger.trace("Call to ComputeInfiltration(" + i + "," + j + "," + dt + ").");
		
		//Call the solver passing the columns parameters
		double dtLocal = richardsSolver.timeStep(TolTh, TolH, MaxIt, dt, dtMin, ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties,
				ProjectBasin.BasinSoilColumns[i][j].ColumnDynamicVariables, ProjectBasin.DepthRaster.getCellValueAsDouble(i, j),
				ProjectBasin.RainIntensityRaster.getCellValueAsDouble(i, j),ProjectBottomBoundaryCondition);

		//Update cell depth value, only if head is > 0 (ponding)
		int TopNodeNum = ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties.nodeZ.length;
		ProjectBasin.DepthRaster.setCellValue(i, j, Math.max(ProjectBasin.BasinSoilColumns[i][j].ColumnDynamicVariables.SuctionNew[TopNodeNum-1],0.0));
		
		//Update water table values
		ProjectBasin.Watertable.setCellValue(i, j, ProjectBasin.BasinSoilColumns[i][j].ComputeWaterTable());
		
		return dtLocal;
		
	}


	public void InitializeAuxVariables(double maxPonding) {
		
		//Log
		logger.info("Call to InitializeAuxVariables().");

		//Create solver class
		int nodesNumber = ProjectBasin.numNodes;
		richardsSolver = new Richards1D(nodesNumber,maxPonding);
		
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
            	
            	//Initialize some new dynamic properties arrays
            	ProjectBasin.BasinSoilColumns[x][y].ColumnDynamicVariables.InitializeColumnAuxVariables();
            	
            	//Fit new dynamic properties to interpolation table values
            	richardsSolver.updateNodeProperties(ProjectBasin.BasinSoilColumns[x][y].ColumnStaticProperties, ProjectBasin.BasinSoilColumns[x][y].ColumnDynamicVariables);
            	
            	//Update old dynamic arrays with the fitted values 
            	ProjectBasin.BasinSoilColumns[x][y].ColumnDynamicVariables.ReInitializeColumnVariables();
            	
            }
        }
				
	}
	
	
	public void updateRainfall(double t, Climate BasinClimate) {
		
		//Log
		logger.info("Call to updateRainfall(" + t + "," + BasinClimate + ").");

		//Get rainfall intensity value
		double precipitationIntensity = BasinClimate.getRainfall(t);
		
		//Loop over all the raster layer cells
        int iNX = ProjectBasin.FillTopoRaster.getNX();
        int iNY = ProjectBasin.FillTopoRaster.getNY();

        /*
         * Loop for all grid cells
         */
        for (int y = 0; y < iNY; y++) {
            for (int x = 0; x < iNX; x++) {
            	ProjectBasin.RainIntensityRaster.setCellValue(x, y, precipitationIntensity);
            }
        }
		
	}
	
	public void readBottomInfiltration(String FileName) {
		
		//Log
		logger.info("Call to readBottomInfiltration(" + FileName + ").");


		//Open file and create reader
		File file = new File(FileName);
        BufferedReader reader = null;

        //Tokenizer
        String delims = "[\\t\\s]+";

        //IO into exceptions catch
        try {
            reader = new BufferedReader(new FileReader(file));
            String text = null;
            String[] tokens;

            if ((text = reader.readLine()) != null) {
                tokens = text.split(delims);
                ProjectBottomBoundaryCondition.BottomBoundaryCondName = tokens[0]; //Dirichlet von_Newman FreeD qGWLF SeepF
            }
            
            delims = "[ =\\t]+";
            
            if ((text = reader.readLine()) != null) {
                tokens = text.split(delims);
                ProjectBottomBoundaryCondition.GWL0L = Double.parseDouble(tokens[2]);
            }
            if ((text = reader.readLine()) != null) {
                tokens = text.split(delims);
                ProjectBottomBoundaryCondition.Aqh = Double.parseDouble(tokens[2]);
            }
            if ((text = reader.readLine()) != null) {
                tokens = text.split(delims);
                ProjectBottomBoundaryCondition.Bqh = Double.parseDouble(tokens[2]);
            }
            if ((text = reader.readLine()) != null) {
                tokens = text.split(delims);
                ProjectBottomBoundaryCondition.hSeep = Double.parseDouble(tokens[2]);
            }
            if ((text = reader.readLine()) != null) {
                tokens = text.split(delims);
                ProjectBottomBoundaryCondition.SuctionBottom = Double.parseDouble(tokens[2]);
            }
            if ((text = reader.readLine()) != null) {
                tokens = text.split(delims);
                ProjectBottomBoundaryCondition.FluxBottom = Double.parseDouble(tokens[2]);
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


	private void VariablesExtrapolation(int i, int j, double dtOld, double dt) {
		
		//Log
		logger.trace("Call to VariablesExtrapolation(" + i + "," + j + "," + dt + ").");
		
		//Call the solver passing the columns parameters
		richardsSolver.timeForward(dt, dtOld, ProjectBasin.BasinSoilColumns[i][j].ColumnStaticProperties,
				ProjectBasin.BasinSoilColumns[i][j].ColumnDynamicVariables);

		
	}

}
