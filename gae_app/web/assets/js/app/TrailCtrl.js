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
/* Controller for trail details */
srsApp.controller('TrailDetailsCtrl',
        ['$scope', '$http', '$routeParams', 'Shared', '$location',
            function ($scope, $http, $routeParams, Shared, $location) {
                Shared.requireLogin();
                $scope.Model = {Trail: {assessments: []}, temp: {resource:{}}, Contents: []};
                if ($routeParams["tid"] !== undefined) {
                    $http.get('/trail/getTrailById.a?trailId=' + $routeParams["tid"])
                            .success(function (data) {
                                if (data.Status === "OK") {
                                    $scope.Model.Trail = data.Message;
                                } else {
                                    $scope.setStatus("Failed to get the Trail data.")
                                }
                            });
                }
                C.log("TrailDetailsCtrl: Loaded.");

                $scope.getContentsForUser = function (q) {
                    var url = '/trail/getContentsForUser.a?userId=' +
                            Shared.User.userId;
                    if (q !== undefined)
                        url += "&q=" + q;
                    return $http.get(url, {params: {}})
                            .then(function (response) {
                                if (response.data.Status === "OK") {
                                    $scope.Model.Contents = response.data.Message;
                                    return response.data.Message;
                                } else {
                                    $scope.setStatus("Failed to get content for user.")
                                    return response.data.Message;
                                }
                            });
                };
                $scope.lookupAssessments = function (q) {
                    return $http.get('/assess/lookupAssessments.a?q=' + q, {params: {}})
                            .then(function (response) {
                                if (response.data.Status !== "OK") {
                                    $scope.setStatus("Failed to load assessments for user.")
                                }
                                return response.data.Message;
                            });
                };
                $scope.toggleTALookup = function () {
                    $scope.Model.temp.showTALookup = !$scope.Model.temp.showTALookup;
                    $scope.Model.temp.selAssessment = undefined;
                };
                $scope.toggleRESLookup = function () {
                    $scope.Model.temp.showRESLookup = !$scope.Model.temp.showRESLookup;
                    $scope.Model.temp.resource = {};
                };
                $scope.addResourceToTrail = function () {
                    var iid = $scope.Model.Trail.trailId+"_"+
                        ($scope.Model.Trail.resources.length + 1);
                    $scope.Model.temp.resource.itemId = iid;
                    $scope.Model.Trail.resources.push($scope.Model.temp.resource);
                    $scope.toggleRESLookup();
                };
                $scope.addAssessmentToTrail = function (item, model, label) {
                    item.trailId = $scope.Model.Trail.trailId;
                    $scope.Model.Trail.assessments.push(item);
                    $scope.toggleTALookup();
                };
                $scope.deleteRes = function (ind) {
                    if (confirm("Confirm delete?")) {
                        $scope.Model.Trail.resources.splice(ind, 1);
                    }
                };
                $scope.saveTrail = function () {
                    C.log("saveTrail()...");
                    $http.post('/trail/saveTrail.a', $scope.Model.Trail)
                            .success(function (data) {
                                if (data.Status === "OK") {
                                    $scope.Model.Trail = data.Message;
                                    $scope.RootModel.modalStatus = "Saved the trail!";
                                    $scope.setStatus("Saved the trail!");
                                } else {
                                    $scope.setStatus("Failed to saved the trail!");
                                }
                            });
                };

                $scope.deleteTrail = function () {
                    C.log("deleteTrail()...");
                    if (!confirm("Are you sure you want to delete?")) {
                        return;
                    }
                    $http.get('/trail/deleteTrail.a?trailId=' + $scope.Model.Trail.trailId)
                            .success(function (data) {
                                C.log("Deleted trail: " + JSON.stringify(data));
                                if (data.Status === "OK") {
                                    $scope.setStatus("Deleted the trail!");
                                    $location.path("/MyContent");
                                } else {
                                    $scope.setStatus("Could not delete the trail!");
                                }
                            });
                };

                // $scope.addContentToTrail = function (item, model, label) {
                //     var ti = {content: item};
                //     C.log("addContentToTrail(): added =" + JSON.stringify(ti));
                //     if (item.contentType === 'V')
                //         $scope.Model.Trail.videos.push(ti);
                //     else
                //         $scope.Model.Trail.resources.push(ti);
                //     $scope.Model.temp.selContent = undefined;
                // };
                $scope.sortableOptions = {
                    items: 'tr',
                    placeholder: '<tr><td colspan="4">&nbsp;</td></tr>'
                };

                $scope.sortableCallback = function (startModel, destModel, start, end) {
                    var x = {startModel: startModel, destModel: destModel, start: start, end: end};
                    C.log("params=" + JSON.stringify(x));
                    var el = $scope.Model.Trail.videos.splice(start, 1);
                    $scope.Model.Trail.videos.splice(end, 0, el[0]);
                    angular.forEach($scope.Model.Trail.videos,
                            function (value, index) {
                                $scope.Model.Trail.videos[index].seqNo = index + 1;
                            });
                    $scope.$apply();
                };

            }]);

