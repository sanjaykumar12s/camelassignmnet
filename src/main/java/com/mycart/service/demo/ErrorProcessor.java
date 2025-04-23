//package com.mycart.service.processors;
//
//import com.mycart.service.camelrouter.ProcessException;
//import org.apache.camel.Exchange;
//import org.apache.camel.Processor;
//import org.bson.Document;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class ErrorProcessor implements Processor {
//    @Override
//    public void process(Exchange exchange) throws Exception {
//        ProcessException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, ProcessException.class);
//        List<Document> errorList = exchange.getProperty("errorList", List.class);
//        if (errorList == null) {
//            errorList = new ArrayList<>();
//            exchange.setProperty("errorList", errorList);
//        }
//
//        Document error = new Document()
//                .append("itemId", exchange.getProperty("itemId"))
//                .append("message", exception.getMessage());
//
//        errorList.add(error);
//
//        exchange.setProperty("skipUpdate", true);
//    }
//}
