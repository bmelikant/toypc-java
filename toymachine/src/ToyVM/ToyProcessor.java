package ToyVM;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Stack;

/* ToyProcessor.java: Define a processor for my Toy Machine */

public class ToyProcessor implements Serializable {
	
	// cpuid string
	private static final long serialVersionUID = 1L;
	public static final String cpuidString = "ToyProcessor r2 v1.1";
	
	// opcode list for normal instructions
	public static final short NOP   = 0x60;		// implemented
	public static final short STO   = 0x01;		// implemented
	public static final short LOD   = 0x02;		// implemented
	public static final short ADD   = 0x03;		// implemented
	public static final short SUB   = 0x04;		// implemented
	public static final short MUL   = 0x05;		// implemented
	public static final short DIV   = 0x06;		// implemented
	public static final short MOD   = 0x07;		// implemented
	public static final short PSH   = 0x08;		// implemented
	public static final short POP   = 0x09;		// implemented
	public static final short SHL   = 0x0A;		// implemented
	public static final short SHR   = 0x0B;		// implemented
	public static final short CALL  = 0x0E;		// implemented
	public static final short JMP   = 0x0F;		// implemented
	public static final short JMPDI = 0x46;		
	public static final short HLT   = 0x10;		// implemented
	public static final short AND   = 0x11;		// implemented
	public static final short OR    = 0x12;		// implemented
	public static final short XOR   = 0x13;		// implemented
	public static final short CMP   = 0x14;		// implemented
	public static final short CPUID = 0x15;		// implemented
	public static final short INT   = 0x16;		// implemented
	public static final short IN    = 0x55;
	public static final short OUT   = 0x56;
	
	// inc and dec instructions (one byte)
	public static final short INCA = 0x30;		// implemented
	public static final short INCB = 0x31;		// implemented
	public static final short INCC = 0x32;		// implemented
	public static final short INCX = 0x33;		// implemented
	public static final short INCY = 0x34;		// implemented
	public static final short INCS = 0x35;		// implemented
	public static final short INCD = 0x36;		// implemented
	
	public static final short DECA = 0x37;		// implemented
	public static final short DECB = 0x38;		// implemented
	public static final short DECC = 0x39;		// implemented
	public static final short DECX = 0x3A;		// implemented
	public static final short DECY = 0x3B;		// implemented
	public static final short DECS = 0x3C;		// implemented
	public static final short DECD = 0x3D;		// implemented
	
	// one-byte flag instructions
	public static final short PSHF = 0x16;		// implemented
	public static final short POPF = 0x17;		// implemented
	public static final short PSHALL = 0x50;	// implemented
	public static final short POPALL = 0x51;	// implemented
	
	public static final short STFZ = 0x18;		// implemented
	public static final short STFA = 0x19;		// implemented
	public static final short STFC = 0x20;		// implemented
	public static final short STFE = 0x21;		// implemented
	public static final short STFG = 0x22;		// implemented
	public static final short STFL = 0x23;
	public static final short STFD = 0x24;
	public static final short STFO = 0x25;
	public static final short STFI = 0x26;
	
	public static final short CLFZ = 0x27;
	public static final short CLFA = 0x28;
	public static final short CLFC = 0x29;
	public static final short CLFE = 0x2A;
	public static final short CLFG = 0x2B;
	public static final short CLFL = 0x2C;
	public static final short CLFD = 0x48;
	public static final short CLFO = 0x49;
	public static final short CLFI = 0x4A;
	
	public static final short LDSTR = 0x40;		// implemented
	public static final short STSTR = 0x41;		// implemented
	public static final short RET   = 0x45;		// implemented
	public static final short IRET  = 0x46;		// implemented

	// opcode jmp modifiers / flag access
	public static final short Z_MOD  = 0x0;
	public static final short A_MOD  = 0x1;
	public static final short C_MOD  = 0x2;
	public static final short E_MOD  = 0x3;
	public static final short G_MOD  = 0x4;
	public static final short L_MOD  = 0x5;
	public static final short D_MOD  = 0x6;
	public static final short O_MOD  = 0x7;
	public static final short I_MOD  = 0x8;
	
	// translation constants
	
	// register mod constants
	public static final short constRegA   = 0x01;
	public static final short constRegB   = 0x02;
	public static final short constRegC   = 0x03;
	public static final short constRegX   = 0x04;
	public static final short constRegY   = 0x05;
	public static final short constRegS   = 0x06;
	public static final short constRegD   = 0x07;
	public static final short constImm8   = 0x08;
	public static final short constImm16  = 0x09;
	public static final short constMemory = 0x0A;
	public static final short constRegF   = 0x0B;
	public static final short constRegIP  = 0x0C;
		
