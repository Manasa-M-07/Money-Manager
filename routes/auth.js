const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const User = require('../models/User');

// Helper to hash password matching Spring Boot SHA-256
function hashPassword(password) {
  return crypto.createHash('sha256').update(password).digest('hex');
}

// GET /
router.get('/', (req, res) => {
  if (req.session && req.session.userId) {
    if (req.session.role === 'ADMIN') {
      return res.redirect('/admin/dashboard');
    }
    return res.redirect('/dashboard');
  }
  res.render('home');
});

// GET /login
router.get('/login', (req, res) => {
  if (req.session && req.session.userId) {
    if (req.session.role === 'ADMIN') {
      return res.redirect('/admin/dashboard');
    }
    return res.redirect('/dashboard');
  }
  res.render('login');
});

// POST /login
router.post('/login', async (req, res) => {
  const { email, password } = req.body;
  try {
    const user = await User.findOne({ where: { email: email.toLowerCase() } });
    if (user) {
      const hashed = hashPassword(password);
      if (user.password === hashed) {
        if (!user.active) {
          req.session.error = 'Your account is deactivated. Please contact Admin.';
          return res.redirect('/login');
        }
        req.session.userId = user.id;
        req.session.role = user.role;
        req.session.success = `Welcome back, ${user.name}!`;
        if (user.role === 'ADMIN') {
          return res.redirect('/admin/dashboard');
        }
        return res.redirect('/dashboard');
      }
    }
    req.session.error = 'Invalid email or password!';
    res.redirect('/login');
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).render('error', { errorMessage: 'An error occurred during sign in.' });
  }
});

// GET /register
router.get('/register', (req, res) => {
  if (req.session && req.session.userId) {
    return res.redirect('/dashboard');
  }
  res.render('register');
});

// POST /register
router.post('/register', async (req, res) => {
  const { name, email, phone, password } = req.body;
  try {
    const existing = await User.findOne({ where: { email: email.toLowerCase() } });
    if (existing) {
      return res.render('register', { error: 'Email already registered', user: req.body });
    }
    
    const newUser = new User({
      name,
      email: email.toLowerCase(),
      phone: phone || '',
      password: hashPassword(password),
      role: 'USER',
      active: true
    });
    
    await newUser.save();
    req.session.success = 'Registration successful! Please log in.';
    res.redirect('/login');
  } catch (error) {
    console.error('Registration error:', error);
    res.render('register', { error: error.message, user: req.body });
  }
});

// GET /forgot-password
router.get('/forgot-password', (req, res) => {
  res.render('forgot-password');
});

// POST /forgot-password
router.post('/forgot-password', async (req, res) => {
  const { email, phone, newPassword } = req.body;
  try {
    const user = await User.findOne({ where: { email: email.toLowerCase() } });
    if (user && user.phone === phone) {
      user.password = hashPassword(newPassword);
      await user.save();
      req.session.success = 'Password reset successful! Please log in with your new password.';
      return res.redirect('/login');
    }
    res.render('forgot-password', { error: 'Invalid Email or Phone Number combination!' });
  } catch (error) {
    console.error('Forgot password error:', error);
    res.status(500).render('error', { errorMessage: 'An error occurred during password reset.' });
  }
});

// GET /logout
router.get('/logout', (req, res) => {
  if (req.session) {
    req.session.destroy((err) => {
      if (err) {
        console.error('Logout error:', err);
      }
      res.redirect('/login');
    });
  } else {
    res.redirect('/login');
  }
});

module.exports = router;
