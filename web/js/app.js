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
    
    // angular.element(document.querySelector('[ng-controller=MainController]')).scope()
    function getControllerScope(ctrl) {
        return angular.element(document.querySelector('[ng-controller='+ctrl+']')).scope();
    }

    // Initialize app
    var BrkPokerApp = angular.module('brkPokerApp', [
        'ui.bootstrap', 
        'dialogs.main', 
        'btford.socket-io',
        'duScroll'
        // 'ui.slider'
    ]);
    
    BrkPokerApp.factory('socket', function (socketFactory) {
        var socket = io.connect('//' + window.location.host + '?authid=abc', {
            'try multiple transports' : false,
            'reconnection limit' : 5000,
            'reconnection delay' : 2000
        });
                
        // The library will stopped on first 50x http request
        // this hack will force it to reconnect.
        socket.on('error', function(err) {
            if (!socket.socket.reconnecting) {
                socket.socket.options['max reconnection attempts'] = Infinity;
                socket.socket.reconnect();
            }
        });
        
        socket.on('reconnecting', function(delay, attempts) {
//            console.log('Reconnecting attempt #'+attempts+', delay for '+delay+'ms');
        });
        
        return socketFactory({
            ioSocket: socket
        });
    })
    
    BrkPokerApp.controller('MainController', function(
            $timeout, 
            $scope, 
            $rootScope, 
            dialogs, 
            socket
        ) {
        /* Get this from session or URL query i guess */
        $scope.authid = "";
        $scope.is_loading = true;
        $scope.is_connected = false;
        $scope.is_playing = false;
        $scope.is_watching = false;
	$scope.dc_count = 0;
        $scope.chat_line = "";
        $scope.name = "";
        $scope.is_debug = false;
	$scope.cash = 0; // player chips
        
        // Access players from getPlayer(seat_num)
        $scope.players = [null];
        for(var seatNum = 1; seatNum <= 9; seatNum++) {
            $scope.players[seatNum] = createPlayerPanel(seatNum);
        }
        console.log("Player panels initialized");

        resetTable();

        if($scope.is_debug)  {
            $timeout(function() {
                $scope.is_connected = true;
                $scope.is_watching = false;
                $scope.is_playing = true;
                $scope.is_action = true;
                $scope.allowed_actions = ["Raise", "Check", "Fold", "All-in"];
		$scope.community_cards = [13, 14, 15, -1, null];
            }, 1000);
        }
        
        var imagesUploaded = 0;
        var imagesStorage = [];
        var addActionLogTimeout = false;
        var addChatTimeout = false;
        
        
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

        
        /* Fields Monitors */
        
        if(!$scope.is_debug) 
        $scope.$watch('is_connected', function(newVal) {
            if (!$scope.is_connected) {
                resetTable();
                // disconnected, show connecting modal
                connect();
            }
        });

        $scope.$watch('name', function(newVal, oldVal) {
            if ((oldVal != "") && $scope.is_connected) {
                socket.emit('set:name', $scope.name);
            }
        });
        
        /* Sockets Messages Handlers */

        // User connected
        socket.on('connect', function() {
           $scope.is_connected = true;
           addActionLog("Connected to engine server");
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

            addActionLog("User authenticated");

            $scope.name = data.name;
            $scope.cash = data.cash;
	    
	    console.log("Name: "+data.name+", cash: $"+data.cash);

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
            $scope.mode = "watch";

            addActionLog("Watching table "+data);
        });

        /**
         * Start playing on a table
         */
        socket.on('sit', function(data) {
            $scope.is_watching = false;
            $scope.is_playing = true;
            $scope.mode = "play";

            var player = getPlayer(data.num);
            player.update(data);
	    player.is_loading = false;
	    $scope.seat_num = data.num;

            addActionLog('Client is now playing as '+data.name);
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
            player.cards = [null, null];

            addActionLog('Player '+data.name+' joined');
        });
        
        /**
         * Player take a stand from his seat
         */
        socket.on('player:leave', function(data) {
            var player = getPlayer(data.num);
            player.reset();
	    player.is_available = !$scope.is_playing;
            addActionLog('Player '+data.name+' leaved table');
        });
        
        /**
         * Player taking an action
         */
        socket.on('player:act', function(data) {
            var player = getPlayer(data.num);
            player.last_action = data.last_action;
            player.bet = data.bet;
	    if(data.last_action == "Fold")
		player.cards = [null, null];

            addActionLog("Action by "+player.name+": "+data.last_action);
        });
        
        /**
         * Player disconnected
         */
        socket.on('player:dc', function(data) {
            var player = getPlayer(data.num);
            addActionLog("Player "+player.name+" disconnected");
            player.reset();
	    player.is_available = !$scope.is_playing;
        });
        
        /**
         * Player updated
         */
        socket.on('player:update', function(data) {
            var player = getPlayer(data.num);
            player.update(data);
	    if(data.last_action == "fold")
		player.cards = [null, null];
	    else if (player.has_cards && data.cards.length == 0)
		player.cards = [-1, -1];
        });

        /**
         * Game start with a new hand
         * data.dealer => seat number of dealer
         */
        socket.on('game:start', function(data) {
            if ($scope.dealer_pos > 0) {
                getPlayer($scope.dealer_pos).update({
                    is_dealer: false,
                    is_turn: false
                });
            }
            $scope.dealer_pos = data.num;
            var dealer = getPlayer($scope.dealer_pos);
            dealer.update({
                is_dealer: true,
                is_turn: true
            });
	    $scope.community_cards = [null, null, null, null, null];
	    for (var num = 1; num <= 9; num++) {
		var player = getPlayer(num);
		if (player.is_playing)
		    player.last_action = "";
	    }

            addActionLog("Game started, dealer: "+dealer.name);
        });

        /**
         * Repaint table
         */
        socket.on('game:repaint', function(data) {
            resetTable();
            $scope.table = data.name;
            $scope.min = data.min;
            $scope.max = data.max;
            $scope.small = data.small;
            $scope.big = data.big;
            $scope.message = data.msg;
            $scope.bet = data.bet;
            $scope.pot = data.pot;
	    $scope.seat_num = 0;
            $scope.community_cards = [null, null, null, null, null];
            if ($scope.cards != undefined) {
                for (var i = 0; i < data.cards.length; i++)
                    $scope.community_cards[i] = data.cards[i];
            }

            addActionLog('Table '+data.name+' repainted');
        });

	/**
	 * On game:rotate
	 */
        socket.on('game:rotate', function(data) {
	    if ($scope.actor_pos > 0)
		getPlayer($scope.actor_pos).is_turn = false;
            getPlayer(data.num).is_turn = true;
            $scope.actor_pos = data.num;

	    console.log("Player rotated to seat "+data.num);
        });

        socket.on('game:update', function(data) {
            $scope.bet = data.bet;
            $scope.pot = data.pot;
	    
	    for (var i = 0; i < data.cards.length; i++)
		$scope.community_cards[i] = data.cards[i];

            console.log('Cards updated');
        });
        
        /**
         * Request this player to act
         */
        socket.on('game:act', function(data) {
            $scope.min_bet = data.min;
            $scope.bet_amount = data.min;
            $scope.allowed_actions = data.actions;
            $scope.is_action = true;
	    // Clear last action
	    getPlayer($scope.seat_num).last_action = "";

            console.log('Request action to player');
        });
        
            
        socket.on('player:won', function(data) {
            var player = getPlayer(data.num);
            player.cash = data.cash;
            player.last_action = "winner";

            addActionLog("Player "+player.name+" wins "+data.amount);
        });
	
	socket.on('cash', function(data) {
            $scope.cash = data;

            addActionLog("Updated cash: $"+data);
        });

        socket.on('chat', function(data) {
            newChatMsg(data.name, data.msg);
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
        
        if(!$scope.is_debug) 
        socket.on('error', function(err) {
            if ($scope.is_connected)
                $scope.is_connected = false;
            console.log('Socket error');
        });

        $scope.isInGame = function() {
            var is_in_game = $scope.is_connected && ($scope.is_watching || $scope.is_playing);
            console.log("I am " + (is_in_game ? "in" : "not in") + " game");
            return is_in_game;
        };
        
        $scope.isActionAllowed = function(action) {
            return ($scope.allowed_actions.indexOf(action) >= 0);
        };

        $scope.act = function(action) {
	    $scope.is_action = false;
	    console.log("Action: "+action);
            var obj = {};
            obj.name = action;
            if(action == "Bet" || action == "Raise")
                obj.amount = $scope.bet_amount;
            socket.emit('act', obj);
        };

        $scope.sit = function(num) {
            console.log("Sitting at table + "+num+" min="+$scope.min+" max="+$scope.max);

            var dlg = dialogs.create('player_buyin.html?','buyinDialogCtrl',{min: $scope.min, max: $scope.max, amount: $scope.min},'sm');
            dlg.result.then(function(buyin){
                console.log("Buyin "+buyin);
		var player = getPlayer(num);
		player.is_loading = true;
		for (var i = 1; i <= 9; i++) {
		    var player = getPlayer(i);
		    if (i != num && !player.is_seated)
			player.is_available = false;
		}
                
                socket.emit('sit', {table: $scope.table, "num": num, buy: buyin});
            },function(){
                console.log("Cancelling buyin");
            });
        };
        
        $scope.newmsg = function() {
            console.log("New chat message");
            newChatMsg($scope.name, $scope.chat_line);
            socket.emit('chat', $scope.chat_line);
            $scope.chat_line = "";
        };

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
            $scope.table = "";
            
            $scope.is_action = false;
            $scope.mode = ""; // "play" or "watch"
            $scope.message = "";
            $scope.min = 0;
            $scope.max = 0;
            $scope.small = 0;
            $scope.big = 0;
            $scope.bet = 0; // bets
            $scope.pot = 0; // total pot
            $scope.community_cards = [null, null, null, null, null];

            $scope.actor_pos = 0;
            $scope.dealer_pos = 0;

            $scope.allowed_actions = [];

            $scope.slider_min = 0;
            $scope.slider_max = 0;
            $scope.bet_amount = 0;

            $scope.buy_in = 0;

            $scope.action_logs = [];
            $scope.chat_messages = [];

            for (var num = 1; num <= 9; num++) {
                getPlayer(num).reset();
            }
        }

        function newChatMsg(name, msg) {
            $scope.chat_messages.push([name, msg]);
            console.log("CHAT: "+msg);

            if (!addChatTimeout)
            {
                addChatTimeout = true;
                $timeout(function() {
                    var el = document.getElementById('chat_content');
                    var box = angular.element(el);
                    var real_height = el.scrollHeight;
                    if ((el.scrollTop + el.offsetHeight) < real_height)
                        box.scrollTop(real_height, 500);
                    addChatTimeout = false;
                }, 200);
                
            }
        }
        
        function addActionLog(msg) {
            $scope.action_logs.push(msg);
            console.log("LOG: "+msg);

            if (!addActionLogTimeout)
            {
                addActionLogTimeout = true;
                $timeout(function() {
                    var el = document.getElementById('action_log_content');
                    var box = angular.element(document.getElementById('action_log_content'));
                    var real_height = el.scrollHeight;
                    if ((el.scrollTop + el.offsetHeight) < real_height)
                        box.scrollTop(real_height, 500);
                    addActionLogTimeout = false;
                }, 200);
                
            }
        }

        /**
         * Creates a player object
         * @param int num
         * @returns {_L63.createPlayerPanel.tbl_info}
         */
        function createPlayerPanel(num) {
            var tbl_info = {
                "num": num,
                sclass: "player_box col-md-2",
//                sit: function(){
//                    var dlg = dialogs.create('player_buyin.html?',
//                        'buyinDialogCtrl',
//                        {min: $scope.min, max: $scope.max},
//                        'sm'
//                    );
//                    dlg.result.then(function(buyin){
//                        this.is_loading = true;
//                        console.log("Buyin "+buyin);
//
//                        socket.emit('sit', {table: $scope.table, "num": num, buy: buyin});
//                    },function(){
//			this.is_loading = false;
//                        console.log("Cancelling buyin");
//                    });
//                },
                update: function(data) {
                    for (var key in data) {
                        this[key] = data[key];
                    }
                },
                reset: function() {
                    this.update({
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
                        last_action: ""
                        
                    });
                }
            };

            tbl_info.reset();
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

        /**
         * Get player instance
         * @param {type} num
         * @returns {unresolved}
         */
        function getPlayer(num) {
            return $scope.players[num];
        }
        
        console.log("Main controller initialized");
    });
    

    BrkPokerApp.controller('buyinDialogCtrl',function($scope,$modalInstance,data){
        //-- Variables --//

        $scope.buy = data;

        //-- Methods --//
        
        $scope.cancel = function(){
            $modalInstance.dismiss('Canceled');
        }; // end cancel
        
        $scope.save = function(){
            $modalInstance.close($scope.buy.amount);
        }; // end save
        
        $scope.hitEnter = function(evt){
            if(angular.equals(evt.keyCode,13) 
                && !(angular.equals($scope.buy.amount,null) 
                || angular.equals($scope.buy.amount,'')))
                $scope.save();
        };
        
        $scope.slider = {
            'options': {
		start: function (event, ui) { 
                    console.log('Slider start');
                } 
            },
            stop: function (event, ui) { 
                console.log('Slider stop'); 
            }
        }
        
        
    }) // end controller(customDialogCtrl)

    
    BrkPokerApp.directive('playerPanel', function() {
        return {
            restrict: 'E',
            scope: {
                player: '=',
		sit: '&'
            },
            templateUrl: 'player_panel.html',
	    controller: function($scope) {
		$scope.player.controller = $scope;
		
		$scope.reset = function() {
		    
		}
	    },
	    link: function(scope, elem, attrs) {
		
	    }
        };
    });
    
    BrkPokerApp.directive('cardImage', function() {
        return {
            restrict: 'E',
            replace: true,
            scope: {
                ngCard: '='
            },
            template: '<img ng-src="{{img(ngCard)}}"/>'
            ,
            controller: ['$scope', function($scope) {
		    $scope.img = function(card) {
			return getCardImg(card);
		    }
            }]
        }
    });
    


})();


