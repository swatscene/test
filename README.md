# Crystal Macro — Build via GitHub (no setup needed)

This mod only breaks crystals YOU placed (tracks your right-clicks).

## Get a pre-built jar in 5 minutes

### Step 1 — GitHub account
Make a free account at https://github.com/signup (if you don't have one).

### Step 2 — Create a new repo
1. Go to https://github.com/new
2. Repo name: anything, e.g. `crystalmacro`
3. Set it to **Public** (Actions are free for public repos)
4. Click **Create repository**

### Step 3 — Upload these files
1. On your new repo page, click **"uploading an existing file"** (it's a link in the middle of the page)
2. Drag the ENTIRE contents of this folder into the upload area
   - Make sure you include the hidden `.github` folder (with `workflows/build.yml`)
   - If `.github` doesn't show on your file manager, enable "show hidden files"
3. Scroll down, click **Commit changes**

### Step 4 — Wait for the build
1. Click the **Actions** tab at the top of the repo
2. You'll see a workflow run starting (yellow circle)
3. Wait ~3 minutes for it to turn into a green checkmark

### Step 5 — Download your jar
1. Click the completed workflow run
2. Scroll to the bottom — there's an **Artifacts** section
3. Download **crystalmacro-jar**
4. Unzip it — inside is `crystalmacro-1.0.0.jar`

### Step 6 — Install in Minecraft
1. Put `crystalmacro-1.0.0.jar` in `%appdata%\.minecraft\mods`
2. Also need Fabric API for 1.21.11: https://modrinth.com/mod/fabric-api/version/0.141.1+1.21.11
3. Launch Minecraft with the Fabric Loader profile
4. In-game press **R** to toggle the macro

## How it works

While the macro is ON and you're holding crystals:
- Each right-click is recorded with the position you're aiming at
- When a crystal entity appears at that position, it's tagged as yours
- The macro instantly hits any crystal in your "tagged" list
- Crystals others placed are ignored

## Tweaking

In `src/main/java/com/crystalmacro/CrystalMacro.java`:

| Constant      | Default | Effect                              |
|---------------|---------|-------------------------------------|
| `REACH`       | 5.0     | Max distance to hit a crystal       |
| `COOLDOWN_T`  | 2       | Ticks between hits (lower = faster) |

Edit values, commit the change, and GitHub will auto-rebuild.
