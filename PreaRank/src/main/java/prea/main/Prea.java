package prea.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import prea.data.splitter.*;
import prea.data.structure.SparseMatrix;
import prea.data.structure.SparseVector;
import prea.recommender.*;
import prea.recommender.baseline.*;
import prea.recommender.memory.*;
import prea.recommender.matrix.*;
import prea.recommender.llorma.*;
import prea.recommender.etc.*;
import prea.util.EvaluationMetrics;
import prea.util.Printer;
import prea.util.KernelSmoothing;

/**
 * The main class of this toolkit. It includes loading the dataset,
 * splitting train/test set, and interface to evaluation for each algorithms.
 * 
 * @author Joonseok Lee
 * @since 2012. 4. 20
 * @version 1.1
 */
public class Prea {
	/** The maximum number of threads which will run simultaneously. 
	 *  We recommend not to exceed physical limit of your machine. */
	public static final int MULTI_THREAD_LEVEL = 8;
	
	/*========================================
	 * Parameters
	 *========================================*/
	/** The name of data file used for test. */
	public static String dataFileName;
	/** Evaluation mode */
	public static int evaluationMode;
	/** Proportion of items which will be used for test purpose. */
	public static double testRatio;
	/** The name of predefined split data file. */
	public static String splitFileName;
	/** The number of folds when k-fold cross-validation is used. */
	public static int foldCount;
	/** Indicating whether to run all algorithms. */
	public static boolean runAllAlgorithms;
	/** The code for an algorithm which will run. */
	public static String algorithmCode;
	/** Parameter list for the algorithm to run. */
	public static String[] algorithmParameters;
	/** Indicating whether loading pre-calculated user similarity file or not */
	public static boolean userSimilarityPrefetch = false;
	/** Indicating whether loading pre-calculated item similarity file or not */
	public static boolean itemSimilarityPrefetch = false;
	/** Indicating whether storing output in a serialized file. */
	public static boolean serialize;
	
	/*========================================
	 * Common Variables
	 *========================================*/
	/** Rating matrix for each user (row) and item (column) */
	public static SparseMatrix rateMatrix;
	/** Rating matrix for test items. Not allowed to refer during training and validation phase. */
	public static SparseMatrix testMatrix;
	/** Average of ratings for each user. */
	public static SparseVector userRateAverage;
	/** Average of ratings for each item. */
	public static SparseVector itemRateAverage;
	/** The number of users. */
	public static int userCount = 1240;
	/** The number of items. */
	public static int itemCount = 300;
	/** Maximum value of rating, existing in the dataset. */
	public static int maxValue;
	/** Minimum value of rating, existing in the dataset. */
	public static int minValue;
	/** The list of item names, provided with the dataset. */
	public static String[] columnName;
	/**
	 * Added By Sudipta
	 */
	/** The matrices holds drug similarity*/
	public static Double[][] drugSim;
	/** The matrices holds drug similarity*/
	public static Double[][] diseaseSim;
	/**Maps drug name to drug id and back */
	public static HashMap<String, Integer> drugNameToIndexMap = new HashMap<String, Integer>();
	public static HashMap<Integer, String> indexToDrugNameMap = new HashMap<Integer, String>();
	
	/**Maps disease name to disease id and back */
	public static HashMap<String, Integer> diseaseNameToIndexMap = new HashMap<String, Integer>();
	public static HashMap<Integer, String> indexToDiseaseNameMap = new HashMap<Integer, String>();
	
	/**
	 * This is for testing purpose
	 */
	/** Rating matrix for each user (row) and item (column) */
	public static SparseMatrix testMatrix1;
	
	/**
	 * Test examples for every algorithm. Also includes parsing the given parameters.
	 * 
	 * @param argv The argument list. Each element is separated by an empty space.
	 * First element is the data file name, and second one is the algorithm name.
	 * Third and later includes parameters for the chosen algorithm.
	 * Please refer to our web site for detailed syntax.
	 */
	public static void main(String argv[]) {
		// Set default setting first:
		dataFileName = "movieLens_100K";
		evaluationMode = DataSplitManager.SIMPLE_SPLIT;
		splitFileName = dataFileName + "_split.txt";
		testRatio = 0.2;
		foldCount = 10;
		runAllAlgorithms = true;
		serialize = false;
		
		// Parsing the argument:
		if (argv.length > 1) {
			parseCommandLine(argv);
		}
		
		/**
		 * Read all input files		
		 */	
		dataFileName = "drugDisease";
		
		/*removeMissingDrugDisease("/home/sudipta/Documents/Research_With_Dr Wang/IBM_UConn_Collaboration/Drug_Disease/drug_sim.csv",
				"/home/sudipta/Documents/Research_With_Dr Wang/IBM_UConn_Collaboration/Drug_Disease/disease-sim.csv", 
				"/home/sudipta/Documents/Research_With_Dr Wang/IBM_UConn_Collaboration/Drug_Disease/drug-disease.tsv");*/
		
		readSimilarityFiles("/home/sudipta/Documents/Research_With_Dr Wang/IBM_UConn_Collaboration/Drug_Disease/drug_sim.csv",
				"/home/sudipta/Documents/Research_With_Dr Wang/IBM_UConn_Collaboration/Drug_Disease/disease-sim.csv");
		/*convertToArffFormat("/home/sudipta/Documents/Research_With_Dr Wang/IBM_UConn_Collaboration/Drug_Disease/drug-disease.tsv");*/
		readArff (dataFileName + ".arff");		
		
		/**
		 * Test whether the files are read correctly
		 */
		//checkCorrectness();
		
		// Train/test data split:
		switch (evaluationMode) {
			case DataSplitManager.SIMPLE_SPLIT:
				SimpleSplit sSplit = new SimpleSplit(rateMatrix, testRatio, maxValue, minValue);
				System.out.println("Evaluation\tSimple Split (" + (1 - testRatio) + " train, " + testRatio + " test)");
				testMatrix = sSplit.getTestMatrix();
				userRateAverage = sSplit.getUserRateAverage();
				itemRateAverage = sSplit.getItemRateAverage();				
				run();
				break;
			case DataSplitManager.PREDEFINED_SPLIT:
				PredefinedSplit pSplit = new PredefinedSplit(rateMatrix, splitFileName, maxValue, minValue);
				System.out.println("Evaluation\tPredefined Split (" + splitFileName + ")");
				testMatrix = pSplit.getTestMatrix();
				userRateAverage = pSplit.getUserRateAverage();
				itemRateAverage = pSplit.getItemRateAverage();
				
				run();
				break;
			case DataSplitManager.K_FOLD_CROSS_VALIDATION:
				KfoldCrossValidation kSplit = new KfoldCrossValidation(rateMatrix, foldCount, maxValue, minValue);
				System.out.println("Evaluation\t" + foldCount + "-fold Cross-validation");
				for (int k = 1; k <= foldCount; k++) {
					testMatrix = kSplit.getKthFold(k);
					userRateAverage = kSplit.getUserRateAverage();
					itemRateAverage = kSplit.getItemRateAverage();
					
					run();
				}
				break;
		}		
	}
	
