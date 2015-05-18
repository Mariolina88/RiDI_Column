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

public class BottomBoundaryCondition {
	
    //Logger
    private static Logger logger;

	public String BottomBoundaryCondName; //Dirichlet von_Newman FreeD qGWLF SeepF
	public double GWL0L;
	public double Aqh;
	public double Bqh;
	public double hSeep;
	public double SuctionBottom;
	public double FluxBottom;

	public BottomBoundaryCondition() {

        //Create logger and log file
        logger = Logger.getLogger("RiDI.BottomBoundaryCondition");

	}
}