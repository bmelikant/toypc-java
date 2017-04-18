package ToyVM;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MachineChooser extends JFrame implements ActionListener, ListSelectionListener {

	private static final long serialVersionUID = 1L;
	private JList<String> machineList;
	private JButton startMachineButton, settingsButton, deleteButton;
	private JMenuBar topMenuBar;
	private JMenu fileMenu, diskMenu;
	private JMenuItem settingsItem, startItem, newItem, deleteItem, addDiskItem, delDiskItem, formatDiskItem;
	
	private GridBagLayout winLayout;
	private GridBagConstraints gbc;
	
	public MachineChooser () {
		
		initTopMenu ();
		initGui ();
		
		// set up the list of virtual machines from the configuration file
		File settingsFile = new File ("chooser_settings.dat");
		
		if (!settingsFile.exists()) {
			
			System.out.println ("No settings file found!!!");
			System.exit (-1);
		}
		
		else 
			updateMachineList (new File ("chooser_settings.dat"));
		
		// make sure the buttons are disabled if no item is selected
		if (machineList.getSelectedValue() == null) {
			
			startMachineButton.setEnabled (false);
			settingsButton.setEnabled (false);
			deleteButton.setEnabled (false);
			
			settingsItem.setEnabled(false);
			startItem.setEnabled (false);
			deleteItem.setEnabled (false);
			diskMenu.setEnabled (false);
		}
		// now show the JFrame
		pack ();
		setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
		setTitle ("Choose a Virtual Machine");
		setVisible (true);
	}
	
	public void initTopMenu () {
		
		// make the JMenuBar
		topMenuBar = new JMenuBar ();
		
		// make the JMenus
		fileMenu = new JMenu ("File");
		diskMenu = new JMenu ("Disk");
		settingsItem = new JMenuItem ("Settings");
		
		// make the JMenuItems
		startItem = new JMenuItem ("Start Instance");
		newItem = new JMenuItem ("New Virtual Machine");
		deleteItem = new JMenuItem ("Delete Instance");
		addDiskItem = new JMenuItem ("Add Disk Controller");
		delDiskItem = new JMenuItem ("Delete Disk Controller");
		formatDiskItem = new JMenuItem ("Format Virtual Disk");
		
		// add the menu items to the menus
		topMenuBar.add(fileMenu);
		topMenuBar.add(diskMenu);
		topMenuBar.add(settingsItem);
		
		fileMenu.add (startItem);
		fileMenu.add (newItem);
		fileMenu.add (deleteItem);
		
		diskMenu.add (addDiskItem);
		diskMenu.add (delDiskItem);
		diskMenu.add (formatDiskItem);
		
		setJMenuBar (topMenuBar);
	}
	
	public void initGui () {
	
		winLayout = new GridBagLayout ();
		gbc = new GridBagConstraints ();
		setLayout (winLayout);
		
		machineList = new JList<String> ();
		machineList.setPreferredSize(new Dimension (200, 100));
		//machineList.setBackground(Color.WHITE);
		//machineList.setFont(new Font ("Courier New", Font.PLAIN, 12));
		
		startMachineButton = new JButton ("Start Machine");
		settingsButton = new JButton ("Machine Settings");
		deleteButton = new JButton ("Delete Machine");
		
		gbc.insets = new Insets (5, 5, 5, 5);
		addComponent (new JLabel ("Available Virtual Machines:"), 0, 0, 1, 1);
		
		gbc.fill = GridBagConstraints.BOTH;
		addComponent (new JScrollPane (machineList), 0, 1, 1, 4);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		addComponent (startMachineButton, 1, 1, 1, 1);
		addComponent (settingsButton, 1, 2, 1, 1);
		addComponent (deleteButton, 1, 3, 1, 1);
		
		machineList.addListSelectionListener (this);
		startMachineButton.addActionListener (this);
		settingsButton.addActionListener (this);
		deleteButton.addActionListener (this);
		
		// add this action listener to the list items
		newItem.addActionListener (this);
		startItem.addActionListener (this);
		settingsItem.addActionListener (this);
		addDiskItem.addActionListener(this);
	}
	
	public void addComponent (Component c, int x, int y, int w, int h) {
	
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = w;
		gbc.gridheight = h;
		
		winLayout.setConstraints(c, gbc);
		add (c);
	}
	
	public void updateMachineList (File settingsFile) {
		
		try {
			
			BufferedReader settingsIn = new BufferedReader (new InputStreamReader (new FileInputStream (settingsFile)));
			ArrayList<String> listModel = new ArrayList<String>();
			
			while (settingsIn.ready())
				listModel.add(settingsIn.readLine());
			
			settingsIn.close ();
			
			// copy the ArrayList to a String array
			String [] listData = new String [listModel.size()];
			for (int i = 0; i < listData.length; i++) 
				listData[i] = listModel.get(i);
			
			machineList.setListData(listData);
			
		} catch (IOException e2) {
			
			System.out.println ("Error loading machine list: " + e2.toString()); 
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		
		// are we adding a new Instance?
		if (e.getSource () == newItem) {
			
			NewMachineDialog test = new NewMachineDialog (this);
			int result = test.showDialog();
			
			if (result == NewMachineDialog.ACCEPT_OPTION) {
				
				File settingsFile = new File ("chooser_settings.dat");
				
				try {
				
					BufferedWriter outFile = new BufferedWriter (new FileWriter (settingsFile, true));
					outFile.write(test.getMachineName() + "\n");
					outFile.flush ();
					outFile.close ();
					
					updateMachineList (settingsFile);
					
				} catch (IOException e1) {
					
					JOptionPane.showMessageDialog(this, "Error writing chooser settings: " + e1.toString());
				}			
			}
		}
		
		// are we starting the machine? If so, pass the machine and it's settings file to a new instance
		if ((e.getSource () == startMachineButton || e.getSource () == startItem) && machineList.getSelectedValue () != null)
			new MachineInterface (machineList.getSelectedValue(), new File (machineList.getSelectedValue()+"\\settings.dat"));
		
		if ((e.getSource () == settingsButton || e.getSource () == settingsItem) && machineList.getSelectedValue () != null) {
			
			SettingsDialog dlg = new SettingsDialog (MachineSettings.loadSettings(new File (machineList.getSelectedValue()+"\\settings.dat")));
			int result = dlg.showDialog();
			
			if (result == SettingsDialog.ACCEPT_OPTION)
				MachineSettings.storeSettings(dlg.getSettings(), new File (machineList.getSelectedValue()+"\\settings.dat"));
		}
		
		if ((e.getSource () == deleteButton || e.getSource () == deleteItem) && machineList.getSelectedValue() != null) {
			
			// delete the name from the list and delete the associated settings file/folder
			try {
				
				BufferedWriter tempOut = new BufferedWriter (new FileWriter ("temporary"));
				BufferedReader tempIn = new BufferedReader (new FileReader ("chooser_settings.dat"));
				
				while (tempIn.ready ()) {
					
					String out = tempIn.readLine();
					if (!out.equals (machineList.getSelectedValue()))
						tempOut.write(out + "\n");
				}
				
				// close the in and out files
				tempIn.close();
				tempOut.close();
				
				// now create new file objects to remove and rename
				File oldData = new File ("chooser_settings.dat");
				File newData = new File ("temporary");
				
				oldData.delete();
				newData.renameTo(oldData);

				// now delete the working directory for the machine
				File directory = new File (machineList.getSelectedValue ());
				File [] subfiles = directory.listFiles ();
				
				for (int i = 0; i < subfiles.length; i++)
					subfiles[i].delete();
				
				directory.delete();
				updateMachineList (oldData);
	
			} catch (IOException e1) {
				
				JOptionPane.showMessageDialog(this, "Error writing settings file: " + e.toString ());
			}
		}
		
		if (e.getSource () == addDiskItem && machineList.getSelectedValue() != null) {
			
			// read the settings file and make sure that one of the disk controllers is free
			// if so, use the primary controller first and the secondary controller second... duh...
			MachineSettings tempSettings = MachineSettings.loadSettings(new File (machineList.getSelectedValue () + "\\settings.dat"));
			
			if (tempSettings.diskControllerOnePath != null && tempSettings.diskControllerTwoPath != null) {
				
				JOptionPane.showMessageDialog (this, "Sorry, no available controller was found on this machine!");
				return;
			}
			
			// make a new JFileChooser with a JPanel accessory housing a JSpinner and label
			// for said Spinner that allows us to select the size in MB of the disk (up to 10240)
			JSpinner diskSzSpinner = new JSpinner ();
			JPanel accessoryPanel = new JPanel ();
		    diskSzSpinner.setPreferredSize(new Dimension (75, 25));
		    diskSzSpinner.setModel(new SpinnerNumberModel (1024, 1, 4096, 1));
		    
			accessoryPanel.add (new JLabel ("Disk Size (MB):"));
			accessoryPanel.add (diskSzSpinner);
			
			JFileChooser dskFileChooser = new JFileChooser ();
			dskFileChooser.setAccessory(accessoryPanel);
			dskFileChooser.setCurrentDirectory(new File (System.getProperty("user.dir")+"\\"+machineList.getSelectedValue()));
			int result = dskFileChooser.showSaveDialog(this);
			
			if (result == JFileChooser.APPROVE_OPTION) {
				
				// make a new virtual disk of dskSzSpinner mbytes
				long byteCount = ((((Integer) diskSzSpinner.getValue ())*1024)*1024);
				
				ByteBuffer outBuf = ByteBuffer.allocate(1024*2);
				ShortBuffer dataBuf = outBuf.asShortBuffer();
				
				for (int i = 0; i < 1024; i++)
					dataBuf.put((short) 0x00);
				
				try {
					
					RandomAccessFile disk = new RandomAccessFile (dskFileChooser.getSelectedFile(), "rw");
					FileChannel diskFile = disk.getChannel();
					
					for (long i = 0; i < byteCount; i += 1024) {
					
						diskFile.write(outBuf);
						outBuf.rewind();
					}
					
					diskFile.close ();
					disk.close ();
					
				} catch (IOException e1) {
					
					System.out.println ("Error writing out file: " + e1.toString ());
					e1.printStackTrace();
				}
				
				if (tempSettings.diskControllerOnePath == null)
					tempSettings.diskControllerOnePath = dskFileChooser.getSelectedFile();
				else if (tempSettings.diskControllerTwoPath == null)
					tempSettings.diskControllerTwoPath = dskFileChooser.getSelectedFile();
				else
					JOptionPane.showMessageDialog (this, "No free controller found???");
				
				MachineSettings.storeSettings(tempSettings, new File(machineList.getSelectedValue()+"\\settings.dat"));
			}
		}
		
		if (e.getSource () == delDiskItem) {
		}
	}	
	
	public static void main (String [] args) {
		
		new MachineChooser ();
	}

	public void valueChanged(ListSelectionEvent e) {
		
		// here we want to enable the buttons if a machine gets selected
		if (machineList.getSelectedValue() != null) {
			
			startMachineButton.setEnabled(true);
			settingsButton.setEnabled(true);
			deleteButton.setEnabled(true);
			
			settingsItem.setEnabled (true);
			startItem.setEnabled (true);
			deleteItem.setEnabled (true);
			diskMenu.setEnabled (true);
		}
	}
}

class NewMachineDialog extends JDialog implements ActionListener, KeyListener {

	private static final long serialVersionUID = 1L;

	public static final int ACCEPT_OPTION = 1, CANCEL_OPTION = 2;
	
	// member variables
	private JTextField nameField;
	private JTextField biosFileField;
	private JTextField directoryField;
	private JSpinner memoryKb;
	
	private JButton okButton;
	private JButton cancelButton;
	private JButton browseButton;
	
	private GridBagLayout layout;
	private GridBagConstraints gbc;
	
	private int returnOption;
	private MachineSettings built;
	
	public NewMachineDialog (JFrame parent) {
	
		// call the superclass instantiation method
		super (parent, true);
		built = new MachineSettings ();
		
		constructDialog ();
		pack ();
		setTitle ("New Virtual Machine");
	}
	
	public void constructDialog () {
		
		layout = new GridBagLayout ();
		gbc = new GridBagConstraints ();
		setLayout (layout);
		
		gbc.insets = new Insets (5, 5, 5, 5);
		
		nameField = new JTextField (30);
		biosFileField = new JTextField (30);
		directoryField = new JTextField (30);
		memoryKb = new JSpinner ();
		memoryKb.setPreferredSize(new Dimension (50, 25));
		
		okButton = new JButton ("Create");
		cancelButton = new JButton ("Cancel");
		browseButton = new JButton ("Browse");
		
		addComponent (new JLabel ("Machine Name:"), 0, 0, 1, 1);
		addComponent (new JLabel ("BIOS File:"), 0, 1, 1, 1);
		addComponent (new JLabel ("Directory:"), 0, 2, 1, 1);
		addComponent (new JLabel ("Memory (KB):"), 0, 3, 1, 1);
		
		addComponent (nameField, 1, 0, 3, 1);
		addComponent (biosFileField, 1, 1, 3, 1);
		addComponent (directoryField, 1, 2, 3, 1);

		addComponent (okButton, 3, 4, 1, 1);
		addComponent (cancelButton, 4, 4, 1, 1);
		addComponent (browseButton, 4, 1, 1, 1);
		
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		addComponent (memoryKb, 1, 3, 2, 1);
		
		Integer [] intList = new Integer [64];
		
		for (int i = 0; i < intList.length; i++)
			intList[i] = i+1;
		
		SpinnerListModel intModel = new SpinnerListModel (intList);
		memoryKb.setModel(intModel);
		
		nameField.addKeyListener(this);
		directoryField.setEditable (false);
		
		okButton.addActionListener(this);
		cancelButton.addActionListener (this);
		browseButton.addActionListener (this);
	}
	
	public int showDialog () {
	
		setVisible (true);
		return returnOption;
	}
	
	private void addComponent (Component c, int x, int y, int w, int h) {
		
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = w;
		gbc.gridheight = h;
		
		layout.setConstraints (c, gbc);
		add (c);
	}
	
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource () == okButton) {
			
			// try to build an entry. If the entry is invalid
			// do not end the dialog!!
			if (nameField.getText().equals ("")) {
				JOptionPane.showMessageDialog(this, "You must specify the machine's name!!!");
				return;
			}
			
			else if (biosFileField.getText().equals ("")) {
				JOptionPane.showMessageDialog (this, "You must specify a BIOS file!!!");
				return;
			}

			built.machineName = nameField.getText ();
			built.biosFilePath = new File (biosFileField.getText ());
			built.memorySize = (Integer) memoryKb.getValue ();
			built.diskControllerOnePath = null;
			built.diskControllerTwoPath = null;
			
			// try to make the directory for the disk. If the directory exists
			// then we have to alert the user that the machine exists
			File machineDir = new File (built.machineName);
			
			if (machineDir.exists()) {
				JOptionPane.showMessageDialog (this, "Sorry, that machine exists!!!");
				return;
			}
			
			if (machineDir.mkdir() == false) {
				JOptionPane.showMessageDialog (this, "Sorry, the directory could not be created!!!");
				return;
			}
			
			// store the settings to the new directory
			File settingsFile = new File (machineDir.getAbsolutePath()+"\\settings.dat");
			MachineSettings.storeSettings(built, settingsFile);
			returnOption = ACCEPT_OPTION;
			setVisible (false);
		}
		
		else if (e.getSource () == cancelButton) {
			
			returnOption = CANCEL_OPTION;
			setVisible (false);
		}
		
		else if (e.getSource () == browseButton) {
			
			JFileChooser fileChooser = new JFileChooser ();
			int result = fileChooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				
				biosFileField.setText (fileChooser.getSelectedFile().getAbsolutePath());
			}
		}
	}

	public void keyPressed(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}
	
	public void keyReleased(KeyEvent e) { 
		
		directoryField.setText(System.getProperty ("user.dir") + "\\" + nameField.getText() + "\\"); 
	}
	
	public String getMachineName () { return built.machineName; }
}

