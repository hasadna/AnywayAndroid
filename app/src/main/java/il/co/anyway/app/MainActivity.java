package il.co.anyway.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import java.util.List;

import il.co.anyway.app.DataObjects.MapMarker;
import il.co.anyway.app.DataObjects.Markers;
import il.co.anyway.app.Networking.GatDataAPI;
import il.co.anyway.app.Networking.RetrofitNetworkManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener, GoogleMap.OnCameraIdleListener {

    private DrawerLayout mDrawerLayout;
    private GoogleMap mGoogleMap;
    private List<MapMarker> mMarkerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prepareToolbar();
        FloatingActionButton floatingActionButton = findViewById(R.id.floating_find_my_location_button);
        floatingActionButton.setOnClickListener(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setOnCameraIdleListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareToolbar() {
        mDrawerLayout = findViewById(R.id.drawer_layout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floating_find_my_location_button:
                break;
        }
    }

    private void getMarkersForCurrentView() {
        VisibleRegion visibleRegion = mGoogleMap.getProjection().getVisibleRegion();
        GatDataAPI service = RetrofitNetworkManager.getClient().create(GatDataAPI.class);

        // http://www.anyway.co.il/markers?ne_lat=32.06828054640345&ne_lng=34.777443297207355&sw_lat=32.06362514879965&sw_lng=34.77358058094978&zoom=17&thin_markers=false&start_date=1451599200&end_date=1501534800&format=json&show_markers=1&show_discussions=1&show_urban=3&show_intersection=3&show_lane=3&show_day=7&show_severe=1&show_fatal=1&show_light=1&show_inaccurate=1&start_date=01%2F01%2F2016&end_date=01%2F08%2F2017
        //https://www.anyway.co.il/markers?ne_lat=32.074933503710426&ne_lng=34.76937219500542&sw_lat=32.06562343302103&sw_lng=34.7616470977664&zoom=17&thin_markers=false&start_date=1451599200&end_date=1501534800&format=json&show_markers=1&show_discussions=1
        String ne_lat = "32.074933503710426";
        String ne_lng = "34.76937219500542";
        String sw_lat = "32.06562343302103";
        String sw_lng = "34.7616470977664";
        if (visibleRegion!= null && visibleRegion.latLngBounds != null &&
                visibleRegion.latLngBounds.northeast != null &&
                visibleRegion.latLngBounds.southwest != null &&
                visibleRegion.latLngBounds.northeast.latitude - visibleRegion.latLngBounds.southwest.latitude < 0.1){
            ne_lat = String.valueOf(visibleRegion.latLngBounds.northeast.latitude);
            ne_lng = String.valueOf(visibleRegion.latLngBounds.northeast.longitude);
            sw_lat = String.valueOf(visibleRegion.latLngBounds.southwest.latitude);
            sw_lng = String.valueOf(visibleRegion.latLngBounds.southwest.longitude);
        }
        String zoom = "17";
        String thin_markers = "false";
        String start_date = "1451599200";
        String end_date = "1501534800";
        String format = "json";
        String show_markers = "1";
        String show_discussions = "1";

        Log.d(getClass().getName(), "Sending new request for markers");
        updateMarkersOnMap(false);
        Call<Markers> call = service.getAllMarkers(ne_lat, ne_lng, sw_lat, sw_lng, zoom, thin_markers, start_date, end_date, format, show_markers, show_discussions);
        call.enqueue(new Callback<Markers>() {

            @Override
            public void onResponse(Call<Markers> call, Response<Markers> response) {
                mMarkerList = response.body().getMarkers();
            }

                    @Override
            public void onFailure(Call<Markers> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Something went wrong...Please try later!", Toast.LENGTH_SHORT).show();
            }
        });
        updateMarkersOnMap(true);
    }

    private void updateMarkersOnMap(boolean addMarkers) {
        if (addMarkers && mMarkerList != null) {
            for (MapMarker marker : mMarkerList) {
                String severity = marker.getAccident_severity() == null ? "" : marker.getAccident_severity().toString();
                int height = 50;
                int width = 50;
                BitmapDrawable bitmapDraw = (BitmapDrawable)getResources().getDrawable(R.drawable.human);
                Bitmap iconBitmap = bitmapDraw.getBitmap();
                Bitmap humanMarker = Bitmap.createScaledBitmap(iconBitmap, width, height, false);
                mGoogleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(marker.getLatitude(), marker.getLongitude()))
                        .icon(BitmapDescriptorFactory.fromBitmap(humanMarker))
                        .title(severity));
            }
            Log.d(getClass().getName(), "Added " + mMarkerList.size() +" markers to map");
        } else {
            mGoogleMap.clear();
            Log.d(getClass().getName(), "Cleared map");
        }
    }

    @Override
    public void onCameraIdle() {
        getMarkersForCurrentView();
    }
}
