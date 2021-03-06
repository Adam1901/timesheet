package timesheet.fun;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import timesheet.Application;
import timesheet.DTO.DTOProjectTimeSheet;
import timesheet.DTO.DTOTime;
import timesheet.connection.ConnectionManager;
import timesheet.connection.DBEngine.DbEngine;
import timesheet.panels.MainWindow;
import timesheet.utils.Props;

public class Achievement {
	// Logged more than 10 hours in a day
	private static final String _10HID = "10HID";
	// Logged more than 50 hours in a week
	private static final String _50HIW = "50HIW";
	// logged 24 hours in a day
	private static final String _100HIP = "100HIP";
	// log more than 100 hours to a project
	private static final String _1000HIP = "1000HIP";

	boolean didIWin = false;

	public void calculateAchievements() throws SQLException {
		FunDBEngine funDbEngine = new FunDBEngine();
		DbEngine db = new DbEngine();
		try (Connection connection = ConnectionManager.getConnection();) {
			List<DTOTime> allTimeForResource = funDbEngine.getAllTimeForResource(connection, Application.resource);
			for (DTOTime dtoTime : allTimeForResource) {
				if (dtoTime.getLogged() >= 10.0 && testBefore(_10HID)) {
					Props.setProperty(_10HID, "1");
					showMessage("You have done 10 hours this week!");
				} else if (dtoTime.getLogged() == 24.0 && testBefore(_50HIW)) {
					Props.setProperty(_50HIW, "1");
					showMessage("You have logged 50 hours  for the first time!");
				}
			}

			HashMap<DTOProjectTimeSheet, List<DTOTime>> loggedTimeByResource = db.getLoggedTimeByResource(connection,
					Application.resource, null, null);
			for (Entry<DTOProjectTimeSheet, List<DTOTime>> dtoTime : loggedTimeByResource.entrySet()) {
				double timeInProj = 0.0;
				for (DTOTime dtoTime2 : dtoTime.getValue()) {
					timeInProj += dtoTime2.getLogged();
				}

				if (timeInProj >= 100 && testBefore(_100HIP)) {
					Props.setProperty(_100HIP, "1");
					showMessage("You have logged 100 hours against a project. Congratulations!");
				} else if (timeInProj >= 1000 && testBefore(_1000HIP)) {
					Props.setProperty(_1000HIP, "1");
					showMessage(
							"You have logged 1000 hours against a project. Go take a break and make a cup of tea ;)\nYou've earned it! ");
				}
			}

			if (!didIWin) {
				String funfact = funDbEngine.getFunFact(connection);
				JOptionPane.showConfirmDialog(null, "You didn't win anything :(\nBut here is a fun fact!\n" + funfact);
			}
		}
	}

	private boolean testBefore(String string) {
		return !(Props.getProperty(string) != null && Props.getProperty(string).equals("1"));
	}

	private void showMessage(String message) {
		JOptionPane.showConfirmDialog(null, message);
		MainWindow.sendNotification(message);
		didIWin = true;
	}
}
