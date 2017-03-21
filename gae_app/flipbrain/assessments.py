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

from common import *
from entities import *
import logging

class AssessmentHandler(BaseHandler):

    def getAssessmentsTakenByUser(self):
        ua_list = AssessmentSubmissionDto.query(AssessmentSubmissionDto.submittedBy
                                                == self.get_current_user_key()).fetch()
        data = [{"submissionId": x.key.id(), "submittedOn": x.submittedOn,
                 "draft": x.draft} for x in ua_list]
        self.send_json_response(Const.STATUS_OK, data)

    def getPortfolio(self):
        a = [p.to_dict_with_id("assessId") for p in
             AssessmentDto.query(AssessmentDto.owner == self.get_current_user_key()).fetch()]
        q = [p.to_dict_with_id("questionId") for p in
             QuestionDto.query(QuestionDto.owner == self.get_current_user_key()).fetch()]
        t = [p.to_dict_with_id("trailId") for p in
             TrailDto.query(TrailDto.owner == self.get_current_user_key()).fetch()]
        
        p = dict()
        p['trails'] = t
        p['authoredAssessments'] = a
        p['questionBank'] = q
        p['content'] = []  # TODO Remove
        
        
        self.send_json_response(Const.STATUS_OK, p)

    def getQuestion(self):
        q_id = self.request.params["qid"]
        q = QuestionDto.get_by_id(long(q_id))
        if q:
            self.send_json_response(Const.STATUS_OK, q.to_dict_with_id("questionId"))
        else:
            self.send_json_response(Const.STATUS_ERROR, "Could not find the requested information.")
    
    def saveQuestion(self):
        qf = json.loads(self.request.body)
        qid = qf.get("questionId")
        if qid:
            q = QuestionDto.get_by_id(int(qid))
            if q.owner == self.get_current_user_key():
                q.populate_from_dict(qf)
                q.put()
            else:
                raise ValueError("Cannot save entity not owned by this user.")
        else:
            q = QuestionDto(owner=self.get_current_user_key())
            q.populate_from_dict(qf)
            q.put()

        self.send_json_response(Const.STATUS_OK, q.to_dict_with_id("questionId"))
    
    def getAssessmentSubmission(self, sub_key=None):

        if sub_key:
            asub = sub_key.get()
            sid = asub.key.id()
        else:
            sid = self.request.params["id"]
            asub = AssessmentSubmissionDto.get_by_id(long(sid))
        if asub:
            ta = asub.traiAssessment.get()
        else:
            raise ValueError("Submission record not found.", sid)

        # Fetch the assessment
        a_dict = self._fetch_assessment(ta.assess.id(), True)

        # Mark the selected answers in assessment as per saved submission
        if a_dict:
            a_dict["submissionId"] = sid
            max_points = 0
            for q_dict in a_dict["questions"]:
                max_points += q_dict["points"]
                # One question may have one or more selected responses
                res_list = [x for x in asub.responses if x.questionId.id() == q_dict["questionId"]]
                for res in res_list:
                    # Expected only one match here
                    if q_dict['type'] != 'FTXT':
                        aopt_dict = [x_dict for x_dict in q_dict['answerOptions']
                                     if x_dict['answer'] == res.answer]
                        if aopt_dict: aopt_dict[0]["marked"] = True
                    else:
                        # Free text answers have a single answerOptions object
                        q_dict['answerOptions'][0]['response'] = res.answer

            # Include the score
            a_dict["score"] = str(asub.score)
            a_dict["maxPoints"] = max_points
            a_dict["draft"] = asub.draft
            self.send_json_response(Const.STATUS_OK, a_dict)
        else:
            self.send_json_response(Const.STATUS_ERROR, "Record not found!")
    
    def getAssessmentForTaking(self):

        if "id" not in self.request.params:
            self.send_json_response(Const.STATUS_ERROR, "Missing required params.")
            return

        a_id = self.request.params["id"]

        # First check for an existing in-progress submission
        ta = TrailAssessmentDto.query(TrailAssessmentDto.assess ==
                                      ndb.Key(AssessmentDto, long(a_id))).fetch(keys_only=True)
        if ta:
            sub_keys = AssessmentSubmissionDto.query(
                AssessmentSubmissionDto.traiAssessment == ta[0],
                AssessmentSubmissionDto.submittedBy == self.get_current_user_key()
                ).fetch(keys_only=True)
            # Found an existing submission
            if sub_keys:
                logging.info(">>>>>> Found existing submission. ID: %s", sub_keys)
                # self.redirect("/#/EditSubmission/%d" % sub_keys[0].id())
                self.getAssessmentSubmission(sub_keys[0])
            else:
                logging.info(">>>>>> Did not find any existing submission. ID: %s", ta)
                self.getAssessment(for_taking=True)
    
    def saveAssessmentResponse(self):
        ar_dict = self.load_json_request()
        # Fix the key properties
        for r in ar_dict["responses"]:
            r["questionId"] = ndb.Key(QuestionDto, long(r["questionId"]))

        if "submissionId" in ar_dict:
            sub = AssessmentSubmissionDto.get_by_id(long(ar_dict["submissionId"]))
        else:
            aid = ar_dict["assessId"]
            ta_list = TrailAssessmentDto.query().\
                filter(TrailAssessmentDto.assess ==
                       ndb.Key(AssessmentDto, long(aid))
                       ).fetch(keys_only=True)
            if not ta_list:
                raise ValueError("Trail assessment record not found for assessment ID %s" % aid)
            sub = AssessmentSubmissionDto(traiAssessment = ta_list[0])

        sub.populate_from_dict(ar_dict)
        sub.submittedBy = self.get_current_user_key()
        sub.put()

        self.send_json_response(Const.STATUS_OK, sub.to_dict_with_id("submissionId"))

    def getAssessmentResult(self):
        sid = self.request.params["id"]
        sub = AssessmentSubmissionDto.get_by_id(long(sid))
        if sub:
            # if not sub.draft:
            #     self.send_json_response(Const.STATUS_ERROR, "Already submitted!")
            #     return

            sub.draft = False
            # Calculate score
            ta = sub.traiAssessment.get()
            res_list = sub.responses
            asmt = ta.assess.get()

            aq_list = AssessmentQuestionDto.query(
                AssessmentQuestionDto.assess == ta.assess).fetch()
            # Reset the score
            sub.score = 0
            for aq in aq_list:
                q = aq.assessQtn.get()
                # Expected correct answers list
                ca_list = [ao.answer for ao in q.answerOptions if ao.correct]
                # Submitted answers list
                qr_list = [r.answer for r in res_list if r.questionId == aq.assessQtn]
                if ca_list == qr_list:
                    sub.score += aq.points
                else:
                    sub.score += asmt.pointsForWrongAns

            # Persist in datastore
            sub_key = sub.put()

            self.getAssessmentSubmission(sub_key=sub_key)
        else:
            self.send_json_response(Const.STATUS_ERROR, "Data not found.")

    def getAssessment(self, for_taking=False):
        aid = self.request.params["id"]
        a_dict = self._fetch_assessment(aid, for_taking)
        if a_dict:
            self.send_json_response(Const.STATUS_OK, a_dict)
        else:
            self.send_json_response(Const.STATUS_ERROR, "Could not find the requested information.")

    def _fetch_assessment(self, aid, for_taking):

        a = AssessmentDto.get_by_id(long(aid))
        if a:
            a_dict = a.to_dict_with_id("assessId")
            aq_list = AssessmentQuestionDto.query(
                AssessmentQuestionDto.assess == a.key).fetch()
            if aq_list:
                q_pts = {}
                keys = []
                for aq in aq_list:
                    q_pts[aq.assessQtn.id()] = aq.points
                    keys.append(ndb.Key(QuestionDto, aq.assessQtn.id()))
                q_list = ndb.get_multi(keys)
                qdict_list = [x.to_dict_with_id("questionId") for x in q_list]
                for q in qdict_list:
                    q["points"] = q_pts[q["questionId"]]
                a_dict["questions"] = qdict_list

            # Clear the correct flags on answers
            if for_taking:
                for qd in a_dict["questions"]:
                    for ao in qd['answerOptions']:
                        ao['correct'] = None
                        if qd['type'] == 'FTXT':
                            ao['answer'] = None

            return a_dict

    def lookupAssessments(self):
        # TODO: Minimize information to be sent
        qry = self.request.params["q"]
        a_list = AssessmentDto.query(AssessmentDto.owner == self.get_current_user_key()).fetch()
        f = [a.to_dict_with_id("assessId") for a in a_list if qry.lower() in a.title.lower()]
        self.send_json_response(Const.STATUS_OK, f)
    
    def saveAssessment(self):
        asmt = self.load_json_request()
        if "assessId" in asmt:
            a = AssessmentDto.get_by_id(int(asmt["assessId"]))
            logging.debug("Loaded assessment from DB.")
        else:
            a = AssessmentDto()
            logging.debug("Creating new assessment.")
        a.populate_from_dict(asmt)
        a.owner = self.get_current_user_key()
        a_key = a.put()

        aq_list = AssessmentQuestionDto.query(
            AssessmentQuestionDto.assess == a_key).fetch()

        if aq_list:
            ndb.delete_multi([x.key for x in aq_list])
            logging.debug("Cleared old AQs.")

        for aq in asmt["questions"]:
            q = AssessmentQuestionDto()
            q.assessQtn = ndb.Key(QuestionDto, aq["questionId"])
            q.assess = a_key
            q.points = aq["points"]
            q.put()

        a_dict = a.to_dict_with_id("assessId")
        a_dict["questions"] = asmt["questions"]
        self.send_json_response(Const.STATUS_OK, a_dict)


