function pad(n, width, z) {
	z = z || '0';
	n = n + '';
	return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

function timestamp2prettyPrint(timestamp) {
	var a = new Date(timestamp);
	var months = [ 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug','Sep', 'Oct', 'Nov', 'Dec' ];
	var year = a.getFullYear();
	var month = months[a.getMonth()];
	var date = a.getDate();
	var hour = pad(a.getHours(), 2);
	var min = pad(a.getMinutes(), 2);
	var sec = pad(a.getSeconds(), 2);
	var time = date + ' ' + month + ' ' + year + ' ' + hour + ':' + min + ':' + sec;
	return time;
}

function addShip(ship) {
	ship.lastUpdated=new Date().getTime(); // Add extra field

    // Select ships source
    var dataShip=(ship.source=="EXTERNAL" || ship.source=="CLOUD") ? dataShips[1] : dataShips[0];

	dataShip.ships[mmsiKey(ship)]=ship;
	
	// Create new marker
	var lonLat = new OpenLayers.LonLat(ship.lon, ship.lat).transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());
	
	var popupText = "<table>";
	popupText += "<tr><td colspan='2'><h3><a href='PLACEHOLDER_MMSI"+ship.mmsi+"'><img class='flag' src='file:///android_asset/images/flags/"+ship.countryFlag+".png'>"+ship.name+ " " + ship.mmsi +"</a></h3></td></tr>";
	popupText += "<tr><td>Country:</td><td>" + ship.countryName + "</td></tr>";
	popupText += "<tr><td>Callsign:</td><td>" + ship.callsign + "</td></tr>";
	popupText += "<tr><td>Ship type:</td><td>" + ship.shipType + "</td></tr>";
	popupText += "<tr><td>Destination:</td><td>" + ship.dest + "</td></tr>";
	popupText += "<tr><td>Nav. status:</td><td>" + ship.navStatus+ "</td></tr>";
	popupText += "<tr><td>Speed:</td><td>" + ship.sog + "</td></tr>";
	popupText += "<tr><td>Heading:</td><td>" + ship.heading +"</td></tr>";
	popupText += "<tr><td>Course:</td><td>" + (ship.cog / 10).toFixed(1)+ "</td></tr>";	
//	popupText += "<tr><td><h3>Position</h3></td><td/></tr>";
//	popupText += "<tr><td> - Latitude:</td><td>" + ship.lat + "</td></tr>";
//	popupText += "<tr><td> - Longitude:</td><td>" + ship.lon + "</td></tr>";
//	popupText += "<tr><td><h3>Dimensions</h3></td><td/></td></tr>";
//	popupText += "<tr><td> - Bow:</td><td>" + ship.dimBow + "</td></tr>";
//	popupText += "<tr><td> - Port:</td><td>" + ship.dimPort + "</td></tr>";
//	popupText += "<tr><td> - Starboard:</td><td>" + ship.dimStarboard+ "</td></tr>";
//	popupText += "<tr><td> - Stern:</td><td>" + ship.dimStern + "</td></tr>";
	popupText += "<tr><td>Updated:</td><td>"+ timestamp2prettyPrint(ship.timestamp) + " ("	+ (new Date().getTime() - ship.timestamp) + ")</td></tr>";
	popupText += "<tr><td>Source:</td><td>"+ship.source+"</td></tr>";
	popupText += "</table>";

	// "rot":
	// specialManIndicator
	// subMessage
	// "draught":0,

	var angle = Math.round((ship.heading!=0) ? ship.heading : (ship.cog / 10));
	var origin = new OpenLayers.Geometry.Point(lonLat.lon, lonLat.lat);
	var name=(ship.name=="") ? ship.mmsi : ship.name;

	// Default: 55 x 5 m
	var width=   ((ship.dimBow!="" && ship.dimStern!="") ? parseInt(ship.dimBow)+parseInt(ship.dimStern) : 55)/10*shipScaleFactor; // = real length
	var height=  ((ship.dimStarboard!="" && ship.dimPort!="") ? parseInt(ship.dimStarboard)+parseInt(ship.dimPort) : 5)*shipScaleFactor; // = real width

	var shipFeature1 = new OpenLayers.Feature.Vector(
		// +90 degrees because icon is pointing to the left instead of top
		origin, {
			angle : angle + 90,
			opacity : 100,
			name : name,
			width: width,
			height: height,
			fontColor: 0,
			shipIcon: ship.shipTypeIcon,
			message : popupText
	})
	
	var pts = new Array(origin, new OpenLayers.Geometry.Point(lonLat.lon,lonLat.lat + ((1 + ship.sog) * SPEED_FACTOR)));

	var shipFeature2 = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString(pts), {name : ""	});
	// Rotation angle in degrees (measured counterclockwise from the positive x-axis)
	shipFeature2.geometry.rotate(angle * -1, origin);

	var shipFeatures = [ shipFeature2, shipFeature1 ];
	dataShip.shipVectors.addFeatures(shipFeatures);

	// We know this ship already. Put marker at new position and draw trace
	var previousMarkers = dataShip.markers[mmsiKey(ship)];
	if (previousMarkers != null) {

		// Remove previous marker
		dataShip.shipVectors.removeFeatures(previousMarkers);

		// Create trace line from previous to new position
		var points = new Array(previousMarkers[0].geometry.getCentroid(),new OpenLayers.Geometry.Point(lonLat.lon, lonLat.lat));

		var line = new OpenLayers.Geometry.LineString(points);

		var strokeColor = pad(ship.mmsi, 6);
		var strokeColor = strokeColor.substring(strokeColor.length - 7,	strokeColor.length - 1);
		// console.log("strokeColor: "+strokeColor);

		var lineFeature = new OpenLayers.Feature.Vector(line, null,{
			strokeColor : '#' + strokeColor,
			strokeOpacity : 0.5,
			strokeWidth : 2
		});
		
		dataShip.lineLayer.addFeatures([ lineFeature ]);
		dataShip.traces[new Date().getTime()] = lineFeature;
	}
	
	if (!disableSound && ship.name!="" && typeof dataShip.shipsNamePlayed[mmsiKey(ship)] === 'undefined') {
		// console.log("Play sound");

		var gongListener = function(event) {
			audioName.src = "audio/" + mmsiKey(ship) + ".wav";
			//console.log("Play: " + audioName.src);
			audioName.play();

			document.querySelector("#audioGong").removeEventListener("ended",gongListener);
		}

		if (ship.audioAvailable){
			document.querySelector("#audioGong").addEventListener("ended",gongListener);
		}
		
		audioGong.play();
		dataShip.shipsNamePlayed[mmsiKey(ship)] = true;
	}

	// Replace/add new marker to map of markers
	dataShip.markers[mmsiKey(ship)] = shipFeatures;
}

