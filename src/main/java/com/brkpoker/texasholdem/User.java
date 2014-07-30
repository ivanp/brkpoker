/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.brkpoker.texasholdem;

import static com.brkpoker.texasholdem.App.Tables;
import com.corundumstudio.socketio.SocketIOClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ozsoft.texasholdem.Card;
import org.ozsoft.texasholdem.Player;
import org.ozsoft.texasholdem.Table;
import org.ozsoft.texasholdem.TableType;
import org.ozsoft.texasholdem.actions.Action;

/**
 *
 * @author ivan
 */
public class User implements org.ozsoft.texasholdem.Client 
{
	private final SocketIOClient client;
	private String name;
	private Player player;
	private int cash = 1000000;
	private Action lastAction;
	private Object actionLock = new Object();
	private Table watchingTable;
	private boolean disconnected = false;
			
	public User(SocketIOClient client)
	{
		this.client = client;
	}
	
	public void setName(String name)
	{
		this.name = name;
		if (null != player)
			player.setName(name);
	}
	
	public String getName()
	{
		return name;
	}
	
	public SocketIOClient getSocket()
	{
		return client;
	}
	
	public Player getPlayer()
	{
		return player;
	}
	
	public void disconnect()
	{
		this.disconnected = true;
		gotoLobby();
	}
	
	public boolean isDisconnected()
	{
		return disconnected;
	}
	
	public void watchTable(Table table)
	{
		System.out.printf("Receiving request to watch table %s\n", table.getName());
		if(null != watchingTable)
			watchingTable.removeSpectator(this);
		table.addSpectator(this);
		watchingTable = table;
		
		client.sendEvent("watch", table.getName());
		repaintTable(table);
	}
	
	public void watchTable(String tableName)
	{
		Table table = Tables.get(tableName);
		if (null != table)
		{
			watchTable(table);
		}
		else
		{
			client.sendEvent("error", "Table "+tableName+" does not exist");
		}
	}
	
	public void repaintTable(Table table)
	{
		System.out.println("Repainting table "+table.getName());
		Map obj = new HashMap();
		obj.put("name", table.getName());
		obj.put("min", table.getMinBuy());
		obj.put("max", table.getMaxBuy());
		obj.put("small", table.getSmallBlind());
		obj.put("big", table.getBigBlind());
		obj.put("msg", table.getMessage());
		obj.put("bet", table.getCurrentBet());
		obj.put("pot", table.getTotalPot());
		obj.put("showdown", table.getIsShowdown());
		// Current actor
		obj.put("actor", table.getActorSeatNum());
		List cards = getCardHash(table.getBoard());
		for (int i = cards.size(); i < 5; i++) {
			cards.add(null);
		}
		obj.put("cards", cards);
		
		List players = new ArrayList();
		for (Player player : table.getPlayers().values())
			players.add(getPlayerObjectData(player));
		obj.put("players", players);
		
		client.sendEvent("game:repaint", obj);
	}
	
	public void gotoLobby()
	{
		if (null != watchingTable) 
		{
			System.out.printf("Removing user %s from spectator in table %s\n", name, watchingTable.getName());
			watchingTable.removeSpectator(this);
			watchingTable = null;
		}
		if (null != player)
		{
			System.out.printf("Removing user %s from player in table %s\n", name, player.getTable().getName());
			// If player still in table, set default action to fold
			actResponse(Action.FOLD);
			
			player.getTable().removePlayer(player);
			player = null;
		}
	}
	
	public void joinTable(Table table, int seatNum, int buyin) 
	{
		synchronized(this) {
			if (buyin > cash)
			{
				client.sendEvent("error", "Player don't have enough cash to play");
				return;
			}
			else if (null != player)
			{
				// Only allow to play on a table
				player.getTable().removePlayer(player);
				doCash(player.getCash());
				player = null;
			}

			Player newPlayer = null;
			try
			{
				doCash(-buyin);
				newPlayer = new Player(seatNum, name, buyin, this, table);
				table.addPlayer(seatNum, newPlayer);
				table.removeSpectator(this);
				player = newPlayer;
				watchingTable = null;
				System.out.printf("Successfully joined table: %s at seat %d\n", table.getName(), seatNum);
				// Confirm client
				client.sendEvent("sit", getPlayerObjectData(player));
			}
			catch (Table.InvalidSeatException e)
			{
				client.sendEvent("error", "Invalid seat number: "+seatNum);
			}
			catch (Table.SeatAlreadyOccupiedException e)
			{
				client.sendEvent("error", "Seat already occupied: "+seatNum);
			}
			catch (Table.PlayerNotEnoughCashException e)
			{
				client.sendEvent("error", "Player don't have enough buyin to play");
			}
			finally
			{
				if (null == newPlayer) {
					System.out.printf("Player %s was failed to enter the table, removing\n", player.getName());
					doCash(player.getCash());
					player = null;
				}
			}
		}
	}
	
