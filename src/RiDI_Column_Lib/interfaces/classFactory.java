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
/**
 * 
 */
package RiDI_Column_Lib.interfaces;

import java.lang.reflect.InvocationTargetException;
import org.apache.log4j.Logger;
import RiDI_Column_Lib.Basin;


/**
 * @author Administrador
 *
 */
public class classFactory {

	
    //Logger
    private static Logger logger;
    
    
	/**
	 * Constructor
	 */
	public classFactory() {

		//Create logger and log file
        logger = Logger.getLogger("RiDI.classFactory");

	}
    
    
	/**
	 * 1 Argument factory, for SRC classes
	 */
	public Object makeFactory(String className) {
		
		//Log
		logger.info("Call to makeFactory(" + className + ").");

		//Create new instance of SRC model using its name
		try {

			return Class.forName(className).getConstructor().newInstance();
		
		} catch (InstantiationException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Impossible to instantiate object: " + className);
			return null;	        
		} catch (IllegalAccessException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Impossible to access object: " + className);
			return null;	        
		} catch (IllegalArgumentException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Wrong arguments to constructor: " + className);
			return null;	        
		} catch (InvocationTargetException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Impossible to invocate object: " + className);
			return null;	        
		} catch (NoSuchMethodException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Impossible to call constructor: " + className);
			return null;	        
		} catch (SecurityException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Security exception in object: " + className);
			return null;
		} catch (ClassNotFoundException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: class defined in case data files not found: " + className);
			return null;
		} catch (Exception ex) {
			logger.fatal("Exception!", ex);
			return null;
		}
		
	}

	/**
	 * 2 Argument factory, for RO and IC classes
	 */
	public Object makeFactory(String className, Basin ProjectBasin) {
		
		//Log
		logger.info("Call to makeFactory(" + className + "," + ProjectBasin + ").");

		//Create new instance of SRC model using its name
		try {

			return Class.forName(className).getConstructor(Basin.class).newInstance(ProjectBasin);
		
		} catch (InstantiationException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Impossible to instantiate object: " + className);
			return null;	        
		} catch (IllegalAccessException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Impossible to access object: " + className);
			return null;	        
		} catch (IllegalArgumentException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Wrong arguments to constructor: " + className);
			return null;	        
		} catch (InvocationTargetException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Impossible to invocate object: " + className);
			return null;	        
		} catch (NoSuchMethodException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Impossible to call constructor: " + className);
			return null;	        
		} catch (SecurityException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: Security exception in object: " + className);
			return null;
		} catch (ClassNotFoundException ex) {
	    	logger.fatal("Exception!", ex);
	        System.err.println("ERROR: class defined in case data files not found: " + className);
			return null;
		} catch (Exception ex) {
			logger.fatal("Exception!", ex);
			return null;
		}
		
	}

	
}
