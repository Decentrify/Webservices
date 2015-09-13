(function(){

    angular.module('statisticsApp')
        .controller('LandingController', ['$log', LandingController]);

    function LandingController($log) {

        var self = this;
        $log.debug('Landing Controller Initialized.');

    }


})();