	public void joinTable(String tableName, int seatNum, int buyin)
	{
		Table table = Tables.get(tableName);
		if (table != null)
		{
			joinTable(table, seatNum, buyin);
		}
		else
		{
			client.sendEvent("error", "Table "+tableName+" does not exist");
		}
	}
	
	public int getCash()
	{
		return cash;
	}
	
	protected int doCash(int amount) 
	{
		cash += amount;
		
		if (!disconnected)
			client.sendEvent("cash", cash);
		
		return cash;
	} 
	
	/**
     * Handles a game message.
     * 
     * @param message
     *            The message.
     */
    public void messageReceived(String message)
	{
		if (disconnected)
			return;
		
		client.sendEvent("msg", message);
	}

    /**
     * Handles the player joining a table.
     * 
     * @param type
     *            The table type (betting structure).
     * @param bigBlind
     *            The table's big blind.
     * @param players
     *            The players at the table (including this player).
     */
    public void joinedTable(TableType type, int bigBlind, int seatNum, Player player)
	{
		if (disconnected)
			return;
		
		Map obj = new HashMap();
		obj.put("num", seatNum);
		obj.put("name", player.getName());
		obj.put("cash", player.getCash());
		obj.put("is_available", false);
		obj.put("is_loading", false);
		obj.put("is_seated", true);
		obj.put("has_cards", false);
		
		//obj.put("cards", cards);
		client.sendEvent("player:join", obj);
	}
	
	public void leavedTable(Player player)
	{
		if (disconnected)
			return;
		
		// player:leave
		Map obj = new HashMap();
		obj.put("num", player.getSeatNum());
		obj.put("name", player.getName());
		client.sendEvent("player:leave", obj);
	}
    
    /**
     * Handles the start of a new hand.
     * 
     * @param dealer
     *            The dealer.
     */
    public void handStarted(Player dealer)
	{
		if (disconnected)
			return;
		
		// game:start
		Map obj = new HashMap();
		obj.put("num", dealer.getSeatNum());
		client.sendEvent("game:start", obj);
	}
    
    /**
     * Handles the rotation of the actor (the player who's turn it is).
     * 
     * @param actor
     *            The new actor.
     */
    public void actorRotated(Player actor)
	{
		if (disconnected)
			return;
		
		// game:rotate
		Map obj = new HashMap();
		obj.put("num", actor.getSeatNum());
		client.sendEvent("game:rotate", obj);
	}
    
    /**
     * Handles an update of this player.
     * 
     * @param player
     *            The player.
     */
    public void playerUpdated(Player player)
	{
		if (disconnected)
			return;
		
		Map obj = getPlayerObjectData(player);
		client.sendEvent("player:update", obj);
	}
    
    /**
     * Handles an update of the board.
     * 
     * @param cards
     *            The community cards.
     * @param bet
     *            The current bet.
     * @param pot
     *            The current pot.
     */
    public void boardUpdated(List<Card> cards, int bet, int pot)
	{
		if (disconnected)
			return;
		
		// game:update
		Map obj = new HashMap();
		obj.put("bet", bet);
		obj.put("pot", pot);
		List cardInts = getCardHash(cards);
		for (int i = cards.size(); i < 5; i++) {
			cardInts.add(null);
		}
		obj.put("cards", cardInts);
		client.sendEvent("game:update", obj);
	}
    
