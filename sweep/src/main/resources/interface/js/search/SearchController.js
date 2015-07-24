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
        var entriesPerPage = 10;

        /**
         * Main function that initializes various services
         * over the page including the pagination and the video
         * player service.
         *
         * @param self
         * @private
         */
        function _initialize(self){

            self.search = {};
            self.search.searchText = $routeParams.searchText;
            self.playerName = 'main_player';

            /**
             * Pagination information.
             * @type {{hits: number, currentPage: number, entriesPerPage: number}}
             */
            self.paginate = {

                hits:0,
                currentPage:1,
                entriesPerPage: entriesPerPage
            };

//          INITIALIZE THE PAGINATE SEARCH.
            _paginateSearch( 0, self.paginate.entriesPerPage, self.search.searchText);

//          INITIALIZE THE PLAYER.
            _initializePlayer(self.playerName);

//          DESTROY PLAYER ON PAGE SWITCH.
            $scope.$on('$destroy', function () {

                $log.info('Destroy the video player instance.');
                if (self.player != null) {

                    self.player.dispose();
                    if (self.currentVideoResource != null) {
                        gvodService.stop(self.currentVideoResource);
                    }
                }
            });

        }


        /**
         * Main search function performing the paginate search
         * . The information is passed to the function based on the
         *
         * @param from
         * @param size
         * @param searchText
         * @private
         */
        function _paginateSearch ( from , size,  searchText) {

            var searchObj  = {

                searchPattern :{
                    fileNamePattern: searchText,
                    category: 'Video'
                },

                pagination: {
                    from: from,
                    size: size,
                    total: 0
                }

            };

            $log.debug("Going to perform a paginate search.");
            $log.debug(angular.toJson(searchObj));

            sweepService.performSearch(searchObj)

                .success(function (data) {

                    $log.debug('Sweep Service -> Successful');
                    $log.debug(angular.toJson(data));
                    self.search.result = data.searchResult;
                })

                .error(function (data) {
                    $log.warn('Sweep Service -> Error' + data);
                })
        }



        /**
         * Main function to be invoked on
         * every page change by the user by
         * pressing the paginate link at the bottom
         * of the page.
         */
        self.pageChange = function(){

            var from = self.paginate.currentPage;
            var size = self.paginate.entriesPerPage;
            var searchText = self.search.searchText;

            $log.debug("page change function invoked.");
            $log.debug("From: " + from + " Size: " + size + " Search Text: " + searchText);

            _paginateSearch(from, size, searchText);
        };




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
         *
         * Initialize the parameters
         * to be used as part of search.
         */
        _initialize(self);
    }
}());
