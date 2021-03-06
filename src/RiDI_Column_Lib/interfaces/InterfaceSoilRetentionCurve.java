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

import RiDI_Column_Lib.SoilType;

/**
 * @author Administrador
 *
 */
public interface InterfaceSoilRetentionCurve {
	
	public double Suction(double saturation, SoilType SoilProperties);
	public double HydraulicConductivity(double suction, SoilType SoilProperties);
	public double WaterCapacity(double suction, SoilType SoilProperties);
	public double WaterContent(double suction, SoilType SoilProperties);
	public double Saturation(double suction, SoilType SoilProperties);

}
