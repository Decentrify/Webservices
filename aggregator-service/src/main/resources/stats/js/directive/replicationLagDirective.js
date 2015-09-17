(function(){


    angular.module('statisticsApp')
        .directive('replicationLag', replicationLag);

    function replicationLag(){

        return{

            restrict: 'E',
            scope: {
                data:'='
            },


            link: function(scope, elem, attrs){

                var chart =  new Highcharts.Chart({

                    chart :{
                        renderTo: 'container'
                    },

                    title: {
                        text: 'July temperatures'
                    },

                    xAxis: {
                        type: 'linear'
                    },

                    yAxis: {
                        title: {
                            text: null
                        }
                    },

                    tooltip: {
                        crosshairs: true,
                        shared: true,
                        valueSuffix: '°C'
                    },

                    legend: {
                    },

                    series: [{
                        name: 'Temperature',
                        data: scope.data,
                        zIndex: 1,
                        marker: {
                            fillColor: 'white',
                            lineWidth: 2,
                            lineColor: Highcharts.getOptions().colors[0]
                        }
                    }
                    //    {
                    //    name: 'Range',
                    //    data: ranges,
                    //    type: 'arearange',
                    //    lineWidth: 0,
                    //    linkedTo: ':previous',
                    //    color: Highcharts.getOptions().colors[0],
                    //    fillOpacity: 0.3,
                    //    zIndex: 0
                    //}
                    ]
                });

                scope.$watch("data", function (newValue) {
                    chart.series[0].setData(newValue, true);
                }, true);

            },

            template: '<div id="container" style="height: 500px; width: 700px" class="medium-top-buffer"> Not Working </div>'
        }
    }


}());