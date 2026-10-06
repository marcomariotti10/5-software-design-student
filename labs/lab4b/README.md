# Mini Sokoban — Factory Method (Teacher)

## Run (Maven + JavaFX)

```bash
cd labs/lab4b
mvn clean javafx:run
```

or

```bash
mvn clean javafx:run -pl labs/lab4b
```

Keys:

- Arrow keys: move/push
- `1`: Warehouse level (B → StandardBox)
- `2`: Glacier level (B → IceBox)

Factory Method:

- `Level.build(...)` parses the same ASCII map but calls `create(char,x,y)` to instantiate symbols.
- `WarehouseLevel` vs `GlacierLevel` override `create(...)` to choose **different box products** for `B`.
- Engine (`World`, input, rendering) unchanged.
