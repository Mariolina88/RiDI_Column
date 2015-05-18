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
import org.math.array.LinearAlgebra;

public class Richards1D {

	//Logger
	private static Logger logger;

	//Banded matrix storage
	private double SourceTerm[];
	private double Diagonal[];
	private double DiagSupInf[];
	private double SourceTermBottom;
	private double DiagonalBottom;
	private double DiagonalInfBottom;
	private double SourceTermTop;
	private double DiagonalTop;
	private double DiagonalSupBottom;
	private double MaxPondingDepth;

	//Maximum minimum pressures
	double rMax = 1.0E+10;
	double rMin = 1.0E-100;

	//Minimal water pressure in the surface
	double hCritA = -1.0E+100; //.2385 - 10.3322;



	public Richards1D(int nodesNumber, double maxPonding) {

		//Create logger and log file
		logger = Logger.getLogger("RiDI.Richards1D");

		//Dim arrays
		SourceTerm = new double[nodesNumber];
		Diagonal = new double[nodesNumber];
		DiagSupInf = new double[nodesNumber];    
		
		//Maximum ponding, consider reservoir h > 100 m
		MaxPondingDepth = maxPonding;


	}

	public double timeStep(double TolTh, double TolH, int MaxIt, double dt, double dtMin, SoilColumnStaticProperties Static, SoilColumnDynamicVariables Dynamic, double waterDepth, double precipitation, BottomBoundaryCondition ProjectBottomBoundaryCondition) {

		//Log
		logger.trace("Call to timeStep(" + dt + "," + Static + "," + Dynamic + ").");

		//Number of nodes
		int nodesNumber = Static.nodeZ.length;

		//Store initial boundary conditions (von Newman or Dirichlet) 
		boolean TopBoundaryType = Dynamic.topBoundCondInfiltration;
		boolean BotBoundaryType = Dynamic.botBoundCondInfiltration;

		//Precipitation
		Dynamic.topBoundaryCondition = - precipitation;
		
		
		//Condition to control the ponding after 2D runoff
		if (waterDepth > 0.0) {
			Dynamic.SuctionOld[nodesNumber-1] = waterDepth;
		}


		//Tolerances
//		double TolTh = 0.001;
//		double TolH = 0.001;
		
		//Maximum internal iterations before time step reducing
//		int MaxIt = 20; 
		
		//Max total iterations before stop
		int MaxTotIt = 100;


		//Counters and controls
		int totalIteration = 0;
		int partialIteration = 0;
		boolean ConvgF = true;
		boolean ItCrit = false;

		//End of ponding (works for both BC and VG)
		if(Dynamic.SuctionNew[nodesNumber-1] > 0.0 && Dynamic.SuctionNew[nodesNumber-1] < 0.00005 &&
				Dynamic.topBoundaryCondition > 0.0) {
			Dynamic.SuctionOld[nodesNumber-1] = Dynamic.SuctionNew[nodesNumber-1];
		}

		//Initial values (¿?)
//		Dynamic.SuctionNew  = DoubleArray.copy(Dynamic.SuctionOld);
//		Dynamic.SuctionTemp = DoubleArray.copy(Dynamic.SuctionOld);
		

		//Internal iterations
		while (!ItCrit) { 
			
			//Log
			logger.trace("Solver internal loop: dt= " + dt + ", iteration= " + String.format("%03d", partialIteration) + ", total iterations = " + String.format("%03d", totalIteration));


			//Generate terms of matrix equation and solve by GaussSolver elimination
			updateNodeProperties(Static, Dynamic);

			//Prepare matrix coefficients
			MatrixTerms(dt, Static, Dynamic, ProjectBottomBoundaryCondition); 

			//Internal Picard linealization
			Dynamic.SuctionTemp = DoubleArray.copy(Dynamic.SuctionNew);

			//Matrix solver (updates hNew)    
			TridiagonalGaussSolver(dt, Static, Dynamic, ProjectBottomBoundaryCondition);

			//Check top atmospheric conditions
			if(Dynamic.SuctionNew[nodesNumber-1] < hCritA) Dynamic.SuctionNew[nodesNumber-1] = hCritA;

			//Check results range
			for(int i = 0; i < nodesNumber; i++) {

				//Check pressure
				if(Math.abs(Dynamic.SuctionNew[i]) > rMax) {
					Dynamic.SuctionNew[i] = Dynamic.SuctionNew[i] / Math.abs(Dynamic.SuctionNew[i]) * rMax;
				}

				//Check almost last node
				if(Dynamic.SuctionNew[i]  < hCritA && i > nodesNumber * (9.0/10.0)) {
					Dynamic.SuctionNew[i] = hCritA;
				}


			}

			//Update counters
			partialIteration = partialIteration + 1;
			totalIteration = totalIteration + 1;


			//Test for convergence
			ItCrit = true;

			//Errors
			double EpsTh = 0.0;
			double EpsH = 0.0;
			double Th = 0.0;
			int i = 0;

			for(i = 0; i < nodesNumber; i++) {
				
				EpsTh = 0.0;
				EpsH = 0.0;
				
				if (Dynamic.SuctionTemp[i] < Static.nodeSoilType[i].SaturatedSuction && Dynamic.SuctionNew[i] < Static.nodeSoilType[i].SaturatedSuction) {

					Th = Dynamic.ThetaNew[i] + Dynamic.Capacity[i] * (Dynamic.SuctionNew[i] - Dynamic.SuctionTemp[i]) / 
							(Static.nodeSoilType[i].qs - Static.nodeSoilType[i].qr);

					EpsTh = Math.abs(Dynamic.ThetaNew[i] - Th);

				} else {
					EpsH = Math.abs(Dynamic.SuctionNew[i] - Dynamic.SuctionTemp[i]);
				}

				//Check increments        
				if( EpsTh > TolTh || EpsH > TolH || Math.abs(Dynamic.SuctionNew[i]) > (rMax*0.999)) {
					ItCrit = false;
					if(Math.abs(Dynamic.SuctionNew[i]) > (rMax*0.999)) partialIteration = MaxIt;
					break;
				}

			}
			
			//Log
			logger.trace("Solver error: i= " + i + ", theta= " + EpsTh + ", suction= " + EpsH + " m");


			//Keep running or done?
			if(!ItCrit || partialIteration <= 2) {

				//Next internal iteration
				if(partialIteration < MaxIt) {
					ItCrit = false;
					continue;

					//With minimal dt it should converge in MaxIt
				} else if(dt <= dtMin) {

					ConvgF = false;

					//Log
					logger.error("Error in solver: The numerical solution has not converged! (" + dt + " < " + dtMin + ").");
					System.out.println("Error in solver: The numerical solution has not converged! (" + dt + " < " + dtMin + ").");

					return dt;


					//Reduce time step and restart
				} else {

					//Recover initial values
					Dynamic.SuctionNew  = DoubleArray.copy(Dynamic.SuctionOld);
					Dynamic.SuctionTemp = DoubleArray.copy(Dynamic.SuctionOld);

					//Recover old BC
					Dynamic.topBoundCondInfiltration = TopBoundaryType;
					Dynamic.botBoundCondInfiltration = BotBoundaryType;


					//Reduce time step
					dt = Math.max(dt / 3.0, dtMin);
					
					partialIteration = 0;
					ConvgF = true;
					
					//Log
					logger.trace("Solver reducing time step: dt_new= " + dt);


				}

			}
		}

		//It converged!
		if(ItCrit) {

			//Theta is computed as Taylor expansion of pressure (implicit term)
			for(int i = 0; i < nodesNumber; i++) {
				Dynamic.ThetaNew[i] = Dynamic.ThetaNew[i] + Dynamic.Capacity[i] * (Dynamic.SuctionNew[i] - Dynamic.SuctionTemp[i]);
			}
		}

		//Check the maximum ponding allowed
		if(Dynamic.SuctionNew[nodesNumber - 1] > MaxPondingDepth) {
			Dynamic.topBoundCondInfiltration = false;
			Dynamic.SuctionNew[nodesNumber - 1] = MaxPondingDepth;
		}
		
		//Update velocities
		computeVelocity(dt, Static, Dynamic);
		
		return dt;

	}


