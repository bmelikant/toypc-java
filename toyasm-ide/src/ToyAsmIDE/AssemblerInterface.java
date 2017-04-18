package ToyAsmIDE;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.InputStream;

public class AssemblerInterface extends JFrame implements ActionListener {
	
	// member variables / window controls
	private static final long serialVersionUID = 1L;
	int tabIdx;
	
	JMenuBar windowMenuBar;
	
	JMenu fileMenu;
	JMenu optionsMenu;
	JMenu buildMenu;
	JMenu saveMenu;
	JMenu newMenu;
	
	JMenuItem saveCodeFileItem;
	JMenuItem saveCodeFileAsItem;
	JMenuItem openCodeFileItem;
	JMenuItem newCodeFileItem;
	JMenuItem newTextFileItem;
	JMenuItem closeCodeFileItem;
	
	JMenuItem assemblerOptionsItem;
	JMenuItem changeFontItem;
	
	JMenuItem buildCodeItem;
	JMenuItem testCodeItem;
	
	JTextArea errorOutputArea;
	
	CodeDocumentPane documentsPane;

	// public AssemblerInterface(): Constructor method
	public AssemblerInterface () {
		
		tabIdx = 0;
		
		try {
			
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			
		} catch (Exception e) {
			
			JOptionPane.showMessageDialog(null, "Error setting look and feel: " + e.toString());	
		}
		
		// construct the menu
		windowMenuBar = new JMenuBar ();
		
		fileMenu    = new JMenu ("File");
		optionsMenu = new JMenu ("Options");
		buildMenu   = new JMenu ("Build");
		newMenu     = new JMenu ("New");
		saveMenu    = new JMenu ("Save");
		
		newCodeFileItem    = new JMenuItem ("New Source File");
		newTextFileItem    = new JMenuItem ("New Text File");
		
		openCodeFileItem   = new JMenuItem ("Open File");
		saveCodeFileItem   = new JMenuItem ("Save File");
		saveCodeFileAsItem = new JMenuItem ("Save File As...");
		closeCodeFileItem  = new JMenuItem ("Close File");
		
		assemblerOptionsItem = new JMenuItem ("Assembler Options");
		changeFontItem = new JMenuItem ("Choose Editor Font");
		
		buildCodeItem = new JMenuItem ("Build EXE");
		testCodeItem  = new JMenuItem ("Run EXE in new ToyVM");
		
		documentsPane = new CodeDocumentPane ();
		errorOutputArea = new JTextArea ();
		errorOutputArea.setRows (5);
		
		// add the menu bar to the window
		setJMenuBar (windowMenuBar);
		
		windowMenuBar.add (fileMenu);
		windowMenuBar.add (optionsMenu);
		windowMenuBar.add (buildMenu);
		
		fileMenu.add (newMenu);
		fileMenu.add (openCodeFileItem);
		fileMenu.add (saveMenu);
		fileMenu.add (closeCodeFileItem);
		
		newMenu.add (newCodeFileItem);
		newMenu.add (newTextFileItem);
		
		saveMenu.add (saveCodeFileItem);
		saveMenu.add (saveCodeFileAsItem);
		
		optionsMenu.add (assemblerOptionsItem);
		optionsMenu.add (changeFontItem);
		
		buildMenu.add (buildCodeItem);
		buildMenu.add (testCodeItem);
		
		documentsPane.getTabPane().setPreferredSize(new Dimension (700, 500));
		add (documentsPane.getTabPane (), BorderLayout.CENTER);
		add (new JScrollPane (errorOutputArea), BorderLayout.PAGE_END);
		
		newCodeFileItem.addActionListener   (this);
		closeCodeFileItem.addActionListener (this);
		saveCodeFileItem.addActionListener  (this);
		openCodeFileItem.addActionListener  (this);
		buildCodeItem.addActionListener     (this);
		testCodeItem.addActionListener      (this);
		
		redirectStdOut();
		
		((BorderLayout)this.getLayout()).setHgap(10);
		((BorderLayout)this.getLayout()).setVgap(10);
		
		pack ();
		setTitle ("Virtual Machine Assembler IDE");
		setVisible (true);
		setDefaultCloseOperation (EXIT_ON_CLOSE);
	}
	
