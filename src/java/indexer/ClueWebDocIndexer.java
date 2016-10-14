/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package indexer;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import webdocs.ClueWebDocList;
import webdocs.ClueWebDocParser;
import webdocs.WTDocumentParser;

/**
 *
 * @author dganguly
 */

public class ClueWebDocIndexer extends WTDocIndexer {
    
    static final String ALLOWED_FILE_TYPE = "warc.gz";  // only index files ending with this name
    
    public ClueWebDocIndexer(String propFile) throws Exception {
        super(propFile);        
    }
    
    @Override
    public WTDocumentParser buildParser(File f, File dir) {
        try {
            return new ClueWebDocParser(this, f, dir);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    void indexFile(File file, File dir) throws Exception {

        String fname = file.getName();
        if (!fname.endsWith(ALLOWED_FILE_TYPE)) {
                System.out.println("Skipping indexing file " + fname + " because the file type is not warc.gz");
                return;
        }
        
        // Check if this is a non-spam document (based on the contents
        // of a list file that is supposed to contain a list of non-spam
        // clue-web documents).

        super.indexFile(file, dir);
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java ClueWebDocIndexer <prop-file>");
            args[0] = "init.properties";
        }

        try {
            ClueWebDocIndexer indexer = new ClueWebDocIndexer(args[0]);
            indexer.processAll();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }    
}
