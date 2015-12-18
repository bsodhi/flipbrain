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
import java.util.List;

/**
 *
 * @author auser
 */
public class TrailDto extends BaseDto {

    public long trailId;
    public String title;
    public boolean isPrivate;
    public long createdBy;
    public List<TrailItemDto> videos = new ArrayList<TrailItemDto>();
    public List<TrailItemDto> resources = new ArrayList<TrailItemDto>();
    public List<TrailAssessmentDto> assessments = new ArrayList<TrailAssessmentDto>();
    public String thumbnailUrl;
    public String tags;
    
    // Stats
    public int viewsCount;
    public int subsCount;
    public int commentsCount;
    
    public TrailDto() {
    }
    
    public static class Search {
        public long trailId;
        public long itemId;
        public String query;
        public boolean searchAll;
        public boolean notReplied;
        public Date postedAfter;
        public Date postedBefore;
    }
}
