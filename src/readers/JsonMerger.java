package readers;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import settings.reading.JsonMergerSettings;
import structure.data.Attribute;
import structure.data.AxisAttributes;
import structure.data.Layer;
import structure.data.Location;
import structure.data.SpreadData;
import structure.data.TimeLine;
import structure.data.attributable.Area;
import structure.data.attributable.Line;
import structure.data.attributable.Point;

public class JsonMerger {

	private JsonMergerSettings settings;

	public JsonMerger(JsonMergerSettings settings) {

		this.settings = settings;

	}// END: Constructor

	public SpreadData merge() throws FileNotFoundException {

		SpreadData data = null;

		// compile a unique list of files
		Set<String> jsonFiles = new HashSet<String>();

		if (settings.pointsFiles != null) {
			for (String file : settings.pointsFiles) {
				jsonFiles.add(file);
			}
		} // END: null check

		if (settings.linesFiles != null) {
			for (String file : settings.linesFiles) {
				jsonFiles.add(file);
			}
		} // END: null check

		if (settings.areasFiles != null) {
			for (String file : settings.areasFiles) {
				jsonFiles.add(file);
			}
		} // END: null check

		if (settings.countsFiles != null) {
			for (String file : settings.countsFiles) {
				jsonFiles.add(file);
			}
		} // END: null check

		jsonFiles.add(settings.geojsonFile);
		jsonFiles.add(settings.axisAttributesFile);

		// Utils.printArray(jsonFiles.toArray());

		DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy/MM/dd");

		TimeLine timeLine = null;
		AxisAttributes axisAttributes = null;
		Set<Attribute> mapAttributes = null;
		Set<Attribute> lineAttributes = null;
		Set<Attribute> pointAttributes = null;
		Set<Location> locations = null;
		
		LinkedList<Layer> layers = new LinkedList<Layer>();
//		GeoJsonData geojson = null;
		List<Point> counts = null;
		List<Point> points = null;
		List<Line> lines = null;
		List<Area> areas = null;

		// Layer: map
		// Layer: tree
		// Layer: counts

		boolean timeLineCreated = false;
		boolean mapAttributesCreated = false;
		boolean pointAttributesCreated = false;
		boolean lineAttributesCreated = false;
		boolean locationsCreated = false;
		boolean linesListCreated = false;
		boolean pointsListCreated = false;
		boolean areasListCreated = false;
		boolean countsListCreated = false;

		for (String file : jsonFiles) {

			Reader reader = new FileReader(file);
			Gson gson = new GsonBuilder().create();
			SpreadData json = gson.fromJson(reader, SpreadData.class);

			// ---TIME LINE---//
			
			if (!timeLineCreated) {

				timeLine = json.getTimeLine();
				if (timeLine != null) {
					timeLineCreated = false;
				}

			} else {

				timeLine = compareTimeLines(timeLine, json.getTimeLine(), dateFormatter);

			} // END: first check

			// ---AXIS ATTRIBUTES---//

			if (file.equalsIgnoreCase(settings.axisAttributesFile)) {
				axisAttributes = json.getAxisAttributes();
			}

			// --- MAP ATTRIBUTES---//

			if (settings.pointsFiles != null) {
				if (Arrays.asList(settings.geojsonFile).contains(file)) {

					if (!mapAttributesCreated) {

						if (json.getMapAttributes() != null) {

							mapAttributes = new HashSet<Attribute>();
							mapAttributes.addAll(json.getMapAttributes());
							mapAttributesCreated = true;
						}

					} else {

						if (json.getMapAttributes() != null) {
							mapAttributes.addAll(json.getMapAttributes());
						}

					} // END: first check

				} // END: map check
			} // END: null check

			// --- POINT ATTRIBUTES---//

			if (settings.pointsFiles != null) {
				if (Arrays.asList(settings.pointsFiles).contains(file)) {

					if (!pointAttributesCreated) {

						if (json.getPointAttributes() != null) {
							pointAttributes = new HashSet<Attribute>();
							pointAttributes.addAll(json.getPointAttributes());
							pointAttributesCreated = true;
						}

					} else {

						if (json.getPointAttributes() != null) {
							pointAttributes.addAll(json.getPointAttributes());
						}

					} // END: first check

				} // END: get check
			} // END: null check

			// --- LINE ATTRIBUTES---//

			if (settings.linesFiles != null) {
				if (Arrays.asList(settings.linesFiles).contains(file)) {

					if (!lineAttributesCreated) {

						if (json.getLineAttributes() != null) {
							lineAttributes = new HashSet<Attribute>();
							lineAttributes.addAll(json.getLineAttributes());
							lineAttributesCreated = true;
						}

					} else {

						if (json.getLineAttributes() != null) {
							lineAttributes.addAll(json.getLineAttributes());
						}

					} // END: first check

				} // END: get map check
			} // END: null check

			// ---GEOJSON LAYER---//

			if (settings.geojsonFile != null) {
				if (Arrays.asList(settings.geojsonFile).contains(file)) {

					for (Layer layer : json.getLayers()) {
						if (layer.getType().equalsIgnoreCase(Layer.Type.map.toString()) ) {
							
							layers.add(layer);
							
						}
					} // END: layers loop

				} // END: get check
			} // END: null check

			// --- LOCATIONS LIST---//

			if (!locationsCreated) {

				if (json.hasLocations()) {
					locations = new HashSet<Location>();
					locations.addAll(json.getLocations());
					locationsCreated = true;
				}

			} else {

				if (json.getLocations() != null) {
					locations.addAll(json.getLocations());
				}

			} // END: first check

			// --- LINES LIST---//

			if (settings.linesFiles != null) {
				if (Arrays.asList(settings.linesFiles).contains(file)) {

					if (!linesListCreated) {

						lines = new ArrayList<Line>();

						for (Layer layer : json.getLayers()) {
							if (layer.hasLines()) {
								lines.addAll(layer.getLines());
							}
						} // END: layers loop

						if (lines.size() != 0) {
							linesListCreated = true;
						}

					} else {

						for (Layer layer : json.getLayers()) {
							if (layer.hasLines()) {
								lines.addAll(layer.getLines());
							}
						} // END: layers loop

					} // END: first check

				} // END: get lines check
			} // END: null check

			// --- POINTS LIST---//

			if (settings.pointsFiles != null) {
				if (Arrays.asList(settings.pointsFiles).contains(file)) {

					if (!pointsListCreated) {

						points = new ArrayList<Point>();

						for (Layer layer : json.getLayers()) {
							if (layer.hasLines()) {
								points.addAll(layer.getPoints());
							}
						} // END: layers loop

						if (points.size() != 0) {
							pointsListCreated = true;
						}

					} else {

						for (Layer layer : json.getLayers()) {
							if (layer.hasPoints()) {
								points.addAll(layer.getPoints());
							}
						} // END: layers loop

					} // END: first check

				} // END: get points check
			} // END: null check

			// --- AREAS LIST---//

			if (settings.areasFiles != null) {
				if (Arrays.asList(settings.areasFiles).contains(file)) {

					if (!areasListCreated) {

						areas = new ArrayList<Area>();

						for (Layer layer : json.getLayers()) {
							if (layer.hasAreas()) {
								areas.addAll(layer.getAreas());
							}
						} // END: layers loop

						if (areas.size() != 0) {
							areasListCreated = true;
						}

					} else {

						for (Layer layer : json.getLayers()) {
							if (layer.hasAreas()) {
								areas.addAll(layer.getAreas());
							}
						} // END: layers loop

					} // END: first check

				} // END: get areas check
			} // END: null check

			// --- COUNTS LIST---//

			if (settings.countsFiles != null) {
				if (Arrays.asList(settings.countsFiles).contains(file)) {

					if (!countsListCreated) {

						counts = new ArrayList<Point>();

						for (Layer layer : json.getLayers()) {
							if (layer.getType().equals(Layer.Type.counts)) {
								counts.addAll(layer.getPoints());
							}
						} // END: layers loop

						if (counts.size() != 0) {
							countsListCreated = true;
						}

					} else {

						for (Layer layer : json.getLayers()) {
							if (layer.getType().equals(Layer.Type.counts)) {
								counts.addAll(layer.getPoints());
							}
						} // END: layers loop

					} // END: first check

				} // END: get counts check
			} // END: null check

		} // END: json loop

		// ---LAYERS---//


//		String id = Utils.splitString(settings.geojsonFile, "/");
		Layer treeLayer = new Layer("id", "Tree Layer", points, lines, areas);
		layers.add(treeLayer);

		if (counts != null) {
			Layer countsLayer = new Layer("id", "Counts layer", counts);
			layers.add(countsLayer);
		}

		// ---SPREAD DATA---//

		LinkedList<Attribute> mapAttributesList = null;
		if (mapAttributes != null) {
			mapAttributesList = new LinkedList<Attribute>(mapAttributes);
		}

		LinkedList<Attribute> lineAttributesList = null;
		if (lineAttributes != null) {
			lineAttributesList = new LinkedList<Attribute>(lineAttributes);
		}

		LinkedList<Attribute> pointAttributesList = null;
		if (pointAttributes != null) {
			pointAttributesList = new LinkedList<Attribute>(pointAttributes);
		}

		LinkedList<Location> locationsList = null;
		if (locations != null) {
			locationsList = new LinkedList<Location>(locations);
		}

		data = new SpreadData(timeLine, //
				axisAttributes, //
				mapAttributesList, //
				lineAttributesList, //
				pointAttributesList, //
				locationsList, //
				layers);

		return data;
	}// END: merge

	private TimeLine compareTimeLines(TimeLine current, TimeLine candidate, DateTimeFormatter dateFormatter) {

		TimeLine timeLine = null;

		LocalDate currentStart = dateFormatter.parseLocalDate(current.getStartTime());
		LocalDate candidateStart = dateFormatter.parseLocalDate(candidate.getStartTime());

		String newStartTime = dateFormatter.print(currentStart);
		if (candidateStart.isBefore(currentStart)) {
			newStartTime = dateFormatter.print(candidateStart);
		}

		LocalDate currentEnd = dateFormatter.parseLocalDate(current.getEndTime());
		LocalDate candidateEnd = dateFormatter.parseLocalDate(candidate.getEndTime());

		String newEndTime = dateFormatter.print(currentEnd);
		if (candidateEnd.isAfter(currentEnd)) {
			newEndTime = dateFormatter.print(candidateEnd);
		}

		timeLine = new TimeLine(newStartTime, newEndTime);

		return timeLine;
	}// END: compareTimeLines

}// END: class