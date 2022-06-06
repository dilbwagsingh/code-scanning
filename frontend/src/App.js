import { Route, BrowserRouter } from "react-router-dom";

import "./App.css";
import Navbar from "./components/Navbar";
import Admin from "./components/Admin";
import Home from "./components/Home";
import Register from "./components/Register";
import Vote from "./components/Vote";
import Voters from "./components/Voters";
import Candidates from "./components/Candidates";

function App() {
  return (
    <div className="App">
      <BrowserRouter>
        <Navbar />
        <div className="content">
          <Route exact path="/" component={Home} />
          <Route path="/register" component={Register} />
          <Route path="/vote" component={Vote} />
          <Route path="/voters" component={Voters} />
          <Route path="/candidates" component={Candidates} />
          <Route path="/admin" component={Admin} />
        </div>
      </BrowserRouter>
    </div>
  );
}

export default App;
