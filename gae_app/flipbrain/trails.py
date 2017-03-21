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

import urllib
import urllib2

from common import *
from entities import *


class YouTubeUtils:
    """
    Utility class for handling youtube API calls.
    """
    def __init__(self, dev_key):
        self.YT_URL = "https://www.googleapis.com/youtube/v3/"
        self.DEVELOPER_KEY = dev_key

    def get_playlist_items(self, pl_id):
        """
        Fetches the youtube playlist items information via youtube API.

        :param pl_id: Playlist ID
        :return: List of videos in the playlist.
        """

        videos = []
        try:
            data = dict()
            data['key'] = self.DEVELOPER_KEY
            data['playlistId'] = pl_id
            data['part'] = "contentDetails,snippet"
            data['maxResults'] = 50
            data['fields'] = "items(snippet(title,description,resourceId/videoId))"
            qs = urllib.urlencode(data)
            url = "{}playlistItems?{}".format(self.YT_URL, qs)
            result = urllib2.urlopen(url)
            res_dict = json.loads(result.read())
            for item in res_dict.get("items", []):
                vid = item['snippet']['resourceId']['videoId']
                title = item['snippet']['title']
                videos.append((vid, title))
        except urllib2.URLError:
            logging.exception('Caught exception fetching url')

        return videos

    def get_playlist_info(self, pl_id):
        """
        Fetches the information such as title and description etc. of
        the given youtube playlist.

        :param pl_id: Playlist ID.
        :return: Tuple (title, description)
        """

        pl_info = None
        try:
            data = dict()
            data['key'] = self.DEVELOPER_KEY
            data['id'] = pl_id
            data['part'] = "snippet"
            data['fields'] = "items(snippet(title,description))"
            qs = urllib.urlencode(data)
            url = "{}playlists?{}".format(self.YT_URL, qs)
            result = urllib2.urlopen(url)
            res_dict = json.loads(result.read())
            # Expected max one item
            for item in res_dict.get("items", []):
                title = item['snippet']['title']
                desc = item['snippet']['description']
                pl_info = (title, desc)
        except urllib2.URLError:
            logging.exception('Caught exception fetching url')

        return pl_info

    def get_video_details(self, v_id):
        """
        Fetches details about a youtube video via youtube API.

        :param v_id: A comma separated list of video IDs.
        :return: List of video detail dictionaries.
        """

        video_list = []
        try:
            data = dict()
            data['key'] = self.DEVELOPER_KEY
            data['id'] = v_id
            data['part'] = "snippet"
            # data['maxResults'] = 50
            data['fields'] = "items(id,snippet(title,description,tags))"
            qs = urllib.urlencode(data)
            url = "{}videos?{}".format(self.YT_URL, qs)
            logging.info(">>>>> YT URL = %s", url)
            result = urllib2.urlopen(url)
            res_dict = json.loads(result.read())
            if "items" in res_dict:
                for item in res_dict["items"]:
                    video = dict()
                    video["title"] = item["snippet"]["title"]
                    video["description"] = item["snippet"]["description"]
                    video["tags"] = ", ".join(item["snippet"]["tags"])
                    video["itemId"] = v_id
                    video["url"] = "http://youtu.be/%s" % v_id
                    video_list.append(video)

        except urllib2.URLError:
            logging.exception('Caught exception fetching url')

        return video_list


