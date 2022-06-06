import React from "react";
import styles from "./styles/Navbar.module.css";
import { NavLink } from "react-router-dom";

function Navbar() {
  return (
    <header>
      <nav>
        <ul className={styles.nav_links}>
          <li>
            <NavLink exact to="/" activeClassName={styles.activeTab}>
              Home
            </NavLink>
          </li>
          <li>
            <NavLink to="/register" activeClassName={styles.activeTab}>
              Register
            </NavLink>
          </li>
          <li>
            <NavLink to="/vote" activeClassName={styles.activeTab}>
              Vote
            </NavLink>
          </li>
          <li>
            <NavLink to="/voters" activeClassName={styles.activeTab}>
              Voters
            </NavLink>
          </li>
          <li>
            <NavLink to="/candidates" activeClassName={styles.activeTab}>
              Candidates
            </NavLink>
          </li>
          <li>
            <NavLink to="/admin" activeClassName={styles.activeTab}>
              Admin
            </NavLink>
          </li>
        </ul>
      </nav>
    </header>
  );
}

export default Navbar;
