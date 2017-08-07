package com.javalab.numveifyapp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jakewharton.threetenabp.AndroidThreeTen;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;
import org.w3c.dom.Text;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    TextView timeField;
    TextView locationField;
    EditText phoneTextField;
    MapFragment mapFragment;
    GeoNumber curNumber;
    GoogleMap googleMap;
    ProgressBar progressBar;
    LinearLayout bg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        timeField = (TextView) findViewById(R.id.timeField);
        locationField = (TextView) findViewById(R.id.locationField);
        phoneTextField = (EditText) findViewById(R.id.phoneTextField);
        progressBar = (ProgressBar)findViewById(R.id.newEntry_ProgressBar);
        bg = (LinearLayout) findViewById(R.id.newEntry_greyBG);
        phoneTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });
        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public void onDoneButtonClicked(View view){
        saveAndExit();
    }

    public void onCheckButtonClicked(View view){
        hideKeyboard(view);
        //NumberGeoLocator.testJSON("http://apilayer.net/api/validate?access_key=ce673d81b13bf15699b5d1b56c700a47&number=4158586273&country_code=US&format=1");
        if(!phoneTextField.getText().toString().isEmpty()){
            try {

                /*curNumber = NumberGeoLocator.getLocalTimeOf(phoneTextField.getText().toString());
                ZonedDateTime zdt = NumberGeoLocator.currentTimeInTimeZone(curNumber.timeZoneName);
                Log.d(":)", zdt.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));
                timeField.setText(zdt.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));
                locationField.setText(curNumber.address);
                hideKeyboard(phoneTextField);
                googleMap.clear();
                LatLng coords = new LatLng(curNumber.lat, curNumber.lng);
                googleMap.addMarker(new MarkerOptions()
                        .position(coords)
                        .title(curNumber.number));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coords, 10));*/
                new getNumberDataAsync().execute(phoneTextField.getText().toString());

            } catch (RuntimeException e){
                Log.e("bad exception", e.getMessage());
            }

        }
    }



    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed(){

        saveAndExit();
    }

    public void saveAndExit(){
        if(curNumber != null) {
            Intent intent = new Intent();
            intent.putExtra("newEntry", (Parcelable) curNumber);
            setResult(RESULT_OK, intent);
        }
        finish();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
    }

    private class getNumberDataAsync extends AsyncTask<String, String, String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            bg.setVisibility(View.VISIBLE);
            bg.bringToFront();
            progressBar.bringToFront();

        }

        @Override
        protected String doInBackground(String... params) {

            try {

                curNumber = NumberGeoLocator.getLocalTimeOf(params[0]);

                //Log.d(":)", zdt.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));

                hideKeyboard(phoneTextField);


            } catch (RuntimeException e){
                Log.e("bad exception", e.getMessage());
            }
            return "";

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            ZonedDateTime zdt = NumberGeoLocator.currentTimeInTimeZone(curNumber.getTimeZoneName());
            timeField.setText(zdt.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));
            locationField.setText(curNumber.getAddress());
            googleMap.clear();
            LatLng coords = new LatLng(curNumber.getLat(), curNumber.getLng());
            googleMap.addMarker(new MarkerOptions()
                    .position(coords)
                    .title(curNumber.getNumber()));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coords, 10));
            progressBar.setVisibility(View.GONE);
            bg.setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }
}
