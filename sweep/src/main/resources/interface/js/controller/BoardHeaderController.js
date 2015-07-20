'use strict';

angular.module('app')

    .controller('BoardHeaderController', ['$log', '$scope', 'common', function ($log, $scope, common) {

        function _initScope(scope) {

            $log.info('Board Header Controller Initialized.');
            scope.routeTo = function (path) {
                common.routeTo(path);
            };
            
            scope.searchObj ={
                searchTerm:  null
            };
        }
        
        
        $scope.search = function(searchTerm){
          
            if(this.searchForm.$valid){
                $log.info('search form valid');
                common.routeTo('/search/'+ searchTerm);
            }
        };

        _initScope($scope);
    }])

    .directive('fileUploader', ['$log','gvodService', 'sweepService', function ($log,gvodService, sweepService) {

        // Upload the file to the system.
        return{
            
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
    }])

    .directive('clickDirective', ['$log', function ($log) {

        return {
            restrict: 'A',
            link: function (scope, element, attributes) {

                element.bind('click', function (clickEvent) {
                    var uploaderElement = angular.element(document.querySelector("#fileUploader"));
                    uploaderElement.trigger('click');
                });

            }
        }

    }]);