const voterCollection = require("../models/voters");
const candidateCollection = require("../models/candidates");
const nodeRSA = require("node-rsa");
const {
  generateVoterID,
  blindVote,
  generateCTFKeys,
  signBlindedVote,
  unblindAndVerifySignature,
  verifySignatureCTF,
  verifyVoteFormat,
} = require("../Crypto/utils");

const voters = async (req, res) => {
  const voters = await voterCollection.find();
  return res.json(voters);
};

const getVoterID = async (req, res) => {
  const voterID = generateVoterID();
  return res.json(voterID);
};

const register = async (req, res) => {
  const registeredVoters = await voterCollection.find();
  const voterName = req.body.voterName;
  const voterID = req.body.voterID;

  let isNewVoter = true;
  registeredVoters.forEach((registeredVoter) => {
    if (registeredVoter.voterID === voterID) isNewVoter = false;
  });

  if (!isNewVoter) return res.json("Already registered");

  const newVoter = new voterCollection({
    voterName: voterName,
    voterID: voterID,
  });
  await newVoter.save();
  return res.json(
    "Registered successfully. Keep the voter ID secure & private."
  );
};

const castVote = async (req, res) => {
  const voterID = req.body.voterID;
  const votedForCandidateID = req.body.candidateID;
  const votedForCandidateName = req.body.votedFor;

  const registeredVoters = await voterCollection.find();
  const registeredCandidates = await candidateCollection.find();
  let eligible = false;
  for (let i = 0; i < registeredVoters.length; ++i) {
    if (
      registeredVoters[i].voterID.indexOf(voterID) >= 0 &&
      registeredVoters[i].voteCasted === false
    ) {
      eligible = true;
      break;
    }
  }
  if (!eligible) return res.json("Not eligible");

  eligible = false;
  for (let i = 0; i < registeredCandidates.length; ++i) {
    if (
      registeredCandidates[i].candidateID.indexOf(votedForCandidateID) >= 0 &&
      registeredCandidates[i].candidateName === votedForCandidateName
    ) {
      eligible = true;
      break;
    }
  }
  if (!eligible) return res.json("Incorrect details");

  const { signingKey, CTFKey } = generateCTFKeys();
  const publicKeyCTF = new nodeRSA(CTFKey.exportKey("public"));
  const privateKeyCTF = new nodeRSA(CTFKey.exportKey("private"));
  const n_CTF = signingKey.keyPair.n.toString();
  const e_CTF = signingKey.keyPair.e.toString();
  const { blindedVote, blindingFactor } = blindVote(
    votedForCandidateID,
    n_CTF,
    e_CTF
  );

  const signedBlindedVote = signBlindedVote(blindedVote, signingKey);

  const value = unblindAndVerifySignature(
    signedBlindedVote,
    blindingFactor,
    n_CTF,
    e_CTF,
    votedForCandidateID
  );

  const signatureOk = value.ok;
  const signedUnblindedVote = value.signedUnblindedVote;

  console.assert(
    signatureOk === true,
    "Verification failed, check the bit length of the signing key!!!"
  );

  const voteString = voterID + signedUnblindedVote;
  const encryptedVote = publicKeyCTF.encrypt(voteString, "base64");

  const decryptedVote = privateKeyCTF.decrypt(encryptedVote, "utf-8");
  const { receivedVoterID, candidateVotedFor } = verifyVoteFormat(
    decryptedVote
  );

  const candidatesList = await candidateCollection.find();
  const { verdict, votedCandidateID } = verifySignatureCTF(
    candidateVotedFor,
    signingKey,
    candidatesList
  );

  if (verdict) {
    // update candidate vote count in DB
    await candidateCollection.findOneAndUpdate(
      { candidateID: votedCandidateID },
      { $inc: { votesReceived: 1 } }
    );
    // update voter's voted status
    await voterCollection.findOneAndUpdate(
      { voterID: receivedVoterID },
      { voteCasted: true }
    );
    return res.json("Successful!!!");
  }

  return res.json("Unsuccessful!!!");
};

module.exports = { voters, getVoterID, register, castVote };
