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
		NetworkBuilder[] netArray = new NetworkBuilder[m];

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
		Node[][] nodeMatrix = new Node[m][regNbr + 2 * depotNbr];



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
				nodeMatrix[i][regNbr+j] = netArray[i].addNode("Source#" + i + j,b);
				nodeMatrix[i][regNbr+depotNbr+j] = netArray[i].addNode("Sink#" + i + j,-b);
			}
		}



		//Construct the arcs.

		//Construct IntVars for upper and lower capacity. These will also represent the flow over an arc.
		//Each row get an extra space to be used in LinearInt later on.
		IntVar[][][] flowMatrix = new IntVar[m][n][n+1];
		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){
				for(int k = 0; k < n; k++){
					boolean sameNode = (j != k); //
					boolean	sourceSink = !((j >= regNbr) && (k >= regNbr)); //
					boolean	sourceNoIn = !(k >= regNbr && k < regNbr+depotNbr); //No arcs into source
					boolean sinkNoOut =  !(j >= regNbr + depotNbr); // No arcs out of sink
					if(sameNode && sourceSink && sourceNoIn && sinkNoOut)flowMatrix[i][j][k] = new IntVar(store, j + "->" + k, 0,1);

					else flowMatrix[i][j][k] = new IntVar(store, j + "->" + k, 0,0);
				}
			}
		}	

		//Define the arcs
		int cost = 0;
		Node nodeA = null;
		Node nodeB = null;
		IntVar flowBounds = null;
		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){ 
				nodeA = nodeMatrix[i][j];
				//System.out.println("Debug: " + nodeA);
				for(int k = 0; k < n; k++){
					nodeB = nodeMatrix[i][k];
					//System.out.println("Debug: " + nodeB);
					boolean sameNode = (j != k); //
					boolean	sourceSink = !((j >= regNbr) && (k >= regNbr)); //
					boolean	sourceNoIn = !(k >= regNbr && k < regNbr+depotNbr); //No arcs into source
					boolean sinkNoOut =  !(j >= regNbr + depotNbr); // No arcs out of sink
					if(sameNode && sourceSink && sourceNoIn && sinkNoOut){ //Cannot go from a node to that node itself, and it shouldn't allow for source and sink to connect directly. Source shouldn't connect to source either and sink shouldn't connect to sink.
						cost = distanceMatrix[j][k];
						//System.out.println("Debug: " + cost);
						flowBounds = flowMatrix[i][j][k];
						//System.out.println("Debug: " + flowBounds);
						netArray[i].addArc(nodeA,nodeB,cost,flowBounds);
					}
				}
			}
		}

		//Impose network constraint and 
		//Define a cost.
		IntVar[] vehicleDistanceArray = new IntVar[m];
		for(int i = 0; i < m; i++){
			vehicleDistanceArray[i] = new IntVar(store,"vehicleDistance",0,n * distanceLimit);
			netArray[i].setCostVariable(vehicleDistanceArray[i]);
			Constraint networkFlow = new NetworkFlow(netArray[i]);
			store.impose(networkFlow);

		}
		IntVar totalDistance = new IntVar(store,"totalDistance",0,m*n*distanceLimit);
		Constraint totalSumCtr = new SumInt(store,vehicleDistanceArray,"==",totalDistance);
		store.impose(totalSumCtr);



		


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
		IntVar[][] sums = new IntVar[m][n+1];
		for(int i = 0; i < m ; i ++){
			for(int j = 0; j < n; j++){	
				IntVar sumVar = new IntVar(store,"nodeSumVariable#" + i + j,0,n * m); //Sum cant be bigger than number of cities * number of vehicles.
				terms = flowMatrix[i][j];
				terms[n] = sumVar;
				Constraint flowSumNode = new LinearInt(store,terms,weights,"==",0);
				store.impose(flowSumNode);
				sums[i][j] = sumVar;
			}
		}

		//Here the constraint for least amount of cities is for each individual vehicle instead of them all together.
		for(int i = 0; i < m; i++){
			IntVar flowSum = new IntVar(store,"flowSum" + i,0,n * m);
			sums[i][n] = flowSum;
			Constraint flowSumTotal = new LinearInt(store,sums[i],weights,"==",0);
			store.impose(flowSumTotal);

			//Force flowSumTotal to have a minimum value. That is a certain amount of travels must be made by the vehicles.

			Constraint leastAmount = new XgteqC(flowSum,leastAmountOfCities);
			store.impose(leastAmount);
		}

		

		
		//If a vehicle travels from i --> j it shall not be allowed to travel from j --> i since this would
		//lead to flow being "used up" to no effect. It causes discrepancy between flow graph and representation of vehicles.
		// Assumptions: #1 Number of cities >> Number of Vehicles (otherwise it would start getting weird that the vehicles have no where to go but are forced, but are not allowed to visit the same cities. More of a general assumption than specifically for this constraint.)
		//              #2 Triangle indifference applies
		//This will produce redundant constraints since it will setup the same rule for a pair of nodes two times each.

		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){
				for(int k = 0; k < n; k++){
					IntVar jTOk = flowMatrix[i][j][k];
					IntVar kTOj = flowMatrix[i][k][j];
					IntVar[] v = {kTOj,jTOk};
					Constraint notBoth = new LinearInt(store,v,new int[] {1,1},"<=",1);
					store.impose(notBoth);
				}	
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
			//After sink there should be source
			for(int j = 0; j < depotNbr; j++){
				IntVar nextCity = nextCities[i][regNbr + depotNbr + j];
				//Since depots are pairs of sources and sinks.
				nextCity.setDomain(regNbr+j+1,regNbr+j+1);
				nextCity.addDom(regNbr + depotNbr + j + 1,regNbr + depotNbr + j + 1);
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
			for(int j = 0; j < n; j++){
				subCircuitMatrix[i][j] = new IntVar(store, "subcircuitmatrix#" + Integer.toString(i) + Integer.toString(j),0,1);
			}
		}




		//Link the nextCities-matrix elements to the subCircuitMatrix elements.
		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){
				IntVar nextCity = nextCities[i][j];
				PrimitiveConstraint ctr = new XneqC(nextCity,j+1);

				IntVar b = subCircuitMatrix[i][j];
				store.impose(new Reified(ctr,b));
			}
		}

		/*
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
		} */

		//Since each vehicle has a different flowMatrix the balance of nodes will not interact between vehicles. Therefore it must be made so that 
		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){
				for(int k = 0; k < n; k++){
					IntVar flow = flowMatrix[i][j][k];
					PrimitiveConstraint positiveFlow = new XgtC(flow,0);
					PrimitiveConstraint nextCityCtr = new XeqC(nextCities[i][j],k+1);  //Note that the indices here change from the other implementation. 
					PrimitiveConstraint ifThenCtr = new IfThen(positiveFlow,nextCityCtr);
					store.impose(ifThenCtr);
				}
			}
		}
		
		//Make sure that each non-depot is visited at most once. Directly lifted from first (non-flow) implementation.

		int[] columnWeights = new int[m];
		for(int k = 0; k < m; k++){
			columnWeights[k] = 1;
		}

		for(int j = 0; j < regNbr; j++){
			IntVar[] column = new IntVar[m];
			for(int i = 0; i < m; i++){
				column[i] = subCircuitMatrix[i][j];
			}
			PrimitiveConstraint columnConstraint = new LinearInt(store,column,columnWeights,"<=",1);
			store.impose(columnConstraint);
		}

		//To be able to use distance variables for side constraints they are calculated.

		//Calculate the distance traveled by the vehicles.
		IntVar[] vehicleDistanceArrayExtra = new IntVar[m];
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


			}
			IntVar vehicleDistance = new IntVar(store,"vehicleDistance",0,n * distanceLimit); 
			Constraint sumCtr = new SumInt(store,cityDistanceArray,"==",vehicleDistance);
			store.impose(sumCtr);
			vehicleDistanceArrayExtra[i] = vehicleDistance;
		}

		IntVar totalDistancePrintable = new IntVar(store,"totalDistancePrintable",0,m * n * distanceLimit);
		Constraint totalSumCtrExtra = new SumInt(store,vehicleDistanceArrayExtra,"==",totalDistancePrintable);
		store.impose(totalSumCtrExtra);

		
		
		
		
		







		//Loads. Just lift the load stuff from the first (non-flow) implementation.

		//Store the load for each vehicle.
		IntVar[] vehicleLoads = new IntVar[m];
		//Make a matrix where an element is the product of boolean whether a city is visited and that cities volume. Row: Vehicle, Column; city
		//Initialize
		IntVar[][] cityVisitedLoad = new IntVar[m][n];
		for(int i = 0;i < m; i++){
			for(int j = 0; j < n; j++){
				cityVisitedLoad[i][j] = new IntVar(store,"cityVisitedLoad#" + Integer.toString(i) +","+ Integer.toString(j), 0,100);
			}
		}
		//Make constraints
		for(int i =0; i < m; i++){
			for(int j = 0; j < n; j++){
				Constraint ctr = new XmulCeqZ(subCircuitMatrix[i][j],volumes[j], cityVisitedLoad[i][j]);
				store.impose(ctr);
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




		//-------------------SIDE CONSTRAINTS--------------------------

		//Side constraint #1: How often a city must be visited at minimum.
		//Take notice of the city indexes!

		//Data

		ArrayList<Integer> visitDueToTime = new ArrayList<Integer>();
		//Test (stadindex 3 = stad nr 4 ty stadindex 0 är första staden)
		//visitDueToTime.add(0);
		//visitDueToTime.add(1);
		//visitDueToTime.add(8);
		//visitDueToTime.add(9);
		//Add cities that need to be visited to this list.

		//Constraint

		//Sum of flow to a certain node for a certain vehicle.
	
		
		for(int city: visitDueToTime){
			IntVar[] nodeSums = new IntVar[m];
			IntVar vehicleSum = new IntVar(store,"vehicleFlow",0,n*m);
			for(int i = 0; i < m; i++){
				IntVar[] flows = new IntVar[n];
				IntVar flowNodeSum = new IntVar(store,"flowNodeSum",0,n*m);
				for(int j = 0; j < n; j++){
					flows[j] = flowMatrix[i][j][city];	
				}
				store.impose(new SumInt(store,flows,"==",flowNodeSum));
				nodeSums[i] = flowNodeSum; 
			}
			store.impose(new SumInt(store,nodeSums,"==",vehicleSum));
			store.impose(new XeqC(vehicleSum,1));
		}	

		//Side constraint #2: How long a route can be in units of length.
		//Data
		//int hardDistanceLimit = 2;

		//Constraint

		//Ganska reduntant constraint, optimering kommer försöka hitta kortaste möjliga ändå. Det enda denna skule kunna göra
		//är att meddela att man bör minska antalet påtvingade besökta städer per rutt.
		/*
		for(IntVar vehicleDistance : vehicleDistanceArray){
			PrimitiveConstraint distCtr = new XltC(vehicleDistance,hardDistanceLimit);
			store.impose(distCtr);
		}*/
		

		//Side constraint #3 
		//Data
		int mostAmountofcities = n;

		//Constraintt How many cities a certain vehicle route can contain at most.
		for(int i = 0; i < m; i++){
			IntVar[] list = subCircuitMatrix[i];
			//Enforce a minimum amount of visited cites per vehicle, or a range (lower bound, upper bound)
			int[] weightsC = new int[n];
			for(int k = 0; k < n; k++){
				weightsC[k] = 1;
			}
			PrimitiveConstraint ub = new LinearInt(store,list,weights,"<=",mostAmountofcities);
			store.impose(ub);
		}		
		
		//Data
		//For each city declare an upper limit of how full it's volume is allowed to be. Represented with a list.
		int[] maxVolumes = new int[n];

		//Default: no restriction.
		for(int j = 0; j < n; j++){
			maxVolumes[j] = 101;
		}
		maxVolumes[0] = 1;

		//Constraint
		//If the volume is greater than the maxVolume for some city, then that city must be visited by a vehicle.
		for(int j = 0; j < regNbr; j++){
			if(volumes[j] >= maxVolumes[j]){
				//System.out.println("Debug maxVolume:" + j + "     volume:" + volumes[j] + "    maxvolume:   " + maxVolumes[j]);
				IntVar[] vehicles = new IntVar[m];
				int[] weightsD = new int[m];
				for(int i = 0; i < m ; i++){
					vehicles[i] = subCircuitMatrix[i][j];
					weightsD[i] = 1; 
				}	
				PrimitiveConstraint volCtr = new LinearInt(store,vehicles,weightsD,"=",1);
				store.impose(volCtr);
			}
		}
		


		



		//--------------------------SEARCH----------------------------

		//Construct an array for the variables.
		//Bygg om till lista om i==j lägg inte till i listan. Gör sedan om listan till vektor.
		ArrayList<IntVar> dummyList = new ArrayList<IntVar>();

		
		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){
				for(int k = 0; k < n; k++){
					//System.out.print("Debug: " + i + j);
					dummyList.add(flowMatrix[i][j][k]);
				}
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
			printIntVarMatrix(nextCities,m,n);

			printArray(vehicleDistanceArrayExtra);

			
			System.out.println("distance: " + totalDistance);
			System.out.println("--------DEBUG--------");
			System.out.println("subCircuitMatrix");
			printIntVarMatrix(subCircuitMatrix,m,n);
			
		}else {
			System.out.println("\n*** No");
			printIntVarMatrix(nextCities,m,n);
			
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