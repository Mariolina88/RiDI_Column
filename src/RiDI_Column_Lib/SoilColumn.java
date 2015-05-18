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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.math.array.DoubleArray;

import org.apache.log4j.Logger;

public class SoilColumn {

	//Properties and variables
	public SoilColumnDynamicVariables ColumnDynamicVariables;
	public SoilColumnStaticProperties ColumnStaticProperties;
	
	//Logger
    private static Logger logger;
    
    //Physical parameters
    public double gParameter = 9.81;
    public double DensAgua = 1000.0;
    private double Accumulated[];
    private double wCumT = 0.0;
    private double wCumA = 0.0;

	
	public SoilColumn(int UnitType) {

		//Initialize
		ColumnDynamicVariables = new SoilColumnDynamicVariables();
		ColumnStaticProperties = new SoilColumnStaticProperties(UnitType);
		
        //Create logger and log file
        logger = Logger.getLogger("RiDI.SoilColumn");

        //Initialize variables
        Accumulated = DoubleArray.fill(12, 0.0);
		
	}
	
	public void CreateColumnNodes(int numNodes, UnitsCatalog BasinUnitsCatalog, SoilsCatalog BasinSoilsCatalog) {
		
		//Log
		logger.info("Call to CreateColumnNodes(" + numNodes + UnitsCatalog.class + SoilsCatalog.class + ").");		
		
		//Create column nodes variables and properties
		ColumnDynamicVariables.CreateColumnNodes(numNodes);
		ColumnStaticProperties.CreateColumnNodes(numNodes, BasinUnitsCatalog, BasinSoilsCatalog);
		
	}
	
	public double ComputeWaterTable() {

		//Log
		logger.trace("Call to ComputeWaterTable().");

		//Initial guess: no watertable
		double auxDepht = ColumnStaticProperties.nodeZ[ColumnDynamicVariables.SuctionOld.length - 1];
		ColumnDynamicVariables.WaterTable = auxDepht;
		
		//Loop for the whole column, from bottom to top
		for (int i = 1; i < ColumnDynamicVariables.SuctionOld.length; i++) {
			
			//Search where the suction sign changes
			if(ColumnDynamicVariables.SuctionOld[i] * ColumnDynamicVariables.SuctionOld[i-1] < 0.0) {
				
				//Linear interpolation of the 0 suction depth
				ColumnDynamicVariables.WaterTable = auxDepht - ColumnStaticProperties.nodeZ[i-1] + (0.0 - ColumnDynamicVariables.SuctionOld[i-1]) * (ColumnStaticProperties.nodeZ[i] - ColumnStaticProperties.nodeZ[i-1]) / (ColumnDynamicVariables.SuctionOld[i] - ColumnDynamicVariables.SuctionOld[i-1]);
				break;
			}
			
		}
		
		return ColumnDynamicVariables.WaterTable;

	}
	
