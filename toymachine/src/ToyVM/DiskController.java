package ToyVM;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

public class DiskController implements IO_Device, Runnable {

	// port numbers
	private static final short DATA_PORT   = 0x01;
	private static final short CMD_PORT    = 0x02;
	private static final short STATUS_PORT = 0x03;
	
	// control register one commands
	private static final short NO_OPERATION      = 0x00;
	private static final short GET_REVISION      = 0x01;

	private static final short SET_DATA_ONE_LO   = 0x02;
	private static final short SET_DATA_ONE_HI   = 0x03;
	private static final short SET_DATA_TWO_LO   = 0x04;
	private static final short SET_DATA_TWO_HI   = 0x05;
	
	private static final short READ_DISK_SECTOR  = 0x06;
	private static final short WRITE_DISK_SECTOR = 0x07;
	
	// status register constants
	private static final short DISK_IDLE = 0x01;
	private static final short DISK_BUSY = 0x02;
	
	// vendor ID
	private static final String vendorID = "ToyDisk 2.0";
	
	private ToyProcessor refMachine;
	private File diskFile;
	private short dataPort, commandPort, statusPort;
	private short dataOneLo, dataOneHi, dataTwoLo, dataTwoHi;
	private short start;
	
	public DiskController (ToyProcessor myProcessor, short portStart) {
		
		refMachine = myProcessor;
		start = portStart;
		dataPort = dataOneLo = dataOneHi = dataTwoLo = dataTwoHi = 0x00;
		commandPort = NO_OPERATION;
		statusPort = DISK_IDLE;
	}
	
	// public void setRefMachine () Setup the reference machine for this controller
	public void setRefMachine (ToyProcessor ref) { refMachine = ref; }
	public void setDiskFile (File toSet) { diskFile = toSet; }
	
	// public void storeDiskSector (): Store a 512-byte sector to the disk
	public void storeDiskSector (int number, short [] data) {
		
		// ensure that the data will fit into one sector (512 'bytes')
		if (data.length != 512) {
					
			System.out.println ("Data too large for sector, returning...");
			return;
		}
				
		// next thing, set up the byte channels for output
		ByteBuffer out = ByteBuffer.allocate(data.length*2);
		ShortBuffer outdata = out.asShortBuffer();
				
		outdata.put (data);
				
		try {
					
			RandomAccessFile myFile = new RandomAccessFile (diskFile, "rw");
			FileChannel diskOut = myFile.getChannel();
					
			while (out.hasRemaining())
				diskOut.write (out);
					
			diskOut.close ();
			myFile.close ();
					
		} catch (Exception e) {
					
			System.out.println ("Error writing sector block: " + e.toString());
			e.printStackTrace();
		}
	}
	
	// public short [] readDiskSector (int number): Read a 512-byte sector from the disk
	public short [] readDiskSector (int number) {
		
		short [] sectorContents = new short [512];
		ByteBuffer receiverBuf = ByteBuffer.allocate (1024);
		ShortBuffer contentsBuf = receiverBuf.asShortBuffer();
		
		try {
			
			RandomAccessFile dskFile = new RandomAccessFile (diskFile, "r");
			FileChannel sectorIn = dskFile.getChannel();
			
			sectorIn.position(number*512);
			sectorIn.read (receiverBuf);
			
			contentsBuf.rewind ();
			int i = 0;
			
			while (contentsBuf.hasRemaining()) {
				
				sectorContents[i++] = contentsBuf.get ();
			}
			
			sectorIn.close ();
			dskFile.close ();
			
		} catch (Exception e) {
			
			System.out.println ("Error reading sector: " + e.toString ());
			e.printStackTrace();
			return null;
		}
		
		return sectorContents;
	}

	public short dev_in (short port) {
		
		if ((port-start) == CMD_PORT)
			return commandPort;
		else if ((port-start) == DATA_PORT)
			return dataPort;
		else if ((port-start) == STATUS_PORT)
			return statusPort;
		
		return -1;
	}

	public void dev_out (short port, short data) {
	
		if (statusPort == DISK_IDLE) {
		
			// process command port data
			if ((port-start) == CMD_PORT) {
			
				commandPort = data;
			
				// are we setting up the data locations?
				if (commandPort == GET_REVISION) {
					
					int memAddr = (dataTwoHi << 8) | dataTwoLo;
					refMachine.storeMemoryString (vendorID, memAddr);
					
				} else if (commandPort == SET_DATA_ONE_LO) {
					
					dataOneLo = dataPort;
					
				} else if (commandPort == SET_DATA_ONE_HI) {
					
					dataOneHi = dataPort;
					
				} else if (commandPort == SET_DATA_TWO_LO) {
					
					dataTwoLo = dataPort;
					
				} else if (commandPort == SET_DATA_TWO_HI) {
					
					dataTwoHi = dataPort;
					
				} else if (commandPort == READ_DISK_SECTOR || commandPort == WRITE_DISK_SECTOR) {
				
					// run the thread that will process the disk operation
					Thread diskThread = new Thread (this);
					diskThread.start ();
				}
			}
		
			else if ((port-start) == DATA_PORT)
				dataPort = data;
		}
	}

	// either load a sector from or store a sector to disk, depending on command register
	public void run() {
		
		// make the disk unavailable during this operation.
		// Commands will be ignored while in DISK_BUSY mode
		statusPort = DISK_BUSY;
		
		// get the memory address to transfer sector to/from
		// and the sector number
		int memAddr = (dataTwoHi << 8) | dataTwoLo;
		int sector = (dataOneHi << 8) | dataOneLo;
		
		if (commandPort == READ_DISK_SECTOR) {
			
			short [] sectorContents = readDiskSector (sector);
			refMachine.storeMemoryRegion(sectorContents, memAddr);
		}
		
		else if (commandPort == WRITE_DISK_SECTOR) {
			
			short [] sectorContents = refMachine.getMemoryRegion(memAddr, memAddr+512);
			storeDiskSector(sector, sectorContents);
		}
		
		// now that we are done we want to re-enable the disk
		commandPort = NO_OPERATION;
		statusPort = DISK_IDLE;
	}
}