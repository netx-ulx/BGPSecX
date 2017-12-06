/*
 * BGPSecXMixedChart -  Oct/2017
 * 
 * Plot results of OA, PV, PEV per RRC in the same image. 
 * Uses as input a set of CSV files defined from a configuration file..
 * 
 * Authors: NETX-ULX Team
 * 
 */
package netx.ulx;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
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
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class BgpSecXMixedChart extends ApplicationFrame {

	public static String appVersion = "1.0.0_b20171009-00";
	public static String appName = "BGPSecX-MixChart";
	private static final long serialVersionUID = 1L;
	private final static String chartDir = System.getProperty("user.dir") + "/chart/";
	public final static String charBold = "\033[0;1m";
	public final static String charNormal = "\033[0;0m";
	final int widthImg = 1360;
	final int heightImg = 720;
	public static int totalDatasets;
	public static int counet;
	public static int start; 
	public static int end; 
	private static int totSamples;
	public static List<Long> dataList = new ArrayList<Long>(); 
	static String[] categoryGrp = { "OA", "PEV", "PV" };
	public static ArrayList<String> datasetList = new ArrayList<String>();
	static HashMap<String, List<Double>> dataResult = new HashMap<>();
	static String[] percentGrp = { "1", "10", "25", "50", "75", "100" };
	static String[] datasetGrp = { "RRC00", "RRC01", "RRC03", "RRC04", "RRC05", "RRC06", "RRC07", "RRC10", "RRC11",
			"RRC12", "RRC13", "RRC14", "RRC15", "RRC16", "RRC18", "RRC19", "RRC20", "RRC21" };
	
	public static void main(final String[] args) {
		// Flag to skip csv header
		int countLines = 0;
		ArrayList<String> aListCsv = null;
		if (args.length == 1) {
			// Read file which contains path of the datafiless
			try (Stream<String> linesPath = Files.lines(Paths.get(args[0]), StandardCharsets.ISO_8859_1)) {
				for (String linePath : (Iterable<String>) linesPath::iterator) {
					datasetList.add(linePath);
					ArrayList<String> aList = new ArrayList<String>(Arrays.asList(linePath.split(";")));
					// Count the type of validation
					for (int i = 0; i < 3; i++) {
						String dataFile = aList.get(0) + categoryGrp[i].toLowerCase() + ".csv";
						// Open dataset file
						Stream<String> linesCsv = Files.lines(Paths.get(dataFile), StandardCharsets.ISO_8859_1);
						int countPercent = 0;
						for (String lineCsv : (Iterable<String>) linesCsv::iterator) {
							// Skip CSV header
							if (countLines > 0 && countLines < 7) {
								aListCsv = new ArrayList<String>(Arrays.asList(lineCsv.split(" ")));
								totSamples = aListCsv.size();
								dataList = new ArrayList<Long>();
								for (int j = 3; j < totSamples; j++) {
									dataList.add(Long.valueOf(aListCsv.get(j)));
								}
								dataResult.put(aList.get(1) + categoryGrp[i] + percentGrp[countPercent],
										convList(dataList, Long.valueOf(aListCsv.get(0))));
								countPercent++;
							}
							countLines++;
						}
						countLines = 0;
					}

					// Get filename without extension and put a new
					int beginFileName = aList.get(0).lastIndexOf('/') + 1;
					int endFileName = aList.get(0).lastIndexOf('_');
					String imgName = chartDir + aList.get(0).substring(beginFileName, endFileName) + "_oa_pev_pv.png";
					final BgpSecXMixedChart plotChart = new BgpSecXMixedChart(
							"Validation of OA, PV and PEV by BGPSecX from " + aList.get(1), "",
							"Percentage based in " + aListCsv.get(0) + " updates", imgName, aList.get(1));
					plotChart.pack();
					RefineryUtilities.centerFrameOnScreen(plotChart);
					plotChart.setVisible(true);
					dataResult.clear();
				}
			} catch (IOException e) {
				System.out.println("I/O error, check the path/filename.");
				System.exit(0);
			}
			// if args.length
		} else {
			System.out.println(charBold + appName + ", v" + appVersion);
			System.out.println("\nSintaxe:" + charNormal + " java -jar BGPSecXGroupedChart.jar <cfg_path>");
			System.exit(0);
		}
	}

	@SuppressWarnings("deprecation")
	public BgpSecXMixedChart(final String title, String xLabel, String yLabel, String imgFilename,
			String group) {
		super(title);
		Font xFont = new Font("Arial", Font.BOLD, 17); // Categoria no eixo X
		Font yFont = new Font("Arial", Font.BOLD, 16); // Valores do eixo Y
		Font yFontLabel = new Font("Arial", Font.BOLD, 18); // Label do eixo Y
		Font titleFont = new Font("Arial", Font.BOLD, 20); // Title
		final DefaultBoxAndWhiskerCategoryDataset dataset = createSampleDataset(group);
		final CategoryAxis xAxis = new CategoryAxis(xLabel);
		final NumberAxis yAxis = new NumberAxis(yLabel);
		yAxis.setAutoRangeIncludesZero(false);
		xAxis.setTickLabelFont(xFont);
		yAxis.setTickLabelFont(yFont);
		yAxis.setLabelFont(yFontLabel);
		final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		renderer.setFillBox(true);
		renderer.setBaseItemLabelsVisible(true);
		renderer.setMedianVisible(true);
		renderer.setMeanVisible(true);
		renderer.setToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
		final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
		CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setLowerMargin(0.03);
		domainAxis.setUpperMargin(0.03);
		domainAxis.setCategoryMargin(0.03);
		plot.setDomainAxis(domainAxis);
		CategoryItemRenderer rendererBox = ((CategoryPlot) renderer.getPlot()).getRenderer();
		DecimalFormat pctFormat = new DecimalFormat("0.00%");
		pctFormat.setMultiplier(1);
		// Format fields of values
		rendererBox.setSeriesItemLabelGenerator(0, new StandardCategoryItemLabelGenerator("{0}", pctFormat));
		rendererBox.setSeriesItemLabelGenerator(1, new StandardCategoryItemLabelGenerator("{2}", pctFormat));
		rendererBox.setSeriesItemLabelGenerator(2, new StandardCategoryItemLabelGenerator("{3}", pctFormat));
		rendererBox.setSeriesItemLabelGenerator(3, new StandardCategoryItemLabelGenerator("{4}", pctFormat));
		// Put value in each bar
		rendererBox.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		rendererBox.setBaseItemLabelsVisible(true);
		rendererBox.setItemLabelFont(new Font("SansSerif", Font.BOLD, 16));
		final JFreeChart chart = new JFreeChart(title, titleFont, plot, true);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(widthImg, heightImg));
		setContentPane(chartPanel);
		writeAsPNG(chart, imgFilename);
	}

	private DefaultBoxAndWhiskerCategoryDataset createSampleDataset(String group) {

		final int percentCount = percentGrp.length;
		final int categoryCount = categoryGrp.length;

		final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
		for (int i = 0; i < categoryCount; i++) {
			for (int j = 0; j < percentCount; j++) {
				dataset.add(dataResult.get(group + categoryGrp[i] + percentGrp[j]), percentGrp[j] + "%",
						categoryGrp[i]);
			}
		}
		return dataset;
	}

	public void writeAsPNG(JFreeChart chart, String filename) {
		try {
			final OutputStream out = new FileOutputStream(filename);
			BufferedImage chartImage = chart.createBufferedImage(widthImg, heightImg, null);
			ImageIO.write(chartImage, "png", out);
			System.out.println("Created the PNG file: " + filename);
			out.close();
		} catch (Exception e) {
			System.out.println(e.getMessage().toString());
		}
	}

	// Convert raw values in percentage values
	public static List<Double> convList(List<Long> listLong, Long totUpdates) {
		List<Double> doubleList = new ArrayList<Double>();
		for (int i = 0; i < listLong.size(); i++) {
			doubleList.add((double) listLong.get(i) / totUpdates * 100);
		}
		return doubleList;
	}
}