	private static void computeVelocity(double dt, SoilColumnStaticProperties Static, SoilColumnDynamicVariables Dynamic) {

		//Log
		logger.trace("Call to computeVelocity(" + dt + "," + Static + "," + Dynamic + ").");


		//Compute top velocity
		int N = Dynamic.SuctionNew.length - 1;

		double dzN = Static.nodeZ[N] - Static.nodeZ[N-1];
		Dynamic.velNew[N] = -(Dynamic.HydConductivity[N] + Dynamic.HydConductivity[N-1]) / 2.0 * ((Dynamic.SuctionNew[N] - Dynamic.SuctionNew[N-1]) / dzN + 1.0) -
				dzN / 2.0 * ((Dynamic.ThetaNew[N] - Dynamic.ThetaOld[N]) / dt);

		//Compute internal nodel velocity
		for(int i=1; i<N ; i++) {



			double dzA = Static.nodeZ[i+1] - Static.nodeZ[i];
			double dzB = Static.nodeZ[i] - Static.nodeZ[i-1];

			double vA = - (Dynamic.HydConductivity[i] + Dynamic.HydConductivity[i+1]) / 2.0 *
					( (Dynamic.SuctionNew[i+1] - Dynamic.SuctionNew[i]) / dzA + 1.0);

			double vB = - (Dynamic.HydConductivity[i] + Dynamic.HydConductivity[i-1]) / 2.0 *
					( (Dynamic.SuctionNew[i] - Dynamic.SuctionNew[i-1]) / dzB + 1.0);

			Dynamic.velNew[i] = (vA * dzA + vB * dzB) / (dzA + dzB);

		}

		//Compute bottom velocity
		double dz1 = Static.nodeZ[1] - Static.nodeZ[0];
		Dynamic.velNew[0] = -(Dynamic.HydConductivity[0] + Dynamic.HydConductivity[1]) / 2.0 * ((Dynamic.SuctionNew[1] - Dynamic.SuctionNew[0]) / dz1 + 1.0) +
				dz1 /2.0 * ((Dynamic.ThetaNew[0] - Dynamic.ThetaOld[0]) / dt);

	}


