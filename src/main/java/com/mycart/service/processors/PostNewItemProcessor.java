package com.mycart.service.processors;

import com.mycart.service.dto.Response;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component("postNewItemProcessor")
public class PostNewItemProcessor {

    public void validate(Exchange exchange) {
        Map<String, Object> item = exchange.getIn().getBody(Map.class);
        exchange.setProperty("item", item);

        Object id = item.get("_id");
        if (id == null || id.toString().trim().isEmpty()) {
            setError(exchange, 400, "Invalid Request", "_id is required and cannot be blank");
            exchange.setProperty("stopProcessing", true);
            return;
        }

        Map<String, Object> itemPrice = (Map<String, Object>) item.get("itemPrice");
        if (itemPrice == null) {
            setError(exchange, 400, "Invalid Request", "itemPrice is required");
            exchange.setProperty("stopProcessing", true);
            return;
        }

        Object base = itemPrice.get("basePrice");
        Object selling = itemPrice.get("sellingPrice");

        if (base == null || selling == null) {
            setError(exchange, 400, "Invalid Request", "basePrice and sellingPrice are required");
            exchange.setProperty("stopProcessing", true);
            return;
        }

        double basePrice = ((Number) base).doubleValue();
        double sellingPrice = ((Number) selling).doubleValue();

        if (basePrice <= 0 || sellingPrice <= 0) {
            setError(exchange, 400, "Invalid Request", "Prices must be greater than zero");
            exchange.setProperty("stopProcessing", true);
            return;
        }

        exchange.getIn().setHeader("itemId", id.toString());

        Object categoryId = item.get("categoryId");
        if (categoryId == null || categoryId.toString().trim().isEmpty()) {
            setError(exchange, 400, "Invalid Request", "categoryId is required and cannot be blank");
            exchange.setProperty("stopProcessing", true);
            return;
        }

        exchange.getIn().setHeader("itemCategoryId", categoryId.toString());
    }

    public void checkCategory(Exchange exchange) {
        if (exchange.getIn().getBody() == null) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            exchange.setProperty("stopProcessing", true);
            exchange.getIn().setBody(new Response(true, "Invalid Request", "Invalid categoryId: Category not found"));
        }
    }

    public void respondInsertOrUpdate(Exchange exchange, boolean isUpdate) {
        if (isUpdate) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            exchange.getIn().setBody(new Response(false, "Success", "Item updated successfully"));
        } else {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
            exchange.getIn().setBody(new Response(false, "Success", "Item inserted successfully"));
        }
    }

    private void setError(Exchange exchange, int code, String title, String msg) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, code);
        exchange.getIn().setBody(new Response(true, title, msg));
    }
}
