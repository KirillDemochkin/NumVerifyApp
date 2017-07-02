package com.javalab.numveifyapp;

import android.os.AsyncTask;
import android.util.Log;


import com.google.android.gms.maps.model.LatLng;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

public class NumberGeoLocator {

 static final String googleMapsTimeZonesApiKey = "AIzaSyACOKu6jYJ5SdXYK1VlQsgc22simLsXDvo";
 static final String googleMapsGeocodeApiKey = "AIzaSyAv92rufvA6ieiUWjHdjMY4d5lGzuFXIaI";
 static final String apiNumVerifyKey = "ce673d81b13bf15699b5d1b56c700a47";
 static final String googleMapsAndroidApiKey = "AIzaSyBfp060_lTrIYkKQ5fTWUq9UUlu6_U_9bk";

 static final String baseURLTimeZoneApi = "https://maps.googleapis.com/maps/api/timezone/json?location=";
 static final String baseURLGeocodeApi = "https://maps.googleapis.com/maps/api/geocode/json?address=";
 static final String baseURLNumVerifyApi = "http://apilayer.net/api/validate?access_key=";

 static GeoNumber getLocalTimeOf(String phoneNumber) {
  try {
   GeoNumber geoNumber = new GeoNumber();
   geoNumber.setNumber(phoneNumber);
   JSONObject numVerifyJSON = getJSON(baseURLNumVerifyApi + apiNumVerifyKey + "&number=" + phoneNumber);
   if ((boolean) numVerifyJSON.get("valid") == false) {
    throw new RuntimeException("invalid phone number");
   }
   Log.d(":)", numVerifyJSON.toString());
   String location = numVerifyJSON.get("location").toString();
   String country = numVerifyJSON.get("country_name").toString();
   if (!location.isEmpty()) {
    geoNumber.setAddress(location + ", " + country);
   } else {
    geoNumber.setAddress(country);
   }
   String address = buildAddress(location, country);

   Log.d(":)", address);
   JSONObject coordsJSON = parseCoords(getJSON(baseURLGeocodeApi + address + "&key=" + googleMapsGeocodeApiKey));

   Log.d(":)", coordsJSON.toString());
   String timestamp = String.valueOf(Instant.now().getEpochSecond());

   String coordsString = coordsJSON.get("lat") + "," + coordsJSON.get("lng");
   geoNumber.setLat(((Number) coordsJSON.get("lat")).doubleValue());
   geoNumber.setLng(((Number) coordsJSON.get("lng")).doubleValue());
   JSONObject timezoneJSON =  getJSON(baseURLTimeZoneApi + coordsString + "&timestamp=" + timestamp + "&key=" + googleMapsTimeZonesApiKey);
   Log.d(":)", timezoneJSON.get("timeZoneId").toString());
   geoNumber.setTimeZoneName(timezoneJSON.get("timeZoneId").toString());
   ZonedDateTime zdt = currentTimeInTimeZone(timezoneJSON.get("timeZoneId").toString());
   //geoNumber.zdt = zdt;
   return geoNumber;
   //Log.d(":)", json.toString());
  } catch (Exception e) {
   Log.e(":(", e.getMessage());
  }
  Log.d("test failed", " :(");
  return null;
 }

 public static void displayMap(JSONObject coords, String address) {

 }

 public static ZonedDateTime currentTimeInTimeZone(String zoneId) {
  return ZonedDateTime.now(ZoneId.of(zoneId));
 }

 private static String buildAddress(String location, String country) {
  StringBuilder address = new StringBuilder();
  String locationString = location;
  String countryString = country;
  if (locationString.contains(" ")) {
   StringTokenizer tokenizer = new StringTokenizer(locationString, " ");
   int i = 0;
   while (i < tokenizer.countTokens() - 1) {
    address.append(tokenizer.nextToken());
    address.append("+");
    ++i;
   }
   address.append(tokenizer.nextToken());
  } else {
   address.append(locationString);
  }
  address.append(",");

  if (countryString.contains(" ")) {
   StringTokenizer tokenizer = new StringTokenizer(countryString, " ");
   while (tokenizer.hasMoreTokens()) {
    address.append("+");
    address.append(tokenizer.nextToken());
   }
  } else {
   address.append("+");
   address.append(countryString);
  }

  return address.toString();
 }

 private static final JSONObject getJSON(String number) {
  HttpURLConnection urlConnection;
  StringBuilder result = new StringBuilder();
  JSONObject json;
  URL url;
  try {
   url = new URL(number);
   urlConnection = (HttpURLConnection) url.openConnection();
  } catch (MalformedURLException e) {
   Log.e("bad url", e.getMessage());
   return null;
  } catch (IOException e) {
   Log.e("url can't open", e.getMessage());
   return null;
  }

  try (
          InputStream in = new BufferedInputStream(urlConnection.getInputStream());
          BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

   JSONParser jsonParser = new JSONParser();
   json = (JSONObject) jsonParser.parse(reader);
   return json;

  } catch (ParseException e) {
   Log.e("can't parse json", e.getMessage());
  } catch (IOException e) {
   Log.e("can't read", e.getMessage());
  }

  return null;

 }

 private static final JSONObject parseCoords(JSONObject json) {
  final JSONArray results = (JSONArray) json.get("results");
  final JSONObject data = (JSONObject) results.get(0);
  final JSONObject geometry = (JSONObject) data.get("geometry");
  final JSONObject coords = (JSONObject) geometry.get("location");
  return coords;
 }
}



