/**
 * Created by babbarshaer on 2015-02-02.
 */

(function () {
    angular.module('app')
        .service('sweepService', ['$log', '$http', '$location', '$rootScope', sweepService]);

    function sweepService($log, $http, $location, $rootScope) {

        var _defaultPrefix = "http://";
        var _defaultHost = $location.host();
        var _defaultPort = "18180";
        var _serverName = "localhost";
        var _defaultContentType = "application/json";


        var server = {
            ip: _defaultHost,
            port: _defaultPort,
            name: _serverName
        };

        function _getUrl(prefix, server, accessPath) {
            return prefix.concat(server.ip).concat(":").concat(server.port).concat("/").concat(accessPath);
        }

        // Get a promise object.
        function _getPromiseObject(method, url, contentType, data) {

            return $http({
                method: method,
                url: url,
                headers: {'Content-Type': contentType},
                data: data
            });
        }


        return {

            setServer: function (data) {
                $log.info("Set Server Command Called");
                server = data;
                $rootScope.$broadcast('sweep-server:updated', server);
            },

            getServer: function () {
                return server;
            },

            performSearch: function (searchJson) {

                var _url = _getUrl(_defaultPrefix, server, "search");
                return _getPromiseObject('PUT', '/search', _defaultContentType, searchJson);
            },

            addIndexEntry: function (entryData) {
                $log.info("Index Entry Initiated.");
                var _url = _getUrl(_defaultPrefix, server, "add");
                return _getPromiseObject('PUT', '/add', _defaultContentType, entryData);
            }

        }

    }

}());



