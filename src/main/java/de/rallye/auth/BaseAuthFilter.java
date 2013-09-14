package de.rallye.auth;

import java.security.Principal;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rallye.db.IDataAdapter;

public abstract class BaseAuthFilter implements ContainerRequestFilter {

	private final Logger logger = LogManager.getLogger(BaseAuthFilter.class);

	@Inject
	protected IDataAdapter data;
	
    /**
     * Apply the filter : check input request, validate or not with user auth
     * @param containerRequest The request from Tomcat server
     */
    @Override
    public void filter(ContainerRequestContext containerRequest) throws WebApplicationException {
    	logger.entry();

        String auth = containerRequest.getHeaderString("authorization");
 
        // No Basic Auth was provided
        if(auth == null){
            throw new WebApplicationException(getUnauthorized());
        }
 
        String[] login = BasicAuth.decode(auth);
 
        // Not matching Basic auth conventions:  user:password
        if(login == null || login.length != 2){
        	throw new WebApplicationException(getUnauthorized());
        }
        
        checkAuthentication(containerRequest, login);
        
//        return logger.exit(containerRequest);
//        return containerRequest;
		logger.exit();
    }
    
    protected abstract Response getUnauthorized(); 
    
    protected abstract Principal checkAuthentication(ContainerRequestContext containerRequest, String[] login);
}