const express = require('express');
const router = express.Router();
const multer = require('multer');
const crypto = require('crypto');
const { Op } = require('sequelize');
const User = require('../models/User');
const Income = require('../models/Income');
const Expense = require('../models/Expense');
const Budget = require('../models/Budget');
const { isAuthenticated } = require('../middleware/auth');

// Multer memory storage configuration for file uploads
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 2 * 1024 * 1024 } // 2MB limit
});

// Password hashing helper
function hashPassword(password) {
  return crypto.createHash('sha256').update(password).digest('hex');
}

// Helpers for dates
function getStartAndEndOfMonth(date = new Date()) {
  const start = new Date(date.getFullYear(), date.getMonth(), 1);
  const end = new Date(date.getFullYear(), date.getMonth() + 1, 0, 23, 59, 59, 999);
  return { start, end };
}

function getStartAndEndOfMonthFromStr(monthStr) {
  // monthStr is expected to be in YYYY-MM format
  const [year, month] = monthStr.split('-').map(Number);
  const start = new Date(year, month - 1, 1);
  const end = new Date(year, month, 0, 23, 59, 59, 999);
  return { start, end };
}

// Utility to inject view helpers
router.use((req, res, next) => {
  res.locals.formatDate = (d) => {
    if (!d) return '';
    try {
      const dateObj = new Date(d);
      return dateObj.toISOString().split('T')[0];
    } catch (e) {
      return '';
    }
  };
  res.locals.formatDecimal = (val) => {
    return Number(val || 0).toFixed(2);
  };
  next();
});

// GET /dashboard
router.get('/', isAuthenticated, async (req, res) => {
  try {
    const user = req.user;
    
    // Total income and expenses across all time
    const incomesAll = await Income.findAll({ where: { userId: user.id } });
    const expensesAll = await Expense.findAll({ where: { userId: user.id } });
    
    const totalIncome = incomesAll.reduce((sum, item) => sum + item.amount, 0);
    const totalExpenses = expensesAll.reduce((sum, item) => sum + item.amount, 0);
    const balance = totalIncome - totalExpenses;
    const totalSavings = balance; // Savings = Income - Expenses

    // Current Month calculations
    const today = new Date();
    const { start, end } = getStartAndEndOfMonth(today);
    const currentMonthStr = today.toISOString().substring(0, 7); // YYYY-MM
    const currentMonthName = today.toLocaleString('default', { month: 'long', year: 'numeric' }); // July 2026
    
    const monthlyIncomes = await Income.findAll({
      where: {
        userId: user.id,
        date: { [Op.between]: [start.toISOString().split('T')[0], end.toISOString().split('T')[0]] }
      }
    });
    const monthlyExpensesData = await Expense.findAll({
      where: {
        userId: user.id,
        date: { [Op.between]: [start.toISOString().split('T')[0], end.toISOString().split('T')[0]] }
      }
    });

    const monthlyIncome = monthlyIncomes.reduce((sum, item) => sum + item.amount, 0);
    const monthlyExpenses = monthlyExpensesData.reduce((sum, item) => sum + item.amount, 0);

    // Get Budget for current month
    const budgetDoc = await Budget.findOne({ where: { userId: user.id, month: currentMonthStr } });
    const monthlyBudget = budgetDoc ? budgetDoc.amount : 0;
    const remainingBudget = monthlyBudget - monthlyExpenses;
    const budgetExceeded = monthlyBudget > 0 && monthlyExpenses > monthlyBudget;

    // Recent combined transactions (top 5)
    const txList = [];
    incomesAll.forEach(inc => {
      txList.push({
        id: inc.id,
        amount: inc.amount,
        type: 'INCOME',
        categoryOrSource: inc.source,
        date: inc.date,
        description: inc.description
      });
    });
    expensesAll.forEach(exp => {
      txList.push({
        id: exp.id,
        amount: exp.amount,
        type: 'EXPENSE',
        categoryOrSource: exp.category,
        date: exp.date,
        description: exp.description
      });
    });

    // Sort descending by date
    txList.sort((a, b) => new Date(b.date) - new Date(a.date));
    const recentTransactions = txList.slice(0, 5);

    res.render('dashboard', {
      totalIncome,
      totalExpenses,
      balance,
      monthlyBudget,
      remainingBudget,
      totalSavings,
      monthlyIncome,
      monthlyExpenses,
      recentTransactions,
      budgetExceeded,
      currentMonth: currentMonthName
    });
  } catch (error) {
    console.error('Dashboard logic error:', error);
    res.status(500).render('error', { errorMessage: 'An error occurred loading your dashboard data.' });
  }
});

