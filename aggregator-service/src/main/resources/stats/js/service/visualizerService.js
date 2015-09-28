
(function(){

    angular.module('statisticsApp')
        .service('VisualizerService', ['$log','$http', '$q', VisualizerService]);


    function VisualizerService($log, $http, $q){

        return {
            getAggregatedState: getAggregatedState,
            getAvgSearchResponse: getAvgSearchResponse,
            getAvgReplicationLag: getAvgReplicationLag,
            getPerReplicationLag: getPerReplicationLag
        };


        /**
         * REST Call to fetch the average replication lag in the system.
         * By default the call fetches the last N windows.
         */
        function getAvgReplicationLag(){

            $log.debug("Invoking the call to fetch the average replication lag in the system.")

            return $http({

                method: 'GET',
                url: '/getAvgReplicationLag'
            })
                .then(httpSuccess)
                .catch(httpError)
        }



        /**
         * REST Call to fetch the percentile replication lag in the system.
         * By default the call fetches the last N windows.
         */
        function getPerReplicationLag(){

            $log.debug("Invoking the call to fetch the average replication lag in the system.")

            return $http({

                method: 'GET',
                url: '/getPerReplicationLag'
            })
                .then(httpSuccess)
                .catch(httpError)
        }



        /**
         * REST Call to fetch the aggregated internal state of all the
         * active nodes in the system.
         * @returns {*}
         */
        function getAggregatedState(){

            $log.debug("Invoking the call to get the aggregated state.");

            return $http({

                method: 'GET',
                url:'/getAllState'
            })
                .then(httpSuccess)
                .catch(httpError)
        }


        /**
         * REST Call to start fetching the Average Search Response for the
         * nodes in the system.
         *
         * @returns {*}
         */
        function getAvgSearchResponse(){
            $log.debug("Invoking REST Call to fetch the avg search response.");
            return $http({
                method: 'GET',
                url: '/getAvgSearchResp'
            })
                .then(httpSuccess)
                .catch(httpError);
        }


        function httpSuccess(response){
            $log.debug("REST Call successful, returning the data");
            return response.data;
        }

        function httpError(reject){

            $log.debug("REST call failed, rejecting further");
            $q.reject("REST Call failed with status " + reject.status);
        }
    }

}());