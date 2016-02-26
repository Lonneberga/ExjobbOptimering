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

public class NetworkSolution{

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
		int leastAmountOfCities = 1;

		//Volume of load in cities
		int[] volumes = new int[n];
		for(int i = 0; i < n; i++){
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

		//Construct the NetworkBuilder.

		NetworkBuilder net = new NetworkBuilder();


		/*Construct an array to store the nodes on the form
		Node
		Node
		Node
		Source
		Source
		Sink
		Sink


		*/
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

		//Construc IntVars for upper and lower capacity. These will also represent the flow over an arc.
		IntVar[][] flowMatrix = new IntVar[n][n];
		for(int i = 0; i < n; i++){
			for(int j = 0; j < n; j++){
				flowMatrix[i][j] = new IntVar(store, i + "->" + j, 0,1);
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
				if(i != j){ //Cannot go from a node to that node itself
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
		IntVar totalDistance = new IntVar(store,"totalDistance",0,m*n*distanceLimit);
		net.setCostVariable(totalDistance);

		//Impose the network constraint
		Constraint networkFlow = new NetworkFlow(net);
		store.impose(networkFlow);


		//------------------------------CONSTRAINTS-------------------------


		//--------------------------SEARCH----------------------------

		//Construct an array for the variables.
		//Bygg om till lista om i==j lägg inte till i listan. Gör sedan om listan till vektor.
		ArrayList<IntVar> dummyList = new ArrayList<IntVar>();

		
		for(int i = 0; i < n; i++){
			for(int j = 0; j < n; j++){
				if(i != j){
					System.out.print("Debug: " + i + j);
					dummyList.add(flowMatrix[i][j]);
				}
			}
		}
		IntVar[] varVector = dummyList.toArray(new IntVar[dummyList.size()]);
		System.out.print("Debug: " + dummyList.size());

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
			System.out.println("\n*** Yes");
			System.out.println("Arrangment: "); 
			//printIntVarMatrix(flowMatrix,n,n); 
			//System.out.println("Ratio: " + ratio);
			//System.out.println("distance: " + totalDistance);
			//System.out.println("load: " + totalLoad);
			
		}else {
			System.out.println("\n*** No");
			//printIntVarMatrix(flowMatrix,n,n);
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
			System.out.println("Vehicle #" + (i+1) + "  ---------------------------------------");
			for(int j = 0; j < column; j++){
				System.out.println(matrix[i][j]);
			}
			System.out.print("\n");
		}
	}
}