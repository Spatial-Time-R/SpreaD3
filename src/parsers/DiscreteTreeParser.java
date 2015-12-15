package parsers;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.trees.RootedTree;
import structure.data.Attribute;
import structure.data.Location;
import structure.data.attributable.Line;
import structure.data.attributable.Point;
import utils.Utils;
import exceptions.AnalysisException;

public class DiscreteTreeParser {

	public static final String COUNT = "count";

	private String locationTrait;
	private RootedTree rootedTree;
	private double timescaleMultiplier;
	private TimeParser timeParser;

	private LinkedList<Location> locationsList;
	private LinkedList<Attribute> uniqueBranchAttributes;
	private LinkedList<Attribute> uniqueNodeAttributes;

	private LinkedList<Line> linesList;
	private LinkedList<Point> pointsList;
	private LinkedList<Point> countsList;

	public DiscreteTreeParser(RootedTree rootedTree, //
			String locationTrait, //
			LinkedList<Location> locationsList, //
			TimeParser timeParser, //
			double timescaleMultiplier //
	) throws AnalysisException {

		this.locationTrait = locationTrait;
		this.rootedTree = rootedTree;
		this.timeParser = timeParser;
		this.timescaleMultiplier = timescaleMultiplier;

		// structures
		this.locationsList = locationsList;
		this.uniqueBranchAttributes = new LinkedList<Attribute>();
		this.uniqueNodeAttributes = new LinkedList<Attribute>();

		this.linesList = new LinkedList<Line>();
		this.pointsList = new LinkedList<Point>();
		this.countsList = new LinkedList<Point>();

	}// END: Constructor

