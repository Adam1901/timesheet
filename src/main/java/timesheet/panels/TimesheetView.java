package timesheet.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.text.NumberFormatter;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import timesheet.Application;
import timesheet.RDNE;
import timesheet.DTO.DTOProject;
import timesheet.DTO.DTOProjectTimeSheet;
import timesheet.DTO.DTOTime;
import timesheet.components.JFormattedTextFieldWithNotes;
import timesheet.connection.ConnectionManager;
import timesheet.connection.DBEngine.DbEngine;
import timesheet.utils.Props;
import timesheet.utils.Utils;

public class TimesheetView {
	private JPanel pan = new JPanel();
	public static final String HIDE_PROPERTY = "hide";

	private List<Row> rows = new ArrayList<>();
	private List<Row> hiddenRows = new ArrayList<>();

	private DateTime dateTime = Utils.getFirstDateOfWeek(new DateTime());

	JLabel lblDay1;
	JLabel lblDay2;
	JLabel lblDay3;
	JLabel lblDay4;
	JLabel lblDay5;
	JLabel lblDay6;
	JLabel lblDay7;
	JLabel lblColumnTotal;
	JTextField txtTot1;
	JTextField txtTot2;
	JTextField txtTot3;
	JTextField txtTot4;
	JTextField txtTot5;
	JTextField txtTot6;
	JTextField txtTot7;
	JTextField txtTotTotal;

	JSeparator sep;

	List<JTextField> txtEndOfRows = new ArrayList<>();
	private JLabel lblRowTotal;
	private JPanel timeview;
	private JLabel lblNewLabel;
	private JScrollPane scrollPane;

	public JPanel getPanel() {
		return pan;
	}

	private JLabel[] getLables() {
		JLabel[] lables = { lblDay1, lblDay2, lblDay3, lblDay4, lblDay5, lblDay6, lblDay7 };
		return lables;
	}

	private JTextField[] getTotalBoxes() {
		JTextField[] lables = { txtTot1, txtTot2, txtTot3, txtTot4, txtTot5, txtTot6, txtTot7 };
		return lables;
	}

	public List<Row> getRows() {
		return rows;
	}

	public DateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(DateTime dateTime) {
		this.dateTime = dateTime;
	}

	/**
	 * Create the panel.
	 * 
	 * @throws SQLException
	 * @throws RDNE
	 */
	public TimesheetView() throws SQLException, RDNE {

		jbInit();
		labelTextInit(getDateTime());
		try (Connection connection = ConnectionManager.getConnection();) {
			createTextFields(connection);
			populateTextField(connection);
		}
		calculateTotals();
		pan.repaint();
		pan.validate();
	}

	public void repopulateTextFields() throws SQLException, RDNE {
		for (Row row : rows) {
			for (JFormattedTextFieldWithNotes ajFormattedTextField : row.getTxtRowDay()) {
				ajFormattedTextField.setVisible(false);
				ajFormattedTextField = null;
			}
			JLabel title = row.getTitle();
			title.setVisible(false);
			title = null;

			JButton hideBtn = row.getHideBtn();
			hideBtn.setVisible(false);
			hideBtn = null;
		}

		rows = new ArrayList<>();
		for (JTextField jTextField : txtEndOfRows) {
			jTextField.setVisible(false);
			jTextField = null;
		}
		txtEndOfRows = new ArrayList<>();
		hiddenRows = new ArrayList<>();

		lblColumnTotal.setText("");
		lblColumnTotal = null;
		pan.remove(sep);
		sep = null;
		try (Connection connection = ConnectionManager.getConnection();) {
			createTextFields(connection);
			populateTextField(connection);
		}
		calculateTotals();
		pan.repaint();
		pan.validate();
	}

	private boolean validateInput() {
		for (Row row : getRows()) {
			for (JTextField jTextField : row.getTxtRowDay()) {
				try {
					Double.valueOf(jTextField.getText());
					jTextField.setBackground(Color.WHITE);
				} catch (NumberFormatException e) {
					String text = jTextField.getText();
					if (text != null && !"".equals(text))
						jTextField.setBackground(Color.RED);
					return false;
				}
			}
		}
		return true;
	}

