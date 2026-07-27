const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/db');
const User = require('./User');

const Budget = sequelize.define('Budget', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true
  },
  amount: {
    type: DataTypes.DOUBLE,
    allowNull: false
  },
  month: {
    type: DataTypes.STRING,
    allowNull: false
  }
}, {
  timestamps: true,
  indexes: [
    {
      unique: true,
      fields: ['userId', 'month']
    }
  ]
});

// Associations
Budget.belongsTo(User, { foreignKey: { name: 'userId', allowNull: false }, onDelete: 'CASCADE' });
User.hasMany(Budget, { foreignKey: 'userId' });

module.exports = Budget;
