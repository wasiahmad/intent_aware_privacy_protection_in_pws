/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.extra;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author wua4nw
 */
public class ODPCategoryReader extends DefaultHandler {

    private final ArrayList<String> topics;
    private final int level;

    public ODPCategoryReader(int param) {
        this.topics = new ArrayList<>();
        this.topics.add("Top");
        this.level = param;
    }

    public ArrayList<String> getTopics() {
        return topics;
    }

    @Override
    public void startElement(String uri,
            String localName, String qName, Attributes attributes)
            throws SAXException {
        if (qName.equalsIgnoreCase("DMOZ")) {
        } else if (qName.equalsIgnoreCase("Topic")) {
            String topicName = attributes.getValue("name");
            String[] split = topicName.split("/");
            if (split.length <= level) {
                topics.add(topicName);
            }
        } else if (qName.equalsIgnoreCase("content")) {
        }
    }

    @Override
    public void endElement(String uri,
            String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("DMOZ")) {
        } else if (qName.equalsIgnoreCase("Topic")) {
        } else if (qName.equalsIgnoreCase("content")) {
        }
    }

    @Override
    public void characters(char ch[],
            int start, int length) throws SAXException {
    }
}
