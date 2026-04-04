package com.fxz.console.pojo.beanexplorer;

import java.util.Collections;
import java.util.List;

public class MembersResponse {

    private String valueTypeName;

    private String error;

    private List<MemberHint> hints = Collections.emptyList();

    public String getValueTypeName() {
        return valueTypeName;
    }

    public void setValueTypeName(String valueTypeName) {
        this.valueTypeName = valueTypeName;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<MemberHint> getHints() {
        return hints;
    }

    public void setHints(List<MemberHint> hints) {
        this.hints = hints == null ? Collections.<MemberHint>emptyList() : hints;
    }
}
