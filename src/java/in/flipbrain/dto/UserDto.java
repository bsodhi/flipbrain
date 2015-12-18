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

import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author theuser
 */
public class UserDto extends BaseDto {

    public long userId;
    public String login;
    public String password;
    public String password2;
    public String newPassword;
    public String firstName;
    public String lastName;
    public String middleName;
    public String gender;
    public boolean changePassword;
    public boolean external;
    public ArrayList<RoleDto> roles=new ArrayList<RoleDto>();
    public String email;
    public String auth2Code;
    public int failedLogins;
    public Date lastLogin;
    public boolean verified;

    public UserDto() {
    }

    public String[] getRoleNames() {
        return new String[]{"USER"}; //TODO: Get from roles list
    }

    public static class OnetimeAuth {
        public long authId;
        public long userId;
        public String uuid;
        public Date expiresOn;
        public Date insTs;
    }
}
