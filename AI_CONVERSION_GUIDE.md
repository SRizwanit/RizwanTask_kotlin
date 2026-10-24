# RizwanTask: React → Android Kotlin (Jetpack Compose) কনভার্সন গাইড

> **এই ফাইলটা প্রতিটি নতুন AI চ্যাটের শুরুতে পেস্ট করুন।**
> এরপর শুধু বলুন: **"এখন Login স্ক্রিনের কাজ করো"** (বা Task / Profile…)। AI নিচের "স্ক্রিন কার্ড" থেকে বুঝে নেবে কোন React ফাইল পড়তে হবে, কী বানাতে হবে।

---

## ১. AI-এর জন্য নির্দেশনা (এটা আগে পড়ো)

তুমি একজন Android সিনিয়র ডেভেলপার। আমার একটা **React (Vite) ওয়েব অ্যাপ** আছে, সেটাকে **Kotlin + Jetpack Compose (Material3)** নেটিভ অ্যাপে রূপান্তর করছি। আমি একবারে **একটা স্ক্রিন** করতে বলব। নিয়ম:

1. আমি স্ক্রিনের নাম বললে নিচের **স্ক্রিন কার্ড** থেকে ওই স্ক্রিনের "React ফাইল" খুঁজো। আমি সেই ফাইলগুলোর কোড পেস্ট করব। **ফাইল না পেলে আগে আমার কাছে চাও, অনুমান করে লিখো না।**
2. React-এর **প্রতিটি ফিচার, টেক্সট (বাংলা লেবেল), ভ্যালিডেশন, এরর মেসেজ ও API কল** হুবহু রাখো। কিছু বাদ দিও না।
3. CSS থেকে রং/স্পেসিং/ফন্ট সাইজ নিয়ে Compose-এ মেলাও। রং `Theme.kt`-তে আগে থেকে থাকলে সেটাই ব্যবহার করো, নতুন রং লাগলে `Theme.kt`-তে কী যোগ করতে হবে আলাদা করে বলো।
4. আউটপুটে দেবে:
   - প্রতিটি `.kt` ফাইলের **পূর্ণ কোড** (ফাইলের পাথসহ: `app/src/main/kotlin/com/srizwan/task/...`)
   - `MainActivity.kt`-এ **কোন লাইন বদলাতে হবে** (শুধু বদলের অংশ)
   - `build.gradle.kts`-এ নতুন dependency লাগলে সেটা
   - `AndroidManifest.xml`-এ নতুন permission লাগলে সেটা
5. কোড কেটে ছোট করো না, "বাকিটা আগের মতো" লিখো না। কোড বড় হলে আমাকে বলো "পার্ট ১/২" করে দিচ্ছ।
6. একটা ফাইলের কাজ শেষ হলে শেষে লিখো: **"✅ শেষ: ___ | পরের ধাপ: ___"**
7. API key বা পাসওয়ার্ড সোর্স কোডে হার্ডকোড করো না; `BuildConfig`/`local.properties` ব্যবহার করো।

---

## ২. প্রজেক্টের অবস্থা

- **প্যাকেজ:** `com.srizwan.task`
- **বর্তমান Kotlin ফাইল** (`app/src/main/kotlin/com/srizwan/task/`):
  - `App.kt`, `MainActivity.kt` (NavHost + `Routes` অবজেক্ট), `Session.kt`
  - `Theme.kt` (`RizwanTheme`), `AppFonts.kt`
  - `SplashScreen.kt` ✅ **শেষ**
  - `DebugActivity.kt`
- **`MainActivity.kt`-এ বর্তমান রুট:** `splash`, `login`, `task`। `login` ও `task` এখনো `PlaceholderScreen`
- **নেভিগেশন:** `androidx.navigation.compose` (`NavHost` / `composable`)
- **ব্যাকএন্ড:** Supabase REST (`/rest/v1/...`), সাপোর্ট চ্যাটে Firebase Realtime Database
- **ভাষা/UI টেক্সট:** বাংলা

### React → Compose সাধারণ রূপান্তর নীতি

| React | Compose |
|---|---|
| `useState` | `remember { mutableStateOf() }` / ViewModel-এর `StateFlow` |
| `useEffect` | `LaunchedEffect` / `DisposableEffect` |
| `useMemo`/`useCallback` | `remember(keys)` |
| `go('login')` (`lib/nav.js`) | `navController.navigate(Routes.LOGIN)` |
| `localStorage` | `SharedPreferences` / DataStore (`Session.kt`) |
| `fetch` Supabase | Retrofit/Ktor বা `HttpURLConnection` + coroutines |
| `<dialog>` / Dialog কম্পোনেন্ট | `AlertDialog` / `Dialog` |
| `toast()` | `Snackbar` বা `Toast` |
| `ThemeToggle` | `Theme.kt`-এর dark/light স্টেট |
| `.css` ফাইল | `Modifier`, `MaterialTheme`, `Theme.kt` |

