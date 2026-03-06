package com.identity_reconciliation.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class IdentityRequest {
    private String email;
    private String phoneNumber;
}
