
(function(){

    angular.module('statisticsApp')
        .service('VisualizerService', ['$log','$http', '$q', VisualizerService]);


    function VisualizerService($log, $http, $q){

        return {
            getAggregatedState: getAggregatedState,
            getAvgSearchResponse: getAvgSearchResponse
        };


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