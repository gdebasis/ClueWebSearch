/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searcher;

import indexer.TrecDocIndexer;
import javax.json.JsonObjectBuilder;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import webdocs.WTDocument;

/**
 *
 * @author Debasis
 */
public class ClueWebRetriever extends WT10GRetriever {

    public ClueWebRetriever(String propFile) throws Exception {
        super(propFile);
    }
    
    @Override
    JsonObjectBuilder constructJSONForDoc(Query q, int docid) throws Exception {
        Document doc = reader.document(docid);
        JsonObjectBuilder objectBuilder = factory.createObjectBuilder();
        objectBuilder.add("title", doc.get(WTDocument.WTDOC_FIELD_TITLE));
        objectBuilder.add("url", doc.get(WTDocument.WTDOC_FIELD_URL));
        objectBuilder.add("id", doc.get(TrecDocIndexer.FIELD_ID));
        return objectBuilder;
    }
    
}
