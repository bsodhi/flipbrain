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
import in.flipbrain.LocalLoginProvider;
import in.flipbrain.LoginProvider;
import in.flipbrain.Utils;
import in.flipbrain.dao.MyBatisDao;
import in.flipbrain.dto.TrailDto;
import in.flipbrain.dto.TrailSubsDto;
import in.flipbrain.dto.UserDto;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.javamvc.core.ViewProvider;
import org.javamvc.core.annotations.Action;
import org.javamvc.core.annotations.Authorize;

/**
 *
 * @author Balwinder Sodhi
 */
public class App extends BaseController {

    private LoginProvider loginProvider;
    
    @Override
    public void init(ConcurrentHashMap sharedData, ServletContext context, 
            HttpServletRequest req, HttpServletResponse res, ViewProvider vp) {
        super.init(sharedData, context, req, res, vp);
    
        try {
            String lpClass = getConfigValue(Constants.CFG_LOGIN_PROVIDER);
            if (lpClass.equals(LocalLoginProvider.class.getName())) {
                loginProvider = new LocalLoginProvider(MyBatisDao.getInstance(
                        getClientInfo()));
            } else {
                loginProvider = (LoginProvider) Class.forName(lpClass).newInstance();
            }
        } catch (Exception e) {
            logger.fatal("Could not initialize login provider. ", e);
        }
    }

    private void recordLoginAttempt(UserDto user, boolean failed) {
        if (user == null) {
            return;
        }
        HashMap<String, Object> param = new HashMap<String, Object>();
        param.put("userId", user.userId);
        if (failed) {
            param.put("failedLogins", user.failedLogins + 1);
        } else {
            param.put("failedLogins", 0);
        }
        MyBatisDao.getInstance(getClientInfo()).recordLoginAttempt(param);
    }

    @Action
    public void loginJson() throws IOException, ServletException {
        if (isJsonRequest()) {
            boolean failed = true;
            HashMap<String, Object> result = new HashMap<String, Object>();
            String jsonPayload = getJsonData();
            UserDto user = gson.fromJson(jsonPayload, UserDto.class);
            // Login using external service
            if (user.auth2Code != null && user.login == null && user.password == null) {
                // Check with Google API
                if (Utils.auth2Check(user.auth2Code, getConfigValue(Constants.CFG_GA_CLIENT_ID))) {
                    user.external = true;
                    user.password = RandomStringUtils.randomAlphanumeric(10);
                    user.login = user.email;
                    UserDto u2 = MyBatisDao.getInstance(getClientInfo()).
                            getUserByLogin(user.login);
                    if (u2 == null) {
                        result = saveUser(user); // Save only first time
                    }
                    user = u2;
                    failed = false;
                }
            } else {
                failed = !loginProvider.login(user.login, user.password);
            }
            recordLoginAttempt(user, failed);
            if (failed) {
                result.put("Status", "error");
                result.put("Message", user != null && user.failedLogins >= 3 ? 
                        "Too many failed logins! Your account has been locked."
                        : "Login or password is wrong. Please retry.");
                sendJsonErrorResponse(HttpServletResponse.SC_UNAUTHORIZED,
                        gson.toJson(result));
                return;
            } else {
                user = MyBatisDao.getInstance(getClientInfo()).
                            getUserByLogin(user.login);
                user.password = null;
                setSessionAttribute(Constants.SK_USER, user);
                setSessionAttribute(Constants.SK_ROLES, user.getRoleNames());
                result.put("Status", "success");
            }
            Json(gson.toJson(result));
        } else {
            sendJsonErrorResponse(406, "Expected JSON request.");
        }
    }

    @Action
    public void requestPassword() throws IOException {
        String em = getRequestParameter("email");
        if (em == null || !em.contains("@")) {
            sendErrorAsJson("Could not process your request. Please retry after sometime.");
            return;
        }
        UserDto u = MyBatisDao.getInstance(getClientInfo()).getUserByEmail(em);
        if (u != null) {
            UserDto.OnetimeAuth rr = makeOnetimeAuth(u.userId, 2);
            MyBatisDao.getInstance(getClientInfo()).addOnetimeAuth(rr);
            try {
                // Send email with reset link
                sendEmail(em, EmailType.PasswordReset, rr);
                sendOKAsJson("Please check your email for further instructions.");
            } catch (Exception ex) {
                logger.fatal("Could not send password reset email to user.", ex);
                sendErrorAsJson("Could not process your request. Please retry after sometime.");
            }
        } else {
            logger.fatal("!!!!! Attempt to reset password using unknown email: " + em);
            sendErrorAsJson("Could not process your request. Please retry after sometime.");
        }
    }

