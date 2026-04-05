package com.fxz.console.pojo.beanexplorer;

public class ExecuteCodeResponse {

    private boolean success;

    private String resultType;

    private String result;

    private BeanPropertyNode resultNode;

    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public BeanPropertyNode getResultNode() {
        return resultNode;
    }

    public void setResultNode(BeanPropertyNode resultNode) {
        this.resultNode = resultNode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
