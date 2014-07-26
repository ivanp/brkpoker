package com.brkpoker.texasholdem;

/* Socket IO */
import com.corundumstudio.socketio.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

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
	public static TreeMap<String, Table> Tables;
	
	/**
	 * Connected players
	 */
	public static Map<String, User> Users;
	
	public static void runGameEngine()
	{
		// Setting up tables
		Tables = new TreeMap<String, Table>();
		for (int tbl_num = 0; tbl_num < TableCount; tbl_num++)
		{
			String tbl_name = String.format("T%03d", tbl_num + 1);
			Table table = new Table(TableType.FIXED_LIMIT, 10);
			table.run();
			Tables.put(tbl_name, table);
		}
		System.out.println("Created tables");
		// Users
		Users = new HashMap<String, User>();
	}
	
	public static void runSocketServer()
	{
		com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("localhost");
        config.setPort(ServerPort);
//		config.setOrigin("http://localhost");
		
		final SocketIOServer server = new SocketIOServer(config);
        System.out.println( "The Server configured at port " +  ServerPort);
		
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
	
    public static void main( String[] args ) throws InterruptedException
    {
		runGameEngine();
		
		runSocketServer();
		
    }
}
