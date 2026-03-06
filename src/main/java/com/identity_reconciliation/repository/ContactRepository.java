package com.identity_reconciliation.repository;

import com.identity_reconciliation.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Integer> {
    // Find any contact that matches either the email or phone number
    List<Contact> findByEmailOrPhoneNumber(String email, String phoneNumber);

    // Find all contacts linked to a specific primary ID
    List<Contact> findByLinkedIdOrId(Integer linkedId, Integer id);
}

