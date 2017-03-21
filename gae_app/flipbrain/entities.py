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

from google.appengine.ext import ndb
from datetime import datetime as DT
import logging


class Settings(ndb.Model):
    """
    This class represents the settings to be used in the application.

    Setting are stored in datastore via developer console.
    """
    name = ndb.StringProperty(required=True)
    value = ndb.StringProperty()

    @staticmethod
    def get(name):
        val = Settings.query(Settings.name == name).get()
        if not val:
            raise Exception('Setting %s not found in the database. '
                            'Please create it via developer console.' % name)
        return val.value


class BaseDto(ndb.Model):
    """
    Base class for entity models.

    This class contains the common functions which are used by derived
    entity models.
    """
    def try_parse_datetime(self, dt_str):
        """
        Attempts to parse a date string using following formats:
        "%Y-%m-%dT%H:%M:%S.%fZ", "%Y-%m-%dT%H:%M:%S" and "%Y-%m-%d"
        in that order.

        :param dt_str: A date string to be parsed into a date object.
        :return: Date object or None
        """
        try:
            dt = DT.strptime(dt_str, "%Y-%m-%dT%H:%M:%S.%fZ")
        except ValueError:
            try:
                dt = DT.strptime(dt_str, "%Y-%m-%dT%H:%M:%S")
            except ValueError:
                dt = DT.strptime(dt_str, "%Y-%m-%d")
        return dt

    def populate_from_dict(self, src_dict, retain_dest=True):
        """
        Populates this model from supplied dictionary. Main difference w.r.t
        the inherited "populate" function is that we try to parse dates using
        "try_parse_datetime" for setting the DateTimeProperty members.

        :param src_dict: Dictionary from which to populate this model.
        :param retain_dest: Flag that indicates whether the destination key
        value should be retained in case "src_dict" does not have that key.
        :return: None
        """
        for k in self._properties.keys():
            if retain_dest and not src_dict.has_key(k):
                continue
            attr = getattr(type(self), k, None)
            logging.debug(">>>>>> Property = %s", str(attr))
            # Handle the ISO 8601 format date value
            val = src_dict.get(k)
            if isinstance(attr, ndb.DateTimeProperty) or isinstance(attr, ndb.DateProperty):
                # Example value: u'2017-03-20T18:30:00.000Z'
                logging.debug("Converting %s (= %s) to datetime.", k, val)
                if val:
                    setattr(self, k, self.try_parse_datetime(val))
                else:
                    setattr(self, k, val)
            elif isinstance(attr, ndb.Key):
                logging.debug(">>>> Found Key: %s", k)
            elif isinstance(attr, ndb.KeyProperty):
                logging.debug(">>>> Found KeyProperty: %s", k)
            else:
                setattr(self, k, val)

    def to_dict_with_id(self, id_prop_name=None, excl=None):
        """
        We simply delegate to "self.to_dict" and then add a new property by
        the name "id_prop_name" whose value is the entity's ID.

        :param id_prop_name: Key/name to be given to ID property in the
        dictionary.
        :param excl: List of properties to be excluded.
        :return: Dictionary containing the properties of this model.
        """
        result = self.to_dict(exclude=excl)
        if id_prop_name:
            result[id_prop_name] = self.key.id() #get the key as a string
        return result


class UserDto(BaseDto):
    """
    Represents the user profile.
    """
    email = ndb.StringProperty()
    external = ndb.BooleanProperty(default=False, indexed=False)
    failedLogins = ndb.IntegerProperty()
    firstName = ndb.StringProperty()
    lastLogin = ndb.DateTimeProperty()
    lastName = ndb.StringProperty()
    middleName = ndb.StringProperty()
    password = ndb.StringProperty(indexed=False)
    verified = ndb.BooleanProperty(indexed=False)


class ContentDto(BaseDto):
    """
    Represents the content for trail items. Content can be a video, audio,
    text, image or other file.
    """
    contentType = ndb.StringProperty(choices=('V', 'A', 'T', 'I', 'O'),indexed=False)
    description = ndb.StringProperty(indexed=False)
    isPrivate = ndb.BooleanProperty(default = False)
    tags = ndb.StringProperty()
    title = ndb.StringProperty(indexed=False)
    url = ndb.StringProperty(indexed=False)
    itemId = ndb.StringProperty()
    #owner = ndb.KeyProperty(UserDto)


class VideoDto(ContentDto):
    """
    Represents the content of type video.
    """
    contentType = 'V'


class CommentDto(BaseDto):
    """
    Replies to a comment should set the parent entity key
    E.g. c2 = CommentDto(parent=c1.key, OTHER_PROPERTIES)
    Fetch a thread like this:
    full_th = CommentDto.query(ancestor = c1.key).fetch()
    """
    owner = ndb.KeyProperty()
    author = ndb.StringProperty(indexed=False)
    comment = ndb.StringProperty(indexed=False)
    level = ndb.IntegerProperty(indexed=False)
    subject = ndb.StringProperty(indexed=False)
    tags = ndb.StringProperty(indexed=False)
    trailItemId = ndb.StringProperty(required=True)
    postedOn = ndb.DateTimeProperty(auto_now=True, indexed=False)
    

