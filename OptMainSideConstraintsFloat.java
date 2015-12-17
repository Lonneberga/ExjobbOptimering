
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


public class OptMainSideConstraintsFloat{


	public static void main(String[] args) {
		System.out.println("running....");
		long T1, T2,T;
		T1 = System.currentTimeMillis();

		//Methods, in future might give input from here. Maybe use seperate function to read input data.
		optimize();

		T2 = System.currentTimeMillis();
		T = T2 - T1;
		System.out.println("\n\t*** Execution time = " + T + " ms");
	}

	static void optimize() {
		int ratioLimit = 1000; //Based upon Maximum load on a trip divided by minimum distance. Multiplied by 10 for a safety margin.
		int distanceLimit = 6000; //Based upon the distance between cities in Sweden. (Most northern to most southern and most western to most eastern). Unit of length is  
		//int[][] coordMatrix = readCoordinates(5,"/Users/Emil/Documents/exjobb/koordinater/testpunktercopy.txt");
		int[][] coordMatrix = readCoordinates(5,"/Users/Emil/Documents/exjobb/koordinater/final.txt");

		//Total number of cities
		int n = coordMatrix.length;
		int[][] distanceMatrix = distanceMatrix(coordMatrix);
		printMatrix(distanceMatrix,n,n);
		
		//Since JaCoP will optimize the route as well as it can it must be hindered to take a shortest route 
		//that for example contains 0 cities.
		//Least amount of visited cities for a certain vehicle. 
		int leastAmountOfCities = 5;

		//Volume of load in cities.			
		int[] volumes  = new int[n]; //Need to be Integers for sumWeight-constraint. Count as parts of a hundred.
		for(int i = 0; i < n; i++){
			volumes[i] = 100;
		}


		//Number of vehicles
		int m = 3; 

		//Declare a store to put constraints into.
		Store store = new Store();




		//Data for side constraints.----------------------------------------------------------------------------------------

		//Side constraint #1: How often a city must be visited at minimum.

		/*Comment: I imagine that this would be dependent on querying a database to check for the latest date the city was visited.
		this date is then compared to the current date. If the difference is larger than a set period of time the city must be visited.
		I will represent this with a list of cities that need to be visited. If it's empty then no citys have to be visited due to time.*/ 

		//Take notice of the city indexes!
		ArrayList<Integer> visitDueToTime = new ArrayList<Integer>();
		//Test (stadindex 3 = stad nr 4 ty stadindex 0 är första staden)
		//visitDueToTime.add(3);
		//visitDueToTime.add(7);
		//visitDueToTime.add(8);
		//visitDueToTime.add(9);
		//Add cities that need to be visited to this list.

		//Side constraint #2: How long a route can be in units of length.
		int hardDistanceLimit = 2;

		//Side constraint #3: How many cities a certain vehicle route can contain at most.
		int mostAmountofcities = n; //n by default.

		/*Comment: This will probably most of the time be an unnecessary constraint as the optimization will almost always keep the
		number of visited cities equal to the lowest number allowed */


		//Side constraint #4: How full a cities volume can be at most.

		/*Comment: The input for this is given as a float, but it will have to be rounded of to parts of hundreds (or thousands or...) and 
		be represented as an Integer.*/

		//For each city declare an upper limit of how full it's volume is allowed to be. Represented with a list.
		int[] maxVolumes = new int[n];

		//Default: no restriction.
		for(int j = 0; j < n; j++){
			maxVolumes[j] = 101;
		}

		//Test
		//maxVolumes[3] = 50;

		/*Depending on the definition of the side constraint (Do the limit mean that the city ABSOLUTELY SHALL NEVER have bigger volume than
		the limit OR does it mean if the limit is broken the city should be visited as soon as possible?) one might want to have a variable
		for a buffer*/
		int buffer = 0;	

		//Side constraint #5: Enforce a specific "special-city" where vehicles start from/or end up. Does not contain a load.


		int startNbr = 1;


		/*Comment: It's effectively the same thing to enforce only start city or only end city.*/

		//Side constraint #6: Enforce two specific "special-cities" one where vehicles start from and another where they end up. Does not contain a load.
		int endNbr = 0;
		







		//------------------------------------------------------------------------------------------------------------------

		//Number of regular cities
		int regNbr = n - startNbr - endNbr;

		//Fix volume in start- and end locations.
		for(int i = regNbr; i < n; i++){
			volumes[i] = 0;
		}

		//Matrix to represent which cities have been visited (element = 1) and not visited (element = 0)
		IntVar[][] subCircuitMatrix = new IntVar[m][n];
		for(int i = 0; i < m; i++){
			for(int j = 0; j < n; j++){
				subCircuitMatrix[i][j] = new IntVar(store, "subcircuitmatrix#" + Integer.toString(i) + Integer.toString(j),0,1);
			}
		}

		//Subcircuit constraints, one per vehicle.

		
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
			PrimitiveConstraint lb = new Linear(store,list,weights,">=",leastAmountOfCities);
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
			PrimitiveConstraint columnConstraint = new Linear(store,column,columnWeights,"<=",1);
			store.impose(columnConstraint);
		}

		
		//For each vehicle; calculate the load: load = sum(c*x), where c(i) is the load in a certain town i and x=0 if the town has not been visited and x=1 if it has been visited.
	
