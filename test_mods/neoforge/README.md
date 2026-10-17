# NeoForge Test Mods

Place any third-party NeoForge mod `.jar` files you want to test here.

When you run:
```bash
./gradlew testNeoForge
```
All `.jar` files placed in this folder will automatically be copied to your NeoForge instance `mods` directory and tested against the target server.

> Note: All `.jar` files in this directory are ignored by Git.
