package com.fxz.console.pojo.beanexplorer;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PathStep {

    private String kind;

    private String name;

    private Integer index;

    private String mapKey;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getIndex() {
        return index;
    }

    /** 兼容 JSON 中 index 为浮点数等情况；Java 侧 setIndex(3) 会装箱为 Integer 传入 */
    @JsonProperty("index")
    public void setIndex(Object v) {
        if (v == null) {
            this.index = null;
        } else if (v instanceof Number) {
            this.index = ((Number) v).intValue();
        } else {
            try {
                this.index = Integer.parseInt(String.valueOf(v));
            } catch (NumberFormatException e) {
                this.index = null;
            }
        }
    }

    public String getMapKey() {
        return mapKey;
    }

    public void setMapKey(String mapKey) {
        this.mapKey = mapKey;
    }
}
