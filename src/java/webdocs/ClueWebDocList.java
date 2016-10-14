/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webdocs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

/**
 *
 * @author Debasis
 */

public class ClueWebDocList {
    HashMap<String, Byte> docList;

    public ClueWebDocList(String spamListFile) throws Exception {
        docList = new HashMap<>();
        load(spamListFile, null);
    }

    public ClueWebDocList(String spamListFile, File dir) throws Exception {
        docList = new HashMap<>();
        String dirName = dir.getName();
        load(spamListFile, dirName);
    }

    void load(String fileName, String dirName) throws Exception {
        FileReader fr = new FileReader(fileName);
        BufferedReader br = new BufferedReader(fr);

        String line;

        System.out.println("Loading non-spam doc ids from " + fileName);

        String prefix = dirName!=null? "clueweb12-" + dirName.substring(0) : null;
        System.out.println("Loading a subset of doc-ids in memory starting with " + prefix);
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\\s+");
            String docId = tokens[1];
            if (prefix == null)
                addClueWebDocId(docId);
            else if (docId.startsWith(prefix))
                addClueWebDocId(docId);
        }
        System.out.println("Finished loading non-spam doc ids from " + fileName);
        br.close();
        fr.close();
    }

    public void addClueWebDocId(String docId) {
        // sample string: clueweb12-0004wb-00-39227
        docList.put(docId, (byte)1);
    }

    public boolean isIndexable(String docId) {
        return docList.containsKey(docId);
    }
}
