(function () {

    'use strict';

    // Your application goes here.
    var BrkPokerApp = angular.module('brkPokerApp', ['ui.bootstrap']);
    
    BrkPokerApp.controller('MainController', function($scope) {
        $scope.table_info = {
            chips: 0,
            name: "T001",
            min: 4000,
            max: 8000,
            small: 200,
            big: 400
        };
    });
    
    BrkPokerApp.controller('BoardPanelController', function($scope) {
        $scope.table_info = {
            bet: 0,
            pot: "T001",
            cards: [],
            message: ""
        };
    });
    
    BrkPokerApp.controller('ControlPanelController', function($scope) {
        $scope.table_info = {
            bet: 0,
            pot: "T001",
            cards: [],
            message: ""
        };
    });
    
    BrkPokerApp.controller('PlayerPanelController', function($scope) {
        $scope.table_info = {
            bet: 0,
            pot: "T001",
            cards: [],
            message: ""
        };
    });
})();