	private double[] sumColumns(double[][] array) {
		int size = array[0].length;
		double temp[] = new double[size];

		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				temp[j] += array[i][j];
			}
		}
		return temp;
	}

	public void labelTextInit(DateTime dateTime) {
		DateTime first = Utils.getFirstDateOfWeek(dateTime);
		DateTimeFormatter fmt = DateTimeFormat.forPattern("EEE - dd/MM");
		for (Component component : getLables()) {
			((JLabel) component).setText(fmt.print(first));
			first = first.plusDays(1);
		}
	}

	public void populateTextField(Connection connection) throws SQLException, RDNE {
		DbEngine db = new DbEngine();
		HashMap<DTOProjectTimeSheet, List<DTOTime>> loggedTimeByResource = db.getLoggedTimeByResource(connection,
				Application.resource, dateTime, null);

		for (Row row : getRows()) {
			List<DTOTime> times = null;
			for (Entry<DTOProjectTimeSheet, List<DTOTime>> entry : loggedTimeByResource.entrySet()) {
				if (row.getProjectTimesheet().equals(entry.getKey())) {
					times = entry.getValue();
					break;
				}
			}
			if (times == null) {
				times = new ArrayList<>();
			}

			List<JFormattedTextFieldWithNotes> txtRowDay = row.getTxtRowDay();
			DateTime firstDayOfWeek = row.getDate();
			for (JFormattedTextFieldWithNotes jFormattedTextFieldWithNotes : txtRowDay) {
				jFormattedTextFieldWithNotes.setText("0.0");
			}
			for (JFormattedTextFieldWithNotes jTextField : txtRowDay) {
				for (DTOTime dtoTime : times) {
					if (firstDayOfWeek.withTimeAtStartOfDay().equals(dtoTime.getDate().withTimeAtStartOfDay())) {
						String valueOf = Utils.doubleValueOf(dtoTime.getLogged());
						if (valueOf == null || valueOf.equals(""))
							valueOf = "0.0";
						jTextField.setText(valueOf);
						jTextField.setNotes(dtoTime.getNotes());
					}
				}
				firstDayOfWeek = firstDayOfWeek.plusDays(1);
			}
		}
	}

	private void createTextFields(Connection connection) throws SQLException, RDNE {
		DbEngine db = new DbEngine();

		List<DTOProjectTimeSheet> allProjectsTimeSheetForResource = db.getAllProjectsTimeSheetForResource(connection,
				Application.resource);

		// Don't load the ones that are ignored
		String property = Props.getProperty(HIDE_PROPERTY);
		if (property != null && !"".equalsIgnoreCase(property)) {
			List<String> hidden = new ArrayList<String>(Arrays.asList(property.split(",")));
			for (Iterator<DTOProjectTimeSheet> iterator = allProjectsTimeSheetForResource.iterator(); iterator
					.hasNext();) {
				DTOProjectTimeSheet ts = iterator.next();
				for (String id : hidden) {
					int idi = Integer.valueOf(id);
					if (idi == ts.getProjectId()) {
						addHiddenToTotals(connection, db, ts, idi);
						iterator.remove();
					}
				}
			}

			List<String> strs = new ArrayList<String>();
			List<DTOProject> allProjects = db.getAllProjects(connection);
			for (DTOProject dtoProject : allProjects) {
				for (String string : hidden) {
					int id = Integer.valueOf(string);
					if (id == dtoProject.getProjectId())
						strs.add(dtoProject.getProjectName());
				}
			}
			MainWindow.sendNotification("Projects (" + String.join(", ", strs) + ") hidden from view.");
		}

		// Perf fix
		List<DTOProject> allProject = db.getAllProjects(connection);
		int y = 1;
		for (DTOProjectTimeSheet dtoProjectTimeSheet : allProjectsTimeSheetForResource) {
			int x = 2;
			List<JFormattedTextFieldWithNotes> txt = new ArrayList<>();
			for (int i = 0; i < 7; i++) {
				NumberFormat format = NumberFormat.getInstance();
				NumberFormatter formatter = new NumberFormatter(format);
				formatter.setMinimum(0.0);
				formatter.setMaximum(24.0);
				formatter.setCommitsOnValidEdit(true);
				formatter.setValueClass(Double.class);
				JFormattedTextFieldWithNotes txtField = new JFormattedTextFieldWithNotes(formatter);

				GridBagConstraints gbc_textField = new GridBagConstraints();
				gbc_textField.insets = new Insets(0, 0, 5, 5);
				gbc_textField.fill = GridBagConstraints.HORIZONTAL;
				gbc_textField.gridx = x++;
				gbc_textField.gridy = y;
				timeview.add(txtField, gbc_textField);
				txtField.setColumns(2);
				txtField.setText("0.0");
				txtField.setValue(new Double(0.0));
				txtField.addFocusListener(getFocusListener());
				txtField.addKeyListener(getKeyListener());
				txtField.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						txtField.setText("");
					}
				});

				txt.add(txtField);
			}

			JTextField txtEndOfRowTotal = new JTextField();
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.insets = new Insets(0, 0, 5, 5);
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.gridx = x;
			gbc_textField.gridy = y;
			timeview.add(txtEndOfRowTotal, gbc_textField);
			txtEndOfRowTotal.setColumns(2);
			txtEndOfRowTotal.setBackground(Color.LIGHT_GRAY);
			txtEndOfRowTotal.setText("0.0");
			txtEndOfRowTotal.setEditable(false);
			txtEndOfRows.add(txtEndOfRowTotal);

			DTOProject project = null;
			for (DTOProject dtoProject : allProject) {
				if (dtoProject.getProjectId() == dtoProjectTimeSheet.getProjectId()) {
					project = dtoProject;
				}
			}
			JButton hide = new JButton("Hide");
			String projectName = project.getProjectName();
			if (!projectName.equalsIgnoreCase("admin") && !projectName.equalsIgnoreCase("holiday")) {
				GridBagConstraints gbc_hideTbn = new GridBagConstraints();
				gbc_hideTbn.insets = new Insets(0, 0, 5, 5);
				gbc_hideTbn.fill = GridBagConstraints.HORIZONTAL;
				gbc_hideTbn.gridx = ++x;
				gbc_hideTbn.gridy = y;
				hide.addActionListener(hideBtnActionListener(dtoProjectTimeSheet));
				timeview.add(hide, gbc_hideTbn);
			}

			JLabel lblProject = new JLabel(projectName);
			GridBagConstraints gbc_lblProject = new GridBagConstraints();
			gbc_lblProject.insets = new Insets(0, 0, 5, 5);
			gbc_lblProject.anchor = GridBagConstraints.EAST;
			gbc_lblProject.gridx = 1;
			gbc_lblProject.gridy = y;
			timeview.add(lblProject, gbc_lblProject);

			y++;

			rows.add(new Row(lblProject, txt, dtoProjectTimeSheet, Utils.getFirstDateOfWeek(getDateTime()), hide));
		}

		// Create separator
		sep = new JSeparator(JSeparator.HORIZONTAL);
		GridBagConstraints gbc_Sep = new GridBagConstraints();
		gbc_Sep.insets = new Insets(0, 0, 0, 5);
		gbc_Sep.anchor = GridBagConstraints.EAST;
		gbc_Sep.gridx = 1;
		gbc_Sep.gridwidth = 9;
		gbc_Sep.gridy = 2;
		pan.add(sep, gbc_Sep);

		// Create total rows
		int x2 = 2;
		for (JTextField jTextField : getTotalBoxes()) {
			GridBagConstraints gbc_textField = new GridBagConstraints();
			gbc_textField.insets = new Insets(0, 0, 5, 5);
			gbc_textField.fill = GridBagConstraints.HORIZONTAL;
			gbc_textField.gridx = x2++;
			gbc_textField.gridy = 2;
			pan.add(jTextField, gbc_textField);
			jTextField.setEditable(false);
			jTextField.setColumns(2);
		}

		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets(0, 0, 0, 5);
		gbc_textField.gridx = x2++;
		gbc_textField.gridy = 2;
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		pan.add(txtTotTotal, gbc_textField);
		txtTotTotal.setEditable(false);
		txtTotTotal.setColumns(2);

		lblRowTotal = new JLabel("Total");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 9;
		gbc_lblNewLabel.gridy = 0;
		pan.add(lblRowTotal, gbc_lblNewLabel);

		lblColumnTotal = new JLabel("Total  ");
		GridBagConstraints gbg = new GridBagConstraints();
		gbg.anchor = GridBagConstraints.EAST;
		gbg.insets = new Insets(0, 0, 0, 5);
		gbg.gridx = 1;
		gbg.gridy = 2;
		pan.add(lblColumnTotal, gbg);
	}

	/**
	 * Add to hiddenRows with the bare amount of data so the totals will still
	 * show the correct amount
	 * 
	 * @param connection
	 * @param db
	 * @param ts
	 * @param idi
	 * @throws SQLException
	 */
	private void addHiddenToTotals(Connection connection, DbEngine db, DTOProjectTimeSheet ts, int idi)
			throws SQLException {
		List<JFormattedTextFieldWithNotes> txtRowDay = new ArrayList<>();
		HashMap<DTOProjectTimeSheet, List<DTOTime>> loggedTimeByResource = db.getLoggedTimeByResource(connection,
				Application.resource, getDateTime(), idi);
		for (Entry<DTOProjectTimeSheet, List<DTOTime>> entry : loggedTimeByResource.entrySet()) {
			List<DTOTime> value = entry.getValue();
			for (DTOTime dtoTime : value) {
				JFormattedTextFieldWithNotes txt = new JFormattedTextFieldWithNotes();
				txt.setText(Utils.doubleValueOf(dtoTime.getLogged()));
				txtRowDay.add(txt);
			}
		}
		hiddenRows.add(new Row(null, txtRowDay, ts, null, null));
	}

	private void jbInit() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 10, 25, 25, 25, 25, 25, 25, 25, 25, 25, 50, 20 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 4.9E-324, 1.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, 0.0 };
		pan.setLayout(gridBagLayout);

		lblDay1 = new JLabel();
		lblDay1.setText(".");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 2;
		gbc_lblNewLabel.gridy = 0;
		pan.add(lblDay1, gbc_lblNewLabel);

		lblDay2 = new JLabel(".");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 3;
		gbc_lblNewLabel_1.gridy = 0;
		pan.add(lblDay2, gbc_lblNewLabel_1);

		lblDay3 = new JLabel(".");
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 4;
		gbc_lblNewLabel_2.gridy = 0;
		pan.add(lblDay3, gbc_lblNewLabel_2);

		lblDay4 = new JLabel(".");
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_3.gridx = 5;
		gbc_lblNewLabel_3.gridy = 0;
		pan.add(lblDay4, gbc_lblNewLabel_3);

		lblDay5 = new JLabel(".");
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4.gridx = 6;
		gbc_lblNewLabel_4.gridy = 0;
		pan.add(lblDay5, gbc_lblNewLabel_4);

		lblDay6 = new JLabel(".");
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5.gridx = 7;
		gbc_lblNewLabel_5.gridy = 0;
		pan.add(lblDay6, gbc_lblNewLabel_5);

		lblDay7 = new JLabel(".");
		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 8;
		gbc_lblNewLabel_6.gridy = 0;
		pan.add(lblDay7, gbc_lblNewLabel_6);

		txtTot1 = new JTextField();
		txtTot2 = new JTextField();
		txtTot3 = new JTextField();
		txtTot4 = new JTextField();
		txtTot5 = new JTextField();
		txtTot6 = new JTextField();
		txtTot7 = new JTextField();
		txtTotTotal = new JTextField(2);

		scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridwidth = 12;
		gbc_scrollPane.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		pan.add(scrollPane, gbc_scrollPane);

		timeview = new JPanel();
		scrollPane.setViewportView(timeview);

		GridBagLayout gbl_timeview = new GridBagLayout();
		gbl_timeview.columnWidths = new int[] { 0, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25 };
		gbl_timeview.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gbl_timeview.columnWeights = new double[] { 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
				Double.MIN_VALUE };
		gbl_timeview.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0 };

		timeview.setLayout(gbl_timeview);

		// HACK ALERT
		lblNewLabel = new JLabel("hidden. used as workaround :P");
		GridBagConstraints gbc_lblNewLabel2 = new GridBagConstraints();
		gbc_lblNewLabel2.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel2.gridx = 0;
		gbc_lblNewLabel2.gridy = 0;
		lblNewLabel.setVisible(false);
		timeview.add(lblNewLabel, gbc_lblNewLabel2);
		lblNewLabel = null;

		MainWindow.sendNotification("You can press F4 to add a note to logged time!");
	}

	private void calculateTotals() {
		if (!validateInput())
			return;

		double oldTotal = Double
				.valueOf(txtTotTotal.getText() != null && !txtTotTotal.equals("") ? "0.0" : txtTotTotal.getText());

		List<Row> rows2 = new ArrayList<Row>(getRows());
		// Add all hidden rows so they are counted
		rows2.addAll(hiddenRows);
		if (rows2.isEmpty())
			return;
		double time[][] = new double[rows2.size()][7];
		for (int i = 0; i < rows2.size(); i++) {
			Row row = rows2.get(i);
			for (int j = 0; j < row.getTxtRowDay().size(); j++) {
				JTextField jTextField = row.getTxtRowDay().get(j);
				double timeLogged = Double.valueOf(jTextField.getText());
				time[i][j] = timeLogged;
			}
		}

		// gets the total for the end of the rows
		int it = 0;
		double totalRowTime[] = new double[rows2.size()];
		for (double[] x : time) {
			double tmp = 0.0;
			for (double y : x) {
				tmp += y;
			}
			totalRowTime[it++] = tmp;
		}
		for (int k = 0; k < txtEndOfRows.size(); k++) {
			JTextField jTextField = txtEndOfRows.get(k);
			try {
				jTextField.setText(Utils.doubleValueOf(totalRowTime[k]));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// gets the column totals
		double totalTime[] = sumColumns(time);

		// sets totals at bottom
		JTextField[] totalBoxes = getTotalBoxes();
		for (int j = 0; j < totalBoxes.length; j++) {
			double d = totalTime[j];
			if (d < Application.HOURS_IN_DAY) {
				totalBoxes[j].setBackground(Color.RED);
			} else if (d == Application.HOURS_IN_DAY) {
				totalBoxes[j].setBackground(Color.YELLOW);
			} else if (d > Application.HOURS_IN_DAY) {
				totalBoxes[j].setBackground(Color.GREEN);
			}
			totalBoxes[j].setText(Utils.doubleValueOf(d));
			if (d > 24.0) {
				DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM");
				MainWindow.sendErrorNotification(
						"More than 24 hours logged in a day W/C " + fmt.print(dateTime) + ". Please check your input!");
				totalBoxes[j].setBackground(Color.MAGENTA);
			}
		}

		// Set colour the bottom right total
		double tmp = 0.0;
		for (double dd : totalTime) {
			tmp += dd;
		}
		txtTotTotal.setText(Utils.doubleValueOf(tmp));
		if (tmp < Application.HOURS_IN_WEEK) {
			txtTotTotal.setBackground(Color.RED);
		} else if (tmp == Application.HOURS_IN_WEEK) {
			txtTotTotal.setBackground(Color.YELLOW);
		} else if (tmp > Application.HOURS_IN_WEEK) {
			txtTotTotal.setBackground(Color.GREEN);
		}

		double newTotal = Double.valueOf(txtTotTotal.getText());
		if (newTotal != oldTotal) {
			MainWindow.madeChanges = true;
		}
	}

	private ActionListener hideBtnActionListener(DTOProjectTimeSheet dtoProjectTimeSheet) {
		return e -> {
			String property = Props.getProperty(HIDE_PROPERTY);
			if (property != null && property.contains(",")) {
				String[] split = property.split(",");
				List<String> asList = new ArrayList<>(Arrays.asList(split));
				asList.add(String.valueOf(dtoProjectTimeSheet.getProjectId()));
				Props.setProperty(HIDE_PROPERTY, String.join(",", asList));
			} else {
				Props.setProperty(HIDE_PROPERTY, String.valueOf(dtoProjectTimeSheet.getProjectId()) + ",");
			}
			try {
				repopulateTextFields();
			} catch (SQLException | RDNE e1) {
				e1.printStackTrace();
			}
		};
	}

	private KeyAdapter getKeyListener() {
		return new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				// java.awt.event.KeyEvent[KEY_RELEASED,keyCode=115,keyText=F4,keyChar=Undefined
				// keyChar,keyLocation=KEY_LOCATION_STANDARD,rawCode=115,primaryLevelUnicode=0,scancode=62,extendedKeyCode=0x73]
				// on ATextField [notes=null]
				if (e.getKeyCode() == 115) {
					// F4

					JFormattedTextFieldWithNotes txt = (JFormattedTextFieldWithNotes) e.getComponent();
					TextNotesFrame txtFrame = new TextNotesFrame(txt);
					txtFrame.pack();
					txtFrame.setVisible(true);
				}
			}
		};
	}

	private FocusAdapter getFocusListener() {
		return new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				calculateTotals();
			}
		};
	}
}