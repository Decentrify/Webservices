/**
 * Created by babbarshaer on 2015-02-16.
 */

angular.module('sweepMain')

    .controller('SidebarController',['$log','$scope','$location',function($log, $scope, $location){

        // Task of redirecting is done by this controller.
        function initScope(scope){
            $log.info(" Executing initialization tasks");
            scope.modules = {
                search : 'search',
                upload : 'upload',
                main: '/'
            }
        }

        $scope.switchTo = function(moduleName){
            $location.path(moduleName);
        };

        initScope($scope);
    }]);