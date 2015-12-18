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

import in.flipbrain.controllers.User;
import in.flipbrain.dao.MyBatisDao;
import in.flipbrain.dto.UserDto;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 *
 * @author Balwinder Sodhi
 */
public class LocalLoginProvider implements LoginProvider {

    private MyBatisDao dao;

    public LocalLoginProvider() {
    }

    public LocalLoginProvider(MyBatisDao dao) {
        this.dao = dao;
    }

    @Override
    public boolean login(String userId, String password) {
        boolean success = false;
        UserDto user = dao.getUserByLogin(userId);
        if (user != null) {
            Calendar refTime = Calendar.getInstance();
            refTime.add(Calendar.DATE, -3);
            boolean inLockedPeriod = user.failedLogins >= 3 && refTime.before(user.lastLogin);
            success = (!inLockedPeriod
                    && User.getHashedPassword(password).equals(user.password));
        }
        return success;
    }

    /**
     * Always returns a list containing one element "USER".
     * @param userId
     * @return 
     */
    @Override
    public List<String> roles(String userId) {
        return Arrays.asList("USER");
    }

}
