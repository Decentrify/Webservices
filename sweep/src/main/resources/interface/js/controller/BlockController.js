(function(){

    angular.module('app')
        .controller('BlockController', ['$log', '$timeout','blockUI', BlockController]);

    function BlockController($log, $timeout, blockUI){

        $log.debug("Block Controller Initialized");
        var self = this;
        var mainBlock = blockUI.instances.get('mainBlock');

        self.testBlock = function(){
            $log.debug("Initiate block ui testing .. ");

            mainBlock.start();
            $timeout(function(){

                $log.debug("Time to finish the block ...")
                mainBlock.stop();

            }, 2000);
        }

    }
}());