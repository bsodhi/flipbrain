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

import in.flipbrain.Constants;
import in.flipbrain.dao.MyBatisDao;
import in.flipbrain.dto.UserDto;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import org.javamvc.core.annotations.Action;
import org.javamvc.core.annotations.Authorize;

/**
 *
 * @author Balwinder Sodhi
 */
public class User extends BaseController {

    public static String getHashedPassword(String passwordToHash) {
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(passwordToHash.getBytes());
            byte byteData[] = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return generatedPassword;
    }

    private String validatePasswordChange(UserDto user) {
        String errorMsg = null;
        UserDto currUser = MyBatisDao.getInstance(getClientInfo()).
                getUserById(getLoggedInUserId());
        if (!getHashedPassword(user.password).equals(currUser.password)) {
            errorMsg = "Invalid password!";
        } else if (user.newPassword == null
                || !user.newPassword.equals(user.password2)) {
            errorMsg = "Please check the new password!";
        }
        return errorMsg;
    }

    @Action
    public void save() throws IOException {
        if (isJsonRequest()) {
            UserDto user = getJsonRequestAsObject(UserDto.class);
            user.setAppUser(getLoggedInUserName());
            UserDto u2 = (UserDto) getSessionAttribute(Constants.SK_USER);
            boolean error = false;
            boolean isNewUser = user.userId < 1;

            if ((u2 == null && !isNewUser)
                    || (u2 != null && user.userId != u2.userId)) {
                sendJsonErrorResponse(403, "Not authorized.");
                error = true;
            } else if (!isNewUser && user.userId == u2.userId) { // Edit user
                if (user.changePassword) {
                    String errMsg = validatePasswordChange(user);
                    if (errMsg == null) {
                        user.password = getHashedPassword(user.newPassword);
                    } else {
                        sendErrorAsJson(errMsg);
                        error = true;
                    }
                } else { // Updating only the profile data
                    user.password = null;
                }
            } else {
                // New user
                if (user.password.equals(user.password2)) {
                    user.password = getHashedPassword(user.password);
                } else {
                    sendErrorAsJson("Passwords must match!");
                    error = true;
                }
            }
            // If all goes well then save
            if (!error) {
                MyBatisDao.getInstance(getClientInfo()).saveUser(user);
                try {
                    if (isNewUser) {
                        UserDto.OnetimeAuth auth = makeOnetimeAuth(user.userId, 2);
                        MyBatisDao.getInstance(getClientInfo()).addOnetimeAuth(auth);
                        // Send email to use for confirming email ID
                        HashMap map = objToMap(user);
                        map.put("uuid", auth.uuid);
                        map.put("siteUrl", getConfigValue(Constants.SITE_URL));
                        sendEmail(user.email, EmailType.ConfirmEmail, map);
                    }
                } catch (Exception ex) {
                    logger.error("Could not send registration confirmation. ", ex);
                }
                // Clear passwords before sending to client
                user.password = user.password2 = user.newPassword = null;
                setSessionAttribute(Constants.SK_USER, user);
                setSessionAttribute(Constants.SK_ROLES, user.getRoleNames());
                Json(gson.toJson(user));
            }
        } else {
            sendJsonErrorResponse(406, "Expected JSON request.");
        }
    }

    @Authorize(roles = {"ADMIN"})
    @Action
    public void getUserByLogin() throws IOException {
        String email = request.getParameter("login");
        UserDto user = MyBatisDao.getInstance(getClientInfo()).getUserByLogin(email);
        Json(gson.toJson(user));
    }

    @Authorize
    @Action
    public void getById() throws IOException {
        Integer id = Integer.parseInt(request.getParameter("userId"));
        UserDto u = (UserDto) getSessionAttribute(Constants.SK_USER);
        if (u == null) {
            sendErrorAsJson("You must be logged in to access this information.");
            return;
        }
        if (u.userId != id) {
            sendJsonErrorResponse(403, "Not authorized!");
        } else {
            UserDto user = MyBatisDao.getInstance(getClientInfo()).getUserById(id);
            user.password = null; // Clear password
            Json(gson.toJson(user));
        }
    }
}
