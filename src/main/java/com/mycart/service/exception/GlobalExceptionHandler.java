package com.mycart.service.exception;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class GlobalExceptionHandler extends RouteBuilder {
    @Override
    public void configure() {
        onException(Exception.class)
                .handled(true)
                .log("Exception: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setBody(simple("Internal server error: ${exception.message}"));
    }
}
