package com.identity_reconciliation.repository;

import com.identity_reconciliation.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Integer> {
    // Find any contact that matches either the email or phone number
    // Finds any record with matching email or phone
    List<Contact> findByEmailOrPhoneNumber(String email, String phoneNumber);

    // Finds all records belonging to a specific primary chain
    List<Contact> findByLinkedIdOrId(Integer linkedId, Integer id);

    // Finds records specifically linked to a primary ID
    List<Contact> findByLinkedId(Integer linkedId);
}