	/** Run an/all algorithm with given data, based on the setting from command arguments. */
	private static void run() {
		if (runAllAlgorithms) {
			runAll();
		}
		else {
			runIndividual(algorithmCode, algorithmParameters);
		}
	}
	
	/**
	 * Parse the command from user.
	 * 
	 * @param command The command string given by user.
	 */
	private static void parseCommandLine(String[] command) {
		int i = 0;
		
		while (i < command.length) {
			if (command[i].equals("-f")) { // input file
				dataFileName = command[i+1];
				i += 2;
			}
			else if (command[i].equals("-s")) { // data split
				if (command[i+1].equals("simple")) {
					evaluationMode = DataSplitManager.SIMPLE_SPLIT;
					testRatio = Double.parseDouble(command[i+2]);
				}
				else if (command[i+1].equals("pred")) {
					evaluationMode = DataSplitManager.PREDEFINED_SPLIT;
					splitFileName = command[i+2].trim();
				}
				else if (command[i+1].equals("kcv")) {
					evaluationMode = DataSplitManager.K_FOLD_CROSS_VALIDATION;
					foldCount = Integer.parseInt(command[i+2]);
				}
				i += 3;
			}
			else if (command[i].equals("-a")) { // algorithm
				runAllAlgorithms = false;
				algorithmCode = command[i+1];
				
				// parameters for the algorithm:
				int j = 0;
				while (command.length > i+2+j && !command[i+2+j].startsWith("-")) {
					j++;
				}
				
				algorithmParameters = new String[j];
				System.arraycopy(command, i+2, algorithmParameters, 0, j);
				
				i += (j + 2);
			}
		}
	}
	
	/**
	 * Test interface for a recommender system.
	 * Print MAE, RMSE, and rank-based half-life score for given test data.
	 * 
	 * @return evaluation metrics and elapsed time for learning and evaluation.
	 */
	public static String testRecommender(String algorithmName, Recommender r) {
		
		long learnStart = System.currentTimeMillis();
		r.buildModel(rateMatrix);
		long learnEnd = System.currentTimeMillis();
		
		long evalStart = System.currentTimeMillis();
		EvaluationMetrics result = r.evaluate(testMatrix);
		long evalEnd = System.currentTimeMillis();
		
		/**
		 * For Testing purpose
		 */
		System.out.println("TestMatrix1 Begins.................");
		long evalStart1 = System.currentTimeMillis();
		EvaluationMetrics result1 = r.evaluate(testMatrix1);
		long evalEnd1 = System.currentTimeMillis();
		
		return algorithmName + "\t" + result.printOneLine() + "\t" + Printer.printTime(learnEnd - learnStart) + "\t" + Printer.printTime(evalEnd - evalStart);
	}
	
