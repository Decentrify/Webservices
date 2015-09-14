(function(){

    angular.module('statisticsApp')
        .controller('LandingController', ['$log','$interval','VisualizerService', LandingController]);

    function LandingController($log, $interval, VisualizerService) {

        var self = this;
        $log.debug('Landing Controller Initialized.');


    }


})();