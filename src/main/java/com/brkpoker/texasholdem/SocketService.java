/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.brkpoker.texasholdem;

import static com.brkpoker.texasholdem.App.TableCount;
import static com.brkpoker.texasholdem.App.Tables;
import static com.brkpoker.texasholdem.App.Users;
import com.brkpoker.texasholdem.webobject.AuthObject;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.*;
import java.util.List;
import java.util.Map;

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

		client.sendEvent("auth", true);

		User user = new User(client);
		user.setName(String.format("user%d", Users.size()+1));
		Users.put(client.getSessionId().toString(), user);
	}
	
	@OnDisconnect
	public void onDisconnectHandler(SocketIOClient client) 
	{
		System.out.println("Client: "+client.getSessionId().toString()+" disconnected");
		Users.remove(client.getSessionId().toString());
	}
	
	@OnEvent("set:name")
	public void onNameHandler(SocketIOClient client, String data, AckRequest ackRequest) 
	{
		System.out.printf("Received name change: %s\n", data);
		User user = Users.get(client.getSessionId().toString());
		user.setName(data);
	}
	
	@OnEvent("get:tables")
	public void onGetTablesHandler(SocketIOClient client, List<String> data, AckRequest ackRequest) 
	{
		System.out.println("Received request get tables");
		int argc = 0;
		for(String str : data)
		{
			System.out.printf("Arg #%d: %s\n", ++argc, str);
		}
		//System.out.println("Received request get tables: "+data);
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
	}
	
	/**
	 * Play on a table given a seat number
	 * @param client
	 * @param data
	 * @param ackRequest 
	 */
	@OnEvent("sit")
	public void onSitHandler(SocketIOClient client, String data, AckRequest ackRequest) 
	{
		
	}
	
	@OnEvent("chat")
	public void onChatHandler(SocketIOClient client, String data, AckRequest ackRequest) 
	{
		System.out.println("Received request chat");
		// @TODO: check if user is currently playing, nonspectator & spectator 
		// are not allowed to chat
		
	}
	
	@OnEvent("auth")
	public void onAuthHandler(SocketIOClient client, AuthObject data, AckRequest ackRequest) 
	{
		System.out.println("Received auth");
		// @TODO: check if user is currently playing, nonspectator & spectator 
		// are not allowed to chat
		
	}
}
