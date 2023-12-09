package africa.jopen.ripple.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtils {

    public static boolean isJson(String str) {
        try {
            new JSONObject(str);
        } catch (JSONException ex) {
            try {
                new JSONArray(str);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
}