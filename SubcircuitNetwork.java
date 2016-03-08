
/*******************************************************************************
 * Copyright (C) 2015  Emil Pettersson
 *
 *This project uses the Java constraint solver JaCoP (http://jacop.osolpro.com)
 *Do not use this project without notifying the owners of JaCoP.
 *
 ******************************************************************************/
import org.jacop.constraints.*;
import org.jacop.core.*;
import org.jacop.search.*;
import java.io.*;
import java.util.ArrayList;
import org.jacop.floats.core.FloatVar;
import org.jacop.floats.constraints.*;
import org.jacop.floats.constraints.XeqP;
import org.jacop.floats.constraints.PmulCeqR;
import org.jacop.floats.constraints.XeqP;
import org.jacop.constraints.ElementInteger;
import org.jacop.constraints.netflow.NetworkBuilder;
import org.jacop.constraints.netflow.simplex.Node;
import org.jacop.constraints.netflow.NetworkFlow;



public class SubcircuitNetwork{	

	public static void main(String[] args) {
		System.out.println("running....");
		long T1, T2,T;
		T1 = System.currentTimeMillis();

		//Methods, in future might give input from here. Maybe use seperate function to read input data.
		findSolution();

		T2 = System.currentTimeMillis();
		T = T2 - T1;
		System.out.println("\n\t*** Execution time = " + T + " ms");
	}

