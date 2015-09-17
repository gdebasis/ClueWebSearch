/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webdocs;

import static indexer.TrecDocIndexer.FIELD_ANALYZED_CONTENT;
import static indexer.TrecDocIndexer.FIELD_ID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.util.BytesRef;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

/**
 *
 * @author Debasis
 */

// TREC Web-track document

public class WTDocument {
    String docNo;
    String title;
    String html;
    String text;
    String url;
    
    public static final String WTDOC_FIELD_TITLE = "title";
    public static final String WTDOC_FIELD_HTML = "html";
    public static final String WTDOC_FIELD_URL = "url";
    static final int MAX_CHARACTERS = -1;
    
    void extractText() {
        try {
            InputStream input = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
            ContentHandler handler = new BodyContentHandler(MAX_CHARACTERS);
            Metadata metadata = new Metadata();
            new HtmlParser().parse(input, handler, metadata, new ParseContext());
            title = metadata.get("title");
            text = title + "\n" + handler.toString();
        }
        catch (Exception ex) {
            text = html;
        }
    }

    BytesRef compress(String str) {
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes());
            gzip.close();
        }
        catch (Exception ex) {
            return new BytesRef("");
        }
        return out==null? new BytesRef("") : new BytesRef(out.toByteArray());
    }
    
    Document constructLuceneDoc() {
        Document doc = new Document();
        doc.add(new Field(FIELD_ID, this.docNo, Field.Store.YES, Field.Index.NOT_ANALYZED));

        // store the title and the raw html
        doc.add(new Field(WTDOC_FIELD_TITLE, this.title==null? "" : this.title,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));
        doc.add(new StoredField(WTDOC_FIELD_HTML, compress(html)));
        
        // the words (also store the term vector)
        doc.add(new Field(FIELD_ANALYZED_CONTENT, this.text,
                Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.NO));
        
        return doc;        
    }
    
    public String toString() {
        return docNo + "\n" + title + "\n" + text;
    }
}

