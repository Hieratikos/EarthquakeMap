package Sample;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.events.EventDispatcher;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.OpenStreetMap;
import de.fhpotsdam.unfolding.ui.BarScaleUI;
import de.fhpotsdam.unfolding.utils.MapUtils;
import processing.core.PApplet;
import processing.core.PFont;

/**
 * EarthquakeCityMap An application with an interactive map displaying
 * earthquake data. Author: UC San Diego Intermediate Software Development MOOC
 * team
 * 
 * @author Hieratikos Date: October 1, 2016
 */
public class EarthquakeCityMap extends PApplet {

	// We will use member variables, instead of local variables, to store the
	// data
	// that the setUp and draw methods will need to access (as well as other
	// methods)
	// You will use many of these variables, but the only one you should need to
	// add
	// code to modify is countryQuakes, where you will store the number of
	// earthquakes
	// per country.

	// You can ignore this. It's to get rid of eclipse warnings
	private static final long serialVersionUID = 1L;

	// IF YOU ARE WORKING OFFILINE, change the value of this variable to true
	private static final boolean offline = false;

	/**
	 * This is where to find the local tiles, for working without an Internet
	 * connection
	 */
	public static String mbTilesString = "blankLight-1-3.mbtiles";

	// feed with magnitude 2.5+ Earthquakes
	//private String earthquakesURL = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";

	// set up the graphics for a full screen
	GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	Rectangle bounds = ge.getMaximumWindowBounds();
	float width = (float) bounds.getWidth();
	float height = (float) bounds.getHeight();
	float mapX = 200;
	float mapY = 50;
	int keyX = 25, keyY = 50, keyWidth = keyX + 125, keyHeight = keyY + 310, radius = 15;
	int yellow = color(255, 255, 0);
	int blue = color(0, 0, 255);
	int green = color(0, 255, 0);
	int red = color(255, 0, 0);
	int darkred = color(150, 0, 0);
	int black = color(0, 0, 0);
	int white = color(255, 255, 255);
	int darkgray = color(100, 100, 100);
	int tan = color(255, 250, 240);

	// The files containing city names and info and country names and info
	private String cityFile = "Sample/city-data.json";
	private String countryFile = "Sample/countries.geo.json";

	// The map
	private UnfoldingMap map;

	// Markers for each city
	private List<Marker> cityMarkers;
	// Markers for each earthquake
	private List<Marker> quakeMarkers;

	// A List of country markers
	private List<Marker> countryMarkers;

	// NEW IN MODULE 5
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;

	BarScaleUI barScale;
	static final int ZOOM_MIN = 3;
	static final int ZOOM_MAX = 15;
	static final int ZOOM_LEVELS = 18;
	PFont keyFont;
	static final int FONT_SIZE = 12;

	// Calendar

	public static void main(String[] args){
		PApplet.main("EarthquakeCityMap");
	}
	public void settings(){
	    
    }
	@Override
	public void setup() {
		keyFont = createFont("Arial Bold", FONT_SIZE);
		size((int) bounds.getWidth(), (int) bounds.getHeight(), OPENGL);
		if (offline) {
			// map = new UnfoldingMap(this, 200, 50, 650, 600, new
			// MBTilesMapProvider(mbTilesString));
			// earthquakesURL = "2.5_week.atom"; // The same feed, but saved
			// August 7, 2015
		} else {
			// map = new UnfoldingMap(this, 200, 50, 650, 600, new
			// OpenStreetMap.OpenStreetMapProvider());
			// map = new UnfoldingMap(this, mapX, mapY, width-250,
			// height-150,new Microsoft.RoadProvider());
			// map = new UnfoldingMap(this, mapX, mapY, width-250,
			// height-150,new Microsoft.AerialProvider());
			// map = new UnfoldingMap(this, mapX, mapY, width-250,
			// height-150,new Microsoft.HybridProvider());
			map = new UnfoldingMap(this, mapX, mapY, width - 250, height - 150,
					new OpenStreetMap.OpenStreetMapProvider());
			map.zoomToLevel(ZOOM_MIN);
			map.setZoomRange(ZOOM_MIN, ZOOM_MAX);
			map.setTweening(true);
		}
		EventDispatcher eventDispatcher = MapUtils.createDefaultEventDispatcher(this, map);

		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for (Feature city : cities) {
			cityMarkers.add(new CityMarker(city));
		}
		List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
		quakeMarkers = new ArrayList<Marker>();
		for (PointFeature feature : earthquakes) {
			// check if LandQuake
			if (isLand(feature)) {
				quakeMarkers.add(new LandQuakeMarker(feature));
			}
			// OceanQuakes
			else {
				quakeMarkers.add(new OceanQuakeMarker(feature));
			}
		}
		// printQuakes();
		map.addMarkers(quakeMarkers);
		map.addMarkers(cityMarkers);
		// sortAndPrint(quakeMarkers.size());
		// sortAndPrint(50);

	}

