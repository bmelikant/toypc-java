package ToyAsmIDE;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.util.ArrayList;

// class ToyAssembler: Define an interface to the assembler for the Toy Virtual Machine
public class ToyAssembler {
	
	// main method for the ToyAssembler
	public static void main (String [] args) {
		
		String inputName = "", outputName = "default.tei", listName = "";
		
		// read the command line arguments and set up the session variables
		if (args.length < 1) {
			
			System.out.println ("Error: no input file specified!");
			System.exit (-1);
		}
		
		// input name is always the first argument
		inputName = args[0];
		listName = inputName + ".lst";
		
		// other arguments are ignored for now
		Assembler myAssembler = new Assembler (inputName, outputName, listName, "Toy Assembler v1.0");
		
		// if the build failed display an error result
		if (myAssembler.assemble() == false) {
			
			System.out.println ("Build failed!");
			System.exit (-1);
		}
		
		// otherwise dump a succeeded result
		System.out.println ("Build succeeded!");
		System.exit (0);
	}
}

// class Assembler: define an Assembler for the Toy Virtual Machine
class Assembler {
	
	// lookup lists
	public static final String [] opcodeNames = { "nop.", "hlt.", "cpuid.", "pshf.", "popf.", "stfz", "stfa", "stfc", "stfe", "stfg",
		"stfl", "stfd", "stfo", "clfz", "clfa", "clfc", "clfe", "clfg", "clfl", "clfd", "clfo", "ldstr", "ststr", "ret",
                
		"inca..", "incb", "incc", "incx", "incy", "incs", "incd",
		"deca..", "decb", "decc", "decx", "decy", "decs", "decd",
		"pshf", "popf", "pshall", "popall", "iret", "stfi", "clfi", "in..", "out..",
		"sta..", "stb", "stc", "stx", "sty", "sts", "std", "lda..", "ldb", "ldc", "ldx", "ldy", "lds", "ldd",
		"ada..", "adb", "adc", "adx", "ady", "ads", "add", "sba..", "sbb", "sbc", "sbx", "sby", "sbs", "sbd", "mla..", "mlb", "mlc", "mlx", "mly",
		"dva..", "dvb", "dvc", "dvx", "dvy", "mda..", "mdb", "mdc", "mdx", "mdy", "psh..",
		"pop..", "..shla", "shlb", "shlc", "shlx", "shly", "shra..", "shrb", "shrc", "shrx", "shry", "call..",
		"jmp..", "je", "jne", "jg", "jng", "jl", "jnl", "jz", "jnz", "jc", "jnc",
		"ana..", "anb", "anc", "anx", "any", "ora..", "orb", "orc", "orx", "ory..", "xora", "xorb", "xorc", "xorx", "xory", 
		"cmpa..", "cmpb", "cmpc", "cmpx", "cmpy", "cmps", "cmpd", "int." };

	public static final short [] opcodeValues = { 0x60, 0x10, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20, 0x21, 0x22,
		0x23, 0x24, 0x25, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x48, 0x49, 0x40, 0x41, 0x45,
                
		0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36,
		0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D,
		0x16, 0x17, 0x50, 0x51, 0x46, 0x26, 0x4A, 0x55, 0x56,
		0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
		0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x05, 0x05,
		0x06, 0x06, 0x06, 0x06, 0x06, 0x07, 0x07, 0x07, 0x07, 0x07, 0x08,
		0x09, 0x0A, 0x0A, 0x0A, 0x0A, 0x0A, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0E,
		0x0F, 0x1F, 0x2F, 0x3F, 0x4F, 0x5F, 0x6F, 0x7F, 0x8F, 0x9F, 0xAF,
		0x11, 0x11, 0x11, 0x11, 0x11, 0x12, 0x12, 0x12, 0x12, 0x12, 0x13, 0x13, 0x13, 0x13, 0x13, 
		0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x16 };

