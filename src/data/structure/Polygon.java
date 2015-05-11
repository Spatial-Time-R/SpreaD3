package data.structure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import utils.Trait;

public class Polygon {

	private final Map<String, Trait> attributes = new LinkedHashMap<String, Trait>();

	//	private final Location location;
	private final String location;
	private final boolean hasLocation;
	
	private final List<Coordinate> coordinates;
	private final double time;
	
	public Polygon(List<Coordinate> coordinates, //
			double time, //
			Map<String, Trait> attributes //
	) {

		super();

		this.location = null;
        this.hasLocation = false;
		
		this.coordinates = coordinates;
		this.time = time;

		if (attributes != null) {
			this.attributes.putAll(attributes);
		}

	}// END: Polygon

	public Polygon(
//			Location centroid, //
			String location, //
			double time, //
			Map<String, Trait> attributes //
	) {

		super();

		this.location = location;
//		this.location = centroid;
		this.hasLocation = true;
		this.coordinates = null;
		this.time = time;

		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
	}

	public List<Coordinate> getCoordinates() {
		return coordinates;
	}

	public double getTime() {
		return time;
	}

	public String getLocationId() {
		return location;
	}

	
//	public Location getCentroid() {
//		return location;
//	}

	public Map<String, Trait> getAttributes() {
		return attributes;
	}
	
	public boolean hasLocation() {
		return hasLocation;
	}
	
}// END: class
