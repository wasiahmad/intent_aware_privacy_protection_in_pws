/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.utility;

import edu.virginia.cs.object.ResultDoc;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

/**
 *
 * @author wua4nw
 */
public class Converter {

    public static Date convertStringToDate(String time) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = formatter.parse(time);
        } catch (ParseException ex) {
            Logger.getLogger(Converter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return date;
    }

    public static String convertStringToDate(Date time) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = formatter.format(time);
        return date;
    }

    public static ResultDoc convertToResultDoc(ScoreDoc scoreDoc, IndexSearcher indexSearcher, String field) {
        ResultDoc rdoc = null;
        try {
            Document doc = indexSearcher.doc(scoreDoc.doc);
            rdoc = new ResultDoc(scoreDoc.doc);
            rdoc.setTitle("" + (scoreDoc.doc + 1));
            rdoc.setBM25Score(scoreDoc.score);
            String contents = doc.getField(field).stringValue();
            String clicked_url = doc.getField("clicked_url").stringValue();
            rdoc.setContent(contents);
            rdoc.setUrl(clicked_url);
        } catch (IOException ex) {
            Logger.getLogger(Converter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rdoc;
    }

    public static ResultDoc[] convertToResultDoc(ScoreDoc[] docs, IndexSearcher indexSearcher, String field) {
        ResultDoc resultDocs[] = new ResultDoc[docs.length];
        for (int i = 0; i < docs.length; i++) {
            resultDocs[i] = convertToResultDoc(docs[i], indexSearcher, field);
        }
        return resultDocs;
    }
}
