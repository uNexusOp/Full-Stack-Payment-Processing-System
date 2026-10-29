# Payment Processing Frontend

React.js frontend for the Payment Processing System.

## Prerequisites

- Node.js (v14 or higher)
- npm or yarn

## Installation

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

## Running the Application

1. Start the React development server:
```bash
npm start
```

The application will open at `http://localhost:3000`

## Features

- **User Authentication**: Login and registration with JWT tokens
- **Payment Creation**: Create new payments with idempotency keys
- **Payment History**: View the signed-in user's payments from the backend
- **Payment Status**: Track pending, processing, successful, and failed payments
- **Refresh**: Reload payment history and updated statuses from the backend
- **Responsive Design**: Works on desktop and mobile devices

## API Integration

The frontend communicates with the Spring Boot backend at `http://localhost:8080` by default. Set `REACT_APP_API_URL` to override the API base URL.

## Environment Variables

Create a `.env` file in the frontend directory:

```
REACT_APP_API_URL=http://localhost:8080
```

## Project Structure

```
src/
├── components/     # Reusable components
├── context/        # React Context (Auth)
├── pages/          # Page components
│   ├── Login.js
│   ├── Register.js
│   ├── Dashboard.js
│   ├── CreatePayment.js
│   └── PaymentHistory.js
├── services/       # API services
│   └── api.js
├── utils/          # Utility functions
├── App.js          # Main app component
└── index.js        # Entry point
```

## Available Scripts

- `npm start` - Runs the app in development mode
- `npm test` - Launches the test runner
- `npm run build` - Builds the app for production
