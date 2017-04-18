package ToyDisk;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

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
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;

public class DiskEditorInterface extends JFrame implements ActionListener {

	// constant member variables
	private static final long serialVersionUID = 1L;
	
	// type member variables
	DiskController loadedDisk;
	boolean diskIsLoaded = false;
	String diskFileName = null;
	File diskFile;
	
	// window controls
	JMenuBar topMenu;
	JMenu diskMenu, filesMenu;
	JMenuItem newDiskItem, openDiskItem, storeDiskItem, formatDiskItem;
	JMenuItem addFileItem, viewFileItem, deleteFileItem;
	JScrollPane listPane;
	JList fnameList;
	JLabel diskStatsLabel, diskStatsExtendedLabel;
	
	GridBagLayout winLayout;
	GridBagConstraints wc;
	
	// constructor method
	public DiskEditorInterface () {
		
		// create the layout for this window
		winLayout = new GridBagLayout ();
		wc = new GridBagConstraints ();
		setLayout (winLayout);
		
		// set up the top menu
		setupTopMenu ();
		
		// instantiate the controls for the window
		diskStatsLabel = new JLabel ("Disk Statistics: No Disk Loaded");
		diskStatsExtendedLabel = new JLabel ("No extended stats available");
		
		fnameList      = new JList();
		listPane       = new JScrollPane (fnameList);
		listPane.setPreferredSize(new Dimension (600, 400));
		
		wc.insets = new Insets (5, 5, 5, 5);
		
		// add the list and it's label
		addComponent (new JLabel ("File List:"), 0, 2, 1, 1);
		
		wc.weightx = wc.weighty = 1.0;
		wc.fill = GridBagConstraints.BOTH;
		addComponent (listPane, 0, 3, 3, 1);
		addComponent (diskStatsLabel, 0, 0, 3, 1);
		addComponent (diskStatsExtendedLabel, 0, 1, 3, 1);
		
		// set up this window
		setJMenuBar (topMenu);
		pack ();
		setTitle ("Toy Disk Editor");
		setDefaultCloseOperation (EXIT_ON_CLOSE);
		setVisible (true);
	}

	// add components to the window
	public void addComponent (Component c, int x, int y, int w, int h) {
		
		wc.gridx = x;
		wc.gridy = y;
		wc.gridwidth = w;
		wc.gridheight = h;
		
		winLayout.setConstraints (c, wc);
		add (c);
	}
	
	public void setupTopMenu () {
		
		// set up the menus
		topMenu   = new JMenuBar ();
		
		diskMenu  = new JMenu ("Disk Options");
		filesMenu = new JMenu ("File Options");
		
		newDiskItem    = new JMenuItem ("New Disk File");
		openDiskItem   = new JMenuItem ("Open Disk File");
		storeDiskItem  = new JMenuItem ("Save Disk File");
		formatDiskItem = new JMenuItem ("Format Disk");
		
		addFileItem    = new JMenuItem ("Add Existing File");
		viewFileItem   = new JMenuItem ("View File Contents");
		deleteFileItem = new JMenuItem ("Delete File");
		
		topMenu.add (diskMenu);
		topMenu.add (filesMenu);
		
		diskMenu.add (newDiskItem);
		diskMenu.add (openDiskItem);
		diskMenu.add (storeDiskItem);
		diskMenu.add (formatDiskItem);
		
		filesMenu.add(addFileItem);
		filesMenu.add(viewFileItem);
		filesMenu.add(deleteFileItem);
		
		// register listeners for the items
		newDiskItem.addActionListener    (this);
		openDiskItem.addActionListener   (this);
		storeDiskItem.addActionListener  (this);
		formatDiskItem.addActionListener (this);
		
		addFileItem.addActionListener    (this);
		viewFileItem.addActionListener   (this);
		deleteFileItem.addActionListener (this);
		
		filesMenu.setEnabled (false);
	}
	
