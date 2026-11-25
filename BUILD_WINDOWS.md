# Building Flower Client on Windows

Two parts: the **mod** (Fabric, needs Java) and the **launcher** (Electron, needs Node).
You do NOT need to install Gradle — the included wrapper downloads it automatically.

## Install once
1. **JDK 21** — Eclipse Temurin: https://adoptium.net/temurin/releases/?version=21  (pick the `.msi`, install, accept "set JAVA_HOME").
2. **Node.js 20+** — https://nodejs.org (the LTS `.msi`).
   Reopen your terminal after installing so PATH updates.

Check they work (in a new PowerShell/Command Prompt):
```
java -version      (should say 21)
node -v            (should say v20 or higher)
```

## Build (run these in order)
Open **PowerShell** in the unzipped `flower-client` folder, then:

```powershell
# 1) Build the mod  (first run downloads Gradle + libraries, a few minutes)
cd mod
.\gradlew.bat build
cd ..

# 2) Put the mod jar where the launcher bundles it (skip the -sources jar)
copy mod\build\libs\flowerclient-1.0.0+1.21.1.jar launcher\resources\mods\

# 3) Build the launcher installer
cd launcher
npm install
npm run dist
```

The installer is created at:
```
launcher\dist\Flower Client Setup 1.0.0.exe
```
Run it, then launch Flower and sign in with Microsoft.

## Just want to test without making an installer?
After `npm install`, run `npm start` in the `launcher` folder — it opens the launcher directly.
It finds the mod jar in `..\mod\build\libs` automatically.

## Notes
- Windows SmartScreen will warn (the build isn't code-signed): **More info → Run anyway**.
- If `gradlew.bat` says JAVA_HOME is wrong, make sure JDK 21 is installed and reopen the terminal.
- Everything builds offline except the first mod build (downloads Gradle + Fabric libraries) and `npm install`.