    @Action
    public void confirmUser() throws IOException {
        HashMap<String, String> model = new HashMap<String, String>();
        String msg = "";
        String em = getRequestParameter("email");
        String uuid = getRequestParameter("u");
        if (em == null || uuid == null || !em.contains("@") || uuid.length() < 36) {
            model.put("message", "Could not process your request. Please retry after sometime.");
            View(model);
            return;
        }
        UserDto u = MyBatisDao.getInstance(getClientInfo()).getUserByEmail(em);
        if (u != null && !u.external) {
            UserDto.OnetimeAuth rr = new UserDto.OnetimeAuth();
            rr.userId = u.userId;
            rr.uuid = uuid;
            boolean exists = MyBatisDao.getInstance(getClientInfo()).
                    onetimeAuthExists(rr);
            if (exists) { // Mark the user as verified
                u.password = null;
                u.verified = true;
                MyBatisDao.getInstance(getClientInfo()).saveUser(u);
                MyBatisDao.getInstance(getClientInfo()).clearOnetimeAuth(rr);
                msg = "Thanks for verifying your email address!";
            } else {
                logger.fatal("!!!!! Attempt to verify email using non-existent request.");
                msg = "Your request could not be processed.";
            }
        } else if (u != null && u.external) {
            logger.fatal("!!!!! Attempt to submit verification of external user: " + em);
            msg = "Your request could not be processed.";
        } else {
            logger.fatal("!!!!! Attempt to submit verification using unknown email: " + em);
            msg = "Could not process your request. Please retry after sometime.";
        }
        model.put("message", msg);
        View(model);
    }

    @Action
    public void resetPassword() throws IOException {
        HashMap<String, String> model = new HashMap<String, String>();
        String msg = "";
        String em = getRequestParameter("email");
        String uuid = getRequestParameter("u");
        if (em == null || uuid == null || !em.contains("@") || uuid.length() < 36) {
            model.put("message", "Could not process your request. Please retry after sometime.");
            View(model);
            return;
        }
        UserDto u = MyBatisDao.getInstance(getClientInfo()).getUserByEmail(em);
        if (u != null && !u.external) {
            UserDto.OnetimeAuth rr = new UserDto.OnetimeAuth();
            rr.userId = u.userId;
            rr.uuid = uuid;
            boolean exists = MyBatisDao.getInstance(getClientInfo()).
                    onetimeAuthExists(rr);
            if (exists) {
                String passwd = RandomStringUtils.randomAlphanumeric(8);
                u.password = User.getHashedPassword(passwd);
                MyBatisDao.getInstance(getClientInfo()).saveUser(u);
                MyBatisDao.getInstance(getClientInfo()).clearOnetimeAuth(rr);
                try {
                    // Notify user about password change.
                    sendEmail(em, EmailType.NewPassword, passwd);
                } catch (Exception ex) {
                    logger.fatal("Could not send password change email to user.", ex);
                }
                msg = "Please login with your new password.";
            } else {
                logger.fatal("!!!!! Attempt to reset password using non-existent request.");
                msg = "Your request could not be processed.";
            }
        } else if (u != null && u.external) {
            logger.fatal("!!!!! Attempt to reset password of external user: " + em);
            msg = "Your request could not be processed.";
        } else {
            logger.fatal("!!!!! Attempt to reset password using unknown email: " + em);
            msg = "Could not process your request. Please retry after sometime.";
        }
        model.put("message", msg);
        View(model);
    }

    private HashMap<String, Object> saveUser(UserDto user) {
        String passwd = user.password;
        HashMap<String, Object> result = new HashMap<String, Object>();
        String hashPasswd = User.getHashedPassword(passwd);
        user.password = hashPasswd;
        MyBatisDao.getInstance(getClientInfo()).saveUser(user);
        if (user.userId > 0) {
            user.password = null;
            setSessionAttribute(Constants.SK_USER, user);
            setSessionAttribute(Constants.SK_ROLES, user.getRoleNames());
            result.put("Status", "success");
        } else {
            result.put("Status", "error");
            result.put("Message", "Could register new user. Please retry.");
        }
        return result;
    }

    @Action
    public void logout() throws IOException {
        request.getSession().invalidate();
        //response.sendRedirect(request.getContextPath());
        Json("OK");
    }

    @Action
    public void search() throws IOException {
        String q = getRequestParameter("q");
        List<TrailDto> list = MyBatisDao.getInstance(getClientInfo()).searchTrails(q);
        Json(gson.toJson(list));
    }

    @Authorize
    @Action
    public void main() throws IOException {
        String user = (String) getSessionAttribute(Constants.SK_USER);
        HashMap model = new HashMap();
        model.put(Constants.SK_USER, user);
        model.put(Constants.SK_ROLES, getSessionAttribute(Constants.SK_ROLES));
        if (user != null) {
            View(model);
        } else {
            response.sendRedirect(request.getContextPath());
        }
    }

    @Action
    public void getSessionDetails() throws IOException {
        HashMap model = new HashMap();
        UserDto u = getSessionAttribute(Constants.SK_USER);
        if (u != null) {
            model.put("User", u);
            List<TrailSubsDto> subs = MyBatisDao.getInstance(
                    getClientInfo()).getSubsForUser(u.userId);
            model.put("Subscriptions", subs);
        }
        Json(gson.toJson(model));
    }

    @Action
    public void getDataForHomePage() throws IOException {
        String pgNoStr = getRequestParameter("pageNo");
        int offset = 0;
        if (pgNoStr != null) {
            offset = Integer.parseInt(pgNoStr) - 1;
        }
        HashMap model = new HashMap();
        List<TrailDto> list = MyBatisDao.getInstance(getClientInfo()).
                getPublicTrails(offset, Constants.PAGE_SIZE);
        model.put("Trails", list);
        int c = MyBatisDao.getInstance(getClientInfo()).getPublicTrailsCount();
        model.put("TrailsCount", c);
        model.put("PageSize", Constants.PAGE_SIZE);
        Json(gson.toJson(model));
    }
}
