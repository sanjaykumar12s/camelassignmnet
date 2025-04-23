package com.mycart.service.processors;

import com.mycart.service.dto.Response;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("postNewCategoryProcessor")
public class PostNewCategoryProcessor {

    public void validate(Exchange exchange) {
        Map<String, Object> category = exchange.getIn().getBody(Map.class);
        exchange.setProperty("category", category);

        String id = category.get("_id") != null ? category.get("_id").toString().trim() : null;
        String name = category.get("categoryName") != null ? category.get("categoryName").toString().trim() : null;

        if (id == null || id.isEmpty()) {
            setError(exchange, 400, "Invalid Request", "_id is required and cannot be blank");
            exchange.setProperty("stopProcessing", true);
            return;
        }

        if (name == null || name.isEmpty()) {
            setError(exchange, 400, "Invalid Request", "categoryName is required and cannot be blank");
            exchange.setProperty("stopProcessing", true);
            return;
        }

        exchange.getIn().setHeader("categoryId", id);
    }

    public void checkDuplicate(Exchange exchange) {
        if (exchange.getIn().getBody() != null) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            exchange.setProperty("stopProcessing", true);
            exchange.getIn().setBody(new Response(true, "Invalid Request", "Category already exists"));
        }
    }

    public void insertSuccess(Exchange exchange) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
        exchange.getIn().setBody(new Response(false, "Success", "Category inserted successfully"));
    }

    private void setError(Exchange exchange, int code, String title, String msg) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, code);
        exchange.getIn().setBody(new Response(true, title, msg));
    }
}
