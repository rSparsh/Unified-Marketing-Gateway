package com.project.unifiedMarketingGateway.controllers;

import com.project.unifiedMarketingGateway.repository.SmsMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/sms")
public class SmsStatusController {

    @Autowired
    SmsMessageRepository smsMessageRepository;

    @GetMapping(
            value = "/{sid}",
            produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> getSmsStatus(@PathVariable String sid) {
        return smsMessageRepository.findBySid(sid)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of(
                                        "message", "SMS not found",
                                        "sid", sid
                                ))
                );
    }
}
