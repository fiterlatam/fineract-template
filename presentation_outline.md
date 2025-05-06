# Apache Fineract Technical Walkthrough

## 1. Introduction
- Overview of Apache Fineract
- Purpose and target audience
- Key features and capabilities

## 2. Tech Stack
### 2.1 Backend Technologies
- Java 17 (Core programming language)
- Spring Boot 3.2.x (Application framework)
- Spring Data JPA/EclipseLink (ORM)
- Spring Security (Authentication and authorization)
- Liquibase (Database migration)
- Resilience4j (Fault tolerance)

### 2.2 Database
- MySQL/MariaDB and PostgreSQL support
- Multi-tenancy architecture
- Database schema overview

### 2.3 API & Documentation
- RESTful API design
- OpenAPI/Swagger for API documentation
- JSON for data exchange

### 2.4 Build & Deployment
- Gradle (Build tool)
- Docker/Jib (Containerization)
- CI/CD with GitHub Actions

### 2.5 Other Technologies
- Avro (Data serialization)
- Kafka/ActiveMQ (Event messaging)
- Spring Batch (Batch processing)

## 3. Swagger Documentation Overview
### 3.1 API Structure
- Authentication and security
- Resource organization
- Common patterns and conventions

### 3.2 Key API Endpoints
#### 3.2.1 Client APIs
- Client management
- Client identification
- Client images and documents

#### 3.2.2 Loan APIs
- Loan products
- Loan applications
- Loan disbursement
- Loan repayments
- Loan charges
- Loan schedule

#### 3.2.3 Savings APIs
- Savings products
- Savings accounts
- Savings transactions
- Interest posting

#### 3.2.4 Accounting APIs
- GL accounts
- Journal entries
- Accounting closures

#### 3.2.5 Webhook APIs
- Event types
- Webhook registration
- Webhook delivery

## 4. Codebase Structure
### 4.1 Module Organization
- fineract-core: Core domain models and utilities
- fineract-accounting: Accounting functionality
- fineract-loan: Loan management
- fineract-savings: Savings account management
- fineract-provider: Main application and API endpoints
- fineract-client: Client SDK
- fineract-avro-schemas: Data serialization schemas

### 4.2 Package Structure
- org.apache.fineract.infrastructure: Cross-cutting concerns
- org.apache.fineract.portfolio: Business domain
- org.apache.fineract.accounting: Accounting functionality
- org.apache.fineract.organisation: Organizational structure

### 4.3 Key Design Patterns
- Domain-Driven Design
- Repository pattern
- Service layer pattern
- Command pattern
- Event-driven architecture

## 5. Deep Dive into Key Classes
### 5.1 Loan Management
- Loan class: Core domain entity for loans
- LoanTransaction: Loan transaction processing
- LoanRepaymentSchedule: Installment generation and management
- LoanWritePlatformService: Business operations for loans
- LoanTransactionProcessor: Different repayment strategies

### 5.2 Savings Management
- SavingsAccount: Core domain entity for savings
- SavingsAccountTransaction: Savings transaction processing
- SavingsAccountInterestPostingService: Interest calculation and posting
- SavingsAccountWritePlatformService: Business operations for savings

### 5.3 Job Scheduling System
- JobSchedulerServiceImpl: Job scheduling and execution
- ScheduledJobDetail: Job configuration
- JobRegisterService: Job registration and management
- Batch processing architecture

### 5.4 Event System
- BusinessEventNotifierService: Internal event notification
- ExternalEventService: External event publishing
- Event types and handlers

## 6. Multi-tenancy Architecture
- Tenant management
- Database per tenant
- Tenant context switching
- Tenant-aware services

## 7. Security Architecture
- Authentication mechanisms
- Authorization and permissions
- Data security
- API security

## 8. Extension Points
- Custom fields
- Hooks
- Plugins
- Integration points

## 9. Development Workflow
- Setting up development environment
- Building and testing
- Debugging tips
- Common issues and solutions

## 10. Conclusion
- Key takeaways
- Resources for further learning
- Q&A