	// handle events for this window's controls
	public void actionPerformed(ActionEvent evt) {
		
		if (evt.getSource () == newDiskItem) {
			
			// accessory controls for the fileChooser
			JPanel accessoryPanel = new JPanel ();
			JSpinner sizeSelector = new JSpinner ();
			sizeSelector.setModel (new SpinnerNumberModel (64, 64, 10240, 64));
			((JSpinner.DefaultEditor) sizeSelector.getEditor()).getTextField().setEditable(false);
			
			accessoryPanel.add (new JLabel ("Disk Size (in Kb):"), BorderLayout.PAGE_START);
			accessoryPanel.add (sizeSelector, BorderLayout.CENTER);
			
			// show a chooser to ask where to store the file
			JFileChooser diskFileChooser = new JFileChooser ();
			FileNameExtensionFilter diskFilter = new FileNameExtensionFilter ("Toy Disk Files (.dsk", "dsk");
			diskFileChooser.setFileFilter(diskFilter);
			diskFileChooser.setAccessory (accessoryPanel);
			
			int chooserResult = diskFileChooser.showSaveDialog(this);
			
			// see what the result is. If the file chosen exists, ask whether to overwrite
			if (chooserResult == JFileChooser.APPROVE_OPTION) {
				
				try {
					
					// see whether to overwrite an existing file
					if (diskFileChooser.getSelectedFile().exists()) {
					
						if (JOptionPane.showConfirmDialog(this, "File exists. Overwrite?") == JOptionPane.NO_OPTION)
							return;
					
						diskFileChooser.getSelectedFile().delete();
					}
				
					diskFile = diskFileChooser.getSelectedFile();
					diskFile.createNewFile();
					int diskKbSize = (int) sizeSelector.getValue();
					System.out.println ("diskKbSize = " + diskKbSize);
					
					loadedDisk = new DiskController (diskKbSize*1024, 0x21F0);
					DiskController.storeDisk (diskFile, loadedDisk);
					
				} catch (Exception e) {
					
					JOptionPane.showMessageDialog (this, "Error making disk file: " + e.toString ());
					
				}
			}
		}
		
		else if (evt.getSource () == openDiskItem) {
			
			// open a disk for reading and writing. We will
			// eventually use the filesystem to load the disk's content
			JFileChooser diskFileChooser = new JFileChooser ();
			FileNameExtensionFilter diskFilter = new FileNameExtensionFilter ("Toy Disk Files (.dsk", "dsk");
			diskFileChooser.setFileFilter(diskFilter);
			int chooserResult = diskFileChooser.showOpenDialog(this);
			
			if (chooserResult == JFileChooser.APPROVE_OPTION) {
				
				// try to use the static loadDisk method to load the file!
				loadedDisk = DiskController.loadDisk(diskFileChooser.getSelectedFile());
				
				if (loadedDisk != null) {
					
					diskIsLoaded = true;
					diskFileName = diskFileChooser.getSelectedFile().getName();
					diskFile = diskFileChooser.getSelectedFile();
				}
			}
		}
		
		// are we saving the disk's contents?
		else if (evt.getSource () == storeDiskItem) {
		
			// if a disk is loaded, store it
			if (loadedDisk != null) {
				
				DiskController.storeDisk(diskFile, loadedDisk);
				JOptionPane.showMessageDialog(this, "Saved disk contents!");
			}
		}
		
		// are we formatting a disk?
		else if (evt.getSource () == formatDiskItem) {
			
			if (loadedDisk != null) {
				
				String [] filesystems = { "None (RAW)", "ToyFAT v1.0", "ToyFAT v2.0" };
				
				// determine which filesystem to use, default is FileSystem.FILESYSTEM_TOYFAT_V2
				String filesys = (String) JOptionPane.showInputDialog(this, "Please select the filesystem to use:", "Which Filesystem?", 
						JOptionPane.PLAIN_MESSAGE, null, filesystems, filesystems[0]);
				
				System.out.println (filesys);
				
				if (filesys == null || filesys.equals ("None (RAW)"))
					FileSystem.formatDisk(loadedDisk, FileSystem.FILESYSTEM_NONE);
				else if (filesys.equals ("ToyFAT v1.0"))
					FileSystem.formatDisk(loadedDisk, FileSystem.FILESYSTEM_FAT_V1);
				else if (filesys.equals ("ToyFAT v2.0"))
					FileSystem.formatDisk(loadedDisk, FileSystem.FILESYSTEM_TOYFAT_V2);
				
				JOptionPane.showMessageDialog(this, "Disk formatted!\nBe sure to save disk contents.");
			}
		}
		
		// are we adding a file to the disk?
		else if (evt.getSource() == addFileItem) {
			
			// pop up an input dialog asking for a file name to add
			JFileChooser toAddChooser = new JFileChooser ();
			if (toAddChooser.showOpenDialog(this) == JFileChooser.CANCEL_OPTION)
				return;
			
			String fname = toAddChooser.getSelectedFile().getName();
			
			if (fname == null)
				return;
			
			else if (fname.length() > 12) {
				
				JOptionPane.showMessageDialog(this, "Filename must be less than eleven characters:\n" + fname);
				return;
			}
			
			else {
				
				try {
					
					// read the contents of the selected file from the Java NIO package						
					System.out.println ("Starting I/O read...");
					FileChannel srcFile = new FileInputStream (toAddChooser.getSelectedFile()).getChannel();
					
					// split the FileChannel into multiple blocks
					ByteBuffer contents = ByteBuffer.allocate ((int) srcFile.size());
					srcFile.read(contents);
					contents.rewind ();
						
					byte [] dataArray = new byte [contents.remaining()];
					contents.get (dataArray);

					int sectors = (dataArray.length / 512) + 1;
					short [][] dataSectors = new short[sectors][512];
						
					// copy the data into the data sectors
					for (int i = 0; i < dataSectors.length; i++) {
							
						for (int j = 0; (i*512)+j < dataArray.length && j < 512; j++)
							dataSectors[i][j] = (short) dataArray [(i*512)+j];
					}
					
					System.out.println ("Data sector count for file: " + dataSectors.length);
					System.out.println ("Total file length: " + dataArray.length);
					
					// now we should have everything the way we need it! And FAST!!
					int storeResult = FileSystem.storeFileToDisk(loadedDisk, fname, dataSectors);
				
					if (storeResult == FileSystem.STORE_UNKNOWN_FAILURE)
						JOptionPane.showMessageDialog (this, "Error storing file to disk!");
					else if (storeResult == FileSystem.STORE_CLUSTER_FAILURE)
						JOptionPane.showMessageDialog (this, "Error storing file to disk:\nCluster Error!");
					if (storeResult == FileSystem.STORE_ALREADY_EXISTS)
						JOptionPane.showMessageDialog (this, "Error storing file to disk:\nFile Exists!");
					
				} catch (IOException e) {
					
					JOptionPane.showMessageDialog (this, "Could not read existing file from hard disk: " + e.toString());
				}
			}
		}
		
		// see if we are supposed to view a file
		else if (evt.getSource () == viewFileItem) {
			
			// try to read the file that is selected
			String fileContents = FileSystem.loadFileFromDisk (loadedDisk, (String) fnameList.getSelectedValue());
			
			if (fileContents == null)
				JOptionPane.showMessageDialog(this, "File was not found on disk! Check your coding!");
			
			else {
				
				JOptionPane.showMessageDialog(this, "File contents:\n" + fileContents);
			}
		}
		
		// update our text fields here!
		updateDisplayFields ();
	}
	
