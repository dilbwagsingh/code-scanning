import React, { Component } from "react";
import axios from "axios";
import notificationHandler from "./Notification";
import styles from "./styles/Form.module.css";

export default class Admin extends Component {
  constructor(props) {
    super(props);

    this.state = {
      candidateID: "",
      candidateName: "",
    };
  }

  onChangeHandler = (event) => {
    event.preventDefault();
    this.setState({
      [event.target.name]: event.target.value,
    });
  };

  addCandidateHandler = async (event) => {
    event.preventDefault();
    if (this.state.candidateID !== "" && this.state.candidateName !== "") {
      const response = await axios.post("/add-candidate", this.state);
      notificationHandler(response.data);
    } else {
      notificationHandler("Incorrect details");
    }
  };

  render() {
    return (
      <div className={styles.container}>
        <h3>Add Candidate</h3>
        <form onSubmit={this.addCandidateHandler}>
          <div className={styles.fields}>
            <input
              type="text"
              id="candidateName"
              name="candidateName"
              value={this.state.candidateName}
              onChange={this.onChangeHandler}
              spellCheck="false"
              placeholder="Candidate Name"
            />
            <input
              type="text"
              id="candidateID"
              name="candidateID"
              value={this.state.candidateID}
              onChange={this.onChangeHandler}
              spellCheck="false"
              placeholder="Candidate ID"
            />
          </div>
          <button className={styles.submitbtn}>Add</button>
        </form>
      </div>
    );
  }
}