function autoZoom(){
	var ext=dataShips[0].shipVectors.getDataExtent();

    // No ships received INTERNAL, use EXTERNAL data to zoom to extent
	if (Object.keys(dataShips[0].ships).length==0 && Object.keys(dataShips[1].ships).length>0){
	    ext=dataShips[1].shipVectors.getDataExtent();
	}

	if (ext!=null){
		// Extend data extent with layerMyPosition-data extent
		ext.extend(layerMyPosition.getDataExtent());
	} else {
		ext=layerMyPosition.getDataExtent();
	}

	if (ext!=null){
		map.zoomToExtent(ext);
		//map.baseLayer.redraw();
	}
}

function cleanup() {
	// Remove after 20 minutes:
	// - Old markers (ship+speed)
	// - Old traces

	var now = new Date().getTime();

	for (i=0;i<NUMBER_OF_SHIPS_SOURCES;i++){
        // Remove ships
        for (keyMmsi in dataShips[i].ships) {
            if (dataShips[i].ships.hasOwnProperty(keyMmsi)) {
                    var ship=dataShips[i].ships[keyMmsi];
                    var timestamp=ship.timestamp;

                    var age=(now-timestamp);

                    if (age>(maxAge/2)){
                        // Indicating ship will be removed in the (near) future
                        // Change label color on second feature (index 1)
                        dataShips[i].markers[keyMmsi][1].attributes.fontColor='#FF0000';
                        dataShips[i].shipVectors.redraw();

                        if (age>maxAge){
                            android.log("Removing ship: "+ship.mmsi+" ("+ship.name+"), Timestamp: "+timestamp+" (Age: "+age+")");

                            // Remove ship markers
                            dataShips[i].shipVectors.removeFeatures(dataShips[i].markers[keyMmsi]);

                            // Remove ship from administration
                            delete dataShips[i].markers[keyMmsi];
                            delete dataShips[i].shipsNamePlayed[keyMmsi];
                            delete dataShips[i].ships[keyMmsi];

                            printStatistics(i);
                        }
                    }
            }
        }

        // ^0$|^[1-9]\d*$/.test(keyTimestamp) &&
        // keyTimestamp <= 4294967294

        // Remove traces
        for (keyTimestamp in dataShips[i].traces) {
            if (dataShips[i].traces.hasOwnProperty(keyTimestamp)){
                    if ((now-keyTimestamp)>maxAge){
                        android.log("Removing trace - Timestamp: "+keyTimestamp+" (Age: "+(now-keyTimestamp)+")");

                        // Remove trace
                        dataShips[i].lineLayer.removeFeatures(dataShips[i].traces[keyTimestamp]);

                        // Remove trace from administration
                        delete dataShips[i].traces[keyTimestamp];
                    }
                }
        }
	}
}

