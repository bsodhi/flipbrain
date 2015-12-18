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
                $scope.Model = {Trail: {assessments: []}, temp: {}, Contents: []};
                if ($routeParams["tid"] !== undefined) {
                    $http.get('./Trail/getTrailById.a?trailId=' + $routeParams["tid"])
                            .success(function (data) {
                                C.log("TrailDetailsCtrl: Fetched trail: " + JSON.stringify(data));
                                $scope.Model.Trail = data;
                            });
                }
                C.log("TrailDetailsCtrl: Loaded.");

                $scope.getContentsForUser = function (q) {
                    var url = './Trail/getContentsForUser.a?userId=' +
                            Shared.User.userId;
                    if (q !== undefined)
                        url += "&q=" + q;
                    return $http.get(url, {params: {}})
                            .then(function (response) {
                                C.log("Loaded contents for user: " + JSON.stringify(response.data));
                                $scope.Model.Contents = response.data;
                                return response.data;
                            });
                };
                $scope.lookupAssessments = function (q) {
                    return $http.get('./Portfolio/lookupAssessments.a?q=' + q, {params: {}})
                            .then(function (response) {
                                C.log("Loaded assessments for user: " + JSON.stringify(response.data));
                                return response.data;
                            });
                };
                $scope.toggleTALookup = function () {
                    $scope.Model.temp.showTALookup = !$scope.Model.temp.showTALookup;
                    $scope.Model.temp.selAssessment = undefined;
                };
                $scope.addAssessmentToTrail = function (item, model, label) {
                    item.trailId = $scope.Model.Trail.trailId;
                    C.log("addAssessmentToTrail(): added =" + JSON.stringify(item));
                    $scope.Model.Trail.assessments.push(item);
                    $scope.toggleTALookup();
                };
                $scope.saveTrail = function () {
                    C.log("saveTrail()...");
                    $http.post('./Trail/saveTrail.a', $scope.Model.Trail)
                            .success(function (data) {
                                C.log("Saved trail: " + JSON.stringify(data));
                                $scope.Model.Trail = data;
                                $scope.RootModel.modalStatus = "Saved the trail!";
                                $scope.setStatus("Saved the trail!");
                            });
                };

                $scope.deleteTrail = function () {
                    C.log("deleteTrail()...");
                    if (!confirm("Are you sure you want to delete?")) {
                        return;
                    }
                    $http.get('./Trail/deleteTrail.a?trailId=' + $scope.Model.Trail.trailId)
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

                $scope.addContentToTrail = function (item, model, label) {
                    var ti = {content: item};
                    C.log("addContentToTrail(): added =" + JSON.stringify(ti));
                    if (item.contentType === 'V')
                        $scope.Model.Trail.videos.push(ti);
                    else
                        $scope.Model.Trail.resources.push(ti);
                    $scope.Model.temp.selContent = undefined;
                };
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

                $http.get('./Trail/getTrailForView.a?trailId=' + $routeParams["tid"])
                        .success(function (data) {
                            C.log("TrailViewCtrl: Fetched trail: " + JSON.stringify(data));
                            $scope.Model.Trail = data;
                            $scope.loadTrailItem(data.videos[0]);
                            C.log("Subscriptions: " + JSON.stringify(Shared.Subscriptions));
                            $scope.Model.temp.subscribed = Shared.Subscriptions.some(
                                    function (elm) {
                                        return elm.trailId === data.trailId;
                                    });
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
                    return $http.post('./Trail/searchComments.a', $scope.Model.SI)
                            .success(function (data) {
                                C.log("Loaded comments: " + JSON.stringify(data));
                                $scope.Model.Comments = data;
                            });
                };
                $scope.subscribeTrail = function (tid) {
                    $http.get('./Trail/addSubs.a?tid=' + tid)
                            .success(function (data) {
                                Shared.Subscriptions.splice(0, Shared.Subscriptions.length, data);
                                C.log("Current subscriptions: " + JSON.stringify(Shared.Subscriptions));
                                $scope.setStatus("Added subscription!");
                                $scope.Model.temp.subscribed = true;
                            }).error(function (data, status, headers, config) {
                        $scope.setStatus(data.Message);
                    });
                };

                $scope.Model.getItemComments = function (iid) {
                    return $http.get('./Trail/getComments.a?iid=' + iid)
                            .success(function (data) {
                                C.log("Loaded comments: " + JSON.stringify(data));
                                $scope.Model.Comments = data;
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
                    C.log("w x h = " + $window.innerWidth + " x " + $window.innerHeight);
                    var vw = 81 * 10;//$window.innerWidth*10/12;
                    var vh = 9 * vw / 16;
                    var htm = "<iframe width='" + vw + "' height='" + vh + "' src='" +
                            $scope.RootModel.getVideoUrl(i.content.url) + "'" +
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
                    $scope.Model.temp.Comment = {level: 0};
                };

                $scope.removeComment = function (c) {
                    C.log("Removing comment id:" + JSON.stringify(c));
                    return $http.get('./Trail/deleteComment.a?cid=' +
                            c.commentId)
                            .success(function (data) {
                                C.log("Deleted comment: " + JSON.stringify(data));
                                if (data > 0)
                                    $scope.Model.getItemComments(c.trailItemId);
                                else
                                    $scope.setStatus("Could not remove comment!");
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
                    $http.post('./Trail/saveComment.a', $scope.Model.temp.Comment)
                            .success(function (data) {
                                C.log("Saved comment: " + JSON.stringify(data));
                                $scope.Model.getItemComments(iid);
                                $scope.setStatus("Saved comment!");
                                $('#postModal').modal('toggle');
                            });
                };

            }]);

