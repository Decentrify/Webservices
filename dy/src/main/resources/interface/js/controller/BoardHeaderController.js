
(function(){

    'use strict';
    angular.module('app')

        .controller('BoardHeaderController', ['$log','$interval','$scope', 'common', BoardHeaderController])
        .directive('fileUploader', ['$log', 'gvodService', 'sweepService', fileUploader])
        .directive('clickDirective', ['$log', clickDirective]);


// MAIN HEADER CONTROLLER.
    function BoardHeaderController($log, $interval, $scope, common) {


        $log.info('Board Header Controller Initialized.');
        var self = this;
        self.serverUp = true;

        self.routeTo = function (path) {
            common.routeTo(path);
        };

        self.searchObj = {
            searchTerm: null
        };

        self.search = function (searchTerm) {

            if (this.searchForm.$valid) {
                $log.info('search form valid');
                common.routeTo('/search/' + searchTerm);
            }
        };

        var timer = $interval( function(){

            $log.debug('Fetching the server status.');
            common.getServerStatus()

                .then(function(data) {

                    $log.debug("Is server down : " + data);
                    self.serverUp = !data;
                })
                .catch(function(data){
                    $log.debug("REST Call to fetch the server status failed with status: " + data.status);
                })

        }, 5000);

        $scope.$on('$destroy', function(event){

            $log.debug("Going to cancel the interval.");
            $interval.cancel(timer);
        })
    }

// DIRECTIVE EXPLAINING

    function fileUploader($log, gvodService, sweepService) {


        // Upload the file to the system.
        return {

            restrict: 'A',
            link: function (scope, element, attributes) {

                element.bind('change', function (changeEvent) {

                    var reader = new FileReader();
                    reader.onload = function (loadEvent) {

                        $log.debug(" Going to load the file content ... ");

                        var serverData = {
                            info: loadEvent.target.result
                        };

                        var serverInfo = JSON.parse(serverData.info);
                        $log.debug(serverInfo);

                        gvodService.setServer(serverInfo.gvod);
                        sweepService.setServer(serverInfo.sweep);

                        element.val("");
                    };

                    reader.readAsText(changeEvent.target.files[0]);
                });
            }
        }
    }


// DIRECTIVE TO REDIRECT CLICK.

    function clickDirective($log) {

        return {
            restrict: 'A',
            link: function (scope, element, attributes) {

                element.bind('click', function (clickEvent) {
                    var uploaderElement = angular.element(document.querySelector("#fileUploader"));
                    uploaderElement.trigger('click');
                });

            }
        }
    }

}());


