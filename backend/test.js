const BS = require("blind-signatures");
const nodeRSA = require("node-rsa");
const { createHash } = require("crypto");

// retruns a voterID which has a fixed length of 64
const generateVoterID = () => {
  const RANGE = 2 ** 53;
  const x = Math.floor(Math.random() * RANGE);
  const hash = createHash("sha256");
  return hash.update(x.toString()).digest("hex").toString();
};

// console.log(generateVoterID().length);

const a = 1;

const voter = {
  vote: a.toString(),
  voterID: generateVoterID(),
  n_CTF: null,
  e_CTF: null,
};

const CTF = {
  signingKey: BS.keyGeneration({ b: 1024 }),
  registeredVoters: [],
};

// Voter wants CTF to sign the vote without revealing it's contents.
// CTF can later verify it did sign the message

// console.log(`Voter's vote: ${voter.vote}\n`);

// Voter gets CTF's public key (used for signing purposes only, encryption key will be different)
voter.n_CTF = CTF.signingKey.keyPair.n.toString();
voter.e_CTF = CTF.signingKey.keyPair.e.toString();

// console.log(`CTF's n: ${voter.n_CTF}\n`);
// console.log(`CTF's e: ${voter.e_CTF}\n`);

// Voter creates say 10 properly formed legit votes
// const numOfVotes = 10;
// const votes = [];
// for (let i = 0; i < numOfVotes - 1; i++) {
//   votes.push("randomGeneratedText");
// }
const vote = voter.vote;
// console.log(`Votes array created:\n${votes}\n`);

// Voter blinds these votes individually
// const bVotes = [];
// const bFactors = [];
// votes.forEach((vote) => {
const { blinded, r } = BS.blind({
  message: vote,
  N: voter.n_CTF,
  E: voter.e_CTF,
});

// bVotes.push(blinded);
// bFactors.push(r);
// });

// console.log(`Blinded Votes: ${bVotes}\n`);
// console.log(`Blinding Factors:  ${bFactors}\n`);

// We are skipping the unblinding and verifying 9/10 votes part here

// function messageToHashInt(message) {
//   const messageHash = BS.messageToHash(message);
//   const messageBig = new jsbn.BigInteger(messageHash, 16);
//   return messageBig;
// }

// const a = bVotes[9];
// const b = BS.unblind({
//   signed: a,
//   N: voter.n_CTF,
//   r: bFactors[9],
// });
// const N = jsbn.BigInteger(voter.n_CTF);
// console.log(b.toString() === messageToHashInt(voter.vote).mod(N));

// Sends the blinded votes array to the CTF for signing
// const signedBlindedVotes = [];
let signed;
if (voter.voterID in CTF.registeredVoters)
  console.log("Already applied, cannot apply again");
else {
  // bVotes.forEach((bVote) => {
  CTF.blindedVote = blinded;
  signed = BS.sign({
    blinded: CTF.blindedVote,
    key: CTF.signingKey,
  });
  // signedBlindedVotes.push(signed);
  // });
  // The CTF registers the unique voterID for later verification and duplication purposes
  CTF.registeredVoters.push(voter.voterID);
  // console.log("Done registration!!");
}
// console.log(`Signed blinded votes: ${signedBlindedVotes}\n`);

// Voter unblinds the signed votes received from the CTF
// const signedUnblindedVotes = [];
// signedBlindedVotes.forEach((signedBlindedVote, i) => {
const unblinded = BS.unblind({
  signed: signed,
  N: voter.n_CTF,
  r: r,
});
// signedUnblindedVotes.push(unblinded);
// });
// console.log(signedUnblindedVotes);

// Voter verifies if the signatures of the CTF are correct or not
let ok = true;
// signedUnblindedVotes.forEach((signedUnblindedVote, i) => {
ok = BS.verify({
  unblinded: unblinded,
  N: voter.n_CTF,
  E: voter.e_CTF,
  message: vote,
});
// });
// Finally ok should be true/1 of it is actually signed by the CTF
// console.log(ok);

// Public/Private key generation of the CTF for vote casting
const keyCTF = new nodeRSA({ b: 1024 });
const publicKeyCTF = new nodeRSA(keyCTF.exportKey("public"));
const privateKeyCTF = new nodeRSA(keyCTF.exportKey("private"));

// Voter encrypts his signed vote using the CTF's public key
const votersChoice = unblinded;
const voteString = voter.voterID + votersChoice;
const encryptedVote = publicKeyCTF.encrypt(voteString, "base64");
// console.log(encryptedVote);

// Sends this over to the CTF where CTF decrypts the vote with his private key
const decryptedVote = privateKeyCTF.decrypt(encryptedVote, "utf-8");

// Separates the VoterId and actual vote choice
let receivedVoterID = null;
let candidateVotedFor = null;
try {
  receivedVoterID = decryptedVote.slice(0, 64);
} catch {
  console.log("Malformed vote received,discarding...");
}
if (CTF.registeredVoters.indexOf(receivedVoterID) !== -1) {
  candidateVotedFor = decryptedVote.slice(64);
} else console.log("Not registered!!!");

// Sanity check for my logic till now (should return true)
console.log(
  `Sanity check passed: ${
    candidateVotedFor.toString() === votersChoice.toString()
  }`
);

// CTF verifies his own signature in the vote
const verdict = BS.verify2({
  unblinded: candidateVotedFor,
  key: CTF.signingKey,
  message: vote /*For this we can try all possible candidates, (since the CTF doesnot know for whom the particular voter voted); if anyone returns true then the sign is vallid and teh choice is recorded */,
});

// If verdict is true then the vote is valid and recorded else discarded
console.log(`Vote successfully registered: ${verdict}`);
