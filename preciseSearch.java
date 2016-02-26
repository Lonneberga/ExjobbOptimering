
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
import org.jacop.floats.search.*;


public class preciseSearch{


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
		double[][] coordMatrix = readCoordinates("/Users/Emil/Documents/exjobb/koordinater/femton.txt");

		//Total number of cities
		int n = coordMatrix.length;
		double[][] distanceMatrix = distanceMatrix(coordMatrix);
		//printMatrix(distanceMatrix,n,n);
		
		//Since JaCoP will optimize the route as well as it can it must be hindered to take a shortest route 
		//that for example contains 0 cities.
		//Least amount of visited cities for a certain vehicle. 
		int leastAmountOfCities = 14;

		//Volume of load in cities.			
		double[] volumes  = new double[n]; //Need to be Integers for sumWeight-constraint. Count as parts of a hundred.
		//Does not use sumWeight anymore. Might be able to use double and later on FloatVar 
		for(int i = 0; i < n; i++){
			volumes[i] = 1;
		}


		//Number of vehicles
		int m = 1; 

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
		double[] maxVolumes = new double[n];

		//Default: no restriction.
		for(int j = 0; j < n; j++){
			maxVolumes[j] = 1.1;
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

		
		//For each vehicle; calculate the load: load = sum(c*x), where c(i) is the load in a certain town i and x=0 if the town has not been visited and x=1 if it has been visited.
	
		//Store the load for each vehicle. (extra space to use in LinearFloat)
		FloatVar[] vehicleLoads = new FloatVar[m+1];
		//Make a matrix where an element is the product of boolean whether a city is visited and that cities volume. Row: Vehicle, Column; city
		//Initialize
		//This has an extra column which is used in LinearFloat later.
		FloatVar[][] cityVisitedLoad = new FloatVar[m][n+1];
		for(int i = 0;i < m; i++){
			for(int j = 0; j < n; j++){
				cityVisitedLoad[i][j] = new FloatVar(store,"cityVisitedLoad#" + Integer.toString(i) +","+ Integer.toString(j), 0.0,1.0);
			}
		}
		//Make constraints
		for(int i =0; i < m; i++){
			for(int j = 0; j < n; j++){


				//------------gör om IntVar till FloatVar för att få in i metod
				FloatVar subCircuitElementFloat = new FloatVar(store,"subCiruitElementAsFloat",0.0,1.0);
				Constraint conv = new XeqP(subCircuitMatrix[i][j], subCircuitElementFloat);
				//Viktigt att inte göra något annant med subCircuitElement då det skulle påverka subCircuitMatrix
				store.impose(conv);

				Constraint ctr = new PmulCeqR(subCircuitElementFloat,volumes[j], cityVisitedLoad[i][j]);
				store.impose(ctr);
			}
		}

		//Volume for a certain vehicle
		for(int i = 0; i < m; i++){
			FloatVar[] list = cityVisitedLoad[i];

			FloatVar vehicleLoad = new FloatVar(store,"vehicleLoad#" + Integer.toString(i),0.0,n);  //Upper limit of vehicle load here.  (Defualt: 100% of each cities load, If there is a limit to how many cities could be visited then that value should replace n.)
			//Fill the empty spot with vehcileLoad
			list[n] = vehicleLoad;

			//Build an array of weights.
			double[] weights = new double[n+1];
			for(int j = 0; j < n; j++){
				weights[j] = 1;
			}
			weights[n] = -1;


			Constraint sumWeight = new LinearFloat(store,list,weights,"==",0);
			

			store.impose(sumWeight);
			vehicleLoads[i] = vehicleLoad;
		}

		//Sum up the vehicles load to a total load.
		FloatVar totalLoad = new FloatVar(store,"totalLoad",0.0,n);

		//Setup for Linear Float
		double[] weights = new double[m+1];
		for(int i = 0; i < m; i++){
			weights[i] = 1;
		}
		weights[m] = -1;

		vehicleLoads[m] = totalLoad;

		Constraint totSum = new LinearFloat(store,vehicleLoads,weights,"==",0);
		//store.impose(new SumInt(store,vehicleLoads,weights"==",0));
		store.impose(totSum);

		//Initialize a variable vector for distance. (Här ändrat från IntVar till FloatVar, skillnad i sökning?)
		FloatVar[] varVectorDistance = new FloatVar[m*n];

		//Calculate the distance traveled by the vehicles.
		FloatVar[] vehicleDistanceArray = new FloatVar[m+1];
		for(int i = 0; i < m; i++){
			FloatVar[] cityDistanceArray = new FloatVar[n+1];
			for(int j = 0; j < n; j++){
				IntVar nextCity = nextCities[i][j];
				double[] distanceRow = distanceMatrix[j]; //Distance to different cities for city j.
				
				FloatVar cityDistance = new FloatVar(store,"cityDistance",0.0,distanceLimit); 
				//Constraint ctr = new Element(nextCity,distanceRow,cityDistance,-1); 
				//Constraint ctr = new Element(nextCity,distanceRow,cityDistance); 
				Constraint ctr = new ElementFloat(nextCity,distanceRow,cityDistance,0); 
				store.impose(ctr);
				cityDistanceArray[j] = cityDistance;

				//Put the distances in a variable vector for if we want to use them for search.
				varVectorDistance[i * n + j] = cityDistance; 

			}
			FloatVar vehicleDistance = new FloatVar(store,"vehicleDistance",0.0,n * distanceLimit); 

			//Construct array with weights
			double[] weightsA = new double[n+1];
			for(int j = 0; j < n; j++){
				weightsA[j] = 1;
			}
			weightsA[n] = -1;

			//Add the sum-variable to enforce the following type of relation:
			//w1 · x1 + w2 · x2 + · · · + (−1) · s = 0.
			cityDistanceArray[n] = vehicleDistance;

			
			Constraint sumCtr = new LinearFloat(store,cityDistanceArray,weightsA,"==",0);
			//Constraint sumCtr = new SumInt(store,[cityDistanceArray,weights,"==",vehicleDistance);
			store.impose(sumCtr);
			vehicleDistanceArray[i] = vehicleDistance;
		}

		FloatVar totalDistance = new FloatVar(store,"totalDistance",0.0,m * n * distanceLimit);
		//Construct array with weights
		double[] weightsB = new double[m+1];
		for(int i = 0; i < m; i++){
			weightsB[i] = 1;
		}
		weightsB[m] = -1;
		
		//Add the sum-variable to enforce the following type of relation:
		//w1 · x1 + w2 · x2 + · · · + (−1) · s = 0.
		vehicleDistanceArray[m] = totalDistance;
		

		Constraint totalSumCtr = new LinearFloat(store,vehicleDistanceArray,weightsB,"==",0);
		//Constraint totalSumCtr = new SumInt(store,vehicleDistanceArray,"==",totalDistance);
		store.impose(totalSumCtr);


		//Side Constraints-------------------------------------------------------------------------------

		//Side constraint #1: How often a container must be emptied at minimum.

		/*Comment: I imagine that this would be dependent on querying a database to check for the latest date the city was visited.
		this date is then compared to the current date. If the difference is larger than a set period of time the city must be visited.
		I will represent this with a boolean for now.*/ 

		//Take notice of the city indices!
		for(int city : visitDueToTime){
			IntVar[] vehicles = new IntVar[m];
			int[] weightsC = new int[m];
			for(int i = 0; i < m ; i++){
				vehicles[i] = subCircuitMatrix[i][city];
				weightsC[i] = 1; 
			}
			PrimitiveConstraint visitCtr = new LinearInt(store,vehicles,weightsC,"=",1);
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
			int[] weightsD = new int[n];
			for(int k = 0; k < n; k++){
				weightsD[k] = 1;
			}
			PrimitiveConstraint ub = new LinearInt(store,list,weightsD,"<=",mostAmountofcities);
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
				int[] weightsE = new int[m];
				for(int i = 0; i < m ; i++){
					vehicles[i] = subCircuitMatrix[i][j];
					weightsE[i] = 1; 
				}	
				PrimitiveConstraint volCtr = new LinearInt(store,vehicles,weightsE,"=",1);
				store.impose(volCtr);
			}
		}

