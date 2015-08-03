/**
 * Created by babbarshaer on 2015-01-31.
 */

angular.module('sweepMain',['ngRoute'])
    
    .config(['$routeProvider','$locationProvider', function($routeProvider,$locationProvider){
        
        $routeProvider.
            
            when('/', 
            {templateUrl: 'partials/main/welcome-partial.html'
            }).
            when('/search',
            {
                templateUrl:'partials/search/search.html',
                controller: 'SearchController'
                
            }).
            when('/upload',
            {
                templateUrl: 'partials/uploader/indexUpload.html',
                controller: 'UploaderController'
            }).
            otherwise({
                redirectTo: '/'
            });


//        $locationProvider.html5Mode({
//            enabled: true,
//            requireBase: false
//        });
//        $locationProvider.hashPrefix('!');
        
    }]);
