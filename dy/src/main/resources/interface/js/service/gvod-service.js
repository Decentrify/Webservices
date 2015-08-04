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
                $rootScope.$broadcast('gvod-server:updated', server);
            },

            getServer: function () {
                return server;
            },

            // Play the resource.
            play: function (json) {

                var method = "PUT";
                var url = _getUrl(_defaultPrefix, server, "play");

                return _getPromiseObject(method, '/play', _defaultContentType, json);
            },


            download: function (json) {

                var method = "PUT";
                var url = _getUrl(_defaultPrefix, server, "downloadvideo");

                return _getPromiseObject(method, '/downloadvideo', _defaultContentType, json);
            },

            // Fetch the files in the library.
            fetchFiles: function () {

                $log.info("Firing Fetch Files call to server at port: " + server.port);
                var method = "GET";
                var url = _getUrl(_defaultPrefix, server, "files");
                $log.info("Url Constructed : " + url);

                return _getPromiseObject(method, '/files', _defaultContentType);
            },


            pendingUpload: function (json) {

                var method = 'PUT';
                var url = _getUrl(_defaultPrefix, server, "pendinguploadvideo");
                $log.info('Sending Pending Upload to: ' + url);
                return _getPromiseObject(method, '/pendinguploadvideo', _defaultContentType, json);

            },

            upload: function (json) {

                var method = 'PUT';
                var url = _getUrl(_defaultPrefix, server, "uploadvideo");
                $log.info('Sending Pending Upload to: ' + url);
                return _getPromiseObject(method, '/uploadvideo', _defaultContentType, json);
            },

            stop: function (json) {

                var method = 'PUT';
                var url = _getUrl(_defaultPrefix, server, "stop");

                return _getPromiseObject(method, '/stop', _defaultContentType, json);
            },

            playPos: function (json, port) {

                var method = 'PUT';
                var url = _getUrl(_defaultPrefix, server, "playPos");

                return _getPromiseObject(method, 'playPos', _defaultContentType, json);
            },

            removeVideo: function (json) {
                var method = 'PUT';
                var url = _getUrl(_defaultPrefix, server, "removeVideo");
                return _getPromiseObject(method, 'removeVideo', _defaultContentType, json);
            }


        }


    }

}());