function printStatistics(i){
    android.log("-------------------------------------");
    var dataShip=dataShips[i];
	android.log("# Ships: "+Object.keys(dataShip.ships).length);
	android.log("# Markers: "+Object.keys(dataShip.markers).length);
	android.log("# Ships (name said): "+Object.keys(dataShip.shipsNamePlayed).length);
	android.log("# Traces: "+Object.keys(dataShip.traces).length);
	android.log("# ShipVectors: "+Object.keys(dataShip.shipVectors).length);
	android.log("# LineLayer: "+Object.keys(dataShip.lineLayer).length);
	android.log("# prefetchedTiles: "+prefetchedTiles.length);
	android.log("-------------------------------------");
}

function calculateAndPrefetchTileUrl(bounds) {	
	var result=null;	

	// When not set to prefetch tiles: Use as minimum zoom, the current zoom level. In this way the for-loop will just run once.
	// Otherwise use 1, so that lower zoom levels from the current to the whole world view will be fetched.
	var minimumZoom=prefetchLowerZoomLevelsTiles ? 1 : this.map.getZoom();
	
	for (zoom=this.map.getZoom();zoom>=minimumZoom;zoom--){
		var path=calculateTilePath(bounds,this.map.getResolution(),this.maxExtent,this.tileSize,zoom,this.type);
	
		if (path!=null){
			var url=this.url;
			if (url instanceof Array) {
				url = this.selectUrl(path, url);
			}
			
			var source=url+path;

			if (zoom==this.map.getZoom()){
				result=source;
			} else {
				var imageId=((new Date().getMilliseconds())*1000)+zoom;
				prefetchedTiles[imageId]=new Image();
				prefetchedTiles[imageId].onload = function(){
					//console.log("calculateAndPrefetchTileUrl - image loaded: "+this.src);
				};
				prefetchedTiles[imageId].src=source;
			}
		}
	}

	return result;
}

function calculateTilePath(bounds,res,maxExtent,tileSize,z,typ) {
	var result=null;	
	
	var x = Math.round((bounds.left - maxExtent.left) / (res * tileSize.w));
	var y = Math.round((maxExtent.top - bounds.top) / (res * tileSize.h));

	var limit = Math.pow(2, z);
	if (y < 0 || y >= limit) {
		result=null;
	} else {
		x = ((x % limit) + limit) % limit;
		
		// Path
		result=z + "/" + x + "/" + y + "." + typ;
	}
	return result;
}

var mmsiKey = function(obj) {
	// some unique object-dependent key
	return obj.mmsi;
};

function createMap(){
	map = new OpenLayers.Map("mapdiv", {
		projection : new OpenLayers.Projection("EPSG:900913"),
		displayProjection : new OpenLayers.Projection("EPSG:4326"),
		controls : [ new OpenLayers.Control.Navigation(),
				new OpenLayers.Control.ScaleLine({
					topOutUnits : "nmi",
					bottomOutUnits : "km",
					topInUnits : 'nmi',
					bottomInUnits : 'km',
					maxWidth : '40'
				}),
				new OpenLayers.Control.LayerSwitcher(),
				new OpenLayers.Control.MousePosition(),
				// new OpenLayers.Control.PanZoomBar(),
				new OpenLayers.Control.TouchNavigation()],
		numZoomLevels : ZOOM_LEVELS,
		maxResolution : 156543,
		units : 'meters'
	});

	window.onresize = function(){
        setTimeout( function() {
            map.updateSize();
        }, 200);
    }
}