	public void updateDisplayFields () {
		
		// if the disk is loaded, display it's statistics in the statistics label
		if (loadedDisk != null) {
			
			Object [] fileList = FileSystem.loadFileNames(loadedDisk);
			String diskStats = "Disk Statistics: Disk Name-" + diskFileName;
			String diskExtStats = "Extended stats: ";
			
			// build diskStats and diskExtStats strings and display them
			if (FileSystem.checkForFileSys(loadedDisk) == FileSystem.FILESYSTEM_FAT_V1 ||
					FileSystem.checkForFileSys (loadedDisk) == FileSystem.FILESYSTEM_TOYFAT_V2) {
				
				// if it is v1, add v1.0 tag. If it is v2, add v2.0 tag
				
				diskStats += "   File System-ToyFAT " + ((FileSystem.checkForFileSys(loadedDisk) == FileSystem.FILESYSTEM_FAT_V1) ? "v1.0" : "v2.0") + 
				"   Files-" + fileList.length + "   Used Clusters-" + FileSystem.usedClusterCount (loadedDisk) +
				"   Free Clusters-" + FileSystem.freeClusterCount(loadedDisk) + "   Bad Clusters-" + FileSystem.badClusterCount(loadedDisk);
				
				diskExtStats += "Disk Size (Kb): " + (loadedDisk.getDiskByteCount() / 1024) + "   Total sectors: " + loadedDisk.getDiskSectorCount ();
			}
			
			else if (FileSystem.checkForFileSys(loadedDisk) == FileSystem.FILESYSTEM_NONE) {
				
				diskStats += "   File System-RAW";
				diskExtStats += "Disk Size (Kb): " + (loadedDisk.getDiskByteCount() / 1024) + "   Total sectors: " + loadedDisk.getDiskSectorCount ();
			}
			
			diskStatsLabel.setText(diskStats);
			diskStatsExtendedLabel.setText (diskExtStats);
			
			// if the list was loaded, set this as the list data
			fnameList.setListData(fileList);
			filesMenu.setEnabled(true);
		}
		
		else {
			
			 diskStatsLabel.setText("Disk Statistics: No disk loaded");
			 diskStatsExtendedLabel.setText ("No extended stats available");
			 filesMenu.setEnabled(false);
		}
		
		pack ();
	}
	
	// main method
	public static void main (String [] args) {
		
		new DiskEditorInterface ();
	}
}

class FileSystem {
	
	// constants
	public static final int FILESYSTEM_NONE      = 0x00;
	public static final int FILESYSTEM_FAT_V1    = 0x01;
	public static final int FILESYSTEM_TOYFAT_V2 = 0x02;
	
	public static final int STORE_CLUSTER_FAILURE = 0x00;
	public static final int STORE_SUCCESS         = 0x01;
	public static final int STORE_ALREADY_EXISTS  = 0x02;
	public static final int STORE_UNKNOWN_FAILURE = 0x03;
	public static final int STORE_CRC_ERROR       = 0x04;
	
	public static final int EMPTY_CLUSTER = 0xffff;
	public static final int EOF_CLUSTER   = 0xfffe;
	public static final int BAD_CLUSTER   = 0xfffd;
	
	private static final String emptyPath = "\0\0\0\0\0\0\0\0\0\0\0";
	
	// toyfat v1.0 constants. Some are used with toyfat v2.0 as well
	private static final short fatBytesPerSector  = 512;		// there are 512 bytes per sector in all toyfat versions!
	private static final short fatReservedSectors = 1;			// one reserved sector; the boot sector
	private static final short fatRootEntries     = 96;			// 96 files can be saved with toyfat v1.0
	private static final short fatCopies          = 2;			// there are always two fat copies on toyfat v1.0 disks
	
