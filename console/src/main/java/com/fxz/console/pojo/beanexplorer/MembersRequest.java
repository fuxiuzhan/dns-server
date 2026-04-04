package com.fxz.console.pojo.beanexplorer;

public class MembersRequest {

    /**
     * Expression evaluated to the receiver whose fields/methods should be listed (e.g. "dnsServer" or "ctx").
     */
    private String expression;

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