	// processor internal interrupts
	public static final short DIVIDE_BY_ZERO       = 0x01;
	public static final short MEMORY_OUT_OF_BOUNDS = 0x02;
	public static final short DOUBLE_FAULT         = 0x03;
	public static final short ILLEGAL_INSTRUCTION  = 0x04;
	public static final short PORT_RANGE_FAULT     = 0x06;
	public static final short NO_DEVICE_AT_PORT    = 0x07;
	
	// registers
	private int regX, regY, regC, regIP, regS, regD;
	private short regA, regB;
	private BitSet regF;
	
	// processor control variables
	private short opcode, argOne, argTwo, loVal, hiVal;
	
	// memory array
	private short [] memory;
	private IO_Device [] io_map;
	private Stack<Short> stack;
	private boolean debugMode;
	
	// interrupt controls
	private int currentInt;
	private boolean interrupted;
	
	// constructor method
	public ToyProcessor (int memSize, boolean debug) {
		
		// set up the registers
		regX = regY = regC = regIP = regS = regD = 0x0;
		regA = regB = 0x0;
		regF = new BitSet (10);
		
		// set up the memory
		memory = new short [memSize];
		io_map = new IO_Device [1024];
		stack  = new Stack<Short>();
		debugMode = debug;
		
		// set up the interrupt mechanism
		currentInt = 0;
		interrupted = false;
		regF.clear(I_MOD);
		
		opcode = argOne = argTwo = loVal = hiVal = 0x0;
	}
	
	// turn debug mode on and off
	public void turnDebugModeOn  () { debugMode = true; }
	public void turnDebugModeOff () { debugMode = false; }
	
	// public void pushStack (short value): Push a value onto the stack
	// inputs: value-the value to push
	// returns: none
	public void pushStack (short value) {
		
		stack.push (value);
	}
	
	// public short popStack (): Pop a value from the stack
	// inputs: none
	// returns: the next value from the top of the stack
	public short popStack () {
	
		return stack.pop ();
	}
	
	// public void storeMemory (short val, int loc): modify a memory address, one byte
	// Inputs: val-value to store, loc-location to store it in
	// Returns: None
	public void storeMemory (short val, int loc) {
		
		if (val > 0xff)
			val = (short) (val & ~0xff00);
		
		if (loc < memory.length)
			memory[loc] = val;
	}

	// public void storeMemory (int val, int loc): modify a memory address, two bytes
	// Inputs: val-value to store, loc-place to store it
	// Returns: None
	public void storeMemory (int val, int loc) {
		
		if (loc < memory.length && (loc+1) < memory.length) {
			
			memory[loc++] = (short) (val & ~0xff00);
			memory[loc]   = (short) (val >> 8);
		}
	}
	
	// public void storeMemoryRegion (short [] data, int loc): store an array of bytes to memory
	public void storeMemoryRegion (short [] data, int loc) {
		
		for (int i = 0; i < data.length; i++) {
			
			if (i+loc < memory.length)
				data[i] = memory[i+loc];
			else
				break;
		}
	}
	
	public void storeMemoryString (String data, int loc) {
		
		for (int i = 0; i < data.length(); i++) {
			
			if (i+loc < memory.length)
				memory[i+loc] = (short) data.charAt(i);
		}
	}
	
	public void reservePort (IO_Device toUse, short port) {
		
		if (port < io_map.length)
			io_map[port] = toUse;
	}
	
	public void reservePortRange (IO_Device toUse, short port, short count) {
	
		for (int i = 0; i < count; i++) {
			
			if (i+port < io_map.length)
				io_map[i+port] = toUse;
			else
				break;
		}
	}
	
	// public short getMemory (int loc): get a memory address, one byte
	// Inputs: loc-location to fetch
	// Returns: None
	public short getMemory (int loc) {
		
		if (loc < memory.length)
			return memory[loc];
		
		return -1;
	}
	
	// public void setRegister (int val, short reg): Set up a register
	// Inputs: val-value to set, reg-register to use
	// Returns: None
	public void setRegister (int val, short reg) {
		
		// if the register is 8 bit and the value is under 0x100 set it up
		if ((reg == constRegA || reg == constRegB)) {
			
			if (val > 0xff)
				val = (short) (val & ~0xff00);
			
			// determine the register to store into
			if (reg == constRegA)
				regA = (short) val;
			else if (reg == constRegB)
				regB = (short) val;
			
			// now we have to set up register C since it unifies A and B
			regC  = (regB << 8) | regA;
		}
		
		// if the register is 16 bit and the value is under 0x10000 set it up
		else if ((reg == constRegC || reg == constRegX || reg == constRegY || reg == constRegIP || reg == constRegS || reg == constRegD) && val < 0x10000) {
			
			// determine the register to store into
			if (reg == constRegX)
				regX = val;
			else if (reg == constRegY)
				regY = val;
			else if (reg == constRegS)
				regS = val;
			else if (reg == constRegD)
				regD = val;
			else if (reg == constRegC) {
				
				// set up C as well as A and B
				regC = val;
				regA = (short) (regC & ~0xff00);
				regB = (short) (regC >> 8);
			}
			
			else if (reg == constRegIP)
				regIP = val;
		}
		
		// if the register is flags we have to figure out what to do with the bits!
		else if (reg == constRegF && val < 0x100) {
			
			regF.clear ();
			
			// convert the value to a bitset
			int idx = 0;
			
			while (val != 0) {
				
				if ((val % 2) != 0)
					regF.set(idx);
				
				++idx;
				val >>>= 1;
			}
		}
	}
	
