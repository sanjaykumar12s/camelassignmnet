package com.mycart.service.config;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class CamelRestConfig extends RouteBuilder {
    @Override
    public void configure() {
        restConfiguration()
                .component("netty-http")
                .host("0.0.0.0")
                .port(8080)
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true");
    }
}


// ✅ REST Configuration
//        restConfiguration()
//                .component("netty-http")
//                .host("0.0.0.0")
//                .port(8080)
//                .bindingMode(RestBindingMode.json)
//                .dataFormatProperty("prettyPrint", "true");

////-------------------------------------------------------------------------------
//
//        // it came both null but working fine
//
//        from("direct:getItemsByCategory")
//                .process(exchange -> {
//                    String categoryId = exchange.getIn().getHeader("categoryId", String.class);
//                    boolean includeSpecial = Boolean.parseBoolean((String) exchange.getIn().getHeader("includeSpecial", "false"));
//
//                    List<Document> pipeline = new ArrayList<>();
//
//                    // Combined match stage
//                    Document matchStage = new Document("categoryId", categoryId);
//                    if (includeSpecial) {
//                        matchStage.append("specialProduct", true);
//                    }
//
//                    pipeline.add(new Document("$match", matchStage));
//
//                    // Lookup category details
//                    pipeline.add(new Document("$lookup",
//                            new Document("from", "category")
//                                    .append("localField", "categoryId")
//                                    .append("foreignField", "_id")
//                                    .append("as", "categoryDetails")
//                    ));
//
//                    // Unwind category details to avoid empty array issue
//                    pipeline.add(new Document("$unwind",
//                            new Document("path", "$categoryDetails")
//                                    .append("preserveNullAndEmptyArrays", true) // Keep documents even if no categoryDetails
//                    ));
//
//                    // Group the results
//                    pipeline.add(new Document("$group",
//                            new Document("_id", "$categoryId")
//                                    .append("categoryName", new Document("$first", "$categoryDetails.categoryName"))
//                                    .append("categoryDepartment", new Document("$first", "$categoryDetails.categoryDepartment"))
//                                    .append("items", new Document("$push", "$$ROOT"))
//                    ));
//
//                    exchange.getIn().setBody(pipeline);
//                })
//                .to("mongodb:mycartdb?database=mycartdb&collection=item&operation=aggregate")
//                .process(exchange -> {
//                    List<Document> result = exchange.getIn().getBody(List.class);
//
//                    if (result.isEmpty()) {
//                        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
//                        exchange.getIn().setBody(Collections.singletonMap("message", "No items found for the given category."));
//                    } else {
//                        exchange.getIn().setBody(result.get(0)); // Return the first group result
//                    }
//                });
