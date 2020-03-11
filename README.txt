
 The Proxy creates a Server Socket which will wait for connections on port 8032. Once a connection arrives and a socket is accepted, the Proxy creates a RequesThreadtHandler object on a new thread and passes the socket to it to be handled.
 This allows the Proxy to continue accept further connections while others are being handled.

 The Proxy runs both HTTP and HTTPS request. There are 2 files ProxyServer.java and RequestThreadHandler.java
 Steps to Run the proxy. 

    (Both the files ProxyServer.java and RequestThreadHandler.java must be in the same folder)
 2. Type Command: javac ProxyServer.java 
	(To Compile ProxyServer )
 3. Type Command: javac RequestThreadHandler.java 
	(To Compile RequestThreadHandler )
 4. Type Command: java ProxyServer 
	(Now run file ProxyServer.java )
 5. Proxy will start at port 8032

 To test the proxy we are using remote desktop
 1. Open Firefox (https is not working on chrome)
 2. Go to option set proxy to manual proxy configuration, Set SSL and HTTP proxy to system64.cs.mtsu.edu on port 8032
 3. Go to browser for http type: http://www.ucla.edu/ and http://www.ox.ac.uk/
 4. For https type : https://www.google.com/ and https://www.youtube.com/


 The header for all the requests are getting saved in HTTPHeader.txt file

 Source file:
 ProxyServer.java , RequestThreadHandler.java, HTTPHeader


 
