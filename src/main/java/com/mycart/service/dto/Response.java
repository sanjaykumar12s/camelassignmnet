package com.mycart.service.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter

public class Response {
    private boolean isError;

    @Getter
    private String errorResponse;

    private String errMsg;

    public Response() {
    }

    public Response(boolean isError, String errorResponse, String errMsg) {
        this.isError = isError;
        this.errorResponse = errorResponse;
        this.errMsg = errMsg;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    public String getErrorResponse() {
        return errorResponse;
    }

    public void setErrorResponse(String errorResponse) {
        this.errorResponse = errorResponse;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    @Override
    public String toString() {
        return "Response{" +
                "isError=" + isError +
                ", errorResponse='" + errorResponse + '\'' +
                ", errMsg='" + errMsg + '\'' +
                '}';
    }
}

