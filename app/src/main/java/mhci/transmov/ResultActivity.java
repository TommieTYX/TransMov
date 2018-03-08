package mhci.transmov;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.WebEntity;
import com.google.api.services.vision.v1.model.WebLabel;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import javax.net.ssl.HttpsURLConnection;

public class ResultActivity extends AppCompatActivity {
    final private String TAG = "ResultActivity";
    private static final String CLOUD_VISION_API_KEY = "AIzaSyAmKx2yRL1WloWEytEDEv0qYkT_rGCkvEY";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private String movieInfo = "";
    private String moviePlot = "";
    private Map<String, String> m;

    ProgressDialog progress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        progress = new ProgressDialog(ResultActivity.this);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray("image");

        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

       // Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        ImageView image = (ImageView) findViewById(R.id.result_image);

        image.setImageBitmap(bitmap);






        final Locale[] locales = Locale.getAvailableLocales();
        int defaultLocaleId = 0;

        ArrayList<String> localcountries=new ArrayList<String>();
        for(Locale l:locales)
        {
            localcountries.add(l.getDisplayLanguage().toString());
            if (l.equals(Resources.getSystem().getConfiguration().locale)){
                defaultLocaleId = localcountries.size() - 1;
            }
        }
        String[] languages=(String[]) localcountries.toArray(new String[localcountries.size()]);

        Spinner langSelect = (Spinner)findViewById(R.id.langSelect);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        langSelect.setAdapter(adapter);

        langSelect.setSelection(defaultLocaleId);

        langSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();
                //translateInfo(m, locales[position].toString()); //TODO: FIX THIS
                //Log.i("ASDKJAHSDAHSJAD", locales[position].toString());
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });




        try {
            callCloudVision(bitmap);
        } catch (IOException e) {
            Log.i(TAG,"error is: "+e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                Log.i("TAG","page is "+ upIntent);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    TaskStackBuilder.create(this)
                            .addNextIntentWithParentStack(upIntent)
                            .startActivities();
                } else {
                    NavUtils.navigateUpTo(this, upIntent);
                }
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("StaticFieldLeak")
    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Switch text to loading
        showProgress(true);
        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                    visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                                    String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                    visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                                }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType("WEB_DETECTION");
                            labelDetection.setMaxResults(10);
                            add(labelDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {
               /// mImageDetails.setText(result);
                TextView director = (TextView)findViewById(R.id.directorTxt);
                TextView actor = (TextView)findViewById(R.id.actorTxt);
                TextView plot = (TextView)findViewById(R.id.plotTxt);
                TextView movieDetail = (TextView)findViewById(R.id.movieDetail);

                m = OmdbHelper.get(result);

                String deviceLocale = Resources.getSystem().getConfiguration().locale.toString();

                movieInfo = "Year: " + m.get("Year") + "\r\n" +
                        "Released: " + m.get("Released") + "\r\n" +
                        "Run-Time: " + m.get("Runtime") + "\r\n" +
                        "Rated: " + m.get("Rated") + "\r\n";

                moviePlot = m.get("Plot");

                if (!m.isEmpty()){
                    if (deviceLocale.contains("en")){
                        translateInfo(m, "de"); //TODO: change to defaultLocale
                    }


                    movieDetail.setText(movieInfo);
                    director.setText(m.get("Director"));
                    actor.setText(m.get("Actors"));
                    plot.setText(moviePlot);

                } else {
                    movieDetail.setText("THIS WAS THE BEST GUESS: " + result);// + m.get("rated").toString());
                }
                showProgress(false);
            }
        }.execute();
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        String message = "";
        AnnotateImageResponse imageResponses = response.getResponses().get(0);
        List<WebLabel> entityAnnotations;

        entityAnnotations = imageResponses.getWebDetection().getBestGuessLabels();
        if (entityAnnotations != null) {
            for (WebLabel entity : entityAnnotations) {
                message += entity.getLabel();
            }
        } else {
            message = "Nothing Found";
        }

        return message;
    }

    public void showProgress(boolean bool){
        if(bool){
            progress.setTitle("Loading");
            progress.setMessage("Please wait while we search for the movie details...");
            progress.setCancelable(false);
            progress.show();
        }else{
            progress.dismiss();
        }
    }

    public String translateText(String s, String locale){
        TranslateOptions options = TranslateOptions.newBuilder()
                .setApiKey(CLOUD_VISION_API_KEY)
                .build();
        Translate translate = options.getService();
        Translation translation = translate.translate(
                s,
                Translate.TranslateOption.sourceLanguage("en"),
                Translate.TranslateOption.targetLanguage(locale));

        return translation.getTranslatedText();
    }

    public void translateInfo(Map m, String locale){
        movieInfo = translateText("Year: " + m.get("Year"), locale) + "\r\n" +
                translateText("Released: " + m.get("Released"), locale) + "\r\n" +
                translateText("Run-Time: " + m.get("Runtime"), locale) + "\r\n" +
                translateText("Rated: " + m.get("Rated"), locale);

        moviePlot = translateText(moviePlot, locale);
    }
}