// --- Income Management ---
router.get('/income', isAuthenticated, async (req, res) => {
  try {
    const incomes = await Income.findAll({
      where: { userId: req.user.id },
      order: [['date', 'DESC']]
    });
    res.render('income', { incomes });
  } catch (error) {
    console.error('Error loading incomes:', error);
    res.status(500).render('error', { errorMessage: 'Failed to retrieve income history.' });
  }
});

router.post('/income/add', isAuthenticated, async (req, res) => {
  const { amount, source, date, description } = req.body;
  try {
    await Income.create({
      amount: parseFloat(amount),
      source,
      date,
      description: description || '',
      userId: req.user.id
    });
    req.session.success = 'Income added successfully!';
    res.redirect('/dashboard/income');
  } catch (error) {
    console.error('Error adding income:', error);
    req.session.error = 'Failed to add income: ' + error.message;
    res.redirect('/dashboard/income');
  }
});

router.post('/income/edit', isAuthenticated, async (req, res) => {
  const { id, amount, source, date, description } = req.body;
  try {
    const income = await Income.findByPk(id);
    if (!income || income.userId.toString() !== req.user.id.toString()) {
      req.session.error = 'Unauthorized access to record';
      return res.redirect('/dashboard/income');
    }
    income.amount = parseFloat(amount);
    income.source = source;
    income.date = date;
    income.description = description || '';
    await income.save();
    req.session.success = 'Record updated successfully!';
    res.redirect('/dashboard/income');
  } catch (error) {
    console.error('Error updating income:', error);
    req.session.error = 'Failed to update record: ' + error.message;
    res.redirect('/dashboard/income');
  }
});

router.get('/income/delete/:id', isAuthenticated, async (req, res) => {
  try {
    const income = await Income.findByPk(req.params.id);
    if (!income || income.userId.toString() !== req.user.id.toString()) {
      req.session.error = 'Unauthorized access to record';
      return res.redirect('/dashboard/income');
    }
    await Income.destroy({ where: { id: req.params.id } });
    req.session.success = 'Record deleted successfully!';
    res.redirect('/dashboard/income');
  } catch (error) {
    console.error('Error deleting income:', error);
    req.session.error = 'Failed to delete record: ' + error.message;
    res.redirect('/dashboard/income');
  }
});

// --- Expense Management ---
const categoriesList = ["Food", "Travel", "Shopping", "Bills", "Rent", "Health", "Education", "Entertainment", "Fuel", "Investment", "Other"];

router.get('/expense', isAuthenticated, async (req, res) => {
  try {
    const expenses = await Expense.findAll({
      where: { userId: req.user.id },
      order: [['date', 'DESC']]
    });
    res.render('expense', { expenses, categories: categoriesList });
  } catch (error) {
    console.error('Error loading expenses:', error);
    res.status(500).render('error', { errorMessage: 'Failed to retrieve expense history.' });
  }
});

router.post('/expense/add', isAuthenticated, async (req, res) => {
  const { amount, category, date, description } = req.body;
  try {
    await Expense.create({
      amount: parseFloat(amount),
      category,
      date,
      description: description || '',
      userId: req.user.id
    });

    // Check if budget is exceeded for the month of the added expense
    const monthStr = date.substring(0, 7); // YYYY-MM
    const budgetDoc = await Budget.findOne({ where: { userId: req.user.id, month: monthStr } });
    if (budgetDoc && budgetDoc.amount > 0) {
      const { start, end } = getStartAndEndOfMonthFromStr(monthStr);
      const monthlyExpensesDocs = await Expense.findAll({
        where: {
          userId: req.user.id,
          date: { [Op.between]: [start.toISOString().split('T')[0], end.toISOString().split('T')[0]] }
        }
      });
      const totalExpForMonth = monthlyExpensesDocs.reduce((sum, item) => sum + item.amount, 0);
      if (totalExpForMonth > budgetDoc.amount) {
        req.session.warning = `Warning: Budget exceeded for ${monthStr}!`;
      }
    }

    req.session.success = 'Expense added successfully!';
    res.redirect('/dashboard/expense');
  } catch (error) {
    console.error('Error adding expense:', error);
    req.session.error = 'Failed to add expense: ' + error.message;
    res.redirect('/dashboard/expense');
  }
});