	/** Run one algorithm with customized parameters with given data. */
	public static void runIndividual(String algorithmCode, String[] parameters) {
		System.out.println(EvaluationMetrics.printTitle() + "\tTrain Time\tTest Time");
		
		if (algorithmCode.toLowerCase().equals("const")) {
			Constant constant = new Constant(userCount, itemCount, maxValue, minValue, (maxValue + minValue) / 2);
			if (serialize) {
				saveSerializedFile(constant, "Const.obj");
				constant = (Constant) readSerializedFile("Const.obj");
			}
			System.out.println(testRecommender("Const", constant));
		}
		else if (algorithmCode.toLowerCase().equals("avg")) {
			Average average = new Average(userCount, itemCount, maxValue, minValue);
			if (serialize) {
				saveSerializedFile(average, "AllAvg.obj");
				average = (Average) readSerializedFile("AllAvg.obj");
			}
			System.out.println(testRecommender("AllAvg", average));
		}
		else if (algorithmCode.toLowerCase().equals("useravg")) {
			UserAverage userAverage = new UserAverage(userCount, itemCount, maxValue, minValue, userRateAverage);
			if (serialize) {
				saveSerializedFile(userAverage, "UserAvg.obj");
				userAverage = (UserAverage) readSerializedFile("UserAvg.obj");
			}
			System.out.println(testRecommender("UserAvg", userAverage));
		}
		else if (algorithmCode.toLowerCase().equals("itemavg")) {
			ItemAverage itemAverage = new ItemAverage(userCount, itemCount, maxValue, minValue, itemRateAverage);
			if (serialize) {
				saveSerializedFile(itemAverage, "ItemAvg.obj");
				itemAverage = (ItemAverage) readSerializedFile("ItemAvg.obj");
			}
			System.out.println(testRecommender("ItemAvg", itemAverage));
		}
		else if (algorithmCode.toLowerCase().equals("random")) {
			Random random = new Random(userCount, itemCount, maxValue, minValue);
			if (serialize) {
				saveSerializedFile(random, "Random.obj");
				random = (Random) readSerializedFile("Random.obj");
			}
			System.out.println(testRecommender("Random", random));
		}
		else if (algorithmCode.toLowerCase().equals("userbased")) {
			int neighborhoodSize;
			int similarityMethod = MemoryBasedRecommender.PEARSON_CORR;
			boolean useDefaultValue = false;
			double defaultValue = 0.0;
			String userSimFileName = "";
			
			if (parameters.length < 1) {
				neighborhoodSize = 50;
			}
			else {
				neighborhoodSize = Integer.parseInt(parameters[0]);
				
				if (parameters.length < 2) {
					similarityMethod = MemoryBasedRecommender.PEARSON_CORR;
				}
				else {
					if (parameters[1].equals("pearson")) similarityMethod = MemoryBasedRecommender.PEARSON_CORR;
					else if (parameters[1].equals("cosine")) similarityMethod = MemoryBasedRecommender.VECTOR_COS;
					else if (parameters[1].equals("msd")) similarityMethod = MemoryBasedRecommender.MEAN_SQUARE_DIFF;
					else if (parameters[1].equals("mad")) similarityMethod = MemoryBasedRecommender.MEAN_ABS_DIFF;
					else if (parameters[1].equals("invuserfreq")) similarityMethod = MemoryBasedRecommender.INVERSE_USER_FREQUENCY;
					else similarityMethod = MemoryBasedRecommender.PEARSON_CORR;
					
					if (parameters.length > 2) {
						for (int i = 2; i < parameters.length; i += 2) {
							if (parameters[i].equals("default")) {
								useDefaultValue = true;
								defaultValue = Double.parseDouble(parameters[i+1]);
							}
							else if (parameters[i].equals("usersim")) {
								userSimilarityPrefetch = true;
								userSimFileName = parameters[i+1];
							}
							else {
								// Do nothing. Pass the wrong command.
							}
						}
					}
				}
			}
			
			String algorithmName;
			if (useDefaultValue) algorithmName = "UserDft";
			else algorithmName = "UserBsd";
			
			UserBased userBsd = new UserBased(userCount, itemCount, maxValue, minValue, neighborhoodSize,
				similarityMethod, useDefaultValue, defaultValue, userRateAverage, userSimilarityPrefetch, userSimFileName);
			if (serialize) {
				saveSerializedFile(userBsd, "UserBsd.obj");
				userBsd = (UserBased) readSerializedFile("UserBsd.obj");
			}
			System.out.println(testRecommender(algorithmName, userBsd));
		}
		else if (algorithmCode.toLowerCase().equals("itembased")) {
			int neighborhoodSize;
			int similarityMethod = MemoryBasedRecommender.PEARSON_CORR;
			boolean useDefaultValue = false;
			double defaultValue = 0.0;
			String itemSimFileName = "";
			
			if (parameters.length < 1) {
				neighborhoodSize = 50;
			}
			else {
				neighborhoodSize = Integer.parseInt(parameters[0]);
				
				if (parameters.length < 2) {
					similarityMethod = MemoryBasedRecommender.PEARSON_CORR;
				}
				else {
					if (parameters[1].equals("pearson")) similarityMethod = MemoryBasedRecommender.PEARSON_CORR;
					else if (parameters[1].equals("cosine")) similarityMethod = MemoryBasedRecommender.VECTOR_COS;
					else if (parameters[1].equals("msd")) similarityMethod = MemoryBasedRecommender.MEAN_SQUARE_DIFF;
					else if (parameters[1].equals("mad")) similarityMethod = MemoryBasedRecommender.MEAN_ABS_DIFF;
					else if (parameters[1].equals("invuserfreq")) similarityMethod = MemoryBasedRecommender.INVERSE_USER_FREQUENCY;
					else similarityMethod = MemoryBasedRecommender.PEARSON_CORR;
					
					if (parameters.length > 2) {
						for (int i = 2; i < parameters.length; i += 2) {
							if (parameters[i].equals("default")) {
								useDefaultValue = true;
								defaultValue = Double.parseDouble(parameters[i+1]);
							}
							else if (parameters[i].equals("usersim")) {
								itemSimilarityPrefetch = true;
								itemSimFileName = parameters[i+1];
							}
							else {
								// Do nothing. Pass the wrong command.
							}
						}
					}
				}
			}
			
			String algorithmName;
			if (useDefaultValue) algorithmName = "ItemDft";
			else algorithmName = "ItemBsd";
			
			ItemBased itemBsd = new ItemBased(userCount, itemCount, maxValue, minValue, neighborhoodSize,
				similarityMethod, useDefaultValue, defaultValue, itemRateAverage, itemSimilarityPrefetch, itemSimFileName);
			if (serialize) {
				saveSerializedFile(itemBsd, "ItemBsd.obj");
				itemBsd = (ItemBased) readSerializedFile("ItemBsd.obj");
			}
			System.out.println(testRecommender(algorithmName, itemBsd));
		}
		else if (algorithmCode.toLowerCase().equals("slopeone")) {
			SlopeOne slope1 = new SlopeOne(userCount, itemCount, maxValue, minValue);
			if (serialize) {
				saveSerializedFile(slope1, "Slope1.obj");
				slope1 = (SlopeOne) readSerializedFile("Slope1.obj");
			}
			System.out.println(testRecommender("Slope1", slope1));
		}
		else if (algorithmCode.toLowerCase().equals("regsvd")) {
			RegularizedSVD regsvd = new RegularizedSVD(userCount, itemCount, maxValue, minValue, Integer.parseInt(parameters[0]), Double.parseDouble(parameters[1]), Double.parseDouble(parameters[2]), 0, Integer.parseInt(parameters[3]), false);
			if (serialize) {
				saveSerializedFile(regsvd, "RegSVD.obj");
				regsvd = (RegularizedSVD) readSerializedFile("RegSVD.obj");
			}
			System.out.println(testRecommender("RegSVD", regsvd));
		}
		else if (algorithmCode.toLowerCase().equals("nmf")) {
			NMF nmf = new NMF(userCount, itemCount, maxValue, minValue, Integer.parseInt(parameters[0]), 0, Double.parseDouble(parameters[1]), 0, Integer.parseInt(parameters[2]), 0.2, false);
			if (serialize) {
				saveSerializedFile(nmf, "NMF.obj");
				nmf = (NMF) readSerializedFile("NMF.obj");
			}
			System.out.println(testRecommender("NMF", nmf));
		}
		else if (algorithmCode.toLowerCase().equals("pmf")) {
			PMF pmf = new PMF(userCount, itemCount, maxValue, minValue, Integer.parseInt(parameters[0]), Integer.parseInt(parameters[1]), Double.parseDouble(parameters[2]), Double.parseDouble(parameters[3]), Integer.parseInt(parameters[4]), false);
			if (serialize) {
				saveSerializedFile(pmf, "PMF.obj");
				pmf = (PMF) readSerializedFile("PMF.obj");
			}
			System.out.println(testRecommender("PMF", pmf));
		}
		else if (algorithmCode.toLowerCase().equals("bpmf")) {
			BayesianPMF bpmf = new BayesianPMF(userCount, itemCount, maxValue, minValue, Integer.parseInt(parameters[0]), 0, 0, 0, Integer.parseInt(parameters[1]), false);
			if (serialize) {
				saveSerializedFile(bpmf, "BPMF.obj");
				bpmf = (BayesianPMF) readSerializedFile("BPMF.obj");
			}
			System.out.println(testRecommender("BPMF", bpmf));
		}
		else if (algorithmCode.toLowerCase().equals("nlpmf")) {
			NonlinearPMF nlpmf = new NonlinearPMF(userCount, itemCount, maxValue, minValue, Integer.parseInt(parameters[0]), Double.parseDouble(parameters[1]), Double.parseDouble(parameters[2]), Integer.parseInt(parameters[3]),
					Double.parseDouble(parameters[4]),Double.parseDouble(parameters[5]),Double.parseDouble(parameters[6]),Double.parseDouble(parameters[7]));
			if (serialize) {
				saveSerializedFile(nlpmf, "NLPMF.obj");
				nlpmf = (NonlinearPMF) readSerializedFile("NLPMF.obj");
			}
			System.out.println(testRecommender("NLPMF", nlpmf));
		}
		else if (algorithmCode.toLowerCase().equals("npca")) {
			FastNPCA npca = new FastNPCA(userCount, itemCount, maxValue, minValue, Double.parseDouble(parameters[0]), Integer.parseInt(parameters[1]));
			if (serialize) {
				saveSerializedFile(npca, "NPCA.obj");
				npca = (FastNPCA) readSerializedFile("NPCA.obj");
			}
			System.out.println(testRecommender("NPCA", npca));
		}
		else if (algorithmCode.toLowerCase().equals("rank")) {
			RankBased rankMean = new RankBased(userCount, itemCount, maxValue, minValue, Double.parseDouble(parameters[0]), RankBased.MEAN_LOSS);
			if (serialize) {
				saveSerializedFile(rankMean, "RankMean.obj");
				rankMean = (RankBased) readSerializedFile("RankMean.obj");
			}
			System.out.println(testRecommender("RnkMean", rankMean));
			
			RankBased rankAsym = new RankBased(userCount, itemCount, maxValue, minValue, Double.parseDouble(parameters[0]), RankBased.ASYMM_LOSS);
			if (serialize) {
				saveSerializedFile(rankAsym, "RankAsym.obj");
				rankAsym = (RankBased) readSerializedFile("RankAsym.obj");
			}
			System.out.println(testRecommender("RnkAsym", rankAsym));
		}
		else if (algorithmCode.toLowerCase().equals("sgllorma")) {
			RegularizedSVD baseline = new RegularizedSVD(userCount, itemCount, maxValue, minValue, 10, 0.005, 0.2, 0, 200, false);
			//baseline.buildModel(rateMatrix);
			/*SingletonGlobalLLORMA sgllorma = new SingletonGlobalLLORMA(userCount, itemCount, maxValue, minValue, Integer.parseInt(parameters[0]),
					Double.parseDouble(parameters[1]), Double.parseDouble(parameters[2]), Integer.parseInt(parameters[3]), Integer.parseInt(parameters[4]),
					KernelSmoothing.EPANECHNIKOV_KERNEL, 0.8, 0.2, baseline, true);*/			
			SingletonGlobalLLORMA sgllorma = new SingletonGlobalLLORMA(userCount, itemCount, maxValue, minValue, Integer.parseInt(parameters[0]),
					Double.parseDouble(parameters[1]), Double.parseDouble(parameters[2]), Integer.parseInt(parameters[3]), Integer.parseInt(parameters[4]),
					KernelSmoothing.EPANECHNIKOV_KERNEL, 2, 0.2, baseline, true, drugSim, diseaseSim);
			System.out.println(testRecommender("sgLRMA", sgllorma));
		}
		else if (algorithmCode.toLowerCase().equals("spllorma")) {
			RegularizedSVD baseline = new RegularizedSVD(userCount, itemCount, maxValue, minValue, 10, 0.005, 0.1, 0, 200, false);
			baseline.buildModel(rateMatrix);
			SingletonParallelLLORMA spllorma = new SingletonParallelLLORMA(userCount, itemCount, maxValue, minValue, Integer.parseInt(parameters[0]),
					Double.parseDouble(parameters[1]), Double.parseDouble(parameters[2]), Integer.parseInt(parameters[3]), Integer.parseInt(parameters[4]),
					KernelSmoothing.EPANECHNIKOV_KERNEL, 0.8, baseline, testMatrix, Integer.parseInt(parameters[5]), true);
			System.out.println(testRecommender("spLRMA", spllorma));
		}
	}
	
