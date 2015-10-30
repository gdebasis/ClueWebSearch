
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package indexer;
//comment milla
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.apache.lucene.document.Document;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
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
    public WTDocumentParser buildParser(File f) {
        return new ClueWebDocParser(f);
    }

     @Override
    void indexFile(File file) throws Exception {

		String fname = file.getName();
		if (!fname.endsWith(ALLOWED_FILE_TYPE)) {
			System.out.println("Skipping indexing file " + fname + " because the file type is not warc.gz");
			return;
		}

		super.indexFile(file);
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
