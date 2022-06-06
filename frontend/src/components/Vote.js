import React, { Component } from "react";
import axios from "axios";
import notificationHandler from "./Notification";
import styles from "./styles/Form.module.css";

export default class Vote extends Component {
  constructor(props) {
    super(props);

    this.state = {
      votedFor: "",
      candidateID: "",
      voterID: "",
    };
  }

  onChangeHandler = (event) => {
    event.preventDefault();
    this.setState({
      [event.target.name]: event.target.value,
    });
  };

  onCastVoteHandler = async (event) => {
    event.preventDefault();
    if (
      this.state.candidateID !== "" &&
      this.state.votedFor !== "" &&
      this.state.votedFor !== ""
    ) {
      const response = await axios.post("/cast-vote", this.state);
      notificationHandler(response.data);
    } else {
      notificationHandler("Incorrect details");
    }
  };

  render() {
    const { votedFor, candidateID, voterID } = this.state;
    return (
      <div className={styles.container}>
        <h3>Draft your vote</h3>
        <form onSubmit={this.onCastVoteHandler}>
          <div className={styles.fields}>
            <input
              type="text"
              id="votedFor"
              name="votedFor"
              value={votedFor}
              onChange={this.onChangeHandler}
              spellCheck="false"
              placeholder="Candidate Name"
            />
            <input
              type="text"
              id="candidateID"
              name="candidateID"
              value={candidateID}
              onChange={this.onChangeHandler}
              spellCheck="false"
              placeholder="Candidate ID"
            />
            <input
              type="text"
              id="voterID"
              name="voterID"
              value={voterID}
              onChange={this.onChangeHandler}
              spellCheck="false"
              placeholder="Voter ID"
            />
          </div>
          <button className={styles.submitbtn}>Vote</button>
        </form>
      </div>
    );
  }
}
