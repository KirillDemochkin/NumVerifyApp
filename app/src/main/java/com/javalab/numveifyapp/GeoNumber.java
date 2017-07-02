package com.javalab.numveifyapp;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.io.Serializable;

public class GeoNumber implements Parcelable, Serializable{
     String address;
     double lat;
     double lng;
     String timeZoneName;
     String number;


    public GeoNumber(){

    };

    @Override
    public String toString(){
        return number + " (" + address + ")";
    }



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(number);
        dest.writeString(address);
        dest.writeDouble(lat);
        dest.writeDouble(lng);
        dest.writeString(timeZoneName);

    }

    public static final Parcelable.Creator<GeoNumber> CREATOR
            = new Parcelable.Creator<GeoNumber>() {
        public GeoNumber createFromParcel(Parcel in) {
            return new GeoNumber(in);
        }

        public GeoNumber[] newArray(int size) {
            return new GeoNumber[size];
        }
    };

    private GeoNumber(Parcel in) {
        number = in.readString();
        address = in.readString();
        lat = in.readDouble();
        lng = in.readDouble();
        //coords = new LatLng(lat, lng);
        timeZoneName = in.readString();
        //zdt = ZonedDateTime.now(ZoneId.of(timeZoneName));
    }

    public String getAddress() {
        return address;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getTimeZoneName() {
        return timeZoneName;
    }

    public String getNumber() {
        return number;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public void setTimeZoneName(String timeZoneName) {
        this.timeZoneName = timeZoneName;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
