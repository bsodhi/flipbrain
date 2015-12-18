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
//-----------------------------------------------------------------------------
// This file contains the code for any angularjs services that we will create.
//-----------------------------------------------------------------------------
'use strict';

/* Declare app level module and its dependencies on other services if any. */
var sharedMod = angular.module('srs.SharedServices', []);
/* An HTTP interceptor is created as a service */
sharedMod.config(['$httpProvider', function ($httpProvider) {
        $httpProvider.interceptors.push(['$q', '$rootScope', 
            function ($q, $rootScope) {
            return {
                // optional method
                'request': function (config) {
                    $('#ajax-waiting').addClass('animate-text');
                    $('#ajax-loader').show();
                    return config;
                },
                // optional method
                'requestError': function (rejection) {
                    // do something on error
                    $('#ajax-waiting').removeClass('animate-text');
                    $('#ajax-loader').hide();
                    return $q.reject(rejection);
                },
                // optional method
                'response': function (response) {
                    // do something on success
                    $('#ajax-waiting').removeClass('animate-text');
                    $('#ajax-loader').hide();
                    $rootScope.$broadcast("ClearMessages", []);
                    return response;
                },
                // optional method
                'responseError': function (rejection) {
                    // do something on error
                    $('#ajax-waiting').removeClass('animate-text');
                    $('#ajax-loader').hide();
                    C.log("Error occurred: >>>>>> " + JSON.stringify(rejection));
                    // Tell the main controller about errors
                    //msg.push({ Message: "Error occurred. Status code " + response.status });
                    //$rootScope.$broadcast("Errors", rejection);
                    return $q.reject(rejection);
                }
            };
        }]);
    }]);
/* This service allows two controllers to share data. */
sharedMod.factory('Shared', ['$http', '$route',
    function ($http, $route) {
    var Shared = {User: {loggedIn: false}};
    Shared.loginTooltip = function (msg) {
        return !Shared.User.loggedIn ? msg : undefined;
    };
    Shared.requireLogin = function () {
        C.log("Skipping requireLogin()");
//        if (!this.User.loggedIn)
//            $location.path("/Index");
    };
    Shared.init = function () {
        C.log("Shared.init().");
        $http.get('./App/getSessionDetails.a')
                .success(function (data, status, headers, config) {
                    if (data.User !== undefined) {
                        Shared.User = data.User;
                        Shared.User.loggedIn = true;
                    }
                    if (data.Subscriptions !== undefined)
                        Shared.Subscriptions = data.Subscriptions;
                    else
                        Shared.Subscriptions = [];
                    $route.reload();
                    C.log("Shared.init(): Loaded User: " + JSON.stringify(data.User));
                })
                .error(function (data, status, headers, config) {
                    var msg = "Shared.init(): Error occurred: " + JSON.stringify(data);
                    C.log(msg);
                });
    };
    Shared.dateDiffFromNow = function (d) {
        var MS = 1000 * 3600 * 24;
        var now = new Date();
        var dd = new Date(d);
        var daysDiff = Math.ceil(now / MS) - Math.ceil(dd / MS);
        var absDD = Math.abs(daysDiff);
        return daysDiff > 0 ? absDD + " days ago." : "After " + absDD + " days.";
    };
    try {
        if (gapi !== undefined) {
            gapi.load('auth2', function () {
                Shared.auth = gapi.auth2.init({
                    client_id: '419854404423-9f472gsk77jc1p9jnp7umbnj17b2u3tq.apps.googleusercontent.com',
                    // Scopes to request in addition to 'profile' and 'email'
                    scope: 'email'
                });
                C.log("Initialized auth.");
            });
        } else {
            C.log("Could not initialize auth: gapi=" + gapi);
        }
    } catch (e) {
        console.error(e);
    }
    Shared.QTypes = {"MCMA": "Multiple choice multiple answer",
        "MCSA": "Multiple choice single answer", "FTXT": "Free form text"};
    return Shared;
}]);
sharedMod.directive('uppercase', function () {
    return {
        require: 'ngModel',
        link: function (scope, el, attr, ngModel) {
            var capitalize = function (inputVal) {
                if (inputVal === undefined)
                    return;
                var cap = inputVal.toUpperCase();
                if (inputVal !== cap) {
                    ngModel.$setViewValue(cap);
                    ngModel.$render();
                }
                return cap;
            };
            ngModel.$parsers.push(capitalize);
            capitalize(scope[attr.ngModel]);
        }
    };
});
sharedMod.directive('dateinput', ['$filter',function ($filter) {
    /*
     Directive for formatting the dates. Any textfield capturing a date should have
     the 'dateinput' attribute specified for formatting the date as dd/MM/yyyy.
     */
    return {
        require: '^ngModel',
        restrict: 'A',
        link: function (scope, elm, attrs, ctrl) {
            ctrl.$formatters.push(function (modelValue) {
                //Model -> View
                if (!modelValue)
                    return "";
                var retVal = $filter('date')(modelValue, 'dd/MM/yyyy');
                //C.log("Formatted date:"+retVal);
                return retVal;
            });
            ctrl.$parsers.push(function (modelValue) {
                //View -> Model
                //return data
                var d = $filter('date')(modelValue, "dd/MM/yyyy");
                //C.log("Parsed date:"+d);
                return d;
            });
        }
    };
}]);
sharedMod.directive('currencyInput', ['$filter', function ($filter) {
    // Directive to format the currency in text box
    return {
        require: '^ngModel',
        restrict: 'A',
        link: function (scope, elm, attrs, ctrl) {
            ctrl.$formatters.unshift(function (modelValue) {
                if (!modelValue)
                    return "";
                var retVal = $filter('currency')(modelValue);
                return retVal;
            });
        }
    };
}]);
sharedMod.directive("compareTo", function () {
    /* Credit: http://goo.gl/4TCtwW (odetocode.com blogs)*/
    return {
        require: "ngModel",
        scope: {
                otherModelValue: "=compareTo"
        },
        link: function (scope, element, attributes, ngModel) {
             
                ngModel.$validators.compareTo = function (modelValue) {
                        return modelValue === scope.otherModelValue;
                };
 
                scope.$watch("otherModelValue", function () {
                        ngModel.$validate();
                });
        }
    };
});
