package com.example.backend.agent;

import java.util.ArrayList;
import java.util.List;

public class AgentDraftResponse {

    private String title;
    private String content;
    private List<String> tags = new ArrayList<>();
    private List<AgentReferenceItem> references = new ArrayList<>();
    private List<String> toolsUsed = new ArrayList<>();
    private String reasoningSummary;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<AgentReferenceItem> getReferences() {
        return references;
    }

    public void setReferences(List<AgentReferenceItem> references) {
        this.references = references;
    }

    public List<String> getToolsUsed() {
        return toolsUsed;
    }

    public void setToolsUsed(List<String> toolsUsed) {
        this.toolsUsed = toolsUsed;
    }

    public String getReasoningSummary() {
        return reasoningSummary;
    }

    public void setReasoningSummary(String reasoningSummary) {
        this.reasoningSummary = reasoningSummary;
    }
}
