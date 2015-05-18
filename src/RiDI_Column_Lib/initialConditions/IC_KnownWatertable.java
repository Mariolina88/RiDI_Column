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
import RiDI_Column_Lib.UnitType;
import RiDI_Column_Lib.interfaces.InterfaceInitialConditions;

import org.math.array.*;

import org.apache.log4j.Logger;


public class IC_KnownWatertable implements InterfaceInitialConditions {

	//Logger
    private static Logger logger;

	//Pointer to the basin object
	Basin ProjectBasin;    
	
	//Variables
	double rainfallIntensity;
    double Depth[]=null;
    double Suction[]=null;
    double WaterContent[]=null;
    boolean suctionData;
	
    
	public IC_KnownWatertable(Basin AuxProjectBasin) {
		
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
		double IC_suctionZ[] = new double [cellSoilColumn.ColumnStaticProperties.nodeZ.length];
        double IC_watercontentZ[] = new double [cellSoilColumn.ColumnStaticProperties.nodeZ.length];
		
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
        	
 
        	
        	//node depth
        	double Z = cellSoilColumn.ColumnStaticProperties.nodeZ[k];
        	//column depth
        	double maxZ = cellSoilColumn.ColumnStaticProperties.columnDepth;
        	//depth relate to % Depth IC
        	double IC_Z[] = LinearAlgebra.times(LinearAlgebra.divide(LinearAlgebra.minus(100, Depth),100), maxZ);
        	
        	int IC_Z_length = IC_Z.length;
        	
        	if(suctionData) {

        		if (Z < IC_Z[IC_Z.length-1]){
        			IC_suctionZ[k] = IC_Z[IC_Z_length-1]-Z; //IDROSTATICO in m
        			IC_watercontentZ[k] = cellSoilColumn.ColumnStaticProperties.nodeSoilType[k].qs; //IDROSTATICO
        		} else {
        			for (int u = 0; u < IC_Z.length; u++){
        				if (Z <= IC_Z[u] && Z >= IC_Z[u+1]){
        					IC_suctionZ[k] = -( ( (Suction[u]-Suction[u+1]) * (Z-IC_Z[u+1])/(IC_Z[u]-IC_Z[u+1]) ) + Suction[u+1] );
        					IC_watercontentZ[k] = cellSoilColumn.ColumnStaticProperties.nodeSoilType[k].SRCmodel.Saturation(IC_suctionZ[k], cellSoilColumn.ColumnStaticProperties.nodeSoilType[k]);  	    		
        					break;
        				}	
        			}
        		}
        	
        	} else {
        	
        		if (Z < IC_Z[IC_Z.length-1]){
        			IC_suctionZ[k] = IC_Z[IC_Z_length-1]-Z; //IDROSTATICO in m
        			IC_watercontentZ[k] = cellSoilColumn.ColumnStaticProperties.nodeSoilType[k].qs; //IDROSTATICO
        		} else {
        			for (int u = 0; u < IC_Z.length; u++){
        				if (Z<= IC_Z[u] && Z>= IC_Z[u+1]){
        					IC_watercontentZ[k] = ((WaterContent[u]-WaterContent[u+1]) * (Z-IC_Z[u+1])/(IC_Z[u]-IC_Z[u+1])) + WaterContent[u+1];
        					IC_suctionZ[k] = cellSoilColumn.ColumnStaticProperties.nodeSoilType[k].SRCmodel.Suction((IC_watercontentZ[k] - cellSoilColumn.ColumnStaticProperties.nodeSoilType[k].qr)/(cellSoilColumn.ColumnStaticProperties.nodeSoilType[k].qs - cellSoilColumn.ColumnStaticProperties.nodeSoilType[k].qr), cellSoilColumn.ColumnStaticProperties.nodeSoilType[k]);  	    		
        					break;
        				}	
        			}
        		}
        	
        	}
        	
        	//Set the suction, saturation and moisture values for the whole column
        	cellSoilColumn.ColumnDynamicVariables.SuctionOld[k] = IC_suctionZ[k];
        	cellSoilColumn.ColumnDynamicVariables.ThetaOld[k] = IC_watercontentZ[k];
        	cellSoilColumn.ColumnDynamicVariables.Saturation[k] = (IC_watercontentZ[k] - cellSoilColumn.ColumnStaticProperties.nodeSoilType[k].qr)/(cellSoilColumn.ColumnStaticProperties.nodeSoilType[k].qs - cellSoilColumn.ColumnStaticProperties.nodeSoilType[k].qr);
        	
        	
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
                
                if (tokens[2].equals("Suction") || tokens[2].equals("suction")) {
                	
                	suctionData = true;
                              	
                	int i = 0;
                
                    //While loop for all the file rows
                    while ((text = reader.readLine()) != null) {
                    	
                    	
                    	//Split the line using the tokenizer
                    	tokens = text.split(delims);
                    	
                    	double[] temp = new double[i + 1];
                    	double[] temp2 = new double[i + 1];
                    	
                    	if (Suction!= null)
                    	    System.arraycopy(Suction, 0, temp, 0, Math.min(Suction.length, temp.length));
                    	Suction = temp;
                    	Suction[i] = Double.parseDouble(tokens[1]);
                    	
                    	if (Depth!= null)
                    	    System.arraycopy(Depth, 0, temp2, 0, Math.min(Depth.length, temp2.length));
                    	Depth = temp2;
                    	Depth[i] = Double.parseDouble(tokens[0]);
                    	
                    	i++;
                    	
                    }	
                }
                else
                {
                	
                	suctionData = false; 
                	
                	int i = 0;
                    
                    //While loop for all the file rows
                    while ((text = reader.readLine()) != null) {
                    	
                    	
                    	//Split the line using the tokenizer
                    	tokens = text.split(delims);
                    	
                    	double[] temp = new double[i + 1];
                    	double[] temp2 = new double[i + 1];
                    	
                    	if (WaterContent!= null)
                    	    System.arraycopy(WaterContent, 0, temp, 0, Math.min(WaterContent.length, temp.length));
                    	WaterContent = temp;
                    	WaterContent[i] = Double.parseDouble(tokens[1]);
                    	
                    	if (Depth!= null)
                    	    System.arraycopy(Depth, 0, temp2, 0, Math.min(Depth.length, temp2.length));
                    	Depth = temp2;
                    	Depth[i] = Double.parseDouble(tokens[0]);
                    	
                    	i++;
                    	
                    }	
                }

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
            } catch (Exception ex) {
                logger.error("Exception!",ex);            	
            }
        }

		
	}
	
	

}