/* Controller for viewing a trail */
srsApp.controller('TrailViewCtrl',
        ['$scope', '$http', '$routeParams', '$filter', 'Shared', '$window',
            function ($scope, $http, $routeParams, $filter, Shared, $window) {
                $scope.Model = {Trails: [], Trail: {}, popoverMsg: undefined, SI: {},
                    temp: {showComment: [], editComment: []},
                    Contents: [], Comment: {}};

                $http.get('/trail/getTrailForView.a?trailId=' + $routeParams["tid"])
                        .success(function (data) {
                            if (data.Status === "OK") {
                                $scope.Model.Trail = data.Message;
                                $scope.loadTrailItem(data.Message.videos[0]);
                                $scope.Model.temp.subscribed = Shared.Subscriptions.some(
                                        function (elm) {
                                            return elm.trailId === data.Message.trailId;
                                        });
                            } else {
                                $scope.setStatus("Failed to get trail.")
                            }
                        });

                C.log("TrailViewCtrl: Loaded...");
                $scope.openCal = function ($event, dt) {
                    $event.preventDefault();
                    $event.stopPropagation();
                    if (dt === 'PA')
                        $scope.paOpen = true;
                    else if (dt === 'PB')
                        $scope.pbOpen = true;
                };
                $scope.searchComments = function () {
                    $scope.Model.SI.trailId = $scope.Model.Trail.trailId;
                    $scope.Model.SI.itemId = $scope.Model.TrailItem.itemId;
                    C.log("SI: " + JSON.stringify($scope.Model.SI));
                    var a = $scope.Model.SI.postedAfter;
                    var b = $scope.Model.SI.postedBefore;
                    if (a !== undefined)
                        $scope.Model.SI.postedAfter = $filter("date")(a, 'medium');
                    if (b !== undefined)
                        $scope.Model.SI.postedBefore = $filter("date")(b, 'medium');
                    return $http.post('/trail/searchComments.a', $scope.Model.SI)
                            .success(function (data) {
                                if (data.Status === "OK") {
                                    $scope.Model.Comments = data.Message;
                                } else {
                                    $scope.setStatus("Failed to search comments.")
                                }
                            });
                };
                $scope.subscribeTrail = function (tid) {
                    $http.get('/trail/addSubs.a?tid=' + tid)
                            .success(function (data) {
                                if (data.Status === "OK") {
                                    Shared.Subscriptions.splice(0,
                                        Shared.Subscriptions.length, data.Message);
                                    $scope.setStatus("Added subscription!");
                                    $scope.Model.temp.subscribed = true;
                                } else {
                                    $scope.setStatus("Failed to add subscription.");
                                }
                            }).error(function (data, status, headers, config) {
                        $scope.setStatus(data.Message);
                    });
                };

                $scope.Model.getItemComments = function (iid) {
                    return $http.get('/trail/getComments.a?iid=' + iid)
                            .success(function (data) {
                                if (data.Status === "OK") {
                                    $scope.Model.Comments = data.Message;
                                } else {
                                    $scope.setStatus("Failed to get comments.");
                                }
                            });
                };

                $scope.loadTrailItem = function (i) {
                    if (i === undefined) {
                        $scope.RootModel.StatusMessage = "Item not found!";
                        return;
                    }
                    C.log("Showing trailitem: " + JSON.stringify(i));
                    $("#videoLoadMsg").show();
                    $scope.Model.videoLoaded = false;
                    $scope.Model.TrailItem = i;
                    var vw = $window.innerWidth * 0.70;
                    var vh = $window.innerHeight * 0.75;
                    C.log("w x h = " + vw + " x " + vh);
                    // var vw = 81 * 10;//$window.innerWidth*10/12;
                    // var vh = 9 * vw / 16;
                    var htm = "<iframe width='" + vw + "' height='" + vh + "' src='" +
                            $scope.RootModel.getVideoUrl(i.url) + "'" +
                            "frameborder='0' allowfullscreen onload='$(\"#videoLoadMsg\").hide(\"slow\")'></iframe>";
                    $("#tivideo").html(htm);
                    $scope.Model.getItemComments(i.itemId);
                    $scope.Model.videoLoaded = true;
                };

                $scope.isItemActive = function (i) {
                    return $scope.Model.TrailItem === i ? "active" : false;
                };

                $scope.itemHasResources = function () {
                    var r = $scope.Model.Trail.resources;
                    return r !== undefined && r.length > 0;
                };
                $scope.editComment = function (c) {
                    if (!Shared.User.loggedIn)
                        return;
                    $scope.Model.temp.Comment = c;
                };
                $scope.replyToComment = function (c) {
                    if (!Shared.User.loggedIn)
                        return;
                    $scope.Model.temp.Comment = {};
                    $scope.Model.temp.Comment.inReplyTo = c.commentId;
                    $scope.Model.temp.Comment.level = c.level + 1;
                };
                $scope.newComment = function () {
                    if (!Shared.User.loggedIn)
                        return;
                    $scope.Model.temp.Comment = {level: 0,
                        trailItemId:$scope.Model.TrailItem.itemId};
                };

                $scope.removeComment = function (c) {
                    C.log("Removing comment id:" + JSON.stringify(c));
                    return $http.get('/trail/deleteComment.a?cid=' +
                            c.commentId)
                            .success(function (data) {
                                if (data.Status === "OK") {
                                    if (data.Message > 0)
                                        $scope.Model.getItemComments(c.trailItemId);
                                    else
                                        $scope.setStatus("Could not remove comment!");
                                } else {
                                    $scope.setStatus("Failed to remove comment!");
                                }
                            });
                };

                $scope.commentLevel = function (l) {
                    var x = [];
                    x.push("col-md-offset-" + l);
                    x.push("col-md-8");
                    return x;
                };
                $scope.addDiscussionPost = function () {
                    C.log("addDiscussionPost():: ");
                };
                $scope.Model.temp.cid = 0;
                $scope.postReply = function () {
                    if (!Shared.User.loggedIn) {
                        return;
                    }

                    var iid = $scope.Model.TrailItem.itemId;
                    $scope.Model.temp.Comment.trailItemId = iid;
                    if ($scope.Model.temp.Comment.commentId < 1)
                        $scope.Model.Comments.unshift($scope.Model.temp.Comment);
                    $http.post('/trail/saveComment.a', $scope.Model.temp.Comment)
                            .success(function (data) {
                                if (data.Status === "OK") {
                                    $scope.Model.getItemComments(iid);
                                    $scope.setStatus("Saved comment!");
                                } else {
                                    $scope.setStatus("Failed to save the comment!");
                                }
                                $('#postModal').modal('toggle');
                            });
                };

            }]);

