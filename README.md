# Electronic-voting

A simple MERN webapp to carry out online voting securely.

## Motivation

Given the current pandemic situation (as of this current commit the world was facing the COVID-19 pandemic); As a person who hails from West Bengal, there is a dire need to digitalize the voting system as the 2021 Bengal elections worsened the case count here. There has been a lot of research done on this very topic but none of them have been implemented to actually take into account all the quirks that online voting systems come with. This project aims to make a practical approach towards secure online voting.

> Note: This is also my submission for the course project of CSE 537-Network Security taught at IIT (BHU).

## A note on secure evoting systems

Computerized voting will never be used for general elections unless there is a protocol that both maintains individual privacy & prevents cheating.
The ideal protocol has, at the very least, these six requirements-

1. Only authorized voters can vote
2. No one can vote more than once
3. No one can determine for whom anyone else voted
4. No one can duplicate anyone else’s vote (This turns out to be the hardest requirement)
5. No one can change anyone else’s vote without being discovered
6. Every voter can make sure that his vote has been taken into account
   in the final tabulation
7. Everyone knows who voted & who didn’t (optional)

## Features of this webapp

- Implements most of the above listed requirements except for points 4 & 5
- Uses **RSA Blind Signatures** algorithm to satisfy requirement numbers 1, 2 & 3
- Requirement number 6 & 7 are fulfilled by simply displaying the list of registered voters with their individual voting statuses

## Caveats of the webapp

You saw this coming didn't you? Well if it wasn't for this, I probably would have been doing conferences and seminars on how I revolutionized voting.

- Depending on how the blind signatures are implemented anonimity is promised, but **not** guaranteed.
- Though the Central Tabulating Facility(CTF) has no way to link a vote to the voter, but it certainly can generate a large number of signed & valid votes itself.
- Say a voter discovers that his vote has been tampered with, he will have no way to prove it.

> So yeah, this webapp is not yet a perfect solution to be used in high risk environments.

## Development environment setup

Well you are in luck this time. This app is **fully dockerized** and setting up you development enviroment is as simple as running a simple command from the root directory of the app.

But before that you will need to create a .env file inside the backend folder. You can take reference from the .env.example file I provided for the exact variables that are to be created.

```Makefile:
make run-dev
```

Alternatively, if you dont have support for Makefiles in your local system you can also issue the following command-

```Docker:
docker-compose up
```

> Note: Docker should be installed in your system for this these commands to work.

You are now ready to make changes to the app. I have also setup hot-reloading for the Docker containers to automatically reflect changes that you make without manually having to rebuild and restart the containers. Happy hacking!!!

## Contributing Guidelines

Thanks for taking the time to contribute!

The following is a set of guidelines for contributing to this project. These are just guidelines, not rules, so use your best judgement and feel free to propose changes to this document in a pull request.

### Getting Started

The webapp is built with the MERN stack. So if you are new to MERN, please take time to read up about the same.

### Community

- The whole documentation, such as setting up a development environment, the project, and testing, can be read [here]().
- If you have any questions regarding the webapp, open an [issue](https://github.com/dilbwagsingh/Electronic-voting/issues/new) or ask it directly on [Linkedin](https://www.linkedin.com/in/dilbwagsingh/).

### Issue

Ensure the bug was not already reported by searching on GitHub under [issues](https://github.com/dilbwagsingh/Electronic-voting/issues). If you're unable to find an open issue addressing the bug, open a [new issue](https://github.com/dilbwagsingh/Electronic-voting/issues/new).

### Write detailed information

Detailed information is very helpful to understand an issue, for example-

- How to reproduce the issue, step-by-step.
- The expected behavior (or what is wrong).
- Screenshots for GUI issues.
- The application version.
- The operating system.

### Pull requests

Pull Requests are always welcome.

- Ensure the PR description clearly describes the problem and solution. It should include-
  - The operating system used while testing
  - The relevant issue number, if applicable.
