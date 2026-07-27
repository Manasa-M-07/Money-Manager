const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/db');
const User = require('./User');

const Income = sequelize.define('Income', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true
  },
  amount: {
    type: DataTypes.DOUBLE,
    allowNull: false
  },
  source: {
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
Income.belongsTo(User, { foreignKey: { name: 'userId', allowNull: false }, onDelete: 'CASCADE' });
User.hasMany(Income, { foreignKey: 'userId' });

module.exports = Income;