	// public void actionPerformed (ActionEvent e): Handle button presses and other window events
	public void actionPerformed (ActionEvent e) {
		
		if (e.getSource () == newCodeFileItem) {
			
			// stay in this loop in case a file exists and we need to ask to overwrite it!
			while (true) {
				
				JFileChooser newFileChooser = new JFileChooser ();
				FileNameExtensionFilter fChooseFilter = new FileNameExtensionFilter ("Assembler Source", "asm");
				newFileChooser.setFileFilter(fChooseFilter);
				newFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			
				int result = newFileChooser.showSaveDialog (this);
			
				if (result == JFileChooser.APPROVE_OPTION) {
				
					File endResult = newFileChooser.getSelectedFile();
				
					// if the selected file doesn't exist, open a new document tab with the new file
					if (!endResult.exists()) {
						
						documentsPane.openDocumentTab (endResult, endResult.getName(), true);
						break;
					}
					
					// if it DOES exist, use a JOptionPane to see whether to overwrite it or not
					else {
						
						int confirmResult = JOptionPane.showConfirmDialog(this, "File already exists. Overwrite?");
						
						if (confirmResult == JOptionPane.YES_OPTION) {
							
							documentsPane.openDocumentTab(endResult, endResult.getName(), true);
							break;
						}
						
						// if the dialog was cancelled, break the loop with no further action
						else if (confirmResult == JOptionPane.CANCEL_OPTION)
							break;
					}
				}
			}
		}
		
		else if (e.getSource () == saveCodeFileItem)
			documentsPane.saveCurrentDocumentTab ();
		
		else if (e.getSource () == openCodeFileItem) {
			
			JFileChooser newFileChooser = new JFileChooser ();
			FileNameExtensionFilter fChooseFilter = new FileNameExtensionFilter ("Assembler Source", "asm");
			newFileChooser.setFileFilter(fChooseFilter);
			newFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			
			int result = newFileChooser.showOpenDialog (this);
			
			if (result == JFileChooser.APPROVE_OPTION) {
				
				File endResult = newFileChooser.getSelectedFile();
				documentsPane.openDocumentTab (endResult, endResult.getName(), false);
			}
		}
		
		else if (e.getSource () == closeCodeFileItem)
			documentsPane.closeCurrentTab();
		
		else if (e.getSource () == buildCodeItem) {
			
			// if there is no tab open we cant build the file!
			if (documentsPane.getTabPane().getTabCount() <= 0) {
				
				JOptionPane.showMessageDialog(null, "No document open to build!");
				return;
			}
			
			// ask whether to save file if needed
			if (documentsPane.getCurrentDocument().textWasUpdated()) {
				
				// ask whether it should be saved
				int confirmResult = JOptionPane.showConfirmDialog (this, "File has not been saved. Save now?", "File not saved", JOptionPane.YES_NO_OPTION);
				
				if (confirmResult == JOptionPane.YES_OPTION)
					documentsPane.saveCurrentDocumentTab();
				else
					return;
			}
			
			String path = documentsPane.getCurrentDocFile().getAbsolutePath();
			String exepath = documentsPane.getCurrentDocFile().getAbsolutePath().substring(0, documentsPane.getCurrentDocFile().getAbsolutePath().lastIndexOf('.')) + ".tei";
			errorOutputArea.setText("Building executable (" + exepath + ")...\n\n");
			
                        try {
		
                            String process = "java -jar toyasm.jar " + path.replace (" ", "\\ ") + " -o " + exepath.replace (" ", "\\ ");
                            System.out.println ("Running process on: " + process);
                            
                            Process p = Runtime.getRuntime().exec ("java -jar toyasm.jar " + path.replace (" ", "\\ ")+ " -o " + exepath.replace (" ", "\\ "));
                            p.waitFor ();
                            
                            InputStream pin = p.getInputStream();
                            InputStream perr = p.getErrorStream();
                            
                            byte [] b = new byte [pin.available()];
                            pin.read (b,0,b.length);
                            
                            byte [] errstream = new byte [perr.available()];
                            perr.read (errstream,0,errstream.length);
                            
                            errorOutputArea.append(new String (b));
                            errorOutputArea.append(new String (errstream));
                        
                        } catch (IOException err) {
                            
                            System.err.println ("Error running compiler: " + err.toString ());
                        
                        } catch (InterruptedException err) {
                        
                            System.err.println ("Error - interrupted");
                        }
		}
		
		else if (e.getSource () == testCodeItem) {
			
			// if there is no tab open we can't start an instance!
			if (documentsPane.getTabPane().getComponentCount() <= 0) {
				
				JOptionPane.showMessageDialog(null, "No document open to test!");
				return;
			}
			
			String exepath = documentsPane.getCurrentDocFile().getAbsolutePath().substring(0, documentsPane.getCurrentDocFile().getAbsolutePath().lastIndexOf('.')) + ".tei";
			File exeFile = new File (exepath);
			
			if (!exeFile.exists()) {
				
				// ask whether we should assemble the file or not
				int confirmResult = JOptionPane.showConfirmDialog (this, "File has not been built. Build now?", "File not built", JOptionPane.YES_NO_OPTION);
				
				if (confirmResult == JOptionPane.YES_OPTION) {
					
					String path = documentsPane.getCurrentDocFile().getAbsolutePath();
					errorOutputArea.setText("Building executable (" + exepath + ")...\n\n");
					
                                        
					//Assembler thisAssembler = new Assembler (path, exepath, path + ".lst", "TASM 1.0");
					//thisAssembler.assemble();
				}
				
				else
					return;
			}
			
			String numString = JOptionPane.showInputDialog ("Enter the origin:");
			int base = 10;
			
			// if no string was returned then exit
			if (numString == null)
				return;
			
			if (numString.endsWith ("h")) {
				
				base = 16;
				numString = numString.replace('h', ' ').trim ();
			}
			
			try {
				
				int ip = Integer.parseInt (numString, base);
				MachineInterface testMachine = new MachineInterface ();
				testMachine.loadCodeIntoMachine (exepath, ip, true);
				testMachine.displayMachine ();
				
			} catch (NumberFormatException exc) {
				
				JOptionPane.showMessageDialog(null, "Bad number format: " + numString);
			}
		}
	}
	