	public void updateNodeProperties(SoilColumnStaticProperties Static, SoilColumnDynamicVariables Dynamic) {

		//Log
		logger.trace("Call to updateNodeProperties(" + Static + "," + Dynamic + ").");

		//Loop for all the nodes
		for(int i = 0; i < Dynamic.SuctionNew.length; i++) {

			Static.nodeSoilType[i].InterpolateValuesFromTable(Dynamic, i);

		}		

	}


	private void MatrixTerms(double dt, SoilColumnStaticProperties Static, SoilColumnDynamicVariables Dynamic, BottomBoundaryCondition ProjectBottomBoundaryCondition) {

		//Log
		logger.trace("Call to MatrixTerms(" + dt + "," + Static + "," + Dynamic + ").");


		//Finite differences


		//Bottom BC
		int nodesNumber = Static.nodeZ.length;

		double dzA = 0.0;
		double dzB = Static.nodeZ[1] - Static.nodeZ[0];
		double dz = dzB / 2.0;

		double ConA = 0.0;
		double ConB = (Dynamic.HydConductivity[0] + Dynamic.HydConductivity[1] ) / 2.0; //Arithmetic average
		double B = ConB * 1.0; //Grav


		DiagSupInf[0] = -ConB / dzB;

		//Stream linkage
		if(ProjectBottomBoundaryCondition.BottomBoundaryCondName.equals("FreeD")) {
			Dynamic.botBoundCondInfiltration = true;
			Dynamic.botBoundaryCondition = -B;
			//Flow rate as a function of bottom pressure and a reference value GWL0L 
		} else if(ProjectBottomBoundaryCondition.BottomBoundaryCondName.equals("qGWLF")) {
			Dynamic.botBoundCondInfiltration = true;
			Dynamic.botBoundaryCondition = Fqh(Dynamic.SuctionNew[0] - ProjectBottomBoundaryCondition.GWL0L,ProjectBottomBoundaryCondition.Aqh,ProjectBottomBoundaryCondition.Bqh);
		} else if(ProjectBottomBoundaryCondition.BottomBoundaryCondName.equals("Dirichlet")) {
			Dynamic.botBoundCondInfiltration = false;
			Dynamic.botBoundaryCondition = ProjectBottomBoundaryCondition.SuctionBottom;
		} else if(ProjectBottomBoundaryCondition.BottomBoundaryCondName.equals("von_Newman")) {
			Dynamic.botBoundCondInfiltration = true;
			Dynamic.botBoundaryCondition = ProjectBottomBoundaryCondition.FluxBottom;
		}

		double F2 = Dynamic.Capacity[0] *dz / dt;

		//First row matrix terms
		DiagonalBottom = ConB / dzB + F2;
		DiagonalInfBottom = -ConB/dzB;      
		SourceTermBottom = B + F2*Dynamic.SuctionNew[0] - (Dynamic.ThetaNew[0] - Dynamic.ThetaOld[0]) * dz / dt + Dynamic.botBoundaryCondition;

		//Internal nodes
		for(int i = 1; i < (nodesNumber - 1); i++) {

			//Length
			dzA = Static.nodeZ[i] - Static.nodeZ[i-1];
			dzB = Static.nodeZ[i+1] - Static.nodeZ[i];
			dz = (dzA + dzB) / 2.0;

			//Hydraulic conductivities
			ConA = (Dynamic.HydConductivity[i] + Dynamic.HydConductivity[i-1]) / 2.0;
			ConB = (Dynamic.HydConductivity[i] + Dynamic.HydConductivity[i+1]) / 2.0;

			B = (ConA-ConB) * 1.0;
			F2 = Dynamic.Capacity[i] * dz / dt;

			double A2 = ConA / dzA + ConB / dzB;
			double A3 =-ConB / dzB;

			//Matrix terms values
			Diagonal[i] = A2 + F2;
			SourceTerm[i] = F2 * Dynamic.SuctionNew[i] - (Dynamic.ThetaNew[i] - Dynamic.ThetaOld[i]) * dz / dt - B;
			DiagSupInf[i] = A3;

		}

		//Top BC
		dzA = Static.nodeZ[nodesNumber - 1] - Static.nodeZ[nodesNumber - 2];
		dz = dzA / 2.0;
		ConA = (Dynamic.HydConductivity[nodesNumber - 1] + Dynamic.HydConductivity[nodesNumber - 2]) / 2.0;

		B = ConA * 1.0;

		F2 = Dynamic.Capacity[nodesNumber - 1] * dz / dt;
		DiagonalTop = ConA / dzA + F2;
		DiagonalSupBottom = - ConA / dzA;
		SourceTermTop = F2 * Dynamic.SuctionNew[nodesNumber - 1] - (Dynamic.ThetaNew[nodesNumber - 1] - Dynamic.ThetaOld[nodesNumber -1]) * dz / dt - B;

		double vTop = -DiagonalSupBottom * Dynamic.SuctionNew[nodesNumber - 2] - DiagonalTop * Dynamic.SuctionNew[nodesNumber - 1] + SourceTermTop;

		//Source term adding rainfall
		SourceTermTop = SourceTermTop - Dynamic.topBoundaryCondition;

		//Source term adding ponding
		if(Dynamic.SuctionNew[nodesNumber - 1] > 0.0) {
			DiagonalTop = DiagonalTop + 1.0 / dt;
			SourceTermTop = SourceTermTop + Math.max(Dynamic.SuctionOld[nodesNumber - 1],0.0) / dt;
		} else {
			SourceTermTop = SourceTermTop + Math.max(Dynamic.SuctionOld[nodesNumber - 1],0.0) / dt;
		}
		
		
		//Seepage face at the bottom
		if(ProjectBottomBoundaryCondition.BottomBoundaryCondName.equals("SeepF")) {

			dz = Static.nodeZ[1] - Static.nodeZ[0];

			double vBot = -(Dynamic.HydConductivity[0] + Dynamic.HydConductivity[1] ) / 2.0 * 
					((Dynamic.SuctionNew[1] - Dynamic.SuctionNew[0]) / dz + 1.0) - dz / 2.0 *((Dynamic.ThetaNew[0] - Dynamic.ThetaOld[0]) / dt);

			if(!Dynamic.botBoundCondInfiltration) {
				if(vBot > 0.0) {
					Dynamic.botBoundCondInfiltration = true;
					Dynamic.botBoundaryCondition = 0.0;
				}

			} else {
				if(Dynamic.SuctionNew[1] > ProjectBottomBoundaryCondition.hSeep) {
					Dynamic.botBoundCondInfiltration = false;
					Dynamic.SuctionNew[1] = ProjectBottomBoundaryCondition.hSeep;
				}
			}
		}

		//Atmospheric boundary condition at top

		//Dirichlet
		if(!Dynamic.topBoundCondInfiltration) {


			dz = Static.nodeZ[nodesNumber-1] - Static.nodeZ[nodesNumber-2];
			vTop = -(Dynamic.HydConductivity[nodesNumber-1] + Dynamic.HydConductivity[nodesNumber-2] ) / 2.0 * 
					((Dynamic.SuctionNew[nodesNumber-1] - Dynamic.SuctionNew[nodesNumber-2]) / dz + 1.0) - dz / 2.0 *((Dynamic.ThetaNew[nodesNumber-1] - Dynamic.ThetaOld[nodesNumber-1]) / dt);


			//Check continuity in surface         
			if(Math.abs(vTop) > Math.abs(Dynamic.topBoundaryCondition) || (vTop*Dynamic.topBoundaryCondition) < 0.0) { 
				if(!Dynamic.topBoundCondInfiltration) {
					Dynamic.topBoundCondInfiltration = true;
				}
			}

			//Too much infiltration
			if(!Dynamic.topBoundCondInfiltration && Dynamic.SuctionNew[nodesNumber-1] <= (0.99*hCritA) && Dynamic.topBoundaryCondition < 0.0) {
				Dynamic.topBoundCondInfiltration = true;
			}


		//Von Newman
		} else {

			//Too much infiltration
			if(Dynamic.SuctionNew[nodesNumber-1] <= hCritA) {
				Dynamic.topBoundCondInfiltration = false;
				Dynamic.topBoundaryCondition = hCritA;
			}
		}

		

	}