	public void parseTree() throws IOException, ImportException,
		  AnalysisException {

		HashMap<Node, Point> pointsMap = new HashMap<Node, Point>();

		Double[] sliceHeights = createSliceHeights(10);
		int[][] locationCounts = new int[sliceHeights.length][locationsList
				.size()];

		int index = 0;
		Location dummy;
		for (Node node : rootedTree.getNodes()) {
			if (!rootedTree.isRoot(node)) {

				// node parsed first

				String nodeState = (String) Utils.getObjectNodeAttribute(node,
						locationTrait);
				if (nodeState.contains("+")) {
					String message = "Found tied state " + nodeState;
					nodeState = Utils.breakTiesRandomly(nodeState);
					message += (" randomly choosing " + nodeState);
					System.out.println(message);
				} // END: tie check

				dummy = new Location(nodeState);
				int locationIndex = Integer.MAX_VALUE;
				if (locationsList.contains(dummy)) {
					locationIndex = locationsList.indexOf(dummy);
				} else {

					String message1 = "Location " + dummy.getId()
							+ " could not be found in the locations file.";
					String message2 = "Resulting file may be incomplete!";
					System.out.println(message1 + " " + message2);
					continue;

				}

				Location nodeLocation = locationsList.get(locationIndex);

				// parent node parsed second
				
				Node parentNode = rootedTree.getParent(node);

				String parentState = (String) Utils.getObjectNodeAttribute(
						parentNode, locationTrait);
				if (parentState.contains("+")) {

					String message = "Found tied state " + parentState;
					parentState = Utils.breakTiesRandomly(parentState);
					message += (" randomly choosing " + parentState);
					System.out.println(message);

				} // END: tie check

				dummy = new Location(parentState);
				locationIndex = Integer.MAX_VALUE;
				if (locationsList.contains(dummy)) {

					locationIndex = locationsList.indexOf(dummy);

				} else {

					String message =  "Parent location " + dummy.getId() + " could not be found in the locations file.";
					throw new  AnalysisException(message);
				}

				Location parentLocation = locationsList.get(locationIndex);

				if (!parentLocation.equals(nodeLocation)) {

					Point parentPoint = pointsMap.get(parentNode);
					if (parentPoint == null) {

						parentPoint = createPoint(index, parentNode, parentLocation);
						pointsMap.put(parentNode, parentPoint);
						index++;

					}// END: null check

					Point nodePoint = pointsMap.get(node);
					if (nodePoint == null) {

						nodePoint = createPoint(index, node, nodeLocation);
						pointsMap.put(node, nodePoint);
						index++;

					}// END: null check

					Line line = new Line(parentPoint.getId(), //
							nodePoint.getId(), //
							parentPoint.getStartTime(), //
							nodePoint.getStartTime(), //
							nodePoint.getAttributes() //
					);

					linesList.add(line);

				} else {

					// count lineages holding state
					for (int i = 0; i < sliceHeights.length; i++) {

						double sliceHeight = sliceHeights[i];
						for (Location location : locationsList) {

							if ((rootedTree.getHeight(node) <= sliceHeight)
									&& (rootedTree.getHeight(parentNode) > sliceHeight)) {

								if (nodeLocation.equals(parentLocation)
										&& parentLocation.equals(location)) {

									int j = locationsList.lastIndexOf(location);
									locationCounts[i][j]++;

								} // END: location check

							} // END:
						} // END: locations loop
					} // END: sliceHeights lop

				} // END: state check

			} // END: root check
		} // END: node loop

		pointsList.addAll(pointsMap.values());

		// create Points list with count attributes

		index = 0;
		Double[] countRange = new Double[2];
		countRange[Attribute.MIN_INDEX] = Double.MAX_VALUE;
		countRange[Attribute.MAX_INDEX] = Double.MIN_VALUE;

		for (int sliceIndex = 0; sliceIndex < locationCounts.length; sliceIndex++) {

			double height = sliceHeights[sliceIndex];
			double nextHeight = sliceIndex < locationCounts.length-1 ? sliceHeights[sliceIndex+1] : 0.0;
			
			for (int locationIndex = 0; locationIndex < locationCounts[0].length; locationIndex++) {

				Double count = (double) locationCounts[sliceIndex][locationIndex];
				if (count > 0) {

					String id = "point_" + index;
					Location location = locationsList.get(locationIndex);
					String startTime = timeParser.getNodeDate(height);
					String endTime = timeParser.getNodeDate(nextHeight);
					
					Map<String, Object> attributes = new LinkedHashMap<String, Object>();
					attributes.put(COUNT,
							locationCounts[sliceIndex][locationIndex]);

					Point point = new Point(id, location, startTime, endTime, attributes);
					countsList.add(point);
					index++;

					if (count < countRange[Attribute.MIN_INDEX]) {
						countRange[Attribute.MIN_INDEX] = count;
					} // END: min check

					if (count > countRange[Attribute.MAX_INDEX]) {
						countRange[Attribute.MAX_INDEX] = count;
					} // END: max check

				}

			}// END: locations loop
		}// END: slice loop

		Attribute countAttribute = new Attribute(COUNT, countRange);

		// collect attributes from lines
		Map<String, Attribute> branchAttributesMap = new HashMap<String, Attribute>();

		for (Line line : linesList) {

			for (Entry<String, Object> entry : line.getAttributes().entrySet()) {

				String attributeId = entry.getKey();
				Object attributeValue = entry.getValue();

				if (branchAttributesMap.containsKey(attributeId)) {

					Attribute attribute = branchAttributesMap.get(attributeId);

					if (attribute.getScale().equals(Attribute.ORDINAL)) {

						attribute.getDomain().add(attributeValue);

					} else {

						double value = Utils
								.round( Double.valueOf(attributeValue.toString()), 100);

						if (value < attribute.getRange()[Attribute.MIN_INDEX]) {
							attribute.getRange()[Attribute.MIN_INDEX] = value;
						} // END: min check

						if (value > attribute.getRange()[Attribute.MAX_INDEX]) {
							attribute.getRange()[Attribute.MAX_INDEX] = value;
						} // END: max check

					} // END: scale check

				} else {

					Attribute attribute;
					if (attributeValue instanceof Double) {

						Double[] range = new Double[2];
						range[Attribute.MIN_INDEX] = (Double) attributeValue;
						range[Attribute.MAX_INDEX] = (Double) attributeValue;

						attribute = new Attribute(attributeId, range);

					} else {

						HashSet<Object> domain = new HashSet<Object>();
						domain.add(attributeValue);

						attribute = new Attribute(attributeId, domain);

					} // END: isNumeric check

					branchAttributesMap.put(attributeId, attribute);

				} // END: key check

			} // END: attributes loop

		} // END: lines loop

		uniqueBranchAttributes.addAll(branchAttributesMap.values());

		// collect attributes from nodes
		Map<String, Attribute> nodeAttributesMap = new HashMap<String, Attribute>();

		for (Point point : pointsList) {

			for (Entry<String, Object> entry : point.getAttributes().entrySet()) {

				String attributeId = entry.getKey();
				Object attributeValue = entry.getValue();

				if (nodeAttributesMap.containsKey(attributeId)) {

					Attribute attribute = nodeAttributesMap.get(attributeId);

					if (attribute.getScale().equals(Attribute.ORDINAL)) {

						attribute.getDomain().add(attributeValue);

					} else {

						double value = Utils
								.round(Double.valueOf(attributeValue.toString()), 100);

						if (value < attribute.getRange()[Attribute.MIN_INDEX]) {
							attribute.getRange()[Attribute.MIN_INDEX] = value;
						} // END: min check

						if (value > attribute.getRange()[Attribute.MAX_INDEX]) {
							attribute.getRange()[Attribute.MAX_INDEX] = value;
						} // END: max check

					} // END: scale check

				} else {

					Attribute attribute;
					if (attributeValue instanceof Double) {

						Double[] range = new Double[2];
						range[Attribute.MIN_INDEX] = (Double) attributeValue;
						range[Attribute.MAX_INDEX] = (Double) attributeValue;

						attribute = new Attribute(attributeId, range);

					} else {

						HashSet<Object> domain = new HashSet<Object>();
						domain.add(attributeValue);

						attribute = new Attribute(attributeId, domain);

					} // END: isNumeric check

					nodeAttributesMap.put(attributeId, attribute);

				} // END: key check

			} // END: attributes loop

		}// END: points loop

		uniqueNodeAttributes.addAll(branchAttributesMap.values());
		// we dump it here with node attributes
		uniqueNodeAttributes.add(countAttribute);

	}// END: parseTree