class TrailHandler(BaseHandler):
    """
    Handler for trail related HTTP requests.
    """

    def getTrailById(self, for_view=False):
        """

        :param for_view:
        :return:
        """
        t = TrailDto.get_by_id(long(self.request.params["trailId"]))
        td = t.to_dict_with_id('trailId')

        if for_view:
            tv_list = TrailViewsDto.query(TrailViewsDto.trail == t.key).fetch()
            if not tv_list:
                tv = TrailViewsDto(views=1, trail=t.key)
            else:
                tv = tv_list[0]
                tv.views += 1
            tv.put()
            td['viewsCount'] = tv.views

        td['assessments'] = []
        tas = TrailAssessmentDto.query(TrailAssessmentDto.trail == t.key).fetch()
        if tas:
            a_list = AssessmentDto.query(
                AssessmentDto.key.IN([a.assess for a in tas])).fetch()
            td['assessments'] = [a.to_dict_with_id("assessId") for a in a_list]
        else:
            logging.info("No trail assessments found.")


        self.send_json_response(Const.STATUS_OK, td)

    # def addYTContent(self):
    #     f = json.loads(self.request.body)
    #     urls = f.get("url")
    #     vids = []
    #     if urls:
    #         yt = YouTubeUtils()
    #         for u in urls.split("\n"):
    #             c = VideoDto()
    #             v = yt.get_video_details(u.split("/")[-1])
    #             c.populate_from_dict(v)
    #             c.put()
    #             vids.append(c.to_dict_with_id("videoId"))
    #
    #     self.send_json_response(Const.STATUS_OK, "Added videos.")


    def addYTrail(self):
        """

        :return:
        """
        pd = self.request.params
        t = TrailDto(tags=pd['tags'])
        t.resources = []
        yt = YouTubeUtils(self.get_setting(Const.CFG_YOUTUBE_KEY))

        if 'isPL' in pd and pd['isPL']:
            pl_info = yt.get_playlist_info(pd['resource'])
            if not pl_info:
                raise ValueError("Playlist not found!")
            t.title = pl_info[0]

            p = yt.get_playlist_items(pd['resource'])
            for vid in p:
                c = VideoDto()
                c.description = vid[1]
                c.title = vid[1]
                c.itemId = vid[0]
                c.url = "https://youtu.be/%s" % vid[0]
                t.videos.append(c)
        else:
            vid_list = yt.get_video_details(pd['resource'])
            for v in vid_list:
                c = VideoDto()
                c.populate_from_dict(v)
                t.videos.append(c)

        t.owner = self.get_current_user_key()

        if 'title' in pd:
            t.title = pd['title']

        t.put()
        self.send_json_response(Const.STATUS_OK, t.to_dict_with_id('trailId'))

    def saveContent(self):
        self.send_json_response(Const.STATUS_ERROR, "Not supported.")

    def deleteContent(self):
        self.send_json_response(Const.STATUS_ERROR, "Not supported.")

    def saveTrail(self):
        """

        :return:
        """
        tf = json.loads(self.request.body)
        t = TrailDto()
        if 'trailId' in tf:
            t = TrailDto.get_by_id(int(tf['trailId']))
            logging.debug("Loaded trail from DB tid=%s", tf['trailId'])
        t.populate_from_dict(tf)
        t_key = t.put()

        # Clear old trail assessments
        ta_list = TrailAssessmentDto.query(TrailAssessmentDto.trail == t_key).fetch()
        if ta_list:
            ndb.delete_multi([x.key for x in ta_list])

        # Insert newly selected assessments for trail
        if 'assessments' in tf:
            for ta_dict in tf['assessments']:
                ta = TrailAssessmentDto()
                ta.trail = t_key
                ta.assess = ndb.Key(AssessmentDto, ta_dict['assessId'])
                ta.put()

        trl = t.to_dict_with_id("trailId")
        trl['assessments'] = tf['assessments']
        logging.debug("Saved trail to DB tid=%s", t_key.id())
        self.send_json_response(Const.STATUS_OK, trl)

    def deleteTrail(self):
        self.send_json_response(Const.STATUS_ERROR, "Not supported.")

    def getTrailForView(self):
        """

        :return:
        """
        self.getTrailById(for_view=True)

    def searchComments(self):
        self.send_json_response(Const.STATUS_ERROR, "Not supported.")

    def addSubs(self):
        self.send_json_response(Const.STATUS_ERROR, "Not supported.")

    def getComments(self):
        """

        :return:
        """
        v_id = self.request.params["iid"]
        qry = CommentDto.query(CommentDto.trailItemId == str(v_id))
        cl = qry.fetch()
        self.send_json_response(Const.STATUS_OK, [c.to_dict_with_id("commentId") for c in cl])

    def deleteComment(self):
        self.send_json_response(Const.STATUS_ERROR, "Not supported.")

    def saveComment(self):
        """

        :return:
        """

        frm = json.loads(self.request.body)
        if "commentId" in frm:
            c = CommentDto.get_by_id(long(frm["commentId"]))
            if c:
                if c.owner != self.get_current_user_key():
                    raise ValueError("Cannot save comment not owned by current user.")

                c.populate_from_dict(frm)
                c.put()
                result = c.to_dict_with_id("commentId")

        else:
            c = CommentDto(owner=self.get_current_user_key())
            c.populate_from_dict(frm)
            u = self.get_current_user()
            c.author = "%s %s" % (u['firstName'], u['lastName'])
            c.put()
            result = c.to_dict_with_id("commentId")

        self.send_json_response(Const.STATUS_OK, result)


    def getSubsForUser(self):
        self.send_json_response(Const.STATUS_ERROR, "Not supported.")

    def getContentsForUser(self):
        self.send_json_response(Const.STATUS_ERROR, "Not supported.")

    def search(self):
        self.send_json_response(Const.STATUS_ERROR, "Not supported.")
