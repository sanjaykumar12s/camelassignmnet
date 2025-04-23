package com.mycart.service.processors;

import com.mycart.service.camelrouter.ProcessException;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;


@Component("inventoryUpdates")
public class InventoryUpdates {

    // Handle errors and mark the update as skipped
    public void handleError(Exchange exchange) {
        ProcessException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, ProcessException.class);
        List<Map<String, Object>> errorList = exchange.getProperty("errorList", List.class);
        if (errorList == null) {
            errorList = new ArrayList<>();
            exchange.setProperty("errorList", errorList);
        }

        Map<String, Object> error = new HashMap<>();
        error.put("itemId", exchange.getProperty("itemId"));
        error.put("message", exception.getMessage());
        errorList.add(error);

        exchange.setProperty("skipUpdate", true);
    }

    // Prepare the final response with success and error lists
    public void prepareFinalResponse(Exchange exchange) {
        List<Map<String, Object>> errors = exchange.getProperty("errorList", List.class);
        List<Map<String, Object>> successes = exchange.getProperty("successList", List.class);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Inventory update completed");
        response.put("successfulUpdates", successes);
        response.put("errors", errors);

        exchange.getContext().createProducerTemplate().sendBody("log:finalResponseLog?level=INFO",
                "Inventory update details:\n" +
                        "Successful updates: " + successes.size() + "\n" +
                        "Errors: " + errors.size() + "\n" +
                        "Successful Items: " + successes + "\n" +
                        "Error Items: " + errors);

        exchange.getContext().createProducerTemplate().sendBody("log:finalResponseLog?level=DEBUG", response);

        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, errors.isEmpty() ? 200 : 400);
        exchange.getIn().setBody(response);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
    }

    // Validate the inventory request (make sure 'items' is present)
    public void validateInventoryRequest(Exchange exchange) {
        Map<String, Object> body = exchange.getIn().getBody(Map.class);
        if (body == null || !body.containsKey("items") || body.get("items") == null) {
            throw new ProcessException("Invalid inventory format: 'items' field is missing or empty.");
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) {
            throw new ProcessException("Invalid inventory format: 'items' list is missing or empty.");
        }

        exchange.setProperty("inventoryList", items);
        exchange.setProperty("errorList", new ArrayList<Map<String, Object>>());
        exchange.setProperty("successList", new ArrayList<Map<String, Object>>());
    }

    // Validate individual item fields
    public void validateItemFields(Exchange exchange) {
        Map<String, Object> item = exchange.getIn().getBody(Map.class);
        if (item == null || item.get("_id") == null || item.get("stockDetails") == null) {
            throw new ProcessException("Item ID or stock details are missing.");
        }

        String id = item.get("_id").toString();
        Map<String, Object> stock = (Map<String, Object>) item.get("stockDetails");

        try {
            int soldOut = Integer.parseInt(stock.get("soldOut").toString());
            int damaged = Integer.parseInt(stock.get("damaged").toString());
            exchange.setProperty("itemId", id);
            exchange.setProperty("soldOut", soldOut);
            exchange.setProperty("damaged", damaged);
        } catch (NumberFormatException e) {
            throw new ProcessException("Invalid numeric format for stock details: soldOut or damaged.");
        }
    }

    // Perform the stock computation
    public void computeStock(Exchange exchange) {
        Map<String, Object> item = exchange.getIn().getBody(Map.class);
        if (item == null) throw new ProcessException("No item found.");

        Map<String, Object> stockDetails = (Map<String, Object>) item.get("stockDetails");
        if (stockDetails == null) throw new ProcessException("Stock details are missing for item");

        int availableStock = parseIntSafe(stockDetails.get("availableStock"));
        int existingSoldOut = parseIntSafe(stockDetails.get("soldOut"));
        int existingDamaged = parseIntSafe(stockDetails.get("damaged"));

        int soldOut = exchange.getProperty("soldOut", Integer.class);
        int damaged = exchange.getProperty("damaged", Integer.class);

        if (availableStock == 0)
            throw new ProcessException("Zero available stock");
        if ((soldOut + damaged) > availableStock)
            throw new ProcessException("Sold out and damaged exceed available stock");

        stockDetails.put("availableStock", Math.max(0, availableStock - soldOut - damaged));
        stockDetails.put("soldOut", existingSoldOut + soldOut);
        stockDetails.put("damaged", existingDamaged + damaged);

        item.put("stockDetails", stockDetails);
        item.put("lastUpdateDate", LocalDate.now().toString());
        exchange.setProperty("updatedItem", item);

        List<Map<String, Object>> successList = exchange.getProperty("successList", List.class);
        if (successList == null) {
            successList = new ArrayList<>();
            exchange.setProperty("successList", successList);
        }

        Map<String, Object> successItem = new HashMap<>();
        successItem.put("itemId", item.get("_id"));
        successItem.put("availableStock", stockDetails.get("availableStock"));
        successList.add(successItem);

        exchange.getIn().setBody(item);
    }

    // Safely parse integer from object, handling invalid numbers
    private int parseIntSafe(Object value) {
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            throw new ProcessException("Invalid number format in stock details");
        }
    }
}
