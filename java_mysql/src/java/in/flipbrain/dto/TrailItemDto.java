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

/**
 *
 * @author auser
 */
public class TrailItemDto extends BaseDto {

    public long itemId;
    public long trailId;
    public ContentDto content;
    public int seqNo;
    public ArrayList<CommentDto> comments = new ArrayList<CommentDto>();

    public TrailItemDto() {
    }

}
