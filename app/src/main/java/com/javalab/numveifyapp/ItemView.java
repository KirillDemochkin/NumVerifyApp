package com.javalab.numveifyapp;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;

public class ItemView extends FragmentActivity implements OnMapReadyCallback {

    TextView phone;
    TextView location;
    TextView time;
    GeoNumber geoNumber;
    MapFragment mapFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_view);
        Intent i = getIntent();
        geoNumber = i.getParcelableExtra("number");
        phone = (TextView) findViewById(R.id.singleItem_phoneField);
        location = (TextView) findViewById(R.id.singleItem_locationField);
        time = (TextView) findViewById(R.id.singleItem_timeField);
        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        phone.setText(geoNumber.getNumber());
        location.setText(geoNumber.getAddress());
        ZonedDateTime zdt = NumberGeoLocator.currentTimeInTimeZone(geoNumber.getTimeZoneName());
        time.setText(zdt.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));
    }

    public void onDoneButtonClicked(View view){
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng coords = new LatLng(geoNumber.getLat(), geoNumber.getLng());
        googleMap.addMarker(new MarkerOptions()
                .position(coords)
                .title(geoNumber.getNumber()));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coords, 10));
    }
}
