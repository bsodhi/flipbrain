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
import in.flipbrain.dto.AnswerOptionDto;
import in.flipbrain.dto.AssessmentDto;
import in.flipbrain.dto.AssessmentQuestionDto;
import in.flipbrain.dto.AssessmentSubmissionDto;
import in.flipbrain.dto.PortfolioDto;
import in.flipbrain.dto.QuestionDto;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.javamvc.core.annotations.Action;
import org.javamvc.core.annotations.Authorize;

/**
 *
 * @author Balwinder Sodhi
 */
public class Portfolio extends BaseController {

    @Authorize
    @Action
    public void getAssessmentsTakenByUser() throws IOException {
        List<AssessmentSubmissionDto> list = MyBatisDao.getInstance(getClientInfo()).
                getSubmissionsByUserId(getLoggedInUserId());
        sendAsJson(list);
    }

    @Authorize
    @Action
    public void get() throws IOException {
        PortfolioDto dto = new PortfolioDto();
        dto.questionBank = MyBatisDao.getInstance(getClientInfo()).
                getQuestionsByUser(getLoggedInUserId());
        dto.authoredAssessments = MyBatisDao.getInstance(getClientInfo()).
                getAssessmentsByUserId(getLoggedInUserId());
        HashMap<String, Object> param = new HashMap<String, Object>();
        param.put("userId", getLoggedInUserId());
        param.put("q", null);
        dto.content = MyBatisDao.getInstance(getClientInfo()).
                getContentsForUser(param);

        dto.trails = MyBatisDao.getInstance(getClientInfo()).
                getTrailsForUser(getLoggedInUserId());
        sendAsJson(dto);
    }

    @Authorize
    @Action
    public void getQuestion() throws IOException {
        String qid = getRequestParameter("qid");
        QuestionDto dto = MyBatisDao.getInstance(getClientInfo()).
                getQuestionById(Long.parseLong(qid), getLoggedInUserId());
        sendAsJson(dto);
    }

    @Authorize
    @Action
    public void lookupQuestions() throws IOException {
        String q = getRequestParameter("q");
        List<QuestionDto> list = MyBatisDao.getInstance(getClientInfo()).
                lookupQuestions(q, getLoggedInUserId());
        sendAsJson(list);
    }

    @Authorize
    @Action
    public void saveQuestion() throws IOException {
        if (isJsonRequest()) {
            QuestionDto ques = getJsonRequestAsObject(QuestionDto.class);
            ques.author = getLoggedInUserId();
            MyBatisDao.getInstance(getClientInfo()).saveQuestion(ques);
            sendAsJson(ques);
        } else {
            sendJsonErrorResponse(406, "Expected JSON request.");
        }
    }

    @Authorize
    @Action
    public void getAssessment() throws IOException {
        String aid = getRequestParameter("aid");
        AssessmentDto dto = MyBatisDao.getInstance(getClientInfo()).
                getAssessmentById(Long.parseLong(aid));
        sendAsJson(dto);
    }

    @Authorize
    @Action
    public void getAssessmentForTaking() throws IOException {
        String aid = getRequestParameter("id");
        AssessmentSubmissionDto dto = MyBatisDao.getInstance(getClientInfo()).
                getAssessmentForTaking(Long.parseLong(aid));
        if (dto != null) {
            dto.student = getLoggedInUserId();
            dto.startTime = new Date();
            dto.draft = true;
            MyBatisDao.getInstance(getClientInfo()).saveAssessmentSubmission(dto);
        }
        sendAsJson(dto);
    }

    @Authorize
    @Action
    public void getAssessmentSubmission() throws IOException {
        String sid = getRequestParameter("id");
        AssessmentSubmissionDto dto = MyBatisDao.getInstance(getClientInfo()).
                getAssessmentSubmission(Long.parseLong(sid),
                        getLoggedInUserId(), false);
        sendAsJson(dto);
    }

    @Authorize
    @Action
    public void lookupAssessments() throws IOException {
        String q = getRequestParameter("q");
        List<AssessmentDto> list = MyBatisDao.getInstance(getClientInfo()).
                lookupAssessments(getLoggedInUserId(), q);
        sendAsJson(list);
    }

    @Authorize
    @Action
    public void saveAssessment() throws IOException {
        if (isJsonRequest()) {
            AssessmentDto assess = getJsonRequestAsObject(AssessmentDto.class);
            assess.author = getLoggedInUserId();
            MyBatisDao.getInstance(getClientInfo()).saveAssessment(assess);
            sendAsJson(assess);
        } else {
            sendJsonErrorResponse(406, "Expected JSON request.");
        }
    }

    @Authorize
    @Action
    public void saveAssessmentResponse() throws IOException {
        if (isJsonRequest()) {
            AssessmentSubmissionDto assess = getJsonRequestAsObject(AssessmentSubmissionDto.class);
            assess.student = getLoggedInUserId();
            MyBatisDao.getInstance(getClientInfo()).saveAssessmentSubmission(assess);
            sendAsJson(assess);
        } else {
            sendJsonErrorResponse(406, "Expected JSON request.");
        }
    }

    @Authorize
    @Action
    public void getAssessmentResult() throws IOException {
        String sid = getRequestParameter("id");
        AssessmentSubmissionDto dto = MyBatisDao.getInstance(getClientInfo()).
                getAssessmentSubmission(Long.parseLong(sid),
                        getLoggedInUserId(), true);
        /*
         int[] score = getScore(dto);
         HashMap res = new HashMap();
         res.put("status", "partial");
         res.put("maxmarks", score[0]);
         res.put("score", score[1]);

         if (dto.closeTime.before(new Date())) {
         res.put("submission", dto);
         res.put("status", "full");
         }
         */
        sendAsJson(dto);
    }

    private int[] getScore(AssessmentSubmissionDto dto) {
        int correctPts = 0;
        int wrong = 0;
        int mm = 0;
        for (AssessmentQuestionDto q : dto.questions) {
            mm += q.points;
            for (AnswerOptionDto ao : q.answerOptions) {
                if (q.type.equals(Constants.QT_FTXT) && ao.marked) {
                    if (ao.response != null && ao.response.equalsIgnoreCase(ao.answer)) {
                        correctPts += q.points;
                    } else {
                        wrong += 1;
                    }
                } else if (ao.correct && ao.marked) {
                    correctPts += q.points;
                } else if (!ao.correct && ao.marked) {
                    wrong += 1;
                }
            }
        }
        int[] score = new int[2];
        score[0] = mm;
        score[1] = dto.wrongAnsPoints * wrong + correctPts;
        return score;
    }
}
