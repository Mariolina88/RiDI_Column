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


//Standard JAVA imports
import java.io.IOException;
import java.util.Locale;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Level;

import RiDI_Column_Lib.interfaces.InterfaceInitialConditions;
import RiDI_Column_Lib.interfaces.classFactory;



public class Main {

	
	/**
	 * @param args
	 */

    //Numerical parameters
    private static NumericalModelParameters Numerics;
    
    //Basin data
    private static Basin ProjectBasin;
        
    //Vertical infiltration
    private static VerticalInfiltration BasinVerticalInfiltration;
    
    //Stability factor
    private static StabilityFactor BasinStabilityFactor;
    
    //Initial conditions
    private static InterfaceInitialConditions BasinInitialConditions;
    
    //Boundary conditions
    private static Climate BasinClimate;
    
    //Logger
    private static Logger logger;
    
    
    public static void main(String[] args) {

    	//Objects and libraries initialization
        initialize();
        
        //Set global logging level
        logger.setLevel(Level.INFO);

        try {
        	
        	//Read data
            readInputData();
                                    
            //Create soil columns, basin raster and compute soil properties
            ProjectBasin.CreateBasinDataStructures(Numerics.ColumnNodes);
            
            //Print the profile properties of selected columns for output
            PrintSelectedColumnProfile();
            
            //Compute initial conditions
    		BasinInitialConditions.ComputeInitialConditions();
    		    		
    		//Set the auxiliar arrays for Richards solver
    		BasinVerticalInfiltration.InitializeAuxVariables(Numerics.maxPonding);
            
            
            //Main temporal loop
            runTransienModel();
            
            System.exit(0);
            
        } catch (Exception ex) {
            System.out.println("ERROR: Error exception, ");
            ex.printStackTrace();
            logger.error("Exception!",ex);
            System.exit(1);
        }

    }

    
    private static void initialize() {
    	
        //Create logger and log file
        logger = Logger.getLogger("RiDI");
        Appender fh = null;

        //Logger file should be configured in a try/catch block
        try {

        	fh = new FileAppender(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %-30c{1}:%5L - %m%n"), "RiDI_Column.log", false);
            logger.addAppender(fh);

            logger.info("Initializaton of log4j logger.");
            

        } catch (SecurityException ex) {
            logger.error("Exception!",ex);
        } catch (IOException ex) {
            logger.error("Exception!",ex); //.printStackTrace();
        }
        
		logger.info("Call to initialize().");
		
		//Set local formats
		Locale.setDefault(new Locale("en", "US"));

		
        //Instantiate classes
        Numerics = new NumericalModelParameters();
        ProjectBasin = new Basin();
        
        //Instantiate models
        BasinVerticalInfiltration = new VerticalInfiltration(ProjectBasin);
        BasinStabilityFactor = new StabilityFactor(ProjectBasin);
        BasinClimate = new Climate();
        
        /*
         * The initial conditions model could not be instantiated yet because it is defined in the "input.txt" file,
         * which has not been ridden yet.
         * Hence, IC are instantiated after reading input files, using a specific class factory
         */
      
    }


    private static void readInputData() {

		//Log
		logger.info("Call to readInputData().");

	
        /*
         * Read parameters from file "input.txt"
         */
		Numerics.ReadInputFile("input.txt");


        /*
         * Read the basin soil types catalog
         */		
		ProjectBasin.ReadSoilTypesCatalog("SoilParameters.csv");

        /*
         * Compute the interpolation table of the soil types catalog
         */			
		ProjectBasin.ComputePropertiesTable(Numerics.MinSucTab, Numerics.MaxSucTab, Numerics.NumberRowsTab);

        /*
         * Read the basin soil units catalog (stratigraphy)
         */		
		ProjectBasin.ReadSoilUnitsCatalog("SoilStratigraphy.csv");
		
        /*
         * Read the basin soil units catalog (stratigraphy)
         */		
		ProjectBasin.ReadColumnSlopeFile("columnSlope.txt");		

		/*
         * Create class factory to instantiate the initial conditions class
         */		
		classFactory ICclassFactory = new classFactory();

		/*
		 * Call the object maker to get the initial conditions object
		 */
		BasinInitialConditions = (InterfaceInitialConditions) ICclassFactory.makeFactory("RiDI_Column_Lib.initialConditions." + Numerics.initialConditions, ProjectBasin);

		/*
		 * Read the initial conditions file
		 */
		BasinInitialConditions.ReadInputFile("initialConditions.txt");
				
		/*
		 * Read the rainfall time series data
		 */		
		BasinClimate.ReadInputFile("rainfall.txt");	

		/*
		 * Read the bottom boundary condition
		 */		
		BasinVerticalInfiltration.readBottomInfiltration("bottomInfiltration.txt");
	
}


    
    /**
     * Compute the transient process, including infiltration and surface runoff.
     */
    private static void runTransienModel() {
    	
		//Log
		logger.info("Call to runTransienModel().");
		
		//Screen
	    System.out.println("Starting time loop computations...");

    	
    	//Variables
    	double t;	//Time
    	double dt;	//Time step
    	int iterationsCount = 0;
    	
    	//Time range
    	t = Numerics.Tini;
    	dt = Numerics.dt;


    	
    	//Main temporal loop    	
    	while (t <= Numerics.Tfin) {

    		//Selected columns output
    		if(Numerics.ResultOutput(t)) { 			    			
        		PrintDetailedResults(t, dt, iterationsCount);
    		}
    		
    		//Check time step size
    		dt = Numerics.CheckTimeStep(t);

    		
    		//Log
    		logger.info("Loop: iteration= " + iterationsCount + ", t= " + t + ", dt= " + dt + ", Tfin= " + Numerics.Tfin);
    		
    		//Screen
    	    System.out.println("Loop: iteration= " + iterationsCount + ", t= " + t + ", dt= " + dt + ", Tfin= " + Numerics.Tfin);

    		
    		//Update boundary conditions
    		BasinVerticalInfiltration.updateRainfall(t, BasinClimate);
    		
    		//Compute vertical infiltration
    		BasinVerticalInfiltration.TimeStep(dt, Numerics.dtMin, Numerics.TolTh, Numerics.TolH, Numerics.MaxIt);
    		
    		//Compute stability factor
    		BasinStabilityFactor.TimeStep(dt);
    		
    		//Update t
    		t = t + dt;
    		
    		
    		iterationsCount++;
    					
    	} //End loop
    	
    }
    
    
    
    private static void PrintDetailedResults(double t, double dt, int iter) {
    	
		//Log
		logger.info("Call to PrintDetailedResults(" + t + ").");

		//Screen
	    System.out.println("Printing profile results for time: " + t);
		
		
		//Call the vertical column results
		ProjectBasin.PrintColumnResults(t, 0.0, 0.0, String.format("%03d_%08f",1,t), dt, iter);
    	
    }
    
    
    private static void PrintSelectedColumnProfile() {
    	
		//Log
		logger.info("Call to PrintSelectedColumnProfile().");

		//Screen
	    System.out.println("Printing profile information for selected nodes.");

		//Call the vertical column results
		ProjectBasin.PrintColumnProfiles(0.0, 0.0, "001");
    	
    }
    

}