	public void redirectStdOut () {
		
		// build a redirection stream
		OutputStream out = new OutputStream() {
			
			public void write(int b) throws IOException {
				updateOutputArea(String.valueOf((char) b));
			}
			
			public void write(byte[] b, int off, int len) throws IOException {
				updateOutputArea(new String(b, off, len));
			}
			
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};
		
		System.setOut(new PrintStream (out, true));
		System.setErr(new PrintStream (out, true));
	}
	
	private void updateOutputArea (final String text) {
		
		SwingUtilities.invokeLater(new Runnable() {
			
			public void run() {
				errorOutputArea.append(text);
			}
		});
	}
	
	// public static void main (String [] args): Main method
	public static void main (String [] args) {
		
		new AssemblerInterface ();
	}
}

// class CodeDocumentPane: hold code documents in a JTabbedPane
class CodeDocumentPane implements KeyListener {
	
	// member variables
	private JTabbedPane ownerPane;
	private ArrayList<CodeDocument> openedDocs;
	private int tabIdx;
	
	// public CodeDocumentPane (): Initialize this pane
	public CodeDocumentPane () {
		
		ownerPane = new JTabbedPane ();
		openedDocs = new ArrayList<CodeDocument> ();
		tabIdx = 0;
	}
	
	// public void openDocumentTab (File toOpen): Open a new document tab with the contents of the requested file
	public void openDocumentTab (File toOpen, String tabName, boolean createNew) {
		
		try {
			
			// if the requested file does not exist, create it. Then read the file in.
			if (createNew) {
				
				if (toOpen.exists ())
					toOpen.delete();
				
				toOpen.createNewFile();
			}
			
			BufferedReader in = new BufferedReader (new InputStreamReader (new FileInputStream (toOpen)));
			String fileContents = "";
			String lineIn = in.readLine();
		
			while (lineIn != null) {
			
				fileContents += lineIn + "\n";
				lineIn = in.readLine ();
			}
		
			in.close ();
		
			// set up the CodeDocument
			CodeDocument toAdd = new CodeDocument (toOpen, fileContents, tabName);
			toAdd.addListener (this);
			
			ownerPane.insertTab(toAdd.getTabName(), null, new JScrollPane(toAdd.getCodeArea()), toAdd.getTabName(), tabIdx++);
			openedDocs.add (toAdd);
			
		} catch (Exception e) {
			
			JOptionPane.showMessageDialog(null, "Error opening document tab: " + e.toString());
		}
	}
	
