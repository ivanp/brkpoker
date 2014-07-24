(function() {

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

    BrkPokerApp.controller('BoardPanelController', function($scope, $timeout) {
        $scope.bet = 10;
        $scope.pot = 20;

        $scope.cards = [null, null, null, null, null];

        $scope.getCardIndex = function(card) {
            var idx;
            if (card == null)
                idx = 'back';
            else if (card.toString().length == 1)
                idx = '0' + card.toString();
            else
                idx = card.toString();
            return idx;
        }

//        $timeout(function() {
//            $scope.cards[0] = 3;
//            console.log('Changing card');
//        }, 2000);
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
        $scope.p1 = createPlayerPanel(1);
        $scope.p2 = createPlayerPanel(2);
        $scope.p3 = createPlayerPanel(3);
        $scope.p4 = createPlayerPanel(4);
        $scope.p5 = createPlayerPanel(5);
        $scope.p6 = createPlayerPanel(6);
        $scope.p7 = createPlayerPanel(7);
        $scope.p8 = createPlayerPanel(8);
        $scope.p9 = createPlayerPanel(9);
        console.log("player: "+$scope.p1.num);
        console.log("name: "+$scope.p1.name);
        
        function createPlayerPanel(num) {
            var tbl_info = {
                "num": num,
                name: "Player "+num,
                cash: 0,
                action: "",
                bet: 0,
                card1: null,
                card2: null,
                is_dealer: false,
                is_smallblind: false,
                is_bigblind: false,
                sclass: "player_box col-md-2"
            };
            switch(num) {
                case 8:
                    tbl_info.sclass = tbl_info.sclass + " col-md-offset-2";
                    break;
                case 7:
                    tbl_info.sclass = tbl_info.sclass + " col-md-offset-1";
                    break;  
            }
            return tbl_info;
        }
    });

    BrkPokerApp.directive('playerPanel', function() {
        return {
            restrict: 'E',
            scope: {
                playerInfo: '=num'
            },
            templateUrl: 'player_panel.html'
        };
    });
})();