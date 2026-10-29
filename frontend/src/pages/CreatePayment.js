import React, { useState } from 'react';
import { paymentAPI } from '../services/api';
import './CreatePayment.css';

const CreatePayment = ({ onPaymentCreated }) => {
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);

    const amountValue = parseFloat(amount);

    if (isNaN(amountValue) || amountValue < 1) {
      setError('Amount must be at least 1.00');
      return;
    }

    setLoading(true);

    try {
      const idempotencyKey = `payment-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
      const response = await paymentAPI.createPayment(amountValue, idempotencyKey);

      setSuccess(true);
      setAmount('');

      if (onPaymentCreated) {
        onPaymentCreated(response.data);
      }

      setTimeout(() => setSuccess(false), 3000);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to create payment');
    }

    setLoading(false);
  };

  return (
    <div className="create-payment-container">
      <h2 className="create-payment-title">Create New Payment</h2>

      {success && (
        <div className="success-message">
          Payment created successfully!
        </div>
      )}

      {error && <div className="error-message">{error}</div>}

      <form onSubmit={handleSubmit} className="create-payment-form">
        <div className="form-group">
          <label htmlFor="amount">Amount (INR)</label>
          <input
            type="number"
            id="amount"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
            min="1"
            step="0.01"
            placeholder="Enter amount"
          />
        </div>

        <button type="submit" className="submit-button" disabled={loading}>
          {loading ? 'Processing...' : 'Create Payment'}
        </button>
      </form>

      <div className="info-box">
        <h3>ℹ️ Payment Information</h3>
        <ul>
          <li>Minimum amount: ₹1.00</li>
          <li>Payments up to ₹10,000 are automatically approved</li>
          <li>Payments above ₹10,000 require manual review</li>
          <li>Each payment uses a unique idempotency key</li>
        </ul>
      </div>
    </div>
  );
};

export default CreatePayment;
