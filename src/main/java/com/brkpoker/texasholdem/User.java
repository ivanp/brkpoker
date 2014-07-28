/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.brkpoker.texasholdem;

import com.brkpoker.texasholdem.webobject.RequestActionObject;
import static com.brkpoker.texasholdem.App.Tables;
import com.brkpoker.texasholdem.webobject.PlayerJoinedTableObject;
import com.corundumstudio.socketio.SocketIOClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.json.simple.JSONObject;
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
	private int cash = 10000;
	private Action lastAction;
	private Object actionLock = new Object();
	private Table watchingTable;
			
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
	
	public Player getPlayer()
	{
		return player;
	}
	
	public void disconnect()
	{
		gotoLobby();
	}
	
	public void watchTable(Table table)
	{
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
		Map obj = new HashMap();
		obj.put("name", table.getName());
		obj.put("count", table.getPlayersCount());
		obj.put("max", table.getMaxPlayers());
		obj.put("bigblind", table.getBigBlind());
		obj.put("msg", table.getMessage());
		obj.put("showdown", table.getIsShowdown());
		// Current actor
		obj.put("actor", table.getIsShowdown());
		// Last action
		obj.put("action", table.getIsShowdown());
		
		List players = new ArrayList();
		for (Map.Entry<Integer, Player> entry : table.getPlayers().entrySet())
		{
			int num = entry.getKey();
			Player player = entry.getValue();
			Map playerObj = new HashMap();
			// seat number
			playerObj.put("num", num);
			playerObj.put("name", player.getName());
			playerObj.put("cash", player.getCash());
			playerObj.put("last_action", player.getAction());
			playerObj.put("bet", player.getBet());
			playerObj.put("is_available", false);
			playerObj.put("is_loading", false);
			playerObj.put("is_seated", true);
			playerObj.put("has_cards", player.hasCards());
			playerObj.put("is_turn", (num == table.getActorPos()));
			playerObj.put("is_dealer", (num == table.getDealerPos()));
			if (table.getIsShowdown()) {
				List cards = new ArrayList<String>();
				for (Card card : player.getCards())
					cards.add(card.hashCode());
				playerObj.put("cards", cards);
			}
			
			players.add(playerObj);
		}
		obj.put("players", players);
		
		client.sendEvent("game:repaint", obj);
	}
	
	public void gotoLobby()
	{
		if (null != watchingTable) 
		{
			watchingTable.removeSpectator(this);
			watchingTable = null;
		}
		if (null != player)
		{
			player.getTable().removePlayer(player);
			player = null;
		}
	}
	
	public void joinTable(Table table, int seatNum, int buyin) 
	{
		if (buyin > cash)
		{
			client.sendEvent("error", "Player don't have enough cash to play");
			return;
		}
		else if (null != player)
		{
			// Only allow to play on a table
			player.getTable().removePlayer(player);
			cash += player.getCash();
			player = null;
		}
		
		cash -= buyin;
		player = new Player(name, buyin, this, table);
		try
		{
			table.addPlayer(seatNum, player);
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
			if (!table.isPlaying(player)) {
				cash += player.getCash();
				player = null;
			}
		}
	}
	
	public void joinTable(String tableName, int seatNum, int buyin)
	{
		Table table = Tables.get(tableName);
		if (table != null)
		{
			joinTable(table, seatNum, buyin);
			// @TODO: send table information
			// @TODO: repaint table
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
	
	/**
     * Handles a game message.
     * 
     * @param message
     *            The message.
     */
    public void messageReceived(String message)
	{
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
		Map obj = new HashMap();
		obj.put("num", seatNum);
		obj.put("name", player.getName());
		obj.put("cash", player.getCash());
		obj.put("is_available", false);
		obj.put("is_loading", false);
		obj.put("is_seated", true);
		obj.put("has_cards", false);
		obj.put("card1", null);
		obj.put("card2", null);
		client.sendEvent("player:join", obj);
	}
	
	public void leavedTable(int seatNum, Player player)
	{
		// player:leave
		Map obj = new HashMap();
		obj.put("num", seatNum);
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
		// game:start
	}
    
    /**
     * Handles the rotation of the actor (the player who's turn it is).
     * 
     * @param actor
     *            The new actor.
     */
    public void actorRotated(int seatNum, Player actor)
	{
		// game:rotate
		Map obj = new HashMap();
		obj.put("num", seatNum);
		client.sendEvent("game:rotate", obj);
	}
    
    /**
     * Handles an update of this player.
     * 
     * @param player
     *            The player.
     */
    public void playerUpdated(int seatNum, Player player)
	{
		Map obj = new HashMap();
		obj.put("num", seatNum);
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
		// table:update
	}
    
    /**
     * Handles the event of a player acting.
     * 
     * @param player
     *            The player that has acted.
     */
    public void playerActed(Player player)
	{
		// player:action
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
		RequestActionObject data = new RequestActionObject(minBet, currentBet, allowedActions.toArray(new String[0]));
		client.sendEvent("game.reqact", data);
		synchronized (actionLock) {
			try
			{
				actionLock.wait();
			}
			catch (InterruptedException e)
			{
			}
		}
		return lastAction;
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
	
	public class NotEnoughCashException extends Exception
	{
		
	}
	
	public class AlreadyPlayingOnOtherTableException extends Exception
	{
		
	}
}
