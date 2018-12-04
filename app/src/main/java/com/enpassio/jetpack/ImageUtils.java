package com.enpassio.jetpack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.lang.Math.min;

public class ImageUtils {

    private static final int BUFFER = 2048;
    private final String LOG_TAG = "ImageUtils";
    //  private const val SERVER_UPLOAD_PATH = "http://10.0.2.2:3000/files" //local server URL
    private final String SERVER_UPLOAD_PATH = "https://simple-file-server.herokuapp.com/files"; //shared service URL

    //grayscale multipliers
    private final double GRAYSCALE_RED = 0.3;
    private final double GRAYSCALE_GREEN = 0.59;
    private final double GRAYSCALE_BLUE = 0.11;

    private final int MAX_COLOR = 255;

    private final int SEPIA_TONE_RED = 110;
    private final int SEPIA_TONE_GREEN = 65;
    private final int SEPIA_TONE_BLUE = 20;

    private final String DIRECTORY_OUTPUTS = "outputs";
    private final int COMPRESS_BUFFER_CHUNK = 1024;

    private OkHttpClient okHttpClient;

    private final String MULTIPART_NAME = "file";

    /**
     * Sepia filter.
     * From: https://github.com/yaa110/Effects-Pro/blob/master/src/org/appsroid/fxpro/bitmap/BitmapProcessing.java
     */
    private Bitmap applySepiaFilter(Bitmap bitmap) {
        // image size
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // create output bitmap
        Bitmap outputBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());

        // color information
        int alpha;
        int red;
        int green;
        int blue;
        int currentPixel;

        // scan through all pixels
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                // get pixel color
                currentPixel = bitmap.getPixel(x, y);

                // get color on each channel
                alpha = Color.alpha(currentPixel);
                red = Color.red(currentPixel);
                green = Color.green(currentPixel);
                blue = Color.blue(currentPixel);

                // apply grayscale sample
                red = (int) (GRAYSCALE_RED * red + GRAYSCALE_GREEN * green + GRAYSCALE_BLUE * blue);
                green = red;
                blue = green;

                // apply intensity level for sepid-toning on each channel
                red += SEPIA_TONE_RED;
                green += SEPIA_TONE_GREEN;
                blue += SEPIA_TONE_BLUE;

                //if you overflow any color, set it to MAX (255)
                red = min(red, MAX_COLOR);
                green = min(green, MAX_COLOR);
                blue = min(blue, MAX_COLOR);

                outputBitmap.setPixel(x, y, Color.argb(alpha, red, green, blue));
            }
        }

        bitmap.recycle();

        return outputBitmap;
    }

    File writeBitmapToFile(Context applicationContext, Bitmap bitmap) {
        String randomId = UUID.randomUUID().toString();
        String name = "$randomId.png";

        File outputDirectory = getOutputDirectory(applicationContext);
        File outputFile = new File(outputDirectory, name);

        try {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /* ignored for PNG */, outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return outputFile;
    }

    private File getOutputDirectory(Context applicationContext) {
        File file = new File(applicationContext.getFilesDir(), DIRECTORY_OUTPUTS);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    void cleanFiles(Context applicationContext) {
        File outputDirectory = getOutputDirectory(applicationContext);
        for (File file : outputDirectory.listFiles()) {
            file.delete();
        }
    }

    Uri createZipFile(Context applicationContext, String[] files) {
        String randomId = UUID.randomUUID().toString();
        String name = "$randomId.zip";

        File outputDirectory = getOutputDirectory(applicationContext);
        File outputFile = new File(outputDirectory, name);

        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        compressFiles(zipOutputStream, files);

        return Uri.fromFile(outputFile);
    }

    private void compressFiles(ZipOutputStream out, String[] files) {


        try {
            BufferedInputStream origin = null;

            byte data[] = new byte[BUFFER];

            for (int i = 0; i < files.length; i++) {
                Log.v("Compress", "Adding: " + files[i]);
                FileInputStream fi = new FileInputStream(files[i]);
                origin = new BufferedInputStream(fi, BUFFER);

                ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;

                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void uploadFile(Uri fileUri) {
        File file = new File(fileUri.getPath());

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(MULTIPART_NAME, file.getName(), RequestBody.create(null, file))
                .build();

        Request request = new Request.Builder()
                .url(SERVER_UPLOAD_PATH)
                .post(requestBody)
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(LOG_TAG, "onResponse - Status: ${response?.code()} Body: ${response?.body()?.string()}");
    }
}