router.post('/expense/edit', isAuthenticated, async (req, res) => {
  const { id, amount, category, date, description } = req.body;
  try {
    const expense = await Expense.findByPk(id);
    if (!expense || expense.userId.toString() !== req.user.id.toString()) {
      req.session.error = 'Unauthorized access to record';
      return res.redirect('/dashboard/expense');
    }
    expense.amount = parseFloat(amount);
    expense.category = category;
    expense.date = date;
    expense.description = description || '';
    await expense.save();
    req.session.success = 'Record updated successfully!';
    res.redirect('/dashboard/expense');
  } catch (error) {
    console.error('Error updating expense:', error);
    req.session.error = 'Failed to update record: ' + error.message;
    res.redirect('/dashboard/expense');
  }
});

router.get('/expense/delete/:id', isAuthenticated, async (req, res) => {
  try {
    const expense = await Expense.findByPk(req.params.id);
    if (!expense || expense.userId.toString() !== req.user.id.toString()) {
      req.session.error = 'Unauthorized access to record';
      return res.redirect('/dashboard/expense');
    }
    await Expense.destroy({ where: { id: req.params.id } });
    req.session.success = 'Record deleted successfully!';
    res.redirect('/dashboard/expense');
  } catch (error) {
    console.error('Error deleting expense:', error);
    req.session.error = 'Failed to delete record: ' + error.message;
    res.redirect('/dashboard/expense');
  }
});

// --- Budget Module ---
router.get('/budget', isAuthenticated, async (req, res) => {
  try {
    const today = new Date();
    const currentMonthStr = today.toISOString().substring(0, 7); // YYYY-MM
    
    // Get budget limit
    const budgetDoc = await Budget.findOne({ where: { userId: req.user.id, month: currentMonthStr } });
    const monthlyBudget = budgetDoc ? budgetDoc.amount : 0;
    
    // Get current month expense
    const { start, end } = getStartAndEndOfMonth(today);
    const monthlyExpensesDocs = await Expense.findAll({
      where: {
        userId: req.user.id,
        date: { [Op.between]: [start.toISOString().split('T')[0], end.toISOString().split('T')[0]] }
      }
    });
    const monthlyExpenses = monthlyExpensesDocs.reduce((sum, item) => sum + item.amount, 0);
    const remainingBudget = monthlyBudget - monthlyExpenses;

    res.render('budget', {
      monthlyBudget,
      monthlyExpenses,
      remainingBudget,
      currentMonth: currentMonthStr
    });
  } catch (error) {
    console.error('Error loading budget:', error);
    res.status(500).render('error', { errorMessage: 'Failed to load budget page.' });
  }
});

router.post('/budget/update', isAuthenticated, async (req, res) => {
  const { amount, month } = req.body;
  try {
    let budget = await Budget.findOne({ where: { userId: req.user.id, month } });
    if (budget) {
      budget.amount = parseFloat(amount);
      await budget.save();
    } else {
      await Budget.create({
        userId: req.user.id,
        month,
        amount: parseFloat(amount)
      });
    }
    req.session.success = 'Budget updated successfully!';
    res.redirect('/dashboard/budget');
  } catch (error) {
    console.error('Error updating budget:', error);
    req.session.error = 'Failed to set budget: ' + error.message;
    res.redirect('/dashboard/budget');
  }
});

