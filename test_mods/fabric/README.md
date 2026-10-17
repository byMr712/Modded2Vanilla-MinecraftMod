# Fabric Test Mods

Place any third-party Fabric mod `.jar` files you want to test here.

When you run:
```bash
./gradlew testFabric
```
All `.jar` files placed in this folder will automatically be copied to `run/mods/` and tested against the target server.

> Note: All `.jar` files in this directory are ignored by Git.