//class SettingsDialog: Get settings for the machine
class SettingsDialog extends JDialog implements ActionListener, ChangeListener {
	
	private static final long serialVersionUID = 1L;
	// constants
	public static final int ACCEPT_OPTION = 0;
	public static final int CANCEL_OPTION = 1;
	
	// member variables
	MachineSettings toReturn;
	int selection;
	
	// window controls
	JTextField biosFileField;
	JLabel biosFileFieldLabel;
	JButton biosBrowseButton;
	
	JSpinner memorySzSpinner;
	
	JButton acceptButton;
	JButton cancelButton;
	
	GridBagLayout dlgLayout;
	GridBagConstraints dc;
	
	// default constructor for making new settings file
	public SettingsDialog () {
		
		toReturn = new MachineSettings ();
		constructDialog();
	}
	
	// overloaded constructor to use existing settings
	public SettingsDialog (MachineSettings toUse) {
		
		toReturn = toUse;
		constructDialog();
		updateFields ();
	}
	
	// construct this dialog
	public void constructDialog () {
		
		// set up the layout for the dialog
		dlgLayout = new GridBagLayout ();
		dc = new GridBagConstraints ();
		setLayout (dlgLayout);
		selection = CANCEL_OPTION;
		
		// create the controls
		biosFileFieldLabel      = new JLabel ("BIOS File:");
		biosFileField           = new JTextField (20);
		biosBrowseButton        = new JButton ("Browse...");
		
		memorySzSpinner = new JSpinner ();		
		memorySzSpinner.setModel(new SpinnerNumberModel (1, 1, 64, 1));
		acceptButton = new JButton ("Accept");
		cancelButton = new JButton ("Cancel");
		
		// disable the text boxes; they should not be editable!
		biosFileField.setEditable(false);

		// add the controls to the Dialog
		dc.insets = new Insets (15, 5, 15, 5);
		dc.fill = GridBagConstraints.BOTH;
		addComponent (biosFileFieldLabel, 0, 0, 1, 1);

		dc.fill = GridBagConstraints.HORIZONTAL;
		addComponent (biosFileField, 1, 0, 2, 1);
		addComponent (biosBrowseButton, 3, 0, 1, 1);
		
		addComponent (new JLabel ("Memory Size (KB):"), 0, 1, 1, 1);
		addComponent (memorySzSpinner, 1, 1, 1, 1);
		
		addComponent (acceptButton, 1, 4, 1, 1);
		addComponent (cancelButton, 2, 4, 1, 1);
		
		biosBrowseButton.addActionListener (this);
		
		acceptButton.addActionListener (this);
		cancelButton.addActionListener (this);
		memorySzSpinner.addChangeListener(this);
		memorySzSpinner.setValue(toReturn.memorySize);
		
		// set the dialog properties
		pack ();
		setTitle ("Machine Settings");
		setModal (true);
		setDefaultCloseOperation (DISPOSE_ON_CLOSE);
	}
	
