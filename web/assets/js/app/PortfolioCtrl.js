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
/**
 * Controller for the listing user's taken assessments.
 */
srsApp.controller('UserAssessCtrl', ['$scope', '$http', 'Shared',
    function ($scope, $http, Shared) {
        Shared.requireLogin();
        $scope.Model = {loaded:false, A: [], temp: {}};
        $scope.Sort = {pred1: "submittedOn", rev1: true};

        $http.get("./Portfolio/getAssessmentsTakenByUser.a")
                .success(function (data) {
                    $scope.Model.A = data;
                    C.log("Loaded user assessments");
                    $scope.Model.loaded=true;
                })
                .error(function (data, status, headers, config) {
                    $scope.setStatus(data.Message);
                    $scope.Model.loaded=true;
                });

        C.log("Loaded UserAssessCtrl...");
    }]);

/**
 * Controller for the managing portfolio related tasks.
 */
srsApp.controller('PortfolioCtrl', ['$scope', '$http', '$location', 'Shared',
    function ($scope, $http, $location, Shared) {
        Shared.requireLogin();
        $scope.Model = {loaded:false, PF: {}, temp: {YT: {}}, 
            Trail: {videos: [], resources: []}};
        $scope.Sort = {pred1: "submittedOn", rev1: true,
            pred2: "type", rev2: false, pred3: "openTime", rev3: false,
            pred4: "updTs", rev4: true, pred5: "updTs", rev5: true};

        C.log("Loaded PortfolioCtrl...");
        
        $http.get("./Portfolio/get.a")
                .success(function (data) {
                    $scope.Model.PF = data;
                    C.log("Loaded portfolio");
                    $scope.Model.loaded=true;
                })
                .error(function (data, status, headers, config) {
                    $scope.setStatus(data.Message);
                    $scope.Model.loaded=true;
                });

        $scope.addContent = function () {
            $scope.Model.PF.content.push($scope.Model.temp.Content);
            $scope.saveContent($scope.Model.temp.Content, true);
            $scope.RootModel.modalStatus = "Saved the content!";
            $scope.Model.temp = {};
        };

        $scope.addYTContent = function () {
            var c = $scope.Model.temp.YT;
            if (c.url === undefined || c.url.length < 10) {
                $scope.RootModel.modalStatus = "Please provide valid URL(s)!";
                return;
            }
            $http.post('./Trail/addYTContent.a', c)
                    .success(function (data) {
                        C.log("Saved content: " + JSON.stringify(data));
                        $scope.Model.PF.content.push(data);
                        $scope.RootModel.modalStatus = "Saved the content!";
                        $scope.Model.temp = {};
                    });
        };

        $scope.addYTrail = function () {
            var c = $scope.Model.temp.YT;
            if (c.resource === undefined || c.resource.length < 10) {
                $scope.RootModel.modalStatus = "Please provide valid playlist/videos!";
                return;
            }
            $http.get('./Trail/addYTrail.a', {params:c})
                    .success(function (data) {
                        C.log("Created trail: " + JSON.stringify(data));
                        $('#ytrailModal').modal('hide');
                        if ($scope.Model.PF.trails === undefined)
                            $scope.Model.PF.trails=[];
                        $scope.Model.PF.trails.push(data);
                    });
        };

        $scope.saveContent = function (c, fromModal) {
            $http.post('./Trail/saveContent.a', c)
                    .success(function (data) {
                        C.log("Saved content: " + JSON.stringify(data));
                        //$scope.Model.PF.content[ind] = data;
                        if (!fromModal)
                            $scope.setStatus("Saved content!");
                    });
        };

        $scope.removeContent = function (c) {
            $http.get('./Trail/deleteContent.a?cid='+c.contentId)
                    .success(function (data) {
                        C.log("Deleted content: " + JSON.stringify(data));
                        var ind = $scope.Model.PF.content.indexOf(c);
                        $scope.Model.PF.content.splice(ind,1);
                        $scope.setStatus("Content deleted!");
                    });
        };

        $scope.addToTrail = function () {
            C.log("Adding to trail...");
            $scope.RootModel.modalStatus = undefined;
            var s = false;
            angular.forEach($scope.Model.PF.content,
                    function (value, index) {
                        if (value.isSelected) {
                            s = true;
                            var ti = {content: {contentId: value.contentId}};
                            if (value.contentType === 'V')
                                $scope.Model.Trail.videos.push(ti);
                            else
                                $scope.Model.Trail.resources.push(ti);
                        }
                    });
            if (!s)
                $scope.RootModel.modalStatus = "Please first select some items to add!";
        };

        $scope.saveTrail = function (fromModal) {
            C.log("ContentCtrl: saveTrail()...");
            $http.post('./Trail/saveTrail.a', $scope.Model.Trail)
                    .success(function (data) {
                        C.log("Saved trail: " + JSON.stringify(data));
                        $scope.Model.Trail = data;
                        if (fromModal)
                            $scope.RootModel.modalStatus = "Saved the trail!";
                        else
                            $scope.setStatus("Saved the trail!");
                    });
        };

    }]);