// --- Reports ---
router.get('/reports', isAuthenticated, async (req, res) => {
  const { type = 'EXPENSE', search, category, startDate, endDate } = req.query;
  try {
    const user = req.user;
    
    // Filters building
    const filter = { userId: user.id };
    
    if (search && search.trim() !== '') {
      const searchVal = `%${search.trim()}%`;
      if (type === 'INCOME') {
        filter[Op.or] = [
          { description: { [Op.like]: searchVal } },
          { source: { [Op.like]: searchVal } }
        ];
      } else {
        filter.description = { [Op.like]: searchVal };
      }
    }
    
    if (type === 'EXPENSE' && category && category !== 'ALL') {
      filter.category = category;
    }
    
    if (startDate || endDate) {
      filter.date = {};
      if (startDate) {
        filter.date[Op.gte] = startDate;
      }
      if (endDate) {
        filter.date[Op.lte] = endDate;
      }
    }

    let reportData = [];
    let totalReportAmount = 0;
    
    if (type === 'INCOME') {
      reportData = await Income.findAll({
        where: filter,
        order: [['date', 'DESC']]
      });
      totalReportAmount = reportData.reduce((sum, item) => sum + item.amount, 0);
    } else {
      reportData = await Expense.findAll({
        where: filter,
        order: [['date', 'DESC']]
      });
      totalReportAmount = reportData.reduce((sum, item) => sum + item.amount, 0);
    }

    // Current Month Summary statistics
    const today = new Date();
    const { start, end } = getStartAndEndOfMonth(today);
    const startStr = start.toISOString().split('T')[0];
    const endStr = end.toISOString().split('T')[0];
    
    const currentMonthIncomes = await Income.findAll({
      where: { userId: user.id, date: { [Op.between]: [startStr, endStr] } }
    });
    const currentMonthExpenses = await Expense.findAll({
      where: { userId: user.id, date: { [Op.between]: [startStr, endStr] } }
    });
    
    const monthlyIncome = currentMonthIncomes.reduce((sum, item) => sum + item.amount, 0);
    const monthlyExpense = currentMonthExpenses.reduce((sum, item) => sum + item.amount, 0);

    // Group current month expenses by category
    const catSumsMap = {};
    categoriesList.forEach(c => { catSumsMap[c] = 0; });
    currentMonthExpenses.forEach(exp => {
      if (catSumsMap[exp.category] !== undefined) {
        catSumsMap[exp.category] += exp.amount;
      } else {
        catSumsMap[exp.category] = exp.amount;
      }
    });

    // Format like Java Object[] (List<Object[]>)
    const categoryBreakdown = Object.keys(catSumsMap)
      .filter(cat => catSumsMap[cat] > 0)
      .map(cat => [cat, catSumsMap[cat]]);

    res.render('reports', {
      reportData,
      totalReportAmount,
      monthlyIncome,
      monthlyExpense,
      categoryBreakdown,
      categories: categoriesList,
      // Pass query parameters back to render inputs
      type,
      search: search || '',
      category: category || 'ALL',
      startDate: startDate || '',
      endDate: endDate || ''
    });
  } catch (error) {
    console.error('Reports error:', error);
    res.status(500).render('error', { errorMessage: 'An error occurred generating reports.' });
  }
});

// --- Profile ---
router.get('/profile', isAuthenticated, (req, res) => {
  res.render('profile');
});

router.post('/profile/update', isAuthenticated, async (req, res) => {
  const { name, email, phone } = req.body;
  try {
    const existing = await User.findOne({ where: { email: email.toLowerCase() } });
    if (existing && existing.id.toString() !== req.user.id.toString()) {
      req.session.error = 'Email already in use by another account';
      return res.redirect('/dashboard/profile');
    }
    
    const user = await User.findByPk(req.user.id);
    user.name = name;
    user.email = email.toLowerCase();
    user.phone = phone || '';
    await user.save();
    
    req.session.success = 'Record updated successfully!';
    res.redirect('/dashboard/profile');
  } catch (error) {
    console.error('Profile update error:', error);
    req.session.error = 'Failed to update details: ' + error.message;
    res.redirect('/dashboard/profile');
  }
});

router.post('/profile/picture', isAuthenticated, upload.single('profilePic'), async (req, res) => {
  try {
    if (!req.file) {
      req.session.error = 'Please select a file to upload.';
      return res.redirect('/dashboard/profile');
    }

    const mimeType = req.file.mimetype;
    if (!mimeType.startsWith('image/')) {
      req.session.error = 'Only image files are allowed.';
      return res.redirect('/dashboard/profile');
    }

    const base64Image = req.file.buffer.toString('base64');
    const dataUrl = `data:${mimeType};base64,${base64Image}`;

    const user = await User.findByPk(req.user.id);
    user.profilePicture = dataUrl;
    await user.save();

    req.session.success = 'Profile picture updated successfully!';
    res.redirect('/dashboard/profile');
  } catch (error) {
    console.error('Profile picture upload error:', error);
    req.session.error = 'Error uploading image: ' + error.message;
    res.redirect('/dashboard/profile');
  }
});

router.post('/profile/password', isAuthenticated, async (req, res) => {
  const { currentPassword, newPassword } = req.body;
  try {
    const user = await User.findByPk(req.user.id);
    const hashedCurrent = hashPassword(currentPassword);
    
    if (user.password !== hashedCurrent) {
      req.session.error = 'Current password is incorrect!';
      return res.redirect('/dashboard/profile');
    }

    user.password = hashPassword(newPassword);
    await user.save();

    req.session.success = 'Password changed successfully!';
    res.redirect('/dashboard/profile');
  } catch (error) {
    console.error('Password change error:', error);
    req.session.error = 'Failed to change password: ' + error.message;
    res.redirect('/dashboard/profile');
  }
});

module.exports = router;
