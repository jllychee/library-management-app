# Library Management System

## Project Overview

This Library Management System is a comprehensive Spring Boot application designed to manage library operations. It provides functionalities for book management, user administration, and borrowing operations with secure authentication and authorization mechanisms.

![UML Diagram](https://github.com/user-attachments/assets/61410dfe-da9c-49d0-b873-a1cd93a0876f)

### Key Features

- **User Management**:
  - User registration and authentication
  - Role-based access control (LIBRARIAN and PATRON roles)
  - User profile management

- **Book Management**:
  - Add, update, delete, and search books
  - Book categorization by genre
  - Track book availability

- **Reactive Operations**:
  - Stream book availability updates using WebFlux

- **Borrowing Operations**:
  - Book checkout and return
  - Due date tracking
  - Overdue book management
  - Borrowing history for users

- **Automated Overdue Fine System**:
  - Automatic daily fine calculation for overdue, unreturned books
  - Fine records linked to users, books, and borrowing records
  - User fine history and librarian-wide fine lookup

- **Email Notification Service**:
  - Registration confirmation emails
  - Overdue reminder emails
  - Book availability emails for waitlisted books
  - Asynchronous delivery that does not interrupt the main system flow

- **Borrowing History-Based Book Recommendation**:
  - Recommendations based on previously borrowed genres and authors
  - Excludes previously borrowed and unavailable books
  - Returns up to five relevant books

- **Reporting**:
  - Overdue books reports in text and PDF formats
  - Book availability tracking
  - User borrowing history

- **Seamless Authentication Flow**:
  - JWT access-token authentication
  - Refresh tokens for obtaining new access tokens without logging in again
  - Refresh-token revocation during logout
  - Role-based access control
  - API endpoint protection

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.3.2
- **Database**: PostgreSQL 16
- **Security**: Spring Security, JWT (JSON Web Tokens)
- **API Documentation**: Swagger/OpenAPI 3
- **Build Tool**: Maven
- **Testing**: JUnit 5, Spring Test
- **Containerization**: Docker, Docker Compose
- **PDF Generation**: iText 7
- **Other Technologies**:
  - Spring Data JPA
  - Lombok
  - MapStruct
  - Spring WebFlux (Reactive Programming)
  - Validation API

## Running the Application Locally

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- PostgreSQL 16 (or Docker for containerized setup)

### Database Setup

1. Create a PostgreSQL database:
   ```sql
   CREATE DATABASE library_management;
   ```

2. Configure database connection in `src/main/resources/application.yml` or use environment variables:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/library_management
       username: postgres
       password: password
   ```

### Building and Running the Application

1. Clone the repository:
   ```bash
   git clone https://github.com/ilhanozkan/library-management-app.git
   cd library-management-app
   ```

2. Build the application:
   ```bash
   ./mvnw clean package
   ```

3. Run the application:
   ```bash
   java -jar target/libraryManagementSystem-0.0.1-SNAPSHOT.jar
   ```

4. Access the application at: http://localhost:8080/api/v1/

## Docker Setup and Run Instructions

The application can be easily deployed using Docker and Docker Compose.

1. Build Docker images
   ```bash
   docker-compose build
   ```

2. To run in the background:
   ```bash
   docker-compose up -d
   ```

3. To stop the containers:
   ```bash
   docker-compose down
   ```

Docker Compose will set up:
- A PostgreSQL 16 database container
- The Spring Boot application container
- Proper networking between containers
- Volume mounting for database persistence
- Volume for logs storage

## API Endpoints

![Image](https://github.com/user-attachments/assets/bc43a0bb-02f9-48a7-b2d5-1ddb3ffd4b2e)

![Image](https://github.com/user-attachments/assets/c9238a14-a28d-4320-84a6-da7d60a5a655)

![Image](https://github.com/user-attachments/assets/07c66635-a5d0-4e9f-815a-52a46267e78b)

![Image](https://github.com/user-attachments/assets/e777e52d-da0b-4b1f-a08b-05382a59f8e4)

![Image](https://github.com/user-attachments/assets/e5619453-f35f-4cc6-bcb6-05239bbbc07d)

![Image](https://github.com/user-attachments/assets/a43f0459-193c-4975-8650-21772b91cc23)

![Image](https://github.com/user-attachments/assets/d910e33e-3331-48f8-8958-1cf392fbc039)

![Image](https://github.com/user-attachments/assets/68407866-48d7-4e71-a495-72696d71f3e3)

![Image](https://github.com/user-attachments/assets/5f3c0136-19bd-406c-bb68-c9873036cf0c)

![Image](https://github.com/user-attachments/assets/0a8fd349-d3ad-41b7-8c32-246979cb7d93)

![Image](https://github.com/user-attachments/assets/9e71f116-594e-409f-82b1-683da20bf1ab)

![Image](https://github.com/user-attachments/assets/5818d399-ad00-408c-a46a-826f77bda473)

![Image](https://github.com/user-attachments/assets/1fe56000-0c12-4c02-8e60-3d7eb03e7c72)

![Image](https://github.com/user-attachments/assets/01f9a583-9e57-4ad5-9423-df1a1c9c9111)

![Image](https://github.com/user-attachments/assets/37c8ccfe-4eaa-4762-9bcd-562cdff59d6b)

![Image](https://github.com/user-attachments/assets/a5613b3e-56bc-4056-a560-60a4c9e67bbb)

The application provides a RESTful API with the following main endpoints:

### Authentication
- `POST /auth/register`: Register a new user
- `POST /auth/login`: Authenticate and get JWT access and refresh tokens
- `POST /auth/refresh-token`: Get a new access token using a valid refresh token
- `POST /auth/logout`: Revoke the refresh token during logout

### Book Management
- `GET /books`: Get all books with pagination
- `GET /books/search`: Search books by title, author, ISBN, or genre
- `GET /books/{id}`: Get book by ID
- `GET /books/isbn/{isbn}`: Get book by ISBN
- `POST /books`: Create a new book (LIBRARIAN role)
- `PUT /books/{id}`: Update a book (LIBRARIAN role)
- `PUT /books/{id}/available-quantity`: Update a book's available quantity (LIBRARIAN role)
- `DELETE /books/{id}`: Delete a book (LIBRARIAN role)

### Enum Operations
- `GET /enums/book-genres`: Get all book genres

### Reactive Operations
- `GET /reactive/books/availability/stream`: Stream book availability updates

### User Operations
- `GET /users`: Get all users with pagination (LIBRARIAN role)
- `GET /users/{id}`: Get user by ID (LIBRARIAN role)
- `GET /users/email/{email}`: Get user by email (LIBRARIAN role)
- `PUT /users/{id}`: Update user (LIBRARIAN role)
- `DELETE /users/{id}`: Delete user (LIBRARIAN role)
- `PUT /users/{id}/deactivate`: Deactivate user (LIBRARIAN role)

### Borrowing Operations
- `GET /borrowings`: Get all borrowings (LIBRARIAN role)
- `GET /borrowings/user/{userId}`: Get borrowings by user ID (LIBRARIAN role)
- `GET /borrowings/user/{userId}/active`: Get active borrowings by user ID (LIBRARIAN role)
- `GET /borrowings/my-history`: Get authenticated user's borrowing history
- `GET /borrowings/my-active`: Get authenticated user's active borrowings
- `POST /borrowings`: Create a new borrowing (for patrons)
- `POST /borrowings/librarian`: Create a new borrowing (for librarians)
- `PUT /borrowings/{id}/return`: Return a book
- `DELETE /borrowings/{id}`: Delete a borrowing (LIBRARIAN role)
- `GET /borrowings/overdue-report`: Generate text report of overdue books (LIBRARIAN role)
- `GET /borrowings/overdue-pdf-report`: Generate PDF report of overdue books (LIBRARIAN role)

### Fine Operations
- `GET /fines`: Librarian views all fine records
- `GET /fines/my`: Authenticated patron views their own fine records

### Waitlist Operations
- `POST /waitlists`: Authenticated patron joins a book waitlist
- `GET /waitlists/mine`: Authenticated patron views their own waitlist entries
- `DELETE /waitlists/{id}`: Remove the authenticated patron's own waitlist entry

### Recommendations
- `GET /recommendations/user/{userId}`: Get book recommendations based on borrowing history

For a complete API reference, access the Swagger documentation at: http://localhost:8080/api/v1/swagger-ui/index.html

## Additional Information

### Default Users

The application initializes with the following default users:
- **LIBRARIAN**: username: `librarian`, password: `password`
- **PATRON**: username: `patron`, password: `password`

### JWT Authentication

Login through `/auth/login` returns `token`, `accessToken`, `refreshToken`, `username`, and `role`. The `token` field is kept as an alias of `accessToken` for compatibility.

Use `accessToken` for protected API requests by passing it in the Authorization header:
```
Authorization: Bearer <accessToken>
```

When the access token expires, send `refreshToken` to `/auth/refresh-token` to obtain a new access token:
```json
{
  "refreshToken": "refresh-token-value"
}
```

To log out, send the refresh token to `/auth/logout`. Logout revokes the refresh token, and a revoked token cannot be reused. Refresh tokens are stored server-side and cannot be used as bearer tokens for protected APIs.

### Data Initialization

The application includes a data initializer that populates the database with sample books, users, and borrowings on first startup.

### Automated Fine Calculation

The application runs a scheduled task every day at midnight (`Asia/Kuala_Lumpur`) to calculate fines for overdue, unreturned books. The default fine rate is configured in `src/main/resources/application.yml`:

```yaml
library:
  fines:
    daily-rate: 1.00
```

Fine records are stored once per borrowing record and updated on later runs, so the scheduled task does not create duplicate fines for the same borrowing.

### Postman Collection

Click to go to [Online Postman Collection Link](https://www.postman.com/flight-geoscientist-10994860/library-management-system/collection/20492318-ec6da3a3-fb40-4818-850e-d575b8a63d32/?action=share&creator=20492318)

A Postman collection is included in the repository (`library_management_system_postman_collection.json`) that contains sample requests for testing the API endpoints.

## Email Notifications

The system sends registration confirmation, overdue reminder, and waitlisted-book availability emails. Email sending is asynchronous and decoupled from the main request flow. Delivery failures are logged without stopping registration, borrowing, returning, or other system operations.

| Event | Trigger | Recipient |
|---|---|---|
| Registration confirmation | `POST /auth/register` commits | The new user |
| Book became available | A book's available quantity goes from 0 to >0 (return, restock, quantity update) | Every patron on that book's waitlist (`POST /waitlists`) |
| Overdue reminder | Daily scheduled scan (default 08:00, configurable via `notification.overdue-cron`) | Each patron with overdue books (one digest per user, deduplicated via `borrowings.last_overdue_notified_at`) |

### Configuring SMTP

Emails are sent via Gmail SMTP. Set the following environment variables before starting the app:

```bash
export GMAIL_USERNAME="you@gmail.com"
export GMAIL_APP_PASSWORD="your-16-char-app-password"
```

(Gmail requires an **App Password**, not your account password — see [Google Account → App passwords](https://myaccount.google.com/apppasswords).)

When these variables are **not** set, the application still boots and all other functionality works normally; outbound mail simply fails soft (logged at `WARN`, never propagated to callers). The cron schedule and remind interval are tunable via `notification.overdue-cron` and `notification.overdue-remind-days` in `application.yml`.

## TODOs / Future Improvements

- Implement a front-end application using React
- Add comprehensive monitoring
- Implement caching for frequently accessed data
- Add support for digital books and e-lending
- CSRF protection for all forms

## License

This project is licensed under the Apache 2.0 License.
