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
 * @author Balwinder Sodhi
 */
public class AssessmentDto extends BaseDto {
    public long assessId;
    public boolean published;
    public String title;
    public String description;
    public long author;
    public int wrongAnsPoints;
    public int correctAnsPoints;
    public boolean randomizeQuestions;
    public boolean randomizeAnswers;
    public int allottedMinutes;
    public boolean showScore;
    public Date openTime;
    public Date closeTime;
    public Date startTime;
    public List<AssessmentQuestionDto> questions = new ArrayList<AssessmentQuestionDto>();
    public List<AssessmentSubmissionDto> submissions = new ArrayList<AssessmentSubmissionDto>();
}