	public static final short [] opcodeModValues = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1,
		0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70,
		0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x10, 0x20, 0x30, 0x40, 0x50,
		0x10, 0x20, 0x30, 0x40, 0x50, 0x10, 0x20, 0x30, 0x40, 0x50, 0x00,
		0x00, 0x10, 0x20, 0x30, 0x40, 0x50, 0x10, 0x20, 0x30, 0x40, 0x50, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		0x10, 0x20, 0x30, 0x40, 0x50, 0x10, 0x20, 0x30, 0x40, 0x50, 0x10, 0x20, 0x30, 0x40, 0x50,
		0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, -1 };

	public static final short [] opcodeBytes = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
		2, 2, 2, 2, 2, 2, 2, 1 };
	
	public static final short [] legalArguments = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x07, 0x07, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x07, 0x07, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F,
		0x07, 0x07, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x07, 0x07, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x07, 0x07, 0x0F, 0x0F, 0x0F,
		0x07, 0x07, 0x0F, 0x0F, 0x0F, 0x07, 0x07, 0x0F, 0x0F, 0x0F, 0x1F,
		0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x10,
		0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18,
		0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
		0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x04 };
	
	public static final String [] directives = { "db", "dw", "resb", "resw", "org", "times" };
	public static final String [] registers  = { "a", "b", "c", "x", "y", "s", "d" };
	public static final short  [] regDefines = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
	
	// argument constants
	public static final short TAKES_REGISTER = 0x01;
	public static final short TAKES_MEMREF   = 0x02;
	public static final short TAKES_IMM8     = 0x04;
	public static final short TAKES_IMM16    = 0x08;
	public static final short TAKES_LABEL    = 0x10;
	
	// member variables
	Preprocessor preprocess;
	LabelParser firstPass;
	CodeGenerator secondPass;
	String destFile;
	String errorList;
	
	// constructor
	public Assembler (String srcFile, String dstFile, String lstFile, String version) {
		
		// perform sanity checks on the lookup tables
		if (opcodeNames.length != opcodeValues.length || opcodeNames.length != opcodeModValues.length || opcodeNames.length != opcodeBytes.length
				|| opcodeNames.length != legalArguments.length) {
			
			System.out.println ("Internal assembler error: Lookup tables are inconsistent!");
		}
		
		preprocess = new Preprocessor (srcFile);
		firstPass = new LabelParser (srcFile, lstFile, version);
		destFile = dstFile;
	}
	
	// public boolean assemble (): Assemble the associated source file
	public boolean assemble () {

		// call preprocess () to preprocess the file into srcName.pre
		//preprocess.runPreProcess();
		
		// call firstPass () to generate a symbol table to pass to secondPass ()
		boolean badPass = firstPass.generateSymbolTable ();
		
		// if the first pass failed display the error log and exit
		if (badPass) {
			
			System.out.println ("Bad assembly! Error list:\n");
			System.out.println (firstPass.getErrorList());
			System.out.println ("Unable to recover from errors on first pass, ending...");
			
			errorList = firstPass.getErrorList ();
			return true;
		}
		
		// now we perform the second pass. Set up the code generator
		secondPass = new CodeGenerator (firstPass.getCodeTable(), firstPass.getSymbolTable(), destFile, firstPass.getAssemblySize(), firstPass.getOrigin());
		badPass = secondPass.generateCode();
		
		if (badPass) {
			
			System.out.println ("Bad assembly! Error list:\n");
			System.out.println (secondPass.getErrorList ());
			errorList = secondPass.getErrorList();
			return true;
		}
		
		System.out.println ("Assembly Succeeded!\n\n");
		return false;
	}
	
	public String getErrorList () { return errorList; }
	
	// public void redirectOutput (PrintStream outRedirect, PrintStream errRedirect): Redirect output if used with an IDE
	public void redirectOutput (PrintStream outRedirect, PrintStream errRedirect) {
		
		// redirect the output to the new print streams
		System.setOut(outRedirect);
		System.setErr(errRedirect);
	}
}

// class LabelParser: define a parser that will generate a list file/symbol table
class LabelParser {
	
	// member variables
	String errorList, asmVer;
	boolean errorFlag;
	File listingFile;
	LexicalAnalyzer labelAnalyzer;
	SymbolTable codeSymbols;
	IntermediateCodeList codeTable;
	int currentLocation;
	short origin = 0;
	
	// constructor
	public LabelParser (String srcFile, String listFile, String version) {
		
		listingFile = new File (listFile);
		labelAnalyzer = new LexicalAnalyzer (new File (srcFile));
		errorFlag = false;
		errorList = "";
		codeSymbols = new SymbolTable ();
		currentLocation = 0;
		origin = 0;
		asmVer = version;
		codeTable = new IntermediateCodeList ();
	}
	
