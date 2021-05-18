import { Component, SimpleChanges, Input, Renderer2, ChangeDetectorRef, Output, EventEmitter, Inject } from '@angular/core';
import MarkerClusterer from '@googlemaps/markerclustererplus';
import { ServoyBaseComponent, BaseCustomObject, ServoyPublicService, LoggerFactory, LoggerService, JSEvent, EventLike, WindowRefService } from '@servoy/public';
import { DOCUMENT } from '@angular/common';

@Component({
    selector: 'googlemaps-svy-G-Maps',
    templateUrl: './svygmaps.html'
})
export class SvyGMaps extends ServoyBaseComponent<HTMLDivElement> {
    @Input() addressTitle: any;
    @Input() apiKey: any;
    @Input() directionsSettings: RouteSettings;
    @Input() fullscreenControl: boolean;
    @Input() gestureHandling: string;
    @Input() KmlLayerURL: any;
    @Input() mapEvents: Array<string>;
    @Input() mapType: string;
    @Input() mapTypeControl: boolean;
    @Input() markerEvents: Array<string>;

    @Input() markers: Array<Marker>;
    @Output() markersChange = new EventEmitter();

    @Input() streetViewControl: boolean;
    @Input() styleClass: string;
    @Input() useGoogleMapCluster: boolean;
    @Input() useGoogleMapDirections: boolean;
    @Input() zoomControl: boolean;
    @Input() zoomLevel: any;
    @Output() zoomLevelChange = new EventEmitter();

    @Input() onMapEvent: (e: JSEvent, latlng: any) => void;
    @Input() onMarkerEvent: (e: JSEvent, index: number, latlng: any) => void;
    @Input() onMarkerGeocoded: (marker: Marker, ltdlng: LatitudeLongitude) => void;
    @Input() onRouteChanged: (route: RouteResult) => void;

    map: google.maps.Map;
    mapMarkers: Map<string, google.maps.Marker>;
    directionsDisplay: google.maps.DirectionsRenderer;
    geocoder: google.maps.Geocoder;
    kmlLayer: google.maps.KmlLayer;
    getScriptInt: any;

