package com.example.rodapp.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.rodapp.BuildConfig
import com.example.rodapp.R
import com.example.rodapp.databinding.FragmentMapaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.Place.Field
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.material.snackbar.Snackbar

class MapaFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapaBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private var currentLocation: LatLng = LatLng(4.6097, -74.0817)
    private val activeMarkers = mutableListOf<Marker>()
    private var selectedPlace: Place? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            enableMapLocation()
            fetchCurrentLocation()
        } else {
            Snackbar.make(binding.root, R.string.label_permiso_ubicacion, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext().applicationContext, BuildConfig.MAPS_API_KEY)
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        placesClient = Places.createClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMap()
        setupSearchBar()
        binding.fabLocation.setOnClickListener { onFabLocationClick() }
    }

    private fun initMap() {
        val mapFragment = childFragmentManager.findFragmentByTag(TAG_MAP) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction()
                    .replace(R.id.map_container, it, TAG_MAP)
                    .commit()
            }
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        applyDarkStyle(map)
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isMyLocationButtonEnabled = false
            isMapToolbarEnabled = false
        }
        map.setOnMarkerClickListener { marker ->
            (marker.tag as? Place)?.let { showPlaceDetail(it) }
            true
        }
        checkLocationPermissions()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14f))
    }

    private fun applyDarkStyle(map: GoogleMap) {
        try {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark))
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Map style file not found", e)
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text?.toString()?.trim()
                if (!query.isNullOrEmpty()) {
                    hideKeyboard()
                    searchPlaces(query)
                }
                true
            } else false
        }
    }

    private fun checkLocationPermissions() {
        if (hasLocationPermission()) {
            enableMapLocation()
            fetchCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun enableMapLocation() {
        googleMap?.isMyLocationEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = LatLng(location.latitude, location.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            }
        }
    }

    private fun onFabLocationClick() {
        if (hasLocationPermission()) {
            fetchCurrentLocation()
        } else {
            checkLocationPermissions()
        }
    }

    private fun searchPlaces(query: String) {
        val bias = CircularBounds.newInstance(currentLocation, SEARCH_RADIUS_METERS)
        val request = SearchByTextRequest.builder(query, PLACE_FIELDS)
            .setMaxResultCount(MAX_RESULTS)
            .setLocationBias(bias)
            .build()

        placesClient.searchByText(request)
            .addOnSuccessListener { response ->
                clearMarkers()
                val places = response.places
                if (places.isEmpty()) {
                    Snackbar.make(binding.root, R.string.error_busqueda_mapa, Snackbar.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val boundsBuilder = LatLngBounds.Builder()
                places.forEach { place ->
                    val location = place.location ?: return@forEach
                    val latLng = LatLng(location.latitude, location.longitude)
                    val marker = googleMap?.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(place.displayName)
                    )
                    marker?.tag = place
                    if (marker != null) {
                        activeMarkers.add(marker)
                        boundsBuilder.include(latLng)
                    }
                }
                if (activeMarkers.isNotEmpty()) {
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), BOUNDS_PADDING)
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Places search failed", e)
                Snackbar.make(binding.root, R.string.error_places_api, Snackbar.LENGTH_LONG).show()
            }
    }

    private fun showPlaceDetail(place: Place) {
        selectedPlace = place
        binding.cardDetalle.visibility = View.VISIBLE
        binding.tvTitle.text = place.displayName ?: getString(R.string.label_taller_ejemplo)
        binding.tvAddress.text = place.formattedAddress ?: getString(R.string.label_ubicacion_ejemplo)
        binding.tvRating.text = place.rating?.let { "${"%.1f".format(it)} ★" } ?: ""
        val openNow = place.currentOpeningHours?.periods?.any { period ->
            period.open != null && period.close == null
        }
        binding.tvState.text = when (openNow) {
            true -> getString(R.string.label_abierto)
            false -> getString(R.string.label_cerrado)
            else -> getString(R.string.label_sin_horario)
        }
        binding.btnComoLlegar.setOnClickListener {
            val loc = place.location ?: return@setOnClickListener
            navigateTo(loc.latitude, loc.longitude, place.displayName ?: "")
        }
    }

    private fun navigateTo(lat: Double, lng: Double, name: String) {
        val hasMaps = isAppInstalled(PKG_MAPS)
        val hasWaze = isAppInstalled(PKG_WAZE)
        when {
            hasMaps && hasWaze -> showNavigationChooser(lat, lng, name)
            hasMaps -> openGoogleMaps(lat, lng)
            hasWaze -> openWaze(lat, lng)
            else -> openBrowserFallback(lat, lng)
        }
    }

    private fun showNavigationChooser(lat: Double, lng: Double, name: String) {
        val options = arrayOf(getString(R.string.label_google_maps), getString(R.string.label_waze))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.label_abrir_con)
            .setItems(options) { _, which ->
                if (which == 0) openGoogleMaps(lat, lng) else openWaze(lat, lng)
            }
            .show()
    }

    private fun openGoogleMaps(lat: Double, lng: Double) {
        val uri = Uri.parse("google.navigation:q=$lat,$lng")
        startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(PKG_MAPS))
    }

    private fun openWaze(lat: Double, lng: Double) {
        val uri = Uri.parse("waze://?ll=$lat,$lng&navigate=yes")
        startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(PKG_WAZE))
    }

    private fun openBrowserFallback(lat: Double, lng: Double) {
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            requireContext().packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun clearMarkers() {
        activeMarkers.forEach { it.remove() }
        activeMarkers.clear()
        selectedPlace = null
        binding.cardDetalle.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "MapaFragment"
        private const val TAG_MAP = "map_fragment"
        private const val SEARCH_RADIUS_METERS = 5000.0
        private const val MAX_RESULTS = 10
        private const val BOUNDS_PADDING = 120
        private const val PKG_MAPS = "com.google.android.apps.maps"
        private const val PKG_WAZE = "com.waze"

        private val PLACE_FIELDS = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.RATING,
            Place.Field.CURRENT_OPENING_HOURS
        )
    }
}
