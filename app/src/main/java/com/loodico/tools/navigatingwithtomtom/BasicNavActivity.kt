package com.loodico.tools.navigatingwithtomtom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.tomtom.sdk.maps.display.image.ImageFactory
import com.tomtom.sdk.maps.display.location.LocationMarkerOptions
import com.tomtom.sdk.maps.display.location.LocationMarkerType
import com.tomtom.sdk.maps.display.marker.Marker
import com.tomtom.sdk.maps.display.marker.MarkerOptions
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


class BasicNavActivity : AppCompatActivity() , RouteProcessFragment.NavigateOptionsInterface{
    // this is the frame layout container of the map and navigation fragments
    private lateinit var navGroupContainer: FrameLayout

    // Only marker in the map : the selected destination
    private var searchMarker: Marker? = null

    // The view that contain the "Navigate" button when we select a destination
    private lateinit var routingFragment: RouteProcessFragment

    // The NavSDK view
    private lateinit var navigationFragment: NavigationFragment

    // the navigation
    private lateinit var tomtomNavigation: TomTomNavigation

    // The route object created when we select a destination
    private lateinit var route: Route

    // Route options as type of vehicle, size, etc
    private lateinit var planRouteOptions: RoutePlanningOptions

    // Who is providing the GPS location?
    private lateinit var locationEngine: AndroidLocationEngine

    // The Map view object
    private lateinit var tomTomMap: TomTomMap

    // Search API
    private lateinit var searchApi: SearchApi

    // Routing API
    private lateinit var routingApi: RoutingApi

    // Dynamic routing engine for creating routes.
    private lateinit var dynamicRoutingApi: DynamicRoutingApi

    // API Key for map and apis
    private val APIKEY= BuildConfig.TomTomApiKey // https://developer.tomtom.com/user/register

    // Default map center
    private val AMSTERDAM = GeoCoordinate(52.377956, 4.897070)

    // SearchFragment Configuration
    val searchApiParameters = SearchApiParameters(
        limit = 5,
        position = AMSTERDAM
    )

    val searchProperties = SearchProperties(
        searchApiKey = APIKEY,
        searchApiParameters = searchApiParameters,
        commands = listOf("TomTom")
    )

    val searchFragment = SearchFragment.newInstance(searchProperties)

    private fun addRoutingOptionsFragment(place: Place) {
        routingFragment = RouteProcessFragment.newInstance(place, this)
        supportFragmentManager.beginTransaction()
            .replace(R.id.route_fragment_container, routingFragment)
            .commitNow()
    }

