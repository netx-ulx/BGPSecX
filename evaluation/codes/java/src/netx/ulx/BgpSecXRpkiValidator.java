/*
 * BGPSecX RPKI Validator -  Jan/2017
 * 
 * Utility to make RPKI validation 
 * Uses as input RIPE/RIS datasets and a JSON file
 * exported from a RIPE RPKI Validation tools 
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;

public class BgpSecXRpkiValidator {

	private final static String appVersion = "1.0.0-b20171015-01";
	private final static String appName = "BGPSecX-RPKIValidator";
	private final static String csvDir = System.getProperty("user.dir") + "/csv";
	private final static String charBold = "\033[0;1m";
	private final static String charNormal = "\033[0;0m";
	private final static int resultInterval = 5000;
	private static HashMap<String, List<Long>> sampleResult = new HashMap<>();
	private static HashMap<String, Long> tb = new HashMap<>();
	private static ArrayList<String> resultsGnuPlot = new ArrayList<String>();
	private static String datasetDesc = null;
	private static String fileName = null;
	private static String debug;
	private static int countUptd = 0; // Update count
	private static long countQueries = 0; // Total of validations
	private static String roaDataset = null;
	private static Instant startTime;
	private static Instant overallTime;
	private static int datasetCount = 0;

	public static void main(String[] args) {
		//verifyRoa("0", "0");
		//System.exit(0);
		if (args.length > 0) {
			initMap();
			resultsGnuPlot.clear();
			System.out.println(charBold + appName + ", v" + appVersion);
			System.out.println(charBold + "Current time: " + charNormal + Instant.now());
			overallTime = Instant.now();
			try (Stream<String> lines = Files.lines(Paths.get(args[0]), StandardCharsets.ISO_8859_1)) {
				for (String line : (Iterable<String>) lines::iterator) {
					sampleResult.clear();
					if (roaDataset == null) {
						roaDataset = line;
					} else {
						// Read path of datafiles and its description
						ArrayList<String> aList = new ArrayList<String>(Arrays.asList(line.split(";")));
						String datasetPath = new String(aList.get(0));
						datasetDesc = aList.get(1);
						processData(datasetPath);
						datasetCount++;
					}
				}
			} catch (IOException e) {
				printFileNotFound(args[0]);
			}
		} else {
			printSintax();
		}

		writeCsvGnuPlot();

		System.out.println(charBold + "Fineshed! Overall datasets processing time: " + charNormal
				+ ChronoUnit.MINUTES.between(overallTime, Instant.now()) + "m");
	}

	public static void processData(String filename) {
		countUptd = 0; // Updates count
		countQueries = 0; // Total of queries (or validations)
		initMap();
		int count2 = 0; // Auxiliary counter for each 5000 updates
		String valResult = null;
		try (Stream<String> lines = Files.lines(Paths.get(filename), StandardCharsets.ISO_8859_1)) {
			startTime = Instant.now();
			System.out.println(charBold + "Routing dataset in use: " + charNormal + filename);
			System.out.println(charBold + "ROA dataset in use: " + charNormal + roaDataset);
			count2 = 0; // Control for show results in each 1000 updates
			for (String line : (Iterable<String>) lines::iterator) {
				// Put in aList fields separated by space
				ArrayList<String> aList = new ArrayList<String>(Arrays.asList(line.split(" ")));
				for (int i = 1; i < aList.size(); i++) {
					countQueries++; // Count number of prefixes (or validations)
					valResult = verifyRoa(aList.get(0), aList.get(i));
					switch (valResult) {
					case "nf":
						tb.put("nf", tb.get("nf") + 1);
						break;
					case "v":
						tb.put("v", tb.get("v") + 1);
						break;
					case "i":
						tb.put("i", tb.get("i") + 1);
						break;
					}
				}
				countUptd++;
				if ((countUptd - count2) == resultInterval) {
					printResult("");
					count2 = countUptd;
				}
			}
			printResult(filename);
		} catch (IOException e) {
			printFileNotFound(filename);
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Index error, total updates: " + countUptd + ", line_str: " + debug);
			e.printStackTrace();
		}
	}

	public static String verifyRoa(String asn, String prefix) {
		//int countRoas = 0;
		asn = "AS" + asn;
		// System.out.println("To verify, ASN=" + asn + ", Prefix=" + prefix);
		byte[] jsonData;
		try {
			jsonData = Files.readAllBytes(Paths.get(roaDataset));
			JsonParser jsonParser = new JsonFactory().createParser(jsonData);
			while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
				//countRoas++;
				String name = jsonParser.getCurrentName();
				// Math ASN
				if ("asn".equals(name)) {
					// Get prefix value
					jsonParser.nextToken();
					if (jsonParser.getText().equals(asn)) {
						jsonParser.nextToken();
						jsonParser.nextToken();
						String jsonPrefix = jsonParser.getText();
						int jsonPrefixMask = Integer
								.valueOf(jsonPrefix.substring(jsonPrefix.indexOf("/") + 1, jsonPrefix.length()));
						String jsonIpPrefix = jsonPrefix.substring(0, jsonPrefix.indexOf("/"));
						// Get bits length
						jsonParser.nextToken();
						jsonParser.nextToken();
						int jsonMaskLength = jsonParser.getValueAsInt();
						int rxPrefixMask = Integer.valueOf(prefix.substring(prefix.indexOf("/") + 1, prefix.length()));
						String rxIpPrefix = prefix.substring(0, prefix.indexOf("/"));
						// Math IP address received with the static ROA
						if (jsonIpPrefix.equals(rxIpPrefix)) {
							// Math prefix mask
							if (rxPrefixMask == jsonPrefixMask
									|| (rxPrefixMask > jsonPrefixMask && rxPrefixMask <= jsonMaskLength)) {
								return "v";
							} else {
								return "i";
							}
						}
					}
				}
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			printFileNotFound(roaDataset);
		}
		//System.out.println("Total of ROAs: " + countRoas);
		return "nf";
	}

	public static void printResult(String filename) {
		System.out.println("Total updates: " + countUptd + ", Total of queries: " + countQueries + ", Valid Updates: "
				+ tb.get("v") + ", Invalid updates: " + tb.get("i") + ", Not Found: " + tb.get("nf"));
		// Write csv file if dataset finished
		if (!(filename.equals(""))) {
			// Get filename without extension and put a new
			int beginFileName = filename.lastIndexOf('/') + 1;
			int endFileName = filename.lastIndexOf('.');
			fileName = csvDir + "/" + filename.substring(beginFileName, endFileName) + "_rpki.csv";
			BufferedWriter writeFile = null;
			try {
				checkDir(csvDir);
				writeFile = Files.newBufferedWriter(Paths.get(fileName));
				writeFile.write("tot_updates tot_queries valid invalid notfound");
				writeFile.newLine();
				writeFile.write(
						countUptd + " " + countQueries + " " + tb.get("v") + " " + tb.get("i") + " " + tb.get("nf"));
				writeFile.newLine();
				writeFile.close();
				System.out.println("Created the CSV file: " + fileName);
				System.out.println(charBold + "Dataset processing time: " + charNormal
						+ ChronoUnit.MINUTES.between(startTime, Instant.now()) + "m\n---\n");
				// Add data in list to create csv for gnuplot
				resultsGnuPlot.add(datasetDesc);
				// Add percentage of valid
				resultsGnuPlot.add(Double.toString(Double.parseDouble(tb.get("v").toString()) / countQueries * 100));
				Double invalid = Double.parseDouble(tb.get("i").toString())
						+ Double.parseDouble(tb.get("nf").toString());
				resultsGnuPlot.add(Double.toString(invalid / countQueries * 100));
			} catch (IOException e) {
				printFileNotFound(fileName);
			}
		}
	}

	public static void writeCsvGnuPlot() {
		// int totCsvGroups = Math.round(datasetCount / 6);
		int totCsvGroups = (datasetCount + 6 - 1) / 6;
		int fileNumber = 0;
		BufferedWriter writeFile = null;
		int fieldCont = 0;
		int count = 0;
		fileName = csvDir + "/rpki_gnuplot_" + "1-" + totCsvGroups + ".csv";
		try {
			writeFile = Files.newBufferedWriter(Paths.get(fileName));
			for (int i = 0; i < resultsGnuPlot.size(); i++) {
				writeFile.write(resultsGnuPlot.get(i));
				fieldCont++;
				if (fieldCont == 3) {
					writeFile.newLine();
					fieldCont = 0;
					count++;
				} else {
					writeFile.write(";");
				}
				// Check if already processed six dataset
				if (count == 6 && i < resultsGnuPlot.size() - 1) {
					writeFile.close();
					System.out.println("Created the CSV file fo GNUPlot: " + fileName);
					fileNumber = (i + 19) / 18;
					fileName = csvDir + "/rpki_gnuplot_" + fileNumber + "-" + totCsvGroups + ".csv";
					writeFile = Files.newBufferedWriter(Paths.get(fileName));
					count = 0;
				}
			}
			writeFile.close();
			System.out.println("Created the CSV file fo GNUPlot: " + fileName);
		} catch (IOException e) {
			printFileNotFound(fileName);
		}

	}

	public static void printSintax() {
		System.out.println(charBold + appName + ", v" + appVersion);
		System.out.println("\nSintaxe:" + charNormal + " java -jar BGPSecXRpkiValidator.jar <cfg_path>\n\n" + charBold
				+ "cfg_path" + charNormal + " parameter must be the path/name of configuration file\n");
		System.exit(0);
	}

	public static void printFileNotFound(String name) {
		System.out.println("I/O error, check the path/filename: " + name);
		System.exit(0);
	}

	public static boolean fileExist(String filename) {
		File file;
		try {
			file = new File(filename);
			if (!file.exists()) {
				file.createNewFile();
			}
			return true;
		} catch (IOException e) {
			System.out.print("Error writing in the file: " + filename);
			e.printStackTrace();
		}
		return false;
	}

	public static void checkDir(String dir) {
		File directory = new File(dir);
		if (!directory.exists()) {
			directory.mkdir();
		}
	}

	public static void initMap() {
		tb.clear();
		tb.put("nf", (long) 0); // not found asn/prefix
		tb.put("v", (long) 0); // valid update
		tb.put("i", (long) 0); // invalid update
	}
}