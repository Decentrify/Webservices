
// Controller for representing the aggregated state information.

(function(){

    angular.module('statisticsApp')
        .controller('AggregatedStateController',['$log','$interval','$scope','VisualizerService', AggregatedStateController]);

    function AggregatedStateController($log, $interval, $scope, VisualizerService){

        $log.debug("Aggregated State Controller Initialized.");

        var self = this;


        self.states = {

        };


        self.timeout = $interval(function(){
            VisualizerService.getAggregatedState()
                .then(success)
                .catch(httpError);
        }, 5000);

        function success(data){
            $log.debug(angular.toJson(data));
            self.states = data;
        }

        function httpError(error){
            $log.debug(error);
        }


        /**
         * On scope destruction, cancelling
         * the timeouts.
         */
        $scope.$on("$destroy", function(){

            $log.debug("Destroying the scope.");
            $interval.cancel(self.timeout);
        })


    }

}());