require('dotenv').config();
const express = require('express');
const path = require('path');
const session = require('express-session');
const crypto = require('crypto');
const { sequelize, connectDB } = require('./config/db');
const User = require('./models/User');
const { exposeFlash } = require('./middleware/auth');

// Initialize Express App
const app = express();

// Connect to MySQL
connectDB();

// Database Seeding: Setup System Admin if DB is empty
const seedAdmin = async () => {
  try {
    const userCount = await User.count({});
    if (userCount === 0) {
      const adminPasswordHash = crypto.createHash('sha256').update('admin123').digest('hex');
      await User.create({
        name: 'System Admin',
        email: 'admin@moneymanager.com',
        phone: '1234567890',
        password: adminPasswordHash,
        role: 'ADMIN',
        active: true
      });
      console.log('Database Seeded: System Admin created (admin@moneymanager.com / admin123)');
    }
  } catch (error) {
    console.error('Seeding error:', error);
  }
};

// Sync schemas and seed
sequelize.sync({ alter: true })
  .then(() => {
    console.log('MySQL schemas synchronized successfully.');
    seedAdmin();
  })
  .catch(err => {
    console.error('Error synchronizing database schemas:', err);
  });

// Set View Engine
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Body Parser Middleware
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Session Middleware
app.use(session({
  secret: process.env.SESSION_SECRET || 'moneymanager_key_456_789',
  resave: false,
  saveUninitialized: false,
  cookie: { maxAge: 24 * 60 * 60 * 1000 } // 24 hours
}));

// Expose flash messages to views
app.use(exposeFlash);

// Serve Static Files
app.use(express.static(path.join(__dirname, 'public')));

// Routing Modules
const authRoutes = require('./routes/auth');
const dashboardRoutes = require('./routes/dashboard');
const adminRoutes = require('./routes/admin');

app.use('/', authRoutes);
app.use('/dashboard', dashboardRoutes);
app.use('/admin', adminRoutes);

// 404 Route handler
app.use((req, res, next) => {
  res.status(404).render('error', { errorMessage: 'The page you requested was not found.' });
});

// General Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).render('error', { errorMessage: err.message || 'Something went wrong on our end.' });
});

// Listen on Port
const PORT = process.env.PORT || 8081;
app.listen(PORT, () => {
  console.log(`Server running in mode on http://localhost:${PORT}`);
});
