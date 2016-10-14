/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webdocs;

import indexer.TrecDocIndexer;
import static indexer.TrecDocIndexer.FIELD_ANALYZED_CONTENT;
import static indexer.TrecDocIndexer.FIELD_ID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.FilteringTokenFilter;
import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
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

class WebDocAnalyzer extends Analyzer {

    CharArraySet stopSet;
    
    public WebDocAnalyzer(Properties prop, CharArraySet stopSet) {
        this.stopSet = stopSet;
    }
    
    @Override
    protected TokenStreamComponents createComponents(String string, Reader reader) {
        final Tokenizer tokenizer = new UAX29URLEmailTokenizer(Version.LUCENE_4_9, reader);
        
        TokenStream tokenStream = new StandardFilter(Version.LUCENE_4_9, tokenizer);
        tokenStream = new LowerCaseFilter(Version.LUCENE_4_9, tokenStream);
        tokenStream = new StopFilter(Version.LUCENE_4_9, tokenStream, stopSet);
        tokenStream = new URLFilter(tokenStream); // remove URLs
        tokenStream = new ValidWordFilter(tokenStream); // remove words with digits
        tokenStream = new PorterStemFilter(tokenStream);
        
        return new Analyzer.TokenStreamComponents(tokenizer, tokenStream);
    }
}

// Removes tokens with any digit
class ValidWordFilter extends FilteringTokenFilter {

    CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

    public ValidWordFilter(TokenStream in) {
        super(Version.LUCENE_4_9, in);
    }
    
    @Override
    protected boolean accept() throws IOException {
        String token = termAttr.toString();
        int len = token.length();
        for (int i=0; i < len; i++) {
            char ch = token.charAt(i);
            if (Character.isDigit(ch))
                return false;
            if (ch == '.')
                return false;
        }
        return true;
    }    
}

class URLFilter extends FilteringTokenFilter {

    TypeAttribute typeAttr = addAttribute(TypeAttribute.class);

    public URLFilter(TokenStream in) {
        super(Version.LUCENE_4_9, in);
    }
    
    @Override
    protected boolean accept() throws IOException {
        boolean isURL = typeAttr.type() == UAX29URLEmailTokenizer.TOKEN_TYPES[UAX29URLEmailTokenizer.URL];
        return !isURL;
    }    
}


public class WTDocument {
    String docNo;
    String title;
    String html;
    String text;
    String url;
    TrecDocIndexer indexer;
    
    int freqCutoffThreshold;
    
    public static final String WTDOC_FIELD_TITLE = "title";
    public static final String WTDOC_FIELD_HTML = "html";
    public static final String WTDOC_FIELD_URL = "url";
    static final int MAX_CHARACTERS = -1;

    public WTDocument(TrecDocIndexer indexer) {
        this.indexer = indexer;
        freqCutoffThreshold = Integer.parseInt(indexer.getProperties().getProperty("indexpruner.mintermfreq", "3"));
    }
    
    // Remove URLs and apply standard filtering (stemming and stop filters)
    String preprocessText(String html, boolean title) throws IOException {
        
        int freqCutoffThreshold = title? 1 : this.freqCutoffThreshold;
        
        HashMap<String, Integer> tfMap = new HashMap<>();
        
        StringBuffer buff = new StringBuffer();
        CharArraySet stopList = StopFilter.makeStopSet(Version.LUCENE_4_9,
                indexer.buildStopwordList("stopfile"));
        
        Analyzer webdocAnalyzer = new WebDocAnalyzer(indexer.getProperties(), stopList);
        TokenStream stream = webdocAnalyzer.tokenStream("field", new StringReader(html));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        
        stream.reset();
        while (stream.incrementToken()) {
            String token = termAtt.toString();
            Integer tf = tfMap.get(token);
            if (tf == null) {
                tf = new Integer(0);
            }
            tf++;
            tfMap.put(token, tf);                
        }

        stream.end();
        stream.close();

        for (Map.Entry<String, Integer> e : tfMap.entrySet()) {
            String word = e.getKey();
            int tf = e.getValue();
            if (tf >= freqCutoffThreshold) {
                for (int i = 0; i < tf; i++) { // print this word tf times... word order doesn't matter!
                    buff.append(word).append(" ");
                }
            }
        }
        return buff.toString();        
    }
    
    void extractText() throws IOException {
        //System.out.println("HTML content: ");
        //System.out.println(html);
        //System.out.println("Filtered HTML content: ");
        //System.out.println(ppHTML);
        
        try {            
            InputStream input = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
            ContentHandler handler = new BodyContentHandler(MAX_CHARACTERS);
            Metadata metadata = new Metadata();
            new HtmlParser().parse(input, handler, metadata, new ParseContext());
            title = metadata.get("title");
            title = title == null? "" : preprocessText(title, true);
            text = preprocessText(handler.toString(), false);
            text = title + "\n" + text;
        }
        catch (Exception ex) {
            System.err.println(ex);
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