/**
 * Controller for the managing questions related tasks.
 */
srsApp.controller('QuestionCtrl', ['$scope', '$http', '$routeParams', 'Shared', '$location',
    function ($scope, $http, $routeParams, Shared, $location) {
        Shared.requireLogin();
        $scope.Q = {answerOptions: []};
        C.log("Loaded QuestionCtrl...");
        var qid = $routeParams["qid"];
        if (qid !== undefined) {
            $http.get("./Portfolio/getQuestion.a?qid=" + qid)
                    .success(function (data) {
                        $scope.Q = data;
                        C.log("Loaded question");
                    })
                    .error(function (data, status, headers, config) {
                        $scope.setStatus(data.Message);
                    });
        }

        $scope.addOpt = function () {
            $scope.Q.answerOptions.push({});
        };

        $scope.save = function () {
            $http.post("./Portfolio/saveQuestion.a", $scope.Q)
                    .success(function (data) {
                        $scope.Q = data;
                        $scope.setStatus("Saved question!");
                        $location.path("/Question/" + data.questionId);
                    })
                    .error(function (data, status, headers, config) {
                        $scope.setStatus(data.Message);
                    });
        };
    }]);
/**
 * Controller for the taking an assessment.
 */
srsApp.controller('AssessmentViewCtrl', ['$scope', '$http', '$routeParams',
    'Shared', '$location',
    function ($scope, $http, $routeParams, Shared, $location) {
        Shared.requireLogin();
        $scope.A = {questions: [], responses: []};
        var isEdit = $location.path().indexOf("/EditSubmission") === 0;
        var action = isEdit ? "getAssessmentSubmission.a" : "getAssessmentForTaking.a";
        $http.get("./Portfolio/" + action + "?id=" + $routeParams["aid"])
                .success(function (data) {
                    $scope.A = data;
                    C.log("Taking assessment...");
                })
                .error(function (data, status, headers, config) {
                    $scope.setStatus(data.Message);
                });

        $scope.isSubmitted = function () {
            return $scope.A.draft !== undefined && !$scope.A.draft;
        };

        $scope.getMarkedAns = function () {
            var marked = [];
            $scope.A.questions.forEach(
                    function (elem, ind, arr) {
                        var ans = elem.answerOptions.filter(
                                function (el, idx, ar) {
                                    return el.marked ||
                                            (elem.type === 'FTXT' && el.response !== undefined);
                                });
                        C.log("Marked: " + JSON.stringify(ans));
                        ans.forEach(function (e) {
                            var a = {};
                            a.answerOptId = e.answerOptId;
                            a.assessQuestId = elem.assessQuestId;
                            if (elem.type === 'FTXT')
                                a.response = e.response;
                            marked.push(a);
                        });
                    });
            return {"assessId": $scope.A.assessId, "responses": marked, "submissionId": $scope.A.submissionId};
        };
        $scope.save = function (isSubmit) {
            if (!confirm("Sure you want to " + (isSubmit ? "submit" : "save") + "?"))
                return;
            var m = $scope.getMarkedAns();
            m.draft = !isSubmit;
            C.log("Sending for save: " + JSON.stringify(m));
            $http.post("./Portfolio/saveAssessmentResponse.a", m)
                    .success(function (data) {
                        $scope.A.submissionId = data.submissionId;
                        //$scope.A.draft = data.draft;
                        if (isSubmit) {
                            $scope.setStatus("Submitted responses!");
                            $location.path("/AssessmentResult/" + data.submissionId);
                        } else {
                            $scope.setStatus("Saved responses as a draft!");
                            $location.path("/EditSubmission/" + data.submissionId);
                        }
                    })
                    .error(function (data, status, headers, config) {
                        $scope.setStatus(data.Message);
                    });
        };

    }]);