	/** Run all algorithms with given data. */
	public static void runAll() {
		System.out.println(EvaluationMetrics.printTitle() + "\tTrain Time\tTest Time");
		
		Constant constant = new Constant(userCount, itemCount, maxValue, minValue, 3.0);
		System.out.println(testRecommender("Const", constant));
		
		Average average = new Average(userCount, itemCount, maxValue, minValue);
		System.out.println(testRecommender("AllAvg", average));
		
		UserAverage userAverage = new UserAverage(userCount, itemCount, maxValue, minValue, userRateAverage);
		System.out.println(testRecommender("UserAvg", userAverage));
		
		ItemAverage itemAverage = new ItemAverage(userCount, itemCount, maxValue, minValue, itemRateAverage);
		System.out.println(testRecommender("ItemAvg", itemAverage));
		
		Random random = new Random(userCount, itemCount, maxValue, minValue);
		System.out.println(testRecommender("Random", random));
		
		UserBased userBsd = new UserBased(userCount, itemCount, maxValue, minValue, 50, MemoryBasedRecommender.PEARSON_CORR, false, 0.0, userRateAverage, userSimilarityPrefetch, dataFileName + "_userSim.txt");
		System.out.println(testRecommender("UserBsd", userBsd));
		
		UserBased userDft = new UserBased(userCount, itemCount, maxValue, minValue, 50, MemoryBasedRecommender.PEARSON_CORR, true, (maxValue + minValue) / 2, userRateAverage, userSimilarityPrefetch, dataFileName + "_userSim.txt");
		System.out.println(testRecommender("UserDft", userDft));
		
		ItemBased itemBsd = new ItemBased(userCount, itemCount, maxValue, minValue, 50, MemoryBasedRecommender.PEARSON_CORR, false, 0.0, itemRateAverage, itemSimilarityPrefetch, dataFileName + "_itemSim.txt");
		System.out.println(testRecommender("ItemBsd", itemBsd));
		
		ItemBased itemDft = new ItemBased(userCount, itemCount, maxValue, minValue, 50, MemoryBasedRecommender.PEARSON_CORR, true, (maxValue + minValue) / 2, itemRateAverage, itemSimilarityPrefetch, dataFileName + "_itemSim.txt");
		System.out.println(testRecommender("ItemDft", itemDft));
		
		SlopeOne slope1 = new SlopeOne(userCount, itemCount, maxValue, minValue);
		System.out.println(testRecommender("Slope1", slope1));
		
		RegularizedSVD regsvd = new RegularizedSVD(userCount, itemCount, maxValue, minValue, 10, 0.005, 0.1, 0, 200, false);
		System.out.println(testRecommender("RegSVD", regsvd));
		
		NMF nmf = new NMF(userCount, itemCount, maxValue, minValue, 4, 0, 0.001, 0, 200, 0.005, false);
		System.out.println(testRecommender("NMF", nmf));
		
		PMF pmf = new PMF(userCount, itemCount, maxValue, minValue, 10, 50, 0.4, 0.8, 200, false);
		System.out.println(testRecommender("PMF", pmf));
		
		BayesianPMF bpmf = new BayesianPMF(userCount, itemCount, maxValue, minValue, 2, 0, 0, 0, 20, false);
		System.out.println(testRecommender("BPMF", bpmf));
		
		NonlinearPMF nlpmf = new NonlinearPMF(userCount, itemCount, maxValue, minValue, 10, 0.0001, 0.9, 2, 1, 1, 0.11, 5);
		System.out.println(testRecommender("NLPMF", nlpmf));
		
		FastNPCA npca = new FastNPCA(userCount, itemCount, maxValue, minValue, 0.15, 50);
		System.out.println(testRecommender("NPCA", npca));
		
		RankBased rnkMean = new RankBased(userCount, itemCount, maxValue, minValue, 0.02, RankBased.MEAN_LOSS);
		System.out.println(testRecommender("RnkMean", rnkMean));
		
		RankBased rnkAsym = new RankBased(userCount, itemCount, maxValue, minValue, 0.02, RankBased.ASYMM_LOSS);
		System.out.println(testRecommender("RnkAsym", rnkAsym));
		
		RegularizedSVD baseline = new RegularizedSVD(userCount, itemCount, maxValue, minValue, 10, 0.005, 0.1, 0, 200, false);
		baseline.buildModel(rateMatrix);

		int[] globalRank = {1, 3, 5};
		for (int r : globalRank) {
			SingletonGlobalLLORMA sgllorma = new SingletonGlobalLLORMA(userCount, itemCount, maxValue, minValue, r, 0.1, 0.001, 100, 50, KernelSmoothing.EPANECHNIKOV_KERNEL, 0.8, 0.01, baseline, true);
			System.out.println(testRecommender("sgLLR" + r, sgllorma));
		}
		
		int[] localRank = {1, 3, 5, 7, 10};
		for (int r : localRank) {
			SingletonParallelLLORMA spllorma = new SingletonParallelLLORMA(userCount, itemCount, maxValue, minValue, r, 0.01, 0.001, 100, 50, KernelSmoothing.EPANECHNIKOV_KERNEL, 0.8, baseline, testMatrix, MULTI_THREAD_LEVEL, false);
			System.out.println(testRecommender("spLLR" + r, spllorma));
		}
	}
	
	
	
