require("dotenv").config();

module.exports = {
  PORT: process.env.PORT,
  MONGODB_URL: process.env.MONGODB_URL.toString(),
  NODE_ENV: process.env.NODE_ENV,
};
