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


public class SoilsCatalog {
	
	static private TreeMap<String, SoilType> BasinSoilTypes;
	static private int numberOfTypes;
	private String inputFileName;
	
    //Logger
    private static Logger logger;

	
	public SoilsCatalog() {

        //Create logger and log file
        logger = Logger.getLogger("RiDI.SoilsCatalog");

	}
	
	public void ReadInputFile(String FileName) {
		
		//Log
		logger.info("Call to ReadInputFile(" + FileName + ").");

		//Save the filename
		inputFileName = FileName;
		
		//Open file and create reader
		File file = new File(FileName);
        BufferedReader reader = null;

        //Tokenizer
        String delims = ",";
        String[] tokens = null;
        
        //Variables
        double auxDouble = 0;
        int i = 0;
        int j = 0;
        

        //IO into exceptions catch
        try {
            reader = new BufferedReader(new FileReader(file));
            String text = null;
            SoilType auxSoilType;
            
            //Read header
            text = reader.readLine();
            String[] SoilTypeNames = text.split(delims);
            int size = SoilTypeNames.length;
            
            //Number of soil types
            numberOfTypes = size - 2;
            
            //Create collection
            BasinSoilTypes = new TreeMap<String, SoilType>();
            
            //Loop to create the SoilType name collection
            for (i = 0; i < numberOfTypes; i++) {
            	auxSoilType = new SoilType();
            	auxSoilType.TypeName = SoilTypeNames[2 + i];
            	BasinSoilTypes.put(SoilTypeNames[2 + i], auxSoilType);
            }

            //Loop for all the soil properties
            String[] PropertyName = {"Ks", "t", "qr", "qs", "a", "n", "w", "hm1", "hm2", "s1", "s2", "c", "c_root", "dphi", "gsat", "gw", "Gs", "e", "phi0", "SRC"};

            for (i = 0; i < 20; i++) {
                text = reader.readLine();
                tokens = text.split(delims);
                
                //Check data columns number
                if (tokens.length < (numberOfTypes+1)) {
        			logger.fatal("The number of columns in row" + PropertyName[i] + " in file " + FileName + " does not match number of soil types " + numberOfTypes);
                    System.out.println("ERROR: Error reading: " + FileName);
                    System.out.println("ERROR: The number of columns in row" + PropertyName[i] + " in file " + FileName + " does not match number of soil types " + numberOfTypes);
                    System.exit(1);                	
                }
                
                //Loop for all soil types
				for (j = 0; j < numberOfTypes; j++) {
					auxSoilType = BasinSoilTypes.get(SoilTypeNames[2 + j]);
					Field field = (auxSoilType.getClass())
							.getField(PropertyName[i]);
					
					
						//SRC
					if(i == 19) {
						field.set(auxSoilType, tokens[2 + j]);
						auxSoilType.CreateSoilRetentionCurveModel();

						//ks (mm/hr -> m/s)
					} else if (i == 0) {
						auxDouble = Double.parseDouble(tokens[2 + j]);
						auxDouble = auxDouble / 1000.0 / 3600.0;
						field.set(auxSoilType, auxDouble);
						
						//alpha (1/mm ->
                    } else if (i == 4) {
    					auxDouble = Double.parseDouble(tokens[2 + j]);                    	
                    	auxDouble = auxDouble * 1000.0;
                    	field.set(auxSoilType, auxDouble);
                    	
                    	//hm1	
                    } else if (i == 7) {
    					auxDouble = Double.parseDouble(tokens[2 + j]);                    	
                    	auxDouble = auxDouble / 1000.0;
                    	field.set(auxSoilType, auxDouble);
                    	
                    	//hm2                    	
                    } else if (i == 8) {
    					auxDouble = Double.parseDouble(tokens[2 + j]);                    	
                    	auxDouble = auxDouble / 1000.0;
                    	field.set(auxSoilType, auxDouble); 

                    	//gw                    	
                    } else if (i == 15) {
    					auxDouble = Double.parseDouble(tokens[2 + j]);                    	
                    	auxDouble = auxDouble *1000;
                    	field.set(auxSoilType, auxDouble); 
                    	
                    } else {					
						auxDouble = Double.parseDouble(tokens[2 + j]);						
						field.set(auxSoilType, auxDouble);
					}
						
				}

			}
                  
        //Catch exceptions    
        } catch (FileNotFoundException ex) {
            logger.error("Exception!",ex); //.printStackTrace();
        } catch (IOException ex) {
            logger.error("Exception!",ex);
        } catch (final NoSuchFieldException ex) {
            logger.error("Exception!",ex);
        } catch (final IllegalAccessException ex) {
            logger.error("Exception!",ex);
		} catch (NumberFormatException ex) {
			logger.fatal("Exception!",ex);
            System.out.println("ERROR: Error reading: " + FileName);
            System.out.println("ERROR: Impossible to convert " + tokens[2 + j] + "to double");
            System.exit(1);

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
	
	public SoilType getSoilType(String nodeSoilTypeName) {
		
		//Log
		logger.debug("Call to getSoilType().");

		return (SoilType) BasinSoilTypes.get(nodeSoilTypeName);
		
	}
	
	
	public void ComputePropertiesTable(double MinSucTab, double MaxSucTab, int NumberRowsTab) {

		//Log
		logger.info("Call to ComputePropertiesTable" + MinSucTab + MaxSucTab + NumberRowsTab);

		
        //Loop for all soil types
		for (SoilType auxSoilType : BasinSoilTypes.values()) {
			auxSoilType.ComputePropertiesTable(MinSucTab, MaxSucTab, NumberRowsTab);
		}
	
	}

	public String getInputFileName() {
		
		//Log
		logger.trace("Call to getInputFileName().");		

		return inputFileName;
	}

	
}
