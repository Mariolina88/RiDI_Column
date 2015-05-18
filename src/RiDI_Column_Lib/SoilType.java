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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import RiDI_Column_Lib.interfaces.InterfaceSoilRetentionCurve;
import RiDI_Column_Lib.interfaces.classFactory;



public class SoilType {
	
    //Logger
    private static Logger logger;
    
    //Hydraulic model
    public InterfaceSoilRetentionCurve SRCmodel;
    public String TypeName;
    public String SRC;
    

    //Soil properties
	public double Ks;
	public double t;
	public double qr;
	public double qs;
	public double a;
	public double n;
	public double w;
	public double hm1;
	public double hm2;
	public double s1;
	public double s2;
	public double c;
	public double c_root;
	public double gw;
	public double Gs;
	public double e;
	public double phi0;
	public double dphi;
	public double gsat;
	public double gwat;
	
	//Computed values
	public double SaturatedSuction;
	
	//Table used for hydraulic properties fast interpolation during computations
	private InterpolationTable soilPropertiesTable;
	
	//Table arrays class
	private class InterpolationTable {

		//Columns
		public double suctionValues[];
		public double hydCondValues[];
		public double capacityValues[];
		public double waterContentValues[];
		public double saturationValues[];
		
		//Parameters
		public double  MinSuc;
		public double MaxSuc;
		public int NumberRows;
		public double incSuction;

		//Constructor, define arrays size
		public InterpolationTable(int NumberRows) {
			
			suctionValues = new double[NumberRows];
			hydCondValues = new double[NumberRows];
			capacityValues = new double[NumberRows];
			waterContentValues = new double[NumberRows];
			saturationValues = new double[NumberRows];
			
		}
		
		
	}
	
	public SoilType() {
		
        //Create logger and log file
        logger = Logger.getLogger("RiDI.SoilType");

	}
	
	public void ComputePropertiesTable(double MinSucTab, double MaxSucTab, int NumberRowsTab) {

		//Log
		logger.info("Call to " + TypeName + ".ComputePropertiesTable(" + MinSucTab + "," + MaxSucTab + "," + NumberRowsTab + ").");

		//Screen
		System.out.println("Computing interpolation tables for soil hydraulic properties.");
				

		//Redim table
		soilPropertiesTable = new InterpolationTable(NumberRowsTab);

		//Compute suction values for interpolation
		double dlh = (Math.log10(-MaxSucTab)-Math.log10(-MinSucTab)) / (NumberRowsTab-1);

		for (int i=0; i< NumberRowsTab; i++) {
			double alh = Math.log10(-MinSucTab) + i* dlh;
			soilPropertiesTable.suctionValues[i] =- Math.pow(10, alh);
		}
		
		SaturatedSuction  = SRCmodel.Suction(1.0,this);

		//Loop for all the rows (suction values) in the table
		for (int i=0; i< NumberRowsTab; i++) {
			soilPropertiesTable.hydCondValues[i]      = SRCmodel.HydraulicConductivity(soilPropertiesTable.suctionValues[i],this);
			soilPropertiesTable.capacityValues[i]     = SRCmodel.WaterCapacity(soilPropertiesTable.suctionValues[i],this);
			soilPropertiesTable.waterContentValues[i] =SRCmodel.WaterContent(soilPropertiesTable.suctionValues[i],this);
			soilPropertiesTable.saturationValues[i]   = SRCmodel.Saturation(soilPropertiesTable.suctionValues[i],this);        		            
		}
		
		//Store table parameters
		soilPropertiesTable.MinSuc = MinSucTab;
		soilPropertiesTable.MaxSuc = MaxSucTab;
		soilPropertiesTable.NumberRows = NumberRowsTab;
		soilPropertiesTable.incSuction = dlh;

        //Export computed table
        ExportTable();
        
		
	}
	
	
	private void ExportTable() {

		//Log
		logger.info("Call to ExportTable().");
		
		File TableOutputFile = null;
		File TableOutputFileMatlab = null;
		
		//Catch file creation exception
		    try {

		    	//The file path
		        TableOutputFile = new File("Soil_" + TypeName + "_PropertiesTable.txt");
		        TableOutputFileMatlab = new File("Soil_" + TypeName + "_PropertiesTable_Matlab.txt");
		        
		        FileOutputStream is = new FileOutputStream(TableOutputFile);
		        OutputStreamWriter osw = new OutputStreamWriter(is);    
		        Writer w = new BufferedWriter(osw);
		        
				FileOutputStream isMatlab = new FileOutputStream(TableOutputFileMatlab);	        
				OutputStreamWriter oswMatlab = new OutputStreamWriter(isMatlab);    
				Writer wMatlab = new BufferedWriter(oswMatlab);
		        
		        
		        //Write header
		        w.write("       Table of Hydraulic Properties which are interpolated in simulation       \r\n");
		        w.write("  theta         h                log h            C                 K               log K            S\r\n");
				w.write("     [-]         [m]                [-]              [1/m]             [m/s]              [-]              [-]\r\n");

		        //Loop over suction values
		        for(int i = 0; i < soilPropertiesTable.NumberRows; i++) {
		            double a10h = Math.log10(Math.max(-soilPropertiesTable.suctionValues[i],1.0E-30));
		            double a10K = Math.log10(soilPropertiesTable.hydCondValues[i]);
		        	w.write(String.format("%8.4f%12.3e%12.4e%12.4e%12.4e%12.4e%10.4f%n",soilPropertiesTable.waterContentValues[i],soilPropertiesTable.suctionValues[i],a10h,soilPropertiesTable.capacityValues[i],soilPropertiesTable.hydCondValues[i],a10K,soilPropertiesTable.saturationValues[i]));
		        	
		        	wMatlab.write(String.format("%14.8f%14.6e%14.6e%14.6e%14.6e%14.6e%14.6f%n",soilPropertiesTable.waterContentValues[i],soilPropertiesTable.suctionValues[i],a10h,soilPropertiesTable.capacityValues[i],soilPropertiesTable.hydCondValues[i],a10K,soilPropertiesTable.saturationValues[i]));
		        	
		        }
		        
		        w.write("end");
		        
		        //Close handle
		        w.close();
		        wMatlab.close();

		        
		    } catch (FileNotFoundException ex) {
		    	logger.error("Exception!", ex);
		        System.err.println("ERROR: Impossible to open file: " + TableOutputFile);
		    } catch (IOException ex) {
		    	logger.error("Exception!", ex);
		        System.err.println("ERROR: Impossible to write to file: " + TableOutputFile);
		    }
		    
		
	}
	
