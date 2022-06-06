import React, { Component } from "react";
import Table from "./Table";
import axios from "axios";

export default class Candidates extends Component {
  constructor(props) {
    super(props);

    this.state = {
      candidateList: [],
    };
  }

  getUpdatedCandidateList = () => {
    axios
      .get("/get-candidates")
      .then((response) => {
        // console.log(response.data);
        const data = response.data;
        const candidates = [];
        data.forEach((candidate) => {
          candidates.push({
            candidateName: candidate.candidateName,
            candidateID: candidate.candidateID,
            votesReceived: candidate.votesReceived,
          });
        });

        // Descending sort the candidates according to their votes received
        candidates.sort((candidate1, candidate2) => {
          return candidate2.votesReceived - candidate1.votesReceived;
        });
        // console.log(candidates);

        // Set the state
        this.setState({
          candidateList: candidates,
        });
      })
      .catch((error) => {
        console.log(error);
      });
  };

  componentDidMount = () => {
    this.getUpdatedCandidateList();
  };

  render() {
    const COLUMNS = [
      {
        Header: "Candidate ID",
        accessor: "candidateID",
      },
      {
        Header: "Candidate Name",
        accessor: "candidateName",
      },
      {
        Header: "Votes Received",
        accessor: "votesReceived",
      },
    ];
    return (
      <>
        <h3>List of registered candidates</h3>
        <Table COLUMNS={COLUMNS} DATA={this.state.candidateList} />
      </>
    );
  }
}
