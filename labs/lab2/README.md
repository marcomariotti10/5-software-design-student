# Lab 2 — Testing + UML: Class Diagrams (Inheritance / Abstraction / Interfaces / Relations)

> **Course:** 5–Software Design — Lab  
> **Tools:** Java 21, IntelliJ IDEA, Maven Wrapper (`./mvnw`), Git  
> **Diagrams:** Visual Paradigm (Community Edition)

## Learning Outcomes

- Write **unit** and **integration** tests.
- Create **Class diagrams** with correct attributes, methods, and relationships.
- Translate diagrams to code and vice versa.
- Apply OO concepts: **inheritance**, **abstraction**, **interfaces**, **associations/aggregation/composition**.

## Class Diagrams (4 Mini‑assignments)

1. **Inheritance** — Daily wage calculation for multiple employees.
    - `Employee`: `(hourly salary * hours worked)`
    - `Programmer`: `(hourly salary * hours worked) + (bug bonus * #bugs)`
    - `CustomerService`: `(hourly salary * hours worked) + (customer bonus * #customers)`
    - `DepartmentOfficer`: `(hourly salary * hours worked) + (company bonus)`

2. **Abstractions** — `Shape` with abstract `calcCircumference()` and `calcArea()`
    - Implement `Circle`, `Square`, and `EquiliteralTriangle`.
    - In `main`, instantiate multiple shapes and compute both measures.
    - Provide tests.

3. **Interfaces** — *Universal remote* (`VolumeDevice` interface).
    - User can raise/lower volume of multiple devices via a `Remote`.
    - Add new devices by implementing the interface.
    - Design the class diagram first, then implement. Tests are optional but encouraged.

4. **Relations** — *Film Festival*
    - Create a class Diagram based on the code.
        - Look at the correct relation type
            - Composition
            - Aggregation
            - Association
            - Inheritance
        - Multiplicity
        - Variables/Methods/Constructors
        - Abstract/Interface
        - ...

## Where to Code

The starter project is a standard Maven + IntelliJ Java project. The methods you must implement are **already declared**
and marked with `TODO` comments. Use one of these approaches to locate them:

- In IntelliJ: **View → Tool Windows → TODO** to list all TODOs in the project.
- Use **Navigate → Symbol** (⌘⌥O / Ctrl+Alt+Shift+N) and search for the method names below.

Keep your implementation simple and readable so it maps cleanly to the class diagrams.

**Diagrams**

- Use **Visual Paradigm (Community Edition)** for all diagrams.
- Export your diagrams as instructed in the presentation.