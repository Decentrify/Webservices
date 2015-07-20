'use strict';

angular.module('app')

    .controller('LandingController', ['$log','$location','$scope','AlertService', LandingController]);


function LandingController($log, $location, $scope, AlertService){

    $log.debug("Current Host Location: " + $location.host());
    $log.debug('Landing Controller Initialized.');

    $scope.$on('gvod-server:updated', function (event, data) {

        $log.debug('gvod server updated');
        $log.debug(data);

        $scope.$apply(function(){
            AlertService.addAlert({type: 'success', msg: 'Gvod Server Details Updated. '});
        })

    });


    $scope.$on('sweep-server:updated', function (event, data) {

        $log.debug('sweep server updated');
        $log.debug(data);

        $scope.$apply(function(){
            AlertService.addAlert({type: 'success', msg: 'Sweep Server Details Updated. '});
        })

    });
}