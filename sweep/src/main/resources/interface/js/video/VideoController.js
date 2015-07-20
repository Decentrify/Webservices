'use strict';
angular.module('app')
    
    .controller('VideoController', ['$scope', '$log','gvodService', function($scope,$log,gvodService){
        
        var precedingText = "http://localhost:";
        
        function _initScope(scope){
            
            // Initialize Player.
            scope.player = videojs('main_player',{}, function(){});
            scope.player.dimensions("100%", "100%");
            scope.player.controls(true);
            
            // Register Callbacks.
            scope.player.on('timeupdate', function(data){
                $log.info(scope.player.currentTime());
                
                var playPos ={
                    overlayId: 0,
                    videoName: scope.linkForm.name,
                    playPos: scope.player.currentTime()
                };
                gvodService.playPos(playPos, parseInt(scope.linkForm.port+1));
            });
            
            scope.player.on('seeking', function(data){
               $log.info("Seeking Player...");
                $log.info(data);
                $log.info(scope.player.currentTime());
            });
            
            
            scope.$on('$destroy', function(){
                if(scope.player != null){
                    scope.player.dispose();
                }
            });
            
            scope.linkForm = {
                linkGenerated: 'none'
            }
            
        }
        
        
        $scope.updateAndPlay = function(){

            if(this.dynamicLinkForm.$valid) {
                
                $log.info("Update and Play called.");
                $scope.linkForm.linkGenerated = precedingText.concat($scope.linkForm.port, "/", $scope.linkForm.name, "/", $scope.linkForm.name);

                if($scope.player != null){
                    
                    $scope.player.pause();
                    $scope.player.src({src: $scope.linkForm.linkGenerated, type: "video/mp4"});
                    $scope.player.load();
                    $scope.player.play();
                    
                }
                
            }
            
        };
        
        
        
        _initScope($scope);

    }]);
