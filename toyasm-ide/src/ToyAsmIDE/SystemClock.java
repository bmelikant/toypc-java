package ToyAsmIDE;
import java.util.Calendar;
import java.util.GregorianCalendar;

// class SystemClock: Define a system clock for this machine
public class SystemClock implements IO_Device {
	
	// system timer interrupt
	private static final short TIMER_INTERRUPT = 0x05;
	
	private static final short GET_TSC    = 0x01;
	private static final short GET_HOUR   = 0x02;
	private static final short GET_MINUTE = 0x03;
	private static final short GET_SECOND = 0x04;
	
	// member variables
	ToyProcessor refMachine;
	private short hourRegister, minuteRegister, secondRegister;
	private short tscRegister;
	private int start;
	
	private short ctrlRegister, dataRegister;
	
	// constructor
	public SystemClock (ToyProcessor ref, int portStart) {
		
		refMachine = ref;
		start = portStart;
		
		// get the current time into the registers
		Calendar currentTime = new GregorianCalendar ();
		
		hourRegister   = (short) currentTime.get(Calendar.HOUR);
		minuteRegister = (short) currentTime.get(Calendar.MINUTE); 
		secondRegister = (short) currentTime.get(Calendar.SECOND);
		tscRegister = 0x00;
		
		ctrlRegister = dataRegister = 0x00;
	}
	
	// public void updateTsc (): Increment the time stamp counter
	public void updateTsc () {
		
		tscRegister++;
		refMachine.interrupt (TIMER_INTERRUPT);
	}
	
	// public void updateSystemClock (): Update the time in the clock
	public void update () {
		
		Calendar currentTime = new GregorianCalendar ();
		
		hourRegister   = (short) currentTime.get(Calendar.HOUR);
		minuteRegister = (short) currentTime.get(Calendar.MINUTE); 
		secondRegister = (short) currentTime.get(Calendar.SECOND);
	}

	public short dev_in(short port) {
		
		if ((port-start) == 0x00)
			return ctrlRegister;
		else if ((port-start) == 0x01)
			return dataRegister;
		
		return 0;
	}

	public void dev_out(short port, short data) {
		
		if ((port-start) == 0x00) {
			
			ctrlRegister = data;
			
			if (ctrlRegister == GET_TSC)
				dataRegister = tscRegister;
			else if (ctrlRegister == GET_HOUR)
				dataRegister = hourRegister;
			else if (ctrlRegister == GET_MINUTE)
				dataRegister = minuteRegister;
			else if (ctrlRegister == GET_SECOND)
				dataRegister = secondRegister;
		}
		
		ctrlRegister = 0x00;
	}
}
