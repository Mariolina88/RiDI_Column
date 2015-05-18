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
import java.util.Arrays;

public class Climate {

    //Logger
    private static Logger logger;
    
    private double time[];
    private double rainfall[];

    
	public Climate() {

        //Create logger and log file
        logger = Logger.getLogger("RiDI.Climate");
        
        //Create short arrays
        time = new double[1];
        rainfall = new double[1];
        
	}
	
	public double getRainfall(double t) {
		
		return interpPiecewise(time, rainfall, t);

	}

	public void ReadInputFile(String FileName) {
		
		//Log
		logger.info("Call to ReadInputFile(" + FileName + ").");


		//Open file and create reader
		File file = new File(FileName);
        BufferedReader reader = null;

        //Tokenizer
        String delims = "[\\t\\s]+";

        //IO into exceptions catch
        try {
            reader = new BufferedReader(new FileReader(file));
            String text = null;
            double auxTime;
            double auxRainfall;
            String[] tokens;

            //Read header
            text = reader.readLine();
            text = reader.readLine();
            
            //First value of the series
            text = reader.readLine();
            tokens = text.split(delims);
            
            //Compute rainfall intensity in m/s
            auxTime = Double.parseDouble(tokens[0] + 1.0) * 3600.0;
            auxRainfall = Double.parseDouble(tokens[1]) / (1000.0 * 3600);
            
           	time[0] = 0.0;
        	rainfall[0] = auxRainfall;
        	
    		time = DoubleArray.insert(time, time.length, auxTime);
    		rainfall = DoubleArray.insert(rainfall, rainfall.length, auxRainfall);


            //Read file until EOF
            while( (text = reader.readLine()) != null) {
            	tokens = text.split(delims);
            	
            	if(tokens.length == 2) {
            	
            		//Read new values
            		auxRainfall = Double.parseDouble(tokens[1]) / (1000.0 * 3600.0);
            		auxTime = (Double.parseDouble(tokens[0]) + 1.0) * 3600.0;

            		//Add new values to array
            		time = DoubleArray.insert(time, time.length, auxTime);
            		rainfall = DoubleArray.insert(rainfall, rainfall.length, auxRainfall);
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
        
	}
	
	
	public static final double interpLinear(double[] x, double[] y, double xi) throws IllegalArgumentException {

		//Log
		logger.info("Call to interpLinear t = " + xi + ").");
		
		if (x.length != y.length) {
			throw new IllegalArgumentException("X and Y must be the same length");
		}
		if (x.length == 1) {
			throw new IllegalArgumentException("X must contain more than one value");
		}
		double[] dx = new double[x.length - 1];
		double[] dy = new double[x.length - 1];
		double[] slope = new double[x.length - 1];
		double[] intercept = new double[x.length - 1];

		// Calculate the line equation (i.e. slope and intercept) between each point
		for (int i = 0; i < x.length - 1; i++) {
			dx[i] = x[i + 1] - x[i];
			if (dx[i] == 0) {
				throw new IllegalArgumentException("X must be montotonic. A duplicate " + "x-value was found");
			}
			if (dx[i] < 0) {
				throw new IllegalArgumentException("X must be sorted");
			}
			dy[i] = y[i + 1] - y[i];
			slope[i] = dy[i] / dx[i];
			intercept[i] = y[i] - x[i] * slope[i];
		}

		// Perform the interpolation here
		double yi;

		if ((xi > x[x.length - 1]) || (xi < x[0])) {
			throw new IllegalArgumentException("t = " + xi + " max t = " + x[x.length - 1]);
		} else {
			int loc = Arrays.binarySearch(x, xi);
			if (loc < -1) {
				loc = -loc - 2;
				yi = slope[loc] * xi + intercept[loc];
			}
			else {
				yi = y[loc];
			}
		}

		return yi;
	}
    

	public static final double interpPiecewise(double[] x, double[] y, double xi) throws IllegalArgumentException {

		//Log
		logger.info("Call to interpPiecewise t = " + xi + ").");
		
		if (x.length != y.length) {
			throw new IllegalArgumentException("X and Y must be the same length");
		}
		if (x.length == 1) {
			throw new IllegalArgumentException("X must contain more than one value");
		}

		// Perform the interpolation here
		double yi;

		if ((xi > x[x.length - 1]) || (xi < x[0])) {
			throw new IllegalArgumentException("t = " + xi + " max t = " + x[x.length - 1]);
		} else {
			int loc = Arrays.binarySearch(x, xi);
			if (loc < -1) {
				loc = -loc - 2;
			}
			yi = y[loc];
		}

		return yi;
	}

	
}
