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
    var BrkPokerApp = angular.module('brkPokerApp', ['ui.bootstrap', 'dialogs.main', 'btford.socket-io']);
    
    BrkPokerApp.factory('socket', function (socketFactory) {
        return socketFactory({
            ioSocket: io.connect('//' + window.location.host + '?authid=abc')
        });
    })
    
    // Factories and services
    BrkPokerApp.factory('getCardImg', function() {
        return function(card_int) {
            return getCardImg(card_int);
        };
    });

    BrkPokerApp.controller('MainController', function($timeout, $scope, $rootScope, dialogs, socket) {
        /* Get this from session or URL query i guess */
        $scope.authid = "";
        // Generate guest name
        $scope.name = "Guest" + (Math.floor(Math.random() * 90000) + 10000);
        $scope.is_connected = true;
        $scope.is_playing = false;
        
        $scope.bet = 0;
        $scope.pot = 0;
        $scope.community_cards = [null, null, null, null, null];
        
        $scope.table_info = {
            chips: 0,
            name: "",
            min: 0,
            max: 0,
            small: 0,
            big: 0
        };
        
        $scope.players = [];
        for(var tbl_num = 1; tbl_num <= 9; tbl_num++) {
            $scope.players.push(createPlayerPanel(tbl_num));
        }
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
                // Is it available to sit on?
                is_available: true,
                // Player clicked sit, waiting status 
                // from server
                is_loading: false,
                // Player sit, but doesn't mean already playing,
                // can be also waiting for his turn
                is_seated: false,
                // Player is currently on the game
                is_playing: false,
                // It's player turn
                is_turn: false,
                is_dealer: false,
                is_smallblind: false,
                is_bigblind: false,
                is_winning: false,
                last_action: "",
                sclass: "player_box col-md-2"
                ,
                sit: function() {
                    console.log("Sitting at table + "+num);
                    for (var idx in $scope.players) {
                        var tbl = $scope.players[idx];
                        if (tbl.num == num)
                            tbl.is_loading = true;
                        else
                            tbl.is_available = false;
                    }
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
        
        // Default was hidden
        angular.element(document.querySelector('#header')).attr('style', '');
        
//        $timeout(function() {
//            angular.element(document.querySelector('#content')).attr('style', '');
//            $scope.is_connected = true;
//            console.log('Showing');
//        }, 2000);
        
        console.log("Main controller initialized");
        
        $scope.imagesUploaded = 0;
        $scope.imagesStorage = [];
        $scope.loadImages = function() {
            // Put all images here
            var images = [
                getCardImg(null),
                'img/bigblindPuck.png',
                'img/smallblindPuck.png',
                'img/dealerPuck.png',
            ];
            
            var bg_img = new Image();
            bg_img.onload = function() {
                // Remove boot loader
                angular.element(document.querySelector('#bootloader')).remove();
                // Display bg
                angular.element(document.querySelector('body')).attr('style', 'background: url(\'img/table.jpg\') no-repeat center bottom fixed');
                $scope.imagesUploaded++;
            }
            bg_img.src = 'img/table.jpg';
            $scope.imagesStorage.push(bg_img);
            
            // Card pictures
            for (var card_idx = -1; card_idx < 52; card_idx++) {
                images.push(getCardImg(card_idx));
            }
            
            for (var img_src in images) {
                var card_img = new Image();
                card_img.onload = function() {
                    $scope.imagesUploaded++;
                }
                card_img.src = images[img_src];
                $scope.imagesStorage.push(card_img);
            }
        }
        $scope.checkImagesProgress = function() {
            if($scope.imagesUploaded < $scope.imagesStorage.length-1) {
                $rootScope.$broadcast('dialogs.wait.progress',{'progress' : ($scope.imagesUploaded / $scope.imagesStorage.length) * 100});
                $timeout($scope.checkImagesProgress, 100);
            } else {
                console.log('Assets loaded');
                $rootScope.$broadcast('dialogs.wait.complete');
                angular.element(document.querySelector('#content')).attr('style', '');
                angular.element(document.querySelector('#footer')).attr('style', '');
            }
        }

        
        // Load wait dialog
        $timeout(function() {
            dialogs.wait("Loading", "Loading assets", 0, {
                backdrop: 'static'
            });
            
            $scope.loadImages();
            $scope.checkImagesProgress();
            
        }, 100);
        
        // Sockets
        socket.on('connect', function() {
           $scope.is_connected = true;
           console.log("Connected to engine server");
           
           /* @TODO: Loading table dialog */
        });
        
        socket.on('disconnect', function() {
           $scope.is_connected = false; 
           console.log("Disconnected from engine server");
        });
        
        $scope.$watch('name', function(newVal) {
            if ($scope.is_connected) {
                socket.emit('set:name', $scope.name);
            }
        });
    });

    BrkPokerApp.directive('playerPanel', function() {
        return {
            restrict: 'E',
            scope: {
                player: '='
            },
            templateUrl: 'player_panel.html'
        };
    });
    
    BrkPokerApp.directive('cardImage', function() {
        return {
            restrict: 'AE',
            replace: true,
            scope: {
                ngCard: '='
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
    
    
    /* Control $scope from outside */
setTimeout(function() {
//    var appElement = document.querySelector('[ng-controller=PlayerPanelController]');
    var $scope = getControllerScope('MainController');
    $scope.$apply(function() {
        $scope.players[0].is_seated = true;
        $scope.players[0].is_playing = true;
        $scope.players[0].is_turn = true;
        $scope.players[0].card1 = 5;
        $scope.players[0].card2 = 15;
        $scope.players[0].is_smallblind = true;
        $scope.players[0].last_action = 'allin';
        
//        $scope['p9'].is_available = false;
//        $scope.p1.seated = true;
//        $scope.p1.card1 = 5;
//        $scope.p1.card2 = 51;
    });
}, 1000);


})();


