// This file is part of the 'texasholdem' project, an open source
// Texas Hold'em poker application written in Java.
//
// Copyright 2009 Oscar Stigter
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// This file is part of the 'texasholdem' project, an open source
// Texas Hold'em poker application written in Java.
//
// Copyright 2009 Oscar Stigter
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.ozsoft.texasholdem;

import com.brkpoker.texasholdem.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.ozsoft.texasholdem.actions.Action;
import org.ozsoft.texasholdem.actions.BetAction;
import org.ozsoft.texasholdem.actions.RaiseAction;

/**
 * Limit Texas Hold'em poker table. <br />
 * <br />
 * 
 * This class forms the heart of the poker engine. It controls the game flow for a single poker table.
 * 
 * @author Oscar Stigter
 */
public class Table implements Runnable {
    
	protected static final int MAX_PLAYERS = 9;
			
    /** In fixed-limit games, the maximum number of raises per betting round. */
    protected static final int MAX_RAISES = 3;
    
    /** Whether players will always call the showdown, or fold when no chance. */
    protected static final boolean ALWAYS_CALL_SHOWDOWN = true;
	
	protected final int sleepBetweenStage = 2000;
	
	protected final String name;
    
    /** Table type (poker variant). */
    protected final TableType tableType;
    
    /** The size of the big blind. */
    protected final int bigBlind;
    
    /** The players at the table. */
    protected final TreeMap<Integer, Player> players;
    
    /** The active players in the current hand. */
    protected final List<Player> activePlayers;
	
	/** The spectators */
	protected final List<User> spectators;
    
    /** The deck of cards. */
    protected final Deck deck;
    
    /** The community cards on the board. */
    protected final List<Card> board;
    
    /** The current dealer position. */
    protected int dealerPosition;

    /** The current dealer. */
    protected Player dealer;

    /** The position of the acting player. */
    protected int actorPosition;
    
    /** The acting player. */
    protected Player actor;
	
    /** The minimum bet in the current hand. */
    protected int minBet;
    
    /** The current bet in the current hand. */
    protected int bet;
    
    /** All pots in the current hand (main pot and any side pots). */
    protected final List<Pot> pots;
    
    /** The player who bet or raised last (aggressor). */
    protected Player lastBettor;
    
    /** Number of raises in the current betting round. */
    protected int raises;
	
	private Object waitForPlayerLock = new Object();
	
	protected String message = "";
	
	protected boolean showdown = false;
	
	protected final int minBuy;
	protected final int maxBuy;

	protected Player smallBlindPlayer;
	protected Player bigBlindPlayer;
	
	protected Map<Integer, Player> removeThesePlayers;
    
    /**
     * Constructor.
     * 
     * @param bigBlind
     *            The size of the big blind.
     */
    public Table(String name, TableType type, int bigBlind, int minBuy, int maxBuy) {
		this.name = name;
        this.tableType = type;
        this.bigBlind = bigBlind;
		this.minBuy = minBuy;
		this.maxBuy = maxBuy;
        players = new TreeMap<Integer, Player>();
        activePlayers = new ArrayList<Player>();
        deck = new Deck();
        board = new ArrayList<Card>();
        pots = new ArrayList<Pot>();
		spectators = new ArrayList<User>();
		removeThesePlayers = new HashMap();
    }
	
	public int getMinBuy()
	{
		return minBuy;
	}
	
	public int getMaxBuy()
	{
		return maxBuy;
	}
	
	public int getSmallBlind()
	{
		return bigBlind / 2;
	}
	
	public String getName()
	{
		return name;
	}
	
	public int getBigBlind()
	{
		return bigBlind;
	}
	
