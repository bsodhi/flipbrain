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
import in.flipbrain.Utils;
import in.flipbrain.dao.MyBatisDao;
import in.flipbrain.dto.AnalyticsDto;
import in.flipbrain.dto.CommentDto;
import in.flipbrain.dto.ContentDto;
import in.flipbrain.dto.TrailDto;
import in.flipbrain.dto.TrailItemDto;
import in.flipbrain.dto.TrailSubsDto;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.javamvc.core.annotations.Action;
import org.javamvc.core.annotations.Authorize;

/**
 *
 * @author Balwinder Sodhi
 */
public class Trail extends BaseController {

    @Authorize
    @Action
    public void addSubs() throws IOException {

        TrailSubsDto dto = new TrailSubsDto();
        dto.trailId = Integer.parseInt(getRequestParameter("tid"));
        dto.userId = getLoggedInUserId();
        MyBatisDao.getInstance(getClientInfo()).addTrailSubs(dto);
        List<TrailSubsDto> list = MyBatisDao.getInstance(getClientInfo()).
                getSubsForUser(dto.userId);
        sendAsJson(list);
        // Log analytics
        AnalyticsDto ad = new AnalyticsDto(Constants.ADD_SUBS, getLoggedInUserId());
        ad.trailId = dto.trailId;
        MyBatisDao.getInstance(getClientInfo()).saveAnalytics(ad);
    }

    @Authorize
    @Action
    public void removeSubs() throws IOException {
        TrailSubsDto c = new TrailSubsDto();
        c.userId = getLoggedInUserId();
        c.trailId = Long.parseLong(getRequestParameter("tid"));
        int rows = MyBatisDao.getInstance(getClientInfo()).removeTrailSubs(c);
        sendAsJson(rows);

        // Log analytics
        AnalyticsDto ad = new AnalyticsDto(Constants.DELETE_SUBS, getLoggedInUserId());
        ad.trailId = c.trailId;
        MyBatisDao.getInstance(getClientInfo()).saveAnalytics(ad);
    }

    @Authorize
    @Action
    public void saveTrail() throws IOException {

        if (isJsonRequest()) {
            long userId = getLoggedInUserId();
            TrailDto trail = getJsonRequestAsObject(TrailDto.class);
            trail.createdBy = userId;
            MyBatisDao.getInstance(getClientInfo()).saveTrail(trail);
            trail = MyBatisDao.getInstance(getClientInfo()).getUserTrailById(trail);
            sendAsJson(trail);
        } else {
            sendJsonErrorResponse(406, "Expected JSON request.");
        }
    }

    @Authorize
    @Action
    public void deleteTrail() throws IOException {

        String id = getRequestParameter("trailId");
        if (!StringUtils.isEmpty(id)) {
            long userId = getLoggedInUserId();
            int rows = MyBatisDao.getInstance(getClientInfo()).
                    deleteTrail(Long.parseLong(id), userId);
            if (rows > 0) {
                sendOKAsJson("Deleted the trail.");
            } else {
                sendErrorAsJson("Could not delete the trail.");
            }
        }
    }

    @Authorize
    @Action
    public void getSubsForUser() throws IOException {
        long userId = getLoggedInUserId();
        List<TrailSubsDto> list = MyBatisDao.getInstance(
                getClientInfo()).getSubsForUser(userId);
        sendAsJson(list);
    }

    @Authorize
    @Action
    public void getTrailById() throws IOException {
        long userId = getLoggedInUserId();
        int tid = Integer.parseInt(getRequestParameter("trailId"));
        TrailDto t = new TrailDto();
        t.trailId = tid;
        t.createdBy = userId;
        t = MyBatisDao.getInstance(getClientInfo()).getUserTrailById(t);
        sendAsJson(t);
    }

    @Action
    public void getTrailForView() throws IOException {
        int tid = Integer.parseInt(getRequestParameter("trailId"));
        TrailDto t = MyBatisDao.getInstance(getClientInfo()).getTrailForView(tid);
        sendAsJson(t);
        // Log analytics
        AnalyticsDto ad = new AnalyticsDto(Constants.VIEW_TRAIL, getLoggedInUserId());
        ad.trailId = t.trailId;
        MyBatisDao.getInstance(getClientInfo()).saveAnalytics(ad);
    }

