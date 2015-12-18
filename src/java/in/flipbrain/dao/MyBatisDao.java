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
package in.flipbrain.dao;

import in.flipbrain.Constants;
import in.flipbrain.dto.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

/**
 *
 * @author Balwinder Sodhi
 */
public class MyBatisDao {

    private Logger log = Logger.getLogger(getClass().getName());
    private static SqlSessionFactory sqlSessionFactory;
    private ClientInfo clientInfo;

    protected enum Operation {

        Ins, Upd, Del
    }

    private void init() throws IOException {
        String resource = "in/flipbrain/dao/mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        /*
         Properties prop = new Properties();
         prop.put("db_url", "");
         prop.put("db_driver", "");
         prop.put("db_username", "");
         prop.put("db_password", "");
         */
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        log.info("Initialized the DAO");
    }

    private void logActivity(final SqlSession session) {
        session.insert("logActivity", getActivity());
    }

    private MyBatisDao() {
        try {
            init();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static MyBatisDao getInstance(ClientInfo clientInfo) {
        MyBatisDaoHolder.INSTANCE.clientInfo = clientInfo;
        return MyBatisDaoHolder.INSTANCE;
    }

    private static class MyBatisDaoHolder {

        private static final MyBatisDao INSTANCE = new MyBatisDao();
    }

    private HashMap<String, String> getActivity() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        String activity = st[4].getMethodName();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("activity", activity);
        map.put("appUser", clientInfo.getAppUser());
        map.put("clientIp", clientInfo.getClientIp());
        return map;
    }

    //////////////// DAO methods /////////////////////
    public AnalyticsDto saveAnalytics(AnalyticsDto dto) {
        saveEntity("insertAnalytics", dto, Operation.Ins);
        return dto;
    }

    public void saveUser(UserDto dto) {
        if (dto.userId > 0) {
            saveEntity("updateUser", dto, Operation.Upd);
        } else {
            saveEntity("insertUser", dto, Operation.Ins);
        }
    }

    public UserDto getUserByLogin(String login) {
        return getEntity("getUserByLogin", login);
    }

    public UserDto getUserByEmail(String email) {
        return getEntity("getUserByEmail", email);
    }

    public UserDto getUserById(long userId) {
        return getEntity("getUserById", userId);
    }

    public void addOnetimeAuth(UserDto.OnetimeAuth rr) {
        saveEntity("addOnetimeAuth", rr, Operation.Ins);
    }

    public boolean onetimeAuthExists(UserDto.OnetimeAuth rr) {
        return getEntity("onetimeAuthExists", rr);
    }

    public void clearOnetimeAuth(UserDto.OnetimeAuth rr) {
        saveEntity("clearOnetimeAuth", rr, Operation.Upd);
    }

    public void recordLoginAttempt(HashMap<String, Object> param) {
        saveEntity("recordLoginAttempt", param, Operation.Upd);
    }

    public int addTrailSubs(TrailSubsDto subs) {
        return saveEntity("insertTrailSubs", subs, Operation.Ins);
    }

    public int removeTrailSubs(TrailSubsDto subs) {
        return saveEntity("deleteTrailSubs", subs, Operation.Del);
    }

    public List<TrailDto> getTrailsForUser(long userId) {
        return getEntityList("getTrailsForUser", userId);
    }

    public int saveTrail(TrailDto trail) {
        int rows = 0;
        SqlSession session = sqlSessionFactory.openSession();
        try {
            if (trail.trailId < 1 && !trail.isDeleted()) {
                rows += session.insert("insertTrail", trail);
            } else if (trail.isDeleted()) {
                rows += session.delete("deleteTrail", trail.trailId);
            } else {
                rows += session.delete("updateTrail", trail);
            }
            ArrayList<TrailItemDto> items = new ArrayList<TrailItemDto>();
            items.addAll(trail.resources);
            items.addAll(trail.videos);
            int seq = 0;
            for (TrailItemDto t : items) {
                t.trailId = trail.trailId;
                if (t.itemId < 1 && !t.isDeleted()) {
                    t.seqNo = seq++;
                    rows += session.insert("insertTrailItem", t);
                } else if (t.isDeleted()) {
                    rows += session.delete("deleteTrailItem", t.itemId);
                } else {
                    rows += session.update("updateTrailItem", t);
                }
            }
            for (TrailAssessmentDto ta : trail.assessments) {
                ta.trailId = trail.trailId;
                if (ta.trailAssessId < 1 && !ta.isDeleted()) {
                    session.insert("insertTrailAssessment", ta);
                } else if (ta.isDeleted()) {
                    session.delete("deleteTrailAssessment", ta.trailAssessId);
                }
            }
            logActivity(session);
            session.commit();
        } finally {
            session.close();
        }
        return rows;
    }

    public int deleteTrail(Long trailId, Long userId) {
        int rows = 0;
        SqlSession session = sqlSessionFactory.openSession();
        try {
            HashMap<String, Object> param = new HashMap<String, Object>();
            param.put("trailId", trailId);
            param.put("createdBy", userId);
            rows = session.delete("deleteTrail", param);
            logActivity(session);
            session.commit();
        } finally {
            session.close();
        }
        return rows;
    }
    public TrailDto getUserTrailById(TrailDto t) {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            logActivity(session);
            TrailDto trail = session.selectOne("getUserTrailById", t);
            if (trail != null) {
                trail.videos = session.selectList("getVideosForTrail", t.trailId);
                trail.resources = session.selectList("getResourcesForTrail", t.trailId);
                trail.assessments = session.selectList("getAssessmentsForTrail", t.trailId);
            }
            return trail;
        } finally {
            session.close();
        }
    }

