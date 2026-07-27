const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/db');

const User = sequelize.define('User', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true
  },
  name: {
    type: DataTypes.STRING,
    allowNull: false
  },
  email: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true,
    validate: {
      isEmail: true
    }
  },
  phone: {
    type: DataTypes.STRING,
    defaultValue: ''
  },
  password: {
    type: DataTypes.STRING,
    allowNull: false
  },
  role: {
    type: DataTypes.ENUM('ADMIN', 'USER'),
    defaultValue: 'USER'
  },
  active: {
    type: DataTypes.BOOLEAN,
    defaultValue: true
  },
  profilePicture: {
    type: DataTypes.TEXT('long'),
    defaultValue: null
  }
}, {
  timestamps: true
});

module.exports = User;
