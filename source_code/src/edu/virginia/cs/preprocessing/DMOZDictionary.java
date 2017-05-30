/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.preprocessing;

import edu.virginia.cs.utility.TextTokenizer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Wasi
 */
public class DMOZDictionary {

    private static SAXParserFactory factory;
    private static SAXParser saxParser;

    public static void main(String[] args) {
        try {
            factory = SAXParserFactory.newInstance();
            saxParser = factory.newSAXParser();
            readFile("./data/ODP-Crawled-Data-Level-4.xml");
        } catch (ParserConfigurationException | SAXException ex) {
            Logger.getLogger(DMOZDictionary.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void readFile(String filename) {
        try {
            File inputFile = new File(filename);
            OdpHandler dataHandler = new OdpHandler("./data/ODP-Dictionary");
            saxParser.parse(inputFile, dataHandler);
        } catch (SAXException | IOException ex) {
            Logger.getLogger(DMOZDictionary.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class OdpHandler extends DefaultHandler {

    private FileWriter fwriter;
    private StringBuilder buffer;
    private boolean isContent;
    private int docCount;
    private int tokenCount;
    private final HashMap<String, Integer> Dictionary;
    private final TextTokenizer tokenizer;

    public OdpHandler(String filename) {
        buffer = new StringBuilder();
        Dictionary = new HashMap<>();
        tokenizer = new TextTokenizer(true, true);
        try {
            fwriter = new FileWriter(filename);
        } catch (IOException ex) {
            Logger.getLogger(OdpHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void startElement(String uri,
            String localName, String qName, Attributes attributes)
            throws SAXException {
        if (qName.equalsIgnoreCase("DMOZ")) {
            System.out.println("Parsing Started!!!");
            docCount = 0;
            tokenCount = 0;
        } else if (qName.equalsIgnoreCase("webpage")) {
        } else if (qName.equalsIgnoreCase("content")) {
            isContent = true;
            buffer.setLength(0);
        }
    }

    @Override
    public void endElement(String uri,
            String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("DMOZ")) {
            System.out.println("Parsing Completed!!!");
            try {
                fwriter.write(tokenCount + "\n");
                for (Map.Entry<String, Integer> entry : Dictionary.entrySet()) {
                    if (entry.getValue() >= 10) {
                        tokenCount += entry.getValue();
                        fwriter.write(entry.getKey() + "\t" + entry.getValue() + "\n");
                        fwriter.flush();
                    }
                }
                fwriter.close();
                System.out.println("Total tokens = " + tokenCount);
            } catch (IOException ex) {
                Logger.getLogger(OdpHandler.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        } else if (qName.equalsIgnoreCase("webpage")) {
            docCount++;
            if (docCount % 10000 == 0) {
                System.out.println(docCount + " pages completed...!");
            }
        } else if (qName.equalsIgnoreCase("content")) {
            if (isContent) {
                isContent = false;
                StoreInDictionary(tokenizer.TokenizeText(buffer.toString()));
            }
        }
    }

    @Override
    public void characters(char ch[],
            int start, int length) throws SAXException {
        if (isContent) {
            buffer.append(ch, start, length);
        }
    }

    private void StoreInDictionary(List<String> param) {
        for (String token : param) {
            if (Dictionary.containsKey(token)) {
                Dictionary.put(token, Dictionary.get(token) + 1);
            } else {
                Dictionary.put(token, 1);
            }
        }
    }
}
