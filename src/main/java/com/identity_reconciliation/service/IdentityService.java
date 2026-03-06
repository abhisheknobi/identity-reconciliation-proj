package com.identity_reconciliation.service;

import com.identity_reconciliation.dto.IdentityRequest;
import com.identity_reconciliation.dto.IdentityResponse;
import com.identity_reconciliation.entity.Contact;
import com.identity_reconciliation.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentityService {
    private final ContactRepository repository;

    @Transactional
    public IdentityResponse reconcile(IdentityRequest request) {
        String email = request.getEmail();
        String phone = request.getPhoneNumber();

        // 1. Fetch matching contacts [cite: 27]
        List<Contact> matches = repository.findByEmailOrPhoneNumber(email, phone);

        // CASE: New User [cite: 89]
        if (matches.isEmpty()) {
            Contact newPrimary = saveContact(email, phone, null, Contact.LinkPrecedence.primary);
            return formatResponse(newPrimary, Collections.emptyList());
        }

        // 2. Identify all involved Primaries [cite: 26]
        Set<Integer> primaryIds = matches.stream()
                .map(c -> c.getLinkedId() != null ? c.getLinkedId() : c.getId())
                .collect(Collectors.toSet());

        List<Contact> allPrimaries = repository.findAllById(primaryIds).stream()
                .sorted(Comparator.comparing(Contact::getCreatedAt))
                .collect(Collectors.toList());

        Contact truePrimary = allPrimaries.get(0); // Oldest is primary [cite: 26]

        // 3. Handle Merging (Primary to Secondary) [cite: 144]
        if (allPrimaries.size() > 1) {
            for (int i = 1; i < allPrimaries.size(); i++) {
                Contact newerPrimary = allPrimaries.get(i);
                if (!newerPrimary.getId().equals(truePrimary.getId())) {
                    newerPrimary.setLinkPrecedence(Contact.LinkPrecedence.secondary);
                    newerPrimary.setLinkedId(truePrimary.getId());
                    newerPrimary.setUpdatedAt(LocalDateTime.now());
                    repository.save(newerPrimary);

                    // Re-link children of the demoted primary [cite: 24]
                    List<Contact> children = repository.findByLinkedId(newerPrimary.getId());
                    children.forEach(c -> {
                        c.setLinkedId(truePrimary.getId());
                        repository.save(c);
                    });
                }
            }
        }

        boolean isNewEmail = email != null && matches.stream().noneMatch(c -> email.equals(c.getEmail()));
        boolean isNewPhone = phone != null && matches.stream().noneMatch(c -> phone.equals(c.getPhoneNumber()));

        if (isNewEmail || isNewPhone) {
            saveContact(email, phone, truePrimary.getId(), Contact.LinkPrecedence.secondary);
        }

        List<Contact> allRelated = repository.findByLinkedIdOrId(truePrimary.getId(), truePrimary.getId());
        return formatResponse(truePrimary, allRelated);
    }

    private Contact saveContact(String e, String p, Integer lId, Contact.LinkPrecedence lp) {
        return repository.save(Contact.builder()
                .email(e).phoneNumber(p).linkedId(lId).linkPrecedence(lp)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    private IdentityResponse formatResponse(Contact primary, List<Contact> related) {
        // LinkedHashSet keeps the primary info at the first position 
        Set<String> emails = new LinkedHashSet<>();
        if (primary.getEmail() != null) emails.add(primary.getEmail());
        related.stream().map(Contact::getEmail).filter(Objects::nonNull).forEach(emails::add);

        Set<String> phones = new LinkedHashSet<>();
        if (primary.getPhoneNumber() != null) phones.add(primary.getPhoneNumber());
        related.stream().map(Contact::getPhoneNumber).filter(Objects::nonNull).forEach(phones::add);

        List<Integer> secondaryIds = related.stream()
                .map(Contact::getId)
                .filter(id -> !id.equals(primary.getId()))
                .collect(Collectors.toList());

        return IdentityResponse.builder()
                .contact(IdentityResponse.ContactDetails.builder()
                        .primaryContactId(primary.getId())
                        .emails(new ArrayList<>(emails))
                        .phoneNumbers(new ArrayList<>(phones))
                        .secondaryContactIds(secondaryIds)
                        .build())
                .build();
    }
}