	// return the error list
	public String getErrorList () { return errorList; }
	public SymbolTable getSymbolTable () { return codeSymbols; }
	public IntermediateCodeList getCodeTable () { return codeTable; }
	
	public int getAssemblySize () { return currentLocation; }
	public short getOrigin () { return origin; }
	
	// public boolean generateSymbolTable (): generate a symbol table from the given code
	public boolean generateSymbolTable () {
		
		// try to open the list file			
		labelAnalyzer.runLexicalAnalysis();
			
		String token = "", currentLineContents = "";
		int currentLine = 0;
			
		// read the info from the lexical analyzer and process it
		while (!(token = labelAnalyzer.nextToken()).equals("NoMoreTokens")) {
		}		
		
		return errorFlag;
	}
	
	// public short lookupOpcode (String mnemonic): Find the correct opcode
	public short lookupOpcode (String mnemonic) {
		
		// first search the oneByteOpcodes list
		for (int i = 0; i < Assembler.opcodeNames.length; i++)
			if (mnemonic.equals (Assembler.opcodeNames[i]))
				return Assembler.opcodeValues[i];
		
		return -1;
	}
	
	// public short lookupRegisterOneValue (String mnemonic): Look up the opcode and it's corresponding default mod value
	public short lookupRegisterOneValue (String mnemonic) {
		
		// search the two-byte opcode list
		for (int i = 0; i < Assembler.opcodeNames.length; i++) {
			
			if (mnemonic.equals (Assembler.opcodeNames[i]))
				return Assembler.opcodeModValues[i];
		}
		
		return -1;
	}
	
	// public short lookupRegisterTwoValue (String register): Find the register value for the given register
	public short lookupRegisterTwoValue (String register) {
		
		for (int i = 0; i < Assembler.registers.length; i++) {
			
			if (register.equals (Assembler.registers[i]))
				return Assembler.regDefines[i];
		}
		
		return -1;
	}
}

// class CodeGenerator: Generate machine code from an IntermediateCodeList
class CodeGenerator {
	
	IntermediateCodeList toGenerate;
	SymbolTable codeSymbols;
	File outputFile;
	short [] generatedCode;
	String errorList;
	boolean errorFlag;
	short codeOrigin;
	
	// construct this object
	public CodeGenerator (IntermediateCodeList inputList, SymbolTable codeLabels, String outfile, int codeSize, short origin) {
		
		toGenerate = inputList;
		codeSymbols = codeLabels;
		outputFile = new File (outfile);
		errorList = "";
		errorFlag = false;
		generatedCode = new short [codeSize];
		codeOrigin = origin;
	}
	
	public String getErrorList () { return errorList; }
	
