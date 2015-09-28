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
                    templateUrl:'partials/internalAggregatedInfo.html',
                    controller: 'AggregatedStateController',
                    controllerAs: 'aggregatedStateController'

                })
                .when('/searchResponse',
                {
                    templateUrl:'partials/searchResponse.html'
                })
                .when('/replicationLag',
                {
                    templateUrl:'partials/replicationLag.html',
                    controller: 'ReplicationLagController',
                    controllerAs: 'replicationLagController'
                })
                .when('/perReplicationLag',
                {
                    templateUrl:'partials/perReplicationLag.html',
                    controller: 'PercentageReplicationLagController',
                    controllerAs: 'percentileLagController'
                })

        }])


})();