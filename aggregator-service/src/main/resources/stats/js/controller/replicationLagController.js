(function(){


    angular.module('statisticsApp')
        .controller('ReplicationLagController',['$log','$timeout', ReplicationLagController]);


    function ReplicationLagController($log, $timeout){

        $log.debug("Replication Lag Controller Initialized.");

        var self = this;
        self.data = [29.9, 71.5, 106.4, 129.2, 144.0, 176.0, 135.6, 148.5, 216.4, 194.1, 95.6, 54.4];


        $timeout(function(){

            $log.debug("Going to update the data");
            self.data = [129.9, 171.5, 10.4, 12.2, 44.0, 76.0, 5.6, 8.5, 6.4, 4.1, 75.6, 34.4];

        }, 5000);
    }

}());