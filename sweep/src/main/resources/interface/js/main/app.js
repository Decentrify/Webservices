'use strict';

angular.module('app', [
    'ngRoute',
    'nvd3',
    'ui.bootstrap'
    ])
    .config(['$routeProvider', function($routeProvider){

        $routeProvider

            .when('/',
            {
                templateUrl: 'partials/main/landing-page.html',
                controller: 'LandingController'
            })
            .when('/search',
            {
                templateUrl: 'partials/search/result.html',
                controller: 'SearchResultController'
            })
            .when('/upload-landing',
            {
                templateUrl: 'partials/uploader/upload-landing.html',
                controller: 'UploadController'
            })
            .when('/upload-main',
            {
                templateUrl: 'partials/uploader/upload-main.html',
                controller: 'EntryUploadController'
            })
            .when('/statistics',{
                templateUrl: 'partials/statistics/statistics.html',
                controller: 'StatisticsController'
            })
            .when('/video',{
                templateUrl: 'partials/video/video-js.html',
                controller: 'VideoController'
            })
            .when('/about',{
                templateUrl: 'partials/others/about-us.html'
            })
            .when('/search/:searchText',{
                templateUrl: 'partials/search/search.html',
                controller: 'SearchController'
            })
            .otherwise({redirectTo: '/'})
    }])
    
    .controller("AlertCtrl",['$log','$scope','$timeout','AlertService', function($log,$scope,$timeout,AlertService){

        var _defaultTimeout = 3000;
        $scope.alerts = [];

        // Keep track of incoming alerts.
        $scope.$watch(AlertService.getAlert,function(alert){

            if(alert !== null){
                var length = $scope.alerts.push(alert);

                $timeout(function(){
                    if($scope.alerts.length >0){
                        $scope.alerts.splice(0,1);
                    }
                }, _defaultTimeout);
            }

        });

        $scope.closeAlert = function(index) {
            $scope.alerts.splice(index, 1);
        };

    }])
    .service('AlertService',['$log',function($log){

        var _currAlert = null;

        return {

            addAlert : function(alert){
                _currAlert = alert;
            },

            getAlert : function(){
                return _currAlert;
            }
        }

    }]);