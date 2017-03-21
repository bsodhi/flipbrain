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
package in.flipbrain.dto;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author auser
 */
public class ContentDto extends BaseDto {

    public long contentId;
    public String contentType;
    public String url;
    public String title;
    public String description;
    public boolean isPrivate;
    public long userId;
    public String tags;
    public static String YT_URL_MAIN="www.youtube.com";
    public static String YT_URL_SHORT="youtu.be";
    public static String YT_URL_EMBED="http://www.youtube-nocookie.com/embed/";
    public static String YT_URL_THUMBNAIL="http://img.youtube.com/vi/{0}/hqdefault.jpg";

    public static String getVideoIdFromUrl(String url) {
        String vid=null;
        try {
            URL u = new URL(url);
            String host = u.getHost();
            String path = u.getPath();
            String q = u.getQuery();
            if(YT_URL_MAIN.equalsIgnoreCase(host) &&
                    "/watch".equalsIgnoreCase(path)) {
                String[] qp = q.split("&");
                for(String p : qp) {
                    if (p.equals("v")) {
                        vid=p.split("=")[1];
                        break;
                    }
                }
            } else if(YT_URL_SHORT.equalsIgnoreCase(host)) {
                vid=path.substring(1);
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
        return vid;
    }
    
}
