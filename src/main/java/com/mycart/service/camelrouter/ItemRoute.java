package com.mycart.service.camelrouter;
import com.mycart.service.dto.Response;
import com.mycart.service.processors.*;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Component
public class ItemRoute extends RouteBuilder {
    private static final Logger logger = LoggerFactory.getLogger(ItemRoute.class);

    @Override
    public void configure() throws ProcessException {
        onException(ProcessException.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "ProcessException occurred: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

                    Response errorResponse = new Response();
                    errorResponse.setError(true);
                    errorResponse.setErrorResponse("Invalid Request");
                    errorResponse.setErrMsg(exception.getMessage());

                    exchange.getIn().setBody(errorResponse); // Will be auto-serialized to JSON by Camel
                });
//------------------------------------------------------------------
        // Get item by ID

        rest("/mycart/item/{itemId}")
                .get()
                .to("direct:getItemById");

        from("direct:getItemById")
                .log("Fetching item with ID: ${header.itemId}")
                .bean("itemProcessors", "validateItemId")  // Calls the validateItemId method
                .to("mongodb:mycartdb?database=mycartdb&collection=item&operation=findById")
                .choice()
                .when(body().isNull())
                .bean("itemProcessors", "itemNotFound")  // Calls the itemNotFound method
                .otherwise()
                .log("Item found: ${body}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .end();

        //-------------------------------------------------
        // get item by categoryId true /false
        // Valid route: This route is for "/Category/{categoryId}"

        rest("/category/{categoryId}")
                .get()
                .param()
                .name("includeSpecial").type(RestParamType.query)
                .description("Include special items").dataType("boolean").defaultValue("")
                .endParam()
                .to("direct:getItemsByCategory");

        // Valid Route Handler
        from("direct:getItemsByCategory")
                .process(exchange -> {
                    // Validate if categoryId is present in the request URL
                    String categoryId = exchange.getIn().getHeader("categoryId", String.class);
                    if (categoryId == null || categoryId.trim().isEmpty()) {
                        throw new ProcessException("Missing categoryId in the request URL.");
                    }
                })
                .log("Processing categoryId: ${header.categoryId}, includeSpecial: ${header.includeSpecial}")
                .process(new CategoryProcessor());


//-----------------------------------------------------------------
        //TODO Add item in the url
        // post items
        rest("/items")
                .post()
                .consumes("application/json")
                .to("direct:postNewItem");

        from("direct:postNewItem")
                .bean("postNewItemProcessor", "validate")
                .choice()
                .when(exchangeProperty("stopProcessing").isEqualTo(true)).stop().end()

                .setBody(simple("${header.itemCategoryId}"))
                .to("mongodb:mycartdb?database=mycartdb&collection=category&operation=findById")
                .bean("postNewItemProcessor", "checkCategory")
                .choice()
                .when(exchangeProperty("stopProcessing").isEqualTo(true)).stop().end()

                .setBody(simple("${header.itemId}"))
                .to("mongodb:mycartdb?database=mycartdb&collection=item&operation=findById")

                .choice()
                .when(body().isNotNull()) // update
                .setBody(exchangeProperty("item"))
                .to("mongodb:mycartdb?database=mycartdb&collection=item&operation=save")
                .process(e -> {
                    PostNewItemProcessor p = e.getContext().getRegistry()
                            .lookupByNameAndType("postNewItemProcessor", PostNewItemProcessor.class);
                    p.respondInsertOrUpdate(e, true);
                })
                .otherwise() // insert
                .setBody(exchangeProperty("item"))
                .to("mongodb:mycartdb?database=mycartdb&collection=item&operation=insert")
                .process(e -> {
                    PostNewItemProcessor p = e.getContext().getRegistry()
                            .lookupByNameAndType("postNewItemProcessor", PostNewItemProcessor.class);
                    p.respondInsertOrUpdate(e, false);
                });
//-------------------------------------------------------------
        // post new category

        // 3. POST new category (unchanged)
        rest("/category")
                .post()
                .consumes("application/json")
                .to("direct:postNewCategory");

        from("direct:postNewCategory")
                .log("Received new category: ${body}")
                .bean("postNewCategoryProcessor", "validate")
                .choice()
                .when(exchangeProperty("stopProcessing").isEqualTo(true))
                .stop()
                .end()

                .setBody(simple("${header.categoryId}"))
                .to("mongodb:mycartdb?database=mycartdb&collection=category&operation=findById")
                .bean("postNewCategoryProcessor", "checkDuplicate")
                .choice()
                .when(exchangeProperty("stopProcessing").isEqualTo(true))
                .stop()
                .end()

                .process(exchange -> {
                    Map<String, Object> category = exchange.getProperty("category", Map.class);
                    exchange.getIn().setBody(category);
                })
                .to("mongodb:mycartdb?database=mycartdb&collection=category&operation=insert")
                .bean("postNewCategoryProcessor", "insertSuccess");

        //-----------------------------------------------------------------------------------------
        // Update stock / products count
        //TODO: Change the query into findById

        rest("/inventory/update")
                .post()
                .to("direct:updateInventory");

//        from("direct:updateInventory")
//                .onException(ProcessException.class)
//                .handled(true)
//                .process(new ErrorProcessor())
//                .end()
//                .process(new InventoryUpdateProcessor())
//                .split(simple("${exchangeProperty.inventoryList}")).streaming()
//                .process(new InventoryValidationProcessor())//TODO
//                .setHeader("CamelMongoDbCriteria", simple("{ \"_id\": \"${exchangeProperty.itemId}\" }"))
//                .setBody(simple("${header.itemId}"))
//                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=findById")
//                .process(new StockComputationProcessor())
//                .choice()
//                .when(simple("${exchangeProperty.skipUpdate} == true"))
//                .stop()
//                .otherwise()
//                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=save")
//                .end()
//                .end()
//                .process(new FinalResponseProcessor());

//        from("direct:updateInventory")
//                .onException(ProcessException.class)
//                .handled(true)
//                .process(new ErrorProcessor())
//                .end()
//                .process(new InventoryUpdateProcessor())
//                .split(simple("${exchangeProperty.inventoryList}")).streaming()
//                .process(new InventoryValidationProcessor())//TODO
//                .setHeader("CamelMongoDbCriteria", simple("{ \"_id\": \"${exchangeProperty.itemId}\" }"))
//                .setBody(simple("${header.itemId}"))
//                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=findById")
//                .process(new StockComputationProcessor())
//                .choice()
//                .when(simple("${exchangeProperty.skipUpdate} == true"))
//                .stop()
//                .otherwise()
//                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=save")
//                .end()
//                .end()
//                .process(new FinalResponseProcessor());

        from("direct:updateInventory")
                .onException(ProcessException.class)
                .handled(true)
                .bean("inventoryUpdates", "handleError") // 🔄 replaces .process(new ErrorProcessor())
                .end()
                .bean("inventoryUpdates", "validateInventoryRequest") // replaces InventoryUpdateProcessor
                .split(simple("${exchangeProperty.inventoryList}")).streaming()
                .bean("inventoryUpdates", "validateItemFields") // replaces InventoryValidationProcessor
                .setHeader("CamelMongoDbCriteria", simple("{ \"_id\": \"${exchangeProperty.itemId}\" }"))
                .setBody(simple("${header.itemId}"))
                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=findById")
                .bean("inventoryUpdates", "computeStock") // replaces StockComputationProcessor
                .choice()
                .when(simple("${exchangeProperty.skipUpdate} == true"))
                .stop()
                .otherwise()
                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=save")
                .end()
                .end()
                .bean("inventoryUpdates", "prepareFinalResponse"); // replaces FinalResponseProcessor


        //TODO NORMAL UPDATING BUT 1 ONE MESSAGE ONLY(ONLY ACTIVEMQ)


//        rest("/inventory/Asynchronous/update")
//                .post()
//                .type(Map.class)
//                .to("direct:sendToQueue");
//
//        from("direct:sendToQueue")
//                .routeId("InventoryUpdateProducer")
//                .log("Received inventory update payload: ${body}")
//                .marshal().json(JsonLibrary.Jackson)  // Use Jackson for JSON Marshalling
//                .to("activemq:queue:inventory.queue?concurrentConsumers=1") // ActiveMQ queue with 1 consumer
//                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202))  // Set HTTP response code to 202
//                .setBody().constant(Map.of("message", "Inventory update received and queued for processing"));
//
//// Consumer Route – processes messages from ActiveMQ queue using Beans
//        from("activemq:queue:inventory.queue?concurrentConsumers=1")
//                .routeId("InventoryUpdateConsumer")
//                .log("Consuming inventory update message from ActiveMQ queue")
//                .unmarshal().json(JsonLibrary.Jackson, Map.class)  // Unmarshal the message into a Map
//                .onException(ProcessException.class)  // Handle process exceptions
//                .handled(true)
//                .bean("inventoryUpdates", "handleError") // Error handler bean
//                .end()
//                .bean("inventoryUpdates", "validateInventoryRequest") // Validate the inventory request
//                .split(simple("${exchangeProperty.inventoryList}")).streaming()  // Split the inventory list and process each item
//                .bean("inventoryUpdates", "validateItemFields") // Validate individual item fields
//                .setHeader("CamelMongoDbCriteria", simple("{ \"_id\": \"${exchangeProperty.itemId}\" }"))  // MongoDB query criteria
//                .setBody(simple("${header.itemId}"))
//                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=findById")  // Retrieve item from MongoDB by ID
//                .bean("inventoryUpdates", "computeStock") // Perform stock computation
//                .choice()
//                .when(simple("${exchangeProperty.skipUpdate} == true"))  // Check if update should be skipped
//                .stop()  // Stop processing if skipped
//                .otherwise()
//                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=save")  // Save updated item to DB
//                .end()
//                .end() // End split block
//                .bean("inventoryUpdates", "prepareFinalResponse");  // Prepare final response after processing

        // Route to handle the inventory update request
            //TODO WORKING ALL BUT ACTIVEMQ ONLY
//        rest("/inventory/Asynchronous/update")
//                .post()
//                .to("direct:inventoryInput");
//
//        from("direct:inventoryInput")
//                .log(" Received inventory update request")
//                .bean("asyncInventoryUpdates", "flattenItems")
//                .split(body()).streaming()
//                .to("activemq:queue:updateInventory")
//                .end()
//                .setBody(constant(" Inventory update request accepted for asynchronous processing"))
//                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202));
//
//        from("activemq:queue:updateInventory")
//                .routeId("asyncInventoryProcessor")
//                .log(" Consuming inventory update message from queue")
//                .process(exchange -> {
//                    List<Map<String, Object>> itemList = new ArrayList<>();
//                    itemList.add(exchange.getIn().getBody(Map.class));
//                    exchange.setProperty("inventoryList", itemList);
//                    exchange.setProperty("successList", new ArrayList<>());
//                    exchange.setProperty("failureList", new ArrayList<>());
//                })
//                .split(simple("${exchangeProperty.inventoryList}")).streaming()
//                .doTry()
//                .bean("asyncInventoryUpdates", "extractStockDetails")
//                .setHeader("CamelMongoDbCriteria", simple("{ \"_id\": \"${exchangeProperty.itemId}\" }"))
//                .to("mongodb:myDb?database=mycartdb&collection=item&operation=findOneByQuery")
//                .bean("asyncInventoryUpdates", "computeAndUpdateStock")
//                .to("mongodb:myDb?database=mycartdb&collection=item&operation=save")
//                .bean("asyncInventoryUpdates", "trackSuccess")
//                .doCatch(Exception.class)
//                .bean("asyncInventoryUpdates", "trackFailure")
//                .end()
//                .end()
//                .bean("asyncInventoryUpdates", "saveFinalStatus");
//

        //TODO  WORKING FOR BOTH ACTIVEMQ AND THEN SEDA)
        rest("/inventory/Asynchronous/update")
                .post()
                .to("direct:inventoryInput");

        from("direct:inventoryInput")
                .log(" Received inventory update request")
                .bean("asyncInventoryUpdates", "flattenItems")
                .split(body()).streaming()
                .to("activemq:queue:updateInventory")  // Sending the request to ActiveMQ
                .end()
                .setBody(constant(" Inventory update request accepted for asynchronous processing"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202));

        from("activemq:queue:updateInventory")
                .routeId("asyncInventoryProcessor")
                .log(" Consuming inventory update message from ActiveMQ queue")
                .process(exchange -> {
                    List<Map<String, Object>> itemList = new ArrayList<>();
                    itemList.add(exchange.getIn().getBody(Map.class));
                    exchange.setProperty("inventoryList", itemList);
                    exchange.setProperty("successList", new ArrayList<>());
                    exchange.setProperty("failureList", new ArrayList<>());
                })
                .to("seda:processInventoryUpdate");  // Using Seda to hold the message for further processing

        from("seda:processInventoryUpdate")
                .log("🛠️ Processing inventory update in Seda queue")
                .split(simple("${exchangeProperty.inventoryList}")).streaming()
                .doTry()
                .bean("asyncInventoryUpdates", "extractStockDetails")
                .setHeader("CamelMongoDbCriteria", simple("{ \"_id\": \"${exchangeProperty.itemId}\" }"))
                .to("mongodb:myDb?database=mycartdb&collection=item&operation=findOneByQuery")
                .bean("asyncInventoryUpdates", "computeAndUpdateStock")
                .to("mongodb:myDb?database=mycartdb&collection=item&operation=save")
                .bean("asyncInventoryUpdates", "trackSuccess")
                .doCatch(Exception.class)
                .bean("asyncInventoryUpdates", "trackFailure")
                .end()
                .end()
                .bean("asyncInventoryUpdates", "saveFinalStatus");  // Final status saving to the database

    }
}