    public List<TrailSubsDto> getSubsForUser(long userId) {
        return getEntityList("getSubsForUser", userId);
    }

    public int getPublicTrailsCount() {
        return getEntity("getPublicTrailsCount", null);
    }

    public List<TrailDto> getPublicTrails(int pgNo, int pgSize) {
        HashMap<String, Integer> params = new HashMap<String, Integer>();
        params.put("offset", pgSize * pgNo);
        params.put("rows", pgSize);
        return getEntityList("getPublicTrails", params);
    }

    public TrailDto getTrailForView(int tid) {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            logActivity(session);
            TrailDto t = session.selectOne("getTrailById", tid);
            if (t != null) {
                t.videos = session.selectList("getVideosForTrail", tid);
                t.resources = session.selectList("getResourcesForTrail", tid);
                t.assessments = session.selectList("getAssessmentsForTrail", tid);
                //Get the stats
                t.viewsCount = session.selectOne("getTrailViewsCount", tid);
                t.commentsCount = session.selectOne("getTrailCommentsCount", tid);
                t.subsCount = session.selectOne("getTrailSubsCount", tid);
            }
            return t;
        } finally {
            session.close();
        }
    }

    public List<CommentDto> getCommentsForTrailItem(long trailItemId) {
        return getEntityList("getCommentsForTrailItem", trailItemId);
    }

    public List<CommentDto> searchComments(TrailDto.Search s) {
        return getEntityList("searchComments", s);
    }

    public Object saveComment(CommentDto c) {
        int rows = 0;
        boolean isInsert = c.commentId < 1;
        if (isInsert && !c.isDeleted()) {
            rows = saveEntity("insertComment", c, Operation.Ins);
        } else if (c.isDeleted()) {
            rows = saveEntity("deleteComment", c, Operation.Del);
        } else {
            rows = saveEntity("updateComment", c, Operation.Upd);
        }
        return !isInsert && c.isDeleted() ? rows : c;
    }

    public List<TrailDto> searchTrails(String query) {
        return getEntityList("searchTrails", query + "*");
    }

    // ============= Content related methods =============
    public List<ContentDto> getContentsForUser(HashMap<String, Object> param) {
        List<ContentDto> res = getEntityList("getContentsForUser", param);
        return res;
    }

    public void saveContent(ContentDto c) {
        if (c.contentId < 1) {
            saveEntity("insertContent", c, Operation.Ins);
        } else {
            saveEntity("updateContent", c, Operation.Upd);
        }
    }

    public int deleteContent(ContentDto c) {
        return saveEntity("deleteContent", c, Operation.Del);
    }

    public void saveAssessmentSubmission(AssessmentSubmissionDto dto) {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            boolean isNew = dto.submissionId < 1 && !dto.isDeleted();
            if (isNew) {
                session.insert("insertAssessmentSubmission", dto);
            } else if (dto.isDeleted()) {
                session.delete("deleteAssessmentSubmission", dto);
            } else {
                session.update("updateAssessmentSubmission", dto);
                session.delete("deleteResponsesForSubmission", dto.submissionId);
            }

            if (!dto.isDeleted()) {
                for (SubmissionResponseDto r : dto.responses) {
                    r.submissionId = dto.submissionId;
                    if (r.responseId < 1 && !r.isDeleted()) {
                        session.insert("insertSubmissionResponse", r);
                    }
                }
            }
            session.commit();
        } finally {
            session.close();
        }
    }

    public List<QuestionDto> getQuestionsByUser(long userId) {
        List<QuestionDto> list = getEntityList("getQuestionsByUser", userId);
        for (QuestionDto q : list) {
            q.answerOptions = getEntityList("getAnsOptsForQuestion", q.questionId);
        }
        return list;
    }

    public QuestionDto getQuestionById(long qid, long userId) {
        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("questionId", qid);
        params.put("author", userId);
        QuestionDto q = getEntity("getQuestionById", params);
        q.answerOptions = getEntityList("getAnsOptsForQuestion", qid);
        return q;
    }

    public List<QuestionDto> lookupQuestions(String qStr, long userId) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("query", qStr);
        params.put("author", userId);
        return getEntityList("lookupQuestions", params);
    }

    public void saveQuestion(QuestionDto dto) {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            if (dto.questionId < 1 && !dto.isDeleted()) {
                session.insert("insertQuestion", dto);
            } else if (dto.isDeleted()) {
                session.delete("deleteQuestion", dto);
            } else {
                session.update("updateQuestion", dto);
            }
            for (AnswerOptionDto opt : dto.answerOptions) {
                opt.questionId = dto.questionId;
                opt.author = dto.author;
                if (opt.answerOptId < 1 && !opt.isDeleted()) {
                    session.insert("insertAnswerOption", opt);
                } else if (opt.isDeleted()) {
                    session.delete("deleteAnswerOption", opt);
                } else {
                    session.update("updateAnswerOption", opt);
                }
            }
            session.commit();
        } finally {
            session.close();
        }
    }

    public void saveAnswerOption(AnswerOptionDto dto) {
        if (dto.answerOptId > 0) {
            saveEntity("updateAnswerOption", dto, Operation.Upd);
        } else {
            saveEntity("insertAnswerOption", dto, Operation.Ins);
        }
    }

    public AssessmentDto getAssessmentById(long aid) {
        AssessmentDto a = getEntity("getAssessmentById", aid);
        a.questions = getEntityList("getQuestionsForAssessment", aid);
        a.submissions = getEntityList("getSubmissionsForAssessment", aid);
        return a;
    }

    public AssessmentSubmissionDto getAssessmentForTaking(long aid) {
        AssessmentSubmissionDto a = getEntity("getAssessmentForTakingId", aid);
        if (a != null) {
            a.questions = getEntityList("getQuestionsForAssessment", aid);
            if (a.randomizeQuestions) {
                Collections.shuffle(a.questions);
            }
            for (AssessmentQuestionDto aq : a.questions) {
                aq.answerOptions = getEntityList("getAnsOptsForView", aq.questionId);
                if (a.randomizeAnswers) {
                    Collections.shuffle(aq.answerOptions);
                }
            }
        }
        return a;
    }

    public AssessmentSubmissionDto getAssessmentSubmission(long sid,
            long userId, boolean withAnswers) {
        HashMap<String, Object> p = new HashMap<String, Object>();
        p.put("submissionId", sid);
        p.put("student", userId);
        AssessmentSubmissionDto a = getEntity("getAssessmentSubmissionById", p);

        if (a != null) {
            a.questions = getEntityList("getQuestionsForAssessment", a.assessId);
            if (a.randomizeQuestions) {
                Collections.shuffle(a.questions);
            }
            List<SubmissionResponseDto> res = getEntityList("getResponsesForSubmission", sid);
            for (AssessmentQuestionDto aq : a.questions) {
                a.maxPoints += aq.points;
                /*
                 aq.answerOptions = getEntityList(
                 withAnswers ? "getAnsOptsForQuestion"
                 : "getAnsOptsForView", aq.questionId);
                 */
                aq.answerOptions = getEntityList("getAnsOptsForQuestion", aq.questionId);
                if (a.randomizeAnswers) {
                    Collections.shuffle(aq.answerOptions);
                }
                for (SubmissionResponseDto r : res) {
                    for (AnswerOptionDto opt : aq.answerOptions) {
                        if (r.answerOptId == opt.answerOptId) {
                            opt.marked = true;
                            opt.response = r.response;
                            // Calculate score
                            if ((aq.type.equalsIgnoreCase(Constants.QT_FTXT)
                                    && r.response != null
                                    && r.response.equals(opt.answer))
                                    || (!aq.type.equalsIgnoreCase(Constants.QT_FTXT)
                                    && opt.correct)) {
                                a.score += aq.points;
                            } else {
                                a.wrongCount += 1;
                            }
                            //break;
                        }
                        if (!withAnswers) {
                            opt.correct = false;
                        }
                    }
                }
            }

        }
        return a;
    }

    public List<AssessmentDto> getAssessmentsByUserId(long uid) {
        return getEntityList("getAssessmentsByUserId", uid);
    }

    public List<AssessmentDto> lookupAssessments(long uid, String qStr) {
        HashMap<String, Object> par = new HashMap<String, Object>();
        par.put("query", qStr);
        par.put("author", uid);
        return getEntityList("lookupAssessments", par);
    }

    public List<AssessmentSubmissionDto> getSubmissionsByUserId(long id) {
        return getEntityList("getSubmissionsByUserId", id);
    }

    public void saveAssessment(AssessmentDto dto) {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            if (dto.assessId < 1 && !dto.isDeleted()) {
                session.insert("insertAssessment", dto);
            } else if (dto.isDeleted()) {
                session.delete("deleteAssessment", dto);
            } else {
                session.update("updateAssessment", dto);
            }

            for (AssessmentQuestionDto q : dto.questions) {
                q.assessId = dto.assessId;
                if (q.assessQuestId < 1 && !q.isDeleted()) {
                    session.insert("insertAssessmentQuestion", q);
                } else if (q.isDeleted()) {
                    session.delete("deleteAssessmentQuestion", q);
                } else {
                    session.update("updateAssessmentQuestion", q);
                }
            }
            session.commit();
        } finally {
            session.close();
        }
    }

    public void saveAssessmentQuestion(AssessmentQuestionDto dto) {
        if (dto.assessQuestId > 0) {
            saveEntity("updateAssessmentQuestion", dto, Operation.Upd);
        } else {
            saveEntity("insertAssessmentQuestion", dto, Operation.Ins);
        }
    }

    protected <T> int saveEntity(String stmtId, T dto, Operation op) {
        int rows = 0;
        SqlSession session = sqlSessionFactory.openSession();
        try {
            logActivity(session);
            switch (op) {
                case Del:
                    rows = session.delete(stmtId, dto);
                    break;
                case Ins:
                    rows = session.insert(stmtId, dto);
                    break;
                case Upd:
                    rows = session.update(stmtId, dto);
                    break;
            }
            session.commit();
        } finally {
            session.close();
        }
        return rows;
    }

    protected <P, R> List<R> getEntityList(String stmtId, P dto) {
        SqlSession session = sqlSessionFactory.openSession();
        List<R> result;
        try {
            logActivity(session);
            result = session.selectList(stmtId, dto);
            session.commit();
        } finally {
            session.close();
        }
        return result;
    }

    protected <P, R> R getEntity(String stmtId, P dto) {
        SqlSession session = sqlSessionFactory.openSession();
        R result;
        try {
            logActivity(session);
            result = session.selectOne(stmtId, dto);
            session.commit();
        } finally {
            session.close();
        }
        return result;
    }
}
