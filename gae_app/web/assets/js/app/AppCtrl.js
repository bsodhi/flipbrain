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
 * Controller for the root view. All other controllers are children of this
 * controller, and hence can access functions and data defined on this one.
 * We load commonly used entities such as countries list etc. here.
 */
srsApp.controller('AppCtrl',['$scope', '$http', '$location', '$routeParams', 'Shared',
        function ($scope, $http, $location, $routeParams, Shared) {

            C.log("Init AppCtrl: $location=(" + JSON.stringify($location) +
                    ". $routeParams=(" + JSON.stringify($routeParams) + ")");
            $scope.Model = {
                Trails: [], Trail: {items: []},
                temp: {
                    showComment: [], editComment: [], Content: {}
                },
                TrailItem: {comments: []}, Contents: []
            };

            $scope.Model.getSubsForUser = function () {
                $http.get('/user/getSubsForUser.a?userId=' + Shared.User.userId)
                        .success(function (data) {
                            if (data.Status === "OK") {
                                Shared.Subscriptions = data.Message;
                            } else {
                                $scope.setStatus("Failed to load subscriptions.")
                            }
                        });
            };

            // Load commonly used entities and any initial data to be shown on home page
            $scope.Model.init = function () {
                C.log("Model.init().");
                var path = $location.path();
                C.log("path=" + path + ". $routeParams=" + JSON.stringify($routeParams));
                $scope.RootModel.modalStatus = undefined; // Clear status in any modal dialog

                if (path.indexOf("/Subs") === 0) { // Load subscriptions for user
                    $scope.Model.getSubsForUser();
                }
            };

            $scope.Model.init();

            C.log("AppCtrl: Loaded.");

        }]);