	/*========================================
	 * Reading drug-drug similarity and disease-disease similarity files
	 *=======================================*/
	/**
	 * Added by Sudipta
	 * Remove missings drugs from drug-disease.tsv
	 */
	private static void removeMissingDrugDisease(String drugFileName, String diseaseFileName,
			String drugDiseaseFileName){
		try{
			
			BufferedReader bufferDrug = new BufferedReader(new InputStreamReader(new FileInputStream(drugFileName)));
			BufferedReader bufferDisease = new BufferedReader(new InputStreamReader(new FileInputStream(diseaseFileName)));
			BufferedReader bufferDrugDisease = new BufferedReader(new InputStreamReader(new FileInputStream(drugDiseaseFileName)));
			
			ArrayList<String> drugList = new ArrayList<String>();
			ArrayList<String> diseaseList = new ArrayList<String>();
			ArrayList<String> drugDiseaseList = new ArrayList<String>();
			String[] drugs = bufferDrug.readLine().split(",");
			String[] diseases = bufferDisease.readLine().split(",");
			for(int dIdx=1; dIdx< drugs.length; dIdx++){
				drugList.add(drugs[dIdx].toString());
			}
			for(int dIdx=1; dIdx< diseases.length; dIdx++){
				diseaseList.add(diseases[dIdx].replace("'", ""));
			}
			
			while(true){
				String line = bufferDrugDisease.readLine();
				if(line==null){
					break;
				}
				drugDiseaseList.add(line);
			}
			
			ArrayList<String> drugDiseaseWrite = new ArrayList<String>();
			for(String drugDiseaseEntry : drugDiseaseList){
				String drug = drugDiseaseEntry.split("\t")[0].trim();
				String disease = drugDiseaseEntry.split("\t")[1].trim();				
				if(drugList.contains(drug) && diseaseList.contains(disease)){
					drugDiseaseWrite.add(drugDiseaseEntry);					
				}
			}
			//rewrite the drug-disease.tsv file.
			PrintWriter pw = new PrintWriter(new FileOutputStream("drug-diseaseNew.tsv"));
			for(String drugDiseaseEntry : drugDiseaseWrite){
				pw.write(drugDiseaseEntry);
				pw.write("\n");
			}
			pw.close();
		}catch(Exception e){
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	/*========================================
	 * Reading drug-drug similarity and disease-disease similarity files
	 *=======================================*/
	/**
	 * Added by Sudipta
	 * Read the drug-drug and disease-disease similarity files and store the info into a matrices.
	 */
	private static void readSimilarityFiles(String drugSimfileName, String diseaseSimFileName){
		try{	
			/** Read the drug file now*/
			BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream(drugSimfileName)));			
			drugSim = new Double[userCount][userCount];	
			diseaseSim = new Double[itemCount][itemCount];	
			//String drugList[] = buffer.readLine().split(",");
			String[] drugList = buffer.readLine().split(",");
			
			for(int dListIdx=1; dListIdx< drugList.length; dListIdx++){
				if(drugList[dListIdx].length()==0){
					if(dListIdx==0){
						//do nothing
					}else{
						throw new Exception("Length of Drug Name is zero. Drug Name cannot be empty");
					}					
				}else{
					drugNameToIndexMap.put(drugList[dListIdx],dListIdx-1);
					indexToDrugNameMap.put(dListIdx-1,drugList[dListIdx]);
				}
			}
			int drugXIdx=0;
			while(true){
				String drugXStr = buffer.readLine();
				if(drugXStr==null){
					break;
				}
				String drugXList[] = drugXStr.split(",");
				if(userCount!=(drugXList.length-1)){
					String errMsg = "Error reading drug similarity file. Number of drugs should be equal to the number comma separated records in each line";
					Exception exp = new Exception(errMsg);
					throw exp;
				}
				for(int drugYIdx=1; drugYIdx<= drugXList.length && drugYIdx<=userCount; drugYIdx++){					
					drugSim[drugXIdx][drugYIdx-1] = Double.parseDouble(drugXList[drugYIdx]);
					
				}
				drugXIdx++;
			}
			
			/** Read the disease file now*/
			buffer = new BufferedReader(new InputStreamReader(new FileInputStream(diseaseSimFileName)));
			String diseaseList[] = buffer.readLine().split(",");			
			for(int dListIdx=1; dListIdx< diseaseList.length; dListIdx++){
				if(diseaseList[dListIdx].length()==0){
					if(dListIdx==0){
						//do nothing
					}else{
						throw new Exception("Length of Disease Name is zero. Disease Name cannot be empty");
					}					
				}else{
					diseaseNameToIndexMap.put(diseaseList[dListIdx].replace("'", ""),dListIdx-1);
					indexToDiseaseNameMap.put(dListIdx-1,diseaseList[dListIdx].replace("'", ""));
				}
			}
			int diseaseXIdx=0;
			while(true){
				String diseaseXStr = buffer.readLine();
				if(diseaseXStr==null){
					break;
				}
				String diseaseXList[] = diseaseXStr.split(",");
				
				if(itemCount!=(diseaseXList.length-1)){
					String errMsg = "Error reading disease similarity file. Number of diseases should be equal to the number comma separated records in each line";
					Exception exp = new Exception(errMsg);
					throw exp;
				}
				for(int diseaseYIdx=1; diseaseYIdx<= diseaseXList.length && diseaseYIdx<=itemCount; diseaseYIdx++){
					diseaseSim[diseaseXIdx][diseaseYIdx-1] = Double.parseDouble(diseaseXList[diseaseYIdx]);
				}
				diseaseXIdx++;
			}			
			
		}catch(IOException ioe){
			System.out.println ("No such file: " + ioe);
			System.exit(0);
		}catch(Exception exp){
			System.out.println(exp.getMessage());
			System.exit(0);
		}		
	}
	
