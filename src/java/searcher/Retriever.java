package searcher;

import javax.json.JsonObject;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public interface Retriever {

    Query buildQuery(String queryStr) throws Exception;

    JsonObject constructJSONForRetrievedSet(Query query, ScoreDoc[] hits) throws Exception;

    JsonObject constructJSONForRetrievedSet(Query query, ScoreDoc[] hits, int pageNum) throws Exception;

    JsonObject constructJSONForRetrievedSet(Query query, ScoreDoc[] hits, int start, int howMany) throws Exception;

    JsonObject constructJSONForRetrievedSet(ScoreDoc[] hits, Iterable<Integer> selection) throws Exception;

    // Called when the webapp passes in the docid and is interested
    // to fetch the content
    String getHTMLFromDocId(String docId) throws Exception;

    TopDocs retrieve(Query query) throws Exception;
    
}
