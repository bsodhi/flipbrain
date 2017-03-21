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

        $http.get("/assess/getAssessmentsTakenByUser.a")
                .success(function (data) {
                    if (data.Status === "OK") {
                        $scope.Model.A = data.Message;
                        $scope.Model.loaded=true;
                    } else {
                        $scope.setStatus("Failed to load assessments.");
                    }
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
        
        $http.get("/assess/getPortfolio.a")
                .success(function (data) {
                    if (data.Status === "OK") {
                        $scope.Model.PF = data.Message;
                        $scope.Model.loaded = true;
                    } else {
                        $scope.setStatus("Failed to load portfolio.");
                    }
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
            $http.post('/trail/addYTContent.a', c)
                    .success(function (data) {
                        if (data.Status === "OK") {
                            $scope.Model.PF.content.push(data.Message);
                            $scope.RootModel.modalStatus = "Saved the content!";
                            $scope.Model.temp = {};
                        } else {
                            $scope.setStatus("Failed to add content.");
                        }
                    });
        };

        $scope.addYTrail = function () {
            var c = $scope.Model.temp.YT;
            if (c.resource === undefined || c.resource.length < 10) {
                $scope.RootModel.modalStatus = "Please provide valid playlist/videos!";
                return;
            }
            $http.get('/trail/addYTrail.a', {params:c})
                    .success(function (data) {
                        if (data.Status === "OK") {
                            $('#ytrailModal').modal('hide');
                            if ($scope.Model.PF.trails === undefined){
                                $scope.Model.PF.trails=[];
                            }
                            $scope.Model.PF.trails.push(data.Message);
                        } else {
                            $scope.setStatus("Failed to add trail.");
                        }
                    });
        };

        $scope.saveContent = function (c, fromModal) {
            $http.post('/trail/saveContent.a', c)
                    .success(function (data) {
                        if (data.Status === "OK") {
                            //$scope.Model.PF.content[ind] = data;
                            if (!fromModal)
                                $scope.setStatus("Saved content!");
                        } else {
                            if (!fromModal)
                                $scope.setStatus("Failed to saved content.");
                        }
                    });
        };

        $scope.removeContent = function (c) {
            $http.get('/trail/deleteContent.a?cid='+c.contentId)
                    .success(function (data) {
                        if (data.Status === "OK") {
                            var ind = $scope.Model.PF.content.indexOf(c);
                            $scope.Model.PF.content.splice(ind, 1);
                            $scope.setStatus("Content deleted!");
                        } else {
                            $scope.setStatus("Failed to delete content.");
                        }
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
            $http.post('/trail/saveTrail.a', $scope.Model.Trail)
                    .success(function (data) {
                        var ok = data.Status === "OK";
                        var msg = "Saved the trail!";
                        if (ok) {
                            $scope.Model.Trail = data.Message;
                        } else {
                            msg = "Failed to save the trail!";
                        }
                        if (fromModal)
                            $scope.RootModel.modalStatus = msg;
                        else
                            $scope.setStatus(msg);
                    });
        };

        $scope.createAssessFromQ = function(){
            var list = [];
            angular.forEach($scope.Model.PF.questionBank, function(q){
                qq = {"questionId":q.questionId, "title":q.title, "description":q.description};
                if (!!q.selected) list.push(qq);
              });
            Shared.aqList = list;
            $location.path("/Assessment");
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
            $http.get("/assess/getQuestion.a?qid=" + qid)
                    .success(function (data) {
                        if (data.Status === "OK") {
                            $scope.Q = data.Message;
                        } else {
                            $scope.setStatus("Failed to load question.");
                        }
                    })
                    .error(function (data, status, headers, config) {
                        $scope.setStatus(data.Message);
                    });
        }

        $scope.addOpt = function () {
            $scope.Q.answerOptions.push({});
        };

        $scope.save = function () {
            $http.post("/assess/saveQuestion.a", $scope.Q)
                    .success(function (data) {
                        if (data.Status === "OK") {
                            $scope.Q = data.Message;
                            $scope.setStatus("Saved question!");
                            $location.path("/Question/" + data.Message.questionId);
                        } else {
                            $scope.setStatus("Failed to save question!");
                        }
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
        $http.get("/assess/" + action + "?id=" + $routeParams["aid"])
                .success(function (data) {
                    if (data.Status === "OK") {
                        $scope.A = data.Message;
                    }
                    else {
                        $scope.setStatus("Failed to process request.");
                    }
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
                            a.answer = e.answer;
                            a.questionId = elem.questionId;
                            if (elem.type === 'FTXT')
                                a.answer = e.response;
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
            $http.post("/assess/saveAssessmentResponse.a", m)
                    .success(function (data) {
                        if (data.Status === "OK") {
                            $scope.A.submissionId = data.Message.submissionId;
                            //$scope.A.draft = data.draft;
                            if (isSubmit) {
                                $scope.setStatus("Submitted responses!");
                                $location.path("/AssessmentResult/" + data.Message.submissionId);
                            } else {
                                $scope.setStatus("Saved responses as a draft!");
                                $location.path("/EditSubmission/" + data.Message.submissionId);
                            }
                        } else {
                            $scope.setStatus("Could not save data.");
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
        $http.get("/assess/getAssessmentResult.a?id=" + $routeParams["sid"])
                .success(function (data) {
                    if (data.Status === "OK") {
                        $scope.A = data.Message;
                        /*
                         $scope.A = data.submission;
                         $scope.score = data.score;
                         $scope.maxmarks = data.maxmarks;
                         $scope.status = data.status;
                         */
                    } else {
                        $scope.setStatus(data.Message);
                    }
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
            $http.get("/assess/getAssessment.a?id=" + aid)
                    .success(function (data) {
                        if (data.Status === "OK") {
                            $scope.A = data.Message;
                        } else {
                            $scope.setStatus("Failed to load assessment.");
                        }
                    })
                    .error(function (data, status, headers, config) {
                        $scope.setStatus(data.Message);
                    });
        } else if (!!Shared.aqList) {
            $scope.A.questions = Array.from(Shared.aqList);
            delete Shared.aqList;
        }

        $scope.save = function () {
            // var fmt = "MMM d,yyyy HH:mm:ss a";
            // $scope.A.openTime = $filter('date')($scope.A.openTime, fmt);
            // $scope.A.closeTime = $filter('date')($scope.A.closeTime, fmt);
            $http.post("/assess/saveAssessment.a", $scope.A)
                    .success(function (data) {
                        if (data.Status === "OK") {
                            $scope.A = data.Message;
                            $scope.setStatus("Saved assessment!");
                            $location.path("/Assessment/" + data.Message.assessId);
                        } else {
                            $scope.setStatus("Could not save assessment.");
                        }
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
            return $http.get("/assess/lookupQuestions.a?q=" + qStr,
                    {params: {}})
                    .then(function (response) {
                        if (response.data.Status === "OK") {
                            return response.data.Message;
                        } else {
                            $scope.setStatus("Lookup failed.");
                            return response.data.Message;
                        }
                    });
        };
    }]);
