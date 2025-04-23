package com.mycart.service.camelrouter;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class InventoryEnqueueRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // REST endpoint to receive the inventory update request
        rest("/inventory/Asynchronous/update")
                .post()
                .to("direct:inventoryInput");

        // Direct endpoint to handle the incoming request and enqueue for async processing
        from("direct:inventoryInput")
                .log("Received inventory update request with ${body[items].size()} items")
                .setBody(simple("${body[items]}")) // Extract the list of items to become the new body
                .split(body()).streaming() // Split items to be processed individually
                    .log("Enqueuing inventory update for item: ${body}")
                    .to("activemq:queue:updateInventory") // Enqueue items into ActiveMQ
                    .log("Item enqueued successfully: ${body}")
                .end()
                .setBody(constant("Inventory update request accepted for asynchronous processing"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202));

        // Route to process items in ActiveMQ queue asynchronously
        from("activemq:queue:updateInventory")
                .routeId("asyncInventoryProcessor")
                .log("Dequeuing inventory update message for item: ${body}")
                .process(exchange -> {
                    // Extract item and set properties to pass through the flow
                    Map<String, Object> item = exchange.getIn().getBody(Map.class);
                    List<Map<String, Object>> itemList = new ArrayList<>();
                    itemList.add(item); // Put item in list to proceed with the same processing logic
                    exchange.setProperty("inventoryList", itemList);
                    exchange.setProperty("successList", new ArrayList<>());
                    exchange.setProperty("failureList", new ArrayList<>());
                })
                .to("seda:processInventoryUpdate"); // Passing to Seda queue for processing
    }
}
