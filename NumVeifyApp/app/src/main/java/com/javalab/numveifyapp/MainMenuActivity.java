package com.javalab.numveifyapp;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

public class MainMenuActivity extends Activity {
    final String saveFile = "test6.ser";
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PERMISSIONS_REQUEST_CAMERA = 200;
    private static final int NEW_ACTIVITY_RESULT = 1;
    private static final int SCAN_ACTIVITY_RESULT = 2;
    List<GeoNumber> numbers;
    ListView listView;
    ArrayAdapter<GeoNumber> adapter;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)){
            requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
        } else {
            AndroidThreeTen.init(this);
            setContentView(R.layout.activity_list);
            numbers = new ArrayList<>();
            listView = (ListView) findViewById(R.id.list);
            progressBar = (ProgressBar) findViewById(R.id.progressBar);

            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, numbers);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    //String num = ((TextView) view).getText().toString();

                    Intent i = new Intent(getApplicationContext(), ItemView.class);
                    i.putExtra("number", (Parcelable) numbers.get(position));
                    startActivity(i);

                }
            });

            listView.setLongClickable(true);
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView parent, View view,
                                               int pos, long id) {

                    numbers.remove(pos);
                    adapter.notifyDataSetChanged();
                    return true;
                }
            });

            new loadDataAsync().execute("");

        }
    }

    public void loadFile(){
        if(fileExists(this, saveFile)) {
            File file = new File(getFilesDir(), saveFile);
            try (FileInputStream fileIn = new FileInputStream(file);
                 ObjectInputStream in = new ObjectInputStream(fileIn)) {

                int numOfEntries = in.readInt();
                for (int i = 0; i < numOfEntries; ++i) {
                    GeoNumber gn = (GeoNumber) in.readObject();
                    numbers.add(gn);

                }


            } catch (IOException e) {
                Log.e("load exception", e.getMessage());
                return;
            } catch (ClassNotFoundException e) {
                Log.e("load exception", e.getMessage());
                return;
            }
        } else{
            getNumber(this.getContentResolver());
        }
    }

    public void getNumber(ContentResolver cr){


            Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

            while (phones.moveToNext()) {
                String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                //cleanPhoneNumber.replaceAll("[0-9]+", "");
                phoneNumber = phoneNumber.replaceAll("[()\\s\\-]+", "");

                Log.i("!", "working on |" + phoneNumber);
                if (phoneNumber.startsWith("+")) {
                    try {
                        GeoNumber gn = NumberGeoLocator.getLocalTimeOf(phoneNumber);
                        if (gn != null) {
                            numbers.add(gn);
                            Log.i("!", "added" + phoneNumber);
                        }
                    } catch (RuntimeException e) {
                        Log.e("Invalid Number", e.getMessage());
                    }

                }
            }

        }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantedResults){
        if(requestCode == PERMISSIONS_REQUEST_READ_CONTACTS){
            if(grantedResults[0] == PackageManager.PERMISSION_GRANTED){
                getNumber(this.getContentResolver());
            } else {
                Toast.makeText(this, "Come back when you are ready", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == PERMISSIONS_REQUEST_CAMERA){
            if(grantedResults[0] == PackageManager.PERMISSION_GRANTED){

            } else {
                Toast.makeText(this, "Come back when you are ready", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onScanButtonClicked(View view){


        Intent i = new Intent(getApplicationContext(), scanNumberActivity.class);
        startActivityForResult(i, SCAN_ACTIVITY_RESULT);
    }

    public void onAddNewButtonClicked(View view){
        Intent i = new Intent(getApplicationContext(), MainActivity.class);

        startActivityForResult(i, NEW_ACTIVITY_RESULT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == NEW_ACTIVITY_RESULT){
            if(resultCode == RESULT_OK){
                GeoNumber receivedNumber = data.getParcelableExtra("newEntry");
                numbers.add(receivedNumber);
                adapter.notifyDataSetChanged();
            }
        }

        if(requestCode == SCAN_ACTIVITY_RESULT){
            if(resultCode == RESULT_OK){
                GeoNumber receivedNumber = data.getParcelableExtra("newEntry");
                numbers.add(receivedNumber);
                adapter.notifyDataSetChanged();
                Intent i = new Intent(getApplicationContext(), ItemView.class);
                i.putExtra("number", (Parcelable) receivedNumber);
                startActivity(i);
            }
        }
    }

    public void onSaveButtonPressed(View view){
        if(!fileExists(this, saveFile)) {
            File file = new File(getFilesDir(), saveFile);
            try (FileOutputStream fileOut =
                         new FileOutputStream(file);
                 ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
                out.writeInt(numbers.size());
                for (GeoNumber number : numbers) {
                    out.writeObject(number);
                }
            } catch (IOException e) {
                Log.e("save exception", e.getMessage());
            }
        } else {
            try (FileOutputStream fileOut =
                         openFileOutput(saveFile, Context.MODE_PRIVATE);
                 ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
                out.writeInt(numbers.size());
                for (GeoNumber number : numbers) {
                    out.writeObject(number);
                }
            } catch (IOException e) {
                Log.e("save exception", e.getMessage());
            }
        }
    }

    public boolean fileExists(Context context, String filename) {
        File file = context.getFileStreamPath(filename);
        if(file == null || !file.exists()) {
            return false;
        }
        return true;
    }

    private class loadDataAsync extends AsyncTask<String, String, String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }

        @Override
        protected String doInBackground(String... params) {
            loadFile();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        }
    }


}
