package com.nasaapps.monke2.modelo;

public class HttpResponse {
    private boolean error;
    private String body;
    private int statusCode;

    public HttpResponse(boolean error, String body, int statusCode) {
        this.error = error;
        this.body = body;
        this.statusCode = statusCode;
    }

    public HttpResponse() {
        this.error = false;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

}
