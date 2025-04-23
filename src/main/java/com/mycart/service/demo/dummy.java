package com.mycart.service.demo;

public class dummy {

//         ✅ Get items by categoryid (with special function true / false ) not show output message only show []
//        rest("/mycart/items/{categoryId}")
//                .get()
//                .param()
//                .name("includeSpecial")
//                .type(RestParamType.query)
//                .description("Include special items")
//                .dataType("boolean")
//                .defaultValue("false")
//                .endParam()
//                .to("direct:getItemsByCategory");
//        from("direct:getItemsByCategory")
//                .process(exchange -> {
//                    String categoryId = exchange.getIn().getHeader("categoryId", String.class);
//                    boolean includeSpecial = Boolean.parseBoolean((String) exchange.getIn().getHeader("includeSpecial", "false"));
//
//                    List<Document> pipeline = new ArrayList<>();
//
//                    // ✅ Always include categoryId and specialProduct filter
//                    Document matchStage = new Document()
//                            .append("categoryId", categoryId)
//                            .append("specialProduct", includeSpecial); // ✅ always add, true or false
//
//                    pipeline.add(new Document("$match", matchStage));
//
//                    // ✅ Lookup category details
//                    pipeline.add(new Document("$lookup",
//                            new Document("from", "category")
//                                    .append("localField", "categoryId")
//                                    .append("foreignField", "_id")
//                                    .append("as", "categoryDetails")
//                    ));
//
//                    // ✅ Unwind category details to avoid empty array issue
//                    pipeline.add(new Document("$unwind",
//                            new Document("path", "$categoryDetails")
//                                    .append("preserveNullAndEmptyArrays", true)
//                    ));
//
//                    // ✅ Group the results
//                    pipeline.add(new Document("$group",
//                            new Document("_id", "$categoryId")
//                                    .append("categoryName", new Document("$first", "$categoryDetails.categoryName"))
//                                    .append("categoryDepartment", new Document("$first", "$categoryDetails.categoryDepartment"))
//                                    .append("items", new Document("$push", "$$ROOT"))
//                    ));
//
//                    exchange.getIn().setBody(pipeline);
//                })
//                .to("mongodb:mycartdb?database=mycartdb&collection=item&operation=aggregate");
}

// for update only 

//        rest("/mycart/item/inventory/update")
//                .post()
//                .consumes("application/json")
//                .to("direct:updateItemInventory");

//        from("direct:updateItemInventory")
//                .log("Received inventory update request: ${body}")
//
//                // Extract the items list from the incoming request
//                .process(exchange -> {
//                    Map<String, Object> body = exchange.getIn().getBody(Map.class);
//                    List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
//                    exchange.setProperty("itemList", items);
//                })
//
//                // Split to process each item individually
//                .split(simple("${exchangeProperty.itemList}"))
//
//                // Prepare MongoDB query with BSON Document
//                .process(exchange -> {
//                    Map<String, Object> item = exchange.getIn().getBody(Map.class);
//                    exchange.setProperty("incomingItem", item);
//
//                    String itemId = (String) item.get("_id");
//
//                    // ✅ Convert Map to BSON Document for MongoDB
//
//                    Document queryDocument = new Document("_id", itemId);
//
//                    exchange.getIn().setBody(queryDocument);
//                })
//
//                // Query MongoDB
//                .to("mongodb:myMongo?database=mycart&collection=item&operation=findOneByQuery")
//
//                // Process the result and compute updated inventory
//                .process(exchange -> {
//                    Map<String, Object> existingItem = exchange.getIn().getBody(Map.class);
//                    Map<String, Object> incomingItem = exchange.getProperty("incomingItem", Map.class);
//
//                    if (existingItem != null) {
//                        Map<String, Object> existingStockDetails = (Map<String, Object>) existingItem.get("stockDetails");
//                        Map<String, Object> incomingStockDetails = (Map<String, Object>) incomingItem.get("stockDetails");
//
//                        int existingAvailableStock = (int) existingItem.get("availableStock");
//                        int soldOut = (int) incomingStockDetails.get("soldOut");
//                        int damaged = (int) incomingStockDetails.get("damaged");
//
//                        int newAvailableStock = existingAvailableStock - soldOut - damaged;
//
//                        // Update existing item
//                        existingItem.put("availableStock", newAvailableStock);
//                        existingStockDetails.put("soldOut", soldOut);
//                        existingStockDetails.put("damaged", damaged);
//
//                        // ✅ Prepare query and update documents as BSON
//
//
//                        Document queryDocument = new Document("_id", existingItem.get("_id"));
//                        Document updateDocument = new Document("$set", new Document(existingItem));
//
//                        exchange.setProperty("queryDocument", queryDocument);
//                        exchange.getIn().setBody(updateDocument);
//                    } else {
//                        exchange.setProperty("skipUpdate", true);
//                        exchange.getIn().setBody(null); // Clean body
//                    }
//                })
//
//                // Perform the update if existing item was found
//                .choice()
//                .when(simple("${exchangeProperty.skipUpdate} == null"))
//                .toD("mongodb:myMongo?database=mycart&collection=item&operation=update&multi=false")
//                .log("Item inventory updated successfully: ${exchangeProperty.incomingItem}")
//                .otherwise()
//                .log("Item not found, skipping update: ${exchangeProperty.incomingItem}")
//                .end();
//        rest("/mycart/item/inventory/update")
//                .post()
//                .consumes("application/json")
//                .to("direct:updateItemInventory");
//
//        from("direct:inventoryUpdate")
//                .log("Received Inventory Update Request: ${body}")
//                .process(exchange -> {
//                    // Get the incoming list of Item objects
//                    List<com.mycart.service.camelrouter.Item> items = exchange.getIn().getBody(List.class);
//
//                    if (items == null || items.isEmpty()) {
//                        throw new RuntimeException("Input list is null or empty. Cannot process inventory update.");
//                    }
//
//                    // Debugging step: Log the items before processing
//                    items.forEach(item ->
//                            exchange.getContext().createProducerTemplate().sendBody("log:info", "Processing Item: " + item.getItemName())
//                    );
//
//                    // Convert each Item to a MongoDB Document
//                    List<Document> documents = items.stream()
//                            .map(item -> {
//                                // Convert each Item object to a Document for MongoDB
//                                Document doc = new Document("_id", item.getId())
//                                        .append("itemName", item.getItemName())
//                                        .append("categoryId", item.getCategoryId())
//                                        .append("availableStock", item.getAvailableStock())
//                                        .append("stockDetails", new Document("soldOut", item.getStockDetails().getSoldOut())
//                                                .append("damaged", item.getStockDetails().getDamaged()));
//                                return doc;
//                            })
//                            .collect(Collectors.toList());
//
//                    // Set the list of Documents as the body for the next processor
//                    exchange.getIn().setBody(documents);
//                })
//                .to("mongodb:myDb?operation=save&collection=items");
// REST endpoint to receive update request
// REST endpoint