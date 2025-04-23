package com.mycart.service.camelrouter;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class InventoryProcessingRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Seda queue to process each item asynchronously
        from("seda:processInventoryUpdate")
                .log("Processing inventory update in Seda queue for item: ${body}")
                .split(simple("${exchangeProperty.inventoryList}")).streaming()
                .doTry()
                    .bean("inventoryUpdates", "extractAndValidateStockFields")
                    .setHeader("CamelMongoDbCriteria", simple("{ \"_id\": \"${exchangeProperty.itemId}\" }"))
                    .to("mongodb:myDb?database=mycartdb&collection=item&operation=findOneByQuery")

                .bean("inventoryUpdates", "computeUnifiedStock")
                    .to("mongodb:myDb?database=mycartdb&collection=item&operation=save")
                    .bean("inventoryUpdates", "trackSuccess")
                .doCatch(Exception.class)
                    .bean("inventoryUpdates", "trackFailure")
                .end()
                .setProperty("errorList", constant(new ArrayList<>())) // Optional reinitialization of errorList
                .bean("inventoryUpdates", "saveFinalStatus");//TODO
    }
}
