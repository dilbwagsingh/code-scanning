const candidateCollection = require("../models/candidates");

const candidates = async (req, res) => {
  const candidatesList = await candidateCollection.find();
  return res.json(candidatesList);
};

module.exports = candidates;