	// addComponent (): add a component to the layout for this window
	public void addComponent (Component c, int x, int y, int w, int h) {
		
		dc.gridx = x;
		dc.gridy = y;
		dc.gridwidth = w;
		dc.gridheight = h;
		
		dlgLayout.setConstraints (c, dc);
		add (c);
	}
	
	// showDialog (): show this dialog
	public int showDialog () {
		
		setVisible (true);
		return selection;
	}
	
	// actionPerformed(): capture action events
	public void actionPerformed (ActionEvent e) {
		
		// did we accept the dialog?
		if (e.getSource () == acceptButton) {
			
			selection = ACCEPT_OPTION;
			dispose ();
		}
		
		// did we cancel the dialog?
		else if (e.getSource () == cancelButton) {
			
			selection = CANCEL_OPTION;
			dispose ();
		}
		
		else if (e.getSource () == biosBrowseButton) {
			
			JFileChooser biosFileChooser = new JFileChooser ();
			FileNameExtensionFilter biosExtensionFilter = new FileNameExtensionFilter ("Toy ASM Exes", "tei");
			biosFileChooser.setFileFilter(biosExtensionFilter);
			int result = biosFileChooser.showOpenDialog(this);
			
			if (result == JFileChooser.APPROVE_OPTION)
				toReturn.setBiosFile(biosFileChooser.getSelectedFile());
			else
				return;
		}
		
		updateFields ();
	}
	
