(function () {

    angular.module('app')

        .filter('waitingUpload', ['$log', waitingUpload])
        .filter('uploaded', ['$log', uploaded])
        .controller('UploadController', ['$log', '$scope', 'common', 'gvodService', UploadController])
        .controller('EntryUploadController', ['$log', '$scope', '$q', 'gvodService', 'sweepService', 'AlertService', EntryUploadController]);


    /**
     * Directive function used to separate the entries in the
     * waiting upload category based on the status provided by
     * the streaming service.
     *
     * @param $log
     * @returns {Function}
     */
    function waitingUpload($log) {

        return function (data) {
            var filteredData = [];

            if (data != null) {
                for (var i = 0; i < data.length; i++) {

                    if (data[i]["status"] === "NONE") {
                        filteredData.push(data[i]);
                    }

                }
            }
            return filteredData;
        }
    }

    /**
     * Directive function used to separate the entries in the upload category
     * based on the successful upload of the entries in the system.
     *
     * @param $log
     * @returns {Function}
     */
    function uploaded($log) {

        return function (data) {
            var filteredData = [];

            if (data != null) {
                for (var i = 0; i < data.length; i++) {

                    if (data[i]["status"] !== "NONE") {
                        filteredData.push(data[i]);
                    }
                }
            }
            return filteredData;
        }
    }


    /**
     * Main Controller for the landing page for the uploader.
     * The main function is to route to the main upload page.
     *
     * @param $log
     * @param $scope
     * @param common
     * @param gvodService
     * @constructor
     */
    function UploadController($log, $scope, common, gvodService) {

        function _initScope(scope) {

            scope.routeTo = function (path) {
                common.routeTo(path);
            };
        }

        _initScope($scope);
    }

    /**
     * The core controller of the uplading the entries to the system.
     * The controller captures metadata from the users and then performs a series
     * of well defined tasks.
     *
     * @param $log
     * @param $scope
     * @param $q
     * @param gvodService
     * @param sweepService
     * @param AlertService
     * @constructor
     */
    function EntryUploadController($log, $scope, $q, gvodService, sweepService, AlertService) {


        // UTILITY FUNCTION.
        function _reformatData(data) {

            var list = [];
            var isCheckSet = false;

            for (var key in data) {

                var obj = {};
                obj["name"] = key;
                obj["status"] = data[key];

                if (!isCheckSet && obj["status"] === "NONE") {
                    // Set the checked flag.
                    obj["isChecked"] = true;
                    isCheckSet = true;

                    // Update the initial entry in the table.
                    $scope.data.entry["fileName"] = obj["name"];
                }
                else {
                    obj["isChecked"] = false;
                }
                list.push(obj);
            }
            return list;
        }


        function _initializeLibrary() {

            gvodService.fetchFiles()

                .success(function (data) {
                    $log.info(data);
                    $scope.files = _reformatData(data);
                    $log.info($scope.files);
                    AlertService.addAlert({type: 'success', msg: 'Library Refreshed.'});
                })
                .error(function () {
                    $log.info("Unable to fetch files.");
                    AlertService.addAlert({type: 'warning', msg: 'Unable to Fetch the files.'});
                });
        }

        /**
         * Internal cleaning and removing old entries.
         * The house keeping is performed after every upload to clear
         * the previous entries metadata.
         *
         * @param data
         * @private
         */
        function _houseKeeping(data) {

            data.fileName = null;
            data.url = undefined;
            data.description = undefined;
            _resetFormStatus();
        }

        /**
         * The status associated with the form needs to be
         * reset as user will try to add a new entry and the validations
         * need to run again.
         * @private
         */
        function _resetFormStatus() {
            $scope.entryAdditionForm.$setPristine();
        }

        /**
         * ********************
         * Main Entry Point to initializing the
         * scope.
         * ********************
         *
         * @param scope
         * @private
         */
        function _initScope(scope) {

            // ==== INITIAL SETUP.

            scope.server = gvodService.getServer();
            scope.data = {

                entry: {
                    
                    fileName: null,
                    language: 'English',
                    fileSize: 1,
                    category: 'Video'
                }
            };

            _initializeLibrary();


            // ==== EVENTS REGISTRATION.
            scope.$on('server:updated', function (event, data) {

                $log.info('server updated');
                $log.info(data);

                scope.$apply(function () {

                    scope.server = gvodService.getServer();
                    AlertService.addAlert({type: 'success', msg: 'Server Details Updated.'});

                    _initializeLibrary();
                })

            });
        }

        /**
         * Main method of adding/home/babbar/workspace/mediasearch-interface the entries in the system. All
         * the entries needs to be
         */
        $scope.submitIndexEntry = function () {

            if (this.entryAdditionForm.$valid) {

                var lastSubmitEntry = $scope.data.entry;
                var uploadObj = {name: lastSubmitEntry.fileName, overlayId: parseInt(lastSubmitEntry.url)};

                gvodService.pendingUpload(uploadObj)

                    .then(function (response) {

                        $log.debug("gvod pending upload successful");
                        lastSubmitEntry.url = response.data.overlayId.toString();

                        return sweepService.addIndexEntry($scope.data);

                    }, function (error) {

                        $log.debug("Gvod Upload Failed ... ");
                        return $q.reject(error);
                    })

                    .then(function (success) {

                        $log.debug("Sweep successfully added the entries ..");

                        uploadObj.overlayId = parseInt(lastSubmitEntry.url);
                        return gvodService.upload(uploadObj);
                    },
                    function (error) {

                        $log.debug("Error pending upload: " + error);
                        return $q.reject(error);
                    })

                    // Gvod Upload Result Handling.
                    .then(function (success) {

                        $log.debug("Index Upload Successful");

                        _houseKeeping($scope.data.entry);
                        _initializeLibrary();

                        AlertService.addAlert({type: 'success', msg: 'Upload Successful.'});
                    },
                    function (error) {

                        $log.debug("Upload Unsuccessful" + error);
                        return $q.reject(error);
                    })

                    // Exception Handling.
                    .then(null, function (error) {
                        AlertService.addAlert({type: 'warning', msg: error});
                    })

            }
        };


        /**
         * Remove Entry from the Library.
         * @param entry
         */
        $scope.removeVideo = function (entry) {

            AlertService.addAlert({type: 'info', msg: 'Functionality under development.'});
            if (entry != null && entry.fileName != null) {
                //gvodService.removeVideo({name: entry.fileName, overlayId: -1});
            }
        };

        /**
         * Initialize the scope with
         * the correct parameters.
         */
        _initScope($scope);

    }

}());

