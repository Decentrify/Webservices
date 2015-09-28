(function(){


    angular.module('statisticsApp')
        .controller('PercentageReplicationLagController',['$log', '$scope','$interval', 'VisualizerService', PercentageReplicationLagController]);

    /**
     * Main controller for depicting the replication lag in the system.
     * @param $log
     * @param $scope
     * @param $interval
     * @param VisualizerService
     * @constructor
     */
    function PercentageReplicationLagController($log, $scope, $interval , VisualizerService){

        var self = this;
        self.container = [];

        self.timeout = $interval(function(){
            VisualizerService.getPerReplicationLag()
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
     * Restructure the data based on the
     *
     * @param baseJsonData
     * @returns {{fifty: Array, seventyFive: Array, ninety: Array}}
     */
    function restructureData(baseJsonData){

        var i, len, minTime;
        var result = {
            fifty:[],
            seventyFive:[],
            ninety:[]
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
            result.fifty.push([(baseJsonData[i].time/1000), baseJsonData[i].fifty]);
            result.seventyFive.push([(baseJsonData[i].time/1000), baseJsonData[i].seventyFive]);
            result.ninety.push([(baseJsonData[i].time/1000), baseJsonData[i].ninety]);
        }


        return result;
    }

}());