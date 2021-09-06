package com.elotouch.otamanager;

import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * author : Kane.wang
 * e-mail : Kane.wang@elotouch.com
 * date   : 21/09/06 17:46
 * desc   :
 * version: 1.0
 */
public class AssertManager {


        private String assetFileName;
        private Context context;

        public AssertManager(Context context){
            this.context = context;
        }

        public AssertManager fromAsset(String assetFileName){
            this.assetFileName = assetFileName;
            return this;
        }

        /**
         */
        public void toSdcard(String filePath){
            try {
                InputStream inStream = context.getAssets().open(assetFileName);
                OutputStream outStream = new FileOutputStream(filePath);
                byte[] buffer = new byte[1024*3];
                int length = inStream.read(buffer);
                outStream.write(buffer, 0, length);
                outStream.flush();
                inStream.close();
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

}