	/*========================================
	 * Convert the drug-disease file to be readable by readArff method
	 *=======================================*/
	/**
	 * Added by Sudipta
	 * Read the data file in ARFF format, and store it in rating matrix.
	 * Peripheral information such as max/min values, user/item count are also set in this method.
	 * 
	 * @param fileName The name of data file.
	 */
	private static void convertToArffFormat(String fileName){
		try{
			FileInputStream stream = new FileInputStream(fileName);
			InputStreamReader reader = new InputStreamReader(stream);
			BufferedReader buffer = new BufferedReader(reader);
			
			PrintWriter pw = new PrintWriter(new FileOutputStream("drugDisease.arff"));			
			
			ArrayList<String> records = new ArrayList<String>();			
			ArrayList<String> recOut = new ArrayList<String>();
			ArrayList<String> recDataOut = new ArrayList<String>();
			
			//initialize recDataOut
			for(int userCountIdx=0; userCountIdx< userCount; userCountIdx++){
				recDataOut.add(new String());
			}
			
			
			Set<String> diseaseSet = new HashSet<String>();
			Set<String> drugSet = new HashSet<String>();			
			
			HashMap<String, String> drugDiseaseMap = new HashMap<String, String>();
			
			String line;
			String attribute= "@ATTRIBUTE";
			String numeric = "NUMERIC\n";
			String drugDiseaseRate = "1";
			
			recOut.add("@RELATION drugdisease");
			recOut.add("\n");
			recOut.add("@ATTRIBUTE DrugId STRING\n");
			
			while(true){
				String drugDisease = buffer.readLine();
				if(drugDisease==null){
					break;
				}
				String[] drugDiseaseTuple = drugDisease.split("\t");
				if(drugDiseaseMap.containsKey(drugDiseaseTuple[0])){
					String value = drugDiseaseMap.get(drugDiseaseTuple[0]);
					String valueNew = value + "," + drugDiseaseTuple[1];
					drugDiseaseMap.remove(drugDiseaseTuple[0]);
					drugDiseaseMap.put(drugDiseaseTuple[0], valueNew);
					
				}else{
					drugDiseaseMap.put(drugDiseaseTuple[0], drugDiseaseTuple[1]);
				}
				diseaseSet.add(drugDiseaseTuple[1].trim());				
				drugSet.add(drugDiseaseTuple[0].trim());				
			}
			/*
			HashSet<String> drugSet1 = new HashSet<String>();
			for(String str : drugList){
				drugSet1.add(str);
			}
			for(String str : drugSet1){
				if(!drugSet.contains(str)){
					System.out.println(str);
				}
			}
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			for(String str : drugSet){
				if(!drugSet1.contains(str)){
					System.out.println(str);
				}
			}
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			System.out.println(drugDiseaseMap.size());
			System.out.println("Total Number of Drugs : " + drugSet.size());
			System.out.println("Total Number of Diseases : " + diseaseSet.size());*/
					
			
			int index=1;
			for(String setRec : diseaseSet){
				recOut.add(attribute + " '" + setRec+ "["+ index + "]" + "' " + numeric);
				index++;
			}
			index=2;
			for(String setRec : diseaseSet){
				recOut.add(attribute + " '" + setRec+ "["+ index + "]" + "' " + numeric);
				index++;
			}
			
			recOut.add("@DATA\n");
			
			for(int drugSetIdx=0; drugSetIdx< drugSet.size(); drugSetIdx++){
				if(drugSetIdx==drugSet.size()-1){
					System.out.println();
				}
				String drugName = indexToDrugNameMap.get(drugSetIdx);
				int drugIndex = drugNameToIndexMap.get(drugName);				
				String diseaseListStr[] = drugDiseaseMap.get(drugName).split(",");
				StringBuilder arffRec = new StringBuilder();	
				arffRec.append("{");
				arffRec.append("0 ");
				arffRec.append(drugIndex);
				arffRec.append(",");
				for(int diseaseIdx=0; diseaseIdx< diseaseListStr.length; diseaseIdx++){
					if(diseaseNameToIndexMap.get(diseaseListStr[diseaseIdx])==0){
						System.out.println();
					}
					arffRec.append(diseaseNameToIndexMap.get(diseaseListStr[diseaseIdx])+1);
					arffRec.append(" ");
					arffRec.append(drugDiseaseRate);
					arffRec.append(",");
				}
				arffRec.deleteCharAt(arffRec.length()-1);	
				arffRec.append("}\n");
				//need to be added to appropriate index which is the index of drug
				recDataOut.add(drugIndex, arffRec.toString());
			}
			recOut.addAll(recDataOut);
			recDataOut.clear();
			for(int recOutIdx=0; recOutIdx< recOut.size(); recOutIdx++){
				System.out.println(recOut.get(recOutIdx));
				pw.write(recOut.get(recOutIdx));
			}	
			pw.close();
			
			/**
			 * Just for testing purpose
			 */
			/*
			stream = new FileInputStream("/home/sudipta/Documents/Research_With_Dr Wang/IBM_UConn_Collaboration/Drug_Disease/disease-sim.csv");
			reader = new InputStreamReader(stream);
			buffer = new BufferedReader(reader);			
			Set<String> drugSet1 = new HashSet<String>();
			String[] drugs = buffer.readLine().split(","); 
			for(String drug : drugs){
				drugSet1.add(drug);
			}
			System.out.println("Testing the length here : " + drugSet1.size());
			for(String drug : diseaseSet){
				if(!drugSet1.contains(drug)){
					System.out.println(drug);
				}
			}
			*/
			
		}catch(IOException ioe) {
			System.out.println ("No such file: " + ioe);
			System.exit(0);
		}
	}	
	
	
	/*========================================
	 * File I/O
	 *========================================*/
	/**
	 * Read the data file in ARFF format, and store it in rating matrix.
	 * Peripheral information such as max/min values, user/item count are also set in this method.
	 * 
	 * @param fileName The name of data file.
	 */
	private static void readArff(String fileName) {
		try {
			FileInputStream stream = new FileInputStream(fileName);
			InputStreamReader reader = new InputStreamReader(stream);
			BufferedReader buffer = new BufferedReader(reader);
			
			ArrayList<String> tmpColumnName = new ArrayList<String>();
			
			String line;
			int userNo = 0; // sequence number of each user
			int attributeCount = 0;
			
			maxValue = -1;
			minValue = 99999;
			
			// Read attributes:
			while((line = buffer.readLine()) != null && !line.equals("TT_EOF")) {
				if (line.contains("@ATTRIBUTE")) {
					String name;
					//String type;
					
					line = line.substring(10).trim();
					if (line.charAt(0) == '\'') {
						int idx = line.substring(1).indexOf('\'');
						name = line.substring(1, idx+1);
						//type = line.substring(idx+2).trim();
					}
					else {
						int idx = line.substring(1).indexOf(' ');
						name = line.substring(0, idx+1).trim();
						//type = line.substring(idx+2).trim();
					}
					
					//columnName[lineNo] = name;
					tmpColumnName.add(name);
					attributeCount++;
				}
				else if (line.contains("@RELATION")) {
					// do nothing
				}
				else if (line.contains("@DATA")) {
					// This is the end of attribute section!
					break;
				}
				else if (line.length() <= 0) {
					// do nothing
				}
			}
			
			// Set item count to data structures:
			itemCount = (attributeCount - 1)/2;
			columnName = new String[attributeCount];
			tmpColumnName.toArray(columnName);
			
			int[] itemRateCount = new int[itemCount+1];
			rateMatrix = new SparseMatrix(500000, itemCount+1); // Netflix: [480189, 17770]
			testMatrix1 = new SparseMatrix(500000, itemCount+1); // Netflix: [480189, 17770]
			
			// Read data:
			while((line = buffer.readLine()) != null && !line.equals("TT_EOF")) {
				if (line.length() > 0) {
					line = line.substring(1, line.length() - 1);
					
					StringTokenizer st = new StringTokenizer (line, ",");
					
					while (st.hasMoreTokens()) {
						String token = st.nextToken().trim();
						int i = token.indexOf(" ");
						
						int movieID, rate;
						int index = Integer.parseInt(token.substring(0, i));
						String data = token.substring(i+1);
						
						if (index == 0) { // User ID
							//int userID = Integer.parseInt(data);
							
							userNo++;
						}
						else if (data.length() == 1) { // Rate
							movieID = index;
							rate = Integer.parseInt(data);
							
							if (rate > maxValue) {
								maxValue = rate;
							}
							else if (rate < minValue) {
								minValue = rate;
							}
							
							(itemRateCount[movieID])++;
							rateMatrix.setValue(userNo, movieID, rate);
							testMatrix1.setValue(userNo, movieID, rate);
						}
						else { // Date
							// Do not use
						}
					}
				}
			}
			if(userCount != userNo){
				throw new Exception("Drug Count does not match!!!");
			}
			userCount = userNo;
			
			// Reset user vector length:
			rateMatrix.setSize(userCount+1, itemCount+1);
			testMatrix1.setSize(userCount+1, itemCount+1);
			for (int i = 1; i <= itemCount; i++) {
				rateMatrix.getColRef(i).setLength(userCount+1);
				testMatrix1.getColRef(i).setLength(userCount+1);
			}
			
			System.out.println ("Data File\t" + dataFileName);
			System.out.println ("User Count\t" + userCount);
			System.out.println ("Item Count\t" + itemCount);
			System.out.println ("Rating Count\t" + rateMatrix.itemCount());
			System.out.println ("Rating Density\t" + String.format("%.2f", ((double) rateMatrix.itemCount() / (double) userCount / (double) itemCount * 100.0)) + "%");
			
			stream.close();
		}
		catch (IOException ioe) {
			System.out.println ("No such file: " + ioe);
			System.exit(0);
		}catch(Exception e){
			System.out.println(e.getMessage());
			System.exit(0);
		}
	}
	
	
	private static void saveSerializedFile(Object obj, String fileName) {
		try {
			FileOutputStream f = new FileOutputStream(fileName); 
			ObjectOutput s = new ObjectOutputStream(f); 
			s.writeObject(obj);
			s.flush();
			f.close();
		}
		catch(IOException e) {
			System.out.println(e);
		}
	}
	
	private static Object readSerializedFile(String fileName) {
		try {
			FileInputStream f = new FileInputStream(fileName);
			ObjectInput s = new ObjectInputStream(f);
			Object obj = s.readObject();
			
			f.close();
			
			return obj;
		}
		catch(IOException e) {
			System.out.println("IO Exception occured: " + e);
			return null;
		}
		catch(ClassNotFoundException e) {
			System.out.println("Class not found: " + e);
			return null;
		}
	}
}