	// toyfat v2.0 constants
	private static final short fatRootEntriesv2 = 224;			// 224 files for toyfat v2.0
	
	// public static void formatDisk (DiskController toFormat, int fsys):
	public static void formatDisk (DiskController toFormat, int fsys) {
		
		// make a new first sector
		short [] sectorZero  = new short [512];
		short [] emptySector = new short [512];
		short [] fatSector   = new short [512];
		
		// initialize the FAT sector
		for (int i = 0; i < fatSector.length; i++)
			fatSector[i] = 0xff;
		
		// if the filesystem is FILESYSTEM_NONE, write the blank block to sector zero
		if (fsys == FILESYSTEM_NONE) {

			for (int i = 0; i < toFormat.getDiskSectorCount(); i++)
				toFormat.storeDiskSector(emptySector, i);
		}
		
		// format as ToyFAT v1.0
		else if (fsys == FILESYSTEM_FAT_V1) {
			
			// set up our boot sector
			sectorZero[0] = 0x01;									// toyfat v1.0
			sectorZero[1] = (short) (fatBytesPerSector & ~0xff00);
			sectorZero[2] = (short) (fatBytesPerSector >> 8);
			sectorZero[3] = fatReservedSectors;
			sectorZero[4] = fatRootEntries;
			sectorZero[5] = fatCopies;
			
			// and write it in
			toFormat.storeDiskSector(sectorZero, 0);
			
			// destroy existing root directory
			for (int i = 1; i < 7; i++)
				toFormat.storeDiskSector(emptySector, i);
			
			// and destroy FAT tables
			toFormat.storeDiskSector(fatSector, 7);
			toFormat.storeDiskSector(fatSector, 8);
		}
		
		// format as ToyFAT v2.0
		else if (fsys == FILESYSTEM_TOYFAT_V2) {
			
			// compute the number of sectors per FAT for this disk. We have to be able to hold
			// 2 byte entries for each sector on the disk
			int fatSectors = ((toFormat.getDiskSectorCount() * 2) / 512) + 1;	// number of sectors for each FAT
			System.out.println ("Sectors per FAT for this disk: " + fatSectors);
			
			// set up the boot sector
			sectorZero[0] = 0x02;			// toyfat v2.0
			sectorZero[1] = (short) (fatBytesPerSector & ~0xff00);
			sectorZero[2] = (short) (fatBytesPerSector >> 8);
			sectorZero[3] = fatReservedSectors;
			sectorZero[4] = (short) (fatRootEntriesv2 & ~0xff00);
			sectorZero[5] = (short) (fatRootEntriesv2 >> 8);
			sectorZero[6] = fatCopies;
			sectorZero[7] = (short) fatSectors;
			
			// store the boot sector
			toFormat.storeDiskSector(sectorZero, 0);
			
			// destroy root directory table
			for (int i = 1; i < 16; i++)
				toFormat.storeDiskSector(emptySector, i);
			
			// destroy FAT tables
			for (int i = 0; i < fatSectors*2; i++) {
				
				// start at sector 16 for FAT entries with ToyFAT v2.0
				toFormat.storeDiskSector(fatSector, i+16);
			}
			
			// here we want to edit the FAT sectors to reflect the fact that
			// we cannot store to some sectors of the disk. Obviously sectors that are part of the root directory
			// or the FAT cannot be written to, nor can the bootsector. So we deduct these from the disk's total sector count.
			// We then compute any entries that cannot be equated to a logical sector and flag them as bad clusters.
			int totalSectors = (((toFormat.getDiskSectorCount () - fatReservedSectors) - ((fatRootEntriesv2*32)/512) - (fatSectors*fatCopies)));
			int supportedSectors = (fatSectors*512)/2;

			// enter a loop that writes "bad sectors" to all unavailable sectors
			for (int i = totalSectors; i <= supportedSectors; i++) {
				
				setClusterValue (toFormat, i, BAD_CLUSTER);
			}
		}
	}
	
	// static method to check disk's file system
	public static int checkForFileSys (DiskController toCheck) {
		
		return toCheck.readDiskSector(0)[0];
	}
	