	// public void updateFields (): Update the text fields for file selection
	public void updateFields () {
		
		if (toReturn.getBiosFile() == null)
			biosFileField.setText("None");
		else
			biosFileField.setText(toReturn.getBiosFile().getAbsolutePath());
		
		toReturn.memorySize = (Integer) memorySzSpinner.getValue();
	}
	
	// public MachineSettings getSettings (): Get the settings object from this dialog
	public MachineSettings getSettings () { return toReturn; }

	public void stateChanged(ChangeEvent e) {
		
		updateFields ();
	}
}

//class MachineSettings: Hold a list of machine settings
class MachineSettings implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// member variables
	int memorySize;
	
	String machineName;
	File biosFilePath;
	File diskControllerOnePath;
	File diskControllerTwoPath;
	
	// public void setBiosFile (File biosFile): Set the BIOS file image for this settings log
	public void setBiosFile (File biosFile) { biosFilePath = biosFile; }
	// public void setDiskControllerOnePath (File diskController): Set the file image for the first disk
	public void setDiskControllerOnePath (File diskController) { diskControllerOnePath = diskController; }
	// public void setDiskControllerTwoPath (File diskController): Set the file image for the second disk
	public void setDiskControllerTwoPath (File diskController) { diskControllerTwoPath = diskController; }
	
	// public File getBiosFile (): get the path to the BIOS file
	public File getBiosFile () { return biosFilePath; }
	// public File getDiskControllerOnePath (): get the path to the first disk controller
	public File getDiskControllerOnePath () { return diskControllerOnePath; }
	// public File getDiskControllerTwoPath (): get the path to the second disk controller
	public File getDiskControllerTwoPath () { return diskControllerTwoPath; }
	
	// public static MachineSettings loadSettings (File toLoad): load settings from a file
	public static MachineSettings loadSettings (File toLoad) {
		
		try {
			
			ObjectInputStream settingsIn = new ObjectInputStream (new FileInputStream (toLoad));
			MachineSettings toReturn = (MachineSettings) settingsIn.readObject ();
			settingsIn.close();
			
			return toReturn;
			
		} catch (Exception e) {
			
			JOptionPane.showMessageDialog (null, "Error opening settings file:\n" + e.toString());
		}
		
		return null;
	}
	
	// public static void storeSettings (MachineSettings toStore, File dest): store the settings to a file
	public static void storeSettings (MachineSettings toStore, File dest) {
		
		try {
			
			ObjectOutputStream settingsOut = new ObjectOutputStream (new FileOutputStream (dest));
			settingsOut.writeObject (toStore);
			settingsOut.flush();
			settingsOut.close();
			
		} catch (Exception e) {
			
			JOptionPane.showMessageDialog (null, "Error opening settings file:\n" + e.toString());
		}
	}
}