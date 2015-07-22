(function () {

    'use strict';

    angular.module('app')

        .controller('SearchController', ['$log', '$scope', '$routeParams', 'sweepService', 'gvodService', SearchController])
        .directive('searchResult', ['$log', searchResult]);

    /**
     * Main function representing a simple directive
     * for displaying the search results.
     *
     * @param $log
     * @returns {{restrict: string, templateUrl: string}}
     */
    function searchResult($log) {

        return {
            restrict: 'AE',
            templateUrl: 'partials/search/search-result.html'
        }
    }


    /**
     * Main Controller respnsible for handling the search of the
     * metadata and pagination. In addition to this, it also handles the
     * video player bootup and dispose protocols.
     *
     * @param $log
     * @param $scope
     * @param $routeParams
     * @param sweepService
     * @param gvodService
     * @constructor
     */
    function SearchController($log, $scope, $routeParams, sweepService, gvodService) {

        var self = this;
        var _defaultPrefix = "http://";

        /**
         * Initialization of the scope.
         * @param scope
         */

        self.search = {};
        self.search.searchText = $routeParams.searchText;
        self.playerName = 'main_player';

        // Initialize Resources.
        _search(self.search.searchText);

        //scope.search.result = _getDummyResults();

        // Initialize Player.
        _initializePlayer(self.playerName);

        // Destroy Player Call Back.
        $scope.$on('$destroy', function () {

            $log.info('Destroy the video player instance.');
            if (self.player != null) {

                self.player.dispose();
                if (self.currentVideoResource != null) {
                    gvodService.stop(self.currentVideoResource);
                }
            }
        });

        /**
         * Play the provided video resource.
         * @param data
         */
        self.playResource = function (data) {

            $log.debug('Play Resource called with : ' + angular.toJson(data));

            var json = {
                name: data["fileName"],
                overlayId: parseInt(data["url"])
            };

            $log.debug('Reconstructing play call with : ' + angular.toJson(json));
            _updateAndPlay(self.player, self.currentVideoResource, angular.copy(json));
        };


        /**
         * Update the video resource in the provided player
         *
         * @param player video player instance.
         * @param currentResource current video resource.
         * @param newResource updated resource.
         * @private
         */
        function _updateAndPlay(player, currentResource, newResource) {

            // 1. Pause the current playing
            if (player != null) {
                player.pause();
            }

            // 2. Inform gvod about the update
            if (currentResource != null) {

                gvodService.stop(currentResource)

                    // 3. Handle the response from the gvod and based on response decide further course of action.
                    .success(function (data) {

                        $log.debug(" Gvod Has successfully stopped playing the video.");
                        _startPlaying(player, newResource);
                    })
                    .error(function (data) {
                        $log.debug(" Unable to stop the resource. ");
                    })
            }
            else {
                $log.info('Current Resource is null, so playing from start');
                _startPlaying(player, newResource);
            }
        }


        /**
         * Internal helper function for starting the video playback.
         *
         * @param player player
         * @param resource video resource
         * @private
         */
        function _startPlaying(player, resource) {

            var name = resource['name'];
            gvodService.play(resource)

                .success(function (data) {

                    $log.debug("Got the port from gvod: " + data.playPort);

                    self.currentVideoResource = resource;
                    var src = _defaultPrefix.concat(gvodService.getServer().ip).concat(":").concat(data.playPort).concat('/').concat(name).concat('/').concat(name);

                    $log.info("Source for the player constructed: " + src);
                    if (player == null) {
                        $log.warn('Player in the scope found as null. Reconstructing it .. ');
                        player = _initializePlayer(self.playerName);
                    }

                    player.src(src);
                    player.load();
                    player.play();
                })

                .error(function (data) {
                    $log.warn(" gvod play service replied with error.");
                })
        }


        /**
         * Constructor function for creating the playback resource.
         * @param playerName Player Name
         * @private
         */
        function _initializePlayer(playerName) {

            self.player = videojs(playerName, {}, function () {
            });
            self.player.dimensions("100%", "100%");
            self.player.controls(true);

            return self.player;
        }


        /**
         * Based on the search term provided,
         * search the sweep for the matching files.
         *
         * @param searchTerm Term to search for.
         * @private
         */
        function _search(searchTerm) {

            $log.debug("Going to perform search for : " + searchTerm);
            
            var searchObj  = {
                
                searchPattern :{
                    fileNamePattern: searchTerm,
                    category: 'Video'
                },
                
                pagination: null
            };

            sweepService.performSearch(searchObj)

                .success(function (data) {
                    
                    $log.debug('Sweep Service -> Successful');
                    self.search.result = data.searchResult;
                })

                .error(function (data) {
                    $log.warn('Sweep Service -> Error' + data);
                })
        }


    }
}());
