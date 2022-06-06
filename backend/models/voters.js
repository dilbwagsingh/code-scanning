// Voters model

const mongoose = require("mongoose");
const Schema = mongoose.Schema;
const voterSchema = new Schema({
  voterName: {
    type: String,
    required: true,
  },
  voterID: {
    type: String,
    required: true,
    unique: true,
  },
  voteCasted: {
    type: Boolean,
    default: false,
  },
});

const voterCollection = mongoose.model("Voter", voterSchema);

module.exports = voterCollection;