	// public boolean generateCode (): Generate the code from the Intermediate code and output it
	public boolean generateCode () {
		
		// enter a loop to begin building the intermediate code
		for (int i = 0; i < toGenerate.size(); i++) {
			
			// read the entry, see what type it is
			IntermediateCodeEntry toBuild = toGenerate.getCodeEntry(i);
			
			// is this an opcode entry?
			if (toBuild.directiveEntry == false) {
				
				// build this as an opcode entry
				generatedCode[toBuild.entryAddr] = toBuild.opcode;
				
				// if there is a modrm this is two bytes!
				if (toBuild.modrm != -1) {
					
					short tstJmp = (short) (toBuild.opcode &~ 0xf0);
					
					if ((tstJmp == 0x0F || toBuild.opcode == 0x0E) && toBuild.modrm != 0x0A) {
						
						errorFlag = true;
						errorList += "Error (Line " + toBuild.instructionLine + "): Invalid combination of opcode and operands!\n";
						continue;
					}
					
					else if (toBuild.opcode == 0x16 && toBuild.modrm != 0x08) {
						
						errorFlag = true;
						errorList += "Error (Line " + toBuild.instructionLine + "): Invalid combination of opcode and operands!\n";
						continue;
					}
					
					else if (tstJmp != 0x0F && toBuild.opcode != 0x0E)
						generatedCode[toBuild.entryAddr+1] = toBuild.modrm;
					
					// if there is a memory or immediate argument add it
					if (toBuild.memoryImmediate != -1) {
						
						if (toBuild.memoryImmediate <= 0xff && tstJmp != 0x0f && toBuild.opcode != 0x0e && toBuild.opcode != 0x16)
							generatedCode[toBuild.entryAddr+2] = (short) toBuild.memoryImmediate;
						else if (toBuild.memoryImmediate <= 0xff && toBuild.opcode == 0x16)
							generatedCode[toBuild.entryAddr+1] = (short) toBuild.memoryImmediate;
						
						else if (toBuild.memoryImmediate <= 0xffff) {
							
							// here we have to check for legal arguments!
							short registerOne = (short) (toBuild.modrm >> 4);
							short registerTwo = (short) (toBuild.modrm & ~0xf0);
							
							if ((registerOne == 0x01 || registerOne == 0x02) && registerTwo == 0x09) {
								
								errorFlag = true;
								errorList += "Error (Line " + toBuild.instructionLine + "): Operand size mismatch!\n";
								continue;
							}
							
							if (tstJmp == 0x0f || toBuild.opcode == 0x0e) {
								
								generatedCode[toBuild.entryAddr+1] = (short) (toBuild.memoryImmediate & ~0xff00);
								generatedCode[toBuild.entryAddr+2] = (short) (toBuild.memoryImmediate >> 8);
							}
							
							else if (toBuild.opcode == 0x16)
								generatedCode[toBuild.entryAddr+1] = (short) (toBuild.memoryImmediate & ~0xff00);
							
							else {
								
								generatedCode[toBuild.entryAddr+2] = (short) (toBuild.memoryImmediate & ~0xff00);
								generatedCode[toBuild.entryAddr+3] = (short) (toBuild.memoryImmediate >> 8);
							}
						}
					}
				
					// if there is a label argument, look it up
					else if (toBuild.label.equals ("") == false) {
					
						boolean isMemoryRef = false;
						
						if (toBuild.label.startsWith("$")) {
							
							isMemoryRef = true;
							toBuild.label = toBuild.label.replaceFirst("$", "");
						}
						
						// find the label. If it can't be found, flag an error
						int address = codeSymbols.getSymbolAddress(toBuild.label);
						int tempAddr = address;
						address += codeOrigin;
						
						if (tempAddr == -1) {
							
							errorFlag = true;
							errorList += "Error (Line " + toBuild.instructionLine + "): Unknown label or mnemonic!"; 
							continue;
						}
	
						// if the label starts with $ we are accessing the data in memory at that location
						// otherwise, we are loading a pointer
						if (!isMemoryRef) {
							
							short registerOne = (short) (toBuild.modrm & ~0x0f);
							
							// we cannot load an address into a or b
							if (registerOne == 0x10 || registerOne == 0x20) {
								
								errorFlag = true;
								errorList += "Error (Line " + toBuild.instructionLine + "): Invalid combination of opcode and operands!";
								continue;
							}
							
							toBuild.modrm = (short) (registerOne | 0x09);
							generatedCode[toBuild.entryAddr+1] = toBuild.modrm;
						
							if (tstJmp == 0x0f || toBuild.opcode == 0x0e) {
							
								generatedCode[toBuild.entryAddr+1] = (short) (address & ~0xff00);
								generatedCode[toBuild.entryAddr+2] = (short) (address >> 8);
							}
						
							else {
							
								generatedCode[toBuild.entryAddr+2] = (short) (address & ~0xff00);
								generatedCode[toBuild.entryAddr+3] = (short) (address >> 8);
							}
						}
						
						else {
							
							toBuild.modrm &= ~0x0f;
							toBuild.modrm |= 0x0a;
							
							generatedCode[toBuild.entryAddr+1] = toBuild.modrm;
							
							if (tstJmp == 0x0f || toBuild.opcode == 0x0e) {
							
								generatedCode[toBuild.entryAddr+1] = (short) (address & ~0xff00);
								generatedCode[toBuild.entryAddr+2] = (short) (address >> 8);
							}
						
							else {
							
								generatedCode[toBuild.entryAddr+2] = (short) (address & ~0xff00);
								generatedCode[toBuild.entryAddr+3] = (short) (address >> 8);
							}
						}
					}
				}
			}
			
			// build directive space
			else if (toBuild.directiveEntry == true) {
				
				if (toBuild.directive.equals ("db")) {
					
					// enter a loop, read directive arguments until we run out
					short currentStore = toBuild.entryAddr;
					
					for (int j = 0; j < toBuild.directiveArguments.size(); j++) {
						
						String currentArg = toBuild.directiveArguments.get(j);
						
						if (currentArg.startsWith("StringLiteral=")) {
							
							String strLit = currentArg.substring(currentArg.indexOf ('=')+1);
							
							for (int k = 0; k < strLit.length(); k++)
								generatedCode[currentStore++] = (short) strLit.charAt(k);
						}
						
						else if (currentArg.startsWith("DecNumber=") || currentArg.startsWith ("HexNumber=") || currentArg.startsWith("BinNumber=")) {

							int toStore = 0;
							
							if (currentArg.startsWith ("DecNumber="))
								toStore = Integer.parseInt (currentArg.substring (currentArg.indexOf('=')+1), 10);
							else if (currentArg.startsWith ("HexNumber="))
								toStore = Integer.parseInt (currentArg.substring (currentArg.indexOf('=')+1), 16);
							if (currentArg.startsWith ("BinNumber="))
								toStore = Integer.parseInt (currentArg.substring (currentArg.indexOf('=')+1), 2);
							
							if (toStore > 0xff) {
								
								errorFlag = true;
								errorList += "Error (Line " + toBuild.instructionLine + "): Operand size mismatch!\n";
								continue;
							}
							
							else
								generatedCode[currentStore++] = (short) toStore;
						}
					}
				}
				
				else if (toBuild.directive.equals ("dw")) {
					
					// enter a loop, read directive arguments until we run out
					short currentStore = toBuild.entryAddr;
					
					for (int j = 0; j < toBuild.directiveArguments.size(); j++) {
						
						String currentArg = toBuild.directiveArguments.get(j);
						
						if (currentArg.startsWith("StringLiteral=")) {
							
							String strLit = currentArg.substring(currentArg.indexOf ('=')+1);
							
							for (int k = 0; k < strLit.length(); k++)
								generatedCode[currentStore] = (short) strLit.charAt(k);
						}
						
						else if (currentArg.startsWith("DecNumber=") || currentArg.startsWith ("HexNumber=") || currentArg.startsWith("BinNumber=")) {

							int toStore = 0;
							
							if (currentArg.startsWith ("DecNumber="))
								toStore = Integer.parseInt (currentArg.substring (currentArg.indexOf('=')+1), 10);
							else if (currentArg.startsWith ("HexNumber="))
								toStore = Integer.parseInt (currentArg.substring (currentArg.indexOf('=')+1), 16);
							if (currentArg.startsWith ("BinNumber="))
								toStore = Integer.parseInt (currentArg.substring (currentArg.indexOf('=')+1), 2);
							
							if (toStore > 0x7fff) {
								
								errorFlag = true;
								errorList += "Error (Line " + toBuild.instructionLine + "): Operand size mismatch!\n";
								continue;
							}
							
							else {
								
								generatedCode[currentStore++] = (short) (toStore & ~0xff00);
								generatedCode[currentStore++] = (short) (toStore >> 8);
							}
						}
					}
				}
			}
		}
		
		try {
			
			outputFile.createNewFile ();
			DataOutputStream exeWriter = new DataOutputStream (new FileOutputStream (outputFile));
			
			for (int i = 0; i < generatedCode.length; i++)
				exeWriter.writeShort(generatedCode[i]);
			
			exeWriter.flush ();
			exeWriter.close ();
			
		} catch (IOException e) {
			
			errorFlag = true;
			errorList += "I/O Error: Could not open destination executable!\n";
		}
		
		// return error code
		return errorFlag;
	}
}

