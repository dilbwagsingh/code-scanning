// Candidates model

const mongoose = require("mongoose");
const Schema = mongoose.Schema;
const candidateSchema = new Schema({
  candidateName: {
    type: String,
    required: true,
  },
  candidateID: {
    type: String,
    required: true,
    unique: true,
  },
  votesReceived: {
    type: Number,
    default: 0,
  },
});

const candidateCollection = mongoose.model("candidate", candidateSchema);

module.exports = candidateCollection;
