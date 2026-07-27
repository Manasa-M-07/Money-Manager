const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const User = require('../models/User');
const Income = require('../models/Income');
const Expense = require('../models/Expense');
const Budget = require('../models/Budget');
const { isAuthenticated, isAdmin } = require('../middleware/auth');

function hashPassword(password) {
  return crypto.createHash('sha256').update(password).digest('hex');
}

// Ensure all admin routes are authenticated and have ADMIN role
router.use(isAuthenticated, isAdmin);

// GET /admin/dashboard
router.get('/dashboard', async (req, res) => {
  try {
    const totalUsers = await User.count({ where: { role: 'USER' } });
    
    // Sum total income across all system users
    const incomes = await Income.findAll({});
    const totalIncome = incomes.reduce((sum, item) => sum + item.amount, 0);
    
    // Sum total expense across all system users
    const expenses = await Expense.findAll({});
    const totalExpense = expenses.reduce((sum, item) => sum + item.amount, 0);
    
    // List of registered standard users
    const usersList = await User.findAll({
      where: { role: 'USER' },
      order: [['createdAt', 'DESC']]
    });
    
    res.render('admin-dashboard', {
      totalUsers,
      totalIncome,
      totalExpense,
      usersList
    });
  } catch (error) {
    console.error('Admin dashboard error:', error);
    res.status(500).render('error', { errorMessage: 'An error occurred loading the administrative dashboard.' });
  }
});

// POST /admin/users/add
router.post('/users/add', async (req, res) => {
  const { name, email, phone, password } = req.body;
  try {
    const existing = await User.findOne({ where: { email: email.toLowerCase() } });
    if (existing) {
      req.session.error = 'Email already registered';
      return res.redirect('/admin/dashboard');
    }
    
    await User.create({
      name,
      email: email.toLowerCase(),
      phone: phone || '',
      password: hashPassword(password),
      role: 'USER',
      active: true
    });
    
    req.session.success = 'User added successfully!';
    res.redirect('/admin/dashboard');
  } catch (error) {
    console.error('Admin add user error:', error);
    req.session.error = 'Failed to create user: ' + error.message;
    res.redirect('/admin/dashboard');
  }
});

// POST /admin/users/edit
router.post('/users/edit', async (req, res) => {
  const { id, name, email, phone } = req.body;
  try {
    const existing = await User.findOne({ where: { email: email.toLowerCase() } });
    if (existing && existing.id.toString() !== id.toString()) {
      req.session.error = 'Email already in use by another account';
      return res.redirect('/admin/dashboard');
    }
    
    const user = await User.findByPk(id);
    if (!user) {
      req.session.error = 'User not found';
      return res.redirect('/admin/dashboard');
    }
    
    user.name = name;
    user.email = email.toLowerCase();
    user.phone = phone || '';
    await user.save();
    
    req.session.success = 'Record updated successfully!';
    res.redirect('/admin/dashboard');
  } catch (error) {
    console.error('Admin edit user error:', error);
    req.session.error = 'Failed to update record: ' + error.message;
    res.redirect('/admin/dashboard');
  }
});

// GET /admin/users/toggle/:id
router.get('/users/toggle/:id', async (req, res) => {
  try {
    const user = await User.findByPk(req.params.id);
    if (!user) {
      req.session.error = 'User not found';
      return res.redirect('/admin/dashboard');
    }
    user.active = !user.active;
    await user.save();
    req.session.success = 'Record updated successfully!';
    res.redirect('/admin/dashboard');
  } catch (error) {
    console.error('Admin toggle user status error:', error);
    req.session.error = 'Failed to update user status: ' + error.message;
    res.redirect('/admin/dashboard');
  }
});

// GET /admin/users/delete/:id
router.get('/users/delete/:id', async (req, res) => {
  try {
    const user = await User.findByPk(req.params.id);
    if (!user) {
      req.session.error = 'User not found';
      return res.redirect('/admin/dashboard');
    }
    
    // Cascading delete: Remove associated financials
    await Income.destroy({ where: { userId: user.id } });
    await Expense.destroy({ where: { userId: user.id } });
    await Budget.destroy({ where: { userId: user.id } });
    
    // Delete the user itself
    await User.destroy({ where: { id: req.params.id } });
    
    req.session.success = 'Record deleted successfully!';
    res.redirect('/admin/dashboard');
  } catch (error) {
    console.error('Admin delete user error:', error);
    req.session.error = 'Failed to delete user record: ' + error.message;
    res.redirect('/admin/dashboard');
  }
});

module.exports = router;
