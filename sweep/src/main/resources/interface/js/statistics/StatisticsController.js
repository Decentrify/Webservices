(function () {
    'use strict';

    angular.module('app').
        controller('StatisticsController', ['$scope', '$log', '$interval', function ($scope, $log, $interval) {

        }])

        .directive('rinnegan', ['$log', '$interval', '$q', 'AlertService', 'aggregatorService', function ($log, $interval, $q, AlertService, aggregatorService) {

            return {

                restrict: 'E',
                scope: {},

                controller: function ($scope) {

                    function startIntervalTask(timeout) {

                        $scope.interval = $interval(function () {

                            $log.info("Executing the scheduled rest call. ");
                            aggregatorService.getSimpleModelView()
                                .success(function (data) {
                                    $scope.states = data;
                                })
                                .error(function (data) {
                                    $log.info('Unable to fetch the state information ')
                                    $scope.states = null;
                                })
                        }, timeout);
                    }


                    function initScope(scp) {

                        // Chaining Promises.
                        aggregatorService.handshake()

                            .then(function (success) {

                                AlertService.addAlert({type: 'success', msg: 'Server handshake successful'});
                                startIntervalTask(5000);

                            }, function (error) {

                                $log.warn(error);
                                return $q.reject('Server Handshake Failed.');
                            })

                            .then(null, function (error) {
                                AlertService.addAlert({type: 'warning', msg: error});
                            });


                        scp.$on('$destroy', function () {
                            if (scp.interval != null) {
                                $log.info("Cancelling the interval");
                                $interval.cancel(scp.interval);
                            }
                        })
                    }

                    initScope($scope);
                },

                templateUrl: 'partials/statistics/rinnegan-view.html'
            }
        }])


        .directive('scatterPlot', ['$log', '$interval', function ($log, $interval) {
            return {
                restrict: 'AE',
                scope: true,
                controller: function ($scope) {
                    /* Random Data Generator (took from nvd3.org) */
                    function generateData(groups, points) {
                        var data = [],
                            shapes = ['circle', 'cross', 'triangle-up', 'triangle-down', 'diamond', 'square'],
                            random = d3.random.normal();

                        for (var i = 0; i < groups; i++) {
                            data.push({
                                key: 'Group ' + i,
                                values: []
                            });

                            for (var j = 0; j < points; j++) {
                                data[i].values.push({
                                    x: random(), y: random(), size: Math.random()
//                        , shape: shapes[j % 6]
                                });
                            }
                        }
                        return data;
                    }

                    function _initScope(scope) {

                        scope.selectValue = 'default';
                        scope.options = {
                            chart: {
                                type: 'scatterChart',
                                height: 500,
                                color: d3.scale.category10().range(),
                                scatter: {
                                    onlyCircles: false
                                },
                                showDistX: false,
                                showDistY: false,
                                tooltipContent: function (key) {
                                    return '<h3>' + key + '</h3>';
                                },
                                transitionDuration: 350,
                                xAxis: {
                                    axisLabel: 'X Axis',
                                    tickFormat: function (d) {
                                        return d3.format('.02f')(d);
                                    }
                                },
                                yAxis: {
                                    axisLabel: 'Y Axis',
                                    tickFormat: function (d) {
                                        return d3.format('.02f')(d);
                                    },
                                    axisLabelDistance: 30
                                }
                            }
                        };

                        scope.data = generateData(2, 40);

                        scope.generateDataInterval = $interval(function () {
                            scope.data = generateData(2, 40);
                        }, 5000);

                        scope.$on('$destroy', function () {
                            if (scope.generateDataInterval != null) {
                                $interval.cancel(scope.generateDataInterval);
                            }
                        })

                    }

                    _initScope($scope);
                }

            }
        }]);

}());
