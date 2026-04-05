package com.fxz.console.pojo.beanexplorer;

import java.util.List;

public class BeanInspectRequest {

    private String beanName;

    private List<PathStep> path;

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
}
