# Campaign Manager

A CRUD application for sellers to manage a campaign for each product they sell. Campaign funds are deducted from the seller's Emerald account, and the new balance is shown on screen.

## Tech stack

- **Backend:** Java 21, Spring Boot 4 (Web MVC, Data JPA, Validation), Maven
- **Database:** H2 in-memory, filled with seed data at startup
- **API docs:** Swagger UI (springdoc-openapi)
- **Frontend:** Angular 21, Angular Material, Reactive Forms

## Requirements

- JDK 21, with `JAVA_HOME` pointing to it
- Node.js 20.19+, 22.12+ or 24+

Maven does not need to be installed, because the project includes the Maven Wrapper.

## Running the application

Start the backend in one terminal:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows Command Prompt or PowerShell, use `.\mvnw spring-boot:run`.

Start the frontend in a second terminal:

```bash
cd frontend
npm ci
npm start
```

| What | Address |
|---|---|
| Application | http://localhost:4200 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 console | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:campaigns`, user `sa`, no password) |

The database lives in memory, so every backend restart brings back the seed data and a balance of 10,000.00.

## Running the tests

```bash
cd backend
./mvnw test
```

```bash
cd frontend
npm test -- --watch=false
```

## API

| Method and path | Description |
|---|---|
| `GET /api/campaigns?productId=` | List campaigns, optionally for one product |
| `GET /api/campaigns/{id}` | Get a campaign |
| `POST /api/campaigns` | Create a campaign |
| `PUT /api/campaigns/{id}` | Edit a campaign |
| `DELETE /api/campaigns/{id}` | Delete a campaign |
| `GET /api/account` | Get the Emerald account balance |
| `GET /api/products` | List the seller's products |
| `GET /api/products/{id}` | Get a product |
| `POST /api/products` | Create a product |
| `PUT /api/products/{id}` | Edit a product |
| `DELETE /api/products/{id}` | Delete a product that has no campaigns |
| `GET /api/towns` | List towns |
| `GET /api/keywords?query=` | Suggest keywords |
| `GET /api/config` | Get form settings, such as the minimum bid |

Errors use the Problem Details format (RFC 9457):

| Status | Meaning |
|---|---|
| `400` | Invalid data, with per-field messages in `fieldErrors` |
| `404` | Campaign or product not found |
| `409` | Insufficient funds, a duplicate product name, a product that still has campaigns, or data changed by another request |

## Assumptions

The task left some points open, so these decisions were made:

1. **Campaigns per product:** a product can have many campaigns, and each campaign belongs to one product.
2. **Minimum bid:** no value was given, so it is `0.10`, set in `application.properties` and shared by all campaigns.
3. **Seller:** there is one seller without login, with an Emerald account from the seed data.
4. **Keywords:** they come only from the pre-populated dictionary, and a campaign needs at least one.
5. **Town and radius:** the town is optional, and the radius is required and greater than 0, as the task specifies.
6. **Campaign fund:** it is deducted when a campaign is created. Editing it deducts or refunds the difference, and deleting a campaign refunds the whole fund, because the application does not simulate spending. Changing the status does not affect the balance.
7. **Products:** the task mentions products but gives no fields, so a product has only a name, unique for the seller regardless of case. Sellers can create, edit and delete products, and the application starts with five example products.
8. **Deleting a product:** a product with any campaign cannot be deleted, including a campaign with status OFF, because that campaign still holds funds. The seller deletes its campaigns first, which refunds their funds.

## Design notes

- **Balance consistency:** each balance change runs in one transaction with the campaign change. Optimistic locking (`@Version`) on the account and the campaign rejects concurrent changes with `409`.
- **Adding login later:** services get the current user only through `CurrentSellerProvider`, API paths contain no seller ID, and data queries are scoped to the seller. Adding login means replacing the `CurrentSellerProvider` implementation and adding an HTTP interceptor to the frontend.
