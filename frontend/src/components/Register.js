import React, { Component } from "react";
import axios from "axios";
import styles from "./styles/Form.module.css";
import notificationHandler from "./Notification";

export default class Register extends Component {
  constructor() {
    super();
    this.state = {
      voterID: "",
      voterName: "",
    };
  }

  getVoterIDHandler = (event) => {
    event.preventDefault();
    axios.get("/get-voterid").then((response) => {
      this.setState({
        voterID: response.data,
      });
    });
  };

  onChangeHandler = (event) => {
    event.preventDefault();
    this.setState({
      [event.target.name]: event.target.value,
    });
  };

  registerHandler = async (event) => {
    event.preventDefault();
    if (this.state.voterID !== "" && this.state.voterName !== "") {
      const response = await axios.post("/register", this.state);
      notificationHandler(response.data);
    } else {
      notificationHandler("Incorrect details");
    }
  };

  render() {
    const { voterName } = this.state;
    return (
      <div className={styles.container}>
        <h3>Fill your details</h3>
        <form onSubmit={this.registerHandler}>
          <div className={styles.fields}>
            <input
              type="text"
              id="voterName"
              name="voterName"
              value={voterName}
              onChange={this.onChangeHandler}
              spellCheck="false"
              placeholder="Voter Name"
              className={styles.voterName}
            />
            <input
              type="text"
              id="voterID"
              name="voterID"
              value={this.state.voterID ?? ""}
              spellCheck="false"
              readOnly={true}
              placeholder="Click on the button to get your voter ID"
            />
            <button onClick={this.getVoterIDHandler}>Get</button>
          </div>
          <button className={styles.submitbtn}>Register</button>
        </form>
      </div>
    );
  }
}
