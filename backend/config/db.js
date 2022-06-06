// Database configuration
const { MONGODB_URL } = require("./config");
const mongoose = require("mongoose");

const connectDB = async () => {
  try {
    const connect = await mongoose.connect(MONGODB_URL, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
      useFindAndModify: false,
    });
    console.log(
      `--------Connected to MongoDB at: ${connect.connection.host}-------`
    );
  } catch (error) {
    console.log(`${error.message}`);
    process.exit(1);
  }
};

module.exports = connectDB;
