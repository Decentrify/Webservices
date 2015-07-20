'use strict';

angular.module('app')
    .controller('LandingController', ['$log','$location','$scope','AlertService', function($log, $location, $scope, AlertService){
        $log.info("Current Host Location: " + $location.host());
        $log.info('Landing Controller Initialized.');


        $scope.$on('gvod-server:updated', function (event, data) {

            $log.info('gvod server updated');
            $log.info(data);

            $scope.$apply(function(){
                AlertService.addAlert({type: 'success', msg: 'Gvod Server Details Updated. '});
            })

        });


        $scope.$on('sweep-server:updated', function (event, data) {

            $log.info('sweep server updated');
            $log.info(data);

            $scope.$apply(function(){
                AlertService.addAlert({type: 'success', msg: 'Sweep Server Details Updated. '});
            })

        });



    }]);