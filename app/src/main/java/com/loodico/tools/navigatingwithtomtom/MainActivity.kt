package com.loodico.tools.navigatingwithtomtom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import com.tomtom.sdk.common.location.GeoCoordinate
import com.tomtom.sdk.common.location.GeoLocation
import com.tomtom.sdk.common.route.Route
import com.tomtom.sdk.common.route.section.travelmode.TravelMode
import com.tomtom.sdk.location.OnLocationUpdateListener
import com.tomtom.sdk.location.android.AndroidLocationEngine
import com.tomtom.sdk.location.mapmatched.MapMatchedLocationEngine
import com.tomtom.sdk.location.simulation.SimulationLocationEngine

import com.tomtom.sdk.maps.display.MapOptions
import com.tomtom.sdk.maps.display.TomTomMap
import com.tomtom.sdk.maps.display.camera.CameraOptions
import com.tomtom.sdk.maps.display.camera.CameraTrackingMode
import com.tomtom.sdk.maps.display.common.screen.Padding
import com.tomtom.sdk.maps.display.location.LocationMarkerOptions
import com.tomtom.sdk.maps.display.location.LocationMarkerType
import com.tomtom.sdk.maps.display.route.Instruction
import com.tomtom.sdk.maps.display.route.RouteOptions
import com.tomtom.sdk.maps.display.ui.MapFragment
import com.tomtom.sdk.navigation.NavigationConfiguration
import com.tomtom.sdk.navigation.NavigationError
import com.tomtom.sdk.navigation.RoutePlan
import com.tomtom.sdk.navigation.TomTomNavigation
import com.tomtom.sdk.navigation.ui.NavigationFragment
import com.tomtom.sdk.navigation.ui.NavigationUiOptions
import com.tomtom.sdk.routing.api.*
import com.tomtom.sdk.routing.api.description.SectionType
import com.tomtom.sdk.routing.api.guidance.AnnouncementPoints
import com.tomtom.sdk.routing.api.guidance.InstructionPhoneticsType
import com.tomtom.sdk.routing.api.guidance.InstructionType
import com.tomtom.sdk.routing.online.OnlineRoutingApi
import com.tomtom.sdk.search.online.client.OnlineSearchApi
import com.tomtom.sdk.search.ui.SearchFragment
import com.tomtom.sdk.search.ui.SearchFragmentListener
import com.tomtom.sdk.search.ui.model.Place
import com.tomtom.sdk.search.ui.model.SearchApiParameters
import com.tomtom.sdk.search.ui.model.SearchProperties

class MainActivity : AppCompatActivity() {
    private lateinit var navigationFragment: NavigationFragment
    private lateinit var tomtomNavigation: TomTomNavigation
    private lateinit var route: Route
    private lateinit var planRouteOptions: PlanRouteOptions
    private lateinit var locationEngine: AndroidLocationEngine
    private lateinit var tomTomMap: TomTomMap
    private val APIKEY= "YOUR API KEY HERE" // https://developer.tomtom.com/user/register

    private val AMSTERDAM = GeoCoordinate(52.377956, 4.897070)
    // Search API
    val searchApi = OnlineSearchApi.create(this, APIKEY)

    val searchApiParameters = SearchApiParameters(
        limit = 5,
        position = AMSTERDAM
    )
    val searchProperties = SearchProperties(
        searchApiKey = APIKEY,
        searchApiParameters = searchApiParameters,
        commands = listOf("TomTom")
    )

    // Routing API
    private lateinit var routingApi: RoutingApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Add the search UI map fragment
        val searchFragment = SearchFragment.newInstance(searchProperties)
        supportFragmentManager.beginTransaction()
            .replace(R.id.search_fragment_container, searchFragment)
            .commitNow()

        // add the search API to the search fragment
        searchFragment.setSearchApi(searchApi)

        // Add the listener when someone click on a suggestion
        val searchFragmentListener = object : SearchFragmentListener {
            override fun onSearchBackButtonClick() {
                /* YOUR CODE GOES HERE */
            }

            override fun onSearchResultClick(place: Place) {
                // now we take the place, let's get the coordinates
                // and create a route!  Easy!
                createRoute(place.position)
            }

            override fun onSearchError(throwable: Throwable) {
                /* YOUR CODE GOES HERE */
            }

            override fun onSearchQueryChanged(input: String) {
                /* YOUR CODE GOES HERE */
            }

            override fun onCommandInsert(command: String) {
                /* YOUR CODE GOES HERE */
            }
        }
        searchFragment.setFragmentListener(searchFragmentListener)

        // Add a map fragment
        val mapOptions = MapOptions(mapKey = APIKEY)
        val mapFragment = MapFragment.newInstance(mapOptions)
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()

        // Location Engine
        locationEngine = AndroidLocationEngine(context = this)
        locationEngine.enable()

        // Deactivate this later  --

        // Add routing API
        routingApi = OnlineRoutingApi.create(context = this, apiKey = APIKEY)

        // Adding Navigation
        val navigationConfiguration = NavigationConfiguration(
            context = this,
            apiKey = APIKEY,
            locationEngine = locationEngine,
            routingApi = routingApi
        )
        tomtomNavigation = TomTomNavigation.create(navigationConfiguration)