    private log: LoggerService;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, logFactory: LoggerFactory,
        private servoyService: ServoyPublicService, private windowRefService: WindowRefService, @Inject(DOCUMENT) private document: Document) {
        super(renderer, cdRef);
        this.log = logFactory.getLogger('svy-google-maps');
    }

    svyOnInit() {
        super.svyOnInit();
        this.windowRefService.nativeWindow['googleMapsLoadedCallback'] = () => {
            this.initMap();
        }

        if (this.windowRefService.nativeWindow['google'] && this.windowRefService.nativeWindow['google'].maps) {
            this.initMap();
        } else {
            //set an interval to wait for apiKey dataprovider to be binded before trying to load google's api.
            this.getScriptInt = this.windowRefService.nativeWindow.setInterval(() => {
                if (!this.apiKey) {
                    this.showErrMessage();
                    this.unloadScript();
                } else {
                    this.windowRefService.nativeWindow.clearInterval(this.getScriptInt);
                    this.loadScript();
                }
            });
        }
    }


    svyOnChanges(changes: SimpleChanges) {
        super.svyOnChanges(changes);
        if (changes['markers']) {
            if (this.geocoder) {
                let location = [];
                for (const googleMarker of this.markers) {
                    if (this.mapMarkers && googleMarker.markerId && this.mapMarkers.has(googleMarker.markerId)) {
                        location.push(this.mapMarkers[googleMarker.markerId].getPosition());
                    } else if (googleMarker.position != null) {
                        location.push(new google.maps.LatLng(googleMarker.position.lat, googleMarker.position.lng));
                    }
                    else if (googleMarker.addressDataprovider || googleMarker.addressString) {
                        location.push(this.getLatLng(googleMarker.addressDataprovider || googleMarker.addressString));
                    }
                }
                Promise.all(location).then((returnVals) => {
                    for (var i in returnVals) {
                        location[i] = returnVals[i];
                        var marker = this.markers[i];
                        if (!marker.position || marker.position.lat == null || marker.position.lng == null) {
                            //add geocode result to marker                          
                            marker.position = this.createLatLngObj(location[i]);
                            if (this.onMarkerGeocoded) {
                                this.onMarkerGeocoded(marker, marker.position);
                            }
                        }
                    }
                }).then(() => {
                    //update markers and clear removed markers
                    for (var m in this.mapMarkers) {
                        var modelMarker = null;
                        for (var n = 0; n < this.markers.length; n++) {
                            if (this.markers[n].markerId == m) {
                                modelMarker = this.markers[n];
                                break;
                            }
                        }
                        if (!modelMarker) {
                            this.mapMarkers[m].setMap(null);
                            this.mapMarkers.delete(m);
                        } else {
                            //set changed properties
                            var markerOptions = {
                                animation: modelMarker.animation ? google.maps.Animation[modelMarker.animation.toUpperCase()] : null,
                                clickable: modelMarker.clickable,
                                draggable: modelMarker.draggable,
                                crossOnDrag: modelMarker.crossOnDrag,
                                cursor: modelMarker.cursor,
                                icon: modelMarker.iconUrl || modelMarker.iconMedia,
                                label: modelMarker.iconLabel,
                                opacity: modelMarker.opacity,
                                visible: modelMarker.visible,
                                zIndex: modelMarker.zIndex,
                                title: modelMarker.title || modelMarker.tooltip //TODO remove tooltip (deprecated)
                            }

                            if (modelMarker.position != null) {
                                this.mapMarkers[m].setPosition(modelMarker.position);
                            } else if (modelMarker.latitude != null && modelMarker.longitude != null) {
                                //TODO remove support for deprecated latitude/longitude properties
                                this.mapMarkers[m].setPosition(new google.maps.LatLng(modelMarker.latitude, modelMarker.longitude));
                            }


                            this.mapMarkers[m].setOptions(markerOptions);
                        }
                    }

                    if (this.useGoogleMapDirections == true) {
                        //remove markers
                        this.mapMarkers = new Map()
                        var directionsService = new google.maps.DirectionsService;
                        if (this.directionsDisplay) {
                            this.directionsDisplay.setMap(null);
                        }
                        this.directionsDisplay = new google.maps.DirectionsRenderer;

                        this.directionsDisplay.setMap(this.map);
                        this.calculateAndDisplayRoute(directionsService, location);
                    } else {
                        var markers = location.map((loc, i) => {
                            //return existing marker or create new marker
                            return this.mapMarkers[this.markers[i].markerId] || this.createMarker(loc, i);
                        });

                        location = location.filter((loc) => {
                            return loc != null;
                        })

                        if (location.length > 1) {
                            var bounds = new google.maps.LatLngBounds();
                            for (var i in markers) {
                                bounds.extend(markers[i].position);
                            }
                            this.map.fitBounds(bounds);
                        } else if (location.length == 1) {
                            this.map.setCenter(markers[0].position);
                        }
                    }
                });
            } else {
                this.createMap();
            }
        }
        if (changes['zoomLevel'] && this.map) {
            try {
                this.map.setZoom(this.zoomLevel);
            } catch (e) { }
        }
        if (changes['KmlLayerURL'] && this.map) {
            if (this.KmlLayerURL) {
                this.kmlLayer = new google.maps.KmlLayer({
                    url: this.KmlLayerURL,
                    map: this.map
                });
            } else {
                if (this.kmlLayer) {
                    this.kmlLayer.setMap(null);
                }
            }
        }

    }

    createMap() {
        if (!this.geocoder) {
            //TODO return error
            return;
        }
        //clear markers from map
        if (this.mapMarkers) {
            for (let m in this.mapMarkers) {
                this.mapMarkers[m].setMap(null);
            }
        }
        this.mapMarkers = new Map();

        var location = [];
        for (let googleMarker of this.markers) {
            if (googleMarker.position != null) {
                location.push(new google.maps.LatLng(googleMarker.position.lat, googleMarker.position.lng));
            } else if (googleMarker.addressDataprovider || googleMarker.addressString) {
                location.push(this.getLatLng(googleMarker.addressDataprovider || googleMarker.addressString));
            }
        }
        Promise.all(location).then((returnVals) => {
            for (let i in returnVals) {
                location[i] = returnVals[i]
            }
        }).then(() => {
            this.createMapAtPoint(location)
        })
    }

    getLatLng(address) {
        return new Promise((resolve, reject) => {
            this.geocoder.geocode({
                address: address
            }, (results, status) => {
                if (status == google.maps.GeocoderStatus.OK) {
                    resolve(results[0].geometry.location);
                } else if (status == google.maps.GeocoderStatus.OVER_QUERY_LIMIT) {
                    this.log.warn('Google maps Geocoder query limit reached, geocoding pauses for 2 seconds');
                    this.sleep(2000);
                    resolve(this.getLatLng(address));
                } else {
                    this.log.error('Could not geocode location ' + address);
                    resolve(null);
                }
            });
        });
    }

    calculateAndDisplayRoute(directionsService: google.maps.DirectionsService, location: Array<google.maps.LatLng>) {
        let routeSettings = this.directionsSettings;
        //When not set go for defaults
        if (!routeSettings) {
            routeSettings = {
                "optimize": true,
                "travelMode": "driving",
                "avoidFerries": false,
                "avoidHighways": false,
                "avoidTolls": false
            }
        }

        let waypts = [];
        for (var i = 1; i < (location.length - 1); i++) {
            waypts.push({
                location: new google.maps.LatLng(location[i].lat(), location[i].lng()),
                stopover: true
            });
        }

        directionsService.route({
            origin: new google.maps.LatLng(location[0].lat(), location[0].lng()),
            destination: new google.maps.LatLng(location[location.length - 1].lat(), location[location.length - 1].lng()),
            waypoints: waypts,
            travelMode: google.maps.TravelMode[routeSettings.travelMode.toUpperCase()],
            optimizeWaypoints: routeSettings.optimize,
            avoidFerries: routeSettings.avoidFerries,
            avoidHighways: routeSettings.avoidHighways,
            avoidTolls: routeSettings.avoidTolls
        }, (response, status) => {
            if (status === 'OK') {
                this.directionsDisplay.setDirections(response);
                let calculatedRoute: RouteResult = {};
                calculatedRoute.legs = [];

                let totalMeters = 0;
                let totalSeconds = 0;

                for (let l = 0; l < response.routes[0].legs.length; l++) {
                    let routeLeg = response.routes[0].legs[l];
                    let startMarkerIndex;
                    if (l == 0) {
                        //first leg starts at start marker
                        startMarkerIndex = 0;
                    } else {
                        //subsequent legs start at marker (waypoint + 1)
                        startMarkerIndex = response.routes[0].waypoint_order[l - 1] + 1;
                    }
                    let endMarkerIndex;
                    if (l == (response.routes[0].legs.length - 1)) {
                        //last leg ends at end marker
                        endMarkerIndex = this.markers.length - 1;
                    } else {
                        //prior markers end at marker (waypoint + 1)
                        endMarkerIndex = response.routes[0].waypoint_order[l] + 1;
                    }
                    var leg = {
                        start_address: routeLeg.start_address,
                        start_markerId: this.markers[startMarkerIndex].markerId,
                        end_address: routeLeg.end_address,
                        end_markerId: this.markers[endMarkerIndex].markerId,
                        distance: routeLeg.distance.text,
                        distance_meters: routeLeg.distance.value,
                        duration: routeLeg.duration.text,
                        duration_seconds: routeLeg.duration.value,
                    }
                    totalMeters += leg.distance_meters;
                    totalSeconds += leg.duration_seconds;
                    calculatedRoute.legs.push(leg);
                }

                calculatedRoute.total_distance = totalMeters;
                calculatedRoute.total_duration = totalSeconds;

                if (this.onRouteChanged) {
                    this.onRouteChanged(calculatedRoute);
                }

            } else {
                this.log.warn('Directions request failed due to ' + status);
            }
        });
    }

    createMarker(location, markerIndex: number) {
        if (!location) {
            return null;
        }

        let marker = this.markers[markerIndex];

        let markerObj = {
            position: new google.maps.LatLng(location.lat(), location.lng()),
            map: this.map,
            title: marker.title,
            draggable: marker.draggable,
            animation: marker.animation ? google.maps.Animation[marker.animation.toUpperCase()] : null,
            clickable: marker.clickable,
            crossOnDrag: marker.crossOnDrag,
            opacity: marker.opacity != null ? marker.opacity : null,
            visible: marker.visible,
            zIndex: marker.zIndex != null ? marker.zIndex : null,
            markerId: marker.markerId || ('marker-' + markerIndex),
            markerIndex: markerIndex,
            icon: marker.iconUrl || marker.iconMedia,
            label: marker.iconLabel,
            cursor: marker.cursor
        }

        var gMarker = new google.maps.Marker(markerObj)
        this.mapMarkers[marker.markerId || ('marker-' + markerIndex)] = gMarker;

        if (marker.infoWindowString) {
            var infowindow = new google.maps.InfoWindow({
                content: marker.infoWindowString
            });
            gMarker.addListener('click', () => {
                infowindow.open(this.map, gMarker);
            });
        }

        if (this.onMarkerEvent && this.markerEvents) {
            for (var e = 0; e < this.markerEvents.length; e++) {
                let eventType = this.markerEvents[e];
                ((eventType) => {
                    gMarker.addListener(eventType, (evt) => {
                        var jsEvent = this.createJSEvent(evt, eventType);
                        jsEvent.data.marker = marker;
                        var index = -1;
                        for (var i = 0; i < this.markers.length; i++) {
                            if (this.markers[i].markerId == marker.markerId) {
                                index = i;
                                break;
                            }
                        }
                        if (eventType == 'dragend' && jsEvent.data.latLng) {
                            //marker position changed
                            this.markers[index].position = jsEvent.data.latLng;
                            //apply changes before calling the handler
                            this.markersChange.emit(this.markers);
                        }

                        this.onMarkerEvent(jsEvent, index, jsEvent.data && jsEvent.data.latLng ? jsEvent.data.latLng : null);
                    });
                })(eventType);
            }
        }

        if (marker.drawRadius == true) {
            const circle = new google.maps.Circle({
                map: this.map,
                radius: marker.radiusMeters || 2000,
                fillColor: marker.radiusColor || "#AA0000",
                strokeColor: marker.radiusColor || "#AA0000"
            });
            circle.bindTo('center', gMarker, 'position');
        }
        return gMarker
    }

    createLatLngObj(latLng: google.maps.LatLng) {
        let result = null;
        if (latLng) {
            result = {
                lat: latLng.lat(),
                lng: latLng.lng()
            }
        }
        return result;
    }

    createJSEvent(evt, eventType) {
        var mouseEvent = null;
        for (let p in evt) {
            if (evt[p] instanceof MouseEvent) {
                mouseEvent = evt[p];
                break;
            }
        }
        if (!mouseEvent || !mouseEvent.target) {
            //create fake event
            let me: EventLike = { target: this.getNativeElement() };
            if (mouseEvent) {
                me.altKey = mouseEvent.altKey;
                me.shiftKey = mouseEvent.shiftKey;
                me.ctrlKey = mouseEvent.ctrlKey;
                me.metaKey = mouseEvent.metaKey;
                me.pageX = mouseEvent.pageX;
                me.pageY = mouseEvent.pageY;
            }
            mouseEvent = me;
        }

        var jsEvent = this.servoyService.createJSEvent(mouseEvent, eventType);
        if (evt && evt.latLng) {
            jsEvent.data = {
                latLng: this.createLatLngObj(evt.latLng)
            }
        }

        if (jsEvent.x === undefined) {
            jsEvent.x = -1;
        }
        if (jsEvent.y === undefined) {
            jsEvent.y = -1;
        }

        return jsEvent;
    }

    createMapAtPoint(location: Array<google.maps.LatLng>) {

        location = location.filter((loc) => {
            return loc != null;
        })

        var mapOptions = {
            center: (location.length == 1 ? new google.maps.LatLng(location[0].lat(), location[0].lng()) : new google.maps.LatLng(0, 0)),
            zoom: this.zoomLevel === null || this.zoomLevel === undefined ? 7 : this.zoomLevel,
            zoomControl: this.zoomControl,
            mapTypeControl: this.mapTypeControl,
            streetViewControl: this.streetViewControl,
            fullscreenControl: this.fullscreenControl,
            mapTypeId: google.maps.MapTypeId[this.mapType],
            gestureHandling: this.gestureHandling
        }

        this.map = new google.maps.Map(this.getNativeElement(), mapOptions);

        //If google maps directions is enabled, create route map.
        if (this.useGoogleMapDirections == true) {
            this.log.info('Google Directions enabled, start building route');
            if (location.length > 1) {
                var directionsService = new google.maps.DirectionsService;

                if (this.directionsDisplay) {
                    this.directionsDisplay.setMap(null);
                }

                this.directionsDisplay = new google.maps.DirectionsRenderer();

                this.directionsDisplay.setMap(this.map);
                this.calculateAndDisplayRoute(directionsService, location);
            } else {
                this.log.error('Google maps directions needs a minimum of 2 locations')
            }
        } else {
            this.mapMarkers = new Map();;
            let markers = location.map((loc, i) => {
                return this.createMarker(loc, i);
            });

            if (this.onMapEvent && this.mapEvents) {
                for (let m = 0; m < this.mapEvents.length; m++) {
                    let eventType = this.mapEvents[m];
                    ((eventType) => {
                        this.map.addListener(eventType, (evt) => {
                            var jsEvent = this.createJSEvent(evt, eventType);
                            this.onMapEvent(jsEvent, jsEvent.data && jsEvent.data.latLng ? jsEvent.data.latLng : null);
                        })
                    })(eventType)
                }
            }

            if (this.useGoogleMapCluster == true) {
                this.log.info('Google Map Clusterview enabled');
                new MarkerClusterer(this.map, markers, { imagePath: 'googlemaps/svyGMaps/libs/images/m' });
            }

            if (location.length > 1) {
                var bounds = new google.maps.LatLngBounds();
                for (var i in markers) {
                    bounds.extend(markers[i].getPosition());
                }
                this.map.fitBounds(bounds);
            }
        }

        //when resizing page re-center the map marker
        google.maps.event.addDomListener(this.windowRefService.nativeWindow, "resize", () => {
            var center = this.map.getCenter();
            google.maps.event.trigger(this.map, "resize");
            this.map.setCenter(center);
        });

        this.map.addListener('zoom_changed', () => {
            if (this.zoomLevel !== null && this.zoomLevel !== undefined) {
                var currLevel = this.map.getZoom();
                if (this.zoomLevel != currLevel) {
                    this.zoomLevel = currLevel;
                    this.zoomLevelChange.emit(this.zoomLevel);
                }
            }
        });
    }

    loadScript() {
        let script = this.document.createElement("script")
        script.id = "googleMapsScript"
        script.type = "text/javascript"
        script.src = "https://maps.googleapis.com/maps/api/js?key=" + this.apiKey + "&callback=googleMapsLoadedCallback"
        this.document.body.appendChild(script);
    }

    //unload google api
    unloadScript() {
        try {
            var script = this.document.getElementById('googleMapsScript');
            script.parentElement.removeChild(script);
            var errContainer = this.document.getElementsByClassName('gm-err-container')[0];
            errContainer.parentElement.removeChild(errContainer);
        } catch (e) {

        }
    }
    //show error message indicating API key not yet loaded.
    showErrMessage() {
        try {
            this.getNativeElement().innerHTML = '<h2> : ( NO API KEY LOADED YET... </h2>'
        } catch (e) { }
    }

    initMap() {
        this.geocoder = new google.maps.Geocoder()
        this.createMap();
    }

    centerMap(latlong) {
        this.map.setCenter(latlong);
    }

    getBounds() {
        if (this.map) {
            var latLngBounds = this.map.getBounds();
            if (latLngBounds) {
                var sw = latLngBounds.getSouthWest();
                var ne = latLngBounds.getNorthEast();
                return {
                    sw: this.createLatLngObj(sw),
                    ne: this.createLatLngObj(ne),
                }
            }
        }
        return null;
    }

    fitBounds(boundsToFit) {
        if (this.map) {
            let bounds = new google.maps.LatLngBounds(boundsToFit.sw, boundsToFit.ne);
            this.map.fitBounds(bounds)
        }
    }

    getCenter() {
        if (this.map) {
            return this.createLatLngObj(this.map.getCenter());
        }
        return null;
    }

    refresh() {
        this.createMap()
        return true;
    }
    centerAtAddress(address) {
        if (address) {
            this.getLatLng(address).then((location) => {
                this.centerMap(location);
                return true;
            });
        }
        return false;
    }

    centerAtLatLng(lat, lng) {
        if (lat != null && lng != null) {
            this.centerMap(new google.maps.LatLng(lat, lng))
            return true;
        }
        return false;
    }



    sleep(ms: number) {
        var d = new Date();
        d.setTime(d.getTime() + ms);
        while (new Date().getTime() < d.getTime()) { }
    }

}

export class Marker extends BaseCustomObject {
    addressDataprovider: any;
    addressString: string;
    cursor: string;
    position: LatitudeLongitude;
    iconLabel: string;
    title: string;
    iconUrl: string;
    iconMedia: string;
    infoWindowString: string;
    drawRadius: boolean;
    radiusMeters: number;
    radiusColor: string;
    draggable: boolean;
    animation: string;
    clickable: boolean;
    crossOnDrag: boolean;
    opacity: number;
    visible: boolean;
    zIndex: number;
    markerId: any;
    userObject: any;
}

export class RouteSettings {
    optimize: boolean;
    travelMode: string;
    avoidFerries: boolean;
    avoidHighways: boolean;
    avoidTolls: boolean;
}

export class LatitudeLongitude {
    lat: number;
    lng: number;
}

class RouteResult {
    legs?: Array<Leg>;
    total_distance?: number;
    total_duration?: number;
}

class Leg {
    start_address: string;
    start_markerId: any;
    end_address: string;
    end_markerId: any;
    distance: string;
    distance_meters: number;
    duration: string;
    duration_seconds: number;
}
