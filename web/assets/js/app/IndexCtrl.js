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
srsApp.controller('ProfileCtrl', ['$scope', '$http', '$location', '$routeParams', 'Shared',
    function ($scope, $http, $location, $routeParams, Shared) {
    C.log("ProfileCtrl ...");
    $scope.Shared = Shared;
    $scope.Reg = Shared.User;
    var isEdit = $routeParams["pid"] !== undefined;
    if (isEdit) {
        // Load profile
        $http.get('./User/getById.a?userId=' + $routeParams["pid"])
                .success(function (data) {
                    C.log("Loaded user profile: " + JSON.stringify(data));
                    $scope.Reg = data;
                });
    }
    $scope.saveProfile = function () {
        $scope.formErrors = [];
        var err = [];
        var f = $scope.Reg;
        var re = /^([\w-]+(?:\.[\w-]+)*)@((?:[\w-]+\.)*\w[\w-]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$/i;
        if (f.login === undefined || !re.test(f.login)) {
            err.push("Please provide a valid email as login.");
        }
        if (f.firstName === undefined || f.firstName.length < 2) {
            err.push("Please provide your first/given name.");
        }
        if (f.lastName === undefined || f.lastName.length < 2) {
            err.push("Please provide your last/family name.");
        }
//        if ($scope.Reg.userId === undefined || $scope.Reg.userId < 0) {
//            if (f.password === undefined || f.password.length < 4) {
//                err.push("Please provide a suitable password.");
//            }
//            if (f.password === undefined || f.password2 === undefined ||
//                    f.password !== f.password2) {
//                err.push("Passwords must match!");
//            }
//        } else if ($scope.Reg.changePassword) {
//            if (f.password === undefined || f.password.length < 4) {
//                err.push("Existing password is missing.");
//            }
//            if (f.password === undefined || f.password2 === undefined ||
//                    f.newPassword !== f.password2) {
//                err.push("New passwords must match!");
//            }
//            C.log("Reg >>>> " + JSON.stringify($scope.Reg));
//        }
        if (err.length > 0) {
            $scope.formErrors = err;
            return;
        }
        $http.post('./User/save.a', $scope.Reg)
                .success(function (data) {
                    C.log("Registeration result: " + JSON.stringify(data));
                    if (data.Status === "ERROR") {
                        $scope.setStatus(data.Message);
                    } else {
                        Shared.User = data;
                        Shared.init();
                        $scope.Reg = data;
                        $location.path("/Profile/" + data.userId).replace();
                        $scope.setStatus("Saved profile!");
                    }
                });
    };
}]);
/**
 * Login page controller.
 */
srsApp.controller('LoginCtrl',['$scope', '$http', '$location', '$q', 'Shared',
        function ($scope, $http, $location, $q, Shared) {
            $scope.LF = {};
            $scope.PR = {};
            $scope.Shared = Shared;
            C.log("LoginCtrl ...");
            $scope.requestPassword = function () {
                C.log('resetPassword called.');
                $http.get('./App/requestPassword.a?email=' + $scope.PR.email)
                        .success(function (data) {
                            C.log("resetPassword result: " + JSON.stringify(data));
                            $location.path("/No-exitent").replace(); // Hackish
                            $scope.setStatus(data.Message);
                        });
            };
            $scope.login = function () {
                $http.post('./App/loginJson.a', $scope.LF)
                        .success(function (data, status, headers, config) {
                            if (data.Status === "success") {
                                Shared.init();
                                $scope.LF = {};
                                $location.path("/No-exitent").replace(); // Hackish
                                // Clear it after login
                                Shared.auth.currentUser.get().disconnect();
                            } else {
                                $scope.setStatus(data.Message);
                            }
                        }).error(function (data, status, headers, config) {
                    $scope.setStatus(data.Message);
                });
            };

            $scope.requestPassword = function () {
                C.log('resetPassword called.');
                $http.get('./App/requestPassword.a?email=' + $scope.PR.email)
                        .success(function (data) {
                            C.log("resetPassword result: " + JSON.stringify(data));
                            $location.path("/No-exitent").replace(); // Hackish
                            $scope.setStatus(data.Message);
                        });
            };

            $scope.gapiAuth2SignIn = function () {
                Shared.auth.signIn({'scope': 'profile email'})
                        .then(function (gu) {
                            if (gu.isSignedIn()) {
                                C.log("successful login");
                                // Send to server
                                var a = gu.getBasicProfile();
                                C.log("Profile: " + JSON.stringify(a));
                                $scope.LF.auth2Code = gu.getAuthResponse().id_token;
                                $scope.LF.email = a.getEmail();
                                var nm = a.getName().split(" ");
                                $scope.LF.firstName = nm[0];
                                $scope.LF.lastName = nm[nm.length - 1];
                                $scope.login();
                            } else {
                                C.log("login failed");
                            }
                        });
                C.log("gapiAuth2SignIn");
            };
        }]);
/**
 * Controller for the root view. All other controllers are children of this
 * controller, and hence can access functions and data defined on this one.
 * We load commonly used entities such as countries list etc. here.
 */
srsApp.controller('IndexCtrl',['$scope', '$http', '$location', '$q', 'Shared',
        function ($scope, $http, $location, $q, Shared) {

            C.log("Init IndexCtrl...");
            $scope.RootModel = {Data: {}};
            $scope.Reg = {};
            $scope.Shared = Shared;

            $scope.setStatus = function (msg) {
                $scope.StatusMessage = msg;
                $("#statusmsg").fadeIn(400);
            };
            $scope.hideStatus = function () {
                $scope.StatusMessage = undefined;
                $("#statusmsg").fadeOut(400);
            };
            $scope.RootModel.getThumbnailUrl = function (u) {
                return C.thumbnailUrl(u);
            };
            $scope.RootModel.getVideoUrl = function (u) {
                return C.videoEmbedUrl(u);
            };

            $scope.RootModel.search = function () {
                var q = $scope.RootModel.q;
                if (q === undefined || q.length < 1) {
                    $scope.setStatus("Please supply some meaningful query.")
                    return;
                }
                $location.path("/Search/" + q);
            };
            $scope.logout = function () {
                $http.get('./App/logout.a')
                        .success(function (data) {
                            C.log("Logged out: " + JSON.stringify(data));
                            Shared.User = {};
                            $location.path("/No-exitent").replace(); // Hackish
                        });
            };
            $scope.signOut = function () {
                Shared.auth.currentUser.get().disconnect();
                //$scope.Auth2.signOut();
                C.log('Disconnected');
                $scope.logout();
            };

            // Emitted in HTTP interceptor in case of errors
            $scope.$on("Errors", function (event, args) {
                $scope.Errors = args;
                $scope.Invalid = args.length > 0;
                C.log("Got errors: " + JSON.stringify(args) + ". Invalid=" + $scope.Invalid);
                $scope.setStatus("Your request could not be processed. Error occurred.");
            });
            $scope.$on("ClearMessages", function (event, args) {
                $scope.Errors = [];
                $scope.Invalid = false;
                $scope.hideStatus();
            });

            Shared.init(); // Gets the session details if any
            C.log("IndexCtrl: Loaded.");

        }]);

/**
 * Search controller
 */
srsApp.controller('SearchCtrl',['$scope', '$http', '$routeParams',
        function ($scope, $http, $routeParams) {
            C.log("SearchCtrl: " + JSON.stringify($routeParams));
            $http.get('./App/search.a?q=' + $routeParams["q"])
                    .success(function (data) {
                        C.log("Search results: " + JSON.stringify(data));
                        $scope.RootModel.Data.Trails = data;
                    });

        }]);

/**
 * Home controller
 */
srsApp.controller('HomeCtrl',['$scope', '$http', '$routeParams','Shared',
        function ($scope, $http, $routeParams, Shared) {
            C.log("HomeCtrl: " + JSON.stringify($routeParams));
            $scope.pageNo = 1;
            $scope.loadData = function () {
                $http.get('./App/getDataForHomePage.a?pageNo=' + $scope.pageNo)
                        .success(function (data) {
                            $scope.hasNext = (data.TrailsCount / data.PageSize > $scope.pageNo);
                            $scope.hasPrev = ($scope.pageNo > 1);
                            $scope.RootModel.Data.Trails = [];
                            while (data.Trails.length > 0)
                                $scope.RootModel.Data.Trails.push(data.Trails.splice(0, 4));

                            C.log("Loaded RootModel.Data: =" + JSON.stringify($scope.RootModel.Data));
                        })
                        .error(function (error) {
                            var msg = "Error occurred: " + JSON.stringify(error);
                            C.log(msg);
                        });
            };

            $scope.loadData();

            $scope.nextPage = function () {
                $scope.pageNo += 1;
                $scope.loadData();
            };
            $scope.prevPage = function () {
                $scope.pageNo -= 1;
                $scope.loadData();
            };

        }]);
