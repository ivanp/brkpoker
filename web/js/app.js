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
        
//        $timeout(function() {
//            angular.element(document.querySelector('#content')).attr('style', '');
//            $scope.is_connected = true;
//            console.log('Showing');
//        }, 2000);
        
        console.log("Main controller initialized");
        
        $scope.imagesUploaded = 0;
        $scope.imagesStorage = [];
        $scope.loadImages = function() {
            
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
            
            var ph_img = new Image();
            ph_img.src = getCardImg(null);
            $scope.imagesStorage.push(ph_img);
            
            for (var card_idx = -1; card_idx < 52; card_idx++) {
                var card_img = new Image();
                card_img.onload = function() {
                    $scope.imagesUploaded++;
                }
                card_img.src = getCardImg(card_idx);
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

    BrkPokerApp.controller('BoardPanelController', function($scope, $timeout) {
        $scope.bet = 0;
        $scope.pot = 0;
        $scope.cards = [null, null, null, null, null];

        console.log("Board panel initialized");
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
            replace: true,
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
setTimeout(function() {
    var appElement = document.querySelector('[ng-controller=PlayerPanelController]');
    var $scope = angular.element(appElement).scope();
    $scope.$apply(function() {
        $scope.p1.seated = true;
        $scope.p1.card1 = 5;
        $scope.p1.card2 = 51;
    });
}, 1000);