function createStyleMapShipSymbol(){
  	styleMapShipSymbol = new OpenLayers.StyleMap({
  		"default" : new OpenLayers.Style({
  			externalGraphic : "file:///android_asset/images/${shipIcon}",
  			graphicWidth : "${width}",
  			graphicHeight: "${height}",
  			// graphicXOffset: -40,
  			// graphicYOffset: -40,
  			rotation : "${angle}",
  			fillOpacity : "${opacity}",
  			label : "${name}",
  			fontColor : "${fontColor}",
  			fontSize : "12px",
  			fontFamily : "Courier New, monospace",
  			fontWeight : "bold",
  			labelAlign : "left",
  			labelXOffset : "0",
  			labelYOffset : "-20",
  			labelOutlineColor : "white",
  			labelOutlineWidth : 3,
  			strokeColor : "#00FF00",
  			strokeOpacity : 1,
  			strokeWidth : 3,
  			fillColor : "#FF5500"
  		}),
  		
  		"select" : new OpenLayers.Style({
  			cursor : "crosshair",
  		})
  	});
}

function createLayers(){
	// Layer: My position
	layerMyPosition = new OpenLayers.Layer.Markers("My position");
	map.addLayer(layerMyPosition);
	
	// Layer: OSM
	//var layerOsm = new OpenLayers.Layer.OSM("OpenStreetMap",
	//[ 'http://127.0.0.1:8181/a.tile.openstreetmap.org/${z}/${x}/${y}.png'], null);
	//["http://"+TILE_PROXY_URL+"a.tile.openstreetmap.org/","http://"+TILE_PROXY_URL+"b.tile.openstreetmap.org/","http://"+TILE_PROXY_URL+"c.tile.openstreetmap.org/"]
	
	var layerOsm = new OpenLayers.Layer.OSM("OpenStreetMap",
			"http://"+TILE_PROXY_URL+"a.tile.openstreetmap.org/", {
				numZoomLevels : ZOOM_LEVELS,
				type : 'png',
				getURL : calculateAndPrefetchTileUrl,
				isBaseLayer : true,
				displayOutsideMaxExtent : true
			});
	
	var layerSeamark = new OpenLayers.Layer.TMS("OpenSeaMap",
			"http://"+TILE_PROXY_URL+"tiles.openseamap.org/seamark/", {
				numZoomLevels : ZOOM_LEVELS,
				type : 'png',
	  				getURL : calculateAndPrefetchTileUrl,
	  				isBaseLayer : false,
	  				displayOutsideMaxExtent : true
	  			});
 	map.addLayers([ layerOsm, layerSeamark ]);
}

function createLayerShips(layerName){
	// Layer: Ships
  	var result=new OpenLayers.Layer.Vector(layerName, {
  		eventListeners : {
  			'featureclick' : function(evt) {
  				console.log("featureselected");
  				
  				var feature = evt.feature;
  	
  				if (typeof feature.attributes.message !== 'undefined' && feature.attributes.message != "") {
  					// Must create a popup on ship symbol
  					var popup = new OpenLayers.Popup.FramedCloud("popup",OpenLayers.LonLat.fromString(feature.geometry.toShortString()),new OpenLayers.Size(200,800),feature.attributes.message, null, true);
  					//popup.autoSize = true;
  					//popup.maxSize = new OpenLayers.Size(20,80);
  					//popup.fixedRelativePosition = true;
  					
  					feature.popup = popup;
  					map.addPopup(popup,true);
  				}
  			},
  			'featureunselected' : function(evt) {
  				console.log("featureunselected");
  				
  				var feature = evt.feature;
  				if (feature.popup != null) {
  					map.removePopup(feature.popup);
  					feature.popup.destroy();
  					feature.popup = null;
  				}
  			},
            'visibilitychanged': function(evt) {
                console.log("visibilitychanged");
                var layer = evt.object;
                // Call Android code
                android.showLayerVisibilityChanged(layer.name,layer.visibility);
            }
  		},
  		styleMap : styleMapShipSymbol
  	});

    map.addLayers([result]);
  	return result;
}

function createControls(){
    for (i=0;i<NUMBER_OF_SHIPS_SOURCES;i++){
        var selectControl = new OpenLayers.Control.SelectFeature(dataShips[i].shipVectors, {hover : true});
        map.addControl(selectControl);
        selectControl.activate();
    }
}

function createLayerTraces(layerName){
  	// Layer: Traces
  	var result=new OpenLayers.Layer.Vector(layerName);
  	map.addLayer(result);
  	map.addControl(new OpenLayers.Control.DrawFeature(result,OpenLayers.Handler.Path));
  	return result;
}

