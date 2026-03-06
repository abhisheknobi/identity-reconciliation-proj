package com.identity_reconciliation.dto;

import lombok.*;

import java.util.List;
import java.util.Set;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IdentityResponse {
    private ContactDetails contact;

    public static class ContactDetails {
        private Integer primaryContactId;
        private Set<String> emails;
        private Set<String> phoneNumbers;
        private List<Integer> secondaryContactIds;

        public Integer getPrimaryContactId() {
            return primaryContactId;
        }

        public Set<String> getEmails() {
            return emails;
        }

        public Set<String> getPhoneNumbers() {
            return phoneNumbers;
        }

        public List<Integer> getSecondaryContactIds() {
            return secondaryContactIds;
        }

        public void setPrimaryContactId(Integer primaryContactId) {
            this.primaryContactId = primaryContactId;
        }

        public void setEmails(Set<String> emails) {
            this.emails = emails;
        }

        public void setPhoneNumbers(Set<String> phoneNumbers) {
            this.phoneNumbers = phoneNumbers;
        }

        public void setSecondaryContactIds(List<Integer> secondaryContactIds) {
            this.secondaryContactIds = secondaryContactIds;
        }
    }
}
