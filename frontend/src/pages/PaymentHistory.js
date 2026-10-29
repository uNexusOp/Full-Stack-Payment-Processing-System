import React, { useState, useEffect } from 'react';
import { paymentAPI } from '../services/api';
import './PaymentHistory.css';

const PaymentHistory = ({ refreshKey }) => {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    const loadPayments = async () => {
      setLoading(true);
      setError('');
      try {
        const response = await paymentAPI.getPayments();
        if (active) {
          setPayments(response.data);
        }
      } catch (err) {
        if (active) {
          setError(err.response?.data?.error || 'Failed to load payment history');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    loadPayments();
    return () => {
      active = false;
    };
  }, [refreshKey, reloadKey]);

  const refreshPayments = () => {
    setReloadKey((key) => key + 1);
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'SUCCESS':
        return '#10b981';
      case 'FAILED':
        return '#ef4444';
      case 'PENDING':
        return '#f59e0b';
      case 'PROCESSING':
        return '#3b82f6';
      default:
        return '#6b7280';
    }
  };

  const formatCurrency = (amount, currency) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency || 'INR',
    }).format(amount);
  };

  const formatDate = (dateString) => {
    if (!dateString) {
      return 'Unavailable';
    }
    const date = new Date(dateString);
    return Number.isNaN(date.getTime()) ? 'Unavailable' : date.toLocaleString();
  };

  return (
    <div className="payment-history-container">
      <div className="history-heading">
        <h2 className="history-title">Payment History</h2>
        <button
          className="refresh-button"
          onClick={refreshPayments}
          disabled={loading}
        >
          Refresh
        </button>
      </div>

      {error && (
        <div className="error-message" role="alert">
          {error}
          <button className="retry-button" onClick={refreshPayments}>
            Try again
          </button>
        </div>
      )}

      {loading ? (
        <p className="history-message" role="status">Loading payment history...</p>
      ) : !error && payments.length === 0 ? (
        <div className="empty-state">
          <p>No payments yet. Create your first payment to get started!</p>
        </div>
      ) : payments.length > 0 ? (
        <div className="payments-list">
          {payments.map((payment) => (
            <div key={payment.id} className="payment-card">
              <div className="payment-header">
                <div className="payment-info">
                  <span className="payment-id">Payment #{payment.id}</span>
                  <span className="payment-amount">{formatCurrency(payment.amount, payment.currency)}</span>
                </div>
                <span
                  className="payment-status"
                  style={{ color: getStatusColor(payment.status) }}
                >
                  {payment.status}
                </span>
              </div>

              <div className="payment-details">
                <div className="detail-row">
                  <span className="detail-label">Currency:</span>
                  <span className="detail-value">{payment.currency}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Created:</span>
                  <span className="detail-value">{formatDate(payment.createdAt)}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  );
};

export default PaymentHistory;
