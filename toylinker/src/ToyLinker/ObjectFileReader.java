/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ToyLinker;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/*
 * ObjectFileReader: Read data from an object file in the .to format
 * For the moment, this is a test class
 *
 * @author Ben Melikant
 */
public class ObjectFileReader {
    
    private File inf;
    private DataInputStream din;
    
    public ObjectFileReader (File in) throws IOException {
        
        inf = in;
        din = new DataInputStream (new FileInputStream (inf));
    }
    
    
    private short fileReadByte () throws EOFException, IOException {
        
        short toRead = din.readByte();
        
        if (toRead < 0) {
            
            toRead = (short) Math.abs(toRead);
            toRead = (short) (toRead | 0x80);
        }
        
        return toRead;
    }
}
