/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webdocs;

import static indexer.TrecDocIndexer.FIELD_ANALYZED_CONTENT;
import static indexer.TrecDocIndexer.FIELD_ID;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import static webdocs.WTDocument.WTDOC_FIELD_HTML;
import static webdocs.WTDocument.WTDOC_FIELD_TITLE;
import java.io.*;

/**
 *
 * @author Debasis
 */
public class ClueWebDoc extends WTDocument {

	boolean isHeader(String line) {
		if (line.trim().equals("WARC/1.0"))
			return true;

		String[] tokens = line.split("\\s+");
		if (tokens.length < 2)
			return false;

		if (tokens[0].startsWith("HTTP/") && tokens.length == 3)
			return true; // HTTP header start

		if (tokens[0].equals("Date:") || tokens[0].equals("Server:") ||
			tokens[0].equals("Link:") || tokens[0].equals("Content-Type:") )
			return true;

		if (tokens.length != 2)
			return false; // probably not a header

		if (tokens[0].indexOf("WARC") >= 0)
			return true; // warc name: vals
		if (tokens[0].endsWith(":"))
			return true; // http attrib name: vals

		return false;
	}

	// Clueweb specific pre-processing of the HTML...
	// Try to identify the WARC records and the HTTP headers and remove them...
	String preProcessHTML(String html) throws Exception {
		StringBuffer buff = new StringBuffer();
		BufferedReader br = new BufferedReader(new StringReader(html));
		String line;

		while ((line = br.readLine()) != null) {
			if (!isHeader(line))
				buff.append(line).append("\n");
		}
		br.close();
		return buff.toString();
	}	
    
    @Override
    Document constructLuceneDoc() {
        Document doc = new Document();
        doc.add(new Field(FIELD_ID, this.docNo, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(WTDOC_FIELD_URL, this.url, Field.Store.YES, Field.Index.NOT_ANALYZED));

        // store the title and the raw html
        doc.add(new Field(WTDOC_FIELD_TITLE, this.title==null? "" : this.title,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));

		String ppHTML = html;
		try {
			ppHTML = preProcessHTML(html);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
        doc.add(new StoredField(WTDOC_FIELD_HTML, compress(ppHTML)));

        // the words only... no term vectors 
        doc.add(new Field(FIELD_ANALYZED_CONTENT, this.text,
                Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.NO));
        
        return doc;        
    }    
}
