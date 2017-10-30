/*
 * BGPSecX Validator -  Jan/2017
 * 
 * Utility to make validation OA, PV and PV.
 * Uses as input RIPE/RIS datasets and peering from peeringDB project. 
 * Datasets are defined from a configuration file.
 * 
 * Authors: NETX-ULX Team
 * 
 */

package netx.ulx;

import java.io.BufferedWriter;
import java.io.File;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

public class BgpSecXValidator {

	public final static String appVersion = "1.0.0_b201710100-00";
	public final static String appName = "BGPSecX - Dataset validator";
	private final static String csvDir = System.getProperty("user.dir") + "/csv";
	public final static DecimalFormat decFormat = new DecimalFormat("#0.00");
	public final static String[] percentLabels = { "1", "10", "25", "50", "75", "100" };
	public final static String[] categoryLabels = { "oa", "pv", "pev" };
	public final static String charBold = "\033[0;1m";
	public final static String charNormal = "\033[0;0m";
	public static BufferedWriter writeFile;
	private static String peeringFile = null;
	public static File file;
	public static String outFile;
	public static long countQueries = 0;
	public static long countUpdts = 0;
	public static int totSamples = 10;
	public static boolean validSeq;
	public static HashMap<String, List<Long>> sampleResult = new HashMap<>();
	public static HashMap<String, List<Long>> sampleResultP = new HashMap<>();
	public static HashMap<String, Long> tb = new HashMap<>();
	// Contains 4147 ASN imported from a text file.
	// It's a peering dataset or IXP peers members.
	public static HashSet<String> allValidAsn = new HashSet<String>();
	// Contains verified ASNs from verification by the percentage groups
	public static HashSet<String> validAsnFromGroups = new HashSet<String>();
	// Contains ASNs to verification for each percentage group
	public static HashSet<String> asnPoolToValidate = new HashSet<String>();
	// Valid samples result list
	public static List<Long> dataResultV = new ArrayList<Long>();
	// Invalid samples result list
	public static List<Long> dataResultI = new ArrayList<Long>();
	// Partially valid samples result list
	public static List<Long> dataResultP = new ArrayList<Long>();
	public static int totRows;
	public static int validationType;
	public static Instant startTime;
	public static Instant overallTime;

	public static void main(String[] args) {
		allValidAsn.clear();
		if (args.length == 2) {
			// Check if second parameter is a number
			if (args[1].matches("[+-]?\\d*(\\.\\d+)?")) {
				// Type of validation (OA=0, PV=1, PEV=2, All=3)
				validationType = Integer.parseInt(args[1]);
				if (validationType >= 0 && validationType <= 3) {
					int totVal = 1;
					// Make all type of validations
					if (validationType == 3) {
						totVal = 3;
					}
					System.out.println(charBold + appName + ", v" + appVersion);
					overallTime = Instant.now();
					for (int k = 0; k < totVal; k++) {
						allValidAsn.clear();
						peeringFile = null;

						// Make all type of validations
						if (totVal == 3) {
							validationType = k;
						}
						try (Stream<String> lines = Files.lines(Paths.get(args[0]), StandardCharsets.ISO_8859_1)) {
							// Read path of each datafile
							for (String datasetPath : (Iterable<String>) lines::iterator) {
								if (peeringFile == null) {
									peeringFile = datasetPath;
									ImportAsnFromFile();
								} else {
									sampleResult.clear();
									for (int i = 0; i < percentLabels.length; i++) {
										dataResultV = new ArrayList<Long>();
										dataResultI = new ArrayList<Long>();
										dataResultV.clear();
										dataResultI.clear();

										// Make only one sample for 100%
										if (i == percentLabels.length - 1) {
											totSamples = 1;
										} else {
											totSamples = 10;
										}

										for (int j = 1; j <= totSamples; j++) {
											// To do a random of percentage
											// groups for each N samples
											if (buildPercentAsn(Integer.valueOf(percentLabels[i]))) {
												// Process OA dataset
												if (validationType == 0) {
													if (j == 1 && percentLabels[i].equals("1")) {
														printProcInfo(datasetPath, validationType);
													}
													processOa(datasetPath, percentLabels[i], j);
													// Process PV dataset
												} else if (validationType == 1) {
													if (j == 1 && percentLabels[i].equals("1")) {
														printProcInfo(datasetPath, validationType);
													}
													processPv(datasetPath, percentLabels[i], j);
													// Process PEV dataset
												} else if (validationType == 2) {
													if (j == 1 && percentLabels[i].equals("1")) {
														printProcInfo(datasetPath, validationType);
													}
													processPev(datasetPath, percentLabels[i], j);
												}
											} else {
												System.out.println("Error in the copy table process, percent value: "
														+ percentLabels[i]);
											}
											// Contains results list of
											// 10 samples in given percentage
											dataResultV.add(tb.get(percentLabels[i] + "v"));
											// For PV Validation
											if (validationType == 1) {
												dataResultP.add(tb.get(percentLabels[i] + "p"));
											}
										} // For j
										sampleResult.put(percentLabels[i] + "v", dataResultV);
										// For PV Validation
										if (validationType == 1) {
											sampleResultP.put(percentLabels[i] + "p", dataResultP);
										}
									}
									// Send as parameter the name
									// of dataset until dot
									writeData(datasetPath.substring(datasetPath.lastIndexOf('/') + 1,
											datasetPath.indexOf('.')));
								} // for interable
							} // for peeringFile |= null
						} catch (IOException e) {
							printFileNotFound(args[0]);
						}
						validationType++;
					} // For k
				} else {
					printSintax();
				}
			} else {
				printSintax();
			}
			// if args.length
		} else {
			printSintax();
		}
		System.out.println(charBold + "Fineshed! Overall datasets processing time: " + charNormal
				+ ChronoUnit.MINUTES.between(overallTime, Instant.now()) + "m");
	}

