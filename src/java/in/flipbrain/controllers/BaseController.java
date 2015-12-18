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
package in.flipbrain.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import in.flipbrain.Constants;
import in.flipbrain.dto.ClientInfo;
import in.flipbrain.dto.UserDto;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;
import org.javamvc.core.Controller;

/**
 *
 * @author Balwinder Sodhi
 */
public abstract class BaseController extends Controller {

    public enum EmailType {

        ConfirmEmail, PasswordReset, NewPassword
    }

    protected Gson gson = new GsonBuilder().setDateFormat(Constants.DATETIME).create();
    protected final Logger logger = Logger.getLogger(getClass());

    @Override
    public void trace() {
        try {
            logger.info("Serving " + request.getRequestURI() + "?"
                    + request.getQueryString() + ". UserId: " + getLoggedInUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected UserDto.OnetimeAuth makeOnetimeAuth(long userId, int validForDays) {
        UserDto.OnetimeAuth rr = new UserDto.OnetimeAuth();
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, validForDays);
        rr.expiresOn = now.getTime();
        rr.userId = userId;
        rr.uuid = UUID.randomUUID().toString();
        return rr;
    }

    protected String getLoggedInUserName() {
        if (getSessionAttribute(Constants.SK_USER) != null) {
            return ((UserDto) getSessionAttribute(Constants.SK_USER)).login;
        } else {
            return "GUEST";
        }
    }

    protected Long getLoggedInUserId() {
        if (getSessionAttribute(Constants.SK_USER) != null) {
            return ((UserDto) getSessionAttribute(Constants.SK_USER)).userId;
        } else {
            return null;
        }
    }

    protected String getClientIP() {
        return request.getRemoteAddr();
    }

    public ClientInfo getClientInfo() {
        return new ClientInfo(getLoggedInUserName(), getClientIP());
    }

    public ClientInfo getClientInfo(String userId) {
        return new ClientInfo(userId, getClientIP());
    }

    protected <T> T getSessionAttribute(String key) {
        return (T) request.getSession().getAttribute(key);
    }

    protected void setSessionAttribute(String key, Object value) {
        request.getSession().setAttribute(key, value);
    }

    protected void removeSessionAttribute(String key) {
        request.getSession().removeAttribute(key);
    }

    protected void invalidateSession() {
        request.getSession().invalidate();
    }

    protected String getRequestParameter(String name) {
        return request.getParameter(name);
    }

    protected void sendAsJson(Object obj) throws IOException {
        Json(gson.toJson(obj));
    }

    protected void sendErrorAsJson(String message) throws IOException {
        HashMap<String, String> obj = new HashMap<String, String>();
        obj.put("Status", "ERROR");
        obj.put("Message", message);
        Json(gson.toJson(obj));
    }

    protected void sendOKAsJson(String message) throws IOException {
        HashMap<String, String> obj = new HashMap<String, String>();
        obj.put("Status", "OK");
        obj.put("Message", message);
        Json(gson.toJson(obj));
    }

    protected <T> T getJsonRequestAsObject(Type type) throws IOException {
        String jsonPayload = getJsonData();
        return (T) gson.fromJson(jsonPayload, type);
    }

    protected void setJsonDateFormat(String fmt) {
        gson = new GsonBuilder().setDateFormat(fmt).create();
    }

    protected void sendEmail(String to, String subject, String body) throws EmailException {
        logger.debug("Sending email to " + to + "\nSubject: " + subject + "\nMessage: " + body);
        if ("true".equalsIgnoreCase(getConfigValue(Constants.EM_FAKE_SEND))) return;

        Email email = new SimpleEmail();
        email.setHostName(getConfigValue("smtp.host"));
        email.setSmtpPort(Integer.parseUnsignedInt(getConfigValue("smtp.port")));
        email.setAuthenticator(new DefaultAuthenticator(
                getConfigValue("smtp.user"), getConfigValue("smtp.password")));
        email.setSSLOnConnect(Boolean.parseBoolean(getConfigValue("smtp.ssl")));
        email.setFrom(getConfigValue("smtp.sender"));
        email.setSubject(subject);
        email.setMsg(body);
        email.addTo(to);
        email.send();
    }

    protected void sendEmail(String to, EmailType type, Object data) 
            throws IOException, EmailException, IllegalAccessException, 
            InvocationTargetException, NoSuchMethodException {
        String keyPrefix=null;
        switch (type) {
            case ConfirmEmail:
                keyPrefix = Constants.EM_CONFIRM_EMAIL;
                break;
            case NewPassword:
                keyPrefix = Constants.EM_NEW_PASSWORD;
                break;
            case PasswordReset:
                keyPrefix = Constants.EM_PASSWORD_RESET;
                break;
        }
        String sub=getConfigValue(keyPrefix+".subject");
        String tem=getConfigValue(keyPrefix+".template");
        String msgBody = populateTemplate(tem, data);
        sendEmail(to, sub, msgBody);
    }
    
    public HashMap objToMap(Object obj) {
        HashMap map = gson.fromJson(gson.toJson(obj), HashMap.class);
        return map;
    }

    public HashMap jsonToMap(String json) {
        HashMap map = gson.fromJson(json, HashMap.class);
        return map;
    }
}
