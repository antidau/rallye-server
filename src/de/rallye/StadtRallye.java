package de.rallye;

import java.beans.PropertyVetoException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rallye.control.GameConsole;
import de.rallye.control.GameHandler;

/**
 * @author Felix H�bner
 * @version 1.0
 *
 */
public class StadtRallye {

	/**
	 * @param args
	 * @author Felix H�bner
	 * @throws PropertyVetoException 
	 */
	public static void main(String[] args) throws PropertyVetoException {
		Logger logger =  LogManager.getLogger(StadtRallye.class.getName());
		//create and init new GameHandler		
		GameHandler game = new GameHandler((args.length > 0 ? args[0]: null));
		
	}

}
