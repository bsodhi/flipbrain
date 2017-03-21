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

import webapp2
import logging
import json
import datetime
from json import JSONEncoder
from google.appengine.ext import ndb
from entities import UserDto, Settings
from webapp2_extras import sessions
from google.appengine.api import app_identity
from google.appengine.api import mail
from google.appengine.api import memcache


# from functools import wraps
# 
# def auth(func):
#     '''
#     Decorator for checking whether there is a valid user is logged
#     in or not. This is to be applied on request handler methods.
#     If a session variable named "user" is found then we assume that
#     there is a used logged on.
#     '''
#     def auth_decorator(func):
#         @wraps(func)
#         def func_wrapper(*args, **kwargs):
#             # We decorate only request handlers
#             if isinstance(args[0], BaseHandler):
#                 if not args[0].session.get("user"):
#                     args[0].abort(403)
#             return func(*args, **kwargs)
#         return func_wrapper
#     return auth_decorator

class Const:
    '''
    Defines the constants used in this application code.
    '''
    CFG_YOUTUBE_KEY = "YOUTUBE_KEY"
    CFG_AUTH_CLIENT_ID = "AUTH_CLIENT_ID"
    STATUS_OK = "OK"
    STATUS_ERROR = "ERROR"

class NdbEncoder(JSONEncoder):
    def default(self, obj):
        if isinstance(obj, datetime.date):
            return obj.isoformat()
        elif isinstance(obj, ndb.KeyProperty) or isinstance(obj, ndb.Key):
            return obj.id()
        # elif isinstance(obj, BaseDto):
        #     return obj.to_dict()
        return JSONEncoder.default(self, obj)

class BaseHandler(webapp2.RequestHandler):
    """
    Base class for all HTTP request handlers.

    It implements common functions which are used by different handlers.
    """
    def dispatch(self):
        """
        This function overrides the base class function in order to provide
        session functionality.
        :return:
        """
        # Get a session store for this request.
        self.session_store = sessions.get_store(request=self.request)

        try:
            # Dispatch the request.
            webapp2.RequestHandler.dispatch(self)
        finally:
            # Save all sessions.
            self.session_store.save_sessions(self.response)

    @webapp2.cached_property
    def session(self):
        """
        Returns a session using the default cookie key.
        :return:
        """
        return self.session_store.get_session()

    def _process_request(self, action):
        """
        Processes the HTTP GET or POST request.

        It simply invokes a function named 'action', if found, in the handler.
        :param action: Name of the action function to be invoked.
        :return:
        """
        try:
            logging.info("Invoking action %s" % action)
            getattr(self, action)()
        except ValueError, v_err:
            logging.fatal("Could not invoke action method %s. Error: %s", action, str(v_err))
            self.abort(500, "ERROR occurred!")

    def post(self, action):
        """
        Handles the HTTP POST request.

        :param action: Action name received as part of the HTTP request path.
        :return:
        """
        self._process_request(action)


    def get(self, action):
        """
        Handles the HTTP GET request.

        :param action: Action name received as part of the HTTP request path.
        :return:
        """
        self._process_request(action)

    def is_logged_in(self):
        return "user" in self.session

    def get_current_user(self):
        """
        Returns details of the currently logged on user if found in session.

        The currently logged on user's details are expected to be stored in
        session under the key 'user'.
        :return:
        """
        if "user" in self.session:
            return self.session.get("user")
        else:
            return None

    def get_current_user_key(self):
        """
        Created and returns an AppEngine NDB Key object from the userId
         of currently logged on user.
        :return:
        """
        return ndb.Key(UserDto, self.get_current_user()['userId'])

    @staticmethod
    def copy_with_dst_keys(dst, src, clear_missing=False):
        """

        :param dst:
        :param src:
        :param clear_missing:
        :return:
        """
        for k in dst.keys():
            if not clear_missing and not src.has_key(k):
                continue
            dst[k] = src.get(k)
        return dst

    def send_json_response(self, status, data):
        """

        :param status:
        :param data:
        :return:
        """
        resp = {}
        resp["Status"] = status
        resp["Message"] = data
        json_str = json.dumps(resp, cls=NdbEncoder)
        logging.debug("Sending JSON string:\n%s", json_str)
        self.response.write(json_str)

    def load_json_request(self):
        """

        :return:
        """
        return json.loads(self.request.body)

    def send_email(self, to, subject, body_text):
        """

        :param to:
        :param subject:
        :param body_text:
        :return:
        """
        sender = '{}@appspot.gserviceaccount.com'.format(
            app_identity.get_application_id())
        mail.send_mail(sender=sender, to=to, subject=subject, body=body_text)

    def get_setting(self, key):
        """
        Fetches the setting value from memcache or from datastore in case it
        is not yet in memcache.

        :param key: Key to be looked up.
        :return: Value of the given key/setting
        """
        mc_key = "FBRN_KEY:{}".format(key)
        data = memcache.get(mc_key)
        if data is None:
            # Fetch setting object from datastore
            s_list = Settings.query(Settings.name == key).fetch()
            if not s_list:
                s = Settings(name=key, value="NOT SET")
                s.put()
                raise Exception("Please create the setting %s via developer console!" % key)

            # Put the setting into memcache
            data = s_list[0]
            memcache.add(mc_key, data, 3600)

        return data.value


class MyWarmupHandler(BaseHandler):
    """
    Initializes the memcache entries for settings etc.
    """
    def get(self):
        try:
            self.get_setting(Const.CFG_AUTH_CLIENT_ID)
        except Exception as e:
            logging.exception(e.message)

        try:
            self.get_setting(Const.CFG_YOUTUBE_KEY)
        except Exception as e:
            logging.exception(e.message)

        self.send_json_response(Const.STATUS_OK, "Initialized.")