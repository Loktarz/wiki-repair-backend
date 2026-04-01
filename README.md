# Repair Ticket System - Backend

A Spring Boot REST API for managing repair tickets with JWT-based authentication and PostgreSQL database.

## 🏗️ Tech Stack

- **Framework**: Spring Boot 3.4.1
- **Language**: Java 17
- **Database**: PostgreSQL
- **Security**: Spring Security + JWT (JJWT)
- **ORM**: Spring Data JPA/Hibernate
- **Build Tool**: Maven
- **Dependencies**:
  - Spring Web (REST APIs)
  - Spring Data JPA
  - Spring Security
  - JWT (JJWT)
  - Lombok
  - PostgreSQL Driver

## ✨ Features

- **User Authentication & Authorization**
  - JWT-based token authentication
  - User registration and login
  - Role-based access control

- **Ticket Management**
  - Create, read, update, and delete repair tickets
  - Track ticket status (Open, In Progress, Resolved, Closed)
  - User-specific ticket tracking

- **User Management**
  - User profile management
  - Role management (Admin, Technician, Customer)

- **Security**
  - Password encryption
  - JWT token validation
  - CORS support for frontend integration
  - Global exception handling

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Git

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd repair-ticket-system
