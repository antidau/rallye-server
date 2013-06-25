package de.rallye.api;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rallye.RallyeResources;
import de.rallye.RallyeServer;
import de.rallye.exceptions.WebAppExcept;

@Path("client")
public class Client {
	private static final String RESOURCE_PATH = "de/rallye/webclient/";

	private Logger logger =  LogManager.getLogger(Client.class);

	private RallyeResources R = RallyeServer.getResources();

	@GET
	@Path("{path}")
	public File index(@PathParam("path") String path, @Context SecurityContext sec) {
		
		if (path.contains("/"))
			throw new WebAppExcept(404, "Not found.");
		
		logger.debug("Trying to load "+RESOURCE_PATH+path);
		
		URL url = this.getClass().getClassLoader().getResource(RESOURCE_PATH+path);
		
		if (url==null) {

			throw new WebAppExcept(404, "Not found. 42");
		}
		
		try {
			return new File(url.toURI());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new WebAppExcept(404, "Not found.");
		}
		
	}
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	public File index(@Context SecurityContext sec) {
		
		return index("index.html",sec);
		
	}
}