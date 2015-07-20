/**
 * Created by babbarshaer on 2015-02-05.
 */



angular.module('app')

    .controller('SearchController', ['$log', '$scope', '$routeParams', 'sweepService','gvodService', function ($log, $scope, $routeParams, sweepService, gvodService) {

        var _defaultPrefix = "http://";

        function _getDummyResults() {

            return [
                {
                    fileName: 'Avengers: Age of Ultron',
                    uploaded: new Date(),
                    description: 'When Tony Stark tries to jumpstart a dormant peacekeeping program, things go awry and it is up to the Avengers to stop the villainous Ultron from enacting his terrible plans.',
                    url: 1

                },
                {
                    fileName: 'Iris',
                    uploaded: new Date(),
                    description: 'A documentary about fashion icon Iris Apfel from legendary documentary filmmaker Albert Maysles.',
                    url: 1
                },
                {
                    fileName: 'Cloud Atlas',
                    uploaded: new Date(),
                    description: 'An exploration of how the actions of individual lives impact one another in the past, present and future, as one soul is shaped from a killer into a hero, and an act of kindness ripples across centuries to inspire a revolution.',
                    url: 1
                },
                {
                    fileName: 'Fight Club',
                    uploaded: new Date(),
                    description: 'An insomniac office worker looking for a way to change his life crosses paths with a devil-may-care soap maker and they form an underground fight club that evolves into something much, much more...',
                    url: 1
                },
                {
                    fileName: 'The Dark Knight',
                    uploaded: new Date(),
                    description: 'When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, the caped crusader must come to terms with one of the greatest psychological tests of his ability to fight injustice.',
                    url: 1
                }

            ]
        }


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

                        $log.info(" Gvod Has successfully stopped playing the video.");
                        _startPlaying(player, newResource);
                    })

                    .error(function (data) {
                        $log.info(" Unable to stop the resource. ");
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

                    $log.info("Got the port from gvod: " + data);
                    $scope.currentVideoResource = resource;
                    var src = _defaultPrefix.concat(gvodService.getServer().ip).concat(":").concat(data).concat('/').concat(name).concat('/').concat(name);

                    $log.info("Source for the player constructed: " + src);
                    if (player == null) {
                        $log.warn('Player in the scope found as null. Reconstructing it .. ');
                        player = _initializePlayer($scope.playerName);
                    }

                    player.src(src);
                    player.load();
                    player.play();
                })

                .error(function(data){
                    $log.warn(" gvod play service replied with error.");
                })
        }


        /**
         * Constructor function for creating the playback resource.
         *
         * @param obj scope variable for player
         * @param playerName Player Name
         * @private
         */
        function _initializePlayer(playerName) {

            $scope.player = videojs(playerName, {}, function () {
            });
            $scope.player.dimensions("100%", "100%");
            $scope.player.controls(true);

            return $scope.player;
        }

        /**
         * Initialization of the scope.
         * @param scope
         */
        function initScope(scope) {

            scope.search = {};
            scope.search.searchText = $routeParams.searchText;
            scope.playerName = 'main_player';

            // Initialize Resources.
            _search(scope.search.searchText);

            //scope.search.result = _getDummyResults();

            // Initialize Player.
            _initializePlayer(scope.playerName);

            // Destroy Player Call Back.
            scope.$on('$destroy', function () {

                $log.info('Destroy the video player instance.');
                if (scope.player != null) {

                    scope.player.dispose();
                    if(scope.currentVideoResource != null){
                        gvodService.stop($scope.currentVideoResource);
                    }
                }
            })
        }


        /**
         * Play the provided video resource.
         * @param data
         */
        $scope.playResource = function (data) {

            $log.info('Play Resource called with : ' + data);

            var json = {
                name: data["fileName"],
                overlayId: parseInt(data["url"])
            };

            //var test_json = {
            //    name: 'demo.mp4',
            //    overlayId: 12
            //};

            $log.info('Reconstructing play call with : ' + json);

            _updateAndPlay($scope.player, $scope.currentVideoResource, angular.copy(json));
        };


        /**
         * Based on the search term provided,
         * search the sweep for the matching files.
         *
         * @param searchTerm Term to search for.
         * @private
         */
        function _search(searchTerm) {

            $log.info("Going to perform search for : " + searchTerm);

            var searchObj = {
                fileNamePattern: searchTerm,
                category: 'Video'
            };

            sweepService.performSearch(searchObj)

                .success(function (data) {
                    $log.info('Sweep Service -> Successful');
                    $scope.search.result = data;
                })

                .error(function (data) {
                    $log.info('Sweep Service -> Error' + data);
                })
        }

        initScope($scope);

    }])

    .directive('searchResult', ['$log', function ($log) {
        return {
            restrict: 'AE',
            templateUrl: 'partials/search/search-result.html'
        }
    }]);