	// public void setPtrRegister (int value, short register): Set the indicated ptr register
	// Inputs: value-the value to store, register-register to use
	// Returns: None
	public void setPtrRegister (int value, short register) {
	
		if (register == constRegS && value < memory.length)
			regS = value;
		else if (register == constRegD && value < memory.length)
			regD = value;
	}
	
	public int getPtrRegister (short register) {
		
		if (register == constRegS)
			return regS;
		
		return regD;
	}
	
	// public int getRegister (short register): Get the indicated register
	// Inputs: register-the register to get
	// Returns: the register value
	public int getRegister (short register) {
		
		if (register == constRegA)
			return regA;
		else if (register == constRegB)
			return regB;
		else if (register == constRegC)
			return regC;
		else if (register == constRegX)
			return regX;
		else if (register == constRegY)
			return regY;
		else if (register == constRegIP)
			return regIP;
		else if (register == constRegS)
			return regS;
		else if (register == constRegD)
			return regD;
		else if (register == constRegF) {
			
		    int bitInteger = 0;
		    
		    for(int i = 0 ; i < 32; i++)
		    	
		        if (regF.get(i))
		            bitInteger |= (1 << i);
		    return bitInteger;
		}
		
		return -1;
	}
	
	// public short [] getMemoryRegion (int startIdx, int endIdx): get a region of memory
	// Inputs: startIdx-where to start, endIdx-where to end
	// Returns: the correct memory region
	public short [] getMemoryRegion (int startIdx, int endIdx) {
		
		if (startIdx < memory.length && endIdx < memory.length && startIdx < endIdx) {	
			
			// allocate the return array
			short [] memRegion = new short [(endIdx+1)-startIdx];
			
			// copy the correct region
			for (int i = 0; i <= (endIdx - startIdx); i++)
				memRegion[i] = memory[i+startIdx];
			
			// return the region
			return memRegion;
		}
		
		return null;
	}
	
	// public void interrupt (short): Interrupt the processor with the given interrupt number, hardware interrupt
	public void interrupt (short intno) {
	
		currentInt = intno;
		interrupted = true;
	}
	
	// public void fetchOpcode (): Fetch the next opcode and if necessary modrm and values
	// Inputs: None
	// Returns: None
	public void fetchOpcode () {
		
		// reset variables
		opcode = argOne = argTwo = 0;
		loVal = hiVal = 0;
		int startIP = regIP;
		
		// now that the registers are reset the first thing we need to do
		// is check for interrupted state. If an interrupt has occurred
		// process it
		if (interrupted == true && regF.get(I_MOD) == true) {
			
			opcode = INT;
			loVal = (short) currentInt;
			interrupted = false;
			return;
		}
		
		// grab the opcode and buffer it
		if (regIP < memory.length)
			opcode = memory[regIP++];
		
		else {
			
			regIP = 0x0;
			opcode = memory[regIP++];
		}
		
		// now see if we have to fetch a modrm byte
		if ((opcode >= STO && opcode <= SHR ) || (opcode >= AND && opcode <= CMP)) {
			
			// sort out the mod, argOne, and argTwo bits
			argOne = (short) (memory[regIP] >> 4);
			argTwo = (short) (memory[regIP] & ~0xf0);
			
			regIP++;
			
			// if we have a memory or immediate argument we have to fetch
			// the next part
			if (isMemoryRM (argTwo)) {
				
				loVal = memory[regIP++];
				hiVal = memory[regIP++];
			}
			
			else if (isImmediateEightRM (argTwo)) {
				
				loVal = memory[regIP++];
			}
			
			else if (isImmediate16RM (argTwo)) {
				
				loVal = memory[regIP++];
				hiVal = memory[regIP++];
			}
		}
		
		// jmps and calls have to have a memory value every time!
		else if (isJump (opcode) || opcode == CALL) {
			
			loVal = memory[regIP++];
			hiVal = memory[regIP++];
		}
		
		if (debugMode) {
			
			System.out.println ("Fetched (IP - " + String.format ("%04X", startIP) + "):");
			System.out.println ("opcode - " + String.format ("%02X", opcode) + " modrm - " + String.format ("%02X", ((argOne << 4) | argTwo)));
			System.out.println ("loval- " + String.format ("%02X", loVal) + " hival - " + String.format ("%02X", hiVal));
		}
	}
	
