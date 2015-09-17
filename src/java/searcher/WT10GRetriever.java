/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package searcher;

import indexer.IndexHtmlToText;
import indexer.TrecDocIndexer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.json.simple.JSONArray;
import org.xml.sax.ContentHandler;
import webdocs.WTDocument;
import org.apache.lucene.analysis.core.*;

/**
 *
 * @author dganguly
 */
public class WT10GRetriever {
    File indexDir;
    Properties prop;
    IndexReader reader;
    IndexSearcher searcher;
    Analyzer analyzer;
    float titleWeight;   // bodyWeight is 1-titleWeight
    float bodyWeight;
    int numTopDocs;
    int pageSize;
    JsonBuilderFactory factory;
    
    public WT10GRetriever(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));

        String indexDirPath = prop.getProperty("index");
        indexDir = new File(indexDirPath);

        reader = DirectoryReader.open(FSDirectory.open(indexDir));
        searcher = new IndexSearcher(reader);
        
        float lambda = Float.parseFloat(prop.getProperty("lm.lambda", "0.6"));
        searcher.setSimilarity(new LMJelinekMercerSimilarity(lambda));

	// English analyzer with SMART stopwords...	
    	analyzer = constructAnalyzer();
        
        titleWeight = Float.parseFloat(prop.getProperty("title.weight", "0.6"));        
        bodyWeight = 1-titleWeight;
        
        numTopDocs = Integer.parseInt(prop.getProperty("serp.total", "1000"));
        pageSize = Integer.parseInt(prop.getProperty("serp.pagesize", "10"));
        factory = Json.createBuilderFactory(null);
    }

    protected List<String> buildStopwordList(String stopwordFileName) {
        List<String> stopwords = new ArrayList<>();
        String stopFile = prop.getProperty(stopwordFileName);        
        String line;

        try (FileReader fr = new FileReader(stopFile);
            BufferedReader br = new BufferedReader(fr)) {
            while ( (line = br.readLine()) != null ) {
                stopwords.add(line.trim());
            }
            br.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return stopwords;
    }

    protected Analyzer constructAnalyzer() {
        Analyzer eanalyzer = new EnglishAnalyzer(
            Version.LUCENE_4_9,
            StopFilter.makeStopSet(
                Version.LUCENE_4_9, buildStopwordList("stopfile"))); // default analyzer
        return eanalyzer;        
    }
    
    String analyze(String query) throws Exception {
        StringBuffer buff = new StringBuffer();
        TokenStream stream = analyzer.tokenStream("dummy", new StringReader(query));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            String term = termAtt.toString();
            term = term.toLowerCase();
            buff.append(term).append(" ");
        }
        stream.end();
        stream.close();
        return buff.toString();
    }
    
    Query buildQuery(String queryStr) throws Exception {
        BooleanQuery q = new BooleanQuery();
        Term thisTerm = null;
        Query tq = null;
        String[] queryWords = analyze(queryStr).split("\\s+");

        // search in title and content...
        for (String term : queryWords) {
            thisTerm = new Term(WTDocument.WTDOC_FIELD_TITLE, term);
            tq = new TermQuery(thisTerm);
            tq.setBoost(titleWeight);
            q.add(tq, BooleanClause.Occur.SHOULD);

            thisTerm = new Term(TrecDocIndexer.FIELD_ANALYZED_CONTENT, term);
            tq = new TermQuery(thisTerm);
            tq.setBoost(bodyWeight);
            q.add(tq, BooleanClause.Occur.SHOULD);
        }
        return q;
    }
    
    // To be called from a servlet... Return the results to the servlet...
    public String retrieve(String queryStr) throws Exception {
        ScoreDoc[] hits = null;
        TopDocs topDocs = null;

        Query query = buildQuery(queryStr);
        TopScoreDocCollector collector = TopScoreDocCollector.create(numTopDocs, true);
        searcher.search(query, collector);
        topDocs = collector.topDocs();
        hits = topDocs.scoreDocs;

        if (hits == null || hits.length == 0) {
            return "Nothing found!!";
        }
        return constructJSONForRetrievedSet(query, hits);
    }

    public TopDocs retrieve(String queryStr, int pageNum) throws Exception {
        TopDocs topDocs = null;

        Query query = buildQuery(queryStr);
        TopScoreDocCollector collector = TopScoreDocCollector.create(numTopDocs, true);
        searcher.search(query, collector);
        topDocs = collector.topDocs();
        return topDocs;
    }
    
    public String constructJSONForRetrievedSet(Query q, ScoreDoc[] hits) throws Exception {
        JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
        
        for (ScoreDoc hit : hits) {
            arrayBuilder.add(constructJSONForDoc(q, hit.doc));
        }
        return arrayBuilder.build().toString();
    }
    
    public String constructJSONForRetrievedSet(String queryStr, ScoreDoc[] hits, int pageNum) throws Exception {
        Query query = buildQuery(queryStr);
        
        JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
        JsonObjectBuilder objectBuilder = factory.createObjectBuilder();
        
        int start = (pageNum-1)*pageSize; 
       	int end = start + pageSize;

		if (end >= hits.length)
			end = hits.length;

		int hasMore = end < hits.length? 1 : 0; 

        for (int i = start; i < end; i++) {
            ScoreDoc hit = hits[i];
            arrayBuilder.add(constructJSONForDoc(query, hit.doc));
        }
		// append the hasMore flag and the number of hits for this query...
		objectBuilder.add("hasmore", hasMore); 
		objectBuilder.add("numhits", hits.length); 
		arrayBuilder.add(objectBuilder);

        return arrayBuilder.build().toString();
    }
    
    JsonArray constructJSONForDoc(Query q, int docid) throws Exception {
        Document doc = reader.document(docid);
        
        JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
        JsonObjectBuilder objectBuilder = factory.createObjectBuilder();
        objectBuilder.add("title", doc.get(WTDocument.WTDOC_FIELD_TITLE));
        objectBuilder.add("snippet", getSnippet(q, doc, docid));
        objectBuilder.add("id", doc.get(TrecDocIndexer.FIELD_ID));
        arrayBuilder.add(objectBuilder);
        return arrayBuilder.build();
    }
    
    String getSnippet(Query q, Document doc, int docid) throws Exception {
        StringBuffer buff = new StringBuffer();
        SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
        Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(q));
        
        // Get the decompressed html
        String html = IndexHtmlToText.decompress(
            doc.getBinaryValue(WTDocument.WTDOC_FIELD_HTML).bytes);
        
        // Generate snippet...
        InputStream input = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
        ContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        new HtmlParser().parse(input, handler, metadata, new ParseContext());
        String text = handler.toString();
                
        TokenStream tokenStream = analyzer.tokenStream("dummy", new StringReader(text));
        TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, text, false, 5);
        for (int j = 0; j < frag.length; j++) {
            if ((frag[j] != null) && (frag[j].getScore() > 0)) {
                buff.append((frag[j].toString()));
            }
        }
        
        return buff.toString();
    }
    
    // Called when the webapp passes in the docid and is interested
    // to fetch the content
    public String getHTMLFromDocId(String docId) throws Exception {
        
        TopScoreDocCollector collector;
        TopDocs topDocs;
        
        Query query = new TermQuery(new Term(TrecDocIndexer.FIELD_ID, docId));
        collector = TopScoreDocCollector.create(1, true);
        searcher.search(query, collector);
        topDocs = collector.topDocs();
        ScoreDoc sd = topDocs.scoreDocs[0];
                
        Document doc = reader.document(sd.doc);
        String htmlDecompressed = IndexHtmlToText.decompress(
            doc.getBinaryValue(WTDocument.WTDOC_FIELD_HTML).bytes);
        
        return htmlDecompressed;
    }
}
