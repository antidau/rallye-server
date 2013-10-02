package de.rallye.filter.auth;

import java.security.Principal;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rallye.annotations.KnownUserOrAdminAuth;

@KnownUserOrAdminAuth
@Provider
@Priority(Priorities.AUTHENTICATION)
public class KnownUserOrAdminAuthFilter extends BaseAuthFilter{
	private static Logger logger = LogManager.getLogger(KnownUserOrAdminAuthFilter.class);
	protected Response getUnauthorized(String message) {
		if (message!=null)
			return Response.status(Status.UNAUTHORIZED).entity(message).header("WWW-Authenticate", "Basic realm=\"RallyeAuth\"").build();
		else
			return Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic realm=\"RallyeAuth\"").build();
	}
	
	protected Response getUnauthorized() {
		return getUnauthorized(null);
	}
	
	/*public KnownUserOrAdminAuthFilter() {
		knownUserAuth = new KnownUserAuthFilter();
		adminAuth = new AdminAuthFilter();
	}
	KnownUserAuthFilter knownUserAuth;
	AdminAuthFilter adminAuth;
	*/

	@Override
	protected Principal checkAuthentication(
			ContainerRequestContext containerRequest, String[] login) {
		if (data==null)
			logger.warn("Data is null");
		else logger.info("Data ok");
		try {
			return (new KnownUserAuthFilter(data)).checkAuthentication(containerRequest, login);
		} catch (WebApplicationException e) {
			try {
				return (new AdminAuthFilter(data)).checkAuthentication(containerRequest, login);
			} catch (WebApplicationException f) {
				//Rethrow exception generated by knownUserAuth,
				throw e;
			}
		}
	}

}
