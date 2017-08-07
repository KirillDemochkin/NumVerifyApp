package com.javalab.numveifyapp;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TessHandler {

    TessBaseAPI mTess;
    String datapath;
    Context context;

    TessHandler(Context context){
        this.context = context;

        datapath = context.getFilesDir()+ "/tesseract/";
        checkFile(new File(datapath + "tessdata/"));

        mTess = new TessBaseAPI();
        String language = "eng";
        mTess.init(datapath, language);

    }

    private void checkFile(File dir) {

        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }

        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {

            String filepath = datapath + "/tessdata/eng.traineddata";


            AssetManager assetManager = context.getAssets();


            InputStream instream = assetManager.open("eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);


            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String processImage(Bitmap bitmap){
        mTess.setImage(bitmap);
        String result = mTess.getUTF8Text();
        return result;
    }

}
