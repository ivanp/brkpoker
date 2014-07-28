(function() {

    'use strict';
    
    // Helpers
    function getCardImg(card_int) {
        var idx;
        // null means no card or not currently playing
        if (card_int == null)
            idx = 'placeholder';
        // -1 is back of the card
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
        var socket = io.connect('//' + window.location.host + '?authid=abc', {
            'try multiple transports' : false,
            'reconnection limit' : 5000,
            'reconnection delay' : 2000
        });
                
        socket.on('error', function(err) {
            if (!socket.socket.reconnecting) {
                socket.socket.options['max reconnection attempts'] = Infinity;
                socket.socket.reconnect();
            }
        });
        
        socket.on('reconnecting', function(delay, attempts) {
            console.log('Reconnecting attempt #'+attempts+', delay for '+delay+'ms');
        });
        
        return socketFactory({
            ioSocket: socket
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
        $scope.name = "";
        $scope.table = "";
        $scope.is_loading = true;
        $scope.is_connected = false;
        $scope.is_playing = false;
        $scope.is_watching = false;
        $scope.mode = ""; // "play" or "watch"
        $scope.dc_count = 0;
        $scope.message = "";
        
        $scope.chips = 0;
        $scope.min = 0;
        $scope.max = 0;
        $scope.small = 0;
        $scope.big = 0;
        
        $scope.bet = 0;
        $scope.pot = 0;
        $scope.community_cards = [null, null, null, null, null];

        $scope.actor_pos = 0;

        $scope.allowedActions = [];
        
        // Access players from getPlayer(seat_num)
        $scope.players = [null];
        for(var seatNum = 1; seatNum <= 9; seatNum++) {
            $scope.players[seatNum] = createPlayerPanel(seatNum);
        }
        console.log("Player panels initialized");
        
        var imagesUploaded = 0;
        var imagesStorage = [];
        
        
        console.log("Main controller initialized");
        
        
        // Load wait dialog
        $timeout(function() {
            // Remove boot loader
            angular.element(document.querySelector('#bootloader')).remove();
                
            dialogs.wait("Loading", "Loading assets", 0, {
                backdrop: 'static'
            });
            
            loadImages();
            checkImagesProgress();
            
        }, 100);

        
        function connect() {
            console.log("Loading is " + ($scope.is_loading ? 'on' : 'off'));
            if ($scope.is_connected || $scope.is_loading)
                return;
            
            var header, msg;
            if ($scope.dc_count) {
                header = "BRKPoker";
                msg = "You were disconnected from the server, please wait while we're trying to reconnect."
            } else {
                header = "BRKPoker";
                msg = "Connecting to server, please wait."
            }
            dialogs.wait(header, msg, 5, {
                backdrop: 'static'
            });
        }


        /* Fields Monitors */
        
        $scope.$watch('is_connected', function(newVal) {
            if (!$scope.is_connected) {
                // disconnected, show connecting modal
                connect();
            }
        });
        
        /* Sockets Messages Handlers */

        // User connected
        socket.on('connect', function() {
           $scope.is_connected = true;
           console.log("Connected to engine server");
           $rootScope.$broadcast('dialogs.wait.progress',{
                'progress' : 30, 
                'msg': 'Successfully connected to server. Waiting for authentication process.'});
        });

        // Receives authentication
        // @TODO: check authentication info
        socket.on('auth', function(data) {
            $rootScope.$broadcast('dialogs.wait.progress',
                {'progress' : 70, 
                'msg': 'Authentication successful. Joining table T001.'});

            // @TODO: Get tables and list in a modal
            socket.emit('watch', 'T001');
        });
        
        
        /**
         * Watching/spectating a table, along with
         * table information
         */
        socket.on('watch', function(data) {
            $rootScope.$broadcast('dialogs.wait.progress',
                {'progress' : 90, 
                'msg': 'Joined table '+data+'. Receiving table stream.'});
            $timeout(function() {
                $rootScope.$broadcast('dialogs.wait.complete');
            }, 2000);

            $scope.is_playing = false;
            $scope.is_watching = true;
            $scope.table = data;
        });

        /**
         * Start playing on a table
         */
        socket.on('sit', function(data) {
            $scope.is_watching = false;
            $scope.is_playing = true;
        });

        /**
         * Another player taking a seat
         * data.num = seat number
         * data.name  player name
         * data.cash  player coin
         */
        socket.on('player:join', function(data) {
            var player = getPlayer(data.num);
            player.update(data);
            console.log('Player '+data.name+' joined');
        });
        
        /**
         * Player take a stand from his seat
         */
        socket.on('player:leave', function(data) {
            var player = getPlayer(data.num);
            player.reset();
            console.log('Player '+data.name+' leaved table');
        });
        
        /**
         * Player taking an action
         */
        socket.on('player:action', function(data) {
            
        });
        
        /**
         * Player disconnected
         */
        socket.on('player:dc', function(data) {
            
        });
        
        socket.on('player:update', function(data) {

        });

        /**
         * Game start with a new hand
         * data.dealer => seat number of dealer
         */
        socket.on('game:start', function(data) {

        });

        /**
         * Repaint table
         */
        socket.on('game:repaint', function(data) {

        });

        socket.on('game:rotate', function(data) {
            console.log("Player rotated to seat "+data.num);
            getPlayer($scope.actor_pos).is_turn = false;
            getPlayer(data.num).is_turn = true;
            $scope.actor_pos = data.num;
        });
        
        /**
         * Request this player to act
         */
        socket.on('game:reqact', function(data) {
            
        });
        
            
        socket.on('game:winner', function(data) {
            
        });

        socket.on('chat', function(data) {
            
        });
        
        socket.on('msg', function(data) {
            $scope.message = data;
        });

        socket.on('disconnect', function() {
            if ($scope.is_connected)
                $scope.is_connected = false;
            $scope.dc_count++;
            console.log("Disconnected from engine server");
        });
        
        socket.on('error', function(err) {
            if ($scope.is_connected)
                $scope.is_connected = false;
            console.log('Socket error');
        });
        
        
        $scope.$watch('name', function(newVal) {
            if ($scope.is_connected) {
                socket.emit('set:name', $scope.name);
            }
        });
        
        function loadImages() {
            // Put all images here
            var images = [
                getCardImg(null),
                'img/table.jpg',
                'img/bigblindPuck.png',
                'img/smallblindPuck.png',
                'img/dealerPuck.png',
                'img/action/action_allin.png',
                'img/action/action_bet.png',
                'img/action/action_call.png',
                'img/action/action_check.png',
                'img/action/action_fold.png',
                'img/action/action_raise.png',
                'img/action/action_winner.png'
            ];
            
            // Card pictures
            for (var card_idx = -1; card_idx < 52; card_idx++) {
                images.push(getCardImg(card_idx));
            }
            
            for (var img_src in images) {
                var card_img = new Image();
                card_img.onload = function() {
                    imagesUploaded++;
                }
                card_img.src = images[img_src];
                imagesStorage.push(card_img);
            }
        }
        
        function checkImagesProgress() {
            if(imagesUploaded < imagesStorage.length-1) {
                $rootScope.$broadcast('dialogs.wait.progress',{'progress' : (imagesUploaded / imagesStorage.length) * 100});
                $timeout(checkImagesProgress, 200);
            } else {
                console.log('Assets loaded');
                $rootScope.$broadcast('dialogs.wait.complete');
                // Default was hidden
                angular.element(document.querySelector('body')).attr('style', 
                    'background: url(\'img/table.jpg\') no-repeat center bottom fixed');
                angular.element(document.querySelector('#header')).attr('style', '');
                angular.element(document.querySelector('#content')).attr('style', '');
                angular.element(document.querySelector('#footer')).attr('style', '');
                // Done loading
                $scope.is_loading = false;
                // Check connection
                connect();
            }
        }

        function resetTable() {
            $scope.bet = 0;
        }
        
        function createPlayerPanel(num) {
            var tbl_info = {
                sit: function() {
                    console.log("Sitting at table + "+num);
                    for (var idx in $scope.players) {
                        var tbl = $scope.players[idx];
                        if (tbl.num == num)
                            tbl.is_loading = true;
                        else
                            tbl.is_available = false;
                    }
                },
                update: function(data) {
                    for (var key in data) {
                        this[key] = data[key];
                    }
                },
                reset: function() {
                    this.update({
                        "num": num,
                        name: "",
                        avatar: "",
                        cash: 0,
                        action: "",
                        bet: 0,
                        cards: [null, null],
                        // Is it available to sit on?
                        is_available: true,
                        // Player clicked sit, waiting status 
                        // from server
                        is_loading: false,
                        // Player sit, but doesn't mean already playing,
                        // can be also waiting for his turn
                        is_seated: false,
                        // Player is currently on the game
                        has_cards: false,
                        // It's player turn
                        is_turn: false,
                        is_dealer: false,
                        is_smallblind: false,
                        is_bigblind: false,
                        is_winning: false,
                        last_action: "",
                        sclass: "player_box col-md-2"
                    });
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
            tbl_info.reset();
            return tbl_info;
        }

        function getPlayer(num) {
            return $scope.players[num];
        }

        
        
        $scope.isActionAllowed = function(action) {
            return ($scope.allowedActions.indexOf(action) >= 0);
        };
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