    @Authorize
    @Action
    public void deleteComment() throws IOException {
        CommentDto c = new CommentDto();
        c.author = getLoggedInUserId();
        c.commentId = Long.parseLong(getRequestParameter("cid"));
        c.setDeleted(true);
        Integer rows = (Integer) MyBatisDao.getInstance(getClientInfo()).saveComment(c);
        sendAsJson(rows);
    }

    @Authorize
    @Action
    public void saveComment() throws IOException {
        CommentDto c = getJsonRequestAsObject(CommentDto.class);
        c.author = getLoggedInUserId();
        c = (CommentDto) MyBatisDao.getInstance(getClientInfo()).saveComment(c);
        sendAsJson(c);

        // Log analytics
        AnalyticsDto ad = new AnalyticsDto(Constants.POST_COMMENT, getLoggedInUserId());
        ad.commentId = c.commentId;
        ad.trailItemId = c.trailItemId;
        MyBatisDao.getInstance(getClientInfo()).saveAnalytics(ad);
    }

    @Action
    public void searchComments() throws IOException {
        if (isJsonRequest()) {
            TrailDto.Search s = getJsonRequestAsObject(TrailDto.Search.class);
            List<CommentDto> nodes = MyBatisDao.getInstance(getClientInfo()).searchComments(s);
            sendAsJson(arrangeCommentsAsTree(nodes));
        } else {
            sendJsonErrorResponse(406, "Expected JSON request.");
        }
    }

    @Action
    public void getComments() throws IOException {
        long tiid = Long.parseLong(getRequestParameter("iid"));
        List<CommentDto> nodes = MyBatisDao.getInstance(getClientInfo()).
                getCommentsForTrailItem(tiid);

        arrangeCommentsAsTree(nodes);
        sendAsJson(nodes);

        // Log analytics
        AnalyticsDto ad = new AnalyticsDto(Constants.VIEW_COMMENTS, getLoggedInUserId());
        ad.trailItemId = tiid;
        MyBatisDao.getInstance(getClientInfo()).saveAnalytics(ad);
    }

    private List<CommentDto> arrangeCommentsAsTree(List<CommentDto> nodes) {

        HashMap<Long, ArrayList<CommentDto>> tree
                = new HashMap<Long, ArrayList<CommentDto>>();
        ArrayList<CommentDto> roots = new ArrayList<CommentDto>();
        for (CommentDto n : nodes) {
            if (n.inReplyTo == null) {
                roots.add(n);
            } else {
                if (!tree.containsKey(n.inReplyTo)) {
                    tree.put(n.inReplyTo, new ArrayList<CommentDto>());
                }
                tree.get(n.inReplyTo).add(n);
            }
        }
        nodes.clear();

        for (CommentDto n : roots) {
            nodes.add(n);
            nodes.addAll(getChildren(n.commentId, tree));
        }

        return nodes;
    }

    private ArrayList<CommentDto> getChildren(Long root,
            HashMap<Long, ArrayList<CommentDto>> tree) {
        ArrayList<CommentDto> children = new ArrayList<CommentDto>();
        if (tree.containsKey(root)) {
            children.addAll(tree.get(root));
            for (CommentDto n : tree.get(root)) {
                children.addAll(getChildren(n.commentId, tree));
            }
        }
        return children;
    }

    // Content related actions
    @Authorize
    @Action
    public void getContentsForUser() throws IOException {
        long userId = getLoggedInUserId();
        HashMap<String, Object> param = new HashMap<String, Object>();
        param.put("userId", userId);
        param.put("q", getRequestParameter("q"));
        List<ContentDto> list = MyBatisDao.getInstance(getClientInfo()).getContentsForUser(param);
        sendAsJson(list);
    }

    @Authorize
    @Action
    public void saveContent() throws IOException {
        ContentDto c = getJsonRequestAsObject(ContentDto.class);
        c.userId = getLoggedInUserId();
        MyBatisDao.getInstance(getClientInfo()).saveContent(c);
        sendAsJson(c);
    }

    @Authorize
    @Action
    public void addYTContent() throws IOException {
        ContentDto c1 = getJsonRequestAsObject(ContentDto.class);
        c1.userId = getLoggedInUserId();
        String[] urls = c1.url.split("\\s");
        ArrayList<ContentDto> list = getYouTubeVideoInfo(urls, c1.tags);
        for (ContentDto c : list) {
            MyBatisDao.getInstance(getClientInfo()).saveContent(c);
        }
        sendAsJson(list);
    }