/**
 * Controller for the assessment results.
 */
srsApp.controller('AssessmentResultCtrl', ['$scope', '$http', '$routeParams', 'Shared',
    function ($scope, $http, $routeParams, Shared) {
        Shared.requireLogin();
        $scope.A = {questions: [], responses: []};
        C.log("Getting assessment result...");
        $http.get("./Portfolio/getAssessmentResult.a?id=" + $routeParams["sid"])
                .success(function (data) {
                    $scope.A = data;
                    /*
                     $scope.A = data.submission;
                     $scope.score = data.score;
                     $scope.maxmarks = data.maxmarks;
                     $scope.status = data.status;
                     */
                    C.log("Assessment result: " + JSON.stringify(data));
                })
                .error(function (data, status, headers, config) {
                    $scope.setStatus(data.Message);
                });

        $scope.showAns = function (q, opt) {
            C.log("q=" + JSON.stringify(q) + "\nopt=" + JSON.stringify(opt));
            if (q.type === 'FTXT') {
                return opt.answer === opt.response;
            } else {
                return opt.correct;
            }
        };

    }]);

/**
 * Controller for the managing assessment related tasks.
 */
srsApp.controller('AssessmentCtrl', ['$scope', '$http', '$routeParams', 'Shared',
    '$filter', '$location',
    function ($scope, $http, $routeParams, Shared, $filter, $location) {
        Shared.requireLogin();
        $scope.A = {questions: []};
        $scope.pred = "submittedOn"; /*For sorting*/
        $scope.reverse = false;

        C.log("Loaded AssessmentCtrl...");

        var aid = $routeParams["aid"];
        if (aid !== undefined) {
            $http.get("./Portfolio/getAssessment.a?aid=" + aid)
                    .success(function (data) {
                        $scope.A = data;
                        C.log("Loaded assessment");
                    })
                    .error(function (data, status, headers, config) {
                        $scope.setStatus(data.Message);
                    });
        }

        $scope.save = function () {
            var fmt = "MMM d,yyyy HH:mm:ss a";
            $scope.A.openTime = $filter('date')($scope.A.openTime, fmt);
            $scope.A.closeTime = $filter('date')($scope.A.closeTime, fmt);
            $http.post("./Portfolio/saveAssessment.a", $scope.A)
                    .success(function (data) {
                        $scope.A = data;
                        $scope.setStatus("Saved assessment!");
                        $location.path("/Assessment/" + data.assessId);
                    })
                    .error(function (data, status, headers, config) {
                        $scope.setStatus(data.Message);
                    });
        };
        $scope.open = function ($event, dt) {
            $event.preventDefault();
            $event.stopPropagation();
            if (dt === 'OD')
                $scope.odOpen = true;
            else if (dt === 'CD')
                $scope.cdOpen = true;
        };

        $scope.toggleLookup = function () {
            $scope.showLookup = !$scope.showLookup;
            $scope.lookupQuery = undefined;
        };

        $scope.setQuestion = function (item, model, label) {
            $scope.A.questions.push(item);
            $scope.toggleLookup();
        };

        $scope.lookupQuestion = function (qStr) {
            C.log("lookupQuestion...");
            return $http.get("./Portfolio/lookupQuestions.a?q=" + qStr,
                    {params: {}})
                    .then(function (response) {
                        return response.data;
                    });
        };
    }]);
