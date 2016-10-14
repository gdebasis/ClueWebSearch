/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import webdocs.ClueWebDocList;

/**
 *
 * @author Debasis
 */
public class SpamRemover {
    IndexWriter writer;
    IndexReader reader;
    Properties prop;
    File indexDir, outIndexDir;
    ClueWebDocList nonSpams;
    
    public SpamRemover(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));                
        String indexPath = prop.getProperty("index.in");        
        indexDir = new File(indexPath);        
        String outIndexPath = prop.getProperty("index.out");        
        outIndexDir = new File(outIndexPath);        
        
        String docsIndexFileName = prop.getProperty("nonspamlist");        
        nonSpams = new ClueWebDocList(docsIndexFileName);
    }
    
    public void filterIndex() throws Exception {
        System.out.println("Filtering index at " + indexDir.getPath());
        
        IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LUCENE_4_9, new WhitespaceAnalyzer(Version.LUCENE_4_9));
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        writer = new IndexWriter(FSDirectory.open(outIndexDir), iwcfg);
        
        reader = DirectoryReader.open(FSDirectory.open(indexDir));
        int nDocs = reader.numDocs();
        
        for (int i=0; i < nDocs; i++) {
            Document doc = reader.document(i);
            String docId = doc.get(ClueWebDocIndexer.FIELD_ID);
            if (nonSpams.isIndexable(docId))
                writer.addDocument(doc);
        }        
        
        reader.close();
        writer.close();
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java SpamRemover <prop-file>");
            args[0] = "init.properties";
        }
        try {
            SpamRemover sr = new SpamRemover(args[0]);
            sr.filterIndex();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }        
    }
}