	// public void processOpcode (): Process the fetched opcode
	// Inputs: None
	// Returns: None
	public boolean processOpcode () {
	
		// NOPs just eat clock cycles
		if (opcode == NOP)
			return true;
		
		// STO stores from a register to a location
		else if (opcode == STO) {
			
			// register-register STO instruction?
			if (isRegisterRM (argOne) && isRegisterRM (argTwo))
				setRegister (getRegister (argOne), argTwo);
			
			// register-memory STO instruction?
			else if (isRegisterRM (argOne) && isMemoryRM (argTwo)) {
				
				int idx = (hiVal << 8) | loVal;
				
				if (argOne == constRegC || argOne == constRegX || argOne == constRegY) {
					
					memory[idx]   = (short) (getRegister (argOne) & ~0xff00);
					memory[idx+1] = (short) (getRegister (argOne) >> 8);
				}
				
				else if (argOne == constRegA || argOne == constRegB || argOne == constRegF)
					memory[idx] = (short) getRegister (argOne);
			}
			
			else {
				
				interrupt (ILLEGAL_INSTRUCTION);
				return true;
			}
		}
		
		// LOD loads from a location to a register
		else if (opcode == LOD || opcode == ADD || opcode == SUB || opcode == MUL || opcode == DIV || opcode == MOD || 
				opcode == SHL || opcode == SHR || opcode == AND || opcode == OR || opcode == XOR) {
		
			// register-register instruction?
			if (isRegisterRM (argOne) && isRegisterRM (argTwo)) {
				
				if (opcode == LOD)
					setRegister (getRegister (argTwo), argOne);
				else if (opcode == ADD)
					setRegister ((getRegister (argTwo)+getRegister(argOne)), argOne);
				else if (opcode == SUB)
					setRegister ((getRegister(argOne)-getRegister(argTwo)), argOne);
				else if (opcode == MUL)
					setRegister ((getRegister(argOne)*getRegister(argTwo)), argOne);
				else if (opcode == DIV) {
					
					// catch divide by zero and print a processor error!
					try {
						
						setRegister ((getRegister(argOne)/getRegister(argTwo)), argOne);
					} catch (ArithmeticException e) {
						
						interrupt (DIVIDE_BY_ZERO);
						return true;
					}
				}
				
				else if (opcode == MOD)
					setRegister ((getRegister(argOne)%getRegister(argTwo)), argOne);
				else if (opcode == SHL)
					setRegister ((getRegister(argOne) << getRegister(argTwo)), argOne);
				else if (opcode == SHR)
					setRegister ((getRegister(argOne) >> getRegister(argTwo)), argOne);
				
				else if (opcode == AND)
					setRegister ((getRegister(argOne) & getRegister(argTwo)), argOne);
	
				else if (opcode == OR)
					setRegister ((getRegister(argOne) | getRegister(argTwo)), argOne);
				
				else if (opcode == XOR)
					setRegister ((getRegister(argOne) ^ getRegister(argTwo)), argOne);
			}
			
			// register-memory instruction?
			else if (isRegisterRM (argOne) && isMemoryRM (argTwo)) {
				
				int idx = (hiVal << 8) | loVal;
				int temp = 0;
				
				if (argOne == constRegC || argOne == constRegX || argOne == constRegY || argOne == constRegS || argOne == constRegD)
					temp  = (memory[idx+1] << 8) | memory[idx];
				
				else if (argOne == constRegA || argOne == constRegB || argOne == constRegF)
					temp = memory[idx];
				
				if (opcode == LOD)
					setRegister (temp, argOne);
				else if (opcode == ADD)
					setRegister ((getRegister(argOne)+temp), argOne);
				else if (opcode == SUB)
					setRegister ((getRegister(argOne)-temp), argOne);
				else if (opcode == MUL)
					setRegister ((getRegister(argOne)*temp), argOne);
				else if (opcode == DIV) {
						
					try {
							
						setRegister ((getRegister(argOne)/temp), argOne);
	
					} catch (ArithmeticException e) {
							
						interrupt (DIVIDE_BY_ZERO);
						return true;
					}
				}
					
				else if (opcode == MOD)
					setRegister ((getRegister(argOne)%temp), argOne);
				else if (opcode == SHL)
					setRegister ((getRegister(argOne) << temp), argOne);
				else if (opcode == SHR)
					setRegister ((getRegister(argOne) >> temp), argOne);
				else if (opcode == AND)
					setRegister ((getRegister(argOne) & temp), argOne);
				else if (opcode == OR)
					setRegister ((getRegister(argOne) | temp), argOne);
				else if (opcode == XOR)
					setRegister ((getRegister(argOne) ^ temp), argOne);

			}
			
			// register-immediate instruction?
			else if (isRegisterRM (argOne) && (isImmediateEightRM (argTwo) || isImmediate16RM (argTwo))) {
								
				int temp = 0;
				
				if (isImmediate16RM (argTwo))
					temp  = (hiVal << 8) | loVal;
				
				else if (isImmediateEightRM (argTwo))
					temp = loVal;
				
				if (opcode == LOD)
					setRegister (temp, argOne);
				else if (opcode == ADD)
					setRegister ((getRegister(argOne)+temp), argOne);
				else if (opcode == SUB)
					setRegister ((getRegister(argOne)-temp), argOne);
				else if (opcode == MUL)
					setRegister ((getRegister(argOne)*temp), argOne);
				else if (opcode == DIV) {
						
					try {
							
						setRegister ((getRegister(argOne)/temp), argOne);
	
					} catch (Exception e) {
							
						interrupt (DIVIDE_BY_ZERO);
						return true;
					}
				}
					
				else if (opcode == MOD)
					setRegister ((getRegister(argOne)%temp), argOne);
				else if (opcode == SHL)
					setRegister ((getRegister(argOne) << temp), argOne);
				else if (opcode == SHR)
					setRegister ((getRegister(argOne) >> temp), argOne);
				else if (opcode == AND)
					setRegister ((getRegister(argOne) & temp), argOne);
				else if (opcode == OR)
					setRegister ((getRegister(argOne) | temp), argOne);
				else if (opcode == XOR)
					setRegister ((getRegister(argOne) ^ temp), argOne);
			}
			
			else {
				
				interrupt (ILLEGAL_INSTRUCTION);
				return true;
			}
			
			// if the end result was zero, set zero flag
			if (getRegister(argOne) == 0)
				regF.set (Z_MOD);
			
			// if the end result was greater than range of register, set overflow and carry flag
			if (((argOne == constRegA || argOne == constRegB) && getRegister(argOne) > 0xff) || getRegister (argOne) > 0x7fff) {
				
				regF.set (O_MOD);
				regF.set (C_MOD);
				
				if (argOne == constRegA || argOne == constRegB)
					setRegister(getRegister(argOne)&~0xff00, argOne);
				else
					setRegister(getRegister(argOne)&~0xffff8000, argOne);
			}
		}
		
		// PSH instruction pushes a value onto the stack
		else if (opcode == PSH) {
			
			// is this a register PSH?
			if (isRegisterRM (argTwo))
				pushStack ((short) getRegister (argTwo));
			
			// is this an immediate PSH?
			else if (isImmediateEightRM (argTwo) || isImmediate16RM (argTwo)) {
				
				if (isImmediateEightRM (argTwo))
					pushStack (loVal);
				else
					pushStack ((short) ((hiVal << 8) | loVal));
			}
			
			// is this a memory PSH?
			else if (isMemoryRM (argTwo))	
				pushStack (memory[(hiVal << 8) | loVal]);
		}
		
		// POP instructions pop a value off the stack
		else if (opcode == POP) {
			
			// is this a register POP?
			if (isRegisterRM (argTwo))
				setRegister (popStack (), argTwo);
			
			// cannot pop immediates!
			else if (isImmediateEightRM (argTwo) || isImmediate16RM (argTwo)) {
				
				interrupt (ILLEGAL_INSTRUCTION);
				return true;
			}
			
			// is this a memory POP?
			else if (isMemoryRM (argTwo))
				memory[(hiVal << 8) | loVal] = popStack ();
		}
		
		// CMP instruction compares values, setting the equal flag if they are equal
		// If argOne is greater than argTwo, greater flag is set
		// If argOne is less than argTwo, less than flag is set
		else if (opcode == CMP) {
			
			// register-register comparison
			if (isRegisterRM (argOne) && isRegisterRM (argTwo)) {
				
				if (getRegister (argOne) == getRegister (argTwo))
					regF.set (E_MOD);
				else if (getRegister (argOne) > getRegister (argTwo))
					regF.set (G_MOD);
				else if (getRegister (argOne) < getRegister (argTwo))
					regF.set (L_MOD);
			}
			
			// register-immediate comparison
			else if (isRegisterRM (argOne) && (isImmediateEightRM (argTwo) || isImmediate16RM (argTwo))) {

				int immediate = 0;
				
				if (argOne == constRegA || argOne == constRegB || argOne == constRegF)
					immediate = loVal;
				
				else if (argOne == constRegX || argOne == constRegY || argOne == constRegC || argOne == constRegS || argOne == constRegD) {
				
					immediate = (hiVal << 8);
					immediate += loVal;
				}
				
				if (getRegister (argOne) == immediate)
					regF.set (E_MOD);
				else if (getRegister (argOne) > immediate)
					regF.set (G_MOD);
				else if (getRegister (argOne) < immediate)
					regF.set (L_MOD);
			}
			
			// register-memory comparison
			else if (isRegisterRM (argOne) && isMemoryRM (argTwo)) {
				
				int idx = ((hiVal << 8) | loVal);
				int comparison = 0;
				
				// have to check for an 8 or 16 bit operation
				if (argOne == constRegA || argOne == constRegB || argOne == constRegF)
					comparison = memory[idx];
				
				else if (argOne == constRegX || argOne == constRegY || argOne == constRegC || argOne == constRegS || argOne == constRegD)
					comparison = (memory[idx+1] << 8) | memory[idx];
				
				if (getRegister (argOne) == comparison)
					regF.set (E_MOD);
				else if (getRegister (argOne) > comparison)
					regF.set (G_MOD);
				else if (getRegister (argOne) < comparison)
					regF.set (L_MOD);
			}
			
			// found illegal combination
			else {
				
				interrupt (ILLEGAL_INSTRUCTION);
				return true;
			}
		}
		
		// CALL instruction transfers control
		else if (opcode == CALL) {
			
			// push the flags and the instruction pointer onto the stack
			// then transfer control
			pushStack ((short) regIP);			
			regIP = (hiVal << 8) | loVal;
			
			return true;
		}
		
		// RET instruction returns from a call. Make sure the stack is fixed before this
		// instruction or you will be dumped to a random point in memory!!!
		else if (opcode == RET) {
			
			regIP = popStack ();
			return true;
		}
		
		// JMP instruction transfers execution but has to be treated special
		else if (isJump (opcode)) {
			
			// all we have to do is transfer control if the
			// flag that is to be checked is set.
			short prefix = (short) (opcode >> 4);
			
			// if the flag to check is not what it is supposed
			// to be, return. Otherwise, transfer control
			if (prefix == 0x01 && regF.get(E_MOD) == false)
				return true;
			else if (prefix == 0x02 && regF.get(E_MOD) == true)
				return true;
			else if (prefix == 0x03 && regF.get(G_MOD) == false)
				return true;
			else if (prefix == 0x04 && regF.get(G_MOD) == true)
				return true;
			else if (prefix == 0x05 && regF.get(L_MOD) == false)
				return true;
			else if (prefix == 0x06 && regF.get(L_MOD) == true)
				return true;
			else if (prefix == 0x07 && regF.get(Z_MOD) == false)
				return true;
			else if (prefix == 0x08 && regF.get(Z_MOD) == true)
				return true;
			else if (prefix == 0x09 && regF.get(C_MOD) == false)
				return true;
			else if (prefix == 0x0A && regF.get(C_MOD) == true)
				return true;
			
			// now perform the jump
			regIP = (hiVal << 8) | loVal;
				
			if (prefix == 0x01 || prefix == 0x02)
				regF.clear (E_MOD);
			else if (prefix == 0x03 || prefix == 0x04)
				regF.clear (G_MOD);
			else if (prefix == 0x05 || prefix == 0x06)
				regF.clear (L_MOD);
			else if (prefix == 0x07 || prefix == 0x08)
				regF.clear (Z_MOD);
			else if (prefix == 0x09 || prefix == 0x0A)
				regF.clear (C_MOD);
			
			return true;
		}
		
		// CPUID stores the cpu's id string at the memory location in register d
		else if (opcode == CPUID) {
			
			// save the value of d on the stack
			pushStack ((short) regD);
			
			for (int i = 0; i < cpuidString.length(); i++)
				memory[regD++] = (short) cpuidString.charAt(i);
			
			regD = popStack ();
		}
		
		// LDSTR loads a byte from $regS into register A
		else if (opcode == LDSTR) {
			
			setRegister (memory[regS], constRegA);
			
			// if the direction flag is set, decrement. if clear, increment
			if (regF.get(D_MOD) == false)
				regS++;
			else if (regF.get(D_MOD) == true)
				regS--;
		}
		
		// STSTR stores a byte from register A into $regD
		else if (opcode == STSTR) {
			
			memory[regD] = (short) getRegister (constRegA);
			
			// if the direction flag is set, decrement. if clear, increment
			if (regF.get(D_MOD) == false)
				regD++;
			else
				regD--;
		}
		
		// INC instructions increment their given registers
		else if (opcode >= INCA && opcode <= INCD) {
			
			// sort the opcode, increment the correct register
			if (opcode == INCA) {
				
				setRegister ((getRegister (constRegA)+1), constRegA);
				
				if (getRegister (constRegA) > 0xff) {
					setRegister ((getRegister (constRegA)-1), constRegA);
					regF.set(C_MOD);
				}
			}
			else if (opcode == INCB) {
				
				setRegister ((getRegister (constRegB)+1), constRegB);
			
				if (getRegister (constRegB) > 0xff) {
					setRegister ((getRegister (constRegB)-1), constRegB);
					regF.set(C_MOD);
				}
			}
			
			else if (opcode == INCC){
				
				setRegister ((getRegister (constRegC)+1), constRegC);
			
				if (getRegister (constRegC) > 0x7fff) {
					setRegister ((getRegister (constRegC)-1), constRegC);
					regF.set(C_MOD);
				}
			}
				
			else if (opcode == INCX) {
				
				setRegister ((getRegister (constRegX)+1), constRegX);
			
				if (getRegister (constRegX) > 0x7fff) {
					setRegister ((getRegister (constRegX)-1), constRegX);
					regF.set(C_MOD);
				}
			}
				
			else if (opcode == INCY) {
				
				setRegister ((getRegister (constRegY)+1), constRegY);
			
				if (getRegister (constRegY) > 0x7fff) {
					setRegister ((getRegister (constRegY)-1), constRegY);
					regF.set(C_MOD);
				}
			}
			
			else if (opcode == INCS) {
				
				setRegister ((getRegister (constRegS)+1), constRegS);
			
				if (getRegister (constRegS) > 0x7fff) {
					setRegister ((getRegister (constRegS)-1), constRegS);
					regF.set(C_MOD);
				}
			}
			
			else if (opcode == INCD) {
				
				setRegister ((getRegister (constRegD)+1), constRegD);
			
				if (getRegister (constRegD) > 0x7fff) {
					setRegister ((getRegister (constRegD)-1), constRegD);
					regF.set(C_MOD);
				}
			}
		}
		
		// DEC instructions decrement registers
		else if (opcode >= DECA && opcode <= DECD) {
			
			// sort the opcode, decrement the correct register
			if (opcode == DECA) {
			
				setRegister ((getRegister (constRegA)-1), constRegA);
				if (getRegister (constRegA) < 0)
					setRegister (0x00, constRegA);
			}
			
			else if (opcode == DECB) {
					
				setRegister ((getRegister (constRegB)-1), constRegB);
				if (getRegister (constRegB) < 0)
					setRegister (0x00, constRegB);
			}
				
			else if (opcode == DECC) {
				
				setRegister ((getRegister (constRegC)-1), constRegC);
				if (getRegister (constRegC) < 0)
					setRegister (0x00, constRegC);
			}
			
			else if (opcode == DECX) {
				
				setRegister ((getRegister (constRegX)-1), constRegX);
				if (getRegister (constRegX) < 0)
					setRegister (0x00, constRegX);
			}
			
			else if (opcode == DECY) {
				
				setRegister ((getRegister (constRegY)-1), constRegY);
				if (getRegister (constRegY) < 0)
					setRegister (0x00, constRegY);
			}
			
			else if (opcode == DECS) {
				
				setRegister ((getRegister (constRegS)-1), constRegS);
				if (getRegister (constRegS) < 0)
					setRegister (0x00, constRegS);
			}
			
			else if (opcode == DECD) {
				
				setRegister ((getRegister (constRegD)-1), constRegD);
				if (getRegister (constRegD) < 0)
					setRegister (0x00, constRegD);
			}
		}
		
		// PSHF pushes flags onto stack
		else if (opcode == PSHF) {
			
			pushStack ((short) getRegister (constRegF));
		}
		
		// POPF pops flags from stack
		else if (opcode == POPF) {
			
			setRegister (popStack (), constRegF);
		}
		
		// PSHALL pushes all registers onto the stack
		else if (opcode == PSHALL) {
			
			pushStack ((short) getRegister (constRegA));
			pushStack ((short) getRegister (constRegB));
			pushStack ((short) getRegister (constRegX));
			pushStack ((short) getRegister (constRegY));
			pushStack ((short) getRegister (constRegS));
			pushStack ((short) getRegister (constRegD));
		}
		
		// POPALL removes all registers from the stack
		else if (opcode == POPALL) {
			
			setRegister (popStack (), constRegD);
			setRegister (popStack (), constRegS);
			setRegister (popStack (), constRegY);
			setRegister (popStack (), constRegX);
			setRegister (popStack (), constRegB);
			setRegister (popStack (), constRegA);
		}
		
		// if the opcode is a flag instruction set or clear the associated flag
		else if (opcode <= STFZ && opcode >= CLFO) {
			
			switch (opcode) {
			
			case STFZ:
				regF.set (Z_MOD);
				break;
			case STFA:
				regF.set (A_MOD);
				break;
			case STFC:
				regF.set (C_MOD);
				break;
			case STFE:
				regF.set (E_MOD);
				break;
			case STFG:
				regF.set (G_MOD);
				break;
			case STFL:
				regF.set (L_MOD);
				break;
			case STFD:
				regF.set (D_MOD);
				break;
			case STFO:
				regF.set (O_MOD);
				break;
			case STFI:
				regF.set (I_MOD);
				break;
				
			case CLFZ:
				regF.clear (Z_MOD);
				break;
			case CLFA:
				regF.clear (A_MOD);
				break;
			case CLFC:
				regF.clear (C_MOD);
				break;
			case CLFE:
				regF.clear (E_MOD);
				break;
			case CLFG:
				regF.clear (G_MOD);
				break;
			case CLFL:
				regF.clear (L_MOD);
				break;
			case CLFD:
				regF.clear (D_MOD);
				break;
			case CLFO:
				regF.clear (O_MOD);
				break;
			case CLFI:
				regF.clear (I_MOD);
				break;
			}
		}
		
		// if we have an interrupt, pick the vector and jump to that memory location
		else if (opcode == INT) {
		
			if (this.regF.get(I_MOD) == true) {
				
				// find the appropriate memory location
				short intLoc = (short) (currentInt*2);
				int memVect = getMemory (intLoc);
			
				pushStack ((short) getRegister (constRegF));
				pushStack ((short) getRegister(constRegIP));
			
				setRegister(memVect, constRegIP);
				regF.clear (I_MOD);
			}
		}
		
		// if we have an IRET we want to jump back to the old position
		else if (opcode == IRET) {
			
			setRegister (popStack (), constRegIP);
			setRegister (popStack (), constRegF);
			regF.set(I_MOD);
		}
		
		// if we have an IN instruction, get data from port number regX into regB
		else if (opcode == IN) {
			
			int port = getRegister (constRegX);
			
			if (port > io_map.length) {
			
				interrupt (PORT_RANGE_FAULT);
				
				if (debugMode)
					System.out.println("Port out of range: " + port);
			}
			
			else if (io_map[port] == null) {
			
				interrupt (NO_DEVICE_AT_PORT);

				if (debugMode)
					System.out.println("No device at port: " + port);
			}
			
			else
				this.setRegister(io_map[port].dev_in ((short) port), constRegB);
		}
		
		// if we have an OUT instruction, send data to port number regX from regB
		else if (opcode == OUT) {
			
			// make sure the port exists
			int port = getRegister (constRegX);
			
			if (port > io_map.length) {
			
				interrupt (PORT_RANGE_FAULT);
				
				if (debugMode)
					System.out.println("Port out of range: " + port);
			}
			
			else if (io_map[port] == null) {
			
				interrupt (NO_DEVICE_AT_PORT);

				if (debugMode)
					System.out.println("No device at port: " + port);
			}
			
			else
				io_map[port].dev_out((short)port, (short)getRegister(constRegB));
		}
		
		// if the opcode could not be processed, flag an invalid opcode and shut down
		else {
			
			interrupt (ILLEGAL_INSTRUCTION);
			return false;
		}
		
		return true;
	}
	
