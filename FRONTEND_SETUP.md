# Payment Processing System - Full Stack Setup

This guide explains how to run both the Spring Boot backend and React.js frontend together.

## Prerequisites

- Java 17+
- Maven
- Node.js (v14 or higher)
- npm
- PostgreSQL (running on localhost:5433)
- Redis (running on localhost:6379)
- Kafka (running on localhost:9092)

Or use Docker Compose for infrastructure:
```bash
docker-compose up -d
```

## Backend Setup

1. Navigate to the project root:
```bash
cd /Users/nexusop/Desktop/payment-processing
```

2. Build and run the backend:
```bash
./mvnw spring-boot:run
```

The backend will start at `http://localhost:8080`

## Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the React development server:
```bash
npm start
```

The frontend will open at `http://localhost:3000`

## Running Both Services

### Terminal 1 - Backend:
```bash
cd /Users/nexusop/Desktop/payment-processing
./mvnw spring-boot:run
```

### Terminal 2 - Frontend:
```bash
cd /Users/nexusop/Desktop/payment-processing/frontend
npm start
```

## Application Flow

1. **Register**: Go to `http://localhost:3000/register` to create a new account
2. **Login**: Go to `http://localhost:3000/login` to sign in
3. **Dashboard**: After login, you'll be redirected to the dashboard
4. **Create Payment**: Enter an amount (minimum ₹1.00) to create a payment
5. **View History**: See all your payments with status and details

## Features

### Backend (Spring Boot)
- RESTful API
- JWT Authentication
- Payment processing with idempotency
- Redis caching
- Kafka event publishing
- PostgreSQL database

### Frontend (React.js)
- User authentication (Login/Register)
- Payment creation with idempotency keys
- Payment history with status tracking
- Real-time updates
- Responsive design
- Gradient UI theme

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login
- `POST /api/users` - Register

### Payments
- `POST /api/payments` - Create payment (requires `Idempotency-Key` header)
- `GET /api/payments` - List the authenticated user's payments, newest first
- `GET /api/payments/{id}` - Get payment by ID

## Testing

### Backend Tests:
```bash
./mvnw test
```

### Frontend Tests:
```bash
cd frontend
npm test
```

## Troubleshooting

### CORS Errors
The backend is configured to allow requests from `http://localhost:3000` and `http://localhost:3001`. If you're running on a different port, update `CorsConfig.java`.

### Database Connection
Ensure PostgreSQL is running on port 5433 with the credentials specified in `application.properties`.

### Redis Connection
Ensure Redis is running on localhost:6379.

### Kafka Connection
Ensure Kafka is running on localhost:9092.

## Production Build

### Backend:
```bash
./mvnw clean package
java -jar target/payment-processing-0.0.1-SNAPSHOT.jar
```

### Frontend:
```bash
cd frontend
npm run build
```

The built frontend will be in the `frontend/build` directory.
