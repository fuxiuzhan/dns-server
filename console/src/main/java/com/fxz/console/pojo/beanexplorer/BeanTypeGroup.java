package com.fxz.console.pojo.beanexplorer;

import java.util.List;

public class BeanTypeGroup {

    private String typeName;

    private Integer count;

    private List<BeanSummary> beans;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<BeanSummary> getBeans() {
        return beans;
    }

    public void setBeans(List<BeanSummary> beans) {
        this.beans = beans;
    }
}