	private Double[] createSliceHeights(int intervals) {
		double rootHeight = rootedTree.getHeight(rootedTree.getRootNode());
		double delta = rootHeight / (double) intervals;

		Double[] sliceHeights = new Double[intervals - 1];
		for (int i = 0; i < (intervals - 1); i++) {
			sliceHeights[i] = rootHeight - ((i + 1) * delta);
		}

		return sliceHeights;
	}//END: createSliceHeights

	private Point createPoint(int index, Node node, Location location)
			throws   AnalysisException {

		String id = "point_" + index;
		Double height = Utils.getNodeHeight(rootedTree, node)
				* timescaleMultiplier;
		String startTime = timeParser.getNodeDate(height);

		Map<String, Object> attributes = new LinkedHashMap<String, Object>();
		for (String attributeName : node.getAttributeNames()) {

			Object nodeAttribute = node.getAttribute(attributeName);

			if (!(nodeAttribute instanceof Object[])) {
				
				// TODO: remove invalid characters
				attributeName = attributeName.replaceAll("%", "");
				attributeName = attributeName.replaceAll("!", "");
				
				attributes.put(attributeName, nodeAttribute);
				
			} // END: multivariate check

		} // END: attributes loop

		Point point = new Point(id, location, startTime, attributes);

		return point;
	}// END: createPoint

	public LinkedList<Line> getLinesList() {
		return linesList;
	}

	public LinkedList<Point> getPointsList() {
		return pointsList;
	}

	public LinkedList<Point> getCountsList() {
		return countsList;
	}

	public LinkedList<Attribute> getLineAttributes() {
		return uniqueBranchAttributes;
	}

	public LinkedList<Attribute> getPointAttributes() {
		return uniqueNodeAttributes;
	}

}// END: class
