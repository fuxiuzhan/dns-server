package com.fxz.console.pojo.beanexplorer;

import java.util.List;

public class BeanPropertyNode {

    private String label;

    private String nodeKind;

    private String className;

    private String valuePreview;

    private boolean expandable;

    private List<PathStep> path;

    private String error;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(String nodeKind) {
        this.nodeKind = nodeKind;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getValuePreview() {
        return valuePreview;
    }

    public void setValuePreview(String valuePreview) {
        this.valuePreview = valuePreview;
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }

    public List<PathStep> getPath() {
        return path;
    }

    public void setPath(List<PathStep> path) {
        this.path = path;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