	public static void writeData(String filename) {
		try {
			checkDir(csvDir);
			writeFile = Files.newBufferedWriter(
					Paths.get(csvDir + "/" + filename + "_" + categoryLabels[validationType] + ".csv"));
			writeFile.write("tot_queries avg_valid percent_valid s1 s2 s3 s4 s5 s6 s7 s8 s9 s10");
			writeFile.newLine();

			System.out.println("\n--- Results ---");
			for (int i = 0; i < percentLabels.length; i++) {
				dataResultV = sampleResult.get(percentLabels[i] + "v");
				double average = dataResultV.stream().mapToLong(a -> a).average().getAsDouble();
				System.out.println(countUpdts + " " + decFormat.format(average) + " "
						+ decFormat.format((average / countUpdts * 100)) + " "
						+ dataResultV.toString().replaceAll("[\\[\\]\\,]", ""));
				writeFile.write(countUpdts + " " + decFormat.format(average) + " "
						+ decFormat.format((average / countUpdts * 100)) + " "
						+ dataResultV.toString().replaceAll("[\\[\\]\\,]", ""));
				writeFile.newLine();
			}
			writeFile.close();
			System.out.println(charBold + "Created the CSV file in: " + charNormal + csvDir + "/" + filename + "_"
					+ categoryLabels[validationType] + ".csv");
			System.out.println(charBold + "Dataset processing time: " + charNormal
					+ ChronoUnit.MINUTES.between(startTime, Instant.now()) + "m\n---\n");
		} catch (IOException e) {
			printFileNotFound(filename);
		}
	}

	/*
	 * Process Origin Authorization
	 */
	public static void processOa(String dataFile, String percent, int sample) {
		initMap();
		String asnToVerify;
		countQueries = 0; // Total of validation queries
		countUpdts = 0; // Count number of updates
		try (Stream<String> lines = Files.lines(Paths.get(dataFile), StandardCharsets.ISO_8859_1)) {
			for (String line : (Iterable<String>) lines::iterator) {
				ArrayList<String> aList = new ArrayList<String>(Arrays.asList(line.split(" ")));
				countQueries++; // Count numbers of ASNs
				countUpdts = countQueries;
				// Get origin ASN in the AS_PATH
				// asnToVerify = aList.get(aList.size() - 1);
				asnToVerify = aList.get(0);
				// System.out.println("ASNtoVerify: " + asnToVerify + ", TotInPool: " +
				// asnPoolToValidate.size());
				if (asnPoolToValidate.contains(asnToVerify)) {
					tb.put(percent + "v", tb.get(percent + "v") + 1);
					// System.out.println("Update: " + line + ", #Updt: " + (countUpdts + 1));
				} else { // Invalid Update
					tb.put(percent + "i", tb.get(percent + "i") + 1);
				}
			}
			printResult(percent, sample);
		} catch (IOException e) {
			printFileNotFound(dataFile);
		}
	}

	/*
	 * Process Path Validation
	 */
	public static void processPv(String dataFile, String percent, int sample) {
		initMap();
		countQueries = 0; // // Total of validation queries
		countUpdts = 0; // Count number of updates
		int totASNs;
		// There are two valid ASNs in sequence (are valid partially)
		try (Stream<String> lines = Files.lines(Paths.get(dataFile), StandardCharsets.ISO_8859_1)) {
			for (String line : (Iterable<String>) lines::iterator) {
				ArrayList<String> aList = new ArrayList<String>(Arrays.asList(line.split(" ")));
				totASNs = aList.size();
				validSeq = true;
				for (int i = 0; i < totASNs; i++) {
					countQueries++;
					// Get each ASN to verifiy
					if (!(asnPoolToValidate.contains(aList.get(i)))) {
						validSeq = false;
					}
				}
				if (validSeq) {
					tb.put(percent + "v", tb.get(percent + "v") + 1);
					// System.out.println("Update: " + line + ", #Updt: " + (countUpdts + 1));
					// Partial validation
				} else {
					tb.put(percent + "i", tb.get(percent + "i") + 1);
				}
				countUpdts++;
			}
			printResult(percent, sample);
		} catch (IOException e) {
			printFileNotFound(dataFile);
		}
	}

