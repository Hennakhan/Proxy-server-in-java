
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;


public class ProxyServer {

	private ServerSocket serverSocket;

	private volatile boolean running = true;

	
	/**
	 *  Main method for the program
	 */
	public static void main(String[] args) {
		// Create an instance of Proxy and begin listening for connections
		ProxyServer myProxy = new ProxyServer(8032);
		myProxy.listen();	
	}

	
	
	//  Create the Proxy Server at port 
	 
	public ProxyServer(int port) {

		try {
			// Create the Server Socket for the Proxy 
			serverSocket = new ServerSocket(port);

			System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "..");
			running = true;
		} 

		// Catch exceptions associated with opening socket
		catch (SocketException se) {
			System.out.println("Socket Exception when connecting to client");
			se.printStackTrace();
		}
		catch (SocketTimeoutException ste) {
			System.out.println("Timeout occured while connecting to client");
		} 
		catch (IOException io) {
			System.out.println("IO exception when connecting to client");
		}
	}

	 
	public void listen(){

		while(running){
			try {
				// serverSocket.accpet() Blocks until a connection is made
				Socket socket = serverSocket.accept();
				
				// Create new Thread and pass it a Runnable object which is RequestThreadHandler
				Thread thread = new Thread(new RequestThreadHandler(socket));
				
				thread.start();	
			} catch (SocketException e) {
				// Socket exception is triggered by management system to shut down the proxy 
				//System.out.println("Server closed");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	}
