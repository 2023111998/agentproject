package com.campus.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 政策知识库模型 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Policy {
    private int id;
    private String title;
    private String content;

    public Policy() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