// class LexicalAnalyzer: define a lexical analyzer for the Toy Virtual Machine
class LexicalAnalyzer {
	
	// input file and analyzer
	File inputFile;
	String fileData;
	StreamTokenizer analyzer;
	String currentLineContents;
	ArrayList<String> analysisLines;
	int currentLine;
	
	public LexicalAnalyzer (File toTokenize) {
		
		// set up this analyzer
		try {
			
			inputFile = toTokenize;
			BufferedReader fileReader = new BufferedReader (new InputStreamReader (new FileInputStream (inputFile)));
			String inputLine = "";
			fileData = "";
			
			while ((inputLine = fileReader.readLine()) != null)
				fileData += inputLine + "\n";
			
			resetAnalzyer (fileData);
			fileReader.close ();
			currentLine = 0;
			analysisLines = new ArrayList<String>();
			
		} catch (IOException e) {
			
			System.out.println ("Error opening file: " + e.toString ());
			System.exit (-1);
		}
	}
	
	// public void resetAnalyzer (): Setup this analzyer for use. Called first time from constructor
	public void resetAnalzyer (String input) {
		
		// try to initialize the analyzer
		analyzer = new StreamTokenizer (new BufferedReader (new InputStreamReader (new ByteArrayInputStream (input.getBytes()))));
			
		// now set up the tokens to parse
		// commas delimit separators outside of strings
		analyzer.whitespaceChars (',', ',');
		analyzer.ordinaryChars ('0', '9');
			
		analyzer.wordChars('0', ':');
		analyzer.wordChars('#', '$');
		analyzer.wordChars('_', '_');
		
		// set up string delimiters
		analyzer.quoteChar ('"');
		analyzer.quoteChar ('\'');
			
		// set up comment character
		analyzer.commentChar(';');
			
		// make sure end of line is significant here!
		analyzer.eolIsSignificant (true);
	}
	
