package com.identity_reconciliation.service;

import com.identity_reconciliation.dto.IdentityRequest;
import com.identity_reconciliation.dto.IdentityResponse;
import com.identity_reconciliation.entity.Contact;
import com.identity_reconciliation.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IdentityService {
    @Autowired
    private ContactRepository repository;

    public IdentityResponse reconcile(IdentityRequest request) {
        String email = request.getEmail();
        String phone = request.getPhoneNumber();

        // 1. Fetch all potentially related contacts
        List<Contact> matches = repository.findByEmailOrPhoneNumber(email, phone);

        // CASE 1: No matches found - Create new Primary contact
        if (matches.isEmpty()) {
            Contact newPrimary = createContact(email, phone, null, Contact.LinkPrecedence.primary);
            return formatResponse(newPrimary, Collections.emptyList());
        }

        // 2. Identify all Primary contacts involved in the matches
        Set<Integer> primaryIds = matches.stream()
                .map(c -> c.getLinkedId() != null ? c.getLinkedId() : c.getId())
                .collect(Collectors.toSet());

        List<Contact> allPrimaries = repository.findAllById(primaryIds).stream()
                .sorted(Comparator.comparing(Contact::getCreatedAt))
                .collect(Collectors.toList());

        Contact truePrimary = allPrimaries.get(0); // The oldest one is always the true primary [cite: 26]

        // CASE 2: Merging two existing Primary contacts
        if (allPrimaries.size() > 1) {
            for (int i = 1; i < allPrimaries.size(); i++) {
                Contact otherPrimary = allPrimaries.get(i);
                otherPrimary.setLinkPrecedence(Contact.LinkPrecedence.secondary);
                otherPrimary.setLinkedId(truePrimary.getId());
                otherPrimary.setUpdatedAt(LocalDateTime.now());
                repository.save(otherPrimary);

                // Also update any secondaries that were pointing to the now-demoted primary
                List<Contact> orphanSecondaries = repository.findByLinkedIdOrId(otherPrimary.getId(), otherPrimary.getId());
                for(Contact s : orphanSecondaries) {
                    s.setLinkedId(truePrimary.getId());
                    repository.save(s);
                }
            }
        }

        // CASE 3: New information provided (e.g., new email for existing phone)
        boolean isNewEmail = email != null && matches.stream().noneMatch(c -> email.equals(c.getEmail()));
        boolean isNewPhone = phone != null && matches.stream().noneMatch(c -> phone.equals(c.getPhoneNumber()));

        if (isNewEmail || isNewPhone) {
            createContact(email, phone, truePrimary.getId(), Contact.LinkPrecedence.secondary);
        }

        // 3. Consolidate data for the final response
        List<Contact> allRelated = repository.findByLinkedIdOrId(truePrimary.getId(), truePrimary.getId());
        return formatResponse(truePrimary, allRelated);
    }

    private Contact createContact(String email, String phone, Integer linkedId, Contact.LinkPrecedence precedence) {
        Contact contact = new Contact();
        contact.setEmail(email);
        contact.setPhoneNumber(phone);
        contact.setLinkedId(linkedId);
        contact.setLinkPrecedence(precedence);
        contact.setCreatedAt(LocalDateTime.now());
        contact.setUpdatedAt(LocalDateTime.now());
        return repository.save(contact);
    }

    private IdentityResponse formatResponse(Contact primary, List<Contact> allRelated) {
        IdentityResponse response = new IdentityResponse();
        IdentityResponse.ContactDetails details = new IdentityResponse.ContactDetails();

        details.setPrimaryContactId(primary.getId());

        // Ensure primary contact info is first in arrays [cite: 48, 50]
        Set<String> emails = new LinkedHashSet<>();
        emails.add(primary.getEmail());
        allRelated.forEach(c -> { if(c.getEmail() != null) emails.add(c.getEmail()); });

        Set<String> phones = new LinkedHashSet<>();
        phones.add(primary.getPhoneNumber());
        allRelated.forEach(c -> { if(c.getPhoneNumber() != null) phones.add(c.getPhoneNumber()); });

        List<Integer> secondaryIds = allRelated.stream()
                .map(Contact::getId)
                .filter(id -> !id.equals(primary.getId()))
                .collect(Collectors.toList());

        details.setEmails(emails);
        details.setPhoneNumbers(phones);
        details.setSecondaryContactIds(secondaryIds);
        response.setContact(details);
        return response;
    }

}
