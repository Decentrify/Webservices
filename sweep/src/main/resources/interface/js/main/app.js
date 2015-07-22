'use strict';

(function(){
    angular.module('app', [
        'ngRoute',
        'nvd3',
        'ui.bootstrap'
    ])
        .config(['$routeProvider','$logProvider', function($routeProvider, $logProvider){

            $logProvider.debugEnabled(true);

            $routeProvider

                .when('/',
                {
                    templateUrl: 'partials/main/landing-page.html',
                    controller: 'LandingController',
                    controllerAs: 'landingController'
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
                    controller: 'SearchController',
                    controllerAs: 'searchController'
                })
                .otherwise({redirectTo: '/'})
        }])

        .controller("AlertCtrl", ['$log','$scope','$timeout','AlertService', AlertCtrl])
        .service('AlertService',['$log', AlertService]);


    /**
     * Main controller for the Alert Service.
     * Creates a separate timeout for each alert added.
     *
     * @param $log
     * @param $scope
     * @param $timeout
     * @param AlertService
     * @constructor
     */

    function AlertCtrl($log, $scope, $timeout, AlertService){

        var _defaultTimeout = 3000;

        var self = this;
        self.alerts = [];

        // Keep track of incoming alerts.
        $scope.$watch(AlertService.getAlert,function(alert){

            if(alert !== null){
                var length = self.alerts.push(alert);

                $timeout(function(){
                    if(self.alerts.length >0){
                        self.alerts.splice(0,1);
                    }
                }, _defaultTimeout);
            }

        });

        self.closeAlert = function(index) {
            self.alerts.splice(index, 1);
        };
    }


    /**
     * Service created for adding and fetching the alerts
     * to and from the system respectively.
     *
     * @param $log
     * @returns {{addAlert: Function, getAlert: Function}}
     * @constructor
     */
    function AlertService($log){

        var _currAlert = null;

        return {

            addAlert : function(alert){
                _currAlert = alert;
            },

            getAlert : function(){
                return _currAlert;
            }
        }
    }




}());


