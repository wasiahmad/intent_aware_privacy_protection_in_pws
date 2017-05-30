package edu.virginia.cs.engine;

import edu.virginia.cs.extra.Constants;
import edu.virginia.cs.extra.Helper;
import edu.virginia.cs.extra.Personalization;
import edu.virginia.cs.object.ResultDoc;
import edu.virginia.cs.object.Session;
import edu.virginia.cs.object.UserQuery;
import edu.virginia.cs.user.Intent;
import edu.virginia.cs.utility.SpecialAnalyzer;
import edu.virginia.cs.user.Profile;
import edu.virginia.cs.utility.Converter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TotalHitCountCollector;

public class Searcher {

    private IndexSearcher indexSearcher;
    private final SpecialAnalyzer analyzer;

    /* User profile which is constructed and maintained in the server side */
    private Profile userProfile;
    /* Flag to turn on or off personalization */
    private boolean activatePersonalization = false;

    /**
     * Sets up the Lucene index Searcher with the specified index.
     *
     * @param indexPath The path to the desired Lucene index.
     */
    public Searcher(String indexPath) {
        analyzer = new SpecialAnalyzer(true, true);
        try {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
            indexSearcher = new IndexSearcher(reader);
        } catch (IOException exception) {
            Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, exception);
        }
    }

    /**
     * Activate or deactivate personalization.
     *
     * @param flag
     */
    public void activatePersonalization(boolean flag) {
        activatePersonalization = flag;
    }

    /**
     * Initialize the user profile maintained by the server side.
     *
     * @param userId
     */
    public void initializeUserProfile(String userId) {
        userProfile = new Profile(userId);
    }

    /**
     * Update user profile based on the clicked document content.
     *
     * @param query
     * @param doc
     * @throws java.io.IOException
     */
    public void clickDocument(UserQuery query, ResultDoc doc) throws IOException {
        Intent intent = (Intent) userProfile.getNodeMap().get(query.getQuery_intent().getName());
        if (intent == null) {
            intent = new Intent(query.getQuery_intent().getName());
            userProfile.addIntent(intent);
        }
        intent.updateUsingClickedDoc(doc.getContent());
    }

    /**
     * Return user profile maintained by the server side.
     *
     * @return user profile
     */
    public Profile getUserProfile() {
        return userProfile;
    }

    /**
     * Sets ranking function for index searching.
     *
     * @param sim
     */
    public void setSimilarity(Similarity sim) {
        indexSearcher.setSimilarity(sim);
    }

    private void updateSessionInformation(UserQuery userQuery) {
        UserQuery lastSubmittedQuery = userProfile.getLastSubmittedQuery();
        boolean isSame = false;
        if (lastSubmittedQuery != null) {
            isSame = Helper.checkSameSession(lastSubmittedQuery, userQuery);
        }

        // current query and previous query (if any) are from different session
        if (!isSame) {
            if (lastSubmittedQuery != null) {
                // set end time of previous session
                userProfile.getLastSession().setEnd_time(lastSubmittedQuery.getQuery_time());
            }
            // start of a new user session
            Session session = new Session(userProfile.getSessions().size());
            session.setStart_time(userQuery.getQuery_time());
            userProfile.addSession(session);
        }
    }

    /**
     * The main search function. Searches the abstract field and returns a the
     * default number of results.
     *
     * @param userQuery
     * @return
     */
    public SearchResult search(UserQuery userQuery) {
        updateSessionInformation(userQuery);
        SearchQuery searchQuery = new SearchQuery(userQuery.getQuery_text(), Constants.DEFAULT_FIELD);
        BooleanQuery combinedQuery = new BooleanQuery();
        for (String field : searchQuery.fields()) {
            QueryParser parser = new QueryParser(Version.LUCENE_46, field, analyzer);
            try {
                Query textQuery = parser.parse(QueryParser.escape(searchQuery.queryText()));
                combinedQuery.add(textQuery, BooleanClause.Occur.MUST);
            } catch (ParseException exception) {
                Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, exception);
            }
        }
        return runSearch(combinedQuery, searchQuery, userQuery);
    }

    /**
     * The main search function. Searches the abstract field and returns a the
     * default number of results.
     *
     * @param query_text
     * @return
     */
    public SearchResult search(String query_text) {
        SearchQuery searchQuery = new SearchQuery(query_text, Constants.DEFAULT_FIELD);
        BooleanQuery combinedQuery = new BooleanQuery();
        for (String field : searchQuery.fields()) {
            QueryParser parser = new QueryParser(Version.LUCENE_46, field, analyzer);
            try {
                Query textQuery = parser.parse(QueryParser.escape(searchQuery.queryText()));
                combinedQuery.add(textQuery, BooleanClause.Occur.MUST);
            } catch (ParseException exception) {
                Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, exception);
            }
        }
        return runSearch(combinedQuery, searchQuery);
    }

    /**
     * Searches for a document content in the index.
     *
     * @param queryText the document title, a URL
     * @param field
     * @param retField
     * @return clicked document content
     */
    public ArrayList<String> search(String queryText, String field, String retField) {
        return runSearch(new SearchQuery(queryText, field), retField);
    }

    /**
     * Performs the actual Lucene search.
     *
     * @param luceneQuery
     * @param numResults
     * @return the SearchResult
     */
    private SearchResult runSearch(Query luceneQuery, SearchQuery searchQuery, UserQuery userQuery) {
        try {
            TopDocs docs = indexSearcher.search(luceneQuery, searchQuery.fromDoc() + searchQuery.numResults());
            ResultDoc[] hits;
            String field = searchQuery.fields().get(0);
            if (activatePersonalization) {
                /* Personalize result using entire user profile. */
                HashMap<String, Integer> tempMap = userProfile.getCompleteHistory();
                hits = Personalization.personalizeResults(indexSearcher, field, tempMap, docs.scoreDocs);
            } else {
                hits = Converter.convertToResultDoc(docs.scoreDocs, indexSearcher, field);
            }

            /* Updating the server side user profile with the query */
            userQuery.setQuery_intent(userProfile.addIntent(userQuery.getQuery_intent()));
            boolean success = userProfile.addQuery(userQuery);
            if (!success) {
                System.err.println("Failed to update server-side user profile and now exiting...");
                System.exit(1);
            }
            userProfile.getLastSession().addUser_queries(userQuery);

            SearchResult searchResult = new SearchResult(searchQuery, docs.totalHits);
            for (ResultDoc hit : hits) {
                searchResult.addResult(hit);
            }
            searchResult.trimResults(searchQuery.fromDoc());
            return searchResult;
        } catch (IOException exception) {
            Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, exception);
        }
        return new SearchResult(searchQuery);
    }

    /**
     * Performs the actual Lucene search.
     *
     * @param luceneQuery
     * @param numResults
     * @return the SearchResult
     */
    private SearchResult runSearch(Query luceneQuery, SearchQuery searchQuery) {
        try {
            TopDocs docs = indexSearcher.search(luceneQuery, searchQuery.fromDoc() + searchQuery.numResults());
            ScoreDoc[] hits = docs.scoreDocs;
            String field = searchQuery.fields().get(0);

            SearchResult searchResult = new SearchResult(searchQuery, docs.totalHits);
            for (ScoreDoc hit : hits) {
                Document doc = indexSearcher.doc(hit.doc);
                ResultDoc rdoc = new ResultDoc(hit.doc);
                String contents = doc.getField(field).stringValue();
                rdoc.setContent(contents);
                rdoc.setUrl(doc.getField("url").stringValue());
                rdoc.setTopic(doc.getField("topic").stringValue());
                searchResult.addResult(rdoc);
            }

            searchResult.trimResults(searchQuery.fromDoc());
            return searchResult;

        } catch (IOException ex) {
            Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new SearchResult(searchQuery);
    }

    /**
     * Searches and returns relevant document's content.
     *
     * @param searchQuery a clicked URL
     * @param indexableField content of this field needs to be returned
     * @return document's content
     */
    private ArrayList<String> runSearch(SearchQuery searchQuery, String indexableField) {
        Query luceneQuery = new TermQuery(new Term(searchQuery.fields().get(0), searchQuery.queryText()));
        ArrayList<String> returnedResult = new ArrayList<>();
        try {
            TotalHitCountCollector collector = new TotalHitCountCollector();
            indexSearcher.search(luceneQuery, collector);
            TopDocs docs = indexSearcher.search(luceneQuery, Math.max(1, collector.getTotalHits()));
            ScoreDoc[] hits = docs.scoreDocs;
            for (ScoreDoc hit : hits) {
                Document doc = indexSearcher.doc(hit.doc);
                String contents = doc.getField(indexableField).stringValue();
                returnedResult.add(contents);
            }
        } catch (IOException exception) {
            Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, exception);
        }

        return returnedResult;
    }

    /**
     * Searches and returns total hit count for given query terms.
     *
     * @param tokens
     * @param field
     * @return hit document's id
     */
    public int search(List<String> tokens, String field) {
        int hit_count = 0;
        BooleanQuery combinedQuery = new BooleanQuery();
        for (String token : tokens) {
            Query luceneQuery = new TermQuery(new Term(field, token));
            combinedQuery.add(luceneQuery, BooleanClause.Occur.MUST);
        }

        try {
            TotalHitCountCollector collector = new TotalHitCountCollector();
            indexSearcher.search(combinedQuery, collector);
            hit_count = collector.getTotalHits();
        } catch (IOException exception) {
            Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, exception);
        }
        return hit_count;
    }

}
