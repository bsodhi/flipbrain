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
// This file contains the code for application setup tasks of angular.
// Mainly we setup the client-side routing here and services dependencies etc.
//-----------------------------------------------------------------------------
/* Declare app level module and its dependencies on filters, and services if any */

var srsApp = angular.module('srsApp', ['ngRoute', 'ngAnimate',
    'srs.SharedServices', 'ui.bootstrap', 'htmlSortable', 'textAngular'])
        .config(['$routeProvider', '$locationProvider',
            function ($routeProvider, $locationProvider) {

                /* Add the routes for views */
                $routeProvider.when('/Index', {
                    /* Points to the server-side controller path */
                    templateUrl: '/web/partials/home.html'
                    /* Name of the angular controller. Commented because it
                     * should be either specified here or in the template.
                     */
                    /* controller: 'IndexCtrl' */
                });
                $routeProvider.when('/Why', {
                    templateUrl: '/web/partials/why.html'
                });
//                $routeProvider.when('/Login', {
//                    templateUrl: '/web/partials/login.html'
//                });
                $routeProvider.when('/Register', {
                    templateUrl: '/web/partials/register.html'
                });
                $routeProvider.when('/Search/:q', {
                    templateUrl: '/web/partials/search.html'
                });
                $routeProvider.when('/MyTrails', {
                    templateUrl: '/web/partials/trails.html'
                });
                $routeProvider.when('/MyAssessments', {
                    templateUrl: '/web/partials/myAssessments.html'
                });
                $routeProvider.when('/Subs', {
                    templateUrl: '/web/partials/subscriptions.html'
                });
                $routeProvider.when('/Trail/:tid', {
                    templateUrl: '/web/partials/trailDetails.html'
                });
                $routeProvider.when('/Trail', {
                    templateUrl: '/web/partials/trailDetails.html'
                });
                $routeProvider.when('/ViewTrail/:tid', {
                    templateUrl: '/web/partials/viewTrail.html'
                });
                $routeProvider.when('/MyContent', {
                    templateUrl: '/web/partials/portfolio.html'
                });
                $routeProvider.when('/Question', {
                    templateUrl: '/web/partials/question.html'
                });
                $routeProvider.when('/Question/:qid', {
                    templateUrl: '/web/partials/question.html'
                });
                $routeProvider.when('/Assessment', {
                    templateUrl: '/web/partials/assessment.html'
                });
                $routeProvider.when('/Assessment/:aid', {
                    templateUrl: '/web/partials/assessment.html'
                });
                $routeProvider.when('/TakeAssessment/:aid', {
                    templateUrl: '/web/partials/takeAssessment.html'
                });
                $routeProvider.when('/EditSubmission/:aid', {
                    templateUrl: '/web/partials/takeAssessment.html'
                });
                $routeProvider.when('/AssessmentResult/:sid', {
                    templateUrl: '/web/partials/assessmentResult.html'
                });
                $routeProvider.when('/Profile/:pid', {
                    templateUrl: '/web/partials/register.html'
                });
                $routeProvider.when('/Register', {
                    templateUrl: '/web/partials/register.html'
                });

                $routeProvider.when('/Login', {
                    templateUrl: '/web/partials/login.html'
                });
                $routeProvider.when('/Logout', {
                    templateUrl: '/web/partials/logout.html',
                    controller: 'DummyCtrl'
                });
                /* Default route */
                $routeProvider.otherwise({redirectTo: '/Index'});

                /* Specify HTML5 mode (using the History APIs) or HashBang syntax. */
                $locationProvider.html5Mode(false);
            }]);

srsApp.controller('DummyCtrl', ['$scope', function ($scope) {
    }]);

// Customize rich textarea toolbar
srsApp.config(['$provide', function ($provide) {
        // changing the classes of the icons
        $provide.decorator('taOptions', ['$delegate', function (taOptions) {
                taOptions.toolbar = [
                    ['p', 'pre', 'quote'],
                    ['bold', 'italics', 'underline', 'strikeThrough', 'ul', 'ol', 'clear'],
                    ['justifyLeft', 'justifyCenter', 'justifyRight', 'indent', 'outdent'],
                    ['html', 'insertImage', 'insertLink']
                ];
                return taOptions;
            }]);
    }]);
/**
 * Global namespace containing functions for common tasks.
 */
var C = {
    YT_URL_MAIN: "www.youtube.com",
    YT_URL_SHORT: "youtu.be",
    YT_URL_EMBED: "http://www.youtube-nocookie.com/embed/",
    YT_URL_THUMBNAIL: "http://img.youtube.com/vi/",
    parseUrl: function (url) {
        var l = document.createElement("a");
        l.href = url;
        return l;
    },
    getVideoID: function (url) {
        var v = C.parseUrl(url);
        var vid;
        if (v.hostname === C.YT_URL_MAIN && v.pathname === "/watch") {
            vid = v.search.split("=")[1];
        } else if (v.hostname === C.YT_URL_SHORT) {
            vid = v.pathname;
        }
        return vid.slice(vid.indexOf("/")+1);
    },
    thumbnailUrl: function (url) {
        return C.YT_URL_THUMBNAIL + C.getVideoID(url) + "/mqdefault.jpg";
    },
    videoEmbedUrl: function (url) {
        return C.YT_URL_EMBED + C.getVideoID(url);
    },
    log: function (msg) {
        console.log(msg);
    }
};
