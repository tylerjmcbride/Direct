package github.tylerjmcbride.direct.utilities;

import com.bluelinelabs.logansquare.LoganSquare;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import github.tylerjmcbride.direct.model.data.Data;

public final class DataParser {

    private static final Map<String, Class> cache = new ConcurrentHashMap();

    // Prevent instantiation
    private DataParser() {

    }

    public static Data parse(String json) throws IOException, ClassNotFoundException, JSONException {
        Class clazz = getClass(new JSONObject(json).getString("type"));
        Data data = (Data) LoganSquare.parse(json, clazz);
        return data;
    }

    private static Class getClass(String clazzName) throws ClassNotFoundException {
        Class clazz = cache.get(clazzName);
        if (clazz == null) {
            clazz = Class.forName(clazzName);
            cache.put(clazzName, clazz);
        }
        return clazz;
    }
}
