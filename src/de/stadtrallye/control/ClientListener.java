package de.stadtrallye.control;

/**
 * @author Felix H�bner
 * @version 1.0
 *
 */
public class ClientListener {
	/**
	 * this method init the ClientListener
	 * @param Port
	 */
	public ClientListener(int Port) {
		
		
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
