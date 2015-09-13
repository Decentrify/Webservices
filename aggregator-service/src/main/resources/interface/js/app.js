'use strict';

(function(){

    angular.module('statisticsApp', ['ngRoute'])
        .config(['$routeProvider', '$logProvider', function($routeProvider, $logProvider){

            $logProvider.debugEnabled(true);

            $routeProvider.when('/',
                {
                    templateUrl:'partials/graphLanding.html'
                })
                .when('/aggregatedInfo',
                {
                    templateUrl:'partials/internalAggregatedInfo.html'
                })
                .when('/searchResponse',
                {
                    templateUrl:'partials/searchResponse.html'
                })


        }])


})();