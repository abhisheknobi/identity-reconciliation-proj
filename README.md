# FluxKart Identity Reconciliation Service

This is a Spring Boot implementation of the **Bitespeed Backend Task: Identity Reconciliation**. The service helps FluxKart.com link different orders made with different contact information (email or phone number) to the same customer profile.

## 🚀 Live Demo
* **API Endpoint:** `https://identity-reconciliation-proj.onrender.com/identify`

## 🛠️ Tech Stack
* **Java 17**
* **Spring Boot 3.x**
* **Spring Data JPA** (for relational database management)
* **MySQL** (or any SQL database)
* **Lombok** (for boilerplate reduction)

## 🏗️ Core Logic
The service implements a reconciliation algorithm based on the following rules:
1.  **New Identity:** If the email/phone doesn't match any existing records, a new "primary" contact is created.
2.  **Secondary Identity:** If a request matches an existing email or phone but introduces new information, a "secondary" contact is created and linked to the oldest primary.
3.  **Primary to Secondary Conversion:** If a request contains an email and phone number that belong to two *different* primary contacts, the newer primary contact (and its descendants) are converted to "secondary" and linked to the oldest primary.

## 🔌 API Documentation

### **Identity Reconciliation**
Consolidate contact information based on provided email and/or phone number.

* **URL:** `/identify`
* **Method:** `POST`
* **Content-Type:** `application/json`

#### **Request Body**
```json
{
  "email": "mcfly@hillvalley.edu",
  "phoneNumber": "123456"
}