বড় স্ক্রিনে লজিক রাখবে `XxxViewModel.kt`-এ, UI রাখবে `XxxScreen.kt`-এ।

---

## ৩. কাজের ক্রম (এই ক্রমেই করবে)

| ধাপ | স্ক্রিন | অবস্থা |
|---|---|---|
| 0 | **Data Layer** (`common.js`) | ☐ |
| 1 | Splash | ✅ শেষ |
| 2 | Login | ✅ |
| 3 | Signup | ✅ |
| 4 | Task (4a–4e) | ✅ |
| 5 | Profile | ✅ |
| 6 | Support | ☐ |
| 7 | View | ☐ |
| 8 | Admin | ☐ |

(কাজ শেষ হলে এখানে ☐ বদলে ✅ করে ফাইলটা আপডেট করে রাখুন।)

---

## ৪. স্ক্রিন কার্ড

> React সোর্স রুট: `rizwantask_react-main/src/`

### ধাপ 0: Data Layer ("ডাটা লেয়ার করো")

- **React ফাইল:** `lib/common.js`, `lib/nav.js`, `lib/usePinNav.js`
- **বানাতে হবে (প্রস্তাবিত):**
  - `data/Api.kt`: Supabase REST ক্লায়েন্ট (get/post/patch/delete, header: `apikey` + `Authorization: Bearer`)
  - `data/Phone.kt`: বাংলাদেশি ফোন নম্বর নরমালাইজ (`Phone.normBD`), অ্যাডমিন ফোন চেক
  - `data/PinHash.kt`: `sha256(salt + ":" + pin)`, `newSalt()`, `verifyPin`, `hasPin`
  - `data/UserRepo.kt`: `findUserByPhone`, `findUserByDevice`, `requestOtp`, `verifyOtp`
  - `data/ActivityLog.kt`: `log(action, detail)`
  - `Session.kt` আপডেট (session get/set/clear, deviceId)
- **নির্ভর করে:** কিছুতে না
- **লক্ষ্য:** `requireAuth`, `blockedScreen`, `touchActive` লজিকও এখানে ঠিকমতো আনা
- **dependency:** coroutines, JSON পার্সার (kotlinx.serialization/Gson), HTTP ক্লায়েন্ট
- **সতর্কতা:** `SB_URL`, `SB_KEY`, `ADMIN_PHONE`, `ADMIN_WA` মান `BuildConfig`-এ রাখো (`.env.example` দেখো)।

### ধাপ 1: Splash ✅ শেষ

- **React:** `pages/Splash.jsx` + `Splash.css`
- **Kotlin:** `SplashScreen.kt` (তৈরি আছে)

### ধাপ 2: Login ("Login স্ক্রিনের কাজ করো")

- **React ফাইল:** `pages/Login.jsx`, `lib/common.js` (ধাপ 0 দেখো), `components/Icon.jsx` (লোগো), `components/ThemeToggle.jsx`, `lib/PageShell.jsx`, `styles/common.css`, `styles/theme.css`
- **বানাতে হবে:** `LoginScreen.kt`, `LoginViewModel.kt`
- **নির্ভর করে:** ধাপ 0
- **MainActivity-তে:** `composable(Routes.LOGIN)`-এর `PlaceholderScreen` বদলে `LoginScreen`; সফল হলে `Routes.TASK`, সাইনআপ লিংকে `Routes.SIGNUP`
- **নোট:** ফোন + পিন/OTP ফ্লো, ডিভাইস লক, ব্লকড ইউজার হ্যান্ডলিং হুবহু রাখো

### ধাপ 3: Signup ("Signup স্ক্রিনের কাজ করো")

- **React ফাইল:** `pages/Signup.jsx`, `lib/common.js`, `components/Icon.jsx`
- **বানাতে হবে:** `SignupScreen.kt` (+ দরকারে `SignupViewModel.kt`)
- **নির্ভর করে:** ধাপ 0, 2
- **MainActivity-তে:** `Routes.SIGNUP = "signup"` যোগ

### ধাপ 4: Task, হোম স্ক্রিন ("Task স্ক্রিনের কাজ করো")

**এটা বড়, তাই ৫টা উপ-ধাপে ভাগ করো।** আমি যে উপ-ধাপ বলব শুধু সেটা করবে।

