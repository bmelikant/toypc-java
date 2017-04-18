package ToyAsmIDE;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class MachineInterface extends JFrame implements ActionListener, Runnable {
	
	private static final long serialVersionUID = 1L;
	
	// display mode constants
	private int [] rows = { 20, 25, 40  };
	private int [] cols = { 40, 80, 100 };
	
	// member variables
	private JTextArea   machineConsole;
	private JButton     startButton;
	private JMenuBar    topMenu;
	private JMenu       programList;
	private JMenuItem   viewRegisters, viewMemory, setStartIP;
	
	// primitive variables
	private boolean threadLives;
	private int currentVMode;
	
	// machine control thread
	private Thread machineThread;
	
	// ToyProcessor instance
	private ToyProcessor mainProcessor;
	private KeyboardController keyCtrl;
	private DiskController diskCtrl;
	private SystemClock sysClock;
	
	GridBagLayout winLayout;
	GridBagConstraints gbc;
	
	// public MachineInterface () constructor method
	public MachineInterface () {
	
		// initialize the layout
		winLayout = new GridBagLayout ();
		gbc       = new GridBagConstraints ();
		
		this.setLayout (winLayout);
	
		// initialize our thread to dead and our processor to 64k of memory
		threadLives = false;
		currentVMode = 0x01;
		
		mainProcessor = new ToyProcessor (32*1024, false);
		mainProcessor.storeMemory ((short) currentVMode, 0x2000);
	
		// set up the keyboard controller with default mappings
		keyCtrl = new KeyboardController (mainProcessor, (short) 0xF0);
		sysClock = new SystemClock (mainProcessor, 0x0FA);
		
		// make a new disk controller. Delete the disk controller file on exit!
		// this disk controller is a temporary controller of size 64 Kilobytes
		diskCtrl = new DiskController (mainProcessor, (short) 0x1F0);
		
		mainProcessor.reservePortRange(keyCtrl, (short) 0xf0, (short) 3);
		mainProcessor.reservePortRange(diskCtrl, (short) 0x1F0, (short) 3);
		mainProcessor.reservePortRange(sysClock, (short) 0xfa, (short) 2);
		
		// initialize the controls
		machineConsole = new JTextArea   ();
		startButton    = new JButton     ("Start Machine");
		
		// add the keyboard controller as our key listener
		machineConsole.addKeyListener(keyCtrl);		
		machineConsole.grabFocus();
		
		// set up the menus
		topMenu         = new JMenuBar ();
		programList     = new JMenu ("Program");
		viewRegisters   = new JMenuItem ("View Registers");
		viewMemory      = new JMenuItem ("View Memory");
		setStartIP      = new JMenuItem ("Set Start IP");
		
		// add the options to the optionList
		programList.add (viewRegisters);
		programList.add (viewMemory);
		programList.add (setStartIP);
		
		// add the lists to the topMenu
		topMenu.add (programList);
		
		// set up the menu for this window
		this.setJMenuBar (topMenu);
		
		// add the controls to the window
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 3.0;
		addComponent (machineConsole, 1, 1, 4, 3);
		
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.NONE;
		addComponent (startButton, 1, 4, 1, 1);
		
		// set up actionListeners
		startButton.addActionListener (this);
		viewRegisters.addActionListener (this);
		viewMemory.addActionListener (this);
		setStartIP.addActionListener (this);
		
		// set up font for console window
		machineConsole.setEditable (false);
		machineConsole.setFont (new Font ("Courier New", Font.PLAIN, 12));
		machineConsole.setRows (25);
		machineConsole.setColumns (80);
		machineConsole.setBackground(Color.BLACK);
		machineConsole.setForeground(Color.WHITE);
		
		// set up video memory
		for (int i = 0; i < 2000; i++)		
			mainProcessor.storeMemory((short) ' ', i+0x1000); 
	
		// open the window and set everything up
		pack ();
		setResizable (false);
		setDefaultCloseOperation (DISPOSE_ON_CLOSE);
		setTitle("Toy Virtual Machine");
	}
	
	// open this window (debug assembler version!)
	public void displayMachine () { setVisible (true); }
	
	// add a control to the window using the GridBagConstraints
	public void addComponent (Component c, int x, int y, int width, int height) {
		
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth  = width;
		gbc.gridheight = height;
		
		winLayout.setConstraints (c, gbc);
		this.add (c);
	}

	// public void updateDisplay (): Read the display memory and place it's contents
	// in the display area
	public void updateDisplay () {
		
		String displayUpdate = "";
		short [] displayRegion = mainProcessor.getMemoryRegion (0x1000, 0x1000+(rows[currentVMode]*cols[currentVMode]));
		
		for (int i = 0; i < rows[currentVMode]; i++) {
			
			for (int j = 0; j < cols[currentVMode]; j++)
				displayUpdate += (char) displayRegion[(i*cols[currentVMode])+j];
			
			displayUpdate += '\n';
		}
		
		machineConsole.setText (displayUpdate);
		
		// check for a mode change
		int newMode = mainProcessor.getMemory (0x2000);
		
		if (newMode != currentVMode && newMode < rows.length) {
			
			currentVMode = newMode;
			machineConsole.setRows(rows[currentVMode]);
			machineConsole.setColumns(cols[currentVMode]);
			machineConsole.setText ("");
			pack ();
		}
	}
	
	// run method for thread
	public void run () {
		
		// declare an update counter for the display
		// int updateCounter = 0;
		int updateCounter = 0;
		
		while (threadLives) {
			
			// fetch and process the opcode. On processOpcode = true
			// we have received the stop signal
			sysClock.updateTsc ();
			mainProcessor.fetchOpcode ();
			
			if (mainProcessor.processOpcode() == false)
				threadLives = false;
			
			if (updateCounter <= 0) {
				
				updateDisplay ();
				sysClock.update ();
				keyCtrl.update ();
				
				updateCounter = 10;
			}
			
			else
				updateCounter--;
		}
		
		updateDisplay ();
		sysClock.update ();
		keyCtrl.update ();
		
		System.out.println ("Processor finished execution.");
		startButton.setText ("Start Machine");
	}
	
	// method to load a program from a file (for debug running)
	// this method is the only way to load code in the version included in the IDE!!!
	public void loadCodeIntoMachine (String toLoad, int origin, boolean debugMode) {
		
		try {
			
			DataInputStream fileInput = new DataInputStream (new FileInputStream (toLoad));
                        
			short codeByte = fileInput.readByte ();
                        
                        if (codeByte < 0) {
                            
                            codeByte = (short) Math.abs (codeByte);
                            codeByte |= 0x80;
                        }
                        
			int i = origin;
			
			try {
				
				while (codeByte >= 0) {
				
					mainProcessor.storeMemory (codeByte, i++);
                                        
					codeByte = fileInput.readByte ();
                                        
                                        if (codeByte < 0) {
                                            
                                            codeByte = (short) Math.abs (codeByte);
                                            codeByte |= 0x80;
                                        }
				}
				
			} catch (EOFException e) {
				
				System.out.println ("Finished loading file!");
			}
			
			fileInput.close ();
			mainProcessor.setRegister (origin, ToyProcessor.constRegIP);
			
			if (debugMode)
				mainProcessor.turnDebugModeOn ();
			
			machineConsole.grabFocus();
			
		} catch (Exception exc) {
			
			JOptionPane.showMessageDialog(this, "File I/O Error: " + exc.toString ());
		}
	}
	
	// action listener for this window
	public void actionPerformed (ActionEvent e) {
		
		// if the start button was pressed control the machine's running state
		if (e.getSource () == this.startButton) {
			
			// if the code is running, stop it
			if (threadLives && machineThread != null) {
				
				try {
					
					// stop the machine thread and wait for it to finish
					threadLives = false;
					machineThread.join();
					
					// set the button text to "Start Machine"
					startButton.setText ("Start Machine");
					
				} catch (InterruptedException exc) {
					
					System.out.println ("Interrupted while waiting for machine to finish running!");
					exc.printStackTrace ();
				}
			}
			
			// if the code is NOT running, start a new instance
			else if (threadLives == false) {
				
				machineThread = new Thread (this);
				machineThread.start ();
				
				startButton.setText ("Stop Machine");
				threadLives = true;
			}
		}
		
		// if we are supposed to load the test program load some test code
		// into memory and adjust IP
		// ITEM STRIPPED FOR ASSEMBLER FINAL REVISION!!!
		
		// if we are showing the registers build a register dump string and
		// make a message box
		else if (e.getSource () == this.viewRegisters) {
		
			// build a register string
			String regString = "";
			
			regString += "Register A: "  + String.format ("%04x", mainProcessor.getRegister (ToyProcessor.constRegA))  + "     ";			
			regString += "Register B: "  + String.format ("%04x", mainProcessor.getRegister (ToyProcessor.constRegB))  + "\n";
			regString += "Register C: "  + String.format ("%04x", mainProcessor.getRegister (ToyProcessor.constRegC))  + "     ";
			regString += "Register X: "  + String.format ("%04x", mainProcessor.getRegister (ToyProcessor.constRegX))  + "\n";
			regString += "Register Y: "  + String.format ("%04x", mainProcessor.getRegister (ToyProcessor.constRegY))  + "     ";
			regString += "Register IP: " + String.format ("%04x", mainProcessor.getRegister (ToyProcessor.constRegIP)) + "\n";
			regString += "Register S: "  + String.format ("%04x", mainProcessor.getRegister (ToyProcessor.constRegS))  + "     ";
			regString += "Register D: "  + String.format ("%04x", mainProcessor.getRegister (ToyProcessor.constRegD))  + "\n";
			
			JOptionPane.showMessageDialog (this, regString);
		}
		
		// if someone wants to view memory open a memory dialog
		else if (e.getSource () == viewMemory)
			new MemoryViewDialog (mainProcessor);
		
		// if we want to set the IP to start with, do it
		// ADDED FOR ASSEMBLER DEBUG VERSION!
		else if (e.getSource () == setStartIP) {
			
			String numString = JOptionPane.showInputDialog("Enter start IP:");
			int base = 10;
			
			if (numString == null)
				return;

			if (numString.endsWith ("h")) {
				
				base = 16;
				numString = numString.replace('h', ' ').trim ();
			}
			
			try {
				
				int ip = Integer.parseInt (numString, base);
				mainProcessor.setRegister(ip, ToyProcessor.constRegIP);
				
			} catch (NumberFormatException exc) {
				
				JOptionPane.showMessageDialog(null, "Bad number format: " + numString);
				return;
			}
		}
		
		// if we are supposed to change the look and feel do so
		// STRIPPED FOR ASSEMBLER REVISION!!!
		
		// load a code file
		// STRIPPED FOR ASSEMBLER REVISION!!!
		machineConsole.grabFocus();
	}
	
	// main method
	// STRIPPED FOR ASSEMBLER DEBUG REVISION!!!
}

