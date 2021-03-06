/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallyeSoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RallyeSoft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.rallye.api;

import de.rallye.admin.AdminWebsocketApp;
import de.rallye.annotations.AdminAuth;
import de.rallye.annotations.KnownUserAuth;
import de.rallye.annotations.KnownUserOrAdminAuth;
import de.rallye.db.IDataAdapter;
import de.rallye.exceptions.DataException;
import de.rallye.exceptions.InputException;
import de.rallye.filter.auth.RallyePrincipal;
import de.rallye.images.ImageRepository;
import de.rallye.model.structures.*;
import de.rallye.util.HttpCacheHandling;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Arrays;
import java.util.List;

@Path("games/rallye/tasks")
@Produces({"application/x-jackson-smile;qs=0.8", "application/xml;qs=0.9", "application/json;qs=1"})
public class Tasks {

	public static final int API_VERSION = 5;
	public static final String API_NAME = "ist_rallye";

	private static final Logger logger =  LogManager.getLogger(Tasks.class);

	@Inject	IDataAdapter data;
	@Inject RallyeGameState gameState;

	@Inject java.util.Map<String, SubmissionPictureLink> submissionPictureMap;
	
	@GET
	@KnownUserOrAdminAuth
	public List<Task> getTasks(@Context SecurityContext sec, @Context Request request) throws DataException {
		logger.entry();

		HttpCacheHandling.checkModifiedSince(request, data.getTasksLastModified());

		//Group id is only passed to getTasks to include ratings
		Integer groupID = null;
		if (sec.getUserPrincipal() instanceof RallyePrincipal) {
			RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
			if (gameState.isShowRatingToUsers())
				groupID = p.getGroupID();
		}
		List<Task> res = data.getTasks(groupID);
		
		return logger.exit(res);
	}

	@PUT
	@Path("{taskID}/primary")
	@KnownUserAuth
	public PrimarySubmissionConfig setPrimarySubmission(@Context SecurityContext sec, @PathParam("taskID") int taskID, PrimarySubmissionConfig primary) {
		logger.entry();

		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
		logger.warn("#Primary Submission for group {}, task {} is now submission {}", p.getGroupID(), taskID, primary);

//		primary = data.setPrimarySubmission(p.getGroupID(), taskID, primary);//TODO

		return logger.exit(primary);
	}
	
	@GET
	@Path("{taskID}")
	@KnownUserAuth
	public List<Submission> getSubmissionsForTask(@PathParam("taskID") int taskID, @Context SecurityContext sec) throws DataException {
		logger.entry();
		
		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
		
		List<Submission> res = data.getSubmissions(taskID, p.getGroupID());
		return logger.exit(res);
	}
	
	@GET
	@Path("all/{groupID}")
	@AdminAuth
	public List<TaskSubmissions> getAllSubmissionsForGroup(@PathParam("groupID") int groupID, @Context SecurityContext sec, @Context Request request) throws DataException {
		logger.entry();

		boolean includeRatings = true;
		
//		if (!p.hasRightsForTaskScoring()) {
//			logger.warn("admin {} has no access rights taskScoring", p.getAdminID());
//			throw new WebApplicationException(Response.Status.FORBIDDEN);
//		}

//		HttpCacheHandling.checkModifiedSince(request, data.getSubmissionsLastModified());//TODO
		
		List<TaskSubmissions> res = data.getAllSubmissions(groupID, includeRatings);
		return logger.exit(res);
	}

	@GET
	@Path("all")
	@KnownUserAuth
	public List<TaskSubmissions> getAllSubmissionsForGroup(@Context SecurityContext sec, @Context Request request) throws DataException {
		logger.entry();

		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();
		int groupID = p.getGroupID();

		boolean includeRatings = gameState.isShowRatingToUsers();

//		HttpCacheHandling.checkModifiedSince(request, data.getSubmissionsLastModified());//TODO

		List<TaskSubmissions> res = data.getAllSubmissions(groupID, includeRatings);
		return logger.exit(res);
	}
	
	@GET
	@Path("byTask/{taskID}")
	@AdminAuth
	public List<TaskSubmissions> getAllByTask(@PathParam("taskID") int taskID) throws DataException {
		logger.entry();
		
		List<TaskSubmissions> res = data.getSubmissionsByTask(taskID, true);
		return logger.exit(res);
	}
	
	@GET
	@Path("unrated")
	@AdminAuth
	public List<TaskSubmissions> getUnrated() throws DataException {
		logger.entry();
		
		List<TaskSubmissions> res = data.getUnratedSubmissions(true);
		return logger.exit(res);
	}
	
	@PUT
	@Path("{taskID}")
	@Consumes(MediaType.APPLICATION_JSON)
	@KnownUserAuth
	public Submission submit(GeoPostSubmission submission, @PathParam("taskID") int taskID, @Context SecurityContext sec) throws DataException, InputException {
		logger.entry();

		if (!gameState.isCanSubmit())
			throw new InputException("Submitting disabled.");
		
		RallyePrincipal p = (RallyePrincipal) sec.getUserPrincipal();

		logger.info("Received submission from location: {}", submission.location);//TODO save to submission table

		Submission res;

		if (submission.picSubmission!=null) {
			SubmissionPictureLink link = SubmissionPictureLink.getLink(submissionPictureMap, submission.picSubmission, data);

			ImageRepository.Picture picture = link.getPicture();
			if (picture != null) {
				logger.debug("Resolved pictureHash {} to picID {}", submission.picSubmission, picture.getPictureID());
				res = data.submit(taskID, p.getGroupID(), p.getUserID(), submission, picture);
			} else {
				res = data.submit(taskID, p.getGroupID(), p.getUserID(), submission, null);
				link.setObject(res);
				logger.debug("Unresolved pictureHash");
			}
		} else
			res = data.submit(taskID, p.getGroupID(), p.getUserID(), submission, null);

		AdminWebsocketApp.getInstance().newSubmission(p.getGroupID(),p.getUserID(),taskID,res);
		return logger.exit(res);
	}
	
	@POST
	@Path("score")
	@Consumes(MediaType.APPLICATION_JSON)
	@AdminAuth
	public Response setScores(SubmissionScore[] scores) throws DataException {
		logger.entry();
		
		logger.info("Writing scores: "+Arrays.toString(scores));
		
		data.scoreSubmissions(scores);
		
		return logger.exit(Response.ok().build());
	}
}