	// public void runLexicalAnalysis (): Run an analysis of this code
	public void runLexicalAnalysis () {
		
		// read tokens into the ArrayList while we don't have an end of file condition
		try {
			
			int tokenType = analyzer.nextToken ();

			// loop until we encounter end of file
			while (tokenType != StreamTokenizer.TT_EOF) {
				
				// do we have to handle a new line?
				if (tokenType == StreamTokenizer.TT_EOL)
					// add tempToken to the list and then blank it out
					analysisLines.add ("EndOfLine");
				
				// do we have a String literal?
				else if (tokenType == '"' || tokenType == '\'')
					analysisLines.add("StringLiteral=" + analyzer.sval);
				
				// do we have a word?
				else if (tokenType == StreamTokenizer.TT_WORD) {
					
					String currentToken = analyzer.sval;
					
					// is this a number?
					if (currentToken.startsWith("#")) {
						
						currentToken = currentToken.replace('#', ' ');
						currentToken = currentToken.trim ();
						
						if (currentToken.endsWith("h")) {
							
							currentToken = currentToken.replace('h', ' ');
							currentToken = currentToken.trim ();
							
							analysisLines.add ("HexNumber=" + currentToken);
						}
						
						else if (currentToken.endsWith("b")) {
							
							currentToken = currentToken.replace ('b', ' ');
							currentToken = currentToken.trim ();
							
							analysisLines.add ("BinaryNumber=" + currentToken);
						}
						
						else
							analysisLines.add("DecNumber=" + currentToken);
					}
					
					// is this a memory reference?
					else if (currentToken.startsWith("$")) {
						
						analysisLines.add ("MemoryRef=" + currentToken);
					}
					
					// is this a register?
					else if (isRegister (currentToken))
						analysisLines.add ("Register=" + currentToken);
					
					// is this a two-byte opcode?
					else if (isOpcode (currentToken))
						analysisLines.add ("Opcode=" + currentToken);
					
					// is this a directive?
					else if (isDirective (currentToken))
						analysisLines.add ("Directive=" + currentToken);
					
					// no, it's a label!!!
					else
						analysisLines.add ("Label=" + currentToken);
				}
				
				tokenType = analyzer.nextToken();
			}
			
			analysisLines.add("EndOfFile");
			
		} catch (IOException e) {
			
			System.out.println ("I/O Error during analysis!");
			System.exit (-1);
		}
	}
	
	// public String nextLine (): Get the next line from the analyzer
	public String nextToken () {
		
		if (currentLine < analysisLines.size ())
			return analysisLines.get(currentLine++);
		
		return "NoMoreTokens";
	}
	
	// public String skipToEOL (): skip to the next end of line token. the next call
	// to nextToken () will return the beginning of the next line
	public String skipToEOL () {
		
		// start by checking for current token = end of line
		if (analysisLines.get(currentLine).equals("EndOfLine") || analysisLines.get(currentLine).equals("EndOfFile"))
			return analysisLines.get(currentLine);
		
		String next = nextToken ();
		
		while (!next.equals("EndOfLine") && !next.equals("EndOfFile"))
			next = nextToken ();
		
		return next;
	}
	
