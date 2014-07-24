package com.brkpoker.texasholdem;

/* Socket IO */
import com.corundumstudio.socketio.listener.*;
import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

/* Texas Holdem */
import org.ozsoft.texasholdem.*;

/**
 * Hello world!
 *
 */
public class App 
{
	public static final int ServerPort = 8082;
	public static final int TableCount = 10;
	
	// This is where the tables
	//public static List<Table> Tables;
	public static Map<String, Table> Tables;
	
	/**
	 * Connected users
	 */
	public static Map<String, User> Users;
	
    public static void main( String[] args ) throws InterruptedException
    {
		// Setting up tables
		Tables = new HashMap<String, Table>();
		for (int tbl_num = 0; tbl_num < TableCount; tbl_num++)
		{
			String tbl_name = String.format("TBL%03d", tbl_num + 1);
			Tables.put(tbl_name, new Table(TableType.FIXED_LIMIT, 10));
		}
		System.out.println("Created tables");
		// Users
		Users = new HashMap<String, User>();
		
		
		com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("localhost");
        config.setPort(ServerPort);
//		config.setOrigin("http://localhost");
		
		final SocketIOServer server = new SocketIOServer(config);
        System.out.println( "The Server configured at port " +  ServerPort);
		
//		server.addConnectListener(new ConnectListener() 
//		{
//			@Override
//			public void onConnect(SocketIOClient client)
//			{
//				//client
//				System.out.printf("Joined client from %s (UUID: %s)\n", 
//						client.getRemoteAddress(), client.getSessionId());
//				
//				// @TODO: put session ID here
//				String authid = client.getHandshakeData().getSingleUrlParam("authid");
//				System.out.printf("authid: %s\n", authid);
//				
//				client.sendEvent("new message", "Hello world");
//				
//				User user = new User(client);
//				user.setName(String.format("user%d", Users.size()+1));
//				Users.put(client.getSessionId().toString(), user);
//			}
//		});
//		
//		server.addDisconnectListener(new DisconnectListener() 
//		{
//			@Override
//			public void onDisconnect(SocketIOClient client) 
//			{
//				System.out.println("Client: "+client+" disconnected");
//				Users.remove(client.getSessionId().toString());
//			}
//		});
//
//		
//		server.addEventListener("name", String.class, new DataListener<String>() 
//		{
//			@Override
//			public void onData(SocketIOClient client, String data,
//					AckRequest ackSender) 
//			{ 
//				System.out.printf("Received name change: %s\n", data);
//				User user = Users.get(client.getSessionId().toString());
//				user.setName(data);
//			}
//		});
//		
//		server.addEventListener("get tables", String.class, new DataListener<String>() 
//		{
//			@Override
//			public void onData(SocketIOClient client, String data,
//					AckRequest ackSender) 
//			{ 
//				System.out.println("Received request get tables");
//				String[] table_names = new String[TableCount];
//				int idx = 0;
//				for (Entry<String, Table> entry : Tables.entrySet())
//				{
//					table_names[idx++] = entry.getKey();
//				}
//				client.sendEvent("list tables", table_names);
//			}
//		});
//		
		
		//server.add
//		server.addJsonObjectListener(ChatObject.class, new DataListener<ChatObject>() {
//            @Override
//            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
//                // broadcast messages to all clients
//                server.getBroadcastOperations().sendJsonObject(data);
//            }
//        });
		
		SocketService service = new SocketService();
		server.addListeners(service);
		
		server.start();
		
		Scanner kb = new Scanner(System.in);

		while(true)
		{
			String input = kb.next();
			if (input.equalsIgnoreCase("quit"))
				break;
			
			System.out.println("Unknown command: " + input);
		}
        //Thread.sleep(Integer.MAX_VALUE);

        server.stop();
    }
	
	
}