    // Call this to add the search functionality to the activity
    private fun addSearchFragment() {
        // Add the search UI map fragment

        supportFragmentManager.beginTransaction()
            .replace(R.id.search_fragment_container, searchFragment)
            .commitNow()

        // add the search API to the search fragment
        searchFragment.setSearchApi(searchApi)

        // Add the listener when someone click on a suggestion
        val searchFragmentListener = object : SearchFragmentListener {
            override fun onSearchBackButtonClick() {
                removeMarker()
                searchFragment.clear()
            }

            override fun onSearchResultClick(place: Place) {
                // make nav and map visible again
                navGroupContainer.setVisibility(View.VISIBLE)
                // now we take the place, let's get the coordinates

                try {
                    tomTomMap.moveCamera(CameraOptions(position = place.position))
                } catch (exception: Exception) {
                    // do nothing because the map could be already
                    // invalidaded.
                }
                
                setMarker(place.position)
                searchFragment.clear()
                removeSearchFragment()

                //Let's hide the keyboard if we have it
                if (currentFocus != null) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
                }

                // add the Routing process fragment
                addRoutingOptionsFragment(place)
            }

            override fun onSearchError(throwable: Throwable) {
                /* YOUR CODE GOES HERE */
            }

            override fun onSearchQueryChanged(input: String) {
                // hide and show the map...
                if (input.length > 0) {
                    navGroupContainer.setVisibility(View.GONE)
                } else {
                    navGroupContainer.setVisibility(View.VISIBLE)
                }
            }

            override fun onCommandInsert(command: String) {
                /* YOUR CODE GOES HERE */
            }
        }
        searchFragment.setFragmentListener(searchFragmentListener)

    }

    private fun removeMarker() {
        searchMarker?.remove()
        searchMarker = null

    }
    // set a marker to the coordinates. It gets replaced everytime
    private fun setMarker(position: GeoCoordinate) {
        searchMarker?.remove()
        val markerOptions = MarkerOptions(
            coordinate = position,
            pinImage = ImageFactory.fromResource(com.tomtom.sdk.search.ui.R.drawable.ic_pin)
        )
        searchMarker = this.tomTomMap.addMarker(markerOptions)
    }

    // Remove the search fragment
    private fun removeSearchFragment() {
        supportFragmentManager.beginTransaction()
            .remove(searchFragment)
            .commitNow()
    }

    // Remove the routing options
    private fun removeRoutingOptionsFragment() {
        supportFragmentManager.beginTransaction()
            .remove(routingFragment)
            .commitNow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchApi = OnlineSearchApi.create(this, APIKEY)

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
            dynamicRoutingApi = dynamicRoutingApi
        )
        tomtomNavigation = TomTomNavigation.create(navigationConfiguration)

        mapFragment.getMapAsync { map ->
            tomTomMap = map
            val initialOptions = CameraOptions(zoom = 16.0, position = AMSTERDAM )
            tomTomMap.moveCamera(initialOptions)
            enableUserLocation()
            setUpMapListeners()
            addSearchFragment()
        }

        val navigationUiOptions = NavigationUiOptions(
            keepInBackground = true
        )
        navigationFragment = NavigationFragment.newInstance(navigationUiOptions)
        supportFragmentManager.beginTransaction()
            .add(R.id.navigation_fragment_container, navigationFragment)
            .commitNow()

        navigationFragment.setTomTomNavigation(tomtomNavigation)

        navGroupContainer = findViewById<FrameLayout>(R.id.nav_group_container)
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
        tomTomMap.removeRoutes()
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
            showBottomOptionsContainer()

        }

        override fun onStopped() {
            stopNavigation()
            showBottomOptionsContainer()
            addSearchFragment()
        }
    }

    private fun navigate() {
        if ( this::route.isInitialized ) { // start the navgation with a set route
            hideBottomOptionsContainer() // we want full screen for navigation
            removeRoutingOptionsFragment()
            try {
                val routePlan = RoutePlan(route, planRouteOptions)
                navigationFragment.startNavigation(routePlan)
                navigationFragment.addNavigationListener(navigationListener)
            } catch (exception: IllegalArgumentException) {
                Toast.makeText(this@BasicNavActivity, "Error. Maybe the navigation already started?", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideBottomOptionsContainer() {
        val container = findViewById<FrameLayout>(R.id.bottom_group_container)
        container.setVisibility(View.GONE)
    }

    private fun showBottomOptionsContainer() {
        val container = findViewById<FrameLayout>(R.id.bottom_group_container)
        container.setVisibility(View.VISIBLE)
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
            navigate()
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

    // We are going to listen to the current location to move the map
    // initially, but when we are navigating this is done automatically,
    // so this listener should be deactivated.
    private val locationUpdateListener = object : OnLocationUpdateListener {
        override fun onLocationUpdate(location: GeoLocation) {
            try {
                tomTomMap.moveCamera(CameraOptions(position = location.position))
            } catch (exception: Exception) {
                // do nothing because the map could be already
                // invalidaded.
            }
        }
    }

    private fun setUpMapListeners() {

        tomTomMap.addOnMapClickListener {
            navigate()
            return@addOnMapClickListener true
        }

        // locationEngine.addOnLocationUpdateListener(locationUpdateListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationEngine.removeOnLocationUpdateListener(locationUpdateListener)
    }

    override fun onNavigate(destination: GeoCoordinate) {
        createRoute(destination)
    }

    override fun onCancel() {
        removeRoutingOptionsFragment()
        addSearchFragment()
    }
}