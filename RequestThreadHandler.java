import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.io.FileWriter;
public class RequestThreadHandler implements Runnable {

	/**
	 * Socket connected to client passed by Proxy server
	 */
	Socket clientSocket;

	/**
	 * Read data client sends to proxy
	 */
	BufferedReader readFromClient;

	/**
	 * Send data from proxy to client
	 */
	BufferedWriter writeToClient;
	
	/**
	 * Send bytes from proxy to client
	 */
	DataOutputStream proxyToClientStream;
	
	/**
	 * Thread that is used to transmit data read from client to server when using HTTPS
	 * Reference to this is required so it can be closed once completed.
	 */
	private Thread httpsClientToServer;

	/**
	 * Creates a ReuqestHandler object capable of servicing HTTP(S) GET requests
	 * @param clientSocket socket connected to the client
	 */
	public RequestThreadHandler(Socket clientSocket){
		this.clientSocket = clientSocket;
		try{
			this.clientSocket.setSoTimeout(4000);
			readFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			writeToClient = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			proxyToClientStream  =
					new DataOutputStream(clientSocket.getOutputStream());

		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	
	/**
	 * Reads and examines the requestString and calls the appropriate method based 
	 * on the request type. 
	 */
	@Override
	public void run() {
		
		// Get Request from client
		String requestString;
		try{
			requestString = readFromClient.readLine();
		} catch (IOException e) {
			/*e.printStackTrace();
			System.out.println("Error reading request from client");*/
			return;
		}
		
		try {
			WriteToFile("\n");
			WriteToFile(requestString);
			}
		catch(IOException e)
			{
			
			}		
		
		// Parse out URL
		// Get the Request type
		String request = requestString.substring(0,requestString.indexOf(' '));

		// remove request type and space
		String urlString = requestString.substring(requestString.indexOf(' ')+1);

		// Remove everything past next space
		urlString = urlString.substring(0, urlString.indexOf(' '));

		// Prepend http:// if necessary to create correct URL
		if(!urlString.substring(0,4).equals("http")){
			String temp = "http://";
			urlString = temp + urlString;
		}


		// Check request type
		if(request.equals("CONNECT")){
			
			processHttpsRequest(urlString);
		} else{
			
			handleHTTPRequest(urlString);
			
		}
	} 


	/**
	 * Sends the contents of the file specified by the urlString to the client
	 * @param urlString URL of the file requested
	 */
	private void handleHTTPRequest(String urlString){
		String text;
		try{	
			
			
			for(int i=0;i<5;i++)
			{
				text = readFromClient.readLine();
				WriteToFile(text);
				
			}
			
			WriteToFile("\n");
			
			// Create the URL
			URL remoteServerURL = new URL(urlString);

			URLConnection conn = remoteServerURL.openConnection();
			//Only wants to do HTTP GET
			conn.setDoInput(true);
			//HTTP Post is disabled
			conn.setDoOutput(false);

			// Get the response
			InputStream is = null;

			try {
				is = conn.getInputStream();
			} catch (IOException ioe) {
				/*System.out.println(
						"********* IO EXCEPTION **********: " + ioe);
				ioe.printStackTrace(); */
			}

			//begin send response to client
			byte byt[] = new byte[32768];
			int index = is.read( byt, 0, 32768 );
			while ( index != -1 )
			{
				proxyToClientStream.write( byt, 0, index );
				index = is.read( byt, 0, 32768 );
			}
			proxyToClientStream.flush();


			if(proxyToClientStream != null){
				proxyToClientStream.close();
			}
        		
		} 

		catch (Exception e){
			//e.printStackTrace();
		}
	}

	
	/**
	 * Handles HTTPS requests between client and remote server
	 * @param urlString desired file to be transmitted over https
	 */
	private void processHttpsRequest(String urlString){
		// Extract the URL and port of remote 
		String url = urlString.substring(7);
		String pieces[] = url.split(":");
		url = pieces[0];
		int port  = Integer.valueOf(pieces[1]);
		String text;

		try{
			// Read first line of the request i.e connect request 
	
			// Write the request to the file HTTPHeader.txt
			
			for(int i=0;i<5;i++){
				text = readFromClient.readLine();
				WriteToFile(text);
				
			}
			WriteToFile("\n");
			
			// Get IP address from URL 
			InetAddress address = InetAddress.getByName(url);
			
			// Open a socket to the server 
			Socket proxyToServerSocket = new Socket(address, port);
			proxyToServerSocket.setSoTimeout(5000);

			// Send Connection established to the client
			String line = "HTTP/1.0 200 Connection established\r\n" +
					"Proxy-Agent: ProxyServer/1.0\r\n" +
					"\r\n";
			writeToClient.write(line);
			writeToClient.flush();
			
			
			
			// Client and Remote will both start sending data to proxy at this point
			// Proxy needs to asynchronously read data from each party and send it to the other party


			//Create a Buffered Writer between proxy and remote
			BufferedWriter writeToServer = new BufferedWriter(new OutputStreamWriter(proxyToServerSocket.getOutputStream()));

			// Create Buffered Reader from proxy and remote
			BufferedReader readFromServer = new BufferedReader(new InputStreamReader(proxyToServerSocket.getInputStream()));



			// Create a new thread to listen to the client and send it to the Server 
			TransmitBytesFromClientToServer clientToServerHttps = 
					new TransmitBytesFromClientToServer(clientSocket.getInputStream(), proxyToServerSocket.getOutputStream());
			
			httpsClientToServer = new Thread(clientToServerHttps);
			httpsClientToServer.start();
			
			
			// Listening from the server and writing it to the client
			try {
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = proxyToServerSocket.getInputStream().read(buffer);
					if (read > 0) {
						clientSocket.getOutputStream().write(buffer, 0, read);
						if (proxyToServerSocket.getInputStream().available() < 1) {
							clientSocket.getOutputStream().flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException e) {
				
			}
			catch (IOException e) {
				//e.printStackTrace();
			}


			// Close Down Resources
			if(proxyToServerSocket != null){
				proxyToServerSocket.close();
			}

			if(readFromServer != null){
				readFromServer.close();
			}

			if(writeToServer != null){
				writeToServer.close();
			}

			if(writeToClient != null){
				writeToClient.close();
			}
			
			
		} catch (SocketTimeoutException e) {
			String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			try{
				writeToClient.write(line);
				writeToClient.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} 
		catch (Exception e){
			System.out.println("Error on HTTPS : " + urlString );
			e.printStackTrace();
		}
	}

	


	/**
	 * Listen to data from client and transmits it to server.
	 * This is done on a separate thread as must be done 
	 * asynchronously to reading data from server and transmitting 
	 * that data to the client. 
	 */
	
	class TransmitBytesFromClientToServer implements Runnable{
		
		InputStream proxyToClientIS;
		OutputStream proxyToServerOS;
		
		/**
		 * Creates Object to Listen to Client and Transmit that data to the server
		 * @param proxyToClientIS Stream that proxy uses to receive data from client
		 * @param proxyToServerOS Stream that proxy uses to transmit data to remote server
		 */
		public TransmitBytesFromClientToServer(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
			this.proxyToClientIS = proxyToClientIS;
			this.proxyToServerOS = proxyToServerOS;
		}

		@Override
		public void run(){
			try {
				// Read byte by byte from client and send directly to server
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = proxyToClientIS.read(buffer);
					if (read > 0) {
						proxyToServerOS.write(buffer, 0, read);
						if (proxyToClientIS.available() < 1) {
							proxyToServerOS.flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException ste) {
				// TODO: handle exception
			}
			catch (IOException e) {}
		}
	}


	 /**
	  * This function write Request Header to a file HTTPHeader.txt
	  */

	public static void WriteToFile(String text) throws IOException
	{	
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("HttpHeader.txt", true));
		if (text == "\n") 
		{
			writer.newLine();
		}
		else 
		{
			writer.write(text);
			writer.newLine();
		}
		writer.close();							
	}
}