	private double Fqh(double GWL,double Aqh,double Bqh) {
		return  (Aqh * Math.exp(Bqh*Math.abs(GWL)));
	}	
	
	
	private void TridiagonalGaussSolver(double dt, SoilColumnStaticProperties Static, SoilColumnDynamicVariables Dynamic, BottomBoundaryCondition ProjectBottomBoundaryCondition) {
		
		
		//Number of nodes
		int nodesNumber = Static.nodeZ.length;

		//Forward
		
		//Bottom
	      if(!Dynamic.botBoundCondInfiltration) {
	        SourceTerm[1] = SourceTerm[1]-DiagSupInf[0] * Dynamic.SuctionNew[0];
	      } else {
	        if(Math.abs(DiagonalBottom) < rMin) DiagonalBottom = rMin;
	        SourceTerm[1] = SourceTerm[1] - SourceTermBottom * DiagSupInf[0] / DiagonalBottom;
	        Diagonal[1] = Diagonal[1] - DiagonalInfBottom * DiagSupInf[0] / DiagonalBottom;
	      }
	      
	      //Internal
	      for (int i = 2; i < (nodesNumber - 1); i++) {
	        if(Math.abs(Diagonal[i-1]) < rMin) Diagonal[i-1] = rMin;
	        SourceTerm[i] = SourceTerm[i] - SourceTerm[i-1] * DiagSupInf[i-1] / Diagonal[i-1];
	        Diagonal[i] = Diagonal[i] - DiagSupInf[i-1] * DiagSupInf[i-1] / Diagonal[i-1];
	      }

	      //Top
	      if(!Dynamic.topBoundCondInfiltration) {
	        SourceTerm[nodesNumber-2] = SourceTerm[nodesNumber-2] - DiagSupInf[nodesNumber-2] * Dynamic.SuctionNew[nodesNumber-1];
	      }else{
	        if(Math.abs(Diagonal[nodesNumber-2] ) < rMin) Diagonal[nodesNumber-2] = rMin;
	        SourceTerm[nodesNumber - 1] = SourceTermTop - SourceTerm[nodesNumber-2] * DiagonalSupBottom / Diagonal[nodesNumber-2];
	        Diagonal[nodesNumber - 1] = DiagonalTop - DiagSupInf[nodesNumber-2] * DiagonalSupBottom/Diagonal[nodesNumber-2];
	      }

	//Backward
	      if(Math.abs(Diagonal[nodesNumber-2]) < rMin) Diagonal[nodesNumber-2] = rMin;

	      //Top
	      if(!Dynamic.topBoundCondInfiltration) {
	        //hNew(N)=hTop
	    	  Dynamic.SuctionNew[nodesNumber-2] = SourceTerm[nodesNumber-2] / Diagonal[nodesNumber-2];
	      } else {
	    	  Dynamic.SuctionNew[nodesNumber-1] = SourceTerm[nodesNumber-1] / Diagonal[nodesNumber-1];
	    	  Dynamic.SuctionNew[nodesNumber-2] = (SourceTerm[nodesNumber-2] - DiagSupInf[nodesNumber-2] * Dynamic.SuctionNew[nodesNumber-1]) / Diagonal[nodesNumber-2];
	      }
	      
	      //Internal
	      for (int i = (nodesNumber - 3); i > 0; i--) {

	        if(Math.abs(Diagonal[i]) < rMin) Diagonal[i] = rMin;
	        Dynamic.SuctionNew[i] = (SourceTerm[i] - DiagSupInf[i] * Dynamic.SuctionNew[i+1]) / Diagonal[i];
	      }
	      
	      if(!Dynamic.botBoundCondInfiltration) {
	    	  Dynamic.SuctionNew[0] = ProjectBottomBoundaryCondition.SuctionBottom;	        
	      } else {
	        if(Math.abs(DiagonalBottom) < rMin) DiagonalBottom = rMin;
	        Dynamic.SuctionNew[0] = (SourceTermBottom - DiagonalInfBottom * Dynamic.SuctionNew[1]) / DiagonalBottom;
	      }
	      
	}
	
	
	public void timeForward(double dt, double dtOld, SoilColumnStaticProperties Static, SoilColumnDynamicVariables Dynamic) {

		//Log
		logger.trace("Call to timeForward(" + dt + "," + dtOld + "," + Static + "," + Dynamic + ").");

		//Number of nodes
		int nodesNumber = Static.nodeZ.length;

		boolean lSat =true;

		int iBot = 0;
		if(!Dynamic.botBoundCondInfiltration) iBot = 1;

		int iTop = nodesNumber;
		if(!Dynamic.botBoundCondInfiltration) iTop = nodesNumber - 1;

		for (int i = iBot; i < iTop; i++) {

			//Extrapolate pressure
			if(Dynamic.SuctionNew[i] < 0.0 && Dynamic.SuctionOld[i] < 0.0) {
				Dynamic.SuctionTemp[i] = Dynamic.SuctionNew[i] + (Dynamic.SuctionNew[i] - Dynamic.SuctionOld[i]) * dt / dtOld;
			} else {
				Dynamic.SuctionTemp[i] = Dynamic.SuctionNew[i];
			}

			//Update old pressure values
			Dynamic.SuctionOld[i] = Dynamic.SuctionNew[i];
			Dynamic.SuctionNew[i] = Dynamic.SuctionTemp[i];

		}

		//Update water content
		for(int i = 0 ; i < nodesNumber; i++) {
			Dynamic.ThetaOld[i] = Dynamic.ThetaNew[i];
			if(Dynamic.SuctionNew[i] < 0.0) lSat = false;
		}


		if(lSat && !Dynamic.topBoundCondInfiltration && Dynamic.topBoundaryCondition > -Static.maxCondHyd) {
			Dynamic.topBoundCondInfiltration = true;
			Dynamic.SuctionNew[nodesNumber-1] = -0.005;      //0.5 cm
			Dynamic.SuctionTemp[nodesNumber-1] = -0.005;      //0.5 cm
			Dynamic.SuctionOld[nodesNumber-1] = -0.005;      //0.5 cm

		}

	}

}