class AnalyticsDto(BaseDto):
    """
    Represents the analytics record.
    """
    action = ndb.StringProperty()
    actionTs = ndb.DateTimeProperty(auto_now_add=True)
    analyticsId = ndb.IntegerProperty()
    commentId = ndb.IntegerProperty()
    trailId = ndb.IntegerProperty()
    trailItemId = ndb.IntegerProperty()
    userId = ndb.IntegerProperty()


#[START Trail and its relations]
class TrailDto(BaseDto):
    """
    Represents the learning trail information.
    """
    owner = ndb.KeyProperty(UserDto, required=True)
    isPrivate = ndb.BooleanProperty(default=False)
    tags = ndb.StringProperty(indexed=False)
    title = ndb.StringProperty(required = True, indexed=False)
    videos = ndb.StructuredProperty(VideoDto, repeated = True)
    resources = ndb.StructuredProperty(ContentDto, repeated = True)

    def thumbnailUrl(self):
        if self.videos and len(self.videos) > 0:
            url = "https://i.ytimg.com/vi/%s/hqdefault.jpg" \
                  % self.videos[0].url.split("/")[-1]
            return url
        else:
            return None


class TrailSubscDto(BaseDto):
    """

    """
    trail = ndb.KeyProperty(TrailDto, required=True)
    subscriber = ndb.KeyProperty(UserDto, required=True)


class TrailViewsDto(BaseDto):
    """
    Tracks the views count for a trail.
    """
    trail = ndb.KeyProperty(TrailDto, required=True)
    views = ndb.IntegerProperty(required = True, default=0)

#[END Trail and its relations]

#[START Assessment related entities]
class AssessmentDto(BaseDto):
    """
    Represents an assessment.
    """
    allottedMinutes = ndb.IntegerProperty(indexed=False)
    owner = ndb.KeyProperty(UserDto)
    closeTime = ndb.StringProperty()
    description = ndb.StringProperty(indexed=False)
    openTime = ndb.StringProperty()
    pointsForCorrectAns = ndb.IntegerProperty(indexed=False)
    pointsForWrongAns = ndb.IntegerProperty(indexed=False)
    published = ndb.BooleanProperty()
    randomizeAnswers = ndb.BooleanProperty(indexed=False)
    randomizeQuestions = ndb.BooleanProperty(indexed=False)
    showScore = ndb.BooleanProperty(indexed=False)
    startTime = ndb.StringProperty()
    title = ndb.StringProperty(indexed=False)


class AnswerOptionDto(BaseDto):
    """
    Answer options.
    """
    answer = ndb.StringProperty(indexed=False)
    correct = ndb.BooleanProperty(indexed=False)
    feedback = ndb.StringProperty(indexed=False)


class QuestionDto(BaseDto):
    """
    Represents a question.
    """
    answerOptions = ndb.StructuredProperty(AnswerOptionDto,repeated=True)
    owner = ndb.KeyProperty(UserDto)
    feedback = ndb.StringProperty(indexed=False)
    question = ndb.StringProperty(indexed=False)
    title = ndb.StringProperty(indexed=False)
    type = ndb.StringProperty(choices=('MCMA', 'MCSA', 'FTXT'))


class AssessmentQuestionDto(BaseDto):
    """
    Questions in an assessment.
    """
    assess = ndb.KeyProperty(AssessmentDto)
    assessQtn = ndb.KeyProperty(QuestionDto)
    points = ndb.IntegerProperty(indexed=False)


class SubmissionResponseDto(BaseDto):
    """
    Response submitted by a user for an assessment that he/she took.
    """
    questionId = ndb.KeyProperty(QuestionDto, required = True)
    answer = ndb.StringProperty(indexed=False)


class TrailAssessmentDto(BaseDto):
    """
    Trail assessment record.
    """
    assess = ndb.KeyProperty(AssessmentDto, required = True)
    trail = ndb.KeyProperty(TrailDto, required = True)


class AssessmentSubmissionDto(BaseDto):
    """
    Submission for an assessment.
    """
    traiAssessment = ndb.KeyProperty(TrailAssessmentDto, required = True)
    draft = ndb.BooleanProperty()
    responses = ndb.StructuredProperty(SubmissionResponseDto, repeated=True)
    score = ndb.IntegerProperty(default=-9999, indexed=False)
    submittedBy = ndb.KeyProperty(UserDto, required = True)
    submittedOn = ndb.DateTimeProperty(auto_now=True)

#[END Assessment related entities]


class ClientInfo(BaseDto):
    appUser = ndb.StringProperty()
    clientIp = ndb.StringProperty()
