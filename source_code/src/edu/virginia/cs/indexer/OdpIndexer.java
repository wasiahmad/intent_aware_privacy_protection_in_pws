package edu.virginia.cs.indexer;

import edu.virginia.cs.utility.SpecialAnalyzer;
import edu.virginia.cs.utility.TextTokenizer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OdpIndexer {

    private SAXParserFactory factory;
    private SAXParser saxParser;
    private final boolean removeStopWords;
    private final boolean doStemming;

    public OdpIndexer(boolean removeStopWords, boolean doStemming) {
        try {
            factory = SAXParserFactory.newInstance();
            saxParser = factory.newSAXParser();
        } catch (ParserConfigurationException | SAXException ex) {
            Logger.getLogger(OdpIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.removeStopWords = removeStopWords;
        this.doStemming = doStemming;
    }

    /**
     * Start creating the lucene index.
     *
     * @param indexPath
     * @param filename
     */
    public void createIndex(String indexPath, String filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            OdpHandler handler = new OdpHandler(indexPath, removeStopWords, doStemming);
            saxParser.parse(fis, handler);
            handler.finish();
        } catch (SAXException | IOException ex) {
            Logger.getLogger(OdpIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws IOException {
        OdpIndexer indexer = new OdpIndexer(false, false);
        indexer.createIndex("../lucene-ODP-index", "./data/ODP-Crawled-Data-Level-4.xml");
    }
}

class OdpHandler extends DefaultHandler {

    private IndexWriter writer;
    private final FieldType _contentFieldType;
    private final StringBuilder content;
    private boolean isContent;
    private int pagesCompleted;
    private String currentTopicName;
    private String currentURL;
    private final TextTokenizer tokenizer;
    private final TextTokenizer globalTokenizer;

    public OdpHandler(String indexPath, boolean removeStopWords, boolean doStemming) throws IOException {
        content = new StringBuilder();
        _contentFieldType = new FieldType();
        _contentFieldType.setIndexed(true);
        _contentFieldType.setStored(true);
        pagesCompleted = 0;
        setupIndex(indexPath);
        tokenizer = new TextTokenizer(removeStopWords, doStemming);
        globalTokenizer = new TextTokenizer(true, true);
    }

    public void finish() throws IOException {
        writer.close();
    }

    /**
     * Creates the initial index files on disk.
     *
     * @param indexPath
     * @return
     * @throws IOException
     */
    private void setupIndex(String indexPath) throws IOException {
        Analyzer analyzer = new SpecialAnalyzer(true, true);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,
                analyzer);
        config.setOpenMode(OpenMode.CREATE);
        config.setRAMBufferSizeMB(2048.0);

        FSDirectory dir = FSDirectory.open(new File(indexPath));
        writer = new IndexWriter(dir, config);
    }

    @Override
    public void startElement(String uri,
            String localName, String qName, Attributes attributes)
            throws SAXException {
        if (qName.equalsIgnoreCase("DMOZ")) {
        } else if (qName.equalsIgnoreCase("Topic")) {
            currentTopicName = attributes.getValue("name");
        } else if (qName.equalsIgnoreCase("webpage")) {
            currentURL = attributes.getValue("url");
        } else if (qName.equalsIgnoreCase("content")) {
            isContent = true;
            content.setLength(0);
        }
    }

    private String getModifiedContent() {
        String modifiedContent = "";
        for (String token : tokenizer.TokenizeText(content.toString())) {
            modifiedContent += token + " ";
        }
        return modifiedContent.trim();
    }

    private String updateContent() {
        String modifiedContent = "";
        for (String token : globalTokenizer.TokenizeText(content.toString())) {
            modifiedContent += token + " ";
        }
        return modifiedContent.trim();
    }

    @Override
    public void endElement(String uri,
            String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("DMOZ")) {
        } else if (qName.equalsIgnoreCase("webpage")) {
            try {
                if (!currentTopicName.contains("Top/World") && !currentTopicName.contains("Top/Regional")) {
                    Document doc = new Document();
                    doc.add(new Field("content", updateContent(), _contentFieldType)); // this content is for searching
                    doc.add(new Field("modified_content", getModifiedContent(), _contentFieldType)); // this for language modeling
                    doc.add(new Field("url", currentURL, _contentFieldType));
                    doc.add(new Field("topic", currentTopicName, _contentFieldType));
                    writer.addDocument(doc);
                    pagesCompleted++;
                    if (pagesCompleted % 10000 == 0) {
                        System.out.println(pagesCompleted + " pages completed");
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(OdpHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (qName.equalsIgnoreCase("content")) {
        }
    }

    @Override
    public void characters(char ch[],
            int start, int length) throws SAXException {
        if (isContent) {
            content.append(ch, start, length);
        }
    }
}
