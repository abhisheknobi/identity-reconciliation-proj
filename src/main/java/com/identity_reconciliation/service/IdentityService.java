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

        // 1. Find all potential relatives (matches)
        List<Contact> matches = repository.findByEmailOrPhoneNumber(email, phone);

        // CASE 1: Brand New User
        if (matches.isEmpty()) {
            Contact newPrimary = saveContact(email, phone, null, Contact.LinkPrecedence.primary);
            return formatResponse(newPrimary, List.of(newPrimary));
        }

        // 2. Identify the true primary (the oldest one in the chain)
        Contact truePrimary = matches.stream()
                .map(c -> c.getLinkedId() != null ? repository.findById(c.getLinkedId()).orElse(c) : c)
                .min(Comparator.comparing(Contact::getCreatedAt))
                .orElseThrow();

        // 3. Demote other primaries if they exist in the current matches
        matches.stream()
                .filter(c -> c.getLinkPrecedence() == Contact.LinkPrecedence.primary && !c.getId().equals(truePrimary.getId()))
                .forEach(c -> {
                    c.setLinkPrecedence(Contact.LinkPrecedence.secondary);
                    c.setLinkedId(truePrimary.getId());
                    c.setUpdatedAt(LocalDateTime.now());
                    repository.save(c);
                });

        // 4. Create new secondary if request contains fresh info
        boolean isNewInfo = (email != null && matches.stream().noneMatch(c -> email.equals(c.getEmail()))) ||
                (phone != null && matches.stream().noneMatch(c -> phone.equals(c.getPhoneNumber())));

        if (isNewInfo) {
            saveContact(email, phone, truePrimary.getId(), Contact.LinkPrecedence.secondary);
        }

        // 5. Gather all related contacts and return
        List<Contact> allRelated = repository.findByLinkedIdOrId(truePrimary.getId(), truePrimary.getId());
        return formatResponse(truePrimary, allRelated);
    }

    private Contact saveContact(String e, String p, Integer lId, Contact.LinkPrecedence lp) {
        return repository.save(Contact.builder()
                .email(e).phoneNumber(p).linkedId(lId).linkPrecedence(lp)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    private IdentityResponse formatResponse(Contact primary, List<Contact> related) {
        // LinkedHashSet automatically handles uniqueness and preserves primary order
        Set<String> emails = new LinkedHashSet<>();
        Set<String> phones = new LinkedHashSet<>();

        // Add primary values first to guarantee they are at index 0
        if (primary.getEmail() != null) emails.add(primary.getEmail());
        if (primary.getPhoneNumber() != null) phones.add(primary.getPhoneNumber());

        related.forEach(c -> {
            if (c.getEmail() != null) emails.add(c.getEmail());
            if (c.getPhoneNumber() != null) phones.add(c.getPhoneNumber());
        });

        List<Integer> secondaryIds = related.stream()
                .map(Contact::getId)
                .filter(id -> !id.equals(primary.getId()))
                .toList();

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
