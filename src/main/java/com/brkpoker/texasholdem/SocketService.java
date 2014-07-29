/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.brkpoker.texasholdem;

import static com.brkpoker.texasholdem.App.TableCount;
import static com.brkpoker.texasholdem.App.Tables;
import static com.brkpoker.texasholdem.App.Users;
import com.brkpoker.texasholdem.webobject.ActionObject;
import com.brkpoker.texasholdem.webobject.AuthObject;
import com.brkpoker.texasholdem.webobject.SitObject;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.*;
import java.util.Map;
import org.ozsoft.texasholdem.Table;

/**
 *
 * @author ivan
 */
public class SocketService
{
	@OnConnect
	public void onConnectHandler(SocketIOClient client)
	{
		//client
		System.out.printf("Joined client from %s (UUID: %s)\n", 
				client.getRemoteAddress(), client.getSessionId());

		// @TODO: put session ID here
		String authid = client.getHandshakeData().getSingleUrlParam("authid");
		System.out.printf("authid: %s\n", authid); 

		User user = new User(client);
		synchronized (Users) {
			String name = String.format("Player %d", Users.size()+1);
			user.setName(name);
			Users.put(client.getSessionId().toString(), user);
		}
		
		AuthObject auth = new AuthObject(true, user.getName(), user.getCash());
		
		client.sendEvent("auth", auth);
	}
	
	@OnDisconnect
	public void onDisconnectHandler(SocketIOClient client) 
	{
		System.out.println("Client: "+client.getSessionId().toString()+" disconnected");
		
		User user = Users.get(client.getSessionId().toString());
		user.disconnect();
		Users.remove(client.getSessionId().toString());
		//Users.remove(client.getSessionId().toString());
		// @TODO: remove from spectators & remove player gracefully
	}
	
	@OnEvent("set:name")
	public void onNameHandler(SocketIOClient client, String data, AckRequest ackRequest) 
	{
		System.out.printf("Received name change: %s\n", data);
		getUser(client).setName(data);
	}
	
	@OnEvent("get:tables")
	public void onGetTablesHandler(SocketIOClient client, String data, AckRequest ackRequest) 
	{
		System.out.println("Received request get tables");
		
		String[] table_names = new String[TableCount];
		int idx = 0;
		for (Map.Entry<String, Table> entry : Tables.entrySet())
		{
			table_names[idx++] = entry.getKey();
		}
		client.sendEvent("list tables", table_names);
	}
	
	/**
	 * Spectating a table
	 * @param client
	 * @param data
	 * @param ackRequest 
	 */
	@OnEvent("watch")
	public void onWatchHandler(SocketIOClient client, String data, AckRequest ackRequest) 
	{
		System.out.printf("Watching table: %s\n", data);
		getUser(client).watchTable(data);
	}
	
	/**
	 * Play on a table given a seat number
	 * @param client
	 * @param data
	 * @param ackRequest 
	 */
	@OnEvent("sit")
	public void onSitHandler(SocketIOClient client, SitObject data, AckRequest ackRequest) 
	{
		System.out.printf("Received request watching table: %s at seat %d\n", data.getTable(), data.getNum());
		getUser(client).joinTable(data.getTable(), data.getNum(), data.getBuy());
	}
	
	@OnEvent("act")
	public void onActHandler(SocketIOClient client, ActionObject data, AckRequest ackRequest)
	{
		getUser(client).actResponse(data.getActionInstance());
	}
	
	@OnEvent("chat")
	public void onChatHandler(SocketIOClient client, String data, AckRequest ackRequest) 
	{
		System.out.println("Received request chat");
		// @TODO: check if user is currently playing, nonspectator & spectator 
		getUser(client).chatResponse(data);
		// are not allowed to chat
		
	}
	
	public User getUser(SocketIOClient client)
	{
		User user = Users.get(client.getSessionId().toString());
		return user;
	}
}
