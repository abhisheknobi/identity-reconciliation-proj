package com.identity_reconciliation.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IdentityResponse {
    private ContactDetails contact;

    @Builder
    @Getter
    @Setter
    public static class ContactDetails {
        private Integer primaryContactId;
        private ArrayList<String> emails;
        private ArrayList<String> phoneNumbers;
        private List<Integer> secondaryContactIds;


    }
}
