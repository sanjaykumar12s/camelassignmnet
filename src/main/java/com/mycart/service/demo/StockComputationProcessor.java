//package com.mycart.service.processors;
//
//import com.mycart.service.camelrouter.ProcessException;
//import org.apache.camel.Exchange;
//import org.apache.camel.Processor;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class StockComputationProcessor implements Processor {
//    @Override
//    public void process(Exchange exchange) throws Exception {
//        Map<String, Object> item = exchange.getIn().getBody(Map.class);
//
//        if (item == null) {
//            exchange.getContext().createProducerTemplate().sendBody("log:ERROR", "Item is null in StockComputationProcessor");
//            throw new ProcessException("no item found there ");
//        }
//
//        Map<String, Object> stockDetails = (Map<String, Object>) item.get("stockDetails");
//
//        if (stockDetails == null) {
//            exchange.getContext().createProducerTemplate().sendBody("log:ERROR", "Stock details are missing for item");
//            throw new ProcessException("Stock details are missing for item");
//        }
//
//        int availableStock;
//        int existingSoldOut;
//        int existingDamaged;
//
//        try {
//            availableStock = Integer.parseInt(stockDetails.get("availableStock").toString());
//            existingSoldOut = Integer.parseInt(stockDetails.get("soldOut").toString());
//            existingDamaged = Integer.parseInt(stockDetails.get("damaged").toString());
//        } catch (NumberFormatException e) {
//            exchange.getContext().createProducerTemplate().sendBody("log:ERROR", "Invalid number format in stock details");
//            throw new ProcessException("Invalid number format in stock details");
//        }
//
//        if (availableStock == 0) {
//            throw new ProcessException("Zero available stock");
//        }
//
//        int soldOut = exchange.getProperty("soldOut", Integer.class);
//        int damaged = exchange.getProperty("damaged", Integer.class);
//
//        if ((soldOut + damaged) > availableStock) {
//            throw new ProcessException("The total of sold out and damaged items cannot exceed the available stock.");
//        }
//
//        int newSoldOut = existingSoldOut + soldOut;
//        int newDamaged = existingDamaged + damaged;
//        int newStock = availableStock - soldOut - damaged;
//
//        stockDetails.put("availableStock", Math.max(0, newStock));
//        stockDetails.put("soldOut", newSoldOut);
//        stockDetails.put("damaged", newDamaged);
//
//        item.put("stockDetails", stockDetails);
//        item.put("lastUpdateDate", LocalDate.now().toString());
//
//        exchange.setProperty("updatedItem", item);
//
//        List<Map<String, Object>> successList = exchange.getProperty("successList", List.class);
//        if (successList == null) {
//            successList = new ArrayList<>();
//            exchange.setProperty("successList", successList);
//        }
//
//        Map<String, Object> successItem = new HashMap<>();
//        successItem.put("itemId", item.get("_id"));
//        successItem.put("availableStock", stockDetails.get("availableStock"));
//
//        successList.add(successItem);
//
//        exchange.getIn().setBody(item);
//    }
//}
