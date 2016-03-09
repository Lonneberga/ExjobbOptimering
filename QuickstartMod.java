/*******************************************************************************
 * Copyright (C) 2014  Stefan Schroeder
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.geotools.tutorial.quickstart;

import jsprit.analysis.toolbox.GraphStreamViewer;
import jsprit.analysis.toolbox.GraphStreamViewer.Label;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.box.SchrimpfFactory;
import jsprit.core.problem.Location;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.io.VrpXMLWriter;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.vehicle.VehicleImpl;
import jsprit.core.problem.vehicle.VehicleImpl.Builder;
import jsprit.core.problem.vehicle.VehicleType;
import jsprit.core.problem.vehicle.VehicleTypeImpl;
import jsprit.core.reporting.SolutionPrinter;
import jsprit.core.util.Solutions;

import java.io.File;
import java.util.Collection;
import java.io.*;
import java.util.ArrayList;


public class QuickstartMod {
    
    
    
    public static void main(String[] args) {

        //Read coordinates and put in a matrix.

        //int[][] coordMatrix = readCoordinates(5,"/Users/Emil/Documents/exjobb/koordinater/actual/ten.txt");
        int[][] coordMatrix = readCoordinates(5,"/Users/Emil/Documents/exjobb/koordinater/one.txt");
        int coordNumber = coordMatrix.length - 2;



        /*
         * some preparation - create output folder
         */
        File dir = new File("output");
        // if the directory does not exist, create it
        if (!dir.exists()){
            System.out.println("creating directory ./output");
            boolean result = dir.mkdir();  
            if(result) System.out.println("./output created");  
        }
        
        /*
         * get a vehicle type-builder and build a type with the typeId "vehicleType" and one capacity dimension, i.e. weight, and capacity dimension value of 2
         */
        final int WEIGHT_INDEX = 0;
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType").addCapacityDimension(WEIGHT_INDEX, 2);
        VehicleType vehicleType = vehicleTypeBuilder.build();
        
        /*
         * get a vehicle-builder and build a vehicle located at (10,10) with type "vehicleType"
         */
        Builder vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle");
        //vehicleBuilder.setStartLocation(Location.newInstance(10, 10));
        vehicleBuilder.setStartLocation(Location.newInstance(coordMatrix[coordNumber+1][0], coordMatrix[coordNumber+1][1]));
        vehicleBuilder.setType(vehicleType);
        VehicleImpl vehicle = vehicleBuilder.build();
        
        /*
         * build services at the required locations, each with a capacity-demand of 1.
         */
        /*
        Service service1 = Service.Builder.newInstance("1").addSizeDimension(WEIGHT_INDEX, 0).setLocation(Location.newInstance(140, 160)).build();
        Service service2 = Service.Builder.newInstance("2").addSizeDimension(WEIGHT_INDEX, 0).setLocation(Location.newInstance(140, 150)).build();
        
        Service service3 = Service.Builder.newInstance("3").addSizeDimension(WEIGHT_INDEX, 0).setLocation(Location.newInstance(150, 160)).build();
        Service service4 = Service.Builder.newInstance("4").addSizeDimension(WEIGHT_INDEX, 0).setLocation(Location.newInstance(150, 140)).build();

        Service service5 = Service.Builder.newInstance("5").addSizeDimension(WEIGHT_INDEX, 0).setLocation(Location.newInstance(150, 150)).build();
        */

        //Added by Emil
        Service[] serviceArray = new Service[coordNumber];
        for(int i = 0; i < coordNumber; i++){
            int longitude = coordMatrix[i][0];
            int latitude = coordMatrix[i][1];
            serviceArray[i] = Service.Builder.newInstance(Integer.toString(i)).addSizeDimension(WEIGHT_INDEX, 0).setLocation(Location.newInstance(longitude, latitude)).build();
        }









        //No longer added by Emil
            
        
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        vrpBuilder.addVehicle(vehicle);
        //vrpBuilder.addJob(service1).addJob(service2).addJob(service3).addJob(service4).addJob(service5);

        //Emil modification
        System.out.print("Debug");
        //vrpBuilder.addJob(serviceArray[0]).addJob(serviceArray[1]).addJob(serviceArray[2]).addJob(serviceArray[3]).addJob(serviceArray[4]).addJob(serviceArray[5]).addJob(serviceArray[6]).addJob(serviceArray[7]).addJob(serviceArray[8]).addJob(serviceArray[9]);
        vrpBuilder.addJob(serviceArray[0]);

        VehicleRoutingProblem problem = vrpBuilder.build();
        
        /*
         * get the algorithm out-of-the-box. 
         */
        VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
        
        /*
         * and search a solution
         */
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
        
        /*
         * get the best 
         */
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
        
        new VrpXMLWriter(problem, solutions).write("output/problem-with-solution.xml");
        
        SolutionPrinter.print(problem, bestSolution, SolutionPrinter.Print.VERBOSE);
        
        /*
         * plot
         */
//      SolutionPlotter.plotSolutionAsPNG(problem, bestSolution, "output/solution.png", "solution");
        
        new GraphStreamViewer(problem, bestSolution).labelWith(Label.ID).setRenderDelay(200).display();
    }

    //Added by Emil Pettersson to read coordinates.

    static int[][] readCoordinates(int coordLength,String path){
        String sCurrentLine;
        BufferedReader br = null;
        ArrayList<String> list = new ArrayList<String>();
        try{
            br = new BufferedReader(new FileReader(path));
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }

        try{
            while ((sCurrentLine = br.readLine()) != null) {
                list.add(sCurrentLine);
            }
        }catch(IOException e){
            e.printStackTrace();
        }

        int n = list.size();
        int[][] partMatrix = new int[n][2];
        for(int i = 0; i < n; i++){
            String line = list.get(i);
            String[] parts = line.split(",");
            String[] longitude = (parts[0]).split("\\.");
            String[] latitude = parts[1].split("\\.");
            parts[0] = longitude[0] + longitude[1];
            parts[1] = latitude[0] + latitude[1];
            //Make long enough
            while(parts[0].length() < coordLength){
                parts[0] = parts[0] + "0";
            }
            while(parts[1].length() < coordLength){
                parts[1] = parts[1] + "0";
            }

            //Make short enough
            parts[0] = parts[0].substring(0,coordLength);
            parts[1] = parts[1].substring(0,coordLength);
            partMatrix[i][0] = Integer.parseInt(parts[0]);
            partMatrix[i][1] = Integer.parseInt(parts[1]);
        }
    return partMatrix;

    }

}
