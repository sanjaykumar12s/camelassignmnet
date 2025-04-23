//package com.mycart.service.processors;
//
//import org.apache.camel.Exchange;
//import org.apache.camel.Processor;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class FinalResponseProcessor implements Processor {
//    @Override
//    public void process(Exchange exchange) throws Exception {
//        List<Map<String, Object>> errors = exchange.getProperty("errorList", List.class);
//        List<Map<String, Object>> successes = exchange.getProperty("successList", List.class);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "Inventory update completed");
//        response.put("successfulUpdates", successes);
//        response.put("errors", errors);
//
//        exchange.getContext().createProducerTemplate().sendBody("log:finalResponseLog?level=INFO",
//                "Inventory update details:\n" +
//                        "Successful updates: " + successes.size() + "\n" +
//                        "Errors: " + errors.size() + "\n" +
//                        "Successful Items: " + successes + "\n" +
//                        "Error Items: " + errors);
//
//        exchange.getContext().createProducerTemplate().sendBody("log:finalResponseLog?level=DEBUG", response);
//
//        if (!errors.isEmpty()) {
//            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
//        } else {
//            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
//        }
//
//        exchange.getIn().setBody(response);
//        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
//    }
//}