	@Override
	public void draw() {
		background(black);
		map.draw();
		addKey();
		addBarScale();
		barScale.draw();
		setLatLon();
	}

	private void addBarScale() {
		rectMode(CORNER);
		fill(tan);
		rect(keyX, keyY + 850, keyX + 125, keyY);
		barScale = new BarScaleUI(this, map);
		barScale.x = keyX + 25;
		barScale.y = keyY + 870;
		barScale.setStyle(color(black), 2, -5, keyFont);
		barScale.setAutoAlignment(false);

	}

	// and then call that method from setUp
	private void sortAndPrint(int numToPrint) {
		Object[] sortedQuakes = quakeMarkers.toArray();
		Arrays.sort(sortedQuakes);
		for (int i = 0; i < numToPrint; i++) {
			System.out.println("Quake #" + Math.addExact(i, 1) + ": " + sortedQuakes[i]);
		}
	}

	/**
	 * Event handler that gets called automatically when the mouse moves.
	 */
	@Override
	public void mouseMoved() {
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;

		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
		// loop();
	}

	// If there is a marker selected
	private void selectMarkerIfHover(List<Marker> markers) {
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}

		for (Marker m : markers) {
			CommonMarker marker = (CommonMarker) m;
			if (marker.isInside(map, mouseX, mouseY)) {
				lastSelected = marker;
				marker.setSelected(true);
				return;
			}
		}
	}

	/**
	 * The event handler for mouse clicks It will display an earthquake and its
	 * threat circle of cities Or if a city is clicked, it will display all the
	 * earthquakes where the city is in the threat circle
	 */
	@Override
	public void mouseClicked() {
		if (lastClicked != null) {
			unhideMarkers();
			lastClicked = null;
		} else if (lastClicked == null) {
			checkEarthquakesForClick();
			if (lastClicked == null) {
				checkCitiesForClick();
			}
		}
	}

	// Helper method that will check if a city marker was clicked on
	// and respond appropriately
	private void checkCitiesForClick() {
		if (lastClicked != null)
			return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker marker : cityMarkers) {
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker) marker;
				map.zoomAndPanTo(6, marker.getLocation());
				// Hide all the other earthquakes and hide
				for (Marker mhide : cityMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : quakeMarkers) {
					EarthquakeMarker quakeMarker = (EarthquakeMarker) mhide;
					if (quakeMarker.getDistanceTo(marker.getLocation()) > quakeMarker.threatCircle()) {
						quakeMarker.setHidden(true);
					}
				}
				return;
			}
		}
	}

	// Helper method that will check if an earthquake marker was clicked on
	// and respond appropriately
	private void checkEarthquakesForClick() {
		if (lastClicked != null)
			return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker m : quakeMarkers) {
			EarthquakeMarker marker = (EarthquakeMarker) m;
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = marker;
				map.zoomAndPanTo(6, marker.getLocation());
				// Hide all the other earthquakes and hide
				for (Marker mhide : quakeMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : cityMarkers) {
					if (mhide.getDistanceTo(marker.getLocation()) > marker.threatCircle()) {
						mhide.setHidden(true);
					}
				}
				return;
			}
		}
	}

	// loop over and unhide all markers
	private void unhideMarkers() {
		for (Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}

		for (Marker marker : cityMarkers) {
			marker.setHidden(false);
		}
		map.zoomAndPanTo(3, new Location(0, 0));
	}

	// helper method to draw key in GUI
	private void addOtherKey() {
		// Remember you can use Processing's graphics methods here
		fill(255, 250, 240);

		int xbase = ((int) mapX) - 175;
		int ybase = ((int) mapY);

		rect(xbase, ybase, 150, 250);

		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", xbase + 25, ybase + 25);

		fill(150, 30, 30);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase - CityMarker.TRI_SIZE, tri_xbase - CityMarker.TRI_SIZE,
				tri_ybase + CityMarker.TRI_SIZE, tri_xbase + CityMarker.TRI_SIZE, tri_ybase + CityMarker.TRI_SIZE);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("City Marker", tri_xbase + 15, tri_ybase);

		text("Land Quake", xbase + 50, ybase + 70);
		text("Ocean Quake", xbase + 50, ybase + 90);
		text("Size ~ Magnitude", xbase + 25, ybase + 110);

		fill(255, 255, 255);
		ellipse(xbase + 35, ybase + 70, 10, 10);
		rect(xbase + 35 - 5, ybase + 90 - 5, 10, 10);

		fill(color(255, 255, 0));
		ellipse(xbase + 35, ybase + 140, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase + 35, ybase + 160, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase + 35, ybase + 180, 12, 12);

		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase + 50, ybase + 140);
		text("Intermediate", xbase + 50, ybase + 160);
		text("Deep", xbase + 50, ybase + 180);

		text("Past hour", xbase + 50, ybase + 200);
		text("Zoom Level: " + map.getZoomLevel(), xbase + 50, ybase + 220);

		fill(255, 255, 255);
		int centerx = xbase + 35;
		int centery = ybase + 200;
		ellipse(centerx, centery, 12, 12);

		strokeWeight(2);
		line(centerx - 8, centery - 8, centerx + 8, centery + 8);
		line(centerx - 8, centery + 8, centerx + 8, centery - 8);
	}

	private void addKey() {
		// Remember you can use Processing's graphics methods here
		int zoomWidth = keyWidth - 20, zoomHeight = keyY - 25;
		stroke(black);
		strokeWeight(1);
		// Key
		rectMode(CORNER);
		PFont titleFont = createFont("Arial Bold", 12);
		textFont(titleFont);
		fill(tan);
		rect(keyX, keyY, keyWidth, keyHeight);

		// City Locations legend
		fill(darkred);
		triangle(keyX + 25, keyY + 40, keyX + 17, keyY + 55, keyX + 33, keyY + 55);
		fill(black);
		text("City Locations", keyX + 50, keyY + 50);

		// Earthquake Key Title Heading
		fill(black);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", keyX + 25, keyY + 25);

		// Earthquake legends
		fill(white);
		ellipse(keyX + 25, keyY + 80, radius, radius);
		rectMode(CENTER);
		rect(keyX + 25, keyY + 110, radius, radius);

		// Earthquake legends text
		fill(black);
		text("Land Quake", keyX + 50, keyY + 80);
		text("Ocean Quake", keyX + 50, keyY + 110);
		// textAlign(LEFT,CENTER);
		text("Size ~ Magnitude", keyX + 25, keyY + 140);

		// Shallow legend
		fill(yellow);
		ellipse(keyX + 25, keyY + 170, radius, radius);
		fill(black);
		text("Shallow", keyX + 50, keyY + 170);

		// Intermediate legend
		fill(blue);
		ellipse(keyX + 25, keyY + 200, radius, radius);
		fill(black);
		text("Intermediate", keyX + 50, keyY + 200);

		// Deep legend
		fill(red);
		ellipse(keyX + 25, keyY + 230, radius, radius);
		fill(black);
		text("Deep", keyX + 50, keyY + 230);

		// Past hour
		int centerX = keyX + 25;
		int centerY = keyY + 260;
		// strokeWeight(2);
		line(centerX - 8, centerY - 8, centerX + 8, centerY + 8);
		line(centerX - 8, centerY + 8, centerX + 8, centerY - 8);
		text("Past Day", keyX + 50, keyY + 260);

		// Zoom Level
		text("Zoom Level: " + map.getZoomLevel() + "/" + ZOOM_LEVELS, keyX + 25, keyY + 290);
		fill(tan);
		rectMode(CORNER);
		// Create a series of rectangles, each representing a single zoom level,
		// within the span of a full rectangle
		rectMode(CORNER);
		for (int i = 0; i < ZOOM_LEVELS + 1; i++) {
			rect(keyX + 10, keyY + 300, zoomWidth * (i / (float) ZOOM_LEVELS), zoomHeight / 2);
		}
		// Fill each zoom square with red
		fill(red);
		rect(keyX + 10, keyY + 300, zoomWidth * ((float) map.getZoomLevel() / (float) ZOOM_LEVELS),
				zoomHeight / 2);
	}

	private void setLatLon() {
		// Lat & Lon
		rectMode(CORNER);
		fill(tan);
		rect(keyX, keyY + 790, keyWidth, keyY);
		fill(red);
		Location latlon = map.getLocation(mouseX, mouseY);
		String latString = "", lonString = "";
		String degree = "\u00B0";
		DecimalFormat df = new DecimalFormat("00.000");
		if (latlon.getLat() > 0) {
			latString = df.format(latlon.getLat()) + degree + "N";
		} else if (latlon.getLat() < 0) {
			latString = df.format(Math.abs(latlon.getLat())) + degree + "S";
		}
		if (latlon.getLon() > 0) {
			lonString = df.format(latlon.getLon()) + degree + "E";
		} else if (latlon.getLon() < 0) {
			lonString = df.format(Math.abs(latlon.getLon())) + degree + "W";
		}

		text("Latitude: " + latString, keyX + 10, keyY + 800);
		text("Longitude: " + lonString, keyX + 10, keyY + 820);
	}

	// Checks whether this quake occurred on land. If it did, it sets the
	// "country" property of its PointFeature to the country where it occurred
	// and returns true. Notice that the helper method isInCountry will
	// set this "country" property already. Otherwise it returns false.
	private boolean isLand(PointFeature earthquake) {

		// IMPLEMENT THIS: loop over all countries to check if location is in
		// any of them
		// If it is, add 1 to the entry in countryQuakes corresponding to this
		// country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}

		// not inside any country
		return false;
	}

	// prints countries with number of earthquakes
	// You will want to loop through the country markers or country features
	// (either will work) and then for each country, loop through
	// the quakes to count how many occurred in that country.
	// Recall that the country markers have a "name" property,
	// And LandQuakeMarkers have a "country" property set.
	private void printQuakes() {
		// int totalWaterQuakes = quakeMarkers.size();
		// for (Marker country : countryMarkers) {
		// String countryName = country.getStringProperty("name");
		// int numQuakes = 0;
		// for (Marker marker : quakeMarkers)
		// {
		// EarthquakeMarker eqMarker = (EarthquakeMarker)marker;
		// if (eqMarker.isOnLand()) {
		// if (countryName.equals(eqMarker.getStringProperty("country"))) {
		// numQuakes++;
		// }
		// }
		// }
		// if (numQuakes > 0) {
		// totalWaterQuakes -= numQuakes;
		// System.out.println(countryName + ": " + numQuakes);
		// }
		// }
		// System.out.println("OCEAN QUAKES: " + totalWaterQuakes);
	}

	// helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the
	// earthquake feature if
	// it's in one of the countries.
	// You should not have to modify this code
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();
		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use
		// isInsideByLoc
		if (country.getClass() == MultiMarker.class) {
			// looping over markers making up MultiMarker
			for (Marker marker : ((MultiMarker) country).getMarkers()) {
				// checking if inside
				if (((AbstractShapeMarker) marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));
					// return if is inside one
					return true;
				}
			}
		}
		// check if inside country represented by SimplePolygonMarker
		else if (((AbstractShapeMarker) country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));
			return true;
		}
		return false;
	}
}
