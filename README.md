# Emberstorm — Android app
By **ItRan Studios** · Ran Jacobi

The whole game is one offline HTML file, wrapped in a fullscreen Android WebView.
Below are two ways to get a real installable **APK** (with the ItRan launcher icon).

--------------------------------------------------------------------
## Option 1 — Build in the cloud, no tools to install  ⭐ easiest
--------------------------------------------------------------------
You only need a free GitHub account. GitHub builds the APK for you.

1. Go to github.com and create a new repository (e.g. "emberstorm").
2. Upload everything in this folder to the repo:
   - On the repo page click **Add file ▸ Upload files**, drag in all the files
     and folders from here (including the `.github` folder), then **Commit**.
   - (Or, if you use git: `git init`, `git add .`, `git commit -m init`,
     `git remote add origin <your-repo-url>`, `git push -u origin main`.)
3. Open the **Actions** tab in your repo. A run called **Build Emberstorm APK**
   starts automatically (or click it and press **Run workflow**).
4. When it finishes (green check, ~3–5 min), open the run and scroll to
   **Artifacts ▸ Emberstorm-apk**. Download it — inside is `app-debug.apk`.
5. Copy that APK to your phone and tap it. Allow "install from unknown
   sources" if prompted. Done — Emberstorm is installed with its icon.

--------------------------------------------------------------------
## Option 2 — Build locally with Android Studio
--------------------------------------------------------------------
1. Install Android Studio (Hedgehog or newer).
2. **File ▸ Open** this `EmberstormAndroid` folder; let Gradle sync.
3. Press **Run ▶** to install on a connected phone/emulator, or
   **Build ▸ Build Bundle(s)/APK(s) ▸ Build APK(s)** to get a file at
   `app/build/outputs/apk/debug/app-debug.apk`.

--------------------------------------------------------------------
## Notes
--------------------------------------------------------------------
- The debug APK is signed with Android's debug key, so it installs directly.
  For the Google Play Store you'd generate a signed *release* build
  (Android Studio ▸ Build ▸ Generate Signed Bundle / APK).
- App runs fully offline, no permissions. Portrait orientation.
- Coins, best score, and purchased abilities are saved on the device.
- To update the game, replace `app/src/main/assets/emberstorm.html` and rebuild.

## Project layout
- `app/src/main/assets/emberstorm.html` — the game
- `app/src/main/java/.../MainActivity.java` — WebView host (fullscreen, offline)
- `app/src/main/res/.../ic_launcher*` — adaptive icon from the ItRan logo
- `.github/workflows/build-apk.yml` — cloud build that outputs the APK

--------------------------------------------------------------------
## Real-money store (Google Play Billing)
--------------------------------------------------------------------
The game store has two tiers:
- **Abilities** — bought with coins you earn by playing (free progression).
- **Premium** — real money, via Google Play Billing.
- **Ember skins** — cosmetic; the premium ones unlock with the Skins Pack.

Premium products (the IDs must match exactly in Play Console):
| Product ID     | Type            | What it gives                                   |
|----------------|-----------------|-------------------------------------------------|
| `skins_pack`   | one-time        | Unlocks Plasma, Void, Solar & Aurora skins      |
| `coin_doubler` | one-time        | 2× coins from every run, forever                |
| `mega_pack`    | one-time        | All abilities + all skins + 2× coins            |
| `coins_large`  | **consumable**  | +2000 coins instantly (repeatable)              |

### One-time setup to actually charge money
1. Create a **Google Play Developer account** (one-time $25) at play.google.com/console.
2. Create the app, then under **Monetize ▸ Products ▸ In-app products**, create the
   four products above using the exact IDs. Set a price for each. Mark `coins_large`
   as **consumable** (the others are managed/one-time).
3. Build a **signed release** APK/AAB (Android Studio ▸ Build ▸ Generate Signed Bundle/APK),
   and upload it to a track (Internal testing is fastest).
4. Add your Google account under **Testing ▸ License testers** so you can test purchases
   without being charged.
5. Install the app **from the Play test link** (not a sideloaded debug APK) — Play Billing
   only works for builds delivered by Play. Prices then load automatically from Play, and
   buying unlocks the item in the game.

### How it's wired (already done in this project)
- `BillingManager.java` talks to Play Billing, queries prices, launches purchases,
  acknowledges/consumes them, and restores past purchases on launch.
- `MainActivity` exposes `window.Android.buy(sku)` to the game and pushes results back via
  `window.grantEntitlement(sku)`, `window.grantCoins(n)`, and `window.setPrices(...)`.
- In a plain browser (no billing), premium buttons show a friendly "get the app" message,
  so the game still runs everywhere.

Tip: for production you should also verify purchases on a small server (Play Developer API)
to prevent tampering. The client flow here is the standard starting point.