		//Store the load for each vehicle.
		IntVar[] vehicleLoads = new IntVar[m];
	
		for(int i = 0; i < m; i++){
			IntVar[] list = subCircuitMatrix[i];
			IntVar vehicleLoad = new IntVar(store,"vehicleLoad#" + Integer.toString(i),0,(100*n));  //Upper limit of vehicle load here.  (Defualt: 100% of each cities load, If there is a limit to how many cities could be visited then that value should replace n.)
			Constraint sumWeight = new SumWeight(list,volumes,vehicleLoad);
			store.impose(sumWeight);
			vehicleLoads[i] = vehicleLoad;
		}

		//Sum up the vehicles load to a total load.
		IntVar totalLoad = new IntVar(store,"totalLoad",0,(100*n));
		store.impose(new Sum(vehicleLoads,totalLoad));


		
		//Calculate the distance traveled by the vehicles.
		IntVar[] vehicleDistanceArray = new IntVar[m];
		for(int i = 0; i < m; i++){
			IntVar[] cityDistanceArray = new IntVar[n];
			for(int j = 0; j < n; j++){
				IntVar nextCity = nextCities[i][j];
				int[] distanceRow = distanceMatrix[j]; //Distance to different cities for city j.
				
				IntVar cityDistance = new IntVar(store,"cityDistance",0,distanceLimit); 
				//Constraint ctr = new Element(nextCity,distanceRow,cityDistance,-1); 
				Constraint ctr = new Element(nextCity,distanceRow,cityDistance); 
				store.impose(ctr);
				cityDistanceArray[j] = cityDistance;  
			}
			IntVar vehicleDistance = new IntVar(store,"vehicleDistance",0,n * distanceLimit); 
			Constraint sumCtr = new Sum(cityDistanceArray,vehicleDistance);
			store.impose(sumCtr);
			vehicleDistanceArray[i] = vehicleDistance;
		}

		IntVar totalDistance = new IntVar(store,"totalDistance",0,m * n * distanceLimit);
		Constraint totalSumCtr = new Sum(vehicleDistanceArray,totalDistance);
		store.impose(totalSumCtr);


		//Side Constraints-------------------------------------------------------------------------------

		//Side constraint #1: How often a container must be emptied at minimum.

		/*Comment: I imagine that this would be dependent on querying a database to check for the latest date the city was visited.
		this date is then compared to the current date. If the difference is larger than a set period of time the city must be visited.
		I will represent this with a boolean for now.*/ 

		//Take notice of the city indices!
		for(int city : visitDueToTime){
			IntVar[] vehicles = new IntVar[m];
			int[] weights = new int[m];
			for(int i = 0; i < m ; i++){
				vehicles[i] = subCircuitMatrix[i][city];
				weights[i] = 1; 
			}
			PrimitiveConstraint visitCtr = new Linear(store,vehicles,weights,"=",1);
			store.impose(visitCtr);
		}

		//Side constraint #2: How long a route can be in units of length.
		//Ganska reduntant constraint, optimering kommer försöka hitta kortaste möjliga ändå. Det enda denna skule kunna göra
		//är att meddela att man bör minska antalet påtvingade besökta städer per rutt.
		/*
		for(IntVar vehicleDistance : vehicleDistanceArray){
			PrimitiveConstraint distCtr = new XltC(vehicleDistance,hardDistanceLimit);
			store.impose(distCtr);
		}*/


		//Side constraint #3: How many cities a certain vehicle route can contain at most.
		for(int i = 0; i < m; i++){
			IntVar[] list = subCircuitMatrix[i];
			//Enforce a minimum amount of visited cites per vehicle, or a range (lower bound, upper bound)
			int[] weights = new int[n];
			for(int k = 0; k < n; k++){
				weights[k] = 1;
			}
			PrimitiveConstraint ub = new Linear(store,list,weights,"<=",mostAmountofcities);
			store.impose(ub);
		}

		/*Comment: This will probably most of the time be an unnecessary constraint as the optimization will almost always keep the
		number of visited cities equal to the lowest number allowed */

		//Side constraint #4: How full a cities volume can be at most.

		//If the volume is greater than the maxVolume for some city, then that city must be visited by a vehicle.
		for(int j = 0; j < regNbr; j++){
			if(volumes[j] + buffer >= maxVolumes[j]){
				//System.out.println("Debug maxVolume:" + j + "     volume:" + volumes[j] + "    maxvolume:   " + maxVolumes[j]);
				IntVar[] vehicles = new IntVar[m];
				int[] weights = new int[m];
				for(int i = 0; i < m ; i++){
					vehicles[i] = subCircuitMatrix[i][j];
					weights[i] = 1; 
				}	
				PrimitiveConstraint volCtr = new Linear(store,vehicles,weights,"=",1);
				store.impose(volCtr);
			}
		}

		//Side constraint #5: Enforce a specific "special-city" where vehicles start from/or end up. Does not contain a load.

		//Every vehicle must visit exactly one such city.
		for(int i = 0; i < m; i++){
			IntVar[] startCities = new IntVar[startNbr];
			int[] weights = new int[startNbr];
			for(int j = regNbr; j < regNbr + startNbr; j++){
				startCities[j-regNbr] = subCircuitMatrix[i][j];
				weights[j-regNbr] = 1;
			}
			PrimitiveConstraint startConstraint = new Linear(store,startCities,weights,"=",1);
			store.impose(startConstraint);
		}



		/*Comment: It's effectively the same thing to enforce only start city or only end city.*/

		//Side constraint #6: Enforce two specific "special-cities" one where vehicles start from and another where they end up. Does not contain a load.
		/*
		//endLoc-cities must point only to startLoc-cities. 
		for(int i = 0; i < m; i++){
			for(int j = regNbr + startNbr; j < n; j++){
				IntVar nextCity = nextCities[i][j];
				//nextCity.setDomain(regNbr+1,regNbr+startNbr);  //Careful with indices
				int indexOfShortest = findShortest(j,regNbr,startNbr,distanceMatrix);
				nextCity.setDomain(indexOfShortest+1,indexOfShortest+1);
				nextCity.addDom((j+1),(j+1));
			}
		}
		
		
		//startLoc-cities must not point to endLoc-cities, or other startLoc-cities.
		for(int i = 0; i < m; i++){
			for(int j = regNbr; j < regNbr + startNbr; j++){
				IntVar nextCity = nextCities[i][j];
				nextCity.setDomain(1,regNbr);
				nextCity.addDom((j+1),(j+1)); //Den måste få peka på sig själv!!!!!
			}
		}
		
		//Regular cities may not point to startLoc.
		for(int i = 0; i < m; i++){
			for(int j = 0; j < regNbr; j++){
				IntVar nextCity = nextCities[i][j];
				nextCity.setDomain(1,regNbr);
				if(endNbr > 0){//Throw exception here instead of if-condition.
					nextCity.addDom(regNbr+startNbr+1,n);	
				}
			}
		}
		*/
		
		
		








			

		/*Comment: Some side constraints forces cities to be visited. Something that can come from this is that
		more cities are needed to be visited than there are vehicles enough for. This will give the result: No solution.*/

		//--

		//--------------------------------------------------------------------------------------------------

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

		//This is where var,varSelect,tieBreakerVarSelect & indomain are decided
		SelectChoicePoint<IntVar> select = new SimpleSelect<IntVar>(varVector,new MostConstrainedDynamic<IntVar>(),new IndomainMin<IntVar>());
		//SelectChoicePoint<IntVar> select = new SimpleSelect<IntVar>(varVector,new SmallestDomain<IntVar>(),new IndomainMin<IntVar>());
		boolean result = label.labeling(store,select,negRatio);

		if (result) {
			System.out.println("\n*** Yes");
			System.out.println("Arrangment: "); 
			printIntVarMatrix(nextCities,m,n); 
			System.out.println("Ratio: " + ratio);
			System.out.println("distance: " + totalDistance);
			System.out.println("load: " + totalLoad);
			
		}
		else System.out.println("\n*** No");
	}

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