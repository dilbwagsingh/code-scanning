const candidateCollection = require("../models/candidates");

const addCandidate = async (req, res) => {
  const candidateID = req.body.candidateID;
  const candidateName = req.body.candidateName;
  const registeredCandidates = await candidateCollection.find();
  for (let i = 0; i < registeredCandidates.length; ++i) {
    if (registeredCandidates[i].candidateID === candidateID) {
      return res.json("Incorrect details");
    }
  }
  const newCandidate = new candidateCollection({
    candidateName: candidateName,
    candidateID: candidateID,
  });
  await newCandidate.save();
  return res.json("New candidate resigtered");
};

module.exports = addCandidate;
