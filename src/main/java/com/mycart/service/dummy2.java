//package com.mycart.service;
//
//import com.mycart.service.camelrouter.ProcessException;
//import com.mycart.service.processor.ErrorResponseProcessor;
//import org.apache.camel.Exchange;
//import org.apache.camel.LoggingLevel;
//import org.apache.camel.builder.RouteBuilder;
//import org.apache.camel.model.dataformat.JsonLibrary;
//import org.bson.Document;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class dummy2 {
//}
//
//package com.mycart.service.camelrouter;
//
//import com.mongodb.client.model.Filters;
//
//import com.mycart.service.processor.ErrorResponseProcessor;
//import org.apache.camel.Exchange;
//import org.apache.camel.LoggingLevel;
//import org.apache.camel.Processor;
//import org.apache.camel.builder.RouteBuilder;
//import org.apache.camel.model.dataformat.JsonLibrary;
//import org.apache.camel.model.rest.RestBindingMode;
//import org.apache.camel.model.rest.RestParamType;
//import org.bson.Document;
//import org.bson.conversions.Bson;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.*;
//
//        import org.apache.camel.Exchange;
//import org.bson.types.ObjectId;
//
//
//@Component
//public class ItemRoute extends RouteBuilder {
//    private static final Logger logger = LoggerFactory.getLogger(com.mycart.service.camelrouter.ItemRoute.class);
//
//    @Value("${app.error.itemNotFound}")
//    private String itemNotFoundMessage;
//
//    @Value("${app.error.categoryNotFound}")
//    private String categoryNotFoundMessage;
//
//    @Override
//    public void configure() throws Exception {
//        onException(ProcessException.class)
//                .handled(true)
//                .log(LoggingLevel.ERROR, "Business error: ${exception.message}")
//                .process(new ErrorResponseProcessor())
//                .marshal().json(JsonLibrary.Jackson);
//        onException(Throwable.class)
//                .handled(true)
//                .log(LoggingLevel.ERROR, "Unhandled error: ${exception.message}")
//                .process(new ErrorResponseProcessor())
//                .marshal().json(JsonLibrary.Jackson);
//
////TODO: Add Throwable exception
//        // Get item by ID
//        // 1. GET item by ID using findById
//        rest("/mycart/item/{itemId}")
//                .get()
//                .to("direct:getItemById");
//
//        from("direct:getItemById")
//                .log("Fetching item with ID: ${header.itemId}")
//                .setBody(simple("${header.itemId}"))
//                .to("mongodb:mycartdb?database=mycartdb&collection=item&operation=findById")
//                .choice()
//                .when(body().isNull())
//                .log("Item not found for ID: ${header.itemId}")
//                //TODO : Add proper response message,set response code and message on ProcessException
//                .throwException(new ProcessException("Message"))
//                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
//                .setBody(simple(itemNotFoundMessage))
//                .otherwise()
//                .log("Item found: ${body}")
//                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
//                .end();
//        //-------------------------------------------------
//        //TODO: Change the url for category
//        //TODO: Add validation for empty catId
//        //TODO: Add proper msg for not found
//        //TODO: Add validation for includeSpecial
//        rest("/mycart/Category/{categoryId}")
//                .get()
////                .param()
////                .name("includeSpecial")
////                .type(RestParamType.query)
////                .description("Include special items")
////                .dataType("boolean")
////                .defaultValue("")
////                .endParam()
//                .to("direct:getItemsByCategory");
//
//        from("direct:getItemsByCategory")
//                .log("Fetching items with Category ID: ${header.categoryId}, includeSpecial: ${header.includeSpecial}")
//
//                //  Step 1: Validate if category exists
//                .setBody(simple("${header.categoryId}"))
//                .to("mongodb:mycartdb?database=mycartdb&collection=category&operation=findById")
//                .choice()
//                .when(body().isNull())
//                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
//                .process(exchange -> {
//                    //TODO: Change it a pojo, set response code and message on ProcessException
//                    Map<String, Object> response = new HashMap<>();
//                    response.put("message", "Invalid categoryId: Category not found");
//                    response.put("items", List.of());
//                    exchange.getIn().setBody(response);
//                })
//                .stop()
//
//                .end()
//
//                //  Step 2: Build aggregation pipeline
//                .process(exchange -> {
//                    String categoryId = exchange.getIn().getHeader("categoryId", String.class);
//                    String includeSpecial = exchange.getIn().getHeader("includeSpecial", String.class);
//
//                    List<Document> pipeline = new ArrayList<>();
//                    Document matchStage = new Document("categoryId", categoryId);
//
//                    if ("true".equalsIgnoreCase(includeSpecial)) {
//                        matchStage.append("specialProduct", true);
//                    } else if ("false".equalsIgnoreCase(includeSpecial)) {
//                        matchStage.append("specialProduct", false);
//                    }
//
//                    pipeline.add(new Document("$match", matchStage));
//
//                    pipeline.add(new Document("$lookup", new Document()
//                            .append("from", "category")
//                            .append("localField", "categoryId")
//                            .append("foreignField", "_id")
//                            .append("as", "categoryDetails")));
//
//                    pipeline.add(new Document("$unwind", new Document()
//                            .append("path", "$categoryDetails")
//                            .append("preserveNullAndEmptyArrays", true)));
//
//                    pipeline.add(new Document("$group", new Document()
//                            .append("_id", "$categoryId")
//                            .append("categoryName", new Document("$first", "$categoryDetails.categoryName"))
//                            .append("categoryDepartment", new Document("$first", "$categoryDetails.categoryDepartment"))
//                            .append("items", new Document("$push", "$$ROOT"))));
//
//                    exchange.getIn().setBody(pipeline);
//                })
//
//                //  Step 3: Run aggregation
//                .to("mongodb:mycartdb?database=mycartdb&collection=item&operation=aggregate")
//
//                // Step 4: Format result
//                .process(exchange -> {
//                    List<Document> results = exchange.getIn().getBody(List.class);
//
//                    if (results == null || results.isEmpty()) {
//                        exchange.getIn().setBody(Map.of("message", "No items found", "items", List.of()));
//                    } else {
//                        exchange.getIn().setBody(results.get(0));
//                    }
//                });
//
//
////-----------------------------------------------------------------
//
//
//        // 3. POST new item - updated to use findById instead of findOneByQuery
//        // posting of new item
//        //TODO Add item in the url
//        rest("/Item")
//                .post()
//                .consumes("application/json")
//                .to("direct:postNewItem");
//
//        from("direct:postNewItem")
//                .log("Received new item: ${body}")
//
//                //  Step 1: Validate _id is present and not blank
//                .process(exchange -> {
//                    var item = exchange.getIn().getBody(Map.class);
//                    exchange.setProperty("item", item);
//
//                    Object id = item.get("_id");
//                    if (id == null || id.toString().trim().isEmpty()) {
//                        exchange.getIn().setHeader("invalidId", true);
//                    }
//                })
//                .choice()
//                .when(header("invalidId").isEqualTo(true))
//                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
//                .setBody(constant("id is required and cannot be blank"))
//                .stop()
//                .end()
//
//                //  Step 2: Validate itemPrice
//                .process(exchange -> {
//                    var item = exchange.getProperty("item", Map.class);
//
//                    var price = (Map<String, Object>) item.get("itemPrice");
//                    if (price == null) {
//                        throw new ProcessException("itemPrice is required");
//                    }
//
//                    Object base = price.get("basePrice");
//                    Object selling = price.get("sellingPrice");
//
//                    if (base == null || selling == null) {
//                        throw new ProcessException("basePrice and sellingPrice are required");
//                    }
//
//                    double basePrice = ((Number) base).doubleValue();
//                    double sellingPrice = ((Number) selling).doubleValue();
//
//                    if (basePrice <= 0) {
//                        throw new ProcessException("basePrice must be greater than zero");
//                    }
//                    if (sellingPrice <= 0) {
//                        throw new ProcessException("sellingPrice must be greater than zero");
//                    }
//
//                    exchange.getIn().setHeader("itemId", item.get("_id").toString());
//                    exchange.getIn().setHeader("itemCategoryId", item.get("categoryId").toString());
//                })
//
//                //  Step 3: Validate Category exists first (applies to both insert & update)
//                .setBody(simple("${header.itemCategoryId}"))
//                .to("mongodb:mycartdb?database=mycartdb&collection=category&operation=findById")
//                .process(exchange -> {
//                    if (exchange.getIn().getBody() == null) {
//                        throw new ProcessException("Invalid categoryId: Category not found");
//                    }
//                })
//
//                //  Step 4: Check if item exists by _id
//                .setBody(simple("${header.itemId}"))
//                .to("mongodb:mycartdb?database=mycartdb&collection=item&operation=findById")
//
//                //  Step 5: Insert or update based on item existence
//                .choice()
//                .when(body().isNotNull()) // Item exists → update
//                .log("Item exists. Updating existing item...")
//                .setBody(exchangeProperty("item"))
//                .to("mongodb:mycartdb?database=mycartdb&collection=item&operation=save")
//                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
//                .setBody(constant("Item updated successfully"))
//                .otherwise() // Item does not exist → insert
//                .log("Item does not exist. Inserting new item...")
//                .setBody(exchangeProperty("item"))
//                .to("mongodb:mycartdb?database=mycartdb&collection=item&operation=insert")
//                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
//                .setBody(constant("Item inserted successfully"))
//                .endChoice();
//
//        // 3. POST new category (unchanged)
//
//        rest("/category")
//                .post()
//                .consumes("application/json")
//                .to("direct:postNewCategory");
//
//        from("direct:postNewCategory")
//                .log("Received new category: ${body}")
//                .process(exchange -> {
//                    var category = exchange.getIn().getBody(Map.class);
//                    exchange.setProperty("newCategory", category);
//
//                    String categoryId = (String) category.get("_id");
//                    String categoryName = (String) category.get("categoryName");
//
//                    if (categoryId == null || categoryId.isBlank() || categoryName == null || categoryName.isBlank()) {
//                        throw new ProcessException("Category ID and Category Name must not be empty");
//                    }
//
//                    // Setting categoryId in header for findById
//                    exchange.getIn().setHeader("categoryId", categoryId);
//                })
//                // Using findById instead of findOneByQuery
//                .setBody(simple("${header.categoryId}"))
//                .to("mongodb:mycartdb?database=mycartdb&collection=category&operation=findById")
//                .choice()
//                .when(body().isNotNull())
//                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
//                .setBody(constant("Category already exists"))
//                .otherwise()
//                .process(exchange -> {
//                    var category = exchange.getProperty("newCategory", Map.class);
//                    exchange.getIn().setBody(category);
//                })
//                .to("mongodb:mycartdb?database=mycartdb&collection=category&operation=insert")
//                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
//                .setBody(constant("Category inserted successfully"))
//                .endChoice()
//                .end();
//
//        //-----------------------------------------------------------------------------------------
//
//        // Update stock / products count
//
//        rest("/inventory/update")
//                .post()
//                .to("direct:updateInventory");
//
//        from("direct:updateInventory")
//                .onException(ProcessException.class)
//                .handled(true)
//                .process(exchange -> {
//                    ProcessException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, ProcessException.class);
//                    List<Map<String, Object>> errorList = exchange.getProperty("errorList", List.class);
//                    if (errorList == null) {
//                        errorList = new ArrayList<>();
//                        exchange.setProperty("errorList", errorList);
//                    }
//
//                    // Add error details to errorList
//                    Map<String, Object> error = new HashMap<>();
//                    error.put("itemId", exchange.getProperty("itemId"));
//                    error.put("error", exception.getClass().getSimpleName());
//                    error.put("message", exception.getMessage());
//                    errorList.add(error);
//
//                    // Mark item as skipped to avoid MongoDB update
//                    exchange.setProperty("skipUpdate", true);
//
//                    // Log the error
//
//                })
//                .end()
//                .process(exchange -> {
//                    Map<String, Object> body = exchange.getIn().getBody(Map.class);
//                    if (body == null || !body.containsKey("items") || body.get("items") == null) {
//                        throw new ProcessException("Invalid inventory format: 'items' field is missing or empty.");
//                    }
//
//                    List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
//                    if (items == null || items.isEmpty()) {
//                        throw new ProcessException("Invalid inventory format: 'items' list is missing or empty");
//                    }
//
//                    exchange.setProperty("inventoryList", items);
//                    exchange.setProperty("errorList", new ArrayList<Map<String, Object>>());
//                    exchange.setProperty("successList", new ArrayList<Map<String, Object>>());
//                    logger.info("Received inventory update body: {}", body);
//                })
//                .split(simple("${exchangeProperty.inventoryList}")).streaming()
//                .process(exchange -> {
//                    Map<String, Object> item = exchange.getIn().getBody(Map.class);
//                    if (item == null || item.get("_id") == null || item.get("stockDetails") == null) {
//                        throw new ProcessException("Item ID or stock details are missing.");
//                    }
//                    String id = item.get("_id").toString();
//                    Map<String, Object> stock = (Map<String, Object>) item.get("stockDetails");
//                    int soldOut;
//                    int damaged;
//                    try {
//                        soldOut = Integer.parseInt(stock.get("soldOut").toString());
//                        damaged = Integer.parseInt(stock.get("damaged").toString());
//                    } catch (NumberFormatException e) {
//                        throw new ProcessException("Invalid numeric format for stock details: soldOut or damaged.");
//                    }
//                    exchange.setProperty("itemId", id);
//                    exchange.setProperty("soldOut", soldOut);
//                    exchange.setProperty("damaged", damaged);
//                })
//                //TODO: Change the query into findById
//                .setHeader("CamelMongoDbCriteria", simple("{ \"_id\": \"${exchangeProperty.itemId}\" }"))
//                .setBody(simple("${header.itemId}"))
//                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=findById")
//                .process(exchange -> {
//                    Map<String, Object> item = exchange.getIn().getBody(Map.class);
//                    logger.info("Fetched item",item);
//                    if (item == null) throw new ProcessException("Item not found");
//
//                    Map<String, Object> stockDetails = (Map<String, Object>) item.get("stockDetails");
//
//                    if (stockDetails == null) throw new ProcessException("Stock details missing");
//
//                    int availableStock;
//                    int existingSoldOut;
//                    int existingDamaged;
//                    try {
//                        availableStock = Integer.parseInt(stockDetails.get("availableStock").toString());
//                        existingSoldOut = Integer.parseInt(stockDetails.get("soldOut").toString());
//                        existingDamaged = Integer.parseInt(stockDetails.get("damaged").toString());
//                    } catch (NumberFormatException e) {
//                        throw new ProcessException("Invalid numeric format for stock details: availableStock, soldOut, or damaged.");
//                    }
//
//                    if (availableStock == 0)
//                        throw new ProcessException("Zero available stock");
//
//                    int soldOut = exchange.getProperty("soldOut", Integer.class);
//                    int damaged = exchange.getProperty("damaged", Integer.class);
//
//                    if ((soldOut + damaged) > availableStock)
//                        throw new ProcessException("The total of sold out and damaged items cannot exceed the available stock.");
//
//                    int newSoldOut = existingSoldOut + soldOut;
//                    int newDamaged = existingDamaged + damaged;
//                    int newStock = availableStock - soldOut - damaged;
//
//                    stockDetails.put("availableStock", Math.max(0, newStock));
//                    stockDetails.put("soldOut", newSoldOut);
//                    stockDetails.put("damaged", newDamaged);
//                    item.put("stockDetails", stockDetails);
//                    item.put("lastUpdateDate", LocalDate.now().toString());
//
//                    exchange.setProperty("updatedItem", item);
//                    exchange.getIn().setBody(item);
//                })
//                .choice()
//                .when(simple("${exchangeProperty.skipUpdate} == true"))
//
//                .otherwise()
//                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=save")
//                .process(exchange -> {
//                    List<Map<String, Object>> successList = exchange.getProperty("successList", List.class);
//                    Map<String, Object> updatedItem = exchange.getProperty("updatedItem", Map.class);
//                    Map<String, Object> response = new HashMap<>();
//                    response.put("itemId", exchange.getProperty("itemId"));
//                    response.put("availableStock", ((Map<String, Object>) updatedItem.get("stockDetails")).get("availableStock"));
//                    successList.add(response);
//                })
//                .end()
//                .end()
//                .process(exchange -> {
//                    Map<String, Object> response = new HashMap<>();
//                    List<Map<String, Object>> errors = exchange.getProperty("errorList", List.class);
//                    List<Map<String, Object>> successes = exchange.getProperty("successList" , List.class);
//
//                    response.put("message", "Inventory update completed");
//                    response.put("successfulUpdates", successes);
//                    response.put("errors", errors);
//
//                    if (!errors.isEmpty()) {
//                        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
//                    } else {
//                        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
//                    }
//
//                    exchange.getIn().setBody(response);
//                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
//                });


                    //TODO DOING OF PROCESS FILE ITSELF ACTIVEMQ

