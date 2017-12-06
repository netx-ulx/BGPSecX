/*
 * BGPSecXGroupedChart -  Oct/2017
 * 
 * Plot results of OA, PV and PV validation tests 
 * grouped by type of validation Uses as input a 
 * set of CSV files defined from a configuration file.
 * 
 * Authors: NETX-ULX Team
 * 
 */

package netx.ulx;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class BgpSecXGroupedChart extends ApplicationFrame {
	private static final long serialVersionUID = 1L;
	private final static String appVersion = "1.0.0_b20171020-01";
	private final static String appName = "BGPSecX-ChartPlot";
	private final static String[] categoryGrp = { "OA", "PV", "PEV" };
	private final static String[] chartTitle = { "OA using BGPSecX ", "PV using BGPSecX ", "PEV using BGPSecX " };
	private final static String[] percentGrp = { "1", "10", "25", "50", "75", "100" };
	private final static String chartDir = System.getProperty("user.dir") + "/chart";
	public final static String charBold = "\033[0;1m";
	public final static String charNormal = "\033[0;0m";
	private final int widthImg = 1360;
	private final int heightImg = 720;
	private static int totSamples;
	private static int typeValidation;
	private static int start;
	private static int end;
	private static HashMap<String, List<Double>> dataResult = new HashMap<>();
	private static List<Double> dataList = new ArrayList<Double>();
	private static ArrayList<String> datasetList = new ArrayList<String>();
	// Contains name of each dataset
	public static List<String> datasetGrp = new ArrayList<>();

	public static void main(final String[] args) {
		if (args.length > 2) {
			if (isNumber(args[1]) && isNumber(args[2])) {
				// Type of validation (OA=0, PV=1, PEV=2, All=3)
				int typeVal = Integer.parseInt(args[1]);
				if (typeVal >= 0 && typeVal <= 4) {
					System.out.println(charBold + appName + ", v" + appVersion);
					int first = 0;
					int last = 0;
					if (typeVal == 3) {
						first = 0;
						last = 2;
					} else {
						first = typeVal;
						last = typeVal;
					}
					try (Stream<String> lines = Files.lines(Paths.get(args[0]), StandardCharsets.ISO_8859_1)) {
						for (String line : (Iterable<String>) lines::iterator) {
							datasetList.add(line);
						}
					} catch (IOException e) {
						printFileNotFound(args[0]);
					}
					int totalSets = datasetList.size();
					int setPerChart = Integer.parseInt(args[2]);
					int totalChart = (int) Math.ceil((float) totalSets / (float) setPerChart);

					if (setPerChart < 1 || setPerChart > totalSets) {
						printSintax();
					}
					for (int j = first; j <= last; j++) {
						typeValidation = j;
						datasetGrp.clear();
						for (int i = 1; i <= totalChart; i++) {
							if (i == totalChart && !(totalSets % setPerChart == 0)) {
								createChartValues(setPerChart * i - setPerChart, totalSets - 1);
							} else {
								createChartValues(setPerChart * i - setPerChart, setPerChart * i - 1);
							}
							String numberCharts = "";
							if (totalChart > 1) {
								numberCharts = "(" + i + "/" + totalChart + ")";
							} else {
								numberCharts = "";
							}
							checkDir();
							String imgName = chartDir + "/" + categoryGrp[typeValidation] + i + "-" + totalChart
									+ ".png";
							final BgpSecXGroupedChart plotChart = new BgpSecXGroupedChart(
									chartTitle[typeValidation] + " " + numberCharts, "",
									"% based in the total of prefixes", imgName);
							plotChart.pack();
							RefineryUtilities.centerFrameOnScreen(plotChart);
							plotChart.setVisible(true);
						}
					} // for j
				} else { // if typeValidation
					printSintax();
				}
				// if args.matches
			} else {
				printSintax();
			}
			// if args.length
		} else {
			printSintax();
		}
	}

	@SuppressWarnings("deprecation")
	public BgpSecXGroupedChart(final String title, String xLabel, String yLabel, String imgFilename) {
		super(title);
		Font xFont = new Font("Arial", Font.BOLD, 17);
		Font yFont = new Font("Arial", Font.BOLD, 16);
		Font yFontLabel = new Font("Arial", Font.BOLD, 18);
		final DefaultBoxAndWhiskerCategoryDataset dataset = createSampleDataset();
		final CategoryAxis xAxis = new CategoryAxis(xLabel);
		final NumberAxis yAxis = new NumberAxis(yLabel);
		yAxis.setAutoRangeIncludesZero(false);
		xAxis.setTickLabelFont(xFont);
		yAxis.setTickLabelFont(yFont);
		yAxis.setLabelFont(yFontLabel);
		final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		renderer.setFillBox(true);
		renderer.setMedianVisible(true);
		renderer.setMeanVisible(true);
		renderer.setToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
		final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
		CategoryAxis domainAxis = plot.getDomainAxis();

		domainAxis.setLowerMargin(0.03);
		domainAxis.setUpperMargin(0.03);
		domainAxis.setCategoryMargin(0.0);

		final JFreeChart chart = new JFreeChart(title, new Font("Arial", Font.BOLD, 20), plot, true);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(widthImg, heightImg));
		setContentPane(chartPanel);
		writeAsPNG(chart, imgFilename);

	}

	private DefaultBoxAndWhiskerCategoryDataset createSampleDataset() {
		final int percentCount = percentGrp.length;
		final int categoryCount = categoryGrp.length;
		final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
		for (int n = start; n <= end; n++) {
			for (int i = 0; i < categoryCount; i++) {
				for (int j = 0; j < percentCount; j++) {
					dataset.add(dataResult.get(datasetGrp.get(n) + categoryGrp[typeValidation] + percentGrp[j]),
							percentGrp[j] + "%", datasetGrp.get(n));
				}
			}
		}
		return dataset;
	}

	public void writeAsPNG(JFreeChart chart, String filename) {
		try {
			final OutputStream out = new FileOutputStream(filename);
			BufferedImage chartImage = chart.createBufferedImage(widthImg, heightImg, null);
			ImageIO.write(chartImage, "png", out);
			System.out.println(charBold + "Created PNG file in: " + charNormal + filename);
			out.close();
		} catch (Exception e) {
			System.out.println(e.getMessage().toString());
		}
	}

	public static void createChartValues(int start, int end) {
		BgpSecXGroupedChart.start = start;
		BgpSecXGroupedChart.end = end;
		int countLines = 0;
		int countPercent = 0;
		// Interval of lines to read in dataset file configuration
		for (int i = start; i <= end; i++) {
			ArrayList<String> aList = new ArrayList<String>(Arrays.asList(datasetList.get(i).split(";")));
			// Filename of dataset to precess
			String dataFile = aList.get(0) + "_" + categoryGrp[typeValidation].toLowerCase() + ".csv";
			System.out.println(charBold + "Datafile in use: " + charNormal + dataFile);
			// Name of dataset collector
			datasetGrp.add(aList.get(1));
			// Open dataset file
			try (Stream<String> line = Files.lines(Paths.get(dataFile), StandardCharsets.ISO_8859_1)) {
				for (String line2 : (Iterable<String>) line::iterator) {
					// New dataset, skip header csv
					if (countLines > 0 && countLines < 7) {
						ArrayList<String> aList2 = new ArrayList<String>(Arrays.asList(line2.split(" ")));
						totSamples = aList2.size();
						dataList = new ArrayList<Double>();
						// Scan sample values in column for each line
						for (int j = 3; j < totSamples; j++) {
							if (countLines == 6) {
								// Repeat the same value when in 100%
								dataList.add(Double.valueOf(aList2.get(3)));
							} else {
								dataList.add(Double.valueOf(aList2.get(j)));
							}
						}
						dataResult.put(aList.get(1) + categoryGrp[typeValidation] + percentGrp[countPercent],
								convList(dataList, Long.valueOf(aList2.get(0))));
						countPercent++;
					}
					countLines++;
				}
				countLines = 0;
				countPercent = 0;
			} catch (IOException e) {
				printFileNotFound(dataFile);
			}
		}
	}

	// Convert raw values in percentage values
	public static List<Double> convList(List<Double> listLong, Long totPrefixes) {
		List<Double> doubleList = new ArrayList<Double>();
		for (int i = 0; i < listLong.size(); i++) {
			doubleList.add((double) listLong.get(i) / totPrefixes * 100);
		}
		return doubleList;
	}

	public static void checkDir() {
		File directory = new File(chartDir);
		if (!directory.exists()) {
			directory.mkdir();
		}
	}

	public static void printSintax() {
		System.out.println(charBold + appName + ", v" + appVersion);
		System.out.println("\nSintaxe:" + charNormal
				+ " java -jar BGPSecXPlotGrouped.jar <cfg_path> <type_val> <#data_per_chart>\n\n" + charBold + "First"
				+ charNormal + " parameter must be the path/name of configuration file\n" + charBold + "Second"
				+ charNormal + " one must be the type of validation like 0=OA, 1=PV, 2=PEV or 3=ALL\n" + charBold
				+ "Third" + charNormal + " one must be the number of datasets per chart.\n");
		System.exit(0);
	}

	public static void printFileNotFound(String name) {
		System.out.println("I/O error, check the path/filename: " + name);
		System.exit(0);
	}

	public static boolean isNumber(String arg) {
		if (arg.matches("[+-]?\\d*(\\.\\d+)?")) {
			return true;
		}
		return false;
	}
}