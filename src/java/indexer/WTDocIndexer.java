/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.util.Version;
import webdocs.WTDocument;
import webdocs.WTDocumentParser;

/**
 *
 * @author Debasis
 */

public class WTDocIndexer extends TrecDocIndexer {
    
    public WTDocIndexer(String propFile) throws Exception {
        super(propFile);
    }

    @Override
    Analyzer constructAnalyzer() {
        Analyzer defaultAnalyzer = new WhitespaceAnalyzer(Version.LUCENE_4_9);
        Map<String, Analyzer> anmap = new HashMap<String, Analyzer>();
        Analyzer enAnalyzer = new EnglishAnalyzer(
            Version.LUCENE_4_9,
            StopFilter.makeStopSet(
                Version.LUCENE_4_9, buildStopwordList("stopfile"))); // default analyzer
        
        anmap.put(WTDocument.WTDOC_FIELD_TITLE, enAnalyzer);
        anmap.put(FIELD_ANALYZED_CONTENT, enAnalyzer);
        
        PerFieldAnalyzerWrapper pfAnalyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, anmap);
        return pfAnalyzer;
    }
    
    public WTDocumentParser buildParser(File f) {
        return new WTDocumentParser(f);
    }
    
    @Override
    void indexFile(File file) throws Exception {
        WTDocumentParser parser = buildParser(file);
        System.out.println("Indexing file: " + file.getName());
        parser.parse();
        List<Document> docs = parser.getDocuments();
        for (Document doc : docs) {
            writer.addDocument(doc);            
        }
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java WTDocIndexer <prop-file>");
            args[0] = "init.properties";
        }

        try {
            WTDocIndexer indexer = new WTDocIndexer(args[0]);
            indexer.processAll();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }        
}