	public void CreateSoilRetentionCurveModel() {
		
		//Log
		logger.info("Call to CreateSoilRetentionCurveModel().");
		
		//Create the class factory to get the SRC model object
		classFactory SRCclassFactory = new classFactory();

		//Call the maker
		SRCmodel = (InterfaceSoilRetentionCurve) SRCclassFactory.makeFactory("RiDI_Column_Lib.soilRetentionCurveModels." + SRC);
		
	}
	
	
	public void InterpolateValuesFromTable(SoilColumnDynamicVariables ColumnDynamicVariables, int i) {

		//Log
		logger.debug("Call to InterpolateValuesFromTable(" + ColumnDynamicVariables + "," + i + ").");

		double suctionNew = ColumnDynamicVariables.SuctionNew[i];
		double suctionTemp = ColumnDynamicVariables.SuctionTemp[i];
		double HydConductivity;
		double Capacity;
		double Theta;
		double Saturation;

				
		//Filter maximum value
		double hi1 = Math.min(SaturatedSuction,suctionTemp);
		double hi2 = Math.min(SaturatedSuction, suctionNew);
		double alh1= Math.log10(-soilPropertiesTable.MinSuc);

		//Relaxed new guess value, relaxation factor 10%
		double hiM = 0.1 * hi1 + 0.9 * hi2;


		//Compute the hydraulic properties associated to new suction value, use interpolation tables or exact equation
		if(hi1 >= SaturatedSuction && hi2 >= SaturatedSuction) {
			
			HydConductivity=Ks;   
			Capacity = 0.0;
            Theta = qs;
            Saturation = 1.0;

			
		} else if (hiM > soilPropertiesTable.MaxSuc && hiM <= soilPropertiesTable.MinSuc) {

			int iT = (int) ((Math.log10(-hiM) - alh1) / soilPropertiesTable.incSuction); 
			double dh = (hiM - soilPropertiesTable.suctionValues[iT]) / (soilPropertiesTable.suctionValues[iT+1] - soilPropertiesTable.suctionValues[iT]);

			HydConductivity = soilPropertiesTable.hydCondValues[iT] + (soilPropertiesTable.hydCondValues[iT+1] - soilPropertiesTable.hydCondValues[iT]) * dh;
			Capacity = soilPropertiesTable.capacityValues[iT] + (soilPropertiesTable.capacityValues[iT+1] - soilPropertiesTable.capacityValues[iT]) * dh;
			Theta = soilPropertiesTable.waterContentValues[iT] + (soilPropertiesTable.waterContentValues[iT+1] - soilPropertiesTable.waterContentValues[iT]) * dh;
			Saturation = soilPropertiesTable.saturationValues[iT] + (soilPropertiesTable.saturationValues[iT+1] - soilPropertiesTable.saturationValues[iT]) * dh;
			
		//Use  exact function
		}else {
			
			HydConductivity = SRCmodel.HydraulicConductivity(hiM, this);
			Capacity = SRCmodel.WaterCapacity(hiM, this);
			Theta = SRCmodel.WaterContent(hiM, this);
			Saturation = SRCmodel.Saturation(hiM, this);

		}

		//Assign new values to column
		ColumnDynamicVariables.HydConductivity[i] = HydConductivity;
		ColumnDynamicVariables.Capacity[i] = Capacity;
		ColumnDynamicVariables.ThetaNew[i] = Theta;
		ColumnDynamicVariables.Saturation[i] = Saturation;

		
	}
	
	public double Suction(double saturation) {
		
		 return SRCmodel.Suction(saturation,this);

	}

}
