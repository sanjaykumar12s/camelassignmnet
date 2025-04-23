//package com.mycart.service.processors;
//
//import com.mycart.service.camelrouter.ProcessException;
//import org.apache.camel.Exchange;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.*;
//
//@Component("asyncInventoryUpdates")
//public class AsynchronousInventoryUpdates {
//
//    public void flattenItems(Exchange exchange) {
//        Map<String, Object> body = exchange.getIn().getBody(Map.class);
//        Object rawItems = body.get("items");
//        List<Map<String, Object>> flatItemList = new ArrayList<>();
//
//        if (rawItems instanceof List<?>) {
//            for (Object group : (List<?>) rawItems) {
//                if (group instanceof List<?>) {
//                    for (Object item : (List<?>) group) {
//                        if (item instanceof Map) {
//                            flatItemList.add((Map<String, Object>) item);
//                        }
//                    }
//                } else if (group instanceof Map) {
//                    flatItemList.add((Map<String, Object>) group);
//                }
//            }
//        }
//        exchange.getIn().setBody(flatItemList);
//    }
//
//    public void extractStockDetails(Exchange exchange) {
//        Map<String, Object> item = exchange.getIn().getBody(Map.class);
//        if (item == null || item.get("_id") == null || item.get("stockDetails") == null) {
//            throw new ProcessException("Item ID or stock details are missing.");
//        }
//
//        String id = item.get("_id").toString();
//        Map<String, Object> stock = (Map<String, Object>) item.get("stockDetails");
//        int soldOut = Integer.parseInt(stock.get("soldOut").toString());
//        int damaged = Integer.parseInt(stock.get("damaged").toString());
//
//        exchange.setProperty("itemId", id);
//        exchange.setProperty("soldOut", soldOut);
//        exchange.setProperty("damaged", damaged);
//    }
//
//    public void computeAndUpdateStock(Exchange exchange) {
//        Map<String, Object> item = exchange.getIn().getBody(Map.class);
//        if (item == null) throw new RuntimeException("Item not found in DB.");
//
//        Map<String, Object> stockDetails = (Map<String, Object>) item.get("stockDetails");
//
//        int availableStock = Integer.parseInt(stockDetails.get("availableStock").toString());
//        int existingSoldOut = Integer.parseInt(stockDetails.get("soldOut").toString());
//        int existingDamaged = Integer.parseInt(stockDetails.get("damaged").toString());
//
//        int soldOut = exchange.getProperty("soldOut", Integer.class);
//        int damaged = exchange.getProperty("damaged", Integer.class);
//
//        if ((soldOut + damaged) > availableStock) {
//            throw new ProcessException("Stock update exceeds available stock for item ID: " + item.get("_id"));
//        }
//
//        stockDetails.put("availableStock", availableStock - soldOut - damaged);
//        stockDetails.put("soldOut", existingSoldOut + soldOut);
//        stockDetails.put("damaged", existingDamaged + damaged);
//        item.put("stockDetails", stockDetails);
//        item.put("lastUpdateDate", LocalDate.now().toString());
//
//        exchange.getIn().setBody(item);
//    }
//
//    public void trackSuccess(Exchange exchange) {
//        String itemId = exchange.getProperty("itemId", String.class);
//        List<Map<String, Object>> successList = exchange.getProperty("successList", List.class);
//
//        Map<String, Object> result = new HashMap<>();
//        result.put("itemId", itemId);
//        result.put("status", "success");
//        result.put("message", "Inventory updated successfully for item " + itemId);
//        successList.add(result);
//    }
//
//    public void trackFailure(Exchange exchange) {
//        String itemId = exchange.getProperty("itemId", String.class);
//        String errorMsg = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class).getMessage();
//        List<Map<String, Object>> failureList = exchange.getProperty("failureList", List.class);
//
//        Map<String, Object> result = new HashMap<>();
//        result.put("itemId", itemId);
//        result.put("status", "failure");
//        result.put("error", errorMsg);
//        failureList.add(result);
//    }
//
//    public void saveFinalStatus(Exchange exchange) {
//        String requestId = UUID.randomUUID().toString();
//        List<Map<String, Object>> successList = exchange.getProperty("successList", List.class);
//        List<Map<String, Object>> failureList = exchange.getProperty("failureList", List.class);
//
//        String status;
//        if (!successList.isEmpty() && !failureList.isEmpty()) {
//            status = "PARTIAL_SUCCESS";
//        } else if (!successList.isEmpty()) {
//            status = "SUCCESS";
//        } else {
//            status = "FAILED";
//        }
//
//        List<Map<String, Object>> allResults = new ArrayList<>();
//        allResults.addAll(successList);
//        allResults.addAll(failureList);
//
//        Map<String, Object> resultDoc = new HashMap<>();
//        resultDoc.put("_id", requestId);
//        resultDoc.put("status", status);
//        resultDoc.put("timestamp", LocalDateTime.now().toString());
//        resultDoc.put("results", allResults);
//
//        exchange.getContext().createProducerTemplate()
//                .sendBody("mongodb:myDb?database=mycartdb&collection=status&operation=save", resultDoc);
//    }
//}
