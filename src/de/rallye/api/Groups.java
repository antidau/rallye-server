package de.rallye.api;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import de.rallye.auth.NewUserAuth;
import de.rallye.auth.RallyePrincipal;
import de.rallye.exceptions.DataException;
import de.rallye.exceptions.InputException;
import de.rallye.model.structures.Group;
import de.rallye.model.structures.LoginInfo;
import de.rallye.model.structures.PushConfig;
import de.rallye.model.structures.User;
import de.rallye.model.structures.UserAuth;

@Path("rallye/groups")
public class Groups {
	
	private Logger logger =  LogManager.getLogger(Groups.class);

	private RallyeResources R = RallyeServer.getResources();

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Group> getGroups() {
		logger.entry();
		
		try {
			List<Group> res = R.data.getGroups();
			return logger.exit(res);
		} catch (DataException e) {
			logger.error("getGroups failed", e);
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@GET
	@Path("{groupID}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<? extends User> getMembers(@PathParam("groupID") int groupID) {
		logger.entry();
		
		try {
			List<? extends User> res = R.data.getMembers(groupID);
			return logger.exit(res);
		} catch (DataException e) {
			logger.error("getGroups failed", e);
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@GET
	@Path("{groupID}/avatar")
	@Produces("image/jpeg")
	public BufferedImage getGroupAvatar(@PathParam("groupID") int groupID) {
		File f = new File("game/"+ groupID +"/avatar.jpg");
		
		try {
			BufferedImage avt = ImageIO.read(f);
			return avt;
		} catch (IOException e) {
			logger.error(e);
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@GET
	@Path("{groupID}/{userID}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUserInfo(@PathParam("groupID") int groupID, @PathParam("userID") int userID) {
		throw new NotImplementedException();
	}
	
	@GET
	@Path("{groupID}/{userID}/pushSettings")
	@Produces(MediaType.APPLICATION_JSON)
	public PushConfig getPushSettings(@PathParam("groupID") int groupID, @PathParam("userID") int userID, @Context SecurityContext sec) {
		logger.entry();
		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
		
		p.checkBothMatch(userID, groupID);
		
		try {
			PushConfig res = R.data.getPushConfig(groupID, userID);
			return logger.exit(res);
		} catch (DataException e) {
			logger.error("logout failed", e);
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@POST
	@Path("{groupID}/{userID}/pushSettings")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response setPushConfig(@PathParam("groupID") int groupID, @PathParam("userID") int userID, PushConfig push, @Context SecurityContext sec) {
		logger.entry();
		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
		
		p.checkBothMatch(userID, groupID);
		
		try {
			R.data.setPushConfig(groupID, userID, push);
			return Response.ok().build();
		} catch (DataException e) {
			logger.error("logout failed", e);
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PUT
	@Path("{groupID}")
	@ResourceFilters(NewUserAuth.class)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public UserAuth login(@PathParam("groupID") int groupID, LoginInfo info, @Context SecurityContext sec) {
		logger.entry();
		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
		
		int authGroup = p.getGroupID();
		
		p.checkGroupMatches(groupID);
		
		try {
			UserAuth login = R.data.login(authGroup, info);
			return login;
		} catch (DataException e) {
			logger.error("login failed", e);
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		} catch (InputException e) {
			logger.catching(e);
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}
		
	}
	
	@DELETE
	@Path("{groupID}/{userID}")
	@ResourceFilters(KnownUserAuth.class)
	public Response logout(@PathParam("groupID") int groupID, @PathParam("userID") int userID, @Context SecurityContext sec) {
		logger.entry();
		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
		
		p.checkBothMatch(userID, groupID);
		
		try {
			R.data.logout(groupID, userID);
			return Response.ok().build();
		} catch (DataException e) {
			logger.error("logout failed", e);
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
}
