/**
 * Created by babbar on 2015-03-18.
 */

(function () {

    angular.module('app')
        .service('aggregatorService', ['$log', '$http', '$location', aggregatorService]);

    function aggregatorService($log, $http, $location) {

        // Default Objects.
        var _defaultMethod = 'PUT';
        var _defaultHeader = {'Content-Type': 'application/json'};
        var _defaultIp = "http://" + $location.host() + (":9100");


        function _getPromiseObject(method, url, headers, data) {
            return $http({
                method: method,
                url: url,
                headers: headers,
                data: data
            })
        }

        return {

            getSimpleModelView: function () {
                var url = _defaultIp.concat("/systemsimplemodel");
                return _getPromiseObject('GET', url, _defaultHeader);
            },

            handshake: function () {
                var url = _defaultIp.concat("/handshake");
                return _getPromiseObject('GET', url, _defaultHeader);
            }
        }
    }


}());


