package com.generated.app;

import java.io.Serializable;

public class Note implements Serializable {
    private long id;
    private String title;
    private String content;
    private String tag;
    private String imagePath;
    private String audioPath;
    private String transcription;
    private String checklistJson;
    private long updatedAt;

    public Note() {
        this.id = -1;
        this.title = "";
        this.content = "";
        this.tag = "עבודה";
        this.imagePath = "";
        this.audioPath = "";
        this.transcription = "";
        this.checklistJson = "[]";
        this.updatedAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public String getTranscription() { return transcription; }
    public void setTranscription(String transcription) { this.transcription = transcription; }

    public String getChecklistJson() { return checklistJson; }
    public void setChecklistJson(String checklistJson) { this.checklistJson = checklistJson; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