	/*
	 * Process Path End Validation
	 */
	public static void processPev(String dataFile, String percent, int sample) {
		initMap();
		countQueries = 0; // // Total of validation queries
		countUpdts = 0; // Count number of updates
		int lenData, asnInPath;
		try (Stream<String> lines = Files.lines(Paths.get(dataFile), StandardCharsets.ISO_8859_1)) {
			for (String line : (Iterable<String>) lines::iterator) {
				ArrayList<String> aList = new ArrayList<String>(Arrays.asList(line.split(" ")));
				asnInPath = aList.size();
				if (asnInPath > 1) {
					lenData = 2;
				} else {
					lenData = 1;
				}
				validSeq = true;
				// for (int i = (asnInPath - lenData); i < asnInPath; i++) {
				for (int i = 0; i < lenData; i++) {
					// Count number of verifications
					countQueries++;
					if (!(asnPoolToValidate.contains(aList.get(i)))) {
						validSeq = false;
					}
				}
				// Valid PEV (Both ASNs are valid)
				if (validSeq) {
					tb.put(percent + "v", tb.get(percent + "v") + 1);
				} else {
					tb.put(percent + "i", tb.get(percent + "i") + 1);
				}
				countUpdts++;
			}
			printResult(percent, sample);
		} catch (IOException e) {
			printFileNotFound(dataFile);
		}
	}

	/*
	 * Build percentage groups to verification
	 */
	public static boolean buildPercentAsn(int percent) {
		asnPoolToValidate.clear();
		List<String> listAsn = new ArrayList<String>(allValidAsn);
		// Ramdomize all ASNs in list
		Collections.shuffle(listAsn);
		// Defines the number of ASNs from total based in percentage group
		int totItems = (int) Math.round((percent / (double) 100) * allValidAsn.size());
		for (int i = 0; i < totItems; i++) {
			asnPoolToValidate.add(listAsn.get(i));
		}
		return true;
	}

	public static void initMap() {
		// "v" is equal valid and "i" is equal invalid
		tb.clear();
		for (int i = 0; i < percentLabels.length; i++) {
			tb.put(percentLabels[i] + "v", (long) 0);
			tb.put(percentLabels[i] + "i", (long) 0);
		}
	}

	@SuppressWarnings("resource")
	public static void ImportAsnFromFile() {
		String line = null;
		try {
			Scanner file = new Scanner(new File(peeringFile)).useDelimiter("[ˆa-zA-Z]+");
			while (file.hasNext()) {
				line = file.nextLine();
				allValidAsn.add(line);
			}
		} catch (IOException e) {
			printFileNotFound(peeringFile);
		}
	}

	public static void printResult(String percent, int sample) {
		// Partially valid should considered only in PV
		System.out.println(percent + "%" + ", Sample: " + sample + "; Total Updates: " + countUpdts + "; Valid: "
				+ tb.get(percent + "v") + "; Invalid: " + tb.get(percent + "i"));
	}

	public static void checkDir(String dir) {
		File directory = new File(dir);
		if (!directory.exists()) {
			directory.mkdir();
		}
	}

	public static void printSintax() {
		System.out.println(charBold + appName + ", v" + appVersion + charNormal);
		System.out.println("\nSintaxe: java -jar BGPSecXPlotGrouped.jar <cfg_path> <type_val>\n\n" + charBold + "First"
				+ charNormal + " parameter must be the path/name of configuration file (in the "
				+ "configuration file - the first line must be the file path of ASNs and "
				+ "in each of others lines is the path/filename of RIS datasets)\n" + charBold + "Second" + charNormal
				+ " one must be the type of validation like 0=OA, 1=PV, 2=PEV or 3=ALL\n");
		System.exit(0);
	}

	public static void printFileNotFound(String name) {
		System.out.println("I/O error, check the path/filename: " + name);
		System.exit(0);
	}

	public static void printProcInfo(String dataset, int type) {
		System.out.println(charBold + "Processing " + categoryLabels[type].toUpperCase() + " for the datafile: "
				+ charNormal + dataset);
		startTime = Instant.now();
		System.out.println(charBold + "Current time: " + charNormal + startTime);
	}
}