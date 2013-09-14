package de.rallye;

import de.rallye.admin.ServerConsole;
import de.rallye.config.ConfigTools;
import de.rallye.config.GitRepositoryState;
import de.rallye.config.RallyeConfig;
import de.rallye.db.DataAdapter;
import de.rallye.injection.RallyeBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StadtRallye {

	// this is needed to minimize the logging from Jersey in console
	private final static java.util.logging.Logger COM_LOGGER = java.util.logging.Logger.getLogger("com");
	private final static java.util.logging.Logger ORG_LOGGER = java.util.logging.Logger.getLogger("org");
	static {
//		ORG_LOGGER.setLevel(Level.SEVERE);
//		COM_LOGGER.setLevel(Level.SEVERE);
	}

	private static Logger logger = LogManager.getLogger(StadtRallye.class);

	public static void main(String[] args) {

		logger.info("Starting RallyeServer");

		//Init resources
		RallyeConfig config = RallyeConfig.fromFile(ConfigTools.findConfigFile(), GitRepositoryState.getState());
		RallyeBinder.config = config;
		RallyeBinder.data = DataAdapter.getInstance(config);
		
		//start server
		String host = (args.length > 0 ? args[0] : config.getHostName());
		RallyeServer server = new RallyeServer(host, config.getRestPort());

		ServerConsole console = new ServerConsole(config.getConsolePort(), server);
		console.start();
	}
}