	public void PrintVerticalResults(String time, double dt) {

		//Log
		logger.info("Call to PrintVerticalResults(" + time + "," + dt + ").");

		File ResOutputFile = null;
		File ResOutputFileMatlab = null;

		//Catch file creation exception
		try {

			//The file path
			ResOutputFile = new File("ColumnRes_" + time + ".txt");
			ResOutputFileMatlab = new File("ColumnRes_" + time + "_Matlab.txt");
			FileOutputStream is = new FileOutputStream(ResOutputFile);	        
			OutputStreamWriter osw = new OutputStreamWriter(is);    
			Writer w = new BufferedWriter(osw);

			FileOutputStream isMatlab = new FileOutputStream(ResOutputFileMatlab);	        
			OutputStreamWriter oswMatlab = new OutputStreamWriter(isMatlab);    
			Writer wMatlab = new BufferedWriter(oswMatlab);
			
			
			//Write header
			w.write("Node   Depth        Head   Moisture       K                 C               Flux         v/KsTop       FOS_Sat     FOS_Unat     SucStress    Saturation\r\n");
			w.write("             [m]            [m]         [-]         [m/s]            [1/m]             [m/s]             [-]             [-]               [-]               [N/m2]               [-]\r\n");


			//Loop over column values
			int N = ColumnDynamicVariables.SuctionNew.length;
			double vi = 0.0;
			double dz = 0.0;

			for(int i = (N - 1); i >= 0; i--) {

				if(i == 0) {
					dz = ColumnStaticProperties.nodeZ[1] - ColumnStaticProperties.nodeZ[0];	
					vi = - (ColumnDynamicVariables.HydConductivity[0] + ColumnDynamicVariables.HydConductivity[1]) / 2.0 * 
							( (ColumnDynamicVariables.SuctionNew[1] - ColumnDynamicVariables.SuctionNew[0]) / dz + 1.0);

				} else if(i == (N - 1)) {

					int N1 = N - 2;
					dz = ColumnStaticProperties.nodeZ[(N - 1)] - ColumnStaticProperties.nodeZ[N1];


					vi = - (ColumnDynamicVariables.HydConductivity[N - 1] + ColumnDynamicVariables.HydConductivity[N1]) / 2.0 * 
							( (ColumnDynamicVariables.SuctionNew[N - 1] - ColumnDynamicVariables.SuctionNew[N1]) / dz + 1.0) -
							(ColumnDynamicVariables.ThetaNew[N-1] -ColumnDynamicVariables.ThetaOld[N-1]) * (dz/2.0) / dt;

				} else {

					double dzA = ColumnStaticProperties.nodeZ[i+1] - ColumnStaticProperties.nodeZ[i];
					double dzB = ColumnStaticProperties.nodeZ[i] - ColumnStaticProperties.nodeZ[i-1];

					double vA = - (ColumnDynamicVariables.HydConductivity[i] + ColumnDynamicVariables.HydConductivity[i+1]) / 2.0 *
							( (ColumnDynamicVariables.SuctionNew[i+1] - ColumnDynamicVariables.SuctionNew[i]) / dzA + 1.0);

					double vB = - (ColumnDynamicVariables.HydConductivity[i] + ColumnDynamicVariables.HydConductivity[i-1]) / 2.0 *
							( (ColumnDynamicVariables.SuctionNew[i] - ColumnDynamicVariables.SuctionNew[i-1]) / dzB + 1.0);

					vi = (vA * dzA + vB * dzB) / (dzA + dzB);

				}
				

				w.write(String.format("%4d %10.4f %11.3f %9.4f %12.4e %12.4e %12.4e %12.4e %13.4f %13.4f   %11.3f   %11.3f%n", N-i,ColumnStaticProperties.nodeZ[i]-ColumnStaticProperties.columnDepth,
						ColumnDynamicVariables.SuctionNew[i],ColumnDynamicVariables.ThetaNew[i],ColumnDynamicVariables.HydConductivity[i],ColumnDynamicVariables.Capacity[i],
						vi,vi / ColumnStaticProperties.nodeSoilType[N-1].Ks,ColumnDynamicVariables.FOS_sat[i],ColumnDynamicVariables.FOS_unsat[i],ColumnDynamicVariables.SuctionStress[i],ColumnDynamicVariables.Saturation[i]));

				wMatlab.write(String.format("%4d %10.4f %11.3f %9.4f %12.4e %12.4e %12.4e %12.4e %13.4f %13.4f %11.3f %11.3f%n", N-i,ColumnStaticProperties.nodeZ[i]-ColumnStaticProperties.columnDepth,
						ColumnDynamicVariables.SuctionNew[i],ColumnDynamicVariables.ThetaNew[i],ColumnDynamicVariables.HydConductivity[i],ColumnDynamicVariables.Capacity[i],
						vi,vi / ColumnStaticProperties.nodeSoilType[N-1].Ks,ColumnDynamicVariables.FOS_sat[i],ColumnDynamicVariables.FOS_unsat[i],ColumnDynamicVariables.SuctionStress[i],ColumnDynamicVariables.Saturation[i]));
				
			}


			//Close handle
			w.close();
			wMatlab.close();
			

		} catch (FileNotFoundException ex) {
			logger.error("Exception!", ex);
			System.err.println("ERROR: Impossible to open file: " + ResOutputFile);
		} catch (IOException ex) {
			logger.error("Exception!", ex);
			System.err.println("ERROR: Impossible to write to file: " + ResOutputFile);
		}

	}
	
	
	public void PrintVerticalProfile(String nodeNumber) {
	

		//Log
		logger.info("Call to PrintVerticalProfile(" + nodeNumber + ").");

		File ResOutputFile = null;

		//Catch file creation exception
		try {

			//The file path
			ResOutputFile = new File("ColumnProf_" + nodeNumber + ".txt");
			FileOutputStream is = new FileOutputStream(ResOutputFile);	        
			OutputStreamWriter osw = new OutputStreamWriter(is);    
			Writer w = new BufferedWriter(osw);

			//Write header
			w.write("    n      depth     THr       THs       hs       Ks           Ks/KsTop\r\n");
			w.write("             [L]         [-]          [-]        [L]      [L/T]             [-]\r\n");

			//Loop over column values
			int N = this.ColumnStaticProperties.nodeZ.length;

			for(int i = (N - 1); i >= 0; i--) {

				w.write(String.format("%5d%10.2f%10.3f%10.3f%10.1f%12.3e%10.3f%n",N-i,ColumnStaticProperties.nodeZ[i]-ColumnStaticProperties.columnDepth,
						ColumnStaticProperties.nodeSoilType[i].qr,ColumnStaticProperties.nodeSoilType[i].qs,ColumnStaticProperties.nodeSoilType[i].SaturatedSuction,
						ColumnStaticProperties.nodeSoilType[i].Ks,ColumnStaticProperties.nodeSoilType[i].Ks/ColumnStaticProperties.nodeSoilType[N-1].Ks));
			}

			//Close handle
			w.close();


		} catch (FileNotFoundException ex) {
			logger.error("Exception!", ex);
			System.err.println("ERROR: Impossible to open file: " + ResOutputFile);
		} catch (IOException ex) {
			logger.error("Exception!", ex);
			System.err.println("ERROR: Impossible to write to file: " + ResOutputFile);
		}
	}
	
	
	public void PrintBalance(double t, String nodeNumber, double dt, int iter) {
		

		//Log
		logger.info("Call to PrintBalance(" + nodeNumber + ").");

		File ResOutputFile = null;

		//Catch file creation exception
		try {

			//The file path
			ResOutputFile = new File("Column_balance_" + nodeNumber + ".txt");
			FileOutputStream is = new FileOutputStream(ResOutputFile);	        
			OutputStreamWriter osw = new OutputStreamWriter(is);    
			Writer w = new BufferedWriter(osw);

			//Loop over column values
			int N = ColumnDynamicVariables.SuctionNew.length;

			//Maximum ponding, consider reservoir h > 100 m
			double MaxPondingDepth = 1000.0;



			int M = N-1;
			double dzN = ColumnStaticProperties.nodeZ[N-1] - ColumnStaticProperties.nodeZ[M-1];
			double vT = -(ColumnDynamicVariables.HydConductivity[N-1] + ColumnDynamicVariables.HydConductivity[M-1]) /2.0 *
					((ColumnDynamicVariables.SuctionNew[N-1] - ColumnDynamicVariables.SuctionNew[M-1]) / dzN + 1.0) - (ColumnDynamicVariables.ThetaNew[N-1] - ColumnDynamicVariables.ThetaOld[N-1]) * dzN/2.0 / dt;

			double dz1 = ColumnStaticProperties.nodeZ[1] - ColumnStaticProperties.nodeZ[0];
			double vB = -(ColumnDynamicVariables.HydConductivity[0] + ColumnDynamicVariables.HydConductivity[1]) /2.0 *((ColumnDynamicVariables.SuctionNew[1] - ColumnDynamicVariables.SuctionNew[0]) / dz1 + 1.0) + (ColumnDynamicVariables.ThetaNew[0]-ColumnDynamicVariables.ThetaOld[0])* dz1/2.0/dt;

			double vTopW = vT;

			double vTopV = vT-vTopW;
			double vTop=vT;
			double vBot=vB;
			double vRunOff=0.0;


			if(ColumnDynamicVariables.SuctionNew[N - 1] > MaxPondingDepth) {
				vRunOff = (ColumnDynamicVariables.SuctionNew[N - 1] - MaxPondingDepth) / dt;
			}

			if(vRunOff < 1.0e-5) vRunOff = 0.0;

			double rInfil = 0.0;
			double rEvap = 0.0;

			if(vTop < 0.0 && (ColumnDynamicVariables.topBoundaryCondition < 0.0 || ColumnDynamicVariables.SuctionNew[0] > 0.0)) rInfil = -vTop;
			if(vTop >= 0.0 && ColumnDynamicVariables.topBoundaryCondition < 0.0) rInfil = - ColumnDynamicVariables.topBoundaryCondition;
			if(vTop > 0.0)               rEvap = vTop - ColumnDynamicVariables.topBoundaryCondition;
			if(vTop < 0.0 && !ColumnDynamicVariables.topBoundCondInfiltration) rInfil = -vTop;
			Accumulated[1] = Accumulated[1] + ColumnDynamicVariables.topBoundaryCondition *dt;
			Accumulated[3] = Accumulated[3] + vTop *dt;
			Accumulated[5] = Accumulated[5] + vBot *dt;
			Accumulated[6] = Accumulated[6] + vRunOff*dt;
			Accumulated[7] = Accumulated[7] + rInfil*dt;
			Accumulated[9] = Accumulated[9] - ColumnDynamicVariables.topBoundaryCondition * dt;
			wCumT = wCumT + (vBot-vTop)*dt;
			wCumA = wCumA + (Math.abs(vBot) + Math.abs(vTop)) * dt;


			//Compute water volume
			double Volume=0.0;

			for(int i = (N - 2); i >= 0; i--) {
				int j = i + 1;
				double dz = ColumnStaticProperties.nodeZ[j] - ColumnStaticProperties.nodeZ[i];
				double VNewi = dz * (ColumnDynamicVariables.ThetaNew[i] + ColumnDynamicVariables.ThetaNew[j]) / 2.0;
				Volume = Volume + VNewi;
			}

			//Write balance
			w.write("         Time   iter         rTop         vTop         vBot    sum(rTop)    sum(vTop)    sum(vBot)         hTop         hBot       RunOff      sum(RunOff)       Volume       sum(Infil)  Cum(WTrans)\r\n");
			w.write("         [T]     []         [L/T]        [L/T]        [L/T]       [L]          [L]          [L]            [L]          [L]        [L/T]           [L]             [L]            [L]\r\n");
			w.write(String
					.format("%13.4f%7d%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%n%n",
							t, iter,
							ColumnDynamicVariables.topBoundaryCondition, vTop,
							vBot, Accumulated[1], Accumulated[3], Accumulated[5],
							ColumnDynamicVariables.SuctionNew[N - 1],
							ColumnDynamicVariables.SuctionNew[0], vRunOff,
							Accumulated[6], Volume, Accumulated[7], Accumulated[11]));

			w.write("          Time     PrecipP     FluxTopP     FluxTopA      FluxBot       RunOff     Sum(PrecP)  Sum(FlTopP)  Sum(FlTopA)  Sum(FluxBot)  Sum(RunOff)    Storage      vTopW        vTopV\r\n");
			w.write(String
					.format("%14.4f%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%13.5e%n%n",
							t, -ColumnDynamicVariables.topBoundaryCondition,
							ColumnDynamicVariables.topBoundaryCondition, vTop,
							vBot, vRunOff, Accumulated[9], Accumulated[1], Accumulated[3], Accumulated[5],
							Accumulated[6], Volume, vTopW, vTopV));

			w.write("      Time          dt    ItCum  Top (von Newman)  Bot (von Newman)\r\n");
			w.write(String.format("%15.7e%13.5e%5d   %b   %b%n", t, dt,
					iter, ColumnDynamicVariables.topBoundCondInfiltration,
					ColumnDynamicVariables.botBoundCondInfiltration));


			//Close handle
			w.close();


		} catch (FileNotFoundException ex) {
			logger.error("Exception!", ex);
			System.err.println("ERROR: Impossible to open file: " + ResOutputFile);
		} catch (IOException ex) {
			logger.error("Exception!", ex);
			System.err.println("ERROR: Impossible to write to file: " + ResOutputFile);
		}
	}

	
}