        mapFragment.getMapAsync { map ->
            tomTomMap = map
            val initialOptions = CameraOptions(zoom = 16.0, position = AMSTERDAM )
            tomTomMap.moveCamera(initialOptions)
            enableUserLocation()
            setUpMapListeners()
        }

        val navigationUiOptions = NavigationUiOptions(
            keepInBackground = true
        )
        navigationFragment = NavigationFragment.newInstance(navigationUiOptions)
        supportFragmentManager.beginTransaction()
            .add(R.id.navigation_fragment_container, navigationFragment)
            .commitNow()

        navigationFragment.setTomTomNavigation(tomtomNavigation)
    }

    private fun setMapMatchedLocationEngine() {
        val mapMatchedLocationEngine = MapMatchedLocationEngine(tomtomNavigation)
        tomTomMap.setLocationEngine(mapMatchedLocationEngine)
        mapMatchedLocationEngine.enable()
    }

    private fun stopNavigation() {
        navigationFragment.stopNavigation()
        tomTomMap.changeCameraTrackingMode(CameraTrackingMode.NONE)
        tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerType.POINTER))
        //resetMapPadding()
    }

    private fun setMapNavigationPadding() {
        val paddingBottom =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                resources.getDimension(R.dimen.map_padding_bottom),
                resources.displayMetrics
            ).toInt()
        val padding = Padding(0, 0, 0, paddingBottom)
        tomTomMap.setPadding(padding)
    }

    private val navigationListener = object : NavigationFragment.NavigationListener {
        override fun onStarted() {
            tomTomMap.changeCameraTrackingMode(CameraTrackingMode.FOLLOW_ROUTE)
            tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerType.CHEVRON))
            // setSimulationLocationEngineToNavigation()
            setMapMatchedLocationEngine()
            setMapNavigationPadding()
        }

        override fun onFailed(error: NavigationError) {
            Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
            stopNavigation()
        }

        override fun onStopped() {
            stopNavigation()
        }
    }

    private fun navigate() {
        val routePlan = RoutePlan( route , planRouteOptions)
        navigationFragment.startNavigation(routePlan)
        navigationFragment.addNavigationListener(navigationListener)
    }

    private fun enableUserLocation() {

        // Getting locations to the map
        tomTomMap.setLocationEngine(locationEngine)
        val locationMarker = LocationMarkerOptions(type=LocationMarkerType.POINTER)
        tomTomMap.enableLocationMarker(locationMarker)
    }

    private val planRouteCallback = object : PlanRouteCallback {
        override fun onSuccess(result: PlanRouteResult) {
            route = result.routes.first()
            drawRoute(route)
        }

        override fun onError(error: RoutingError) {
            Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun Route.mapInstructions(): List<Instruction> {
        val routeInstructions = legs.flatMap { routeLeg -> routeLeg.instructions }
        return routeInstructions.map {
            Instruction(
                routeOffset = it.routeOffset,
                combineWithNext = it.isPossibleToCombineWithNext
            )
        }
    }

    private fun drawRoute(route: Route) {
        val instructions = route.mapInstructions()
        val geometry = route.legs.flatMap { it.points }
        val routeOptions = RouteOptions(
            geometry = geometry,
            destinationMarkerVisible = true,
            departureMarkerVisible = true,
            instructions = instructions
        )
        tomTomMap.addRoute(routeOptions)
        val ZOOM_PADDING = 20
        tomTomMap.zoomToRoutes(ZOOM_PADDING)

    }

    private fun createRoute(destination: GeoCoordinate) {
        val userLocation = tomTomMap.currentLocation?.position ?: return
        val itinerary = Itinerary(origin = userLocation, destination = destination)
        planRouteOptions = PlanRouteOptions(
            itinerary = itinerary,
            instructionType = InstructionType.TEXT,
            instructionPhonetics = InstructionPhoneticsType.IPA,
            instructionAnnouncementPoints = AnnouncementPoints.ALL,
            sectionTypes = listOf(SectionType.MOTORWAY, SectionType.LANES, SectionType.SPEED_LIMIT),
            travelMode = TravelMode.CAR
        )
        routingApi.planRoute( planRouteOptions, planRouteCallback)
    }

    private fun setUpMapListeners() {
        // We are creating the route now when we select a results.. this is not needed anymore
//        tomTomMap.addOnMapLongClickListener { coordinate: GeoCoordinate ->
//            createRoute(coordinate)
//            return@addOnMapLongClickListener true
//        }

        tomTomMap.addOnMapClickListener { coordinate: GeoCoordinate ->
            navigate()
            return@addOnMapClickListener true
        }

        // We are going to listen to the current location to move the map
        // initially, but when we are navigating this is done automatically,
        // so this listener should be deactivated.
        val locationUpdateListener = object : OnLocationUpdateListener {
            override fun onLocationUpdate(location: GeoLocation) {
                    tomTomMap.moveCamera(CameraOptions(position = location.position))
            }
        }
        locationEngine.addOnLocationUpdateListener(locationUpdateListener)
    }
}