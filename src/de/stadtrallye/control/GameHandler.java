package de.stadtrallye.control;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;

import de.stadtrallye.model.DataHandler;
import com.google.android.gcm.server.*;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

/**
 * @author Felix H�bner
 * @version 1.0
 * 
 */
public class GameHandler {
	DataHandler data;
	ClientListener listener;
	private static URI BASE_URI = null;
	private HttpServer httpServer = null;
	
	
	//this is needed to minimise the logging from Jersey in console
	private final static Logger COM_SUN_JERSEY_LOGGER = Logger.getLogger( "com.sun.jersey" ); 
	static { COM_SUN_JERSEY_LOGGER.setLevel( Level.SEVERE ); }

	public GameHandler() {
		// create and init new DataHander
		data = new DataHandler();
		
		// create URI 
		//TODO get uri and port from DataHandler
		BASE_URI = UriBuilder.fromUri("http://"+this.data.getUri()+"/").port(this.data.getPort()).build();

		// start server
		try {
			httpServer = startServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Google GCM, snippet
	 */
	public void push() {
		Sender sender = new Sender("AIzaSyBvku0REe1MwJStdJ7Aye6NC7bwcSO-TG0");
		Message msg = new Message.Builder().build();
		try {
			Result res = sender.send(msg, "", 3);// RegIds

			if (res.getMessageId() != null) {
				String canonicalRegId = res.getCanonicalRegistrationId();
				if (canonicalRegId != null) {
					// same device has more than on registration ID: update
					// database
				}
			} else {
				String error = res.getErrorCodeName();
				if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
					// application has been removed from device - unregister
					// database
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 * @author Felix H�bner
	 */
	protected static HttpServer startServer() throws IOException {
		System.out.println("Starting grizzly...");
		ResourceConfig rc = new PackagesResourceConfig("de.stadtrallye.control");
		
		HttpServer serv = GrizzlyServerFactory.createHttpServer(BASE_URI, rc);
		return serv;
	}

	/**
	 * 
	 * 
	 * @author Felix H�bner
	 */
	public void stopServer() {
		if (this.httpServer != null) {
			this.httpServer.stop();
		}
	}
}
