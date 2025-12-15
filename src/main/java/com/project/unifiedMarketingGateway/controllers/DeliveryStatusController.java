package com.project.unifiedMarketingGateway.controllers;

import com.project.unifiedMarketingGateway.metrics.dto.DeliveryStatusResponse;
import com.project.unifiedMarketingGateway.processor.DeliveryStatusQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/status")
public class DeliveryStatusController {

    @Autowired
    DeliveryStatusQueryService queryService;

    @GetMapping(
            value = "/{requestId}",
            produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE
    )
    public DeliveryStatusResponse getStatus(
            @PathVariable String requestId
    ) {
        return queryService.getStatus(requestId);
    }
}