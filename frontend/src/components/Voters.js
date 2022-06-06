import React, { Component } from "react";
import Table from "./Table";
import axios from "axios";

export default class Voters extends Component {
  constructor(props) {
    super(props);

    this.state = {
      voterList: [],
    };
  }

  getUpdatedVoterList = () => {
    axios
      .get("/get-voters")
      .then((response) => {
        const voters = [];
        const data = response.data;
        data.forEach((voter) => {
          voters.push({
            voterID: voter.voterID.substr(0, 10) + "...",
            voterName: voter.voterName,
            voteCasted: voter.voteCasted ? "Voted" : "Not voted",
          });
        });

        this.setState({
          voterList: voters,
        });
      })
      .catch((error) => {
        console.log(error);
      });
  };

  componentDidMount = () => {
    this.getUpdatedVoterList();
  };

  render() {
    const COLUMNS = [
      {
        Header: "Voter ID",
        accessor: "voterID",
      },
      {
        Header: "Voter Name",
        accessor: "voterName",
      },
      {
        Header: "Voting Status",
        accessor: "voteCasted",
      },
    ];

    return (
      <>
        <h3>List of registered voters</h3>
        <Table COLUMNS={COLUMNS} DATA={this.state.voterList} />
      </>
    );
  }
}
