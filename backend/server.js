const express = require("express");

const { PORT } = require("./config/config");
const connectDB = require("./config/db");

const { voters, getVoterID, register, castVote } = require("./Routes/voters");
const candidates = require("./Routes/candidates");
const addCandidate = require("./Routes/addCandidate");

const app = express();
app.use(express.json());

connectDB();

app.get("/get-voters", voters);

app.get("/get-candidates", candidates);

app.post("/add-candidate", addCandidate);

app.get("/get-voterid", getVoterID);

app.post("/register", register);

app.post("/cast-vote", castVote);

app.get("*", (req, res) => {
  res.json("You missed all valid routes!!!");
});

app.listen(PORT, () => {
  console.log(`Server running at http://localhost:${PORT}`);
});