	static void findSolution() {
		int ratioLimit = 1000; //Based upon Maximum load on a trip divided by minimum distance. Multiplied by 10 for a safety margin.
		int distanceLimit = 6000; //Based upon the distance between cities in Sweden. (Most northern to most southern and most western to most eastern). Unit of length is  
		//int[][] coordMatrix = readCoordinates(5,"/Users/Emil/Documents/exjobb/koordinater/testpunktercopy.txt");
		/* 
		Coordinates shall be on the form:
		Regular node
		Regular node
		Regular node
		Regular node
		Regular node
		Regular node
		DepotA source
		DepotB source
		DepotA sink 
		DepotB sink


		*/
		int[][] coordMatrix = readCoordinates(5,"/Users/Emil/Documents/exjobb/koordinater/testpunkter.txt");

		//Total number of cities
		int n = coordMatrix.length;
		int[][] distanceMatrix = distanceMatrix(coordMatrix);

		//Number of depots (or start locations if the number of end locations is greater than zero).
		//This will be the number of sources.
		int depotNbr = 1;


		//Number of regular cities.
		int regNbr = n - 2 * depotNbr;

		//Least amount of cities that must be visited
		int leastAmountOfCities = 2;

		//Volume of load in cities
		int[] volumes = new int[n];
		for(int i = 0; i < regNbr; i++){
			volumes[i] = 0;
		}

		//volumes[5] = 1;
		//volumes[6] = 0;

		//Number of vehicles at each depot. (translated into balance of source and sink)
		//IF MORE THAN ONE DEPOT: FORCE SUBCIRCUIT-MATRIX TO BE EQUAL TO TO THE RIGHT AMOUNT IN EACH DEPOT
		int[] vehicleNumbers = new int[depotNbr];
		vehicleNumbers[0] = 2;
		//vehicleNumbers[1] = 1;
		//vehicleNumbers[2] = 

		//Total number of vehicles
		int m = 0;
		for(int i = 0; i < vehicleNumbers.length; i++){
			m += vehicleNumbers[i];
		}

		Store store = new Store();


		//-----------------DEFINE THE STUFF FOR THE SUBCIRCUIT-ROUTES---------------------

		//Matrix to represent which cities have been visited (element = 1) and not visited (element = 0)
		IntVar[][] subCircuitMatrix = new IntVar[m][n];
		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){
				subCircuitMatrix[i][j] = new IntVar(store, "subcircuitmatrix#" + Integer.toString(i) + Integer.toString(j),0,1);
			}
		}

		//Store the circuit-arrays. Each row in matrix nextCities is a circuit-array.
		IntVar[][] nextCities = new IntVar[m][n];

		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){
				nextCities[i][j] = new IntVar(store,"city#" + Integer.toString(j+1),1,n);
			}
			//Construct the array for a specific vehicle
			IntVar[] nextCityArray = nextCities[i];
			//Impose Subcircuit-constraint on the array. 	
			store.impose(new Subcircuit(nextCityArray));
		}

		//Input for SelectChoicePoint
		IntVar[] varVector = new IntVar[m*n];
		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){
				varVector[i * n + j] = nextCities[i][j];
			}
		}

		//Link the nextCities elements to the subCircuitMatrix elements.
		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){
				IntVar nextCity = nextCities[i][j];
				PrimitiveConstraint ctr = new XneqC(nextCity,j+1); //Values of elements in nextCities-matrix start at value 1, not 0. What is interesting is whether v[i] = i or not (where v is a row in the nextCities-matrix).
				IntVar b = subCircuitMatrix[i][j];
				store.impose(new Reified(ctr,b));
			}
		}

		//Enforce a certain amount of visited cities per vehicle, so that optimization does not lead to zero cities being visited.
		for(int i = 0; i < m; i++){
			IntVar[] list = subCircuitMatrix[i];
			//Enforce a minimum amount of visited cites per vehicle
			int[] weights = new int[n];
			for(int k = 0; k < n; k++){
				weights[k] = 1;
			}
			PrimitiveConstraint lb = new LinearInt(store,list,weights,">=",leastAmountOfCities);
			store.impose(lb);
		}

		//Enforce that the sum of a column in the subCircuit-matrix is not greater than 1.
		//This translates to a city being visited by at most one vehicle.
		int[] columnWeights = new int[m];
		for(int k = 0; k < m; k++){
			columnWeights[k] = 1;
		}

		//Side constraint #5 and #6 demand changes here. Only the regular cities shall be affected by the "columnConstraint". "n" has been changed to "regNbr" in the for-loop
		for(int j = 0; j < regNbr; j++){
			IntVar[] column = new IntVar[m];
			for(int i = 0; i < m; i++){
				column[i] = subCircuitMatrix[i][j];
			}
			PrimitiveConstraint columnConstraint = new LinearInt(store,column,columnWeights,"<=",1);
			store.impose(columnConstraint);
		}

		//-----------------------------------LOAD------------------------------------------

		//Store the load for each vehicle.
		IntVar[] vehicleLoads = new IntVar[m];
		//Make a matrix where an element is the product of boolean whether a city is visited and that cities volume. Row: Vehicle, Column; city
		//Initialize
		IntVar[][] cityVisitedLoad = new IntVar[m][n];
		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){
				cityVisitedLoad[i][j] = new IntVar(store,"cityVisitedLoad#" + Integer.toString(i) +","+ Integer.toString(j), 0,100);
			}
		}
		//Make constraints
		for(int i =0; i < m; i++){
			for(int j = 0; j < n; j++){
				Constraint ctr = new XmulCeqZ(subCircuitMatrix[i][j],volumes[j], cityVisitedLoad[i][j]);
				store.impose(ctr); //!!!!????!!!!???!!!!
			}
		}

		//Volume for a certain vehicle
		for(int i = 0; i < m; i++){
			IntVar[] list = cityVisitedLoad[i];
			IntVar vehicleLoad = new IntVar(store,"vehicleLoad#" + Integer.toString(i),0,(100*n));  //Upper limit of vehicle load here.  (Defualt: 100% of each cities load, If there is a limit to how many cities could be visited then that value should replace n.)
			Constraint sumWeight = new SumInt(store,list,"==",vehicleLoad);
			store.impose(sumWeight);
			vehicleLoads[i] = vehicleLoad;
		}

		//Sum up the vehicles load to a total load.
		IntVar totalLoad = new IntVar(store,"totalLoad",0,(100*n));
		store.impose(new SumInt(store,vehicleLoads,"==",totalLoad));

		//----------------------------------------DISTANCE----------------------------------------------

		//Initialize a variable vector for distance
		IntVar[] varVectorDistance = new IntVar[m*n];

		//Calculate the distance traveled by the vehicles.
		IntVar[] vehicleDistanceArray = new IntVar[m];
		for(int i = 0; i < m; i++){
			IntVar[] cityDistanceArray = new IntVar[n];
			for(int j = 0; j < n; j++){
				IntVar nextCity = nextCities[i][j];
				int[] distanceRow = distanceMatrix[j]; //Distance to different cities for city j.
				
				IntVar cityDistance = new IntVar(store,"cityDistance",0,distanceLimit); 
				//Constraint ctr = new Element(nextCity,distanceRow,cityDistance,-1); 
				//Constraint ctr = new Element(nextCity,distanceRow,cityDistance); 
				Constraint ctr = new ElementInteger(nextCity,distanceRow,cityDistance,0); 
				store.impose(ctr);
				cityDistanceArray[j] = cityDistance;

				//Put the distances in a variable vector for if we want to use them for search.
				varVectorDistance[i * n + j] = cityDistance; 

			}
			IntVar vehicleDistance = new IntVar(store,"vehicleDistance",0,n * distanceLimit); 
			Constraint sumCtr = new SumInt(store,cityDistanceArray,"==",vehicleDistance);
			store.impose(sumCtr);
			vehicleDistanceArray[i] = vehicleDistance;
		}

		IntVar totalDistance = new IntVar(store,"totalDistance",0,m * n * distanceLimit);
		Constraint totalSumCtr = new SumInt(store,vehicleDistanceArray,"==",totalDistance);
		store.impose(totalSumCtr);

		//-------------------------DEPOT---------------------------------------------
		//Make sure that these correspond to the rules setup for source and sink in the network


		//Every vehicle must visit a the two cities that together represent a depot.
		
		//Force each vehicle to visit a sink (endCity).
		for(int i = 0; i < m; i++){
			IntVar[] depotSinks = new IntVar[depotNbr];
			int[] weights = new int[depotNbr];
			for(int j = 0; j < depotNbr; j++){
				depotSinks[j] = subCircuitMatrix[i][regNbr+depotNbr+j];
				weights[j] = 1;
			}
			PrimitiveConstraint depotConstraint = new LinearInt(store,depotSinks,weights,"=",1);
			store.impose(depotConstraint);
		}

		//Make sure that each sink points to it's corresponding source, or is not part of the route (points to itself)
		for(int i = 0; i < m; i++){
			for(int j = 0; j < depotNbr; j++){
				IntVar nextCity = nextCities[i][regNbr + depotNbr +j];
				nextCity.setDomain(regNbr + j + 1,regNbr + j + 1);
				nextCity.addDom(regNbr + depotNbr + j + 1, regNbr + depotNbr + j + 1);
			}
		}

		//Source must not point to other sources or to sinks.
		for(int i = 0; i < m; i++){
			for(int j = 0; j < depotNbr; j++){
				IntVar nextCity = nextCities[i][regNbr+j];
				nextCity.setDomain(1,regNbr);
				nextCity.addDom(regNbr+j+1,regNbr+j+1);
			}
		}

		//Regular cities must not point to sources.
		for(int i = 0; i < m; i++){
			for(int j = 0; j < regNbr; j++){
				IntVar nextCity = nextCities[i][j];
				nextCity.setDomain(1,regNbr);
				nextCity.addDom(regNbr + depotNbr + 1, n);
			}
		}









		//-------------------------NETWORK---------------------------------------------

		//Construct the NetworkBuilder.

		NetworkBuilder net = new NetworkBuilder();

		Node[] nodeArray = new Node[regNbr + 2 * depotNbr];

		//Construct the regular nodes.
		for(int i = 0; i < regNbr; i++){
			nodeArray[i] = net.addNode("Node#" + i,0);
		}

		//Construct the source and sinks.
		for(int i = 0; i < vehicleNumbers.length; i++){
			int b = vehicleNumbers[i];
			//System.out.println("Debug: " + b);
			nodeArray[regNbr+i] = net.addNode("Source#" + i,b);
			nodeArray[regNbr+depotNbr+i] = net.addNode("Sink#" + i,-b);
		}


		//Construct the arcs.

		//Construct IntVars for upper and lower capacity. These will also represent the flow over an arc.
		//Each row get an extra space to be used in LinearInt later on.
		IntVar[][] flowMatrix = new IntVar[n][n+1];
		for(int i = 0; i < n; i++){
			for(int j = 0; j < n; j++){
				boolean sameNode = (i != j); //
				boolean	sourceSink = !((i >= regNbr) && (j >= regNbr)); //
				boolean	sourceNoIn = !(j >= regNbr && j < regNbr+depotNbr); //No arcs into source
				boolean sinkNoOut =  !(i >= regNbr + depotNbr); // No arcs out of sink
				if(sameNode && sourceSink && sourceNoIn && sinkNoOut)flowMatrix[i][j] = new IntVar(store, i + "->" + j, 0,1);

				else flowMatrix[i][j] = new IntVar(store, i + "->" + j, 0,0);
			}
		}

		//Define the arcs
		int cost = 0;
		Node nodeA = null;
		Node nodeB = null;
		IntVar flowBounds = null;
		for(int i =0; i < n; i++){ 
			nodeA = nodeArray[i];
			//System.out.println("Debug: " + nodeA);
			for(int j = 0; j < n; j++){
				nodeB = nodeArray[j];
				//System.out.println("Debug: " + nodeB);
				boolean sameNode = (i != j); //
				boolean	sourceSink = !((i >= regNbr) && (j >= regNbr)); //
				boolean	sourceNoIn = !(j >= regNbr && j < regNbr+depotNbr); //No arcs into source
				boolean sinkNoOut =  !(i >= regNbr + depotNbr); // No arcs out of sink
				if(sameNode && sourceSink && sourceNoIn && sinkNoOut){ //Cannot go from a node to that node itself, and it shouldn't allow for source and sink to connect directly. Source shouldn't connect to source either and sink shouldn't connect to sink.
					cost = distanceMatrix[i][j];
					//System.out.println("Debug: " + cost);
					flowBounds = flowMatrix[i][j];
					//System.out.println("Debug: " + flowBounds);
					net.addArc(nodeA,nodeB,cost,flowBounds);
				}
			}
		}

		//Define a cost.
		//This should be the total cost-per-unit * flow over all arcs.
		//This translates into total distance traveled by all vehicles.
		IntVar totalDistanceFlow = new IntVar(store,"totalDistance",0,m*n*distanceLimit);
		net.setCostVariable(totalDistanceFlow);

		//Impose the network constraint
		Constraint networkFlow = new NetworkFlow(net);
		store.impose(networkFlow);

		//-------------------------------------------- LINK ROUTES AND NETWORK ---------------------------

		for(int i = 0; i < m; i++){
			for(int j = 0; j < regNbr + depotNbr; j++){
				for(int k = 0; k < n; k++){
					if(j != k){
						PrimitiveConstraint routeJtoK = new XeqC(nextCities[i][j],k+1);
						PrimitiveConstraint flowJtoK = new XeqC(flowMatrix[j][k],1);
						store.impose(new IfThen(routeJtoK,flowJtoK));
					}
				}
			}
		}

		//-------------------------------------SEARCH---------------------------------------------------------------
		
		//Optimize

		//Convert to Float for division
		FloatVar floatTotalLoad = new FloatVar(store,"floatTotalLoad",0,(100*n));
		Constraint convLoad = new XeqP(totalLoad, floatTotalLoad);
		store.impose(convLoad);

		
		FloatVar ratio = new FloatVar(store,"ratio",0,ratioLimit);
		FloatVar floatTotalDistance = new FloatVar(store,"floatTotalDistance",0,m * n * distanceLimit);
		//Convert to Float for division
		Constraint convertDist = new XeqP(totalDistance, floatTotalDistance);
		store.impose(convertDist); 
		Constraint division = new PdivQeqR(floatTotalLoad, floatTotalDistance, ratio);

		store.impose(division);

		//Maximizing x is to minimize (-x)
		FloatVar negRatio = new FloatVar(store,"negDivision", -ratioLimit,0);
		Constraint negationConstraint = new PmulCeqR(ratio,-1, negRatio);

		store.impose(negationConstraint);


		
		Search<IntVar> label = new DepthFirstSearch<IntVar>();

		SelectChoicePoint<IntVar> selectOne = new SimpleSelect<IntVar>(varVectorDistance,new MaxRegret<IntVar>(),new IndomainMin<IntVar>());




		boolean resultOne = label.labeling(store,selectOne,negRatio);
		


		if (resultOne) {
			System.out.println("\n*** Yes");
			System.out.println("Arrangment: "); 
			printIntVarMatrix(nextCities,m,n);
			printIntVarMatrix(subCircuitMatrix,m,n); 
			//printIntVarMatrix(cityVisitedLoad,m,n);
			System.out.println("Ratio: " + ratio);
			System.out.println("distance: " + totalDistance);
			System.out.println("load: " + totalLoad);
			printIntVarMatrix(flowMatrix,n,n);
			
		}
		else {
			System.out.println("\n*** No");
			printIntVarMatrix(nextCities,m,n); 
			printIntVarMatrix(flowMatrix,n,n);
		}

	}


	//------------------------------------------------------Other methods------------------------------------------------------------------------	


	static int findShortest(int endLocIndex,int indexOfFirstStartLoc,int nbrOfStartLocs,int[][] distanceMatrix){
		int shortest = 0;
		int shortestDistance = Integer.MAX_VALUE;
		for(int i = indexOfFirstStartLoc; i < (indexOfFirstStartLoc + nbrOfStartLocs); i++){
			if(distanceMatrix[endLocIndex][i] < shortestDistance){
				shortestDistance = distanceMatrix[endLocIndex][i];
				shortest = i;
			}
		}
		return shortest;

	}


	//Input: n x 2 matris.
	//Give input as ints. 
 	static int[][] distanceMatrix(int[][] input){
 		//Debug
 		int debug = 0;

		if(input[1].length != 2){
			System.out.print("Wrong dimension on distance matrix");
			System.exit(1);
		}
		int n = input.length;
		int distanceMatrix[][] = new int[n][n];
		int distance;  
		for(int i = 0; i < n; i++){
			int x1 = input[i][0]; 
			int y1 = input[i][1];

			for(int j = 0; j < n; j++){
				int x2 = input[j][0];
				int y2 = input[j][1];
				int deltaX = Math.abs(x1 - x2);
				int deltaY = Math.abs(y1 - y2);
				if(deltaX > 46340 || deltaY > 46340 || ((deltaX*deltaX) + (deltaY * deltaY))<0){ //overflow from deltaX * deltaX, deltaY * deltaY, sum of those products
					System.out.print("Overflow. To big distances between cities.");
					System.exit(1);
				}
				int squaredSum = (deltaX*deltaX) + (deltaY * deltaY);
				distance = Math.round(Math.round(Math.sqrt(squaredSum)));

				distanceMatrix[i][j] = distance;
			}


		}
		return distanceMatrix;
	}

	static void printIntVarMatrix(IntVar[][] matrix, int row, int column){
		for(int i = 0; i < row; i++){
			System.out.println("Vehicle #" + (i+1) + "  ---------------------------------------");
			for(int j = 0; j < column; j++){
				System.out.println(matrix[i][j]);
			}
			System.out.print("\n");
		}
	}

	static void printMatrix(int[][] matrix, int row, int column){
		for(int i = 0; i < row; i++){
			for(int j = 0; j < column; j++){
				System.out.printf("%-15s", matrix[i][j]);
			}
			System.out.print("\n");
		}
	}

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