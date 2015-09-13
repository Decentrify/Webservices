
// Controller for representing the aggregated state information.

(function(){

    angular.module('statisticsApp')
        .controller('AggregatedStateController',['$log','$interval', AggregatedStateController]);

    function AggregatedStateController($log, $interval){
        $log.debug("Aggregated State Controller Initialized.");


        $interval(function(){

        })
    }

}());