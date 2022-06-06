import React from "react";
import styles from "./styles/Home.module.css";

function Home() {
  return (
    <h3 className={styles.container}>
      This is our implementation of secure elections using RSA blind signatures.
      The main motive behind blind signatures is to decouple the voter from his
      vote. Most of the cryptography and network security features have been
      abstracted out. Instead, what we offer is an elegant and simple to use
      platform to cast vote without reveleaing one's choice to the public. Feel
      free to fiddle around to get a hang of it.
    </h3>
  );
}

export default Home;
