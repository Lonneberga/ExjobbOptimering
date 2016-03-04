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
import org.jacop.constraints.netflow.NetworkBuilder;
import org.jacop.constraints.netflow.simplex.Node;
import org.jacop.constraints.netflow.NetworkFlow;


public class NetworkSolutionMultipleNetworks{

	public static void main(String[] args){
		System.out.println("Running...");
		//Keep check of the execution time of the program.
		long T1,T2,T;
		T1 = System.currentTimeMillis();

		//Run program
		findSolution();

		T2 = System.currentTimeMillis();
		T = T2 - T1;
		System.out.println("\n\t*** Execution time = " + T +" ms.");

	}

	static void findSolution(){
		//-----------GIVEN DATA AND VALUES-----------------------



		//Read coordinates.
		int[][] coordMatrix = readCoordinates(5,"/Users/Emil/Documents/exjobb/koordinater/testpunkter.txt");

		//number of cities
		int n = coordMatrix.length;
		int[][] distanceMatrix = distanceMatrix(coordMatrix);

		//Number of depots (or start locations if the number of end locations is greater than zero).
		//This will be the number of sources.
		int depotNbr = 1;


		//Number of regular cities.
		int regNbr = n - 2 * depotNbr;

		//Least amount of cities that must be visited
		int leastAmountOfCities = 5;

		//Volume of load in cities
		int[] volumes = new int[n];
		for(int i = 0; i < regNbr; i++){
			volumes[i] = 100;
		}

		//Number of vehicles at each depot. (translated into balance of source and sink)
		int[] vehicleNumbers = new int[depotNbr];
		vehicleNumbers[0] = 1;
		//vehicleNumbers[1] =
		//vehicleNumbers[2] = 
		

		//Total number of vehicles
		int m = 0;
		for(int i = 0; i < vehicleNumbers.length; i++){
			m += vehicleNumbers[i];
		}

		//Other stuff
		int ratioLimit = 1000;
		int distanceLimit = 6000;



		//Declare the store

		Store store = new Store();

		//---------------------DEFINE A NETWORK-------------------------

		//Construct the NetworkBuilder-array.
		NetworkBuilder[] netArray = new NetworkBuilder[];

		for(int i = 0; i < m; i++){
			netArray[i] = new NetworkBuilder();
		}
		


		/*Construct an array to store the nodes on the form
		Node
		Node
		Node
		Node
		Node
		Node
		Source
		Source
		Sink
		Sink


		*/
		Node[][] nodeMatrix = new Node[regNbr + 2 * depotNbr][m];



		//Construct the regular nodes.
		for(int i = 0; i < m; i++){
			for(int j = 0; j < regNbr; j++){
				nodeMatrix[i][j] = netArray[i].addNode("Node#" + i + j,0);
			}
		}

		//Construct the source and sinks.
		for(int i = 0; i < m; i++){
			for(int j = 0; j < vehicleNumbers.length; j++){
				int b = vehicleNumbers[j];
				nodeMatrix[regNbr+j,i] = net.addNode("Source#" + i,b);
				nodeMatrix[regNbr+depotNbr+j,i] = netArray[i].addNode("Sink#" + i,-b);
			}
		}



		//Construct the arcs.

		//Construct IntVars for upper and lower capacity. These will also represent the flow over an arc.
		//Each row get an extra space to be used in LinearInt later on.
		IntVar[][][] flowMatrix = new IntVar[n][n+1][m];
		for(int = 0; k < m; k++){
			for(int i = 0; i < n; i++){
				for(int j = 0; j < n; j++){
					boolean sameNode = (i != j); //
					boolean	sourceSink = !((i >= regNbr) && (j >= regNbr)); //
					boolean	sourceNoIn = !(j >= regNbr && j < regNbr+depotNbr); //No arcs into source
					boolean sinkNoOut =  !(i >= regNbr + depotNbr); // No arcs out of sink
					if(sameNode && sourceSink && sourceNoIn && sinkNoOut)flowMatrix[i][j][k] = new IntVar(store, i + "->" + j, 0,1);

					else flowMatrix[i][j][k] = new IntVar(store, i + "->" + j, 0,0);
				}
			}
		}	

		//Define the arcs
		int cost = 0;
		Node nodeA = null;
		Node nodeB = null;
		IntVar flowBounds = null;
		for{int k = 0; k < m; k++}
			for(int i = 0; i < n; i++){ 
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
						flowBounds = flowMatrix[i][j][k];
						//System.out.println("Debug: " + flowBounds);
						net.addArc(nodeA,nodeB,cost,flowBounds);
					}
				}
			}
		}


		//Define a cost.
		//This should be the total cost-per-unit * flow over all arcs.
		//This translates into total distance traveled by all vehicles.
		IntVar totalDistance = new IntVar(store,"totalDistance",0,m*n*distanceLimit);
		net.setCostVariable(totalDistance);



		//Impose the network constraint
		Constraint networkFlow = new NetworkFlow(net);
		store.impose(networkFlow);


		//------------------------------CONSTRAINTS-------------------------

		//Force a certain number of cities to be visited.
		//Translates into a certain number of arcs having positive flow.

		//Define weights.
		int[] weights = new int[n+1];
		for(int i = 0; i < n; i++){
			weights[i] = 1;
		}
		weights[n] = -1;


		IntVar[] terms = new IntVar[n+1];
		IntVar[] sums = new IntVar[n+1];
		for(int i = 0; i < n; i++){	
			IntVar sumVar = new IntVar(store,"nodeSumVariable#" + i,0,n * m); //Sum cant be bigger than number of cities * number of vehicles.
			terms = flowMatrix[i];
			terms[n] = sumVar;
			Constraint flowSumNode = new LinearInt(store,terms,weights,"==",0);
			store.impose(flowSumNode);
			sums[i] = sumVar;
		}

		IntVar flowSum = new IntVar(store,"flowSum",0,n * m);
		sums[n] = flowSum;
		Constraint flowSumTotal = new LinearInt(store,sums,weights,"==",0);
		store.impose(flowSumTotal);

		//Force flowSumTotal to have a minimum value. That is a certain amount of travels must be made by the vehicles.

		Constraint leastAmount = new XgteqC(flowSum,leastAmountOfCities);
		store.impose(leastAmount);


		

		
		//If a vehicle travels from i --> j it shall not be allowed to travel from j --> i since this would
		//lead to flow being "used up" to no effect. It causes discrepancy between flow graph and representation of vehicles.
		// Assumptions: #1 Number of cities >> Number of Vehicles (otherwise it would start getting weird that the vehicles have no where to go but are forced, but are not allowed to visit the same cities. More of a general assumption than specifically for this constraint.)
		//              #2 Triangle indifference applies
		//This will produce redundant constraints since it will setup the same rule for a pair of nodes two times each.

		for(int i = 0; i < n; i++){
			for(int j = 0; j < n; j++){
				IntVar iTOj = flowMatrix[i][j];
				IntVar jTOi = flowMatrix[j][i];
				IntVar[] v = {jTOi,iTOj};
				Constraint notBoth = new LinearInt(store,v,new int[] {1,1},"<=",1);
				store.impose(notBoth);
			}	
		}

		//Force nodes to be connected to source and sink to avoid subcircuits.

		//Construct an array to keep track of wether a node is connected to a source or an node that is connected to a source and son on....

		

		//Define a matrix for which a row is the route of a vehicle. Also enforce a subCircuitConstraint on them.
		IntVar[][] nextCities = new IntVar[m][n];
		for(int i = 0; i < m; i++){
			for(int j = 0; j <n; j++){
				nextCities[i][j] = new IntVar(store,"nextCity#" + i + (j+1),1,n);
			}
			IntVar[] nextCityArray = nextCities[i];
			store.impose(new Subcircuit(nextCityArray));
		}
		//Defina matrix to keep record of weather a city is visited by a vehicle.
		//NOTE NOTE NOTE: This matrix is probably not directly needed except maybe for convenience for side constraints.
		//If I use it though I can lift some side constraints directly from the first implementation straight into this one.
		//Then I might discuss the ease of doing so in my report.
		IntVar[][] subCircuitMatrix = new IntVar[m][n];
		for(int i = 0; i < m; i++){
			for(int j = 0; j <n; j++){
				subCircuitMatrix[i][j] = new IntVar(store, "subcircuitmatrix#" + Integer.toString(i) + Integer.toString(j),0,1);
			}
		}

		//Link the nextCities-matrix elements to the subCircuitMatrix elements.
		for(int i = 0; i < m; i++){
			for(int j = 0; j < m; j++){
				IntVar nextCity = nextCities[i][j];
				PrimitiveConstraint ctr = new XneqC(nextCity,j+1);
				IntVar b = subCircuitMatrix[i][j];
				store.impose(new Reified(ctr,b));
			}
		}

		//Define an array to store m constraints that should obey to  c1 ∨ c2 ∨ · · · ∨ cn (disjunction)
		PrimitiveConstraint[][] orMatrix = new PrimitiveConstraint[m][n];

		//Find arcs with flow over them and link that to the nextCities-matrix and the or-Matrix.
		for(int i = 0; i < n; i++){
			for(int j = 0; j < n; j++){
				IntVar flow =  flowMatrix[i][j];
				PrimitiveConstraint positiveFlow = new XgtC(flow,0);
				//FÅ RÄTT INDEX PÅ NEXTCITIES
				for(int k = 0; k < m; k++){
					//nextCity, k is the vehicle, i is the city to go from, j+1 is the city to go to. 
					PrimitiveConstraint nextCityCtr = new XeqC(nextCities[k][i],j+1);
					orMatrix[k][j] = nextCityCtr; //I must allow this assignment to be "overwritten" for each lap in the inner for-loop. In case of a node having flow to multiple nodes.
					//For the same reason the constraints in the active column of the orMatrix must be imposed before the end of each lop of the inner for-loop.
				}

				PrimitiveConstraint[] orArray = new PrimitiveConstraint[m];
				for(int k = 0; k < m; k++){
					orArray[k] = orMatrix[k][j]; //I think this is the proper way to do it. MatLab is easier :P ??? [k][i] or [k][j]
				}

				PrimitiveConstraint orCtr = orArray[0]; //If there is only one vehicle, then there is no need for a disjunction
				if(m > 1){
					orCtr =  new Or(orArray); //Maybe this doesn't work if there is only one vehicle. Probably not.
				}
				
				PrimitiveConstraint ifThenCtr = new IfThen(positiveFlow,orCtr);

				store.impose(ifThenCtr);
			}
		} 

		
		
		
		
		
		







		//Loads

		//Use the array sums from earlier (doing least amount of cities stuff).
		//If sum[i] != 0, then that city has been visisted.
		IntVar[] visitedArray = new IntVar[regNbr]; 
		for(int i = 0; i < regNbr; i++){
			visitedArray[i] = new IntVar(store, "visitedArray#" + i,0,1);
		}


		for(int i = 0; i < regNbr; i++){
			IntVar b = visitedArray[i];
			PrimitiveConstraint ctr = new XgtC(sums[i],0);
			store.impose(new Reified(ctr,b));
		} 

		//Make array with loads for visited cities.
		IntVar[] visitedLoads = new IntVar[regNbr+1];
		for(int i = 0; i < regNbr; i++){
			visitedLoads[i] = new IntVar(store, "visitedLoads#" + i, 0,100);
		}

		for(int i = 0; i < regNbr; i++){
			Constraint ctr = new XmulCeqZ(visitedArray[i],volumes[i],visitedLoads[i]);
			store.impose(ctr);
		}

		int[] loadWeights = new int[regNbr+1];
		for(int i = 0; i < regNbr; i++){
			loadWeights[i] = 1;
		}
		loadWeights[regNbr] = -1;

		//Sum up the loads.
		IntVar totalLoad = new IntVar(store, "totalLoad", 0, 100*regNbr);
		visitedLoads[regNbr] = totalLoad;
		Constraint sumTotalLoad = new LinearInt(store,visitedLoads,loadWeights,"==",0);
		store.impose(sumTotalLoad);



		//-------------------SIDE CONSTRAINTS--------------------------

		//Side constraint #1: How often a city must be visited at minimum.
		//Take notice of the city indexes!

		//Data

		ArrayList<Integer> visitDueToTime = new ArrayList<Integer>();
		//Test (stadindex 3 = stad nr 4 ty stadindex 0 är första staden)
		visitDueToTime.add(0);
		//visitDueToTime.add(7);
		//visitDueToTime.add(8);
		//visitDueToTime.add(9);
		//Add cities that need to be visited to this list.

		//Constraint
		for(int city : visitDueToTime){
			IntVar[] flowToNode = new IntVar[n];
			int[] weightsA = new int[n];
			for(int i = 0; i < n; i++){
				flowToNode[i] = flowMatrix[city][i];
				weightsA[i] = 1;
			}
			PrimitiveConstraint visitCtr = new LinearInt(store,flowToNode,weightsA,">",0);
			store.impose(visitCtr);
		}



		//--------------------------SEARCH----------------------------

		//Construct an array for the variables.
		//Bygg om till lista om i==j lägg inte till i listan. Gör sedan om listan till vektor.
		ArrayList<IntVar> dummyList = new ArrayList<IntVar>();

		
		for(int i = 0; i < n; i++){
			for(int j = 0; j < n; j++){
					//System.out.print("Debug: " + i + j);
					dummyList.add(flowMatrix[i][j]);
			}
		}
		IntVar[] varVector = dummyList.toArray(new IntVar[dummyList.size()]);
		//System.out.print("Debug: " + dummyList.size());

		//Diagonalen är null eftersom jag inte ger värden för nod till samma nod.

		/*
		"Note that the NetworkFlow only ensures that cost z(x) ≤ Z
		max, where z(x) is the
		total cost of the flow (see equation (3.6)). In our code it is defined as variable cost." - JaCoP User's Guide
		*/


		Search<IntVar> label = new DepthFirstSearch<IntVar>();
		SelectChoicePoint<IntVar> select = new SimpleSelect<IntVar>(varVector,new MaxRegret<IntVar>(),new IndomainMin<IntVar>());
		boolean result = label.labeling(store,select);

		if (result) {
			System.out.println("\n*** YES! SOLUTION FOUND!!!!");
			System.out.println("Arrangment: "); 
			printIntVarMatrix(flowMatrix,n,n);
			
			System.out.println("distance: " + totalDistance);
			
			
		}else {
			System.out.println("\n*** No");
			printIntVarMatrix(flowMatrix,n,n);
			
		}





		



		

		
		






	}

	static void printArray(IntVar[] array){
		for(int i = 0; i < array.length; i++){
			System.out.println(array[i]);
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
			System.out.println("Node #" + (i+1) + "  ---------------------------------------");
			for(int j = 0; j < column; j++){
				System.out.println(matrix[i][j]);
			}
			System.out.print("\n");
		}
	}
}