"""
Copyright 2017 Balwinder Sodhi

Licenced under MIT Licence as available here:
https://opensource.org/licenses/MIT

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

Created on Mar 3, 2017

@author: Balwinder Sodhi
"""

import hashlib
import random
import string
import urllib2

from common import *
from flipbrain.entities import TrailDto, UserDto


class UserHandler(BaseHandler):
    """
    Handler class for user actions.

    All requests related to user details and activities are handled by this
    class.
    """
    def getDataForHomePage(self):
        """
        Returns the data to be shown on home page.

        Data is returned as a JSON string of the following structure:
        {"Status":"OK or ERROR", "Message":"payload JSON"}
        :param self:
        :return:
        """
        result = dict()
        result["Trails"] = []
        result["TrailsCount"] = 1
        pg_size = 20
        result["PageSize"] = pg_size
        pg = self.request.params['pageNo']
        trails = TrailDto.query().fetch(pg_size, offset=pg_size * (int(pg)-1))

        for t in trails:
            td = dict()
            td['trailId'] = t.key.id()
            td['thumbnailUrl'] = t.thumbnailUrl()
            td['title'] = t.title
            td['tags'] = t.tags
            result["Trails"].append(td)

        self.send_json_response(Const.STATUS_OK, result)

    def getSessionDetails(self):
        """
        Returns the details stored in a user session.

        Mainly the session data for a user includes the user's profile and
        any trail subscriptions etc.
        :return: JSON string containing the user session information.
        """
        result = dict()
        result["Subscriptions"] = []
        if self.session.has_key('user'):                
            result["User"] = self.session['user']

        self.send_json_response(Const.STATUS_OK, result)
    
    def getCurrentUser(self):
        """
        Sends to the client the profile of currently logged in user.

        :return: JSON string containing the currently logged in user profile
        is sent back as HTTP response to the client.
        """
        if self.is_logged_in():
            self.send_json_response(Const.STATUS_OK, self.session.get("user"))
        else:
            self.send_json_response(Const.STATUS_ERROR, "User is not logged in.")

    def register(self):
        """
        Creates a new user record in datastore.

        :return: On successful creation of user record it send the status
        JSON back as HTTP response to client.
        """
        lf = json.loads(self.request.body)
        u = UserDto()
        # Update values from the input form
        u.populate_from_dict(lf)

        # Update password if sent in the form
        if lf['password'] != lf["password2"]:
            self.send_json_response(Const.STATUS_ERROR, "Passwords not matching. Please check again.")
            return

        logging.info("Updated password.")
        u.password = hashlib.sha256(lf['password']).hexdigest()

        # Save the record
        u.put()
        logging.info("Created new user!")
        # u.password = None
        # self.session['user'] = u.to_dict_with_id('userId')
        self.send_json_response(Const.STATUS_OK, "Added new user.")

    def saveUser(self):
        """
        Saves the user profile in datastore.

        :return: If successful then the JSON string containing the user
        profile will be sent as HTTP response to the client.
        """
        lf = json.loads(self.request.body)
        cu = self.get_current_user()
        if cu is not None:
            # Load user from DB
            u = UserDto.get_by_id(cu['userId'])
            if u is not None:
                curr_passwd = u.password
                # Update values from the input form
                u.populate_from_dict(lf)

                # Change password if sent in the form
                if 'changePassword' in lf:
                    p_old = hashlib.sha256(lf['password']).hexdigest()
                    if p_old == curr_passwd and lf['newPassword'] == lf['password2']:
                        logging.info("Changed password.")
                        u.password = hashlib.sha256(lf['newPassword']).hexdigest()
                    else:
                        self.send_json_response(Const.STATUS_ERROR, "Incorrect password.")
                        return
                else:
                    u.password = curr_passwd # Restore original

                # Save the record
                u.put()
                logging.info("Updated the user!")
                u.password = None
                self.session['user'] = u.to_dict_with_id('userId')
            else:
                logging.warn("!!!! User found in session but missing in datastore!")
        else:
            logging.info("User not found in session. Creating new user.")
            u = UserDto()
            u.populate_from_dict(lf)
            u.password = hashlib.sha256(lf['password']).hexdigest()
            u.put()
        
        self.send_json_response(Const.STATUS_OK, lf)
    
    def requestPassword(self):
        """
        It expects an HTTP parameter named "email". If found then the user's
        password is reset to a temporarily generated password which is also
        emailed to the user at supplied email address.

        :return: JSON status string is sent as HTTP response to the client.
        """
        email = self.request.params["email"]
        success = False
        if email:
            u_list = UserDto.query(UserDto.email == email).fetch()
            if u_list:
                user = u_list[0]
                pswd = ''.join(random.choice(string.ascii_uppercase + string.digits)
                               for _ in range(6))
                user.password = hashlib.sha256(pswd).hexdigest()
                user.put()
                self.send_email(user.email, "Temporary password for flipBRAIN", '''
                Dear {},\n\nYour temporary password is {}.\n\nThanks,
                Team flipBRAIN\n\nThis is an automated email. Please DO NOT reply.
                '''.format(user.firstName, pswd))
                success = True

        if success:
            self.send_json_response(Const.STATUS_OK,
                                    "Please check your email for further instructions.")
        else:
            self.send_json_response(Const.STATUS_ERROR, "Invalid user.")
    
    def __httpGet(self, url):
        """
        Invokes the HTTP GET on the supplied URL and returns the contents.

        :param url: The HTTP URL to be loaded.
        :return: Content returned by the HTTP GET.
        """
        try:
            result = urllib2.urlopen(url)
            return result.read()
        except urllib2.URLError:
            logging.exception('Caught exception fetching url')
    
    def login(self):
        """
        Performs the user authentication using supplied user name password
        or using Google OAuth2 protocol.

        If the HTTP request contains a parameter named "auth2code" then it
        is assumed that the Google OAuth2 based authentication check needs
        to be done. Otherwise, a form based authentication is performed.
        :return: Status JSON string is sent as HTTP response to the client.
        """
        status = Const.STATUS_ERROR
        data = "Login failed."
        lf = json.loads(self.request.body)
        success = False
        user = None
        # In case of Google login
        if lf.has_key('auth2Code'):
            logging.info("Authenticating via Google")
            ga_client_id = self.get_setting(Const.CFG_AUTH_CLIENT_ID)
            auth_check_json = self.__httpGet("https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=%s" % lf['auth2Code']);
            rd = json.loads(auth_check_json)
            if rd["aud"] == ga_client_id:
                ul = UserDto.query(UserDto.email==rd['email']).fetch()
                # Create a user record if first time login
                if not ul:
                    logging.info("First time logging in. Creating user record.")
                    user = UserDto(email = rd['email'], external=True,
                                firstName=rd['given_name'],
                                lastName=rd['family_name'],
                                )
                    user.put()
                else:
                    user = ul[0]
                success = True
        # Form based login
        else:
            logging.info("Authenticating via with email. %s", str(lf))
            ul = UserDto.query(UserDto.email == lf['login']).fetch()
            hex_pw = hashlib.sha256(lf['password']).hexdigest()
            if ul != [] and hex_pw == ul[0].password:
                user = ul[0]
                success = True

        if success:
            #user = user.to_dict()
            user.password = None # Clear the password
            self.session['user'] = user.to_dict_with_id('userId')
            status = Const.STATUS_OK
            data = "Login succeeded."
        else:
            logging.info("Login failed for user %s", lf.get('login'))

        self.send_json_response(status, data)

    def logout(self):
        """
        Logs a user out and clears the session data for the user.
        :return: JSON string with status is sent as HTTP response to client.
        """
        self.session.clear()
        self.send_json_response(Const.STATUS_OK, "User logged out.")
