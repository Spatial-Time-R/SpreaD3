package data.structure.attributable;

import java.util.LinkedHashMap;
import java.util.Map;

import data.structure.primitive.Polygon;

public class Area {

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
	
	private final Polygon polygon;
	private final String startTime;
	
	public Area( Polygon polygon, String startTime) {
		
		this.polygon = polygon;
		this.startTime = startTime;
		
		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
		
	}//END: Constructor
	
	public Polygon getPolygon() {
		return polygon;
	}

	public String getStartTime() {
		return startTime;
	}
	
	public Map<String, Object> getAttributes() {
		return attributes;
	}
	
}//END: class
