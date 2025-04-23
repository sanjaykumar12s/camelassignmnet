package com.mycart.service.processors;

import com.mycart.service.camelrouter.ProcessException;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component("inventoryUpdates")
public class InventoryUpdates {

    private int parseIntSafe(Object value) {
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            throw new ProcessException("Invalid number format in stock details");
        }
    }

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

    public void extractAndValidateStockFields(Exchange exchange) {
        Map<String, Object> item = exchange.getIn().getBody(Map.class);
        if (item == null || item.get("_id") == null || item.get("stockDetails") == null) {
            throw new ProcessException("Item ID or stock details are missing.");
        }

        String id = item.get("_id").toString();
        Map<String, Object> stock = (Map<String, Object>) item.get("stockDetails");

        int soldOut = parseIntSafe(stock.get("soldOut"));
        int damaged = parseIntSafe(stock.get("damaged"));

        exchange.setProperty("itemId", id);
        exchange.setProperty("soldOut", soldOut);
        exchange.setProperty("damaged", damaged);
    }

    // ===== ASYNC METHODS =====

    public void flattenItems(Exchange exchange) {
        Map<String, Object> body = exchange.getIn().getBody(Map.class);
        Object rawItems = body.get("items");
        List<Map<String, Object>> flatItemList = new ArrayList<>();

        if (rawItems instanceof List<?>) {
            for (Object group : (List<?>) rawItems) {
                if (group instanceof List<?>) {
                    for (Object item : (List<?>) group) {
                        if (item instanceof Map) {
                            flatItemList.add((Map<String, Object>) item);
                        }
                    }
                } else if (group instanceof Map) {
                    flatItemList.add((Map<String, Object>) group);
                }
            }
        }
        exchange.getIn().setBody(flatItemList);
    }

public void computeUnifiedStock(Exchange exchange) {
    Map<String, Object> item = exchange.getIn().getBody(Map.class);
    if (item == null) throw new ProcessException("Item not found in DB.");

    Map<String, Object> stockDetails = (Map<String, Object>) item.get("stockDetails");
    if (stockDetails == null) throw new ProcessException("Stock details are missing for item");

    // Use parseIntSafe for robustness
    int availableStock = parseIntSafe(stockDetails.get("availableStock"));
    int existingSoldOut = parseIntSafe(stockDetails.get("soldOut"));
    int existingDamaged = parseIntSafe(stockDetails.get("damaged"));

    int soldOut = exchange.getProperty("soldOut", Integer.class);
    int damaged = exchange.getProperty("damaged", Integer.class);

    if (availableStock == 0)
        throw new ProcessException("Zero available stock");
    if ((soldOut + damaged) > availableStock)
        throw new ProcessException("Sold out and damaged exceed available stock for item ID: " + item.get("_id"));

    // Update stock values
    stockDetails.put("availableStock", Math.max(0, availableStock - soldOut - damaged));
    stockDetails.put("soldOut", existingSoldOut + soldOut);
    stockDetails.put("damaged", existingDamaged + damaged);
    item.put("stockDetails", stockDetails);
    item.put("lastUpdateDate", LocalDate.now().toString());

    // Always set updatedItem for consistency
    exchange.setProperty("updatedItem", item);

    // Update success list (always, or control with flag if needed)
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


    public void trackSuccess(Exchange exchange) {
        String itemId = exchange.getProperty("itemId", String.class);
        List<Map<String, Object>> successList = exchange.getProperty("successList", List.class);

        Map<String, Object> result = new HashMap<>();
        result.put("itemId", itemId);
        result.put("status", "success");
        result.put("message", "Inventory updated successfully for item " + itemId);
        successList.add(result);
    }

    public void trackFailure(Exchange exchange) {
        String itemId = exchange.getProperty("itemId", String.class);
        String errorMsg = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class).getMessage();
        List<Map<String, Object>> failureList = exchange.getProperty("failureList", List.class);

        Map<String, Object> result = new HashMap<>();
        result.put("itemId", itemId);
        result.put("status", "failure");
        result.put("error", errorMsg);
        failureList.add(result);
    }

    public void saveFinalStatus(Exchange exchange) {
        String requestId = UUID.randomUUID().toString();
        List<Map<String, Object>> successList = exchange.getProperty("successList", List.class);
        List<Map<String, Object>> failureList = exchange.getProperty("failureList", List.class);

        String status;
        if (!successList.isEmpty() && !failureList.isEmpty()) {
            status = "PARTIAL_SUCCESS";
        } else if (!successList.isEmpty()) {
            status = "SUCCESS";
        } else {
            status = "FAILED";
        }

        List<Map<String, Object>> allResults = new ArrayList<>();
        allResults.addAll(successList);
        allResults.addAll(failureList);

        Map<String, Object> resultDoc = new HashMap<>();
        resultDoc.put("_id", requestId);
        resultDoc.put("status", status);
        resultDoc.put("timestamp", LocalDateTime.now().toString());
        resultDoc.put("results", allResults);

        exchange.getContext().createProducerTemplate()
                .sendBody("mongodb:myDb?database=mycartdb&collection=status&operation=save", resultDoc);
    }
}

