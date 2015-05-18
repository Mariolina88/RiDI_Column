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

public class FracturePropagation {

	//Pointer to the basin object
	Basin ProjectBasin;
	
	
	public FracturePropagation(Basin AuxProjectBasin) {

		//Point to the data
		ProjectBasin = AuxProjectBasin;

	}
	
	
	public void TimeStep(double dt) {
		
		//Loop over all the raster layer cells
		
		
        /*
         * Number of cells and columns
         */
        int iNX = ProjectBasin.FillTopoRaster.getNX();
        int iNY = ProjectBasin.FillTopoRaster.getNY();

        /*
         * Loop for all grid cells
         */
        for (int y = 0; y < iNY; y++) {
            for (int x = 0; x < iNX; x++) {
                ComputeFracture(x, y);
                PropagateFracture(x, y);
            }
        }
		
		
	}

	private void ComputeFracture(int i, int j) {
		
	}
	
	private void PropagateFracture(int i, int j) {
		
	}


}
