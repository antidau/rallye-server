package de.rallye.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.sun.jersey.spi.container.ResourceFilters;

import de.rallye.RallyeResources;
import de.rallye.RallyeServer;
import de.rallye.auth.KnownUserAuth;
import de.rallye.auth.RallyePrincipal;
import de.rallye.db.DataAdapter;
import de.rallye.exceptions.DataException;
import de.rallye.exceptions.InputException;
import de.rallye.model.structures.ChatEntry;
import de.rallye.model.structures.Chatroom;
import de.rallye.model.structures.SimpleChatEntry;

@Path("rallye/chatrooms")
public class Chatrooms {
	
	private Logger logger =  LogManager.getLogger(Chatrooms.class);

	private RallyeResources R = RallyeServer.getResources();

	@GET
	@ResourceFilters(KnownUserAuth.class)
	@Produces(MediaType.APPLICATION_JSON)
	public List<Chatroom> getChatrooms(@Context SecurityContext sec) {
		logger.entry();
		
		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
		
		try {
			List<Chatroom> res = R.data.getChatrooms(p.getGroupID());
			return logger.exit(res);
		} catch (DataException e) {
			logger.error("getChatrooms failed", e);
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@GET
	@ResourceFilters(KnownUserAuth.class)
	@Path("{roomID}/since/{timestamp}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<ChatEntry> getChats(@PathParam("roomID") int roomID, @PathParam("timestamp") long timestamp, @Context SecurityContext sec) {
		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
		return getChats(roomID, timestamp, p.getGroupID());
	}
	
	@GET
	@ResourceFilters(KnownUserAuth.class)
	@Path("{roomID}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<ChatEntry> getChats(@PathParam("roomID") int roomID, @Context SecurityContext sec) {
		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
		return getChats(roomID, 0, p.getGroupID());
	}
	
	private List<ChatEntry> getChats(int roomID, long timestamp, int groupID) {
		logger.entry();
		
		try {
			if (!R.data.hasRightsForChatroom(groupID, roomID)) {
				logger.warn("group "+ groupID +" has no access rights for chatroom "+ roomID);
			}
			
			List<ChatEntry> res = R.data.getChats(roomID, timestamp, groupID);
			return logger.exit(res);
		} catch (DataException e) {
			logger.error("getChats failed", e);
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PUT
	@ResourceFilters(KnownUserAuth.class)
	@Path("{roomID}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ChatEntry addChat(@PathParam("roomID") int roomID, SimpleChatEntry chat, @Context SecurityContext sec) {
		logger.entry();
		
		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
		int groupID = p.getGroupID();
		int userID = p.getUserID();
		
		if (chat.message.isEmpty())
			throw new WebApplicationException(new InputException("Message must not be empty"), Response.Status.BAD_REQUEST);
		
		try {
			if (!R.data.hasRightsForChatroom(groupID, roomID)) {
				logger.warn("group "+ groupID +" has no access rights for chatroom "+ roomID);
			}
			
			ChatEntry res = R.data.addChat(chat, roomID, groupID, userID);
			return logger.exit(res);
		} catch (DataException e) {
			logger.error("getChats failed", e);
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PUT
	@ResourceFilters(KnownUserAuth.class)
	@Path("{roomID}/{hash}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ChatEntry addChatWithHash(@Context SecurityContext sec, @PathParam("hash") String hash) {
		throw new NotImplementedException();//TODO
	}
	
}
