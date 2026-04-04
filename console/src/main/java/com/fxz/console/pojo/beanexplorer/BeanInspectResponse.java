package com.fxz.console.pojo.beanexplorer;

import java.util.List;

public class BeanInspectResponse {

    private String beanName;

    private String rootClassName;

    private String currentClassName;

    private String valuePreview;

    private String nodeKind;

    private List<PathStep> path;

    private List<BeanPropertyNode> children;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getRootClassName() {
        return rootClassName;
    }

    public void setRootClassName(String rootClassName) {
        this.rootClassName = rootClassName;
    }

    public String getCurrentClassName() {
        return currentClassName;
    }

    public void setCurrentClassName(String currentClassName) {
        this.currentClassName = currentClassName;
    }

    public String getValuePreview() {
        return valuePreview;
    }

    public void setValuePreview(String valuePreview) {
        this.valuePreview = valuePreview;
    }

    public String getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(String nodeKind) {
        this.nodeKind = nodeKind;
    }

    public List<PathStep> getPath() {
        return path;
    }

    public void setPath(List<PathStep> path) {
        this.path = path;
    }

    public List<BeanPropertyNode> getChildren() {
        return children;
    }

    public void setChildren(List<BeanPropertyNode> children) {
        this.children = children;
    }
}
