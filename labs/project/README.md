# Lab 6 — Project Kick‑off: Money Tracker (Groups of 2)

> **Course:** 5–Software Design — Lab  
> **Tools:** Java 21, IntelliJ IDEA, Maven Wrapper (`./mvnw`), Git  
> **Diagrams:** Visual Paradigm (Community Edition)

## Problem

When traveling in a group, people pay different things (some split evenly, some not). At the end, who owes whom how
much?

## Functional Requirements

- Add/remove **People**.
- Enter **Tickets/Expenses** with payer and amount.
- Support at least two ticket kinds:
    1) **Split evenly**, 2) **Itemized / not evenly split**.
- Compute a **global bill**: who should pay whom, and how much.
- Provide a **functional GUI** (looks are secondary).

## Non‑functional Requirements

- Use patterns from labs: **Singleton, Observer, (Abstract) Factory, MVC**.
- Include **≥ 1 additional pattern** from theory (e.g., Strategy, Decorator, Command, Adapter, Facade, Proxy, Iterator,
  State, Template Method, Composite).
- Provide **UML**:
    - Class diagram of the whole app (GUI may be simplified to one `GUI` box).
    - Use Case diagram.
    - Sequence diagram for “**Calculate for whole trip**”.
- **Testing**:
    - Unit tests for ≥ one core class.
    - ≥ one integration test.

## Collaboration

- Work in pairs (register your teammate on Blackboard).
- Use **Git/GitHub** for version control (branches, commits, PRs).

## Suggested Milestones

1. Domain model + class diagram.
2. Core data structures & algorithms for settlements.
3. Basic GUI + wiring (MVC, observer).
4. Extra patterns + polish.
5. Tests & packaging.

## Build & Test (Maven wrapper)

From the **repo root**:

- Build this lab only (and any dependent modules):
  ```bash
  ./mvnw -pl labs/lab6 -am package
  ```

- Run tests for this lab:
  ```bash
  ./mvnw -pl labs/lab6 -am test
  ```

> If the Maven tab in IntelliJ is greyed out, you likely opened the folder instead of the **root `pom.xml`**. Close the
> project and re-open the **`pom.xml`**.

## What to Deliver (Portfolio)

Create **one directory** for this lab in your portfolio and submit a **single zip**:

- **UML** (if applicable): export correctly from Visual Paradigm (XML export, then compress).
- **Code**: zip the IntelliJ/Maven module for this lab.

**Naming convention:** `Lab6_FirstnameLastname.zip`.

> Deadlines are announced on Blackboard. Follow the instructions there if they differ.
