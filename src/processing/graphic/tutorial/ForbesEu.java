package processing.graphic.tutorial;

import static processing.data.Table.FLOAT;
import static processing.data.Table.INT;
import static processing.data.Table.STRING;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gicentre.utils.stat.BarChart;

import processing.core.PApplet;
import processing.core.PFont;
import processing.data.FloatDict;
import processing.data.Table;
import processing.data.TableRow;

public class ForbesEu extends PApplet {
	private static final long serialVersionUID = -762738362468026636L;

	private static final String DELIMITER = ";";
	private static final String HEADLINE_COUNT = "Anzahl EU-Unternehmen im Forbes 2000 für 2013";
	private static final String HEADLINE_MARKET_VALUE = "Market value (in Mio. ) der EU-Staaten im Forbes 2000 für 2013";
	private static final String HEADLINE_RANKING = "Ranking der im Forbes 2000 vertrettene EU-Staaten";
	private static final String SUBHEADLINE_LESS_IS_BETTER = "weniger ist besser";
	private static final String SUBHEADLINE_MORE_IS_BETTER = "mehr ist besser";

	private static final String SOURCE_FILE = "Forbes Global 2000 - 2013.csv";

	private final Set<String> allEuCountries = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("Austria",
			"Belgium", "Bulgaria", "Croatia", "Cyprus", "Czech Republic", "Denmark", "Estonia", "Finland", "France", "Germany",
			"Greece", "Hungary", "Ireland", "Italy", "Latvia", "Lithuania", "Luxembourg", "Malta", "Netherlands", "Poland",
			"Portugal", "Romania", "Slovakia", "Slovenia", "Spain", "Sweden", "United Kingdom")));

	private static final int COMPANY_INDEX = 0;
	private static final int INDUSTRY_INDEX = 1;
	private static final int COUNTRY_INDEX = 2;
	private static final int MARKET_VALUE_INDEX = 3;
	private static final int SALES_INDEX = 4;
	private static final int PROFIT_INDEX = 5;
	private static final int ASSETS_INDEX = 6;
	private static final int RANK_INDEX = 7;
	private static final int FORBES_WEBPAGE_INDEX = 8;

	private String[] data;
	private final Table forbesGlobal = new Table();

	private final FloatDict euCountries = new FloatDict();
	private final FloatDict euMarketValue = new FloatDict();
	private final FloatDict euRanking = new FloatDict();

	private BarChart barChart;
	private PFont titleFont, smallFont;

	public static void main(String args[]) {
		PApplet.main("processing.graphic.tutorial.ForbesEu", new String[] { ARGS_FULL_SCREEN });
	}

	@Override
	public void setup() {
		size(750, 750);
		initFonts();

		acquire();
		parse();
		filterAndMine();

		createBarChart(euCountries);
		noLoop();
	}

	@Override
	public void draw() {
		background(255);
		fill(90);

		if (mouseButton == LEFT) {
			drawHeadlineText(HEADLINE_COUNT, SUBHEADLINE_MORE_IS_BETTER);
		} else if (mouseButton == RIGHT) {
			drawHeadlineText(HEADLINE_MARKET_VALUE, SUBHEADLINE_MORE_IS_BETTER);
		} else if (key == 'r' || key == 'R') {
			drawHeadlineText(HEADLINE_RANKING, SUBHEADLINE_LESS_IS_BETTER);
		} else {
			drawHeadlineText(HEADLINE_COUNT, SUBHEADLINE_MORE_IS_BETTER);
		}

		barChart.draw(40, 40, width - 50, height - 50);
		noLoop();
	}

	@Override
	public void keyPressed() {
		if (key == 'r' || key == 'R') {
			createBarChart(euRanking);
			redraw();
		}
	}

	@Override
	public void mousePressed() {
		if (mouseButton == RIGHT) {
			createBarChart(euMarketValue);
		} else if (mouseButton == LEFT) {
			createBarChart(euCountries);
		}
		redraw();
	}

	private void initFonts() {
		titleFont = createFont("Helvetica", 18);
		smallFont = loadFont("Helvetica-12.vlw");
	}

	private void acquire() {
		data = loadStrings(SOURCE_FILE);
	}

	private void parse() {
		if (data != null) {
			int lineCount = data.length;
			createTableHeader(data[0]);
			for (int i = 1; i < lineCount; i++) {
				forbesGlobal.addRow(replaceBtwColumns(data[i], 3, 8, ',', '.'));
			}
		}
	}

	private void filterAndMine() {
		Iterator<TableRow> rows = forbesGlobal.rows().iterator();
		while (rows.hasNext()) {
			TableRow row = rows.next();

			String country = row.getString(COUNTRY_INDEX);
			if (allEuCountries.contains(country)) {
				euCountries.add(country, 1f);

				Float marketVal = row.getFloat(MARKET_VALUE_INDEX);
				euMarketValue.add(country, marketVal);

				int rank = row.getInt(RANK_INDEX);
				if (euRanking.get(country) == 0) {
					euRanking.set(country, Integer.MAX_VALUE);
				}
				euRanking.set(country, Math.min(rank, euRanking.get(country)));
			}
		}
		euCountries.sortValues();
		euMarketValue.sortValues();
		euRanking.sortValuesReverse();
	}

	private void createTableHeader(String header) {
		String[] values = header.substring(0, header.length() - 1).split(DELIMITER);
		forbesGlobal.addColumn(values[COMPANY_INDEX], STRING);
		forbesGlobal.addColumn(values[INDUSTRY_INDEX], STRING);
		forbesGlobal.addColumn(values[COUNTRY_INDEX], STRING);

		forbesGlobal.addColumn(values[MARKET_VALUE_INDEX], FLOAT);
		forbesGlobal.addColumn(values[SALES_INDEX], FLOAT);
		forbesGlobal.addColumn(values[PROFIT_INDEX], FLOAT);
		forbesGlobal.addColumn(values[ASSETS_INDEX], FLOAT);

		forbesGlobal.addColumn(values[RANK_INDEX], INT);

		forbesGlobal.addColumn(values[FORBES_WEBPAGE_INDEX], STRING);
	}

	/**
	 * Replace btw fromColumn and toColumn all occurrences of oldChar with
	 * newChar. The parameters are counted 1-based and the from- and toColumn
	 * are included. End column should be greater than start column. Correct
	 * parameters are assumed. Audit is not performed.
	 * 
	 * @param line
	 *            DELIMITER separated data
	 * @param fromColumn
	 *            the start column. It is included.
	 * @param toColumn
	 *            the end column, It is included
	 * @return
	 */
	private Object[] replaceBtwColumns(String line, int fromColumn, int toColumn, char oldChar, char newChar) {
		Object[] result = line.substring(0, line.length() - 1).split(DELIMITER);
		for (int i = fromColumn; i < toColumn; i++) {
			result[i] = result[i].toString().replace(oldChar, newChar);
		}

		return result;
	}

	private void createBarChart(FloatDict dict) {
		barChart = new BarChart(this);
		barChart.setData(dict.valueArray());
		barChart.setBarLabels(dict.keyArray());
		barChart.setBarColour(color(200, 80, 80, 100));
		barChart.setBarGap(4);
		barChart.setValueFormat("###,###");
		barChart.showValueAxis(true);
		barChart.showCategoryAxis(true);
		barChart.transposeAxes(true);
	}

	private void drawHeadlineText(String headline, String subheadline) {
		float textHeight = textAscent();

		textFont(titleFont);
		text(headline, 70, 20);
		textFont(smallFont);
		text(subheadline, 70, 20 + textHeight);
	}
}