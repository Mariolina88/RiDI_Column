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
import java.util.*;
import java.lang.reflect.Field;

import org.apache.log4j.Logger;


public class UnitsCatalog {
	
	
	static private TreeMap<Integer, UnitType> BasinUnitTypes;
	static private int numberOfTypes;
	private String inputFileName;
	
    //Logger
    private static Logger logger;

		
	public UnitsCatalog() {

        //Create logger and log file
        logger = Logger.getLogger("RiDI.UnitsCatalog");

	}
	
	public void ReadInputFile(String FileName, SoilsCatalog BasinSoilsCatalog) {

		//Log
		logger.info("Call to ReadInputFile(" + "," + FileName + ").");
		
		//Save the filename
		inputFileName = FileName;
		
		//Open file and create reader
		File file = new File(FileName);
        BufferedReader reader = null;

        //Tokenizer
        String delims = ",";
        String[] tokens = null;

        //IO into exceptions catch
        try {
            reader = new BufferedReader(new FileReader(file));
            String text = null;
            UnitType auxUnitType;
            SoilType auxSoilType;
            
            //Read header
            text = reader.readLine();
            
            //Create collection
            BasinUnitTypes = new TreeMap<Integer, UnitType>();

            //While loop for all the file rows
            while ((text = reader.readLine()) != null) {
            	
            	//Split the line using the tokenizer
            	tokens = text.split(delims);
            	numberOfTypes = (tokens.length - 1) / 2;

            	//Create the UnitType structure
            	auxUnitType = new UnitType(numberOfTypes);
            	
            	//Soil layer
            	int j = 0;
            	
                //Loop to fill the UnitType fields
                for (int i = 1; j < numberOfTypes; i = i + 2, j++) {

                	
                	//Check if the soil type exists in the catalog 
                	auxSoilType = BasinSoilsCatalog.getSoilType(tokens[i]);

                    if (auxSoilType == null ) {
                    	
                    	logger.fatal("There is a soil type: " + tokens[0]  + ", in stratigraphy file " + FileName + ", non existing in SoilTypesCatalog " + BasinSoilsCatalog.getInputFileName());
                        System.err.println("ERROR: Error reading: " + FileName);
                        System.out.println("ERROR: There is a soil type: " + tokens[0]  + ", in stratigraphy file " + FileName + ", non existing in SoilTypesCatalog " + BasinSoilsCatalog.getInputFileName());
                        System.exit(1); 
                        
                    } else {
                    	auxUnitType.SoilType[j] = tokens[i];
                    	auxUnitType.Z_fin[j] = Double.parseDouble(tokens[i+1]);                    	
                    }

                }

                //Compute soil depth
                auxUnitType.computeSoilDepth();
                
                //Store SoilUnit in the catalog
                BasinUnitTypes.put(Integer.parseInt(tokens[0]), auxUnitType);
            	            	           
            }
            
        	System.out.println("Soil types catalog ridden: " + FileName);

                        
        //Catch exceptions    
        } catch (FileNotFoundException ex) {
            logger.error("Exception!",ex); //.printStackTrace();
        } catch (IOException ex) {
        	logger.error("Exception!",ex);
        } catch (NumberFormatException ex) {
        	System.out.println("ERROR: Impossible to parse to int the SoilUnit number, check: " + FileName);
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
	
	//Defines node Z coordinate, depends on the unit type total depth
	static public double getNodeZ(int columnUnit, int numNodes, int i) {
		
		//Log
		logger.trace("Call to getNodeZ(" + columnUnit + "," + numNodes + "," + i + ").");
		
		double auxZ = (double)(i) / ((double)(numNodes - 1)) * (BasinUnitTypes.get(columnUnit).SoilDepth);
		return auxZ;

	}
	
	public double getUnitDepth(int columnUnit) {
		
		return BasinUnitTypes.get(columnUnit).SoilDepth;
		
	}
	
	static public String getNodeSoilName(int columnUnit, int numNodes, int i) {

		//Log
		logger.trace("Call to getNodeZ(" + columnUnit + "," + numNodes + "," + i + ").");
		
		//To find the soil layer of a node is necessary to find the Z of the node
		double Z = getNodeZ(columnUnit, numNodes, i);
		return BasinUnitTypes.get(columnUnit).getSoilTypeByZ(Z);
		
	}

	public String getInputFileName() {

		//Log
		logger.trace("Call to getInputFileName().");		
		
		return inputFileName;
	}
	

}