	// public void dumpAnalysisList (): Dump the lexical analysis to the screen
	public void dumpAnalysisList () {
		
		for (int i = 0, j = 0; i < analysisLines.size(); i++) {
			
			// read the next line from the analyzer
			String nextToken = "";
			String lineAnalysis = "";
			
			while (i < analysisLines.size () && !(nextToken = analysisLines.get(i)).equals("NoMoreTokens")) {
				
				lineAnalysis += nextToken + " ";
				
				// if we have encountered the end of the line, increment j to next line
				// and display the current line
				if (nextToken.equals("EndOfLine") || nextToken.equals("EndOfFile")) {
					
					j++;
					break;
				}
				
				i++;
			}
				
			System.out.println ("Analysis of line " + j + ": " + lineAnalysis);
		}
	}
	
	// public int getCurrentLine (): return current line
	public int getCurrentLine () { return currentLine; }
	public String getCurrentLineContents () { return currentLineContents; }
	
	// public boolean isRegister (String argument): See if this argument matches a register
	public boolean isRegister (String argument) {
		
		for (int i = 0; i < Assembler.registers.length; i++) {
			
			if (argument.equals (Assembler.registers[i]))
				return true;
		}
		
		return false;
	}
	
	// public boolean isTwoByteOpcode (String argument): See if this argument matches a two byte opcode
	public boolean isOpcode (String argument) {
		
		for (int i = 0; i < Assembler.opcodeNames.length; i++) {
			
			if (argument.equals (Assembler.opcodeValues[i]))
				return true;
		}
		
		return false;
	}
	
	// public boolean isDirective (String argument): See if this argument matches a directive
	public boolean isDirective (String argument) {
		
		for (int i = 0; i < Assembler.directives.length; i++) {
			
			if (argument.equals (Assembler.directives[i]))
				return true;
		}
		
		return false;
	}
}

// class SymbolTable: Define a symbol table
class SymbolTable {

	// member variables
	private ArrayList<SymbolTableEntry> symTable;
	
	// constructor method
	public SymbolTable () {
		
		// create the symbol table
		symTable = new ArrayList<SymbolTableEntry>();
	}
	
	// public boolean addSymbol (): Add a symbol to the table
	public boolean addSymbol (String symbol, int addr) {
		
		for (int i = 0; i < symTable.size(); i++) {
			
			if (symTable.get(i).entryName.equals(symbol))
				return true;
		}
		
		symTable.add (new SymbolTableEntry (symbol, addr, true));
		return false;
	}
	
	// public short getSymbolAddress (String symbol): Get an address from the table
	public int getSymbolAddress (String symbol) {
		
		for (int i = 0; i < symTable.size(); i++) {
			
			if (symbol.equals (symTable.get(i).entryName))
				return symTable.get(i).addr;
		}
		
		return -1;
	}
}

// class SymbolTableEntry: define a symbol table entry
class SymbolTableEntry {
	
	public SymbolTableEntry (String name, int address, boolean found) {
		
		entryName = name;
		addr = address;
		refFound = found;
	}
	
	// all members are public
	public String  entryName;
	public int     addr;
	public boolean refFound;
}

class IntermediateCodeList {
	
	private ArrayList<IntermediateCodeEntry> codeList = new ArrayList<IntermediateCodeEntry>();
	
	// addCodeEntry (): add an entry to the list
	public void addCodeEntry (IntermediateCodeEntry entry) {
		
		codeList.add (entry);
	}
	
	// getCodeEntry (int idx): get the entry at the specified index
	public IntermediateCodeEntry getCodeEntry (int idx) { return codeList.get(idx); }
	public int size () { return codeList.size (); }
}

// class IntermediateCodeEntry: define an entry for intermediate code
class IntermediateCodeEntry {
	
	public boolean directiveEntry;
	public short entryAddr;
	public int instructionLine;
	
	public short opcode;
	public short modrm;
	public int memoryImmediate;
	public String label;

	public String directive;
	public ArrayList<String> directiveArguments;
	
	public IntermediateCodeEntry () {
		
		entryAddr = 0;
		opcode = -1;
		modrm = -1;
		memoryImmediate = -1;
		label = "";
		
		directive = "";
		directiveEntry = false;
		directiveArguments = new ArrayList<String>();
	}
}

class Preprocessor {

	String file;
	String errorList;
	
	// look for files to include
	public Preprocessor (String fname) {
		
		file = fname;
		errorList = "";
	}
	
	// preprocess the file, save it as a temporary file
	public void runPreProcess () {
		
		// begin breaking down the file line-by-line
		// process directives
	}
}