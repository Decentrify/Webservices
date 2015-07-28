'use strict';

angular.module('app')
    
    .service('common', ['$log','$location', function($log, $location){

        return {
            
            routeTo : function(path){
                $location.path(path);
            }
        }
        
    }]);