    @Authorize
    @Action
    public void addYTrail() throws IOException {
        String isPL = getRequestParameter("isPL");
        String res = getRequestParameter("resource");
        String title = getRequestParameter("title");
        String tags = getRequestParameter("tags");
        
        TrailDto trail = null;
        // Trail from individual video URLs
        if (!"true".equalsIgnoreCase(isPL)) {
            String[] urlList = res.split("\\s");
            ArrayList<ContentDto> items = getYouTubeVideoInfo(urlList, tags);
            trail = new TrailDto();
            if (items != null && items.size() > 0) {
                trail.title = title;
                trail.tags = tags;
                trail.createdBy = getLoggedInUserId();
                int seqNo = 0;
                for (ContentDto c : items) {
                    MyBatisDao.getInstance(getClientInfo()).saveContent(c);
                    TrailItemDto ti = new TrailItemDto();
                    ti.trailId = trail.trailId;
                    ti.content = c;
                    ti.seqNo = seqNo++;
                    trail.videos.add(ti);
                }
                MyBatisDao.getInstance(getClientInfo()).saveTrail(trail);
            }
        } else { // Trail from playlist
            trail = addTrailFromYouTubePlayList(res, title, tags);
        }
        sendAsJson(trail);
    }

    private ArrayList<ContentDto> getYouTubeVideoInfo(String[] urls, String tags) throws MalformedURLException {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("key", getConfigValue(Constants.CFG_YT_API_KEY));
        params.put("part", "snippet");
        params.put("fields", "items(id,snippet(title,description))");
        String uri = getConfigValue(Constants.CFG_YT_VIDEO_API);
        ArrayList<ContentDto> list = new ArrayList<ContentDto>();
        for (String url : urls) {
            params.put("id", Utils.getVideoId(url));
            String json = Utils.httpGet(uri, params);

            HashMap map = jsonToMap(json);
            Map item = (Map) ((ArrayList) map.get("items")).get(0);
            Map snippet = (Map) item.get("snippet");

            ContentDto c = new ContentDto();
            c.userId = getLoggedInUserId();
            c.tags = tags;
            c.contentType = "V";
            c.description = (String) snippet.get("description");
            c.title = (String) snippet.get("title");
            c.url = url;
            list.add(c);
        }
        return list;
    }

    private TrailDto addTrailFromYouTubePlayList(String playListId, String trailTitle,
            String trailTags) {

        String uri = getConfigValue(Constants.CFG_YT_PLAYLIST_ITEMS_API);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("key", getConfigValue(Constants.CFG_YT_API_KEY));
        params.put("playlistId", playListId);
        params.put("maxResults", "50"); // Max allowed by youtube
        params.put("part", "contentDetails,snippet");
        params.put("fields", "items(snippet(title,description,resourceId/videoId))");

        String json = Utils.httpGet(uri, params);
        HashMap map = jsonToMap(json);
        List<Map> items = ((List<Map>) map.get("items"));
        TrailDto trail = new TrailDto();
        if (items != null && items.size() > 0) {
            trail.title = trailTitle;
            trail.tags = trailTags;
            trail.createdBy = getLoggedInUserId();
        }
        int seqNo = 1;
        for (Map item : items) {
            Map snippet = (Map) item.get("snippet");
            ContentDto c = new ContentDto();
            c.userId = getLoggedInUserId();
            c.contentType = "V";
            c.description = (String) snippet.get("description");
            c.title = (String) snippet.get("title");
            Map resourceId = (Map) snippet.get("resourceId");
            c.url = "https://youtu.be/" + resourceId.get("videoId");

            MyBatisDao.getInstance(getClientInfo()).saveContent(c);
            TrailItemDto ti = new TrailItemDto();
            ti.trailId = trail.trailId;
            ti.content = c;
            ti.seqNo = seqNo++;
            trail.videos.add(ti);
        }
        MyBatisDao.getInstance(getClientInfo()).saveTrail(trail);
        return trail;
    }

    @Authorize
    @Action
    public void deleteContent() throws IOException {
        ContentDto c = new ContentDto();
        c.userId = getLoggedInUserId();
        c.contentId = Long.parseLong(getRequestParameter("cid"));
        int rows = MyBatisDao.getInstance(getClientInfo()).deleteContent(c);
        sendAsJson(rows);
    }

}