	// public void saveCurrentDocumentTab (): Save the document tab we are working on
	public void saveCurrentDocumentTab () {
		
		// open the file associated with the current CodeDocument
		File saveFile = openedDocs.get(ownerPane.getSelectedIndex()).getFileName();
		
		try {
			
			PrintWriter outWriter = new PrintWriter (new FileOutputStream (saveFile));
			String [] textLines = openedDocs.get(ownerPane.getSelectedIndex()).getCodeArea().getText().split("\r\n");
			
			// write each line as we get it
			for (int i = 0; i < textLines.length; i++)
				outWriter.println (textLines[i]);
			
			outWriter.flush();
			outWriter.close();
			
			openedDocs.get(ownerPane.getSelectedIndex()).setOriginalText(openedDocs.get(ownerPane.getSelectedIndex()).getCodeArea().getText());
			openedDocs.get(ownerPane.getSelectedIndex()).setTabName(openedDocs.get(ownerPane.getSelectedIndex()).getTabName().replace('*', ' ').trim());
			ownerPane.setTitleAt(ownerPane.getSelectedIndex (), openedDocs.get(ownerPane.getSelectedIndex()).getTabName());
			
		} catch (Exception e) {
			
			JOptionPane.showMessageDialog(null, "Error saving document tab: " + e.toString ());
		}
	}
	
	// public void closeCurrentTab (): Close the tab we are working on, saving if necessary
	public void closeCurrentTab () {
		
		if (ownerPane.getComponentCount() == 0)
			return;
		
		if (openedDocs.get(ownerPane.getSelectedIndex()).textWasUpdated()) {
			
			// prompt for save
			int result = JOptionPane.showConfirmDialog (null, "Would you like to save the file before close?");
			if (result == JOptionPane.OK_OPTION)
				saveCurrentDocumentTab ();
		}
		
		openedDocs.remove (ownerPane.getSelectedIndex());
		ownerPane.removeTabAt (ownerPane.getSelectedIndex());
		tabIdx--;
	}
	
	// public JTabbedPane getTabPane (): Return our JTabbedPane
	public JTabbedPane getTabPane () { return ownerPane; }
	public File getCurrentDocFile () { return openedDocs.get(ownerPane.getSelectedIndex()).getFileName(); }
	public CodeDocument getCurrentDocument () { return openedDocs.get(ownerPane.getSelectedIndex()); }
	
	// capture keyTyped events
	public void keyTyped (KeyEvent e) {
		
		// check to see the state of the current CodeDocument
		if (openedDocs.get(ownerPane.getSelectedIndex()).textWasUpdated()) {
			
			if (!openedDocs.get(ownerPane.getSelectedIndex()).getTabName().endsWith("*"))
				openedDocs.get(ownerPane.getSelectedIndex()).setTabName(openedDocs.get(ownerPane.getSelectedIndex()).getTabName() + "*");
		}
		
		else
			openedDocs.get(ownerPane.getSelectedIndex()).setTabName(openedDocs.get(ownerPane.getSelectedIndex()).getTabName().replace('*', ' '));
		
		ownerPane.setTitleAt(ownerPane.getSelectedIndex (), openedDocs.get(ownerPane.getSelectedIndex()).getTabName());
	}
	
	// ignore these two, we only want to use keyTyped
	public void keyPressed (KeyEvent e) { }
	public void keyReleased (KeyEvent e) { }
}

// class CodeDocument: information on code documents
class CodeDocument {

	// member variables
	JTextPane myCodeArea;		// this document's code text
	File myFileName;			// the name of this code document's file
	String originalText;		// the contents that this control had at startup
	String tabName;				// the associated tab name for this document
	JButton closeButton;		// make a close button for the tab
	
	// Constructor used with a new code file
	public CodeDocument (File associatedName, String text, String name) {
		
		myFileName = associatedName;
		myCodeArea = new JTextPane ();
		myCodeArea.setFont (new Font ("Courier New", Font.PLAIN, 12));
		originalText = text;
		tabName = name;
		
		myCodeArea.setText (originalText);
	}
	
	// accessors
	public String getTabName () { return tabName; }
	public JTextPane getCodeArea () { return myCodeArea; }
	public File getFileName () { return myFileName; }
	
	// mutators
	public void setTabName (String name) { tabName = name.trim (); }
	public void setOriginalText (String text) { originalText = text; }
	
	public void addListener (KeyListener toAdd) { myCodeArea.addKeyListener(toAdd); }
	
	// check to see if the content of this control changed
	public boolean textWasUpdated () {
		
		if (myCodeArea.getText().equals(originalText))
			return false;
		else
			return true;
	}
}