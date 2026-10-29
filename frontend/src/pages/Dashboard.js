import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import CreatePayment from './CreatePayment';
import PaymentHistory from './PaymentHistory';
import './Dashboard.css';

const Dashboard = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [paymentRefreshKey, setPaymentRefreshKey] = useState(0);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handlePaymentCreated = () => {
    setPaymentRefreshKey((key) => key + 1);
  };

  return (
    <div className="dashboard-container">
      <div className="dashboard-content">
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Payment Dashboard</h1>
            <p className="dashboard-subtitle">Manage your payments efficiently</p>
          </div>
          <button className="logout-button" onClick={handleLogout}>
            Logout
          </button>
        </div>

        <CreatePayment onPaymentCreated={handlePaymentCreated} />
        <PaymentHistory refreshKey={paymentRefreshKey} />
      </div>
    </div>
  );
};

export default Dashboard;