| উপ-ধাপ | React ফাইল | Kotlin |
|---|---|---|
| 4a. কাঠামো ও স্টেট | `pages/Task.jsx`, `Task.css`, `pages/task/helpers.js` | `TaskScreen.kt` (শেল + নিচের ৩ ট্যাবের NavigationBar: টাস্ক / পেমেন্ট / র‍্যাংক), `TaskViewModel.kt` |
| 4b. টাস্ক ট্যাব | `pages/task/Dashboard.jsx`, `pages/task/BookGrid.jsx` | `TaskDashboard.kt`, `BookGrid.kt` (দৈনিক লক্ষ্য, টানা দিন, চালিয়ে যান, কিতাবের তালিকা + সার্চ + অগ্রগতি) |
| 4c. পেমেন্ট ট্যাব | `pages/task/PayPane.jsx` | `PayPane.kt` |
| 4d. র‍্যাংক ট্যাব | `pages/task/RankPane.jsx` | `RankPane.kt` |
| 4e. ডায়ালগ ও গাইড | `pages/task/Dialogs.jsx` (GoalDialog, NoticeDialog, RequestDialog), `GuideDialog.jsx`, `guideScenes.js` (**গাইডের অ্যানিমেশন, পরে/ঐচ্ছিক**), `guideImages.js` (**৪০KB, ইমেজ ডেটা, আমি বললে তবেই**) | `TaskDialogs.kt`, `GuideDialog.kt` |

- **নির্ভর করে:** ধাপ 0, 2
- **MainActivity-তে:** `composable(Routes.TASK)` বদলাও
- **নোট:** `Task.jsx`-এর ট্যাব: `task`, `pay`, `rank`। `page_proofs` টেবিল থেকে ইউজারের অগ্রগতি আসে। কিতাবে ট্যাপ করলে `view?book=..&page=..` রুটে যায় (ধাপ 7)।

### ধাপ 5: Profile ("Profile স্ক্রিনের কাজ করো")

- **React ফাইল:** `pages/Profile.jsx` (322 লাইন), `Profile.css`, `lib/common.js`, `components/Icon.jsx`, `components/ThemeToggle.jsx`
- **বানাতে হবে:** `ProfileScreen.kt`, `ProfileViewModel.kt`
- **নির্ভর করে:** ধাপ 0, 2
- **MainActivity-তে:** `Routes.PROFILE = "profile"`
- **নোট:** বড় ফাইল। AI বললে দুই পার্টে ভাগ করে দেবে।

### ধাপ 6: Support ("Support স্ক্রিনের কাজ করো")

- **React ফাইল:** `pages/Support.jsx`, `Support.css`, `lib/common.js`
- **বানাতে হবে:** `SupportScreen.kt`, `SupportViewModel.kt`
- **নির্ভর করে:** ধাপ 0, 2
- **dependency:** Firebase Realtime Database (`firebase-database`) + `google-services.json`
- **নোট:** `push`, `onChildAdded`, `onValue`, `update` = Firebase Android SDK-এর `ChildEventListener`/`ValueEventListener`। Firebase কনফিগ `Support.jsx`-এর ভেতরে আছে, সেটা `google-services.json` দিয়ে বদলাতে হবে।

### ধাপ 7: View, কিতাব পড়ার স্ক্রিন ("View স্ক্রিনের কাজ করো")

**সবচেয়ে বড় (`View.jsx` 1239 লাইন + `View.logic.js` 2284 লাইন)। একসাথে দেবে না, উপ-ধাপে করো।**

| উপ-ধাপ | React ফাইল | Kotlin |
|---|---|---|
| 7a. কনফিগ, স্টোরেজ, ইউটিল | `view/config.js`, `view/storage.js`, `view/utils.js`, `view/hooks.js` | `view/ViewConfig.kt`, `view/ViewStorage.kt`, `view/ViewUtils.kt` |
| 7b. মূল রিডার UI | `View.jsx`, `View.css`, `View.logic.js` (**আমি অংশ অংশ করে দেব**), `view/Highlight.jsx`, `view/ImageLoader.jsx`, `view/Sprite.jsx` | `ViewScreen.kt`, `ViewViewModel.kt` |
| 7c. ছোট UI | `view/Dialog.jsx`, `view/Dropdown.jsx`, `view/Toasts.jsx`, `view/toaster.js`, `view/useRipple.js` | `ViewDialogs.kt` (Compose-এ ripple/toast বিল্ট-ইন, তাই সরল হবে) |
| 7d. সেটিংস | `view/SettingsDialog.jsx` | `ViewSettingsDialog.kt` |
| 7e. TTS (ভয়েস পড়া) | `view/TtsController.js`, `view/ttsEngines.js`, `view/TtsDialog.jsx` | `tts/TtsController.kt`, `tts/TtsEngines.kt`, `TtsDialog.kt` (Android `TextToSpeech` / MediaPlayer) |
| 7f. এআই ও GitHub | `view/gemini.js`, `view/github.js`, `view/ApiDialogs.jsx` | `data/GeminiClient.kt`, `data/GithubClient.kt`, `ApiDialogs.kt` |

