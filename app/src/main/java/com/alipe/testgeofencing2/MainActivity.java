package com.alipe.testgeofencing2;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;


@RuntimePermissions
public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        ResultCallback<Status>{

    //private static final String TAG = MainActivity.class.getSimpleName();

    private GoogleApiClient googleApiClient;

    private Location lastLocation;

    private Marker geoFenceMarker;

    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;

    private Marker locationMarker;

    private Circle geoFenceLimits;

    private LocationRequest locationRequest;
    // Defined in mili seconds.
    // This number in extremely low, and should be used only for debug
    private final int UPDATE_INTERVAL = 3 * 60 * 1000; // 3 minutes
    private final int FASTEST_INTERVAL = 30 * 1000;  // 30 secs

    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 500.0f; // in meters

    private MapFragment mapFragment;
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeGoogleMaps();
        createGoogleApiClient();
        startGeofence();
    }

    private void createGoogleApiClient() {
        //Toast.makeText(this, "createGoogleApi()", Toast.LENGTH_SHORT).show();
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void initializeGoogleMaps() {
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Callback called when Map is touched
    @Override
    public void onMapClick(LatLng latLng) {
        //Toast.makeText(this, "onMapClick(" + latLng + ")", Toast.LENGTH_SHORT).show();

        Toast.makeText(this, "markerForGeofence(" + latLng + ")", Toast.LENGTH_SHORT).show();
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title);
        if (map != null) {
            // Remove last geoFenceMarker
            if (geoFenceMarker != null) {
                geoFenceMarker.remove();
            }
            geoFenceMarker = map.addMarker(markerOptions);
        }
    }


    // Callback called when Marker is touched
    @Override
    public boolean onMarkerClick(Marker marker) {
        //Toast.makeText(this, "onMarkerClickListener: " + marker.getPosition(), Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Toast.makeText(this, "onMapReady()", Toast.LENGTH_SHORT).show();
        map = googleMap;
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Call GoogleApiClient connection when starting the Activity
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect GoogleApiClient when stopping Activity
        googleApiClient.disconnect();
    }


    // GoogleApiClient.ConnectionCallbacks connected
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "onConnected()", Toast.LENGTH_SHORT).show();
        MainActivityPermissionsDispatcher.getLastKnownLocationWithCheck(this);
    }


    @NeedsPermission({android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION})
    protected void getLastKnownLocation() {
        Toast.makeText(this, "getLastKnownLocation", Toast.LENGTH_SHORT).show();


//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // Consider calling ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }


        boolean permissionGranted = (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        Toast.makeText(this, "getLastKnownLocation:" + (permissionGranted ? " permission granted" : " NOT ALLOWED"), Toast.LENGTH_SHORT).show();


        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (lastLocation != null) {
            Toast.makeText(this, "LasKnown location. " +
                    "Long: " + lastLocation.getLongitude() +
                    " | Lat: " + lastLocation.getLatitude(), Toast.LENGTH_SHORT).show();
            writeLastLocation();
            MainActivityPermissionsDispatcher.startLocationUpdatesWithCheck(this);
        } else {
            Toast.makeText(this, "No location retrieved yet", Toast.LENGTH_SHORT).show();
            MainActivityPermissionsDispatcher.startLocationUpdatesWithCheck(this);
        }
    }


    @NeedsPermission({android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION})
    protected void startLocationUpdates() {
        Toast.makeText(this, "startLocationUpdates", Toast.LENGTH_SHORT).show();
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // Consider calling ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }

        boolean permissionGranted = (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        Toast.makeText(this, "startLocationUpdates:" + (permissionGranted ? " permission granted" : " NOT ALLOWED"), Toast.LENGTH_SHORT).show();


        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private void writeLastLocation() {
        writeActualLocation(lastLocation);
    }

    private void writeActualLocation(Location location) {
        Toast.makeText(this, "Actual Lat: " + location.getLatitude() + ", long: " + location.getLongitude(), Toast.LENGTH_SHORT)
                .show();
        markerLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private void markerLocation(LatLng latLng) {
        Toast.makeText(this, "markerLocation("+latLng+")", Toast.LENGTH_SHORT).show();
        String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if ( map!=null ) {
            // Remove the anterior marker
            if ( locationMarker != null )
                locationMarker.remove();
            locationMarker = map.addMarker(markerOptions);
            float zoom = 14f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            map.animateCamera(cameraUpdate);
        }
    }


    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Toast.makeText(this, "createGeofenceRequest", Toast.LENGTH_SHORT).show();
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }


    private PendingIntent createGeofencePendingIntent() {
        Toast.makeText(this, "createGeofencePendingIntent", Toast.LENGTH_SHORT).show();
        if (geoFencePendingIntent != null)
            return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceService.class);
        return PendingIntent.getService(this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


//    protected static Intent makeNotificationIntent(final Context context, String message) {
//        Intent contentIntent = new Intent(context, MainActivity.class);
//        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        contentIntent.putExtra("message", message);
//        //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//        return contentIntent;
//    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }


    @NeedsPermission({android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION})
    // Add the created GeofenceRequest to the device's monitoring list
    protected void addGeofence(GeofencingRequest request) {
        Toast.makeText(this, "addGeofence", Toast.LENGTH_SHORT).show();
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // Consider calling ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }

        boolean permissionGranted = (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        Toast.makeText(this, "addGeofence:" + (permissionGranted ? " permission granted" : " NOT ALLOWED"), Toast.LENGTH_SHORT).show();

        LocationServices.GeofencingApi.addGeofences(
                googleApiClient,
                request,
                createGeofencePendingIntent()
        ).setResultCallback(this);
    }


    /** Note that this Activity implements ResultCallback<Status>, requiring that
     * onResult must be defined. onResult runs when the result of calling
     * GeofencingApi#addGeofences(GoogleApiClient, GeofencingRequest, PendingIntent)}  addGeofences()} or
     * com.google.android.gms.location.GeofencingApi#removeGeofences(GoogleApiClient, java.util.List)  removeGeofences()}
     * becomes available.
     */
    @Override
    public void onResult(@NonNull Status status) {
        Toast.makeText(this, "on result: " + status, Toast.LENGTH_SHORT).show();
        if (status.isSuccess()) {
            drawGeofence();
        } else {
            Toast.makeText(this, "Error drawing geofence" + status, Toast.LENGTH_SHORT).show();
        }
    }

    // Draw Geofence circle on GoogleMap
    private void drawGeofence() {
        Toast.makeText(this, "draw geofence", Toast.LENGTH_SHORT).show();

        if (geoFenceLimits != null)
            geoFenceLimits.remove();

        CircleOptions circleOptions = new CircleOptions()
                .center(geoFenceMarker.getPosition())
                .strokeColor(Color.argb(50, 70, 70, 70))
                .fillColor(Color.argb(100, 150, 150, 150))
                .radius(GEOFENCE_RADIUS);
        geoFenceLimits = map.addCircle(circleOptions);
    }


    // Start Geofence creation process
    private void startGeofence() {
        Toast.makeText(this, "startGeofence()", Toast.LENGTH_SHORT).show();
        if (geoFenceMarker != null) {
            Geofence geofence = createGeofence(geoFenceMarker.getPosition(), GEOFENCE_RADIUS);
            GeofencingRequest geofenceRequest = createGeofenceRequest(geofence);
            MainActivityPermissionsDispatcher.addGeofenceWithCheck(this, geofenceRequest);
        } else {
            Toast.makeText(this, "Geofence marker is null", Toast.LENGTH_SHORT).show();
        }
    }


    private Geofence createGeofence(LatLng latLng, float radius) {
        Toast.makeText(this, "createGeofence", Toast.LENGTH_SHORT).show();
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, "Location changed [" + location + "]", Toast.LENGTH_SHORT).show();
        lastLocation = location;
        writeActualLocation(location);
    }


    // GoogleApiClient.ConnectionCallbacks suspended
    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "onConnectionSuspended()", Toast.LENGTH_SHORT).show();
    }

    // GoogleApiClient.OnConnectionFailedListener fail
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
    }
}