    /**
     * Handles the event of a player acting.
     * 
     * @param player
     *            The player that has acted.
     */
    public void playerActed(Player player)
	{
		if (disconnected)
			return;
		
		// player:act
		Map obj = new HashMap();
		obj.put("num", player.getSeatNum());
		Action action = player.getAction();
		obj.put("bet", player.getBet());
		obj.put("action", action);
		String last_action = "";
		if (null != action) {
			last_action = action.getName();
			if (action.getName()=="Bet" || action.getName()=="Raise")
				obj.put("amount", action.getAmount());
		}
		obj.put("last_action", last_action);
			
		client.sendEvent("player:act", obj);
	}

    /**
     * Requests this player to act, selecting one of the allowed actions.
     * 
     * @param minBet
     *            The minimum bet.
     * @param currentBet
     *            The current bet.
     * @param allowedActions
     *            The allowed actions.
     * 
     * @return The selected action.
     */
    public Action act(int minBet, int currentBet, Set<Action> allowedActions)
	{
		if (disconnected)
		{
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
			}
			return Action.FOLD;
		}
		
		Map obj = new HashMap();
		obj.put("min", minBet);
		obj.put("bet", currentBet);
		
		List actions = new ArrayList();
		for (Action action : allowedActions) {
			actions.add(action.getName());
		}
		obj.put("actions", actions);
		
		client.sendEvent("game:act", obj);
		synchronized (actionLock) {
			try
			{
				actionLock.wait();
			}
			catch (InterruptedException e)
			{
				System.out.printf("User %s User.act received InterruptedException, giving fold as a default\n", this.name);
				lastAction = Action.FOLD;
			}
		}
		return lastAction;
	}
	
	@Override
	public void playerWon(Player player, int amount)
	{
		if (disconnected)
			return;
		
		Map obj = new HashMap();
		obj.put("num", player.getSeatNum());
		obj.put("cash", player.getCash());
		obj.put("amount", amount);
		client.sendEvent("player:won", obj);
	}
	
	/**
	 * 
	 */
	public void actResponse(Action action)
	{
		lastAction = action;
		synchronized (actionLock) {
			actionLock.notify();
		}
	}
	
	public void chatResponse(String msg)
	{
		if (null != player) {
			for (Player playerToNotify : player.getTable().getPlayers().values()) {
				if (!player.getClient().equals(playerToNotify.getClient())) {
					playerToNotify.getClient().chatReceived(player, msg);
				}
			}
		}
	}
	
	@Override
	public void chatReceived(Player player, String msg)
	{
		if (disconnected)
			return;
		
		Map obj = new HashMap();
		obj.put("name", player.getName());
		obj.put("msg", msg);
		client.sendEvent("chat", obj);
	}
	
	public Map getPlayerObjectData(Player player)
	{
		Table table = player.getTable();
		
		Map obj = new HashMap();
		
		int seatNum = player.getSeatNum();
		obj.put("num", seatNum);
		obj.put("name", player.getName());
		obj.put("cash", player.getCash());
		Action action = player.getAction();
		String last_action = "";
		if (action != null)
			last_action = action.toString().toLowerCase();
		obj.put("last_action", last_action);
		obj.put("bet", player.getBet());
		obj.put("is_available", false);
		obj.put("is_loading", false);
		obj.put("is_seated", true);
		obj.put("has_cards", player.hasCards());
		obj.put("is_turn", (seatNum == table.getActorSeatNum()));
		obj.put("is_dealer", (seatNum == table.getDealerSeatNum()));
		if (player.hasCards()) {
			if (player.getCards().length == 0) {
				int[] backs = {-1, -1};
				obj.put("cards", backs);
			} else
			obj.put("cards", getCardHash(
					player.getCards()));
		} else {
			Object[] nulls = {null, null};
			obj.put("cards", nulls);
		}
			
		return obj;
	}
	
	public List getCardHash(Card[] cards)
	{
		List hash = new ArrayList();
		for (Card card : cards) 
			hash.add(card.hashCode());
		return hash;
	}
	
	public List getCardHash(List<Card> cards)
	{
		List hash = new ArrayList();
		for (Card card : cards) 
			hash.add(card.hashCode());
		return hash;
	}
	
	public class NotEnoughCashException extends Exception
	{
		
	}
	
	public class AlreadyPlayingOnOtherTableException extends Exception
	{
		
	}
}
