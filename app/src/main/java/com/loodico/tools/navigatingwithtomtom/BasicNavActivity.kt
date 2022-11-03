package com.loodico.tools.navigatingwithtomtom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.tomtom.sdk.common.location.GeoCoordinate
import com.tomtom.sdk.common.location.GeoLocation
import com.tomtom.sdk.common.route.Route
import com.tomtom.sdk.location.OnLocationUpdateListener
import com.tomtom.sdk.location.android.AndroidLocationEngine
import com.tomtom.sdk.location.mapmatched.MapMatchedLocationEngine

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
import com.tomtom.sdk.navigation.dynamicrouting.api.DynamicRoutingApi
import com.tomtom.sdk.navigation.dynamicrouting.online.OnlineDynamicRoutingApi
import com.tomtom.sdk.navigation.ui.NavigationFragment
import com.tomtom.sdk.navigation.ui.NavigationUiOptions

import com.tomtom.sdk.routing.api.*
import com.tomtom.sdk.routing.common.RoutingError
import com.tomtom.sdk.routing.common.options.Itinerary
import com.tomtom.sdk.routing.common.options.RoutePlanningOptions
import com.tomtom.sdk.routing.common.options.guidance.*
import com.tomtom.sdk.routing.common.options.vehicle.Vehicle

import com.tomtom.sdk.routing.online.OnlineRoutingApi
import com.tomtom.sdk.search.client.SearchApi
import com.tomtom.sdk.search.online.client.OnlineSearchApi
import com.tomtom.sdk.search.ui.SearchFragment
import com.tomtom.sdk.search.ui.SearchFragmentListener
import com.tomtom.sdk.search.ui.model.Place
import com.tomtom.sdk.search.ui.model.SearchApiParameters
import com.tomtom.sdk.search.ui.model.SearchProperties


class BasicNavActivity : AppCompatActivity() {
    private lateinit var navigationFragment: NavigationFragment
    private lateinit var tomtomNavigation: TomTomNavigation
    private lateinit var route: Route
    private lateinit var planRouteOptions: RoutePlanningOptions
    private lateinit var locationEngine: AndroidLocationEngine
    private lateinit var tomTomMap: TomTomMap
    private lateinit var dynamicRoutingApi: DynamicRoutingApi

    private val APIKEY= "Vn26cA8knt2E8sl0WBEWvAgWGRUf59mm" // https://developer.tomtom.com/user/register

    private val AMSTERDAM = GeoCoordinate(52.377956, 4.897070)

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

    // Search API
    private lateinit var searchApi: SearchApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchApi = OnlineSearchApi.create(this, APIKEY)

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
                searchFragment.clear()

                //Let's hide the keyboard if we have it
                if (currentFocus != null) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
                }
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

        // Lets check for FINE LOCATION permissions ...
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationEngine.enable()


        // Add routing API
        routingApi = OnlineRoutingApi.create(context = this, apiKey = APIKEY)
        dynamicRoutingApi = OnlineDynamicRoutingApi.create(routingApi)

        // Adding Navigation
        val navigationConfiguration = NavigationConfiguration(
            context = this,
            apiKey = APIKEY,
            locationEngine = locationEngine,
            //routingApi = routingApi
            dynamicRoutingApi = dynamicRoutingApi
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
            setMapMatchedLocationEngine()
            setMapNavigationPadding()
        }

        override fun onFailed(error: NavigationError) {
            Toast.makeText(this@BasicNavActivity, error.message, Toast.LENGTH_SHORT).show()
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

    private val routePlanningCallback = object : RoutePlanningCallback {
        override fun onSuccess(result: RoutePlanningResult) {
            route = result.routes.first()
            drawRoute(route)
        }

        override fun onError(error: RoutingError) {
            Toast.makeText(this@BasicNavActivity, error.message, Toast.LENGTH_SHORT).show()
        }

        override fun onRoutePlanned(route: Route) {
            this@BasicNavActivity.route = route
            drawRoute(route)
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
        planRouteOptions = RoutePlanningOptions(
            itinerary = itinerary,
            guidanceOptions = GuidanceOptions(
                instructionType = InstructionType.TEXT,
                phoneticsType = InstructionPhoneticsType.IPA,
                announcementPoints = AnnouncementPoints.ALL,
                extendedSections = ExtendedSections.ALL,
                progressPoints = ProgressPoints.ALL
            ),
            vehicle = Vehicle.Car()
        )
        routingApi.planRoute( planRouteOptions, routePlanningCallback)
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