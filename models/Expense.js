const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/db');
const User = require('./User');

const Expense = sequelize.define('Expense', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true
  },
  amount: {
    type: DataTypes.DOUBLE,
    allowNull: false
  },
  category: {
    type: DataTypes.STRING,
    allowNull: false
  },
  date: {
    type: DataTypes.DATEONLY,
    allowNull: false
  },
  description: {
    type: DataTypes.STRING,
    defaultValue: ''
  }
}, {
  timestamps: true
});

// Associations
Expense.belongsTo(User, { foreignKey: { name: 'userId', allowNull: false }, onDelete: 'CASCADE' });
User.hasMany(Expense, { foreignKey: 'userId' });

module.exports = Expense;
