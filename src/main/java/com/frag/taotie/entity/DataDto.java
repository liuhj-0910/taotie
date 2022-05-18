package com.frag.taotie.entity;

public class DataDto {

    private Long userId;

    private String msg;

    public Long getUserId() {
        return userId;
    }

    public String getMsg() {
        return msg;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return userId + "::" + msg;
    }
}
