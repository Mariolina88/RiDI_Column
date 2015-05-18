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
package RiDI_Column_Lib.soilRetentionCurveModels;

import org.apache.log4j.Logger;

import RiDI_Column_Lib.SoilType;
import RiDI_Column_Lib.interfaces.InterfaceSoilRetentionCurve;

public class SRC_van_Genuchten implements InterfaceSoilRetentionCurve {
	
	

    //Logger
    private static Logger logger;

	
	public  SRC_van_Genuchten() {
	
        //Create logger and log file
        logger = Logger.getLogger("RiDI.soilRetentionCurveModels");

	}
	
	
	public double Suction(double saturation, SoilType SoilProperties) {

		//Log
		logger.debug("Call to Suction(" + saturation + ","+ SoilProperties + ").");
		
		double m = 1.0 - 1.0/SoilProperties.n;
		return Math.max( -1.0 / SoilProperties.a * Math.pow(Math.pow(saturation,(-1.0 / m)) - 1.0, (1.0 / SoilProperties.n)), -1.e37);

	}

	public double HydraulicConductivity(double suction, SoilType SoilProperties) {

		//Log
		logger.debug("Call to HydraulicConductivity(" + suction + ","+ SoilProperties + ").");
		
		double HMin = -Math.pow(1.0e+300,(1.0/SoilProperties.n)) / Math.max(SoilProperties.a, 1.0e+0);
		double HH = Math.max(suction,HMin);

		if(suction < 0.0) {

			double m = 1.0 - 1.0/SoilProperties.n;
			double Qee = Math.pow(1.0 + Math.pow((-SoilProperties.a * HH),SoilProperties.n),-m);
			double FFQ =  1.0 - Math.pow(1.0-Math.pow(Qee,(1.0/m)), m);
		
			if(FFQ < 0.0) {
				// IMPORTANTE considera la suzione col segno negativo
				FFQ = m * Math.pow(Qee,(1.0/m));
				// vd formula conducibilità: FFQ è l'argomento della potenza al quadrato
			}
			double Kr = Math.pow(Qee,SoilProperties.t) * Math.pow(FFQ, 2);

			return Math.max(SoilProperties.Ks * Kr,1.0e-37);
			// calcola la conducibilità per un dato Se imponendo la cond che sia maggiore di 1.0e-37
		
		} else {
			return SoilProperties.Ks;
		}

	}

	public double WaterCapacity(double suction, SoilType SoilProperties) {

		//Log
		logger.debug("Call to WaterCapacity(" + suction + ","+ SoilProperties + ").");

		double m = 1.0 - 1.0 / SoilProperties.n;

		double HMin = -Math.pow(1.0e+300,(1.0/SoilProperties.n)) / Math.max(SoilProperties.a, 1.0e+0);
		double HH = Math.max(suction,HMin);

		if(suction < 0.0) {
			//derivata del volumetric content
			
			double C1 = Math.pow(1.0 + Math.pow((-SoilProperties.a * HH),SoilProperties.n),(-m-1));
			double C2 = (SoilProperties.qs - SoilProperties.qr) * m * SoilProperties.n * Math.pow(SoilProperties.a, SoilProperties.n)* Math.pow(-HH, SoilProperties.n-1.0) * C1;
			
			return Math.max(C2,1.0e-37);
		} else {
			return 0.0;
		}

	}

	public double WaterContent(double suction, SoilType SoilProperties) {

		//Log
		logger.debug("Call to WaterContent(" + suction + ","+ SoilProperties + ").");

		double m = 1.0 - 1.0 / SoilProperties.n;

		double HMin = -Math.pow(1.0e+300,(1.0/SoilProperties.n)) / Math.max(SoilProperties.a, 1.0);
		double HH = Math.max(suction,HMin);

		if(suction < 0.0) {
			double Qee = Math.pow(1.0 + Math.pow((-SoilProperties.a * HH),SoilProperties.n),-m);
			// non capisco perchè (-m-1) e non solo m
			return Math.max(SoilProperties.qr + (SoilProperties.qs - SoilProperties.qr) * Qee,1.d-37);

		} else {
			return SoilProperties.qs;
		}

	}

	public double Saturation(double suction, SoilType SoilProperties) {

		//Log
		logger.debug("Call to Saturation(" + suction + ","+ SoilProperties + ").");

		double m = 1.0 - 1.0 / SoilProperties.n;


		if(suction < 0.0) {

			double HMin = -Math.pow(1.0e+300,(1.0/SoilProperties.n)) / Math.max(SoilProperties.a, 1.0e+0);
			double HH = Math.max(suction,HMin);
			double saturation = Math.pow(1.0 + Math.pow((-SoilProperties.a * HH),SoilProperties.n),-m);
			return  Math.max(saturation,1.e-37);
			
		} else {
			return 1.0;
		}

	}

}