// class MemoryViewDialog: Implement a dialog to view segments of memory
class MemoryViewDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JTextField startAddressField;
	private JTextField countField;
	private JLabel     startAddressFieldLabel;
	private JLabel     countFieldLabel;
	private JTextArea  outputArea;
	private JButton    displayContentButton;
	
	ToyProcessor refMachine;
	
	GridBagLayout dialogLayout;
	GridBagConstraints gbc;
	
	public MemoryViewDialog (ToyProcessor memoryReference) {
		
		refMachine = memoryReference;
		
		dialogLayout = new GridBagLayout ();
		gbc = new GridBagConstraints ();
		setLayout (dialogLayout);
		
		// create the controls
		startAddressField      = new JTextField ();
		countField             = new JTextField ();
		startAddressFieldLabel = new JLabel ("Start Address:");
		countFieldLabel        = new JLabel ("Count:");
		outputArea             = new JTextArea (10, 75);
		displayContentButton   = new JButton ("Get Memory");
		
		// set the font for the outputArea
		outputArea.setFont (new Font ("Courier New", Font.PLAIN, 12));
		outputArea.setEditable(false);
		
		// add the controls to the window
		gbc.insets = new Insets (15, 5, 15, 5);
		gbc.fill = GridBagConstraints.BOTH;
		addComponent (startAddressFieldLabel, 0, 0, 1, 1);
		addComponent (countFieldLabel, 0, 1, 1, 1);
		addComponent (startAddressField, 1, 0, 1, 1);
		addComponent (countField, 1, 1, 1, 1);
		addComponent (new JScrollPane (outputArea), 0, 3, 2, 1);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		addComponent (displayContentButton, 0, 2, 1, 1);
		displayContentButton.addActionListener(this);
		
		pack ();
		setTitle ("Memory Viewer");
		setModal (true);
		setVisible (true);
	}
	
	// addComponent (): add a component
	public void addComponent (Component c, int x, int y, int width, int height) {
		
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = width;
		gbc.gridheight = height;
		
		dialogLayout.setConstraints (c, gbc);
		add (c);
	}
	
	public void actionPerformed (ActionEvent e) {
		
		if (e.getSource () == displayContentButton) {
			
			// get the starting address and the count
			try {
				
				int startAddr = Integer.parseInt(startAddressField.getText());
				int count     = Integer.parseInt(countField.getText());
			
				// if count > 0x7fff print an error message
				if (count > 0x7fff || startAddr > 0x7fff) {
					
					JOptionPane.showMessageDialog (this, "Error: Field is out of acceptable range! Max is 0x7fff");
					return;
				}
				
				else if (startAddr + count > 0x7fff) {
					
					JOptionPane.showMessageDialog (this, "Error: Fields must combine to be less than 0x7fff!");
					return;
				}
				
				// build an output string using those values
				String taContent = "";
				int currentLine = 0;
				boolean lineStart = true;
				
				for (int i = 1; i < count+1; i++) {
					
					// is this the start of a line?
					if (lineStart) {
						
						taContent += String.format ("%04X:", (currentLine+startAddr)*16);
						lineStart = false;
					}
					
					// add the current memory location to the string
					taContent += " " + String.format("%02X", refMachine.getMemory(startAddr+(i-1)));
					
					// did we reach the end of a line?
					if (i % 16 == 0) {
						
						lineStart = true;
						taContent += "\n";
						currentLine++;
					}
				}
				
				outputArea.setText (taContent);
				
			} catch (NumberFormatException exc) {
				
				JOptionPane.showMessageDialog (this, "Error with number format for either start address or count!");
			}
		}
	}
}