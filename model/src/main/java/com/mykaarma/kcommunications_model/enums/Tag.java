package com.mykaarma.kcommunications_model.enums;

public enum Tag {
    TRASH("Trash");

    private String tag;

    private Tag(String tag) {
        this.setTag(tag);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
