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
	public static final int TableCount = 1;
	
	public static final int BigBlind = 400;
	public static final int MinBuy = 5000;
	public static final int MaxBuy = 80000;
	
	// This is where the tables
	//public static List<Table> Tables;
	public static TreeMap<String, Table> Tables;
	
	public static TreeMap<String, Thread> Threads;
	
	/**
	 * Connected players
	 */
	public static Map<String, User> Users;
	
	public static void runGameEngine()
	{
		// Setting up tables
		Tables = new TreeMap<String, Table>();
		Threads = new TreeMap<String, Thread>();
		for (int tbl_num = 1; tbl_num <= TableCount; tbl_num++)
		{
			// Table id/name: T001 - TXXX
			String tbl_name = String.format("T%03d", 
					tbl_num);
			Table table = new Table(tbl_name, TableType.FIXED_LIMIT, BigBlind, MinBuy, MaxBuy);
			Tables.put(tbl_name, table);
			
			// Create a thread for each table
			Thread thread = new Thread(table);
			thread.start();
			Threads.put(tbl_name, thread);
			
			System.out.println("Created table: "+tbl_name);
		}
		System.out.println("Started "+TableCount+" table threads");
		// Users
		Users = new HashMap<String, User>();
	}
	
	public static void runSocketServer()
	{
		com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("localhost");
        config.setPort(ServerPort);
		SocketConfig socketConfig = new SocketConfig();
		socketConfig.setSoLinger(60);
		socketConfig.setReuseAddress(true);
		config.setSocketConfig(socketConfig);
//		config.setWorkerThreads(5);
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
			{
				System.out.println("Quitting");
				break;
			}
			
			System.out.println("Unknown command: " + input);
		}

		// Interrupt all threads
		for (Thread thread : Threads.values())
			thread.interrupt();
		
		// Wait for threads to finish
		for (Thread thread : Threads.values())
		{
			try 
			{
				thread.join();
			}
			catch (InterruptedException e)
			{
			}
		}

        server.stop();
	}
	
    public static void main( String[] args ) throws InterruptedException
    {
		// Run game engine threads
		runGameEngine();
		// Run socket.io server
		runSocketServer();
    }
}
