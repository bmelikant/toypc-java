/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ToyAsm;

import java.io.File;
import java.io.FileNotFoundException;

/*
 * ToyAsm.java: Recursive descent based ToyAsm assembler. Works on ToyASM v3.0 machines
 * using old standard instruction set.
 *
 * @author Ben Melikant
 *
 */

public class ToyAsm {

    public static final int FORMAT_BINARY = 0;
    public static final int FORMAT_OBJECT = 1;
    
    public static void main (String [] args) {
        
        String infile_name = "";
        String outfile_name = "toyout.bin";
        String listfile_name = "";
        
        int format = FORMAT_BINARY;
        
        // print usage info if there are no arguments
        if (args.length < 1) {
            
            System.err.println ("Error - no input files specified");
            System.err.println ("Usage: java -jar toyasm.jar infile [-o (outfile) | -f (format) ]");
            System.err.println ("Type java -jar toyasm.jar --help for more info");
            
            System.exit (-1);
        }
        
        // process the command line arguments
        for (int i = 0; i < args.length; i++) {
        
            if (args[i].equals ("--help")) {
                
                print_help ();
                System.exit (0);
            }
            
            else if (args[i].equals ("-o") || args[i].equals ("--outfile")) {
                
                // an output name must exist
                if (args.length > i+1) {
                    
                    outfile_name = args[i+1];
                    i += 2;
                }
                
                else {
                    
                    System.err.println ("Error - no output filename was specified");
                    System.exit (-1);
                }
            }
            
            else if (args[i].equals ("-f") || args[i].equals ("--format")) {
                
                // if there is another argument and the argument conforms to a specific type, set the type
                if (args.length > i+1 && args[i+1].equals ("bin")) {
                    
                    i += 2;
                    format = FORMAT_BINARY;
                }
                
                else if (args.length > i+1 && args[i+1].equals ("obj")) {
                    
                    i += 2;
                    format = FORMAT_OBJECT;
                }
                
                else if (args.length > i+1) {
                    
                    System.err.println ("Error - Unknown format: " + args[i+1]);
                    System.exit (-1);
                }
                
                else {
                    
                    System.err.println ("Error - file format not specified");
                    System.exit (-1);
                }
            }
            
            // otherwise this is an input filename!
            else if (!args[i].isEmpty()) {
                
                infile_name = args[i];
            }
        }
        
        // check if the file exists before assembly
        if (!new File(infile_name).exists()) {
            
            System.err.println ("Fatal error: could not open input file " + infile_name);
            System.exit (-1);
        }
        
        // try to create an assembler with the correct parameters above
        Assembler testAsm = new Assembler (new File (infile_name));
        testAsm.assemble ();
        
        if (testAsm.errors_exist()) {

            // print error messages and exit with error
            for (int i = 0; i < testAsm.error_count(); i++) {
                
                ErrorPacket p = testAsm.error_at(i);
                System.err.println ("Error (line " + p.lineno + "): " + p.errmsg);
            }
            
            System.exit (-1);
        }
        
        else {
            
            // if this is a binary output, send the assembler to a BinaryFileWriter
            if (format == FORMAT_BINARY) {
            
                BinaryFileWriter out = new BinaryFileWriter (new File (outfile_name));
                out.write_binary(testAsm);
                out.close_stream();
                
                System.out.println ("Toyasm: Build successful.");
            }
        }
    }
    
    private static void print_help () {
        
        System.out.println ("Toyasm Help: ");
        System.out.println ("");
        System.out.println ("Usage: java -jar toyasm.jar (infile [ -options ]) | (--help)");
        System.out.println ("Options:");
        System.out.println ("");
        System.out.println ("   -o, --outfile [filename]: Set output filename");
        System.out.println ("   -f, --format [format]: Set output format");
        System.out.println ("");
        System.out.println ("Formats:");
        System.out.println ("");
        System.out.println ("   obj: Toyasm object file format");
        System.out.println ("   bin: Toyasm flat binary file");
        System.out.println ("   tei: Toyasm executable format (not implemented yet!)");
        System.out.println ("");
    }
}
