package com.identity_reconciliation.controller;

import com.identity_reconciliation.dto.IdentityRequest;
import com.identity_reconciliation.dto.IdentityResponse;
import com.identity_reconciliation.service.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
class IdentityController {
    @Autowired
    private IdentityService identityService;

    /**
     * Endpoint for identity reconciliation.
     * Receives email and/or phoneNumber and returns a consolidated contact.
     */
    @PostMapping("/identify")
    public ResponseEntity<IdentityResponse> identify(@RequestBody IdentityRequest request) {

        IdentityResponse response = identityService.reconcile(request);
        return ResponseEntity.ok(response);
    }

}
