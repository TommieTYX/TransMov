package mhci.transmov;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


/**
 * Created by Yuxiang on 8/3/2018.
 */

public class OmdbHelper {
    public static Map<String, String> get(String movieName){
        //OMDB REST API http://www.omdbapi.com/?t=Black+Panther+Poster&apikey=6c6c7fd
        Map<String, String> result = new HashMap<String, String>();
        URL httpbinEndpoint = null;
        HttpsURLConnection myConnection = null;

        movieName.replace("poster", "").trim().replace(' ', '+').trim();
        Log.i("**********************", movieName); //TODO: REMOVE THIS
        try {
            //httpbinEndpoint = new URL("https://www.omdbapi.com/?t="+ movieName +"&apikey=6c6c7fd"); //TODO: FIX THIS
            httpbinEndpoint = new URL("https://www.omdbapi.com/?t=furious+7&apikey=6c6c7fd");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            myConnection = (HttpsURLConnection) httpbinEndpoint.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        myConnection.setRequestProperty("User-Agent", "TransMov-v0.1");

        try {
            if (myConnection.getResponseCode() == 200) {
                // Success
                InputStream responseBody = myConnection.getInputStream();
                InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                JsonReader jsonReader = new JsonReader(responseBodyReader);

                jsonReader.beginObject(); // Start processing the JSON object
                while (jsonReader.hasNext()) { // Loop through all keys
                    String key = jsonReader.nextName(); // Fetch the next key
                    if (key.equals("Title") || key.equals("Rated") || key.equals("Year") ||
                            key.equals("Released") || key.equals("Runtime") || key.equals("Genre") ||
                            key.equals("Director") || key.equals("Actors") || key.equals("Plot")) { // Check if desired key
                        // Fetch the value as a String
                        result.put(key, jsonReader.nextString());
                    } else {
                        jsonReader.skipValue(); // Skip values of other keys
                    }
                }
                jsonReader.close();
            } else {
                // Error handling code goes here
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        myConnection.disconnect();
        return result;
    }
}
