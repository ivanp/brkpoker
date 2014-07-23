package com.brkpoker.texasholdem;

/* Socket IO */
import com.corundumstudio.socketio.listener.*;
import com.corundumstudio.socketio.*;

/* Texas Holdem */
import org.ozsoft.texasholdem.*;

/**
 * Hello world!
 *
 */
public class App 
{
	public static final int ServerPort = 8080;
	
    public static void main( String[] args )
    {
		Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(ServerPort);
        System.out.println( "Server configured at port " +  ServerPort);
		
		Table table = new Table(TableType.NO_LIMIT, 20);
		System.out.println( "Created table" );
    }
}
