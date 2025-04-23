package com.mycart.service.processors;

import com.mycart.service.dto.Response;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;

import java.util.*;

public class CategoryProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        try {
            // Step 1: Validate categoryId
            String categoryId = exchange.getIn().getHeader("categoryId", String.class);
            if (categoryId == null || categoryId.trim().isEmpty()) {
                setError(exchange, 400, "Missing or empty categoryId in the request.");
                return;
            }

            // Step 2: Validate includeSpecial (optional)
            String includeSpecial = exchange.getIn().getHeader("includeSpecial", String.class);
            if (includeSpecial != null) {
                includeSpecial = includeSpecial.trim();
                if (includeSpecial.isEmpty() ||
                        (!includeSpecial.equalsIgnoreCase("true") && !includeSpecial.equalsIgnoreCase("false"))) {
                    setError(exchange, 400, "Invalid value for includeSpecial. Allowed values: true, false.");
                    return;
                }
            }

            // Step 3: Check category exists using MongoDB findById
            exchange.getIn().setBody(categoryId);  // set categoryId as body
            Exchange findResult = exchange.getContext().createProducerTemplate()
                    .send("mongodb:mycartdb?database=mycartdb&collection=category&operation=findById", exchange.copy());

            Object categoryResult = findResult.getIn().getBody();
            if (categoryResult == null) {
                setError(exchange, 400, "Invalid categoryId");
                return;
            }

            // Step 4: Build aggregation pipeline
            List<Document> pipeline = new ArrayList<>();
            Document matchStage = new Document("categoryId", categoryId);

            if ("true".equalsIgnoreCase(includeSpecial)) {
                matchStage.append("specialProduct", true);
            } else if ("false".equalsIgnoreCase(includeSpecial)) {
                matchStage.append("specialProduct", false);
            }

            pipeline.add(new Document("$match", matchStage));

            // Remove categoryDetails field completely from the lookup
            pipeline.add(new Document("$lookup", new Document()
                    .append("from", "category")
                    .append("localField", "categoryId")
                    .append("foreignField", "_id")
                    .append("as", "categoryDetails")));

            pipeline.add(new Document("$unwind", new Document()
                    .append("path", "$categoryDetails")
                    .append("preserveNullAndEmptyArrays", true)));

            // Step 5: Group by categoryId and push items into the items array (categoryDetails is excluded)
            pipeline.add(new Document("$group", new Document()
                    .append("_id", "$categoryId")
                    .append("categoryName", new Document("$first", "$categoryDetails.categoryName"))
                    .append("categoryDepartment", new Document("$first", "$categoryDetails.categoryDepartment"))
                    .append("items", new Document("$push", new Document()
                            .append("_id", "$_id")
                            .append("name", "$name")
                            .append("categoryId", "$categoryId")
                            .append("itemPrice", "$itemPrice")
                            .append("stockDetails", "$stockDetails")
                            .append("specialProduct", "$specialProduct")
                            .append("lastUpdateDate", "$lastUpdateDate")
                            .append("rating", "$rating")
                            .append("comment", "$comment")))));

            // Step 6: Run aggregation
            exchange.getIn().setBody(pipeline);
            Exchange aggResult = exchange.getContext().createProducerTemplate()
                    .send("mongodb:mycartdb?database=mycartdb&collection=item&operation=aggregate", exchange.copy());

            List<Document> results = aggResult.getIn().getBody(List.class);

            // Step 7: Format response
            if (results == null || results.isEmpty()) {
                exchange.getIn().setBody(Map.of("message", "No items found", "items", List.of()));
            } else {
                exchange.getIn().setBody(results.get(0));
            }

        } catch (Exception e) {
            setError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void setError(Exchange exchange, int statusCode, String message) {
        Response response = new Response(true, "Invalid request", message);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
        exchange.getIn().setBody(response);
        exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE); // Stop route
    }
}