- **নির্ভর করে:** ধাপ 0, 4
- **MainActivity-তে:** `Routes.VIEW = "view?book={book}&page={page}"`, আর্গুমেন্টসহ
- **নোট:** `View.logic.js` অনেক বড়। আমি যখন একটা অংশ পেস্ট করব, শুধু ওই অংশের Kotlin দেবে এবং কোন ফাংশন কোন Kotlin ফাইলে গেল তার তালিকা রাখবে।

### ধাপ 8: Admin ("Admin স্ক্রিনের কাজ করো")

**প্রতিটা ট্যাব আলাদা কাজ। আমি যে ট্যাব বলব শুধু সেটা করবে।**

| উপ-ধাপ | React ফাইল | Kotlin |
|---|---|---|
| 8a. কাঠামো ও ডাটা | `pages/Admin.jsx`, `Admin.css`, `admin/useAdminData.js`, `admin/useTickets.js`, `admin/helpers.js`, `admin/ui.jsx` | `admin/AdminScreen.kt`, `AdminViewModel.kt`, `AdminUi.kt` |
| 8b. ড্যাশবোর্ড | `admin/DashTab.jsx` | `admin/DashTab.kt` |
| 8c. ইউজার | `admin/UsersTab.jsx` | `admin/UsersTab.kt` |
| 8d. রিভিউ | `admin/ReviewTab.jsx` | `admin/ReviewTab.kt` |
| 8e. পেমেন্ট | `admin/PayTab.jsx` | `admin/PayTab.kt` |
| 8f. কিতাব | `admin/BooksTab.jsx` | `admin/BooksTab.kt` |
| 8g. নোটিশ | `admin/NoticeTab.jsx` | `admin/NoticeTab.kt` |
| 8h. সাপোর্ট | `admin/SupportTab.jsx` | `admin/SupportTab.kt` |
| 8i. লগ | `admin/LogTab.jsx` | `admin/LogTab.kt` |
| 8j. সেটিংস | `admin/SettingsTab.jsx` | `admin/SettingsTab.kt` |

- **নির্ভর করে:** ধাপ 0, 2
- **MainActivity-তে:** `Routes.ADMIN = "admin"`
- **সতর্কতা:** শুধু অ্যাডমিন ফোন নম্বরের ইউজার ঢুকতে পারবে (`requireAuth({admin:true})`-এর লজিক)।

---

## ৫. আমি যেভাবে প্রতিবার বলব (কপি-পেস্ট টেমপ্লেট)

### নতুন স্ক্রিন শুরু

```
এখন [স্ক্রিনের নাম / উপ-ধাপ] এর কাজ করো।

React ফাইল:
--- [ফাইলের নাম] ---
[কোড]

আমার বর্তমান Kotlin ফাইল:
--- Theme.kt ---
[কোড]
--- MainActivity.kt ---
[কোড]
(আর যা যা এই স্ক্রিনের সাথে দরকার, যেমন Session.kt, data লেয়ার)
```

### বড় ফাইলের পরের অংশ

```
এটা [ফাইলের নাম]-এর পার্ট ২। আগের পার্টে যে Kotlin ফাইল বানিয়েছ সেটার সাথে জুড়ে এই অংশ করো।
[কোড]
```

### এরর ঠিক করা

```
বিল্ড এরর এসেছে। ফাইল: [নাম]
এরর: [এরর টেক্সট]
কোড: [কোড]
```

### ধাপ শেষ করার পর

```
এই ধাপ শেষ। ম্যাপে ✅ দাও এবং পরের ধাপের জন্য আমাকে কোন কোন ফাইল পেস্ট করতে হবে বলো।
```

---

## ৬. টোকেন বাঁচানোর টিপস

- প্রতি চ্যাটে **এই গাইড + ওই স্ক্রিনের ফাইল + `Theme.kt` + `MainActivity.kt`** দিন, পুরো প্রজেক্ট নয়।
- `guideImages.js`, `package-lock.json`, `View.logic.js`-এর মতো বড় ফাইল শুধু ওই নির্দিষ্ট উপ-ধাপে দিন।
- একটা চ্যাটে একটা স্ক্রিন বা উপ-ধাপ। চ্যাট লম্বা হলে নতুন চ্যাটে এই গাইড দিয়ে শুরু করুন।
- প্রতি ধাপ শেষে বিল্ড করে দেখে নিন। এরর জমতে দেবেন না।