	// store a file to the disk
	public static int storeFileToDisk (DiskController toStore, String fileName, short [][] dataSectors) {
		
		short [] curSector;
		
		// check for ToyFAT v1.0 filesystem.
		if (checkForFileSys (toStore) == FILESYSTEM_FAT_V1) {
			
			// attempt to store the file on the disk. First locate an empty root entry
			for (int i = 1; i < 7; i++) {
			
				// load a sector
				curSector = toStore.readDiskSector(i);
			
				// check for empty entry
				for (int j = 0; j < 512; j += 32) {
				
					String fname = "";
				
					// copy the eleven byte filename from the entry
					for (int k = 0; k < 11; k++)
						fname += (char) curSector[j+k];
				
					// if we found a duplicate file, return an error
					if (fname.equals(FileSystem.convertToLoadedName(fileName)))
						return STORE_ALREADY_EXISTS;
					
					// if we found an empty entry, store the file here
					if (fname.equals(emptyPath)) {
					
						// convert the name to a ToyFAT name
						fileName = FileSystem.convertToLoadedName(fileName);
						System.out.println (fileName);
					
						// write the data into the sector
						for (int k = 0; k < 11; k++)
							curSector[j+k] = (short) fileName.charAt (k);
					
						// find the first available cluster; mark it
						int startCluster = findAvailableCluster (toStore);
						int absStart = startCluster;
						
						if (startCluster == EMPTY_CLUSTER)
							return STORE_CLUSTER_FAILURE;
					
						// store the first cluster into the root entry
						curSector[j+12] = (short) (startCluster & ~0xff00);
						curSector[j+13] = (short) (startCluster >> 8);
					
						// now write the data sectors. Find available clusters
						// and mark them. After that, store the data in the given cluster
						int lastCluster = startCluster;
						setClusterValue (toStore, lastCluster, EOF_CLUSTER);
						
						for (int x = 0; x < dataSectors.length; x++) {
						
							// find the next available cluster
							setClusterValue (toStore, lastCluster, EOF_CLUSTER);
							
							int nextCluster = findAvailableCluster (toStore);
						    
							// if we can't find a free cluster, exit with an error
							if (nextCluster == EMPTY_CLUSTER || nextCluster == lastCluster)
								return STORE_CLUSTER_FAILURE;
						
							// store the data from the current data sector into the given cluster
							// we have to adjust for boot sector at sector zero, root directory
							// at sectors 1 - 6, and FAT tables at 7 - 8. That makes our starting
							// cluster number 9
							toStore.storeDiskSector(dataSectors[x], lastCluster+9);
							
							// store this cluster's value at the last cluster
							setClusterValue (toStore, lastCluster, nextCluster);
						
							System.out.println ("Last cluster: " + lastCluster + " Next cluster: " + nextCluster);
							
							// now set the cluster we are working on to be the last cluster
							// for the continuation of the loop
							lastCluster = nextCluster;
						}
					
						// flag that this is the last cluster
						setClusterValue (toStore, lastCluster, EOF_CLUSTER);
					
						// if all went well, store the root directory
						// and the disk should be updated!
						toStore.storeDiskSector (curSector, i);
						return STORE_SUCCESS;
					}
				}
			}
		}
		
		// check for ToyFAT v2.0 filesystem.
		else if (checkForFileSys (toStore) == FILESYSTEM_TOYFAT_V2) {
			
			// attempt to store the file on the disk. First locate an empty root entry
			for (int i = 1; i < 16; i++) {
			
				// load a sector
				curSector = toStore.readDiskSector(i);
			
				// check for empty entry
				for (int j = 0; j < 512; j += 32) {
				
					String fname = "";
				
					// copy the eleven byte filename from the entry
					for (int k = 0; k < 11; k++)
						fname += (char) curSector[j+k];
				
					// if we found a duplicate file, return an error
					if (fname.equals(FileSystem.convertToLoadedName(fileName)))
						return STORE_ALREADY_EXISTS;
					
					// if we found an empty entry, store the file here
					if (fname.equals(emptyPath)) {
					
						// convert the name to a ToyFAT name
						fileName = FileSystem.convertToLoadedName(fileName);
						System.out.println (fileName);
					
						// write the data into the sector
						for (int k = 0; k < 11; k++)
							curSector[j+k] = (short) fileName.charAt (k);
					    
						// find the first available cluster; mark it
						int startCluster = findAvailableCluster (toStore);
						int absStart = startCluster;							// our undo location!
						
						if (startCluster == EMPTY_CLUSTER) {
							
							// invalidate this root directory entry
							for (int k = 0; k < 11; k++)
								curSector[j+k] = (short) '\0';
							
							toStore.storeDiskSector (curSector, i);
							return STORE_CLUSTER_FAILURE;
						}
						
						// store the first cluster into the root entry
						curSector[j+12] = (short) (startCluster & ~0xff00);
						curSector[j+13] = (short) (startCluster >> 8);
						
						// save the root entry
						toStore.storeDiskSector (curSector, i);
						
						// now write the data sectors. Find available clusters
						// and mark them. After that, store the data in the given cluster
						int lastCluster = startCluster;
						setClusterValue (toStore, lastCluster, EOF_CLUSTER);
						
						for (int x = 0; x < dataSectors.length; x++) {
						
							setClusterValue (toStore, lastCluster, EOF_CLUSTER);
							
							// if x+1 >= dataSectors.length we need to stop!
							if (x+1 >= dataSectors.length) {
								
								setClusterValue (toStore, lastCluster, EOF_CLUSTER);
								
								// compute the offset of the cluster. Add the number of FAT sectors * 2 to the first cluster
								int sectorOffset = 15 + ((toStore.readDiskSector(0)[7]*2));
								toStore.storeDiskSector (dataSectors[x], lastCluster+sectorOffset);
								
								break;
							}
							
							// find the next available cluster
							int nextCluster = findAvailableCluster (toStore);
							
							// if we can't find a free cluster, exit with an error
							if (nextCluster == EMPTY_CLUSTER) {
								
								// roll back disk changes by setting this to an empty root entry and EMPTY_CLUSTER for start cluster
								for (int k = 0; k < 11; k++)
									curSector[j+k] = (short) '\0';
								
								curSector[j+12] = (short) (EMPTY_CLUSTER >> 8);
								curSector[j+13] = (short) (EMPTY_CLUSTER & ~0xff00);
								
								// now follow the cluster chain starting at absStart and flag all entries as free!
								for (int cluster = absStart; cluster != EOF_CLUSTER && cluster != EMPTY_CLUSTER;) {
									
									nextCluster = readClusterValue (toStore, cluster);
									setClusterValue (toStore, cluster, EMPTY_CLUSTER);
									cluster = nextCluster;
								}
								
								toStore.storeDiskSector(curSector, i);
								
								return STORE_CLUSTER_FAILURE;
							}
							
							if (nextCluster == lastCluster)
								return STORE_CRC_ERROR;
							
							// store this cluster's value at the last cluster
							setClusterValue (toStore, lastCluster, nextCluster);
						
							// now set the cluster we are working on to be the last cluster
							// for the continuation of the loop
							// compute the offset of the cluster. Add the number of FAT sectors * 2 to the first cluster
							int sectorOffset = ((toStore.readDiskSector(0)[7]*2))+15;
							toStore.storeDiskSector (dataSectors[x], lastCluster+sectorOffset);
							
							lastCluster = nextCluster;
						}
					
						return STORE_SUCCESS;
					}
				}
			}
		}
		
		return STORE_UNKNOWN_FAILURE;
	}
	
	// load file contents from the disk given a pathname and a disk
	public static String loadFileFromDisk (DiskController toLoad, String fName) {
	
		// this is the current root directory sector we are on
		short [] curSector;
		short [] fatSector = toLoad.readDiskSector(7);
		
		// toyfat v1.0?
		if (checkForFileSys (toLoad) == FILESYSTEM_FAT_V1) {

			// locate the correct entry
			for (int i = 1; i < 7; i++) {
			
				curSector = toLoad.readDiskSector(i);
				fName = convertToLoadedName (fName);
			
				// loop through the sector, looking for directory entries
				for (int j = 0; j < 512; j += 32) {
				
					String curFile = "";
				
					// read the filename from this entry
					for (int k = 0; k < 11; k++)
						curFile += (char) curSector[j+k];
				
					// did we find a matching name?
					if (fName.equals (curFile)) {
					
						// read the file in
						String contents = "";
						int firstCluster = (curSector[j+13] << 8) | curSector[j+12];
					
						// now enter a loop, reading clusters and their content
						while (firstCluster != EOF_CLUSTER && firstCluster != EMPTY_CLUSTER) {
						
							// find the next cluster for later
							int nextCluster = (fatSector[firstCluster+1] << 8) | fatSector[firstCluster];
						
							// read the data from the current cluster
							short [] sectorData = toLoad.readDiskSector(firstCluster+9);
						
							// now enter a loop, copy the data from sectorData into contents
							for (int x = 0; x < 512 && sectorData[x] != '\0'; x++)
								contents += (char) sectorData[x];
						
							// now the nextCluster becomes the firstCluster
							firstCluster = nextCluster;
						}
					
						return contents;
					}
				}
			}
		}
		
		if (checkForFileSys (toLoad) == FILESYSTEM_TOYFAT_V2) {

			// locate the correct entry
			for (int i = 1; i < 16; i++) {
			
				curSector = toLoad.readDiskSector(i);
				fName = convertToLoadedName (fName);
			
				// loop through the sector, looking for directory entries
				for (int j = 0; j < 512; j += 32) {
				
					String curFile = "";
				
					// read the filename from this entry
					for (int k = 0; k < 11; k++)
						curFile += (char) curSector[j+k];
				
					// did we find a matching name?
					if (fName.equals (curFile)) {
					
						// read the file in
						String contents = "";
						int firstCluster = (curSector[j+13] << 8) | curSector[j+12];
						int current = 1;
						
						// now enter a loop, reading clusters and their content
						while (firstCluster != EOF_CLUSTER && firstCluster != EMPTY_CLUSTER) {
							
							// find the next cluster for later
							int nextCluster = readClusterValue (toLoad, firstCluster);
							System.out.println ("Reading file - Last Cluster: " + firstCluster + " Next Cluster: " + ((nextCluster == EOF_CLUSTER) ? "EOF Cluster" : nextCluster));
							
							// read the data from the current cluster
							int clusterOffset = ((toLoad.readDiskSector(0)[7]*2))+15;
							short [] sectorData = toLoad.readDiskSector(firstCluster+clusterOffset);
						
							// now enter a loop, copy the data from sectorData into contents
							for (int x = 0; x < 512 && sectorData[x] != '\0'; x++)
								contents += (char) sectorData[x];
						
							// now the nextCluster becomes the firstCluster
							firstCluster = nextCluster;
						}
					
						return contents;
					}
				}
			}
		}
		
		return null;
	}
	
	// load the filenames from the disk
	public static Object [] loadFileNames (DiskController toLoad) {
		
		// create a list to hold the names
		ArrayList fNames = new ArrayList();
		short [] curSector;
		
		// toyfat v1.0?
		if (checkForFileSys (toLoad) == FILESYSTEM_FAT_V1) {
			
			// load the filenames from the root directory. We have to read six sectors for
			// the root directory
			for (int i = 1; i < 7; i++) {
			
				curSector = toLoad.readDiskSector(i);
			
				// now read each entry in the sector for the filename
				for (int j = 0; j < 512; j += 32) {
				
					String fname = "";
				
					// copy the eleven byte filename from the entry
					for (int k = 0; k < 11; k++)
						fname += (char) curSector[j+k];
				
					if (!fname.equals(emptyPath))
						fNames.add(convertToPathname(fname));
				}
			}
		}
		
		// toyfat v2.0?
		else if (checkForFileSys (toLoad) == FILESYSTEM_TOYFAT_V2) {
			
			for (int i = 1; i < 16; i++) {
				
				curSector = toLoad.readDiskSector (i);
			
				// now read each entry in the sector for the filename
				for (int j = 0; j < 512; j += 32) {
				
					String fname = "";
				
					// copy the eleven byte filename from the entry
					for (int k = 0; k < 11; k++)
						fname += (char) curSector[j+k];
				
					if (!fname.equals(emptyPath))
						fNames.add(convertToPathname(fname));
				}
			}
		}
		
		// return the filenames
		return fNames.toArray();
	}
	
	// find an available cluster on the disk; return it's number
	public static int findAvailableCluster (DiskController toRead) {
		
		// start at sector seven for toyfat v1.0
		if (checkForFileSys (toRead) == FILESYSTEM_FAT_V1) {
			
			short [] fatSector = toRead.readDiskSector (7);
		
			// enter a cluster finding loop. The first FAT is located at cluster
			// seven, the second at cluster eight
			for (int i = 0; i < 512; i += 2) {
			
				// read the cluster from the disk sector
				int clusterVal = (fatSector[i+1] << 8) | fatSector[i];
			
				// see if the cluster is equal to empty_cluster (0xffff)
				if (clusterVal == EMPTY_CLUSTER)
					return (i/2);
			}
		}
		
		// we have to compute the fat sectors for toyfat v2.0
		else if (checkForFileSys (toRead) == FILESYSTEM_TOYFAT_V2) {
			
			short fatSectors = toRead.readDiskSector(0)[7];
			
			// read each fat sector to check for empty cluster
			for (int i = 0; i < fatSectors; i++) {
				
				short [] currentSector = toRead.readDiskSector(i+16);
				
				// now enter a sublooop to read in clusters from the sector
				for (int j = 0; j < 512; j += 2) {
					
					int clusterVal = (currentSector[j+1] << 8) | currentSector[j];
					if (clusterVal == EMPTY_CLUSTER)
						return ((i*512)+j)/2;
				}
			}
		}
		
		return EMPTY_CLUSTER;
	}
	
	public static int readClusterValue (DiskController toRead, int cluster) {
		
		// toyfat v1.0 is easy, just load cluster from sector seven
		if (checkForFileSys(toRead) == FILESYSTEM_FAT_V1) {
			
			short [] fatSector = toRead.readDiskSector (7);
			return ((fatSector[(cluster*2)+1] << 8) | fatSector[(cluster*2)]);
		}
		
		// for toyfat v2.0 we have to compute which sector the cluster will be in
		else if (checkForFileSys(toRead) == FILESYSTEM_TOYFAT_V2) {
			
			// compute which sector to read based off of cluster / 512
			int sector = ((cluster*2)/512)+16;			
			return ((toRead.readDiskSector(sector)[((cluster*2)%512)+1] << 8) | toRead.readDiskSector(sector)[((cluster*2)%512)]);
		}
		
		return EMPTY_CLUSTER;
	}
	
	// store a value at a given cluster in the FAT
	public static void setClusterValue (DiskController toEdit, int cluster, int clusterVal) {
		
		// using toyfat v1.0?
		if (checkForFileSys (toEdit) == FILESYSTEM_FAT_V1) {
			
			// read the FAT table from the disk
			short [] fatSector = toEdit.readDiskSector (7);
		
			// now store the new cluster value into the FAT table
			fatSector[(cluster*2)]   = (short) (clusterVal & ~0xff00);
			fatSector[(cluster*2)+1] = (short) (clusterVal >> 8);
		
			// write the sector back to the disk
			toEdit.storeDiskSector(fatSector, 7);
		}
		
		// using toyfat v2.0?
		else if (checkForFileSys (toEdit) == FILESYSTEM_TOYFAT_V2) {
			
			// compute the appropriate sector, read it from the disk
			short [] fatSector = toEdit.readDiskSector (((cluster*2)/512)+16);
		
			fatSector[((cluster*2)%512)]   = (short) (clusterVal & ~0xff00);
			fatSector[((cluster*2)%512)+1] = (short) (clusterVal >> 8);
			
			// store the sector back to the disk
			toEdit.storeDiskSector (fatSector, ((cluster*2)/512)+16);
		}
	}
	
	// convert an FSYS loaded name into a pathname
	public static String convertToPathname (String loadedName) {
		
		if (loadedName.indexOf(' ') > -1) {
			
			String prefix = loadedName.substring(0, loadedName.indexOf (' '));
			String ext    = loadedName.substring (loadedName.lastIndexOf(' ')+1);
			return prefix + '.' + ext;
		}
		
		else
			return loadedName;
	}
	
	// convert a pathname to an FSYS loaded name
	public static String convertToLoadedName (String pathName) {
		
		if (pathName.length() > 12)
			return null;
		
		String file = pathName.substring(0, pathName.indexOf('.'));
		String ext  = pathName.substring(pathName.indexOf('.')+1);
		
		while (file.length() < 8)
			file += ' ';
		
		while (ext.length () < 3)
			ext += ' ';
		
		return file.toUpperCase() + ext.toUpperCase();
	}
	
	// public static int usedClusterCount (DiskController toRead): get the number of used clusters
	public static int usedClusterCount (DiskController toRead) {
		
		// toyfat v1.0?
		if (checkForFileSys (toRead) == FILESYSTEM_FAT_V1) {
			
			int usedClusters = 0;
			short [] fatSector = toRead.readDiskSector(7);
		
			for (int i = 0; i < 512; i += 2) {
			
				// read the current cluster
				int currentCluster = (fatSector[i+1] << 8) | fatSector [i];
			
				// mark it if it is not an empty cluster
				if (currentCluster != EMPTY_CLUSTER && currentCluster != BAD_CLUSTER)
					usedClusters++;
			}
		
			return usedClusters;
		}
		
		// toyfat v2.0?
		else if (checkForFileSys (toRead) == FILESYSTEM_TOYFAT_V2) {
			
			int usedClusters = 0;
			int fatSectors = toRead.readDiskSector(0)[7];
			
			for (int i = 0; i < fatSectors; i++) {
				
				short [] fatSector = toRead.readDiskSector(i+16);
				
				for (int j = 0; j < 512; j += 2) {
					
					int currentCluster = (fatSector[j+1] << 8) | fatSector[j];
					
					if (currentCluster != EMPTY_CLUSTER && currentCluster != BAD_CLUSTER)
						usedClusters++;
				}
			}
			
			return usedClusters;
		}
		
		return 0;
	}
	
	// public static int freeClusterCount (DiskController toRead): Get the number of available clusters
	public static int freeClusterCount (DiskController toRead) {
		
		// toyfat v1.0?
		if (checkForFileSys (toRead) == FILESYSTEM_FAT_V1) {
			
			int freeClusters = 0;
			short [] fatSector = toRead.readDiskSector(7);
		
			for (int i = 0; i < 512; i += 2) {
			
				// read the current cluster
				int currentCluster = (fatSector[i+1] << 8) | fatSector [i];
			
				// mark it if it is not an empty cluster
				if (currentCluster == EMPTY_CLUSTER)
					freeClusters++;
			}
		
			return freeClusters;
		}
		
		// toyfat v2.0?
		else if (checkForFileSys (toRead) == FILESYSTEM_TOYFAT_V2) {
			
			int freeClusters = 0;
			int fatSectors = toRead.readDiskSector(0)[7];
			
			for (int i = 0; i < fatSectors; i++) {
				
				short [] fatSector = toRead.readDiskSector(i+16);
				
				for (int j = 0; j < 512; j += 2) {
					
					int currentCluster = (fatSector[j+1] << 8) | fatSector[j];
					
					if (currentCluster == EMPTY_CLUSTER)
						freeClusters++;
				}
			}
			
			return freeClusters;
		}
		
		return 0;
	}
	
	// public static int freeClusterCount (DiskController toRead): Get the number of available clusters
	public static int badClusterCount (DiskController toRead) {
		
		// toyfat v1.0?
		if (checkForFileSys (toRead) == FILESYSTEM_FAT_V1) {
			
			int badClusters = 0;
			short [] fatSector = toRead.readDiskSector(7);
		
			for (int i = 0; i < 512; i += 2) {
			
				// read the current cluster
				int currentCluster = (fatSector[i+1] << 8) | fatSector [i];
			
				// mark it if it is not an empty cluster
				if (currentCluster == BAD_CLUSTER)
					badClusters++;
			}
		
			return badClusters;
		}
		
		// toyfat v2.0?
		else if (checkForFileSys (toRead) == FILESYSTEM_TOYFAT_V2) {
			
			int badClusters = 0;
			int fatSectors = toRead.readDiskSector(0)[7];
			
			for (int i = 0; i < fatSectors; i++) {
				
				short [] fatSector = toRead.readDiskSector(i+16);
				
				for (int j = 0; j < 512; j += 2) {
					
					int currentCluster = (fatSector[j+1] << 8) | fatSector[j];
					
					if (currentCluster == BAD_CLUSTER)
						badClusters++;
				}
			}
			
			return badClusters;
		}
		
		return 0;
	}
}