	// public void isRegisterRM (short toTest): see if this rm set represents a register
	public boolean isRegisterRM (short toTest) {
		
		if (toTest == constRegA || toTest == constRegB || toTest == constRegC ||
				toTest == constRegX || toTest == constRegY || toTest == constRegS || toTest == constRegD)
			return true;
		
		return false;
	}
	
	// public void isImmediateRM (short toTest, short modBit): see if the given short is immediate
	public boolean isImmediateEightRM (short toTest) {
		
		if (toTest == constImm8)
			return true;
		
		return false;
	}
	
	// public boolean isImmediate16RM (short toTest): see if this is an immediate 16
	public boolean isImmediate16RM (short toTest) {
		
		if (toTest == constImm16)
			return true;
		
		return false;
	}
	
	// public boolean isMemoryRM (short toTest, short modBit): see if this represents a memory reference
	public boolean isMemoryRM (short toTest) {
		
		if (toTest == constMemory)
			return true;
		
		return false;
	}
	
	// public boolean isJump (short opcode): test an opcode for jump instruction
	public boolean isJump (short opcode) {
		
		if ((opcode & ~0xf0) == JMP)
			return true;
		
		return false;
	}
}

interface IO_Device {
	
	short dev_in (short port);
	void dev_out (short port, short data);
}