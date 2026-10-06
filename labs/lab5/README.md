# Lab 5 — MVC: Wire the GUI to Your Time Registration App

> **Course:** 5–Software Design — Lab  
> **Tools:** Java 21, IntelliJ IDEA, Maven Wrapper (`./mvnw`), Git  
> **Diagrams:** Visual Paradigm (Community Edition)

## Learning Outcomes

- Apply **Model–View–Controller** to separate concerns.
- Use the **Observer** pattern to keep the view in sync with the model.
- Integrate an existing GUI with your controller and model.

## Given

- Your **Controller** and **Database (Model)** from earlier labs.
- A **GUI** scaffold in the `view` package and a renewed `Main` class.

## Tasks

1. Make the **View** observe your **Model** and refresh on changes.
2. **Inject the Controller** into the View so buttons trigger the correct actions.
3. (Recommended) Extend the GUI:
    - add a panel to manage employees (separate DB/controller),
    - experiment with layouts, labels, titles, colors, etc.

## Acceptance

- Demonstrate check‑in/check‑out flow fully via the GUI.
- View updates must be driven by model changes via observer notifications.

## Build & Test (Maven wrapper)

Run the lab:

```bash
mvn clean javafx:run -pl labs/lab5
```

From the **repo root**:

- Build this lab only (and any dependent modules):
  ```bash
  ./mvnw -pl labs/lab5 -am package
  ```

- Run tests for this lab:
  ```bash
  ./mvnw -pl labs/lab5 -am test
  ```

> If the Maven tab in IntelliJ is greyed out, you likely opened the folder instead of the **root `pom.xml`**. Close the
> project and re-open the **`pom.xml`**.

## What to Deliver (Portfolio)

Create **one directory** for this lab in your portfolio and submit a **single zip**:

- **UML** (if applicable): export correctly from Visual Paradigm (XML export, then compress).
- **Code**: zip the IntelliJ/Maven module for this lab.

**Naming convention:** `Lab5_FirstnameLastname.zip`.

> Deadlines are announced on Blackboard. Follow the instructions there if they differ.
