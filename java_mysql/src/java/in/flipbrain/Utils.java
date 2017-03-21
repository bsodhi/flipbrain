/*
Copyright 2015 Balwinder Sodhi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package in.flipbrain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

/**
 *
 * @author Balwinder Sodhi
 */
public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class);
    private static final Gson gson = new GsonBuilder().create();
    
    public static boolean auth2Check(String code, String gaClientId) {
        boolean passed = false;
        try {
            String authCheckJson = httpGet("https://www.googleapis.com/oauth2/v3/tokeninfo?id_token="+code);
            logger.debug(authCheckJson);
            HashMap jsonObj = gson.fromJson(authCheckJson, HashMap.class);
            passed = jsonObj.get("aud").equals(gaClientId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return passed;
    }

    public static String httpPost(String uri, HashMap<String, String> params) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entrySet : params.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();
            nvps.add(new BasicNameValuePair(key, value));
        }
        String res = null;
        try {
            res = Request.Post(uri).bodyForm(nvps).execute().returnContent().asString();
            logger.debug("Response: " + res);
        } catch (IOException e) {
            logger.fatal("Failed to process request. ", e);
        }
        return res;
    }

    public static String httpGet(String uri, HashMap<String, String> params) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entrySet : params.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();
            query.append(URLEncoder.encode(key)).append("=");
            query.append(URLEncoder.encode(value)).append("&");
        }
        if (query.length() > 0) {
            query.insert(0, "?").insert(0, uri);
        }
        String res = null;
        try {
            String fullUrl = query.toString();
            logger.debug("Request URL: " + fullUrl);
            res = Request.Get(fullUrl).execute().returnContent().asString();
            logger.debug("Response: " + res);
        } catch (IOException e) {
            logger.fatal("Failed to process request. ", e);
        }
        return res;
    }

    public static String httpGet(String fullUrl) {
        String res = null;
        try {
            res = Request.Get(fullUrl).execute().returnContent().asString();
            logger.debug("Response: " + res);
        } catch (IOException e) {
            logger.fatal("Failed to process request. ", e);
        }
        return res;
    }

    public static String getVideoId(String url) throws MalformedURLException {
        URL u = new URL(url);
        String host = u.getHost();
        if (host.equals("youtu.be")) {
            return u.getPath();
        } else if (host.equals("www.youtube.com")) {
            String path = u.getPath();
            if (path.contains("embed")) {
                return path.substring(path.indexOf("/"));
            } else {
                String query = u.getQuery();
                String[] parts = query.split("&");
                for (String p : parts) {
                    if (p.startsWith("v=")) {
                        return p.substring(2);
                    }
                }
            }
        }
        return null;
    }
}
