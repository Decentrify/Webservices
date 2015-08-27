(function () {

    'use strict';

    angular.module('app')
        .service('common', ['$log', '$location', '$http', '$q', CommonService]);

    function CommonService($log, $location, $http, $q) {

        return {

            routeTo: routeTo,
            getServerStatus: serverStatus

        };

        function routeTo(path) {
            $location.path(path);
        }

        function serverStatus() {

            $log.debug('Invoking the fetch server status.');

            return $http({
                method: 'GET',
                url: '/caracalStatus'
            })
                .then(httpSuccess)
                .catch(httpError)
        }


        function httpSuccess(response){
            $log.debug("Server replied with response");
            return response.data;
        }

        function httpError(response){
            $log.debug("Received error for the rest call from the server");
            return $q.reject("Call to the server failed with HTTP status: " + response.status);
        }
    }

}());






