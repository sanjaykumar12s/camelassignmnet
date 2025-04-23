//package com.mycart.service.processors;
//
//import com.mycart.service.camelrouter.ProcessException;
//import org.apache.camel.Exchange;
//import org.apache.camel.Processor;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//public class InventoryUpdateProcessor implements Processor {
//    @Override
//    public void process(Exchange exchange) throws ProcessException {
//        Map<String, Object> body = exchange.getIn().getBody(Map.class);
//        if (body == null || !body.containsKey("items") || body.get("items") == null) {
//            throw new ProcessException("Invalid inventory format: 'items' field is missing or empty.");
//        }
//
//        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
//        if (items == null || items.isEmpty()) {
//            throw new ProcessException("Invalid inventory format: 'items' list is missing or empty.");
//        }
//
//        exchange.setProperty("inventoryList", items);
//        exchange.setProperty("errorList", new ArrayList<Map<String, Object>>());
//        exchange.setProperty("successList", new ArrayList<Map<String, Object>>());
//    }
//
//    public void validateAndProcessItem(Exchange exchange) {
//        Map<String, Object> item = exchange.getIn().getBody(Map.class);
//        if (item == null || item.get("_id") == null || item.get("stockDetails") == null) {
//            throw new ProcessException("Item ID or stock details are missing.");
//        }
//
//        String id = item.get("_id").toString();
//        Map<String, Object> stock = (Map<String, Object>) item.get("stockDetails");
//
//        int soldOut;
//        int damaged;
//        try {
//            soldOut = Integer.parseInt(stock.get("soldOut").toString());
//            damaged = Integer.parseInt(stock.get("damaged").toString());
//        } catch (NumberFormatException e) {
//            throw new ProcessException("Invalid numeric format for stock details: soldOut or damaged.");
//        }
//
//        exchange.setProperty("itemId", id);
//        exchange.setProperty("soldOut", soldOut);
//        exchange.setProperty("damaged", damaged);
//    }
//
//    public void processStockDetails(Map<String, Object> stockDetails, int soldOut, int damaged) {
//        int availableStock = Integer.parseInt(stockDetails.get("availableStock").toString());
//        int existingSoldOut = Integer.parseInt(stockDetails.get("soldOut").toString());
//        int existingDamaged = Integer.parseInt(stockDetails.get("damaged").toString());
//
//        if (availableStock == 0) throw new ProcessException("Zero available stock, you cannot update it ");
//        if ((soldOut + damaged) > availableStock) throw new ProcessException("The total of sold out and damaged items cannot exceed the available stock.");
//
//        int newSoldOut = existingSoldOut + soldOut;
//        int newDamaged = existingDamaged + damaged;
//        int newStock = availableStock - soldOut - damaged;
//
//        stockDetails.put("availableStock", Math.max(0, newStock));
//        stockDetails.put("soldOut", newSoldOut);
//        stockDetails.put("damaged", newDamaged);
//    }
//}
