# Lab 4a — Factory Method (Extend Time Registration)

> **Course:** 5–Software Design — Lab  
> **Tools:** Java 21, IntelliJ IDEA, Maven Wrapper (`./mvnw`), Git  
> **Diagrams:** Visual Paradigm (Community Edition)

## Learning Outcomes

- Apply the **Factory Method** pattern to centralize object creation.
- Keep your main application free of concrete type knowledge.

## Tasks

1. **Copy your Lab 3 Time Registration app** into a new module/folder for **Lab 4a**.
2. Implement an `EmployeeFactory` that creates different employee types.
    - Supported roles: `Manager`, `Programmer`, `CustomerService`.
    - Provide a method:
      ```java
      public Employee getEmployee(String name, String function){}
      ```
    - Use `function` to return the correct subtype.
3. Replace direct `new` calls with the factory throughout your app.
4. Keep tests green; add/adjust tests if needed.

## Tips

- The factory encapsulates creation logic; adding a new role later should not break callers.
- Consider normalizing the `function` argument (e.g., case‑insensitive).

## Build & Test (Maven wrapper)

From the **repo root**:

- Build this lab only (and any dependent modules):
  ```bash
  ./mvnw -pl labs/lab4a -am package
  ```

- Run tests for this lab:
  ```bash
  ./mvnw -pl labs/lab4a -am test
  ```

> If the Maven tab in IntelliJ is greyed out, you likely opened the folder instead of the **root `pom.xml`**. Close the
> project and re-open the **`pom.xml`**.

## What to Deliver (Portfolio)

Create **one directory** for this lab in your portfolio and submit a **single zip**:

- **UML** (if applicable): export correctly from Visual Paradigm (XML export, then compress).
- **Code**: zip the IntelliJ/Maven module for this lab.

**Naming convention:** `Lab4a_FirstnameLastname.zip`.

> Deadlines are announced on Blackboard. Follow the instructions there if they differ.