// Asunchronous update detail code

//        rest("/inventory/asynchronous/updates")
//                .post()
//                .to("direct:inventoryAsyncProducer");
//
//        from("direct:inventoryAsyncProducer")
//                .process(new InventoryUpdateProcessor()) // Validate and extract items
//                .split(simple("${exchangeProperty.inventoryList}")).streaming()
//                .marshal().json() // Marshal each item to JSON
//                .to("activemq:queue:inventory.update.queue")
//                .end()
//                .setBody(simple("{\"message\": \"Inventory update request accepted and is being processed asynchronously.\"}"))
//                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202))
//                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"));
//
//        from("activemq:queue:inventory.update.queue")
//                .unmarshal().json(Map.class)
//                .process(exchange -> {
//                    Map<String, Object> item = exchange.getIn().getBody(Map.class);
//                    if (item == null || item.get("_id") == null || item.get("stockDetails") == null) {
//                        throw new ProcessException("Item ID or stock details are missing.");
//                    }
//
//                    String id = item.get("_id").toString();
//                    Map<String, Object> stock = (Map<String, Object>) item.get("stockDetails");
//                    int soldOut = Integer.parseInt(stock.get("soldOut").toString());
//                    int damaged = Integer.parseInt(stock.get("damaged").toString());
//                    exchange.setProperty("itemId", id);
//                    exchange.setProperty("soldOut", soldOut);
//                    exchange.setProperty("damaged", damaged);
//                })
//                .setHeader("CamelMongoDbCriteria", simple("{ \"_id\": \"${exchangeProperty.itemId}\" }"))
//                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=findOneByQuery")
//                .process(exchange -> {
//                    Map<String, Object> item = exchange.getIn().getBody(Map.class);
//                    if (item == null) throw new ProcessException("Item not found");
//                    Map<String, Object> stockDetails = (Map<String, Object>) item.get("stockDetails");
//                    int availableStock = Integer.parseInt(stockDetails.get("availableStock").toString());
//                    int existingSoldOut = Integer.parseInt(stockDetails.get("soldOut").toString());
//                    int existingDamaged = Integer.parseInt(stockDetails.get("damaged").toString());
//                    int soldOut = exchange.getProperty("soldOut", Integer.class);
//                    int damaged = exchange.getProperty("damaged", Integer.class);
//                    if ((soldOut + damaged) > availableStock) {
//                        throw new ProcessException("Update exceeds available stock.");
//                    }
//                    stockDetails.put("availableStock", availableStock - soldOut - damaged);
//                    stockDetails.put("soldOut", existingSoldOut + soldOut);
//                    stockDetails.put("damaged", existingDamaged + damaged);
//                    item.put("lastUpdateDate", java.time.LocalDate.now().toString());
//                    exchange.getIn().setBody(item);
//                })
//                .to("mongodb:myMongo?database=mycartdb&collection=item&operation=save")
//                .log("Inventory updated for item: ${exchangeProperty.itemId}")
//                .onException(ProcessException.class)
//                .handled(true)
//                .log("Inventory update failed for itemId ${exchangeProperty.itemId}: ${exception.message}")
//                .end();
//
//    }
//}
////