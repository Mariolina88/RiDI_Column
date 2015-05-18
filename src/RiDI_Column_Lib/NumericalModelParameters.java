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

/**
 * @author Vicente
 *
 */
public class NumericalModelParameters {

	//Model configuration parameters
	public double Tini;
	public double Tfin;
	public double dt;
	public double dtMin;
	public double TolTh;
	public double TolH;
	public int MaxIt;
	private int Nout;
	public int ColumnNodes;
	public boolean Restart;
	private double OutStep;
	private double NextResultOutput;
	public double MinSucTab;
	public double MaxSucTab;
	public int NumberRowsTab;
	public String initialConditions;
	public double maxPonding;
	
	//Coordinates of the vertical column output points
	int numberOfPoints = 10;
	public double[] Xcoord = new double[numberOfPoints];
	public double[] Ycoord = new double[numberOfPoints];
	
	
    //Logger
    private static Logger logger;
	
	
	public NumericalModelParameters() {
		
        //Create logger and log file
        logger = Logger.getLogger("RiDI.NumericalModelParameters");
        
	}
	
	/**
	 * @param FileName
	 */
	public void ReadInputFile(String FileName) {
		
		//Log
		logger.info("Call to ReadInputFile(" + FileName + ").");


		//Open file and create reader
		File file = new File(FileName);
        BufferedReader reader = null;

        //Tokenizer
        String delims = "[ =\\t]+";

        //IO into exceptions catch
        try {
            reader = new BufferedReader(new FileReader(file));
            String text = null;

            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                Restart = Boolean.parseBoolean(tokens[1]);//()
            }
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                Tini = Double.parseDouble(tokens[2]);//(hr)
            }
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                Tfin = Double.parseDouble(tokens[2]);//(hr)
            }
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                dt = Double.parseDouble(tokens[2]);//(hr)
            }
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                dtMin = Double.parseDouble(tokens[2]);//(hr)
            }            
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                Nout = Integer.parseInt(tokens[1]);//()
            }
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                ColumnNodes = Integer.parseInt(tokens[1]);//()
            }
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                MinSucTab = Double.parseDouble(tokens[2]);//(Pa)
            }
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                MaxSucTab = Double.parseDouble(tokens[2]);//(Pa)
            }
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                NumberRowsTab = Integer.parseInt(tokens[1]) + 1;//()
            }
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
            	initialConditions = tokens[1];//()
            }
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                maxPonding = Double.parseDouble(tokens[2]);//(m)
            }     
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                TolTh = Double.parseDouble(tokens[2]);//()
            }          	
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                TolH = Double.parseDouble(tokens[2]);//(m)
            }  
            if ((text = reader.readLine()) != null) {
                String[] tokens = text.split(delims);
                MaxIt = Integer.parseInt(tokens[1]);//()
            }
            
            for(int i = 0; i < numberOfPoints; i++) {
                if ((text = reader.readLine()) != null) {
                    String[] tokens = text.split(delims);
                	Xcoord[i] = Double.parseDouble(tokens[2]);//(m)
                }            	
                if ((text = reader.readLine()) != null) {
                    String[] tokens = text.split(delims);
                	Ycoord[i] = Double.parseDouble(tokens[2]);//(m)
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
            }
        }
        
        //Compute result output timestep
        ComputeResultOutputTimeStep();
        NextResultOutput = 0.0;
		
	}

	/**
	 * 
	 */
	private void ComputeResultOutputTimeStep() {
		
		//Log
		logger.info("Call to ComputeResultOutputTimeStep().");
		
		this.OutStep = (this.Tfin - this.Tini) / this.Nout;
		
	}
	
	
	/**
	 * @param Time
	 * @return
	 */
	public double CheckTimeStep(double Time) {
		
		//Log
		logger.info("Call to CheckTimeStep(" + Time + ").");
		
		double correctedTime;
		double correctedDelta;
		double deltaTime = dt;
		
		//Time step short enough to avoid result output mismatch
		if ( (int)Math.floor(Time / this.OutStep) != (int)Math.floor( (Time + deltaTime) / this.OutStep))
		{
			correctedTime = this.OutStep * (((int)Math.floor(Time / this.OutStep)) + 1);
			correctedDelta = correctedTime - Time;
		}
		else
		{
			correctedDelta = deltaTime;
		}

		return correctedDelta;
		
	}
	
	public boolean ResultOutput(double Time) {
		
		//Log
		logger.info("Call to ResultOutput(" + Time + ").");
		
		
		if ( Time >= NextResultOutput) {
			this.NextResultOutput = this.NextResultOutput + this.OutStep;
			return true;
		} else {
			return false;
		}
	}
	
	
	
}
