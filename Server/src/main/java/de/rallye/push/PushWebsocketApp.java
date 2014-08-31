/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallySoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.rallye.push;

import de.rallye.db.IDataAdapter;
import de.rallye.filter.auth.KnownUserAuthFilter;
import de.rallye.filter.auth.RallyePrincipal;
import de.rallye.model.structures.UserInternal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.websockets.*;

import javax.ws.rs.WebApplicationException;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PushWebsocketApp extends WebSocketApplication implements
		IPushAdapter {
	private static final Logger logger = LogManager
			.getLogger(PushWebsocketApp.class);

	//Map from UserID to associated Socket
	Map<Integer, WebSocket> sockets = new ConcurrentHashMap<Integer, WebSocket>();

	private PushWebsocketApp() {

	}

	
	@Override
public boolean isApplicationRequest(HttpRequestPacket request) {
	logger.info("check for uri "+request.getRequestURI());
    return "/rallye/push".equals(request.getRequestURI());
}
	private static PushWebsocketApp instance = new PushWebsocketApp();

	public static PushWebsocketApp getInstance() {
		return instance;
	}

	/**
	 * Creates a customized {@link WebSocket} implementation.
	 * 
	 * @return customized {@link WebSocket} implementation -
	 *         {@link PushWebSocket}
	 */
	@Override
	public WebSocket createSocket(ProtocolHandler handler, HttpRequestPacket request, WebSocketListener... listeners) {
		return new PushWebSocket(handler, request, listeners);
	}

	/**
	 * Method is called, when {@link PushWebSocket} receives a {@link Frame}.
	 * 
	 * @param websocket
	 *            {@link PushWebSocket}
	 * @param data
	 *            {@link Frame}
	 */
	@Override
	public void onMessage(WebSocket websocket, String data) {
		try {
			logger.info(data);
			ClientMessage message = mapper.readValue(data, ClientMessage.class);
			message.app = this;
			message.handleMessage(websocket);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onConnect(WebSocket socket) {

			logger.info("new websocket connection");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClose(WebSocket websocket, DataFrame frame) {

			logger.info("websocket closed");
	}

	static class PushMessage{
		public String payload;
		public de.rallye.model.structures.PushEntity.Type type;
	}

	ObjectMapper mapper = new ObjectMapper();

	private static IDataAdapter data;
	
	@Override
	public void push(List<UserInternal> users, String payload,
			de.rallye.model.structures.PushEntity.Type type) {
		PushMessage msg = new PushMessage();
		msg.payload = payload;
		msg.type = type;
		
		try {
			String send = mapper.writeValueAsString(msg);
			for (UserInternal user : users) {
				WebSocket socket = sockets.get(user.userID);
				if (socket==null) {
					logger.warn("Push user without socket: "+user.userID);
					continue;
				}
				if (!socket.isConnected()){
					logger.warn("Push user with disconnected socket: "+user.userID);
					continue;
				}
				socket.send(send);
			}
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({ @Type(value = LoginMessage.class, name = "login") })
	static abstract class ClientMessage {
		public String type;
		@JsonIgnore
		PushWebsocketApp app;

		abstract void handleMessage(WebSocket socket);
	}

	static class LoginMessage extends ClientMessage {
		public String username;
		public String password;

		@Override
		void handleMessage(WebSocket socket) {
			try {
				logger.debug("Trying to authenticate user");
				
				KnownUserAuthFilter auth = new KnownUserAuthFilter(data);
				RallyePrincipal p = auth.checkAuthentication(new String[]{username, password});
			
				int userID = p.getUserID();
				logger.debug("User authenticated as "+userID);
					
				socket.send("{\"type\":\"login\", \"state\": \"ok\"}");
				
				
				app.sockets.put(userID,socket);
				((PushWebSocket)socket).setUser(userID);
						
			} catch (WebApplicationException e) {
				socket.send("{\"type\":\"login\", \"state\": \"fail\", \"message\": \"Unauthorized.\"}");
				logger.debug("User unauthorized");
			} catch (Exception e) {
				socket.send("{\"type\":\"login\", \"state\": \"error\", \"message\": \""
						+ e.getMessage() + "\"}");
				e.printStackTrace();
			}
			logger.debug("Logging in with " + username + " " + password);
		}
	}

	public static void setData(IDataAdapter data) {
		PushWebsocketApp.data = data;		
	}
}
