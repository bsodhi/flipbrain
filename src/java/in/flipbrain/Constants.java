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

/**
 *
 * @author Balwinder Sodhi
 */
public interface Constants {
    String CFG_LOGIN_PROVIDER = "login.provider.class";
    String CFG_YT_API_KEY="youtube.api.key";
    String CFG_YT_VIDEO_API="youtube.videos.api";
    String CFG_YT_PLAYLIST_ITEMS_API="youtube.playlistItems.api";
    String CFG_GA_CLIENT_ID = "ga.client.id";
    // Keys for session attributes
    String SK_USER = "user";
    String SK_ROLES = "roles";
    
    String DATE_DDMMYYYY = "dd/MM/yyyy";
    String DATETIME = "MMM d,yyyy HH:mm:ss";
    int PAGE_SIZE = 8;
    
    // Actions used in analytics
    String VIEW_TRAIL = "VT";
    String VIEW_TRAIL_ITEM = "VI";
    String ADD_SUBS = "AS";
    String DELETE_SUBS = "DS";
    String POST_COMMENT = "PC";
    String DELETE_COMMENT = "DC";
    String VIEW_COMMENTS = "VC";
    
    // Question types
    String QT_FTXT = "FTXT";
    String QT_MCMA = "MCMA";
    String QT_MCSA = "MCSA";
    
    /* Keys for extra config properties. */
    // Email templates
    String EM_CONFIRM_EMAIL = "email.confirm.email";
    String EM_NEW_PASSWORD = "email.new.password";
    String EM_PASSWORD_RESET = "email.reset.password";
    String EM_FAKE_SEND = "email.fake.send";
    String SITE_URL = "site.url";
    
}
