package com.fxz.console.pojo.beanexplorer;

import java.util.List;

public class UpdateBeanPropertyRequest {

    private String beanName;

    private List<PathStep> path;

    private String value;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public List<PathStep> getPath() {
        return path;
    }

    public void setPath(List<PathStep> path) {
        this.path = path;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
