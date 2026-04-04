package com.fxz.console.pojo.beanexplorer;

public class MemberHint {

    private String name;

    /** PROPERTY（与 /inspect 子节点一致）或 METHOD（反射，如 List.add） */
    private String kind;

    /** Simple type name for fields / return type for methods */
    private String typeName;

    /** Full Java-ish signature for display (field type or method signature) */
    private String signature;

    /** 与 Bean 树一致的值预览（来自 inspect 子节点） */
    private String valuePreview;

    private boolean expandable;

    private String nodeKind;

    /**
     * 插入编辑器的文本；为空时用 name。Map/List 子项为 ["key"] / [0] 形式。
     */
    private String insertText;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
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

    public String getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(String nodeKind) {
        this.nodeKind = nodeKind;
    }

    public String getInsertText() {
        return insertText;
    }

    public void setInsertText(String insertText) {
        this.insertText = insertText;
    }
}
