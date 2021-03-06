<%--
    Document   : index
    Created on : Feb 6, 2014, 2:12:36 PM
    Author     : theuser
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html lang="en" ng-app="srsApp">
    <head>
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="description" content="Provides collection of online learning trails. Opportunities to discuss topics with fellow learners. Assessment quizzes.">
        <meta name="keywords" content="flipbrain, online learning, learning trails, topic discussions, community of learners, assessment quizzes">
        <link rel="icon" href="./assets/img/favicon.ico">

        <title>flipBRAIN :: Learning Trails from popular open content on diverse topics.</title>

        <!-- Bootstrap core CSS -->
        <link href="./assets/css/bootstrap.min.css" rel="stylesheet">
        <link rel="stylesheet" href="./assets/css/font-awesome.min.css">
        <link href="./assets/css/textAngular.css" rel="stylesheet">
        <!-- Custom styles for this template -->
        <link href="./assets/css/overrides.css" rel="stylesheet">

        <!-- Just for debugging purposes. Don't actually copy these 2 lines! -->
        <!--[if lt IE 9]><script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->
        <script src="./assets/js/ie-emulation-modes-warning.js"></script>

        <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
        <!--[if lt IE 9]>
          <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
          <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
        <![endif]-->
    </head>

    <body ng-controller="IndexCtrl">
        <div id="rootView" ng-view class="container-fluid"></div>
        <footer>
            <p class="center2">&copy; FlipBrain 2015</p>
        </footer>
        <!-- Bootstrap core JavaScript
        ================================================== -->
        <!-- Placed at the end of the document so the pages load faster -->
<!--        <script src="https://apis.google.com/js/platform.js" async defer></script>-->
        <script src="./assets/js/lib/jquery.min.js"></script>
        <script src="./assets/js/lib/angular.min.js"></script>
        <script src="./assets/js/lib/angular-route.min.js"></script>
        <script src="./assets/js/lib/angular-animate.min.js"></script>
        <script src="./assets/js/lib/bootstrap.min.js"></script>
        <script src="./assets/js/lib/ui-bootstrap-tpls-0.12.1.js"></script>
        <script src="./assets/js/lib/html.sortable.min.js"></script>
        <script src="./assets/js/lib/html.sortable.angular.min.js"></script>
        <!-- Rich textarea -->
        <script src="./assets/js/lib/textAngular-rangy.min.js"></script>
        <script src="./assets/js/lib/textAngular-sanitize.min.js"></script>
        <script src="./assets/js/lib/textAngular.min.js"></script>
        <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
        <script src="./assets/js/lib/ie10-viewport-bug-workaround.js"></script>
        <script src="https://apis.google.com/js/platform.js"></script>
        <!-- App controller scripts -->
        <script src="./assets/js/lib/app.min.js"></script>
        <script>
            (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
            (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
            })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

            ga('create', 'UA-63986539-1', 'auto');
            ga('send', 'pageview');

        </script>
    </body>
</html>