function createZoomAction(){
  	// Zoom (in/out) ships
  	var zoomSquared=ZOOM_LEVELS*ZOOM_LEVELS;
  	map.events.register("zoomend", map, function() {
  	        // http://gis.stackexchange.com/questions/31943/how-to-resize-a-point-on-zoom-out
  	        //var new_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
  	        var stle=styleMapShipSymbol.styles['default'].defaultStyle;
  	        stle.pointRadius=((45/zoomSquared)*map.getZoom()*map.getZoom());
  	        //stle.graphicWidth=((75/zoomSquared)*map.getZoom()*map.getZoom());
  	        //stle.fontSize=((12/zoomSquared)*map.getZoom()*map.getZoom());
  	        //shipVectors.redraw();
  	        
  	        map.baseLayer.redraw();
  	});
}

// Called from Java
function setCurrentPosition(lon,lat){
	var lonLat = new OpenLayers.LonLat(lon, lat).transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());
	
	if (previousMyPositionMarker!=null){
	    layerMyPosition.removeMarker(previousMyPositionMarker);
		previousMyPositionMarker.destroy();
		previousMyPositionMarker=null;
	}
	
	var size = new OpenLayers.Size(50, 50);
	var offset = new OpenLayers.Pixel(-(size.w/2), -(size.h/2)); // Middle
	//var icon = new OpenLayers.Icon('http://www.openstreetmap.org/openlayers/img/marker.png',size,offset);
	var icon = new OpenLayers.Icon('file:///android_asset/images/'+ownLocationIcon,size,offset);

	previousMyPositionMarker=new OpenLayers.Marker(lonLat,icon);
	layerMyPosition.addMarker(previousMyPositionMarker);
	
	if (zoomToExtent) {
		autoZoom();
	}
}

// Incoming data
// Called from Java
function onShipReceived(data){
    //$('#messages').append(data).append("<br />");

    var ship=null;

    try {
        ship=JSON.parse(data);
    } catch (e){
        console.log(e);
    }

    if (ship!=null){
        var now = new Date().getTime();
        var age=(now-ship.timestamp);

        if (age<maxAge){
            // Only add ship when it is not too old
            addShip(ship);

            cleanup();

            if (!init || zoomToExtent) {
                init = true;
                autoZoom();
            }
        }
    }
}

// Called from Java
function setZoomToExtent(zoomToExtentIn){
	zoomToExtent=zoomToExtentIn;

	var but = $('#but_zoom_to_extent')[0];
    but.innerText=((zoomToExtent) ? "DISABLE" : "ENABLE")+ " - Autozoom";
}

// Called from Java
function setDisableSound(disableSoundIn){
	disableSound=disableSoundIn;
}

//Called from Java
function setPrefetchLowerZoomLevelsTiles(prefetchLowerZoomLevelsTilesIn){
	prefetchLowerZoomLevelsTiles=prefetchLowerZoomLevelsTilesIn;
}

// Called from Java
function setShipScaleFactor(shipScaleFactorIn){
	shipScaleFactor=shipScaleFactorIn;
}

// Called from Java
function setOwnLocationIcon(ownLocationIconIn){
	ownLocationIcon=ownLocationIconIn;
}

// Called from Java
function setMaxAge(maxAgeIn){
	maxAge=(1000*60*maxAgeIn);
}

/*************************************************************************************************************************** */

const TILE_PROXY_URL="127.0.0.1:8181/";
const ZOOM_LEVELS=18;
const SPEED_FACTOR=25;
const DEFAULT_SHIP_SCALE_FACTOR=5;
const DEFAULT_OWN_LOCATION_ICON="antenna.png";
const NUMBER_OF_SHIPS_SOURCES=2;
const DEFAULT_MAX_AGE=(1000*60*20); // 20 minutes

// Array of dataShip
// - at index 0: Ships received INTERNAL
// - at index 1: Ships received EXTERNAL (from CLOUD, is a special type of EXTERNAL source)
var dataShips=[];
for (i=0;i<NUMBER_OF_SHIPS_SOURCES;i++){
 var dataShip=new Object();
 dataShip.ships={};
 dataShip.shipsNamePlayed = {};
 dataShip.markers = {};
 dataShip.traces = {};
 //dataShip.shipVectors
 //dataShip.lineLayer
 dataShips.push(dataShip);
}

var prefetchedTiles=new Array(); // Array of images
var zoomToExtent=false;
var disableSound=false;
var prefetchLowerZoomLevelsTiles=true;
var init = false;
var previousMyPositionMarker=null;
var map;
var layerMyPosition;
var styleMapShipSymbol;
var shipScaleFactor=DEFAULT_SHIP_SCALE_FACTOR;
var ownLocationIcon=DEFAULT_OWN_LOCATION_ICON;
var maxAge = DEFAULT_MAX_AGE;

