/**
 * Created by babbarshaer on 2015-02-02.
 */



(function () {

    angular.module("app")

        .service("gvodService", ['$log', '$http', '$location', '$rootScope', gvodService]);

    function gvodService($log, $http, $location, $rootScope) {

        var _defaultPrefix = "http://";
        var _defaultHost = $location.host();
        var _defaultPort = "18080";
        var _serverName = "localhost";
        var _defaultContentType = "application/json";


        var server = {
            ip: _defaultHost,
            port: _defaultPort,
            name: _serverName
        };


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
                $rootScope.$broadcast('gvod-server:updated', server);
            },

            getServer: function () {
                return server;
            },

            // Play the resource.
            play: function (json) {

                $log.debug("Initiating play on the resource.");
                var method = "PUT";
                return _getPromiseObject(method, '/play', _defaultContentType, json);
            },


            download: function (json) {

                $log.debug("Initiating downloadvideo on the resource.");
                var method = "PUT";
                return _getPromiseObject(method, '/downloadvideo', _defaultContentType, json);
            },

            // Fetch the files in the library.
            fetchFiles: function () {

                $log.debug("Initiating fetch file call.");
                var method = "GET";
                return _getPromiseObject(method, '/files', _defaultContentType);

            },


            pendingUpload: function (json) {

                $log.debug("Initiating the pending upload.");
                var method = 'PUT';
                return _getPromiseObject(method, '/pendinguploadvideo', _defaultContentType, json);
            },

            upload: function (json) {

                $log.debug("Initiating the upload");
                var method = 'PUT';
                return _getPromiseObject(method, '/uploadvideo', _defaultContentType, json);
            },

            stop: function (json) {

                $log.debug("Invoking the stop on the resource.");
                var method = 'PUT';
                return _getPromiseObject(method, '/stop', _defaultContentType, json);
            },

            playPos: function (json, port) {

                $log.debug("Fetching the play position on the resource.");
                var method = 'PUT';
                return _getPromiseObject(method, 'playPos', _defaultContentType, json);
            },

            removeVideo: function (json) {

                $log.debug("Initiating a call to remove the video.");
                var method = 'PUT';
                return _getPromiseObject(method, 'removeVideo', _defaultContentType, json);
            }


        }


    }

}());

