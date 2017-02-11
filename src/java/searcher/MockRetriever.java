package searcher;

import indexer.TrecDocIndexer;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import webdocs.WTDocument;


public class MockRetriever implements Retriever {

    @Override
    public Query buildQuery(String queryStr) throws Exception {
        
        return null;
    }
    
    @Override
    public TopDocs retrieve(Query query) throws Exception {
        int totalHits = 1000;
        ScoreDoc[] scoreDocs = new ScoreDoc[totalHits];
        TopDocs topDocs = new TopDocs(totalHits, scoreDocs, 0);
        return topDocs;
    }

    @Override
    public JsonObject constructJSONForRetrievedSet(Query query, ScoreDoc[] hits) throws Exception {
        return constructJSONForRetrievedSet(hits, new IntRange(0, hits.length));
    }

    @Override
    public JsonObject constructJSONForRetrievedSet(Query query, ScoreDoc[] hits, int pageNum) throws Exception {
        int start = (pageNum - 1) * 10;
        int end = start + 10;
        //int hasMore = end < hits.length ? 1 : 0;
        //objectBuilder.add("hasmore", hasMore);
        IntRange range = new IntRange(start, end).limit(0, hits.length);
        return constructJSONForRetrievedSet(hits, range);
    }

    @Override
    public JsonObject constructJSONForRetrievedSet(Query query,ScoreDoc[] hits, int start, int howMany) throws Exception {
        IntRange range = new IntRange(start, start + howMany).limit(0, hits.length);
        return constructJSONForRetrievedSet( hits, range);
    }

    @Override
    public JsonObject constructJSONForRetrievedSet(ScoreDoc[] hits, Iterable<Integer> selection) throws Exception {
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder objectBuilder = factory.createObjectBuilder();
        if (hits == null || hits.length == 0) {
            return objectBuilder.add("error", "Nothing found").build();
        }
        objectBuilder.add("numHits", hits.length);
        JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
        for (Integer i : selection) {
            try {
                ScoreDoc hit = hits[i];
                arrayBuilder.add(constructJSONForDoc(i));
            } catch (NullPointerException | IndexOutOfBoundsException e) {}
        }
        objectBuilder.add("results", arrayBuilder);
        return objectBuilder.build();
    }
    
    JsonObjectBuilder constructJSONForDoc(int docid) throws Exception {
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        
        JsonObjectBuilder objectBuilder = factory.createObjectBuilder();
        objectBuilder.add("title", "title-" + docid);
        objectBuilder.add("snippet", "snippet-" + docid);
        objectBuilder.add("id", "" + docid);
        return objectBuilder;
    }

    @Override
    public String getHTMLFromDocId(String docId) throws Exception {
        throw new UnsupportedOperationException("");
    }  
}
