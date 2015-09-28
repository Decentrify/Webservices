
(function(){

    angular.module('statisticsApp')
        .directive('percentileLag',['$log', percentileLag]);


    /**
     * Main directive for the replication lag display.
     * @param $log
     * @returns {{restrict: string, scope: {data: string}, link: Function, template: string}}
     */
    function percentileLag($log) {

        return {

            restrict: 'E',
            scope: {
                container: '='
            },


            link: function (scope, elem, attrs) {

                var chart = new Highcharts.Chart({

                    chart: {
                        renderTo: 'percentileReplicationLagContainer'
                    },

                    title: {
                        text: 'Entry Replication Lag'
                    },

                    xAxis: {
                        type: 'linear',
                        title:{
                            text: 'Time(sec)'
                        }
                    },

                    yAxis: {
                        title: {
                            text: 'Number of Entries'
                        }
                    },

                    tooltip: {
                        crosshairs: true,
                        shared: true,
                        valueSuffix: ' entries'
                    },

                    legend: {
                        layout: 'vertical',
                        align: 'left',
                        verticalAlign: 'top',
                        x: 40,
                        y: 80,
                        floating: true,
                        backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF',
                        borderWidth: 1
                    },

                    series: [{
                        name: 'Fifty Percentile Lag',
                        data: scope.container.fifty,
                        zIndex: 1,
                        marker: {
                            fillColor: 'white',
                            lineWidth: 2,
                            lineColor: Highcharts.getOptions().colors[0]
                        }
                    },

                        {
                            name: 'Seventy Five Percentile Lag',
                            data: scope.container.seventyFive,
                            zIndex: 1,
                            marker: {
                                fillColor: 'white',
                                lineWidth: 2,
                                lineColor: Highcharts.getOptions().colors[1]
                            }
                        },

                        {
                            name: 'Ninety Percentile Lag',
                            data: scope.container.ninety,
                            zIndex: 1,
                            marker: {
                                fillColor: 'white',
                                lineWidth: 2,
                                lineColor: Highcharts.getOptions().colors[2]
                            }
                        }
                    ]
                });

                scope.$watch("container", function (newValue) {
                    chart.series[0].setData(newValue.fifty, true);
                    chart.series[1].setData(newValue.seventyFive, true);
                    chart.series[2].setData(newValue.ninety, true);
                }, true);

            },

            template: '<div id="percentileReplicationLagContainer" style="height: 500px; width: 980px" class="medium-top-buffer"> Not Working </div>'
        }
    }

}());



