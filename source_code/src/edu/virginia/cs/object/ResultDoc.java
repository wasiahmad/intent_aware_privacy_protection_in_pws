/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.object;

/**
 *
 * @author wua4nw
 */
public class ResultDoc {

    private final int _id;
    private String _title = "[no title]";
    private String _content = "[no content]";
    private String _modifiedContent = "[no modified content]";
    private String _docUrl = "[no url]";
    private boolean _isClicked = false;
    private String _topic = "[no topic]";
    private float _BM25Score;
    private float _personalizationScore;

    public ResultDoc() {
        _id = -1;
    }

    public float getBM25Score() {
        return _BM25Score;
    }

    public void setBM25Score(float _BM25Score) {
        this._BM25Score = _BM25Score;
    }

    public float getPersonalizationScore() {
        return _personalizationScore;
    }

    public void setPersonalizationScore(float _personalizationScore) {
        this._personalizationScore = _personalizationScore;
    }

    public ResultDoc(int id) {
        _id = id;
    }

    public int getId() {
        return _id;
    }

    public String getTitle() {
        return _title;
    }

    public ResultDoc setTitle(String nTitle) {
        _title = nTitle;
        return this;
    }

    public String getContent() {
        return _content;
    }

    public String getUrl() {
        return _docUrl;
    }

    public ResultDoc setContent(String nContent) {
        _content = nContent;
        return this;
    }

    public String getModifiedContent() {
        return _modifiedContent;
    }

    public void setModifiedContent(String _modifiedContent) {
        this._modifiedContent = _modifiedContent;
    }

    public ResultDoc setUrl(String nContent) {
        _docUrl = nContent;
        return this;
    }

    public ResultDoc setClicked() {
        _isClicked = true;
        return this;
    }

    public boolean isClicked() {
        return _isClicked;
    }

    public String getTopic() {
        return _topic;
    }

    public void setTopic(String _topic) {
        this._topic = _topic;
    }

}