	public int getCurrentBet()
	{
		return bet;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public boolean getIsShowdown()
	{
		return showdown;
	}
	
	public int getActorSeatNum()
	{
		if (null != actor)
			return actor.getSeatNum();
		else
			return -1;
	}
    
	public int getDealerSeatNum()
	{
		if (null != dealer)
			return dealer.getSeatNum();
		else
			return -1;
	}
	
	public List<Card> getBoard()
	{
		List<Card> boardCopy = new ArrayList<Card>(board);
		return boardCopy;
	}
	
    /**
     * Adds a player.
     * 
     * @param player
     *            The player.
     */
    public void addPlayer(int seatNum, Player player) throws InvalidSeatException, SeatAlreadyOccupiedException, PlayerNotEnoughCashException {
		synchronized(players) {
			if (seatNum < 1 || seatNum > 9) 
				throw new InvalidSeatException();
			else if (players.containsKey(seatNum))
				throw new SeatAlreadyOccupiedException();
			else if (player.getCash() <= 0)
				throw new PlayerNotEnoughCashException();
			players.put(seatNum, player);
			for (Player playerToNotify : players.values()) {
				// Notify everyone BUT the joined player
				if (!playerToNotify.equals(player))
					playerToNotify
						.getClient()
						.joinedTable(tableType, bigBlind, seatNum, player);
			}
		}
		
		// Notify if waiting for players
		synchronized(waitForPlayerLock) {
			if (players.size() > 1)
				waitForPlayerLock.notify();
		}
		
		for (User user : spectators) {
			if (!user.equals(player.getClient())) 
				user.joinedTable(tableType, bigBlind, seatNum, player);
		}
    }
	
	public void removePlayer(int seatNum)
	{
		synchronized(this) {
			Player player = players.get(seatNum);
			if (null != player)
				removeThesePlayers.put(seatNum, player);
		}
	}
	
	public void removePlayer(Player player)
	{
		synchronized(this) {
			for (Map.Entry<Integer, Player> entry : players.entrySet())
			{
				if (entry.getValue().equals(player)) 
				{
					int seatNum = entry.getKey();
					removeThesePlayers.put(seatNum, player);
					break;
				}
			}
		}
	}
	
	public int getMaxPlayers()
	{
		return MAX_PLAYERS;
	}
	
	public Map<Integer, Player> getPlayers()
	{
		Map<Integer, Player> copyPlayers = new TreeMap<Integer, Player>();
		synchronized(players) {
			for (Map.Entry<Integer, Player> entry : players.entrySet())
			{
				if (showdown) {
					copyPlayers.put(entry.getKey(), entry.getValue());
				} else {
					// Hide secret information to other players.
					copyPlayers.put(entry.getKey(), entry.getValue().publicClone());
				}
			}
		}
		return copyPlayers;
	}
	
	public List<User> getSpectators()
	{
		return spectators;
	}
	
	public int getPlayersCount()
	{
		return players.size();
	}
	
	public boolean isPlaying(Player player)
	{
		return players.containsValue(player);
	}
	
	public void addSpectator(User user)
	{
		spectators.add(user);
	}
	
	public void removeSpectator(User user)
	{
		spectators.remove(user);
	}
	
	protected void sleep(int ms)
	{
		try 
		{
			if (ms < 0)
				ms = sleepBetweenStage;
			Thread.sleep(sleepBetweenStage);
		} 
		catch (InterruptedException e)
		{
		}
	}
	
	protected void sleep()
	{
		sleep(-1);
	}
	
    /**
     * Main game loop.
     */
    public void run() {
		while(!Thread.currentThread().isInterrupted())
		{
			dealerPosition = -1;
			actorPosition = -1;
//			smallBlindPosition = -1;
//			bigBlindPosition = -1;
			while (true) {
				checkForRemovedPlayers();
				int noOfActivePlayers = 0;
				for (Player player : players.values()) {
					if (player.getCash() >= bigBlind) {
						noOfActivePlayers++;
					}
				}
				if (noOfActivePlayers > 1) {
					playHand();
				} else {
					break;
				}
			}

			System.out.println("No more available players, waiting");
			// Game over.
			board.clear();
			pots.clear();
			bet = 0;
			notifyBoardUpdated();
			for (Player player : players.values()) {
				player.resetHand();
			}
			notifyPlayersUpdated(false);
			notifyMessage("Waiting for players");
			
			// Clear the panel for this lonely player
			for (Player player : players.values()) {
				player.getClient().playerUpdated(player);
			}
		
			synchronized(waitForPlayerLock) {
				try
				{
					waitForPlayerLock.wait();
				}
				catch (InterruptedException e)
				{
					System.out.println("Thread interrupted");
					break;
				}
			}
		}
    }
	
	private void checkForRemovedPlayers() {
		// Check for removed players
		List<Player> removedPlayers = new ArrayList<Player>();
		synchronized (this) {
			for (Map.Entry<Integer, Player> entry : removeThesePlayers.entrySet()) {
				int seatNum = entry.getKey();
				Player player = entry.getValue();
				players.remove(seatNum);
				removedPlayers.add(player);
			}
			removeThesePlayers.clear();
		}
		
		// Notify players and spectators
		for (Player removedPlayer : removedPlayers) {
			for (Player playerToNotify : players.values()) 
				playerToNotify.getClient().leavedTable(removedPlayer);
			for (User user : spectators)
				user.leavedTable(removedPlayer);
		}
	}
	
    /**
     * Plays a single hand.
     */
    private void playHand() {
        resetHand();
        
        // Small blind.
        if (activePlayers.size() > 2) {
            rotateActor();
        }
        postSmallBlind();
		sleep(500);
        
        // Big blind.
        rotateActor();
        postBigBlind();
		sleep(500);
        
        // Pre-Flop.
        dealHoleCards();
		sleep();
        doBettingRound();
        
        // Flop.
        if (activePlayers.size() > 1) {
            bet = 0;
            dealCommunityCards("Flop", 3);
            minBet = bigBlind;
			sleep();
            doBettingRound();

            // Turn.
            if (activePlayers.size() > 1) {
                bet = 0;
                dealCommunityCards("Turn", 1);
                if (tableType == TableType.FIXED_LIMIT) {
                    minBet = 2 * bigBlind;
                } else {
                    minBet = bigBlind;
                }
				sleep();
                doBettingRound();

                // River.
                if (activePlayers.size() > 1) {
                    bet = 0;
                    dealCommunityCards("River", 1);
                    if (tableType == TableType.FIXED_LIMIT) {
                        minBet = 2 * bigBlind;
                    } else {
                        minBet = bigBlind;
                    }
					sleep();
                    doBettingRound();

                    // Showdown.
                    if (activePlayers.size() > 1) {
                        bet = 0;
                        minBet = bigBlind;
                        doShowdown();
                    }
                }
            }
        }
		
		sleep(5000);
    }
    
    /**
     * Resets the game for a new hand.
     */
    private void resetHand() {
        // Clear the board.
        board.clear();
        pots.clear();
        notifyBoardUpdated();
        
        // Determine the active players.
        activePlayers.clear();
        for (Map.Entry<Integer, Player> entry : players.entrySet()) {
			Player player = entry.getValue();
            player.resetHand();
            // Player must be able to afford at least the big blind.
            if (player.getCash() >= bigBlind) {
                activePlayers.add(player);
            }
        }
        
        // Rotate the dealer button.
        dealerPosition = (dealerPosition + 1) % activePlayers.size();
        dealer = activePlayers.get(dealerPosition);

        // Shuffle the deck.
        deck.shuffle();

        // Determine the first player to act.
        actorPosition = dealerPosition;
        actor = activePlayers.get(actorPosition);
        
        // Set the initial bet to the big blind.
        minBet = bigBlind;
        bet = minBet;
        
        // Notify all clients a new hand has started.
        for (Player player : players.values()) {
            player.getClient().handStarted(dealer);
        }
		
        notifyPlayersUpdated(false);
        notifyMessage("New hand, %s is the dealer.", dealer);
		
		// spectators
		for (User user : spectators) {
			user.handStarted(dealer);
		}
    }

    /**
     * Rotates the position of the player in turn (the actor).
     */
    private void rotateActor() {
        actorPosition = (actorPosition + 1) % activePlayers.size();
        actor = activePlayers.get(actorPosition);
        for (Player player : players.values()) {
            player.getClient().actorRotated(actor);
        }
		
		// spectators
		for (User user : spectators) {
            user.actorRotated(actor.publicClone());
        }
    }
    
    /**
     * Posts the small blind.
     */
    private void postSmallBlind() {
        final int smallBlind = bigBlind / 2;
        actor.postSmallBlind(smallBlind);
        contributePot(smallBlind);
        notifyBoardUpdated();
        notifyPlayerActed();
    }
    
    /**
     * Posts the big blind.
     */
    private void postBigBlind() {
        actor.postBigBlind(bigBlind);
        contributePot(bigBlind);
        notifyBoardUpdated();
        notifyPlayerActed();
    }
    
    /**
     * Deals the Hole Cards.
     */
    private void dealHoleCards() {
        for (Player player : activePlayers) {
            player.setCards(deck.deal(2));
        }
        System.out.println();
        notifyPlayersUpdated(false);
        notifyMessage("%s deals the hole cards.", dealer);
    }
    
    /**
     * Deals a number of community cards.
     * 
     * @param phaseName
     *            The name of the phase.
     * @param noOfCards
     *            The number of cards to deal.
     */
    private void dealCommunityCards(String phaseName, int noOfCards) {
        for (int i = 0; i < noOfCards; i++) {
            board.add(deck.deal());
        }
        notifyPlayersUpdated(false);
        notifyMessage("%s deals the %s.", dealer, phaseName);
    }
    
    /**
     * Performs a betting round.
     */
    private void doBettingRound() {
        // Determine the number of active players.
        int playersToAct = activePlayers.size();
        // Determine the initial player and bet size.
        if (board.size() == 0) {
            // Pre-Flop; player left of big blind starts, bet is the big blind.
            bet = bigBlind;
        } else {
            // Otherwise, player left of dealer starts, no initial bet.
            actorPosition = dealerPosition;
            bet = 0;
        }
        
        /*if (playersToAct == 2) {  //removed, fix by IW
            // Heads Up mode; player who is not the dealer starts.
            actorPosition = dealerPosition;
        }*/
        
        lastBettor = null;
        raises = 0;
        notifyBoardUpdated();
        
        while (playersToAct > 0) {
            rotateActor();
            Action action = null;
            if (actor.isAllIn()) {
                // Player is all-in, so must check.
                action = Action.CHECK;
                playersToAct--;
            } else {
                // Otherwise allow client to act.
                Set<Action> allowedActions = getAllowedActions(actor);
                action = actor.getClient().act(minBet, bet, allowedActions);
                // Verify chosen action to guard against broken clients (accidental or on purpose).
                if (!allowedActions.contains(action)) {
                    if (!(action instanceof BetAction && allowedActions.contains(Action.BET)) && !(action instanceof RaiseAction && allowedActions.contains(Action.RAISE))) {
                        throw new IllegalStateException(String.format("Player '%s' acted with illegal %s action", actor, action));
                    }
                }
                playersToAct--;
                if (action == Action.CHECK) {
                    // Do nothing.
                } else if (action == Action.CALL) {
                    int betIncrement = bet - actor.getBet();
                    if (betIncrement > actor.getCash()) {
                        betIncrement = actor.getCash();
                    }
                    actor.payCash(betIncrement);
                    actor.setBet(actor.getBet() + betIncrement);
                    contributePot(betIncrement);
                } else if (action instanceof BetAction) {
                    int amount = (tableType == TableType.FIXED_LIMIT) ? minBet : action.getAmount();
                    if (amount < minBet && amount < actor.getCash()) {
                        throw new IllegalStateException("Illegal client action: bet less than minimum bet!");
                    }
                    if (amount > actor.getCash() && actor.getCash() >= minBet) {
                        throw new IllegalStateException("Illegal client action: bet more cash than you own!");
                    }
                    bet = amount;
                    minBet = amount;
                    int betIncrement = bet - actor.getBet();
                    if (betIncrement > actor.getCash()) {
                        betIncrement = actor.getCash();
                    }
                    actor.setBet(bet);
                    actor.payCash(betIncrement);
                    contributePot(betIncrement);
                    lastBettor = actor;
                    playersToAct = (tableType == TableType.FIXED_LIMIT) ? activePlayers.size() : (activePlayers.size() - 1);
                } else if (action instanceof RaiseAction) {
                    int amount = (tableType == TableType.FIXED_LIMIT) ? minBet : action.getAmount();
                    if (amount < minBet && amount < actor.getCash()) {
                        throw new IllegalStateException("Illegal client action: raise less than minimum bet!");
                    }
                    if (amount > actor.getCash() && actor.getCash() >= minBet) {
                        throw new IllegalStateException("Illegal client action: raise more cash than you own!");
                    }
                    bet += amount;
                    minBet = amount;
                    int betIncrement = bet - actor.getBet();
                    if (betIncrement > actor.getCash()) {
                        betIncrement = actor.getCash();
                    }
                    actor.setBet(bet);
                    actor.payCash(betIncrement);
                    contributePot(betIncrement);
                    lastBettor = actor;
                    raises++;
                    if (tableType == TableType.FIXED_LIMIT && (raises < MAX_RAISES || activePlayers.size() == 2)) {
                        // All players get another turn.
                        playersToAct = activePlayers.size();
                    } else {
                        // Max. number of raises reached; other players get one more turn.
                        playersToAct = activePlayers.size() - 1;
                    }
                } else if (action == Action.FOLD) {
                    actor.setCards(null);
                    activePlayers.remove(actor);
                    actorPosition--;
                    if (activePlayers.size() == 1) {
                        // Only one player left, so he wins the entire pot.
                        notifyBoardUpdated();
                        notifyPlayerActed();
                        Player winner = activePlayers.get(0);
                        int amount = getTotalPot();
                        winner.win(amount);
                        notifyBoardUpdated();
                        notifyMessage("%s wins $ %d.", winner, amount);
                        playersToAct = 0;
                    }
                } else {
                    // Programming error, should never happen.
                    throw new IllegalStateException("Invalid action: " + action);
                }
            }
            actor.setAction(action);
            if (activePlayers.size() > 1) {
                notifyBoardUpdated();
                notifyPlayerActed();
            }
        }
        
        // Reset player's bets.
        for (Player player : activePlayers) {
            player.resetBet();
        }
        notifyBoardUpdated();
        notifyPlayersUpdated(false);
    }
    
    /**
     * Returns the allowed actions of a specific player.
     * 
     * @param player
     *            The player.
     * 
     * @return The allowed actions.
     */
    private Set<Action> getAllowedActions(Player player) {
        Set<Action> actions = new HashSet<Action>();
        if (player.isAllIn()) {
            actions.add(Action.CHECK);
        } else {
            int actorBet = actor.getBet();
            if (bet == 0) {
                actions.add(Action.CHECK);
                if (tableType == TableType.NO_LIMIT || raises < MAX_RAISES || activePlayers.size() == 2) {
                    actions.add(Action.BET);
                }
            } else {
                if (actorBet < bet) {
                    actions.add(Action.CALL);
                    if (tableType == TableType.NO_LIMIT || raises < MAX_RAISES || activePlayers.size() == 2) {
                        actions.add(Action.RAISE);
                    }
                } else {
                    actions.add(Action.CHECK);
                    if (tableType == TableType.NO_LIMIT || raises < MAX_RAISES || activePlayers.size() == 2) {
                        actions.add(Action.RAISE);
                    }
                }
            }
            actions.add(Action.FOLD);
        }
        return actions;
    }
    
    /**
     * Contributes to the pot.
     * 
     * @param amount
     *            The amount to contribute.
     */
    private void contributePot(int amount) {
        for (Pot pot : pots) {
            if (!pot.hasContributer(actor)) {
                int potBet = pot.getBet();
                if (amount >= potBet) {
                    // Regular call, bet or raise.
                    pot.addContributer(actor);
                    amount -= pot.getBet();
                } else {
                    // Partial call (all-in); redistribute pots.
                    pots.add(pot.split(actor, amount));
                    amount = 0;
                }
            }
            if (amount <= 0) {
                break;
            }
        }
        if (amount > 0) {
            Pot pot = new Pot(amount);
            pot.addContributer(actor);
            pots.add(pot);
        }
    }
    
    /**
     * Performs the showdown.
     */
    private void doShowdown() {
//        System.out.println("\n[DEBUG] Pots:");
//        for (Pot pot : pots) {
//            System.out.format("  %s\n", pot);
//        }
//        System.out.format("[DEBUG]  Total: %d\n", getTotalPot());
        
        // Determine show order; start with all-in players...
        List<Player> showingPlayers = new ArrayList<Player>();
        for (Pot pot : pots) {
            for (Player contributor : pot.getContributors()) {
                if (!showingPlayers.contains(contributor) && contributor.isAllIn()) {
                    showingPlayers.add(contributor);
                }
            }
        }
        // ...then last player to bet or raise (aggressor)...
        if (lastBettor != null) {
            if (!showingPlayers.contains(lastBettor)) {
                showingPlayers.add(lastBettor);
            }
        }
        //...and finally the remaining players, starting left of the button.
        int pos = (dealerPosition + 1) % activePlayers.size();
        while (showingPlayers.size() < activePlayers.size()) {
            Player player = activePlayers.get(pos);
            if (!showingPlayers.contains(player)) {
                showingPlayers.add(player);
            }
            pos = (pos + 1) % activePlayers.size();
        }
        
        // Players automatically show or fold in order.
        boolean firstToShow = true;
        int bestHandValue = -1;
        for (Player playerToShow : showingPlayers) {
            Hand hand = new Hand(board);
            hand.addCards(playerToShow.getCards());
            HandValue handValue = new HandValue(hand);
            boolean doShow = ALWAYS_CALL_SHOWDOWN;
            if (!doShow) {
                if (playerToShow.isAllIn()) {
                    // All-in players must always show.
                    doShow = true;
                    firstToShow = false;
                } else if (firstToShow) {
                    // First player must always show.
                    doShow = true;
                    bestHandValue = handValue.getValue();
                    firstToShow = false;
                } else {
                    // Remaining players only show when having a chance to win.
                    if (handValue.getValue() >= bestHandValue) {
                        doShow = true;
                        bestHandValue = handValue.getValue();
                    }
                }
            }
            if (doShow) {
                // Show hand.
                for (Player player : players.values()) {
                    player.getClient().playerUpdated(playerToShow);
                }
                notifyMessage("%s has %s.", playerToShow, handValue.getDescription());
				sleep(3000);
            } else {
                // Fold.
                playerToShow.setCards(null);
                activePlayers.remove(playerToShow);
                for (Player player : players.values()) {
                    if (player.equals(playerToShow)) {
                        player.getClient().playerUpdated( playerToShow);
                    } else {
                        // Hide secret information to other players.
                        player.getClient().playerUpdated(playerToShow.publicClone());
                    }
                }
                notifyMessage("%s folds.", playerToShow);
				sleep(1000);
            }
        }
		
        // Sort players by hand value (highest to lowest).
        Map<HandValue, List<Player>> rankedPlayers = new TreeMap<HandValue, List<Player>>();
        for (Player player : activePlayers) {
            // Create a hand with the community cards and the player's hole cards.
            Hand hand = new Hand(board);
            hand.addCards(player.getCards());
            // Store the player together with other players with the same hand value.
            HandValue handValue = new HandValue(hand);
//            System.out.format("[DEBUG] %s: %s\n", player, handValue);
            List<Player> playerList = rankedPlayers.get(handValue);
            if (playerList == null) {
                playerList = new ArrayList<Player>();
            }
            playerList.add(player);
            rankedPlayers.put(handValue, playerList);
        }

        // Per rank (single or multiple winners), calculate pot distribution.
        int totalPot = getTotalPot();
        Map<Player, Integer> potDivision = new HashMap<Player, Integer>();
        for (HandValue handValue : rankedPlayers.keySet()) {
            List<Player> winners = rankedPlayers.get(handValue);
            for (Pot pot : pots) {
                // Determine how many winners share this pot.
                int noOfWinnersInPot = 0;
                for (Player winner : winners) {
                    if (pot.hasContributer(winner)) {
                        noOfWinnersInPot++;
                    }
                }
                if (noOfWinnersInPot > 0) {
                    // Divide pot over winners.
                    int potShare = pot.getValue() / noOfWinnersInPot;
                    for (Player winner : winners) {
                        if (pot.hasContributer(winner)) {
                            Integer oldShare = potDivision.get(winner);
                            if (oldShare != null) {
                                potDivision.put(winner, oldShare + potShare);
                            } else {
                                potDivision.put(winner, potShare);
                            }
                            
                        }
                    }
                    // Determine if we have any odd chips left in the pot.
                    int oddChips = pot.getValue() % noOfWinnersInPot;
                    if (oddChips > 0) {
                        // Divide odd chips over winners, starting left of the dealer.
                        pos = dealerPosition;
                        while (oddChips > 0) {
                            pos = (pos + 1) % activePlayers.size();
                            Player winner = activePlayers.get(pos);
                            Integer oldShare = potDivision.get(winner);
                            if (oldShare != null) {
                                potDivision.put(winner, oldShare + 1);
//                                System.out.format("[DEBUG] %s receives an odd chip from the pot.\n", winner);
                                oddChips--;
                            }
                        }
                        
                    }
                    pot.clear();
                }
            }
        }
        
        // Divide winnings.
        StringBuilder winnerText = new StringBuilder();
        int totalWon = 0;
        for (Player winner : potDivision.keySet()) {
            int potShare = potDivision.get(winner);
            winner.win(potShare);
            totalWon += potShare;
            if (winnerText.length() > 0) {
                winnerText.append(", ");
            }
            winnerText.append(String.format("%s wins $ %d", winner, potShare));
            notifyPlayersUpdated(true);
        }
        winnerText.append('.');
        notifyMessage(winnerText.toString());
		sleep(5000);
        
        // Sanity check.
        if (totalWon != totalPot) {
            throw new IllegalStateException("Incorrect pot division!");
        }
    }
    
    /**
     * Notifies listeners with a custom game message.
     * 
     * @param message
     *            The formatted message.
     * @param args
     *            Any arguments.
     */
    protected void notifyMessage(String message, Object... args) {
        this.message = String.format(message, args);
        for (Player player : players.values()) {
            player.getClient().messageReceived(this.message);
        }
		
		// spectators
		for (User user : spectators) {
			user.messageReceived(this.message);
		}
    }
    
    /**
     * Notifies clients that the board has been updated.
     */
    protected void notifyBoardUpdated() {
        int pot = getTotalPot();
        for (Player player : players.values()) {
            player.getClient().boardUpdated(board, bet, pot);
        }
		
		// spectators
		for (User user : spectators) {
            user.boardUpdated(board, bet, pot);
        }
    }
    
    /**
     * Returns the total pot size.
     * 
     * @return The total pot size.
     */
    public int getTotalPot() {
        int totalPot = 0;
        for (Pot pot : pots) {
            totalPot += pot.getValue();
        }
        return totalPot;
    }

    /**
     * Notifies clients that one or more players have been updated. <br />
     * <br />
     * 
     * A player's secret information is only sent its own client; other clients
     * see only a player's public information.
     * 
     * @param showdown
     *            Whether we are at the showdown phase.
     */
    protected void notifyPlayersUpdated(boolean showdown) {
		this.showdown = showdown;
		
        for (Player playerToNotify : players.values()) {
            for (Player player : players.values()) {
                if (!showdown && !player.equals(playerToNotify)) {
                    // Hide secret information to other players.
                    player = player.publicClone();
                }
                playerToNotify.getClient().playerUpdated(player);
            }
        }
		
		// spectators
		for (User userToNotify : spectators) {
			for (Player player : players.values()) {
				if (showdown) {
					userToNotify.playerUpdated(player);
				} else {
					// Hide secret information to other players.
					userToNotify.playerUpdated(player.publicClone());
				}
			}
				
        }
    }
    
    /**
     * Notifies clients that a player has acted.
     */
    protected void notifyPlayerActed() {
        for (Player p : players.values()) {
            Player playerInfo = p.equals(actor) ? actor : actor.publicClone();
            p.getClient().playerActed(playerInfo);
        }
		
		// spectators
		for (User user : spectators) {
            user.playerActed(actor.publicClone());
        }
    }
    
	public class TableJoinException extends Exception {}
	public class InvalidSeatException extends TableJoinException {}
	public class SeatAlreadyOccupiedException extends TableJoinException {}
	public class PlayerNotEnoughCashException extends TableJoinException {}
}
