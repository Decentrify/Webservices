(function(){


    angular.module('statisticsApp')
        .controller('ReplicationLagController',['$log', '$scope','$interval', 'VisualizerService', ReplicationLagController]);

    /**
     * Main controller for depicting the replication lag in the system.
     * @param $log
     * @param $scope
     * @param $interval
     * @param VisualizerService
     * @constructor
     */
    function ReplicationLagController($log, $scope, $interval, VisualizerService){

        var self = this;
        self.container = [];

        self.timeout = $interval(function(){
            VisualizerService.getAvgReplicationLag()
                .then(success)
                .catch(httpError);
        }, 5000);

        function success(data){
            self.container = restructureData(data);
            $log.debug(angular.toJson(self.container));
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

    /**
     * Restructure the aggregated data based on the
     * UI requirement for rendering the information.
     *
     * @param baseJsonData
     * @returns {{data: Array, ranges: Array}}
     */
    function restructureData(baseJsonData){

        var i, len, minTime;
        var result = {
            data:[],
            ranges:[]
        };

        if(baseJsonData.length > 0){
            minTime = baseJsonData[0].time;
        }

        for(i= 0, len = baseJsonData.length; i < len ; i ++){
            if(baseJsonData[i].time < minTime){
                minTime = baseJsonData[i].time;
            }
        }

//      AT THIS POINT TIME IN EACH OBJECT IS RESTRUCTURED.
        for( i=0 , len= baseJsonData.length; i < len ; i++){

            baseJsonData[i].time = baseJsonData[i].time - minTime;
            result.data.push([(baseJsonData[i].time/1000), baseJsonData[i].avgLag]);
            result.ranges.push([(baseJsonData[i].time/1000), baseJsonData[i].minLag, baseJsonData[i].maxLag]);
        }

        return result;
    }

}());