		//Side constraint #5: Enforce a specific "special-city" where vehicles start from/or end up. Does not contain a load.

		//Every vehicle must visit exactly one such city.
		for(int i = 0; i < m; i++){
			IntVar[] startCities = new IntVar[startNbr];
			int[] weightsF = new int[startNbr];
			for(int j = regNbr; j < regNbr + startNbr; j++){
				startCities[j-regNbr] = subCircuitMatrix[i][j];
				weightsF[j-regNbr] = 1;
			}
			PrimitiveConstraint startConstraint = new LinearInt(store,startCities,weightsF,"=",1);
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

		

		
		FloatVar ratio = new FloatVar(store,"ratio",0.0,ratioLimit);

		Constraint division = new PdivQeqR(totalLoad, totalDistance, ratio);

		store.impose(division);

		//Maximizing x is to minimize (-x)
		FloatVar negRatio = new FloatVar(store,"negDivision", -ratioLimit,0.0);
		Constraint negationConstraint = new PmulCeqR(ratio,-1, negRatio);

		store.impose(negationConstraint);


		//Speshul Float Search
		
		
		DepthFirstSearch<FloatVar> search = new DepthFirstSearch<FloatVar>();
		SplitSelectFloat<FloatVar> select = new SplitSelectFloat<FloatVar>(store,varVectorDistance,new LargestDomainFloat<FloatVar>());
		boolean result = search.labeling(store,select);

		//Search<IntVar> label = new DepthFirstSearch<IntVar>();




		//This is where var,varSelect,tieBreakerVarSelect & indomain are decided
		//SelectChoicePoint<IntVar> select = new SimpleSelect<IntVar>(varVector,new MostConstrainedDynamic<IntVar>(),new IndomainMin<IntVar>());
		//SelectChoicePoint<IntVar> selectTwo = new SimpleSelect<IntVar>(varVector,new MaxRegret<IntVar>(),new IndomainMin<IntVar>());
					
		//Stuff for varVectorDistance
		//SelectChoicePoint<IntVar> select = new SimpleSelect<IntVar>(varVectorDistance,new MostConstrainedDynamic<IntVar>(),new IndomainMin<IntVar>());
		//SelectChoicePoint<IntVar> selectOne = new SimpleSelect<IntVar>(varVectorDistance,new MaxRegret<IntVar>(),new IndomainMin<IntVar>());



		//boolean result = label.labeling(store,select,negRatio);
		//boolean resultOne = label.labeling(store,selectOne,negRatio);
		//boolean resultTwo = label.labeling(store,selectTwo,negRatio);


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
 	static double[][] distanceMatrix(double[][] input){
 		//Debug
 		int debug = 0;

		if(input[1].length != 2){
			System.out.print("Wrong dimension on distance matrix");
			System.exit(1);
		}
		int n = input.length;
		double[][] distanceMatrix = new double[n][n];
		double distance;  
		for(int i = 0; i < n; i++){
			double x1 = input[i][0]; 
			double y1 = input[i][1];

			for(int j = 0; j < n; j++){
				double x2 = input[j][0];
				double y2 = input[j][1];
				double deltaX = Math.abs(x1 - x2);
				double deltaY = Math.abs(y1 - y2);
				if(deltaX > 46340 || deltaY > 46340 || ((deltaX*deltaX) + (deltaY * deltaY))<0){ //overflow from deltaX * deltaX, deltaY * deltaY, sum of those products
					System.out.print("Overflow. To big distances between cities.");
					System.exit(1);
				}
				double squaredSum = (deltaX*deltaX) + (deltaY * deltaY);
				distance = Math.sqrt(squaredSum);

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

	static void printMatrix(double[][] matrix, int row, int column){
		for(int i = 0; i < row; i++){
			for(int j = 0; j < column; j++){
				System.out.printf("%-15s", matrix[i][j]);
			}
			System.out.print("\n");
		}
	}

	static double[][] readCoordinates(String path){
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
		double[][] partMatrix = new double[n][2];
		for(int i = 0; i < n; i++){
			String line = list.get(i);
			String[] parts = line.split(",");
			partMatrix[i][0] = Double.parseDouble(parts[0]);
			partMatrix[i][1] = Double.parseDouble(parts[1]);
		}
	return partMatrix;

	}

}
