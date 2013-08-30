package de.rallye.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rallye.auth.GroupPrincipal;
import de.rallye.auth.RallyePrincipal;
import de.rallye.exceptions.DataException;
import de.rallye.exceptions.InputException;
import de.rallye.exceptions.UnauthorizedException;
import de.rallye.model.structures.ChatEntry;
import de.rallye.model.structures.Chatroom;
import de.rallye.model.structures.Edge;
import de.rallye.model.structures.Group;
import de.rallye.model.structures.GroupUser;
import de.rallye.model.structures.LoginInfo;
import de.rallye.model.structures.Node;
import de.rallye.model.structures.PushConfig;
import de.rallye.model.structures.PushMode;
import de.rallye.model.structures.ServerConfig;
import de.rallye.model.structures.SimpleChatEntry;
import de.rallye.model.structures.SimpleSubmission;
import de.rallye.model.structures.Submission;
import de.rallye.model.structures.Task;
import de.rallye.model.structures.TaskSubmissions;
import de.rallye.model.structures.UserAuth;
import de.rallye.model.structures.UserInternal;

public interface IDataAdapter {

	public abstract List<Group> getGroups() throws DataException;

	public abstract List<Task> getTasks() throws DataException;

	public abstract List<Submission> getSubmissions(int taskID, int groupID)
			throws DataException;

	public abstract List<TaskSubmissions> getAllSubmissions(int groupID)
			throws DataException;

	public abstract Submission submit(int taskID, int groupID, int userID,
			SimpleSubmission submission) throws DataException, InputException;

	public abstract Map<Integer, Node> getNodes();

	public abstract List<Edge> getEdges();

	public abstract ServerConfig getServerConfig() throws DataException;

	public abstract RallyePrincipal getKnownUserAuthorization(int groupID,
			int userID, String password) throws DataException,
			UnauthorizedException, InputException;

	public abstract GroupPrincipal getNewUserAuthorization(int groupID,
			String password) throws DataException, UnauthorizedException;

	public abstract UserAuth login(int groupID, LoginInfo info)
			throws DataException, InputException;

	public abstract boolean logout(int groupID, int userID)
			throws DataException;

	public abstract boolean hasRightsForChatroom(int groupID, int roomID)
			throws DataException;

	public abstract List<Chatroom> getChatrooms(int groupID)
			throws DataException;

	public abstract List<ChatEntry> getChats(int roomID, long timestamp,
			int groupID) throws DataException, UnauthorizedException;

	public abstract ChatEntry addChat(SimpleChatEntry chat, int roomID,
			int groupID, int userID) throws DataException;

	public abstract void setPushConfig(int groupID, int userID, PushConfig push)
			throws DataException;

	public abstract PushConfig getPushConfig(int groupID, int userID)
			throws DataException;

	public abstract List<PushMode> getPushModes() throws DataException;

	public abstract List<GroupUser> getAllUsers() throws DataException;

	public abstract List<UserInternal> getMembers(int groupID)
			throws DataException;

	public abstract List<UserInternal> getChatroomMembers(int roomID)
			throws DataException;

	public abstract int assignNewPictureID(int userID) throws DataException;

	public abstract void editChatAddPicture(int chatID, int pictureID)
			throws DataException;

	public abstract void updatePushIds(HashMap<String, String> changes)
			throws DataException;

}