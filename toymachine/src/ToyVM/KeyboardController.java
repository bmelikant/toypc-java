package ToyVM;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import java.util.Queue;

// class KeyboardController: Define a keyboard controller for this machine
public class KeyboardController implements KeyListener, IO_Device {
	
	// interrupts
	private static final short KEYPRESS_INTERRUPT = 0x09;
	
	// constants
	private static final String vendorId = "ToyKeys 1.0";
	private static final short NO_OPERATION  = 0x00;
	private static final short GET_REVISION  = 0x01;
	private static final short CLEAR_QUEUE   = 0x02;
	private static final short REQUEST_KEY   = 0x05;
	
	private static final short KEY_WAITING = 0x00;
	private static final short KEY_READY   = 0x01;
	
	// member variables
	private Queue<Short> typedKeys;
	private ToyProcessor referenceMachine;
	private short controlRegister;
	private short dataRegisterLo, dataRegisterHi;
	private short start;
	
	// constructor method
	public KeyboardController (ToyProcessor ref, short portStart) {
	
		// set up pointers to the registers
		typedKeys = new LinkedList<Short>();
		referenceMachine = ref;
		controlRegister = NO_OPERATION;
		dataRegisterLo = 0x00;
		dataRegisterHi = KEY_WAITING;
		start = portStart;
	}
	
	// keyTyped captures input data into the Key queue
	public void keyTyped (KeyEvent e) {
		
		// add the key typed to the queue
		if (e.getKeyChar() == '\n' || e.getKeyChar() == '\r')
			typedKeys.add ((short) 10);
		else
			typedKeys.add((short) e.getKeyChar());
		
		// fire a keypress interrupt
		referenceMachine.interrupt(KEYPRESS_INTERRUPT);
	}
	
	// public short getNextKey (): returns the next key from the queue
	public short getNextKey () {
		
		// return the next item from the queue!
		if (typedKeys.peek() == null)
			return 0xff;
		
		return typedKeys.remove();
	}
	
	// unused KeyListener methods
	public void keyPressed  (KeyEvent e) {}
	public void keyReleased (KeyEvent e) {}

	public void update () {
				
		// did someone request the revision string?
		if (controlRegister == GET_REVISION) {
			
			// store the revision string into memory at the location
			// given in the data registers
			
			int storageLoc = (dataRegisterHi << 8) | dataRegisterLo;
			dataRegisterHi = KEY_WAITING;
			
			for (int i = 0; i < vendorId.length(); i++)
				referenceMachine.storeMemory((short) vendorId.charAt(i), i+storageLoc);
			
			// store a successful result at the location
			dataRegisterHi = KEY_READY;
			controlRegister = NO_OPERATION;
			System.out.println("Got revision string: storageLoc=" + storageLoc);
		}
		
		// did someone request to clear the key queue?
		else if (controlRegister == CLEAR_QUEUE) {
			
			// just clear the key queue
			dataRegisterHi = KEY_WAITING;
			typedKeys.clear();
			dataRegisterHi = KEY_READY;
			controlRegister = NO_OPERATION;
		}
		
		else if (controlRegister == REQUEST_KEY) {
					
			// the key value goes in the low data register
			if (typedKeys.peek() == null) {
				
				dataRegisterLo = 0xff;
				dataRegisterHi = KEY_WAITING;
			}
				
			else {
					
				dataRegisterLo = typedKeys.remove ();
				dataRegisterHi = KEY_READY;
				controlRegister = NO_OPERATION;
			}
		}
	}
	
	public short dev_in (short port) {
		
		// control port?
		if ((port-start) == 0x00)
			return controlRegister;
		
		// data one?
		else if ((port-start) == 0x01)
			return dataRegisterLo;
		
		// data two?
		else if ((port-start) == 0x02)
			return dataRegisterHi;

		return 0;
	}

	public void dev_out (short port, short data) {
		
		// control port?
		if ((port-start) == 0x00) { 
		
			// set up the control register
			controlRegister = data;
		}
		
		else if ((port-start) == 0x01)
			dataRegisterLo = data;
		else if ((port-start) == 0x02)
			dataRegisterHi = data;
	}
}
