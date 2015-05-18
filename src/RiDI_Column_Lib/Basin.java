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

/*
 * Used classes.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import es.unex.sextante.core.OutputFactory;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.rasterWrappers.GridExtent;
import GITS.IMPRINTS.COMMON.SextanteGrid;
import GITS.IMPRINTS.COMMON.RasterOutputFactory;
import RiDI_Column_Lib.SoilColumn;

import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.apache.log4j.Logger;

/**
 * @author
 *
 */
public class Basin {

	
	//Geospatial data
	
		
    //Basin class rasters

    
    //Input data
    protected static SextanteGrid dtm;
    protected static SextanteGrid soilUnit;
    
    //New rasters creator
    protected static OutputFactory BasinOutputFactory;
    
	//Computed rasters
    public IRasterLayer FillTopoRaster = null;	
    public IRasterLayer SlopeRaster = null;
    public IRasterLayer AccFlowRaster = null;
    public IRasterLayer DirectionRaster = null;
    
    //Model results rasters
    public SextanteGrid DischargeRaster;
    public SextanteGrid DepthRaster;
    public SextanteGrid FactorStabilitySatRaster;	
    public SextanteGrid FactorStabilityUnsatRaster;	
    public SextanteGrid RainIntensityRaster;	
    public SextanteGrid ExtraWeightRaster;
    public SextanteGrid BrokenCellRaster;
    public SextanteGrid Watertable;

    //Physical parameters
    public double gParameter = 9.81;
    public double DensAgua = 1000.0;
    
    //SEXTANTE static strings
    public static final String H = "H";
    public static final String Q = "Q";
    public static final String FOS_Sat = "FOS_Sat";
    public static final String FOS_Unsat = "FOS_Unsat";
    public static final String RI = "RI";
    public static final String W = "W";
    public static final String B = "B";    
    public static final String WT = "WT";    
    public static final String DTM = "DTM";    
    public static final String SOILUNIT = "SOILUNIT";    
    public static final String FILL = "FILL";    
    public static final String SLOPE = "SLOPE";    
    public static final String ACC = "ACC";    
    public static final String DIRECTION = "DIRECTION"; 
    
    //Column especial properties
    public double ColumnSlope;
    
    //Soil columns
    public SoilColumn[][] BasinSoilColumns; 
    
    //Soil types catalog
    protected SoilsCatalog BasinSoilsCatalog;

    //Soil units catalog (stratigraphy)
    protected UnitsCatalog BasinUnitsCatalog;	
	
	//Column nodes number
	public int numNodes;
	
    //Logger
    private static Logger logger;
    
    
	/**
	 * @param AuxFillTopoRaster
	 * @param AuxSlopeRaster
	 * @param AuxAccFlowRaster
	 */
	public Basin() {
		
        //Create logger and log file
        logger = Logger.getLogger("RiDI.Basin");
		
		
        /*
         * Initialize the SEXTANTE library.
         * This will load all the algorithms and resource strings.
         * Since no language code is passed, default language(en)
         * will be used
         */
        Sextante.initialize();
		
		//Create the rasters constructor used by SEXTANTE geoalgorithms
		BasinOutputFactory = new RasterOutputFactory();						
		
	}
		
	public void ReadSoilTypesCatalog(String filename) {
		
		//Log
		logger.info("Call to ReadSoilTypesCatalog().");
		
		//Instantiate the soils catalog
		BasinSoilsCatalog = new SoilsCatalog(); 
	
		//Read the soils properties file
		BasinSoilsCatalog.ReadInputFile(filename);
		
	}
	
	public void ComputePropertiesTable(double MinSucTab, double MaxSucTab, int NumberRowsTab) {
	
		//Log
		logger.info("Call to ComputePropertiesTable" + MinSucTab + MaxSucTab + NumberRowsTab);

		//Call table creator
		BasinSoilsCatalog.ComputePropertiesTable(MinSucTab, MaxSucTab, NumberRowsTab);
	
	}
	
	public void ReadSoilUnitsCatalog(String filename) {
		
		//Log
		logger.info("Call to ReadSoilUnitsCatalog().");
		
		//Instantiate the soils catalog
		BasinUnitsCatalog = new UnitsCatalog(); 
	
		//Read the soils properties file and check the coherence of soils and units catalogs
		BasinUnitsCatalog.ReadInputFile(filename, this.BasinSoilsCatalog);
		
	}	
		
	public void CreateBasinDataStructures(int auxNumNodes){
		
		//Log
		logger.info("Call to CreateBasinDataStructures().");
		
		//Store number of nodes per column
		numNodes = auxNumNodes;
		
		//Define dummy grid extent
		GridExtent BasinExtent = new GridExtent();

		BasinExtent.setCellSize(1);
		BasinExtent.setXRange(0, 1);
		BasinExtent.setYRange(0, 1);
		
		
		//Create raster containing results
		DepthRaster = new SextanteGrid();
		DepthRaster.create(H, "Dummy_H.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		DepthRaster.setFullExtent();

		DischargeRaster = new SextanteGrid();
		DischargeRaster.create(Q, "Dummy_Q.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		DischargeRaster.setFullExtent();
		
		FactorStabilitySatRaster = new SextanteGrid();
		FactorStabilitySatRaster.create(FOS_Sat, "Dummy_FOS_Sat.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		FactorStabilitySatRaster.setFullExtent();

		FactorStabilityUnsatRaster = new SextanteGrid();
		FactorStabilityUnsatRaster.create(FOS_Unsat, "Dummy_FOS_Unsat.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		FactorStabilityUnsatRaster.setFullExtent();
		
		RainIntensityRaster = new SextanteGrid();
		RainIntensityRaster.create(RI, "Dummy_RI.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		RainIntensityRaster.setFullExtent();

		ExtraWeightRaster = new SextanteGrid();
		ExtraWeightRaster.create(W, "Dummy_Weight.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		ExtraWeightRaster.setFullExtent();

		BrokenCellRaster = new SextanteGrid();
		BrokenCellRaster.create(B, "Dummy_Broken.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_INT, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		BrokenCellRaster.setFullExtent();
		
		Watertable = new SextanteGrid();
		Watertable.create(WT, "Dummy_Watertable.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		Watertable.setFullExtent();
		
		dtm = new SextanteGrid();
		dtm.create(DTM, "Dummy_Broken.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_INT, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		dtm.setFullExtent();
		
		soilUnit = new SextanteGrid();
		soilUnit.create(SOILUNIT, "Dummy_Watertable.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		soilUnit.setFullExtent();
		
		FillTopoRaster = new SextanteGrid();
		((SextanteGrid) FillTopoRaster).create(FILL, "Dummy_FillTopoRaster.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_INT, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		FillTopoRaster.setFullExtent();
		
		SlopeRaster = new SextanteGrid();
		((SextanteGrid) SlopeRaster).create(SLOPE, "Dummy_SlopeRaster.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		SlopeRaster.setFullExtent();
		
		AccFlowRaster = new SextanteGrid();
		((SextanteGrid)AccFlowRaster).create(ACC, "Dummy_AccFlowRaster.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_INT, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		AccFlowRaster.setFullExtent();
		
		DirectionRaster = new SextanteGrid();
		((SextanteGrid)DirectionRaster).create(DIRECTION, "Dummy_DirectionRaster.asc", BasinExtent, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, 1, DefaultEngineeringCRS.CARTESIAN_2D);
		DirectionRaster.setFullExtent();
		
		//Set raster values
		DepthRaster.setCellValue(0, 0, 0.0);
		DischargeRaster.setCellValue(0, 0, 0.0);
		FactorStabilitySatRaster.setCellValue(0, 0, 0.0);
		FactorStabilityUnsatRaster.setCellValue(0, 0, 0.0);
		RainIntensityRaster.setCellValue(0, 0, 0.0);
		ExtraWeightRaster.setCellValue(0, 0, 0.0);
		BrokenCellRaster.setCellValue(0, 0, 0.0);
		Watertable.setCellValue(0, 0, 0.0);
		dtm.setCellValue(0, 0, 0.0);
		soilUnit.setCellValue(0, 0, 1.0);
	    FillTopoRaster.setCellValue(0, 0, 0.0);
	    SlopeRaster.setCellValue(0, 0, ColumnSlope);
	    AccFlowRaster.setCellValue(0, 0, 0.0);
	    DirectionRaster.setCellValue(0, 0, 0.0);

		
		//Create soil columns
		BasinSoilColumns = new SoilColumn [FillTopoRaster.getNX()][FillTopoRaster.getNY()]; 
		
		//Initialize the columns
        for (int y = 0; y < BasinSoilColumns[0].length; y++) {
            for (int x = 0; x < BasinSoilColumns.length ; x++) {
            	
            	if (!soilUnit.isNoDataValue(soilUnit.getCellValueAsInt(x, y))) {
            		BasinSoilColumns[x][y] = new SoilColumn(soilUnit.getCellValueAsInt(x, y));
            		BasinSoilColumns[x][y].CreateColumnNodes(auxNumNodes,BasinUnitsCatalog,BasinSoilsCatalog);
            	}
            }
        }
		
				
	}
		
	public void PrintColumnResults(double t, double X, double Y, String time, double dt, int iter) {
		
		//Log
		logger.debug("Call to PrintColumnResults(" + X + "," + Y + "," + time + ").");
				
		BasinSoilColumns[0][0].PrintVerticalResults(time, dt);
		BasinSoilColumns[0][0].PrintBalance(t, time, dt, iter);
			
}
		
	public void PrintColumnProfiles(double X, double Y, String nodeNumber) {

		//Log
		logger.debug("Call to PrintColumnProfiles(" + X + "," + Y + "," + nodeNumber + ").");
				
		BasinSoilColumns[0][0].PrintVerticalProfile(nodeNumber);

	}

	public void ReadColumnSlopeFile(String FileName) {

		//Log
		logger.debug("Call to ReadColumnSlopeFile(" + FileName + ").");
				
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
                ColumnSlope = Double.parseDouble(tokens[2]);//(hr)
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
	

	
}
