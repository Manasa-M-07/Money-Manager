const User = require('../models/User');

const isAuthenticated = async (req, res, next) => {
  if (!req.session || !req.session.userId) {
    req.session.error = 'Please log in to access this page';
    return res.redirect('/login');
  }

  try {
    const user = await User.findByPk(req.session.userId);
    if (!user) {
      req.session.destroy(() => {
        res.redirect('/login');
      });
      return;
    }

    if (!user.active) {
      req.session.destroy(() => {
        req.session = req.session || {};
        req.session.error = 'Your account is deactivated. Please contact Admin.';
        res.redirect('/login');
      });
      return;
    }

    req.user = user;
    res.locals.user = user; // Make user available in all EJS templates as res.locals.user
    next();
  } catch (error) {
    console.error('Auth middleware error:', error);
    res.status(500).render('error', { errorMessage: 'Internal server error during authentication' });
  }
};

const isAdmin = (req, res, next) => {
  if (req.user && req.user.role === 'ADMIN') {
    return next();
  }
  res.status(403).render('error', { errorMessage: 'Access denied: Admin role required' });
};

const exposeFlash = (req, res, next) => {
  // Custom flash implementation
  res.locals.success = req.session.success || null;
  res.locals.error = req.session.error || null;
  res.locals.warning = req.session.warning || null;
  res.locals.budgetExceeded = req.session.budgetExceeded || null;

  // Clear them
  delete req.session.success;
  delete req.session.error;
  delete req.session.warning;
  delete req.session.budgetExceeded;
  
  next();
};

module.exports = {
  isAuthenticated,
  isAdmin,
  exposeFlash
};
