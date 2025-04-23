//package com.mycart.service.processors;
//
//import com.mycart.service.camelrouter.ProcessException;
//import org.apache.camel.Exchange;
//import org.apache.camel.Processor;
//
//import java.util.Map;
//
//public class InventoryValidationProcessor implements Processor {
//    @Override
//    public void process(Exchange exchange) throws Exception {
//        Map<String, Object> item = exchange.getIn().getBody(Map.class);
//        if (item == null || item.get("_id") == null || item.get("stockDetails") == null) {
//            throw new ProcessException("Item ID or stock details are missing.");
//        }
//
//        String id = item.get("_id").toString();
//        Map<String, Object> stock = (Map<String, Object>) item.get("stockDetails");
//
//        try {
//            int soldOut = Integer.parseInt(stock.get("soldOut").toString());
//            int damaged = Integer.parseInt(stock.get("damaged").toString());
//            exchange.setProperty("itemId", id);
//            exchange.setProperty("soldOut", soldOut);
//            exchange.setProperty("damaged", damaged);
//        } catch (NumberFormatException e) {
//            throw new ProcessException("Invalid numeric format for stock details: soldOut or damaged.");
//        }
//    }
//}
