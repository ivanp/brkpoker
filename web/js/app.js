(function() {

    'use strict';
    
    // Helpers
    function getCardImg(card_int) {
            var idx;
        if (card_int == null)
            idx = 'placeholder';
        else if (card_int == -1)
            idx = 'back';
        else if (card_int.toString().length == 1)
            idx = '0' + card_int.toString();
        else
            idx = card_int.toString();
        return "img/cards/card_" + idx +".png";
    };
    
    function getRootScope() {
        return angular.element(document.querySelector('[ng-app=brkPokerApp]')).scope();
    }
    
    function getControllerScope(ctrl) {
        return angular.element(document.querySelector('[ng-controller='+ctrl+']')).scope();
    }

    // Initialize app
    var BrkPokerApp = angular.module('brkPokerApp', ['ui.bootstrap', 'dialogs.main']);
    
    // Factories and services
    BrkPokerApp.factory('getCardImg', function() {
        return function(card_int) {
            return getCardImg(card_int);
        };
    });

    BrkPokerApp.controller('MainController', function($timeout, $scope, $rootScope, dialogs) {
        /* Get this from session or URL query i guess */
        $scope.authid = "";
        // Generate guest name
        $scope.name = "Guest" + (Math.floor(Math.random() * 90000) + 10000);
        $scope.is_connected = false;
        $scope.is_playing = false;
        $scope.table_info = {
            chips: 0,
            name: "",
            min: 0,
            max: 0,
            small: 0,
            big: 0
        };
        
        // Default was hidden
        angular.element(document.querySelector('#header')).attr('style', '');
        angular.element(document.querySelector('#content')).attr('style', '');
        angular.element(document.querySelector('#footer')).attr('style', '');
//        $timeout(function() {
//            angular.element(document.querySelector('#content')).attr('style', '');
//            $scope.is_connected = true;
//            console.log('Showing');
//        }, 2000);
        
        console.log("Main controller initialized");
        
        var dlg = dialogs.wait("Loading", "Loading assets", 5, {
            backdrop: 'static'
        });
        
        var imgs = [];
        var img = new Image();
        img.src = getCardImg(null);
        for (var card_idx = -1; card_idx < 52; card_idx++) {
            var card_img = new Image();
            card_img.src = getCardImg(card_idx);
            imgs.push(card_img);
        }
        console.log("Assets loaded");
        
        $timeout(function() {
            getRootScope().$broadcast('dialogs.wait.progress',{'progress' : 30, 'msg': 'Connecting to server'});
        }, 2000);
    });

    BrkPokerApp.controller('BoardPanelController', function($scope, $timeout) {
        $scope.bet = 0;
        $scope.pot = 0;
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
//            $scope.cards[0] = 0;
//            $scope.cards[1] = 32;
//            $scope.cards[2] = 44;
//            $scope.cards[3] = 12;
//            $scope.cards[4] = 16;
//            console.log('Changing card');
//        }, 2000);

        console.log("Board panel initialized");
    });

    BrkPokerApp.controller('ControlPanelController', function($scope) {
        $scope.table_info = {
            bet: 0,
            pot: "T001",
            cards: [],
            message: ""
        };
        
        console.log("Control panel initialized");
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
        console.log("Player panels initialized");
        
        function createPlayerPanel(num) {
            var tbl_info = {
                "num": num,
                name: "Player "+num,
                avatar: "",
                cash: 0,
                action: "",
                bet: 0,
                card1: null,
                card2: null,
                seated: false,
                is_dealer: false,
                is_smallblind: false,
                is_bigblind: false,
                is_loading: false,
                sclass: "player_box col-md-2",
                btnSit: function(num) {
                    console.log("Sitting at table + "+num);
                }
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
    
    BrkPokerApp.directive('cardImage', function() {
        return {
            restrict: 'AE',
            scope: {
                ngCard: '@'
            },
            template: '<img ng-src="{{ngCardImg}}"/>'
            ,
            controller: ['$scope', function($scope) {
                    // Default
                    $scope.ngCardImg = getCardImg(null);
            }]
            ,
            "link": function(scope, elem, attrs, ctrl) {
                scope.$watch('ngCard', function(newVal) {
                    if (newVal) {
                        scope.ngCardImg = getCardImg(scope.ngCard);
                    }
                });
            }
        }
    });
})();

/* Control $scope from outside */
//setTimeout(function() {
//    var appElement = document.querySelector('[ng-controller=PlayerPanelController]');
//    var $scope = angular.element(appElement).scope();
//    $scope.$apply(function() {
//        $scope.p1.seated = true;
//    });
//}, 3000);