package com.vtc.openapi.app.service;

public interface IWebhookTestAppService {

    void receive(String rawBody, String signature, String timestamp);
}
