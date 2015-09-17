/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webdocs;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author Debasis
 */
public class ClueWebDocParser extends WTDocumentParser {

    public ClueWebDocParser(File ifile) {
        super(ifile);
    }
    
    String getValue(String warcRcdLine) {
        String[] tokens = warcRcdLine.split("\\s+");
        return tokens[1];
    }
    
    @Override
    public void parse() throws Exception {
        InputStream fileStream = new FileInputStream(ifile);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
        BufferedReader br = new BufferedReader(decoder);
        StringBuffer htmlBuff = new StringBuffer();
        String line;
        doc = null;
        
        while ((line = br.readLine()) != null) {
            if (line.startsWith("WARC-TREC-ID: ")) {
                // Got a new record start.. save the previous record
                if (doc != null) {
                    doc.html = htmlBuff.toString();
                    doc.extractText();
                    docs.add(doc);                                    
                }
                                
                doc = new ClueWebDoc();
                doc.docNo = getValue(line);
                // the next line is the url (which we store in the html field)
                doc.url = getValue(br.readLine());
                
                // skip five lines after this...
                for (int i=0; i<5; i++)
                    br.readLine();
                                
                htmlBuff = new StringBuffer();
            }
            htmlBuff.append(line).append("\n");
        }
		if (doc == null) {
			br.close();
			return;
		}

        // Add the last one...
        doc.html = htmlBuff.toString();
        doc.extractText();
        docs.add(doc);                    
        
        br.close();        
    }
    
    public static void main(String[] args) {
        try {
            ClueWebDocParser parser = new ClueWebDocParser(
                    new File("C:/research/corpora/clueweb_subset/docs/0000tw-00.warc.gz"));
            parser.parse();
            System.out.println(parser);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }    
}
