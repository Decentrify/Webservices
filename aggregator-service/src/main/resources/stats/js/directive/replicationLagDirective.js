(function(){


    angular.module('statisticsApp')
        .directive('replicationLag', ['$log',replicationLag]);

    /**
     * Main directive for the replication lag display.
     * @param $log
     * @returns {{restrict: string, scope: {data: string}, link: Function, template: string}}
     */
    function replicationLag($log) {

        return {

            restrict: 'E',
            scope: {
                container: '='
            },


            link: function (scope, elem, attrs) {

                var chart = new Highcharts.Chart({

                    chart: {
                        renderTo: 'replicationLagContainer'
                    },

                    title: {
                        text: 'Average Entry Replication Lag'
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

                    tooltip: {
                        crosshairs: true,
                        shared: true,
                        valueSuffix: ''
                    },

                    series: [{
                        name: 'Average Lag',
                        data: scope.container.data,
                        zIndex: 1,
                        marker: {
                            fillColor: 'white',
                            lineWidth: 2,
                            lineColor: Highcharts.getOptions().colors[0]
                        }
                    },
                        {
                            name: 'Entry Lag Range',
                            data: scope.container.ranges,
                            type: 'arearange',
                            lineWidth: 0,
                            linkedTo: ':previous',
                            color: Highcharts.getOptions().colors[0],
                            fillOpacity: 0.3,
                            zIndex: 0
                        }
                    ]
                });

                scope.$watch("container", function (newValue) {
                    chart.series[0].setData(newValue.data, true);
                    chart.series[1].setData(newValue.ranges, true);
                }, true);

            },

            template: '<div id="replicationLagContainer" style="height: 500px; width: 980px" class="medium-top-buffer"> Not Working </div>'
        }
    }


}());