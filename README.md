# 📚 Nena Sayura

**Nena Sayura** is an Android study app for Sri Lankan students. It brings past papers, marking schemes, guide books, quizzes and teacher-made notes into one place, and builds each student a personal study timetable that adapts as the exam gets closer.

Built with **Kotlin**, **Jetpack Compose** and **Firebase**.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Backend](https://img.shields.io/badge/backend-Firebase-FFCA28?logo=firebase&logoColor=black)
![Min SDK](https://img.shields.io/badge/minSdk-26-blue)

---

## 📑 Table of Contents

- [Features](#-features)
- [User Roles](#-user-roles)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Firebase Setup](#-firebase-setup)
- [Password Reset Web Page](#-password-reset-web-page)
- [Running Tests](#-running-tests)
- [Git Workflow](#-git-workflow)
- [Security Notes](#-security-notes)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)

---

## ✨ Features

### 🎓 Study Resources
- **Past, Model, Provincial and Target papers**, filtered by stream, subject and year
- Provincial papers can be filtered by **term (1, 2, 3)** and by **province** (all 9 provinces)
- **Marking schemes** for every subject
- **Guide books** organised by stream and subject
- **In-app file viewer** for PDFs and images, plus a **Downloads** screen for offline access
- **YouTube** video lessons played inside the app
- **Search** across all study content

### 🧠 Quizzes and Progress
- Multiple-choice quizzes per subject, with explanations for every answer
- Quiz attempts are saved, so students can see their results over time
- **Study Progress** analyser that rates each subject (*Not started / Needs work / Getting there / Strong*) and shows whether the trend is improving, steady or slipping

### 🗓️ Smart Study Planner
- A personal timetable generated from the student's goal, learning style, confidence per subject and exam date
- The plan changes with the exam phase:

  | Phase | Time left | Focus |
  |---|---|---|
  | Build | more than 120 days | Learning new material |
  | Practice | 30 to 120 days | Papers and quizzes |
  | Revise | 8 to 29 days | Revision |
  | Exam week | 7 days or less | Final revision |

- Session types: video lesson, short note, resource book, paper practice, marking review, quiz, revision, or custom
- Week and day views, session status (planned, done, missed), and a **weekly review** that adjusts the plan
- Planner logic is plain Kotlin with no Android or Firestore dependency, so the same input always gives the same plan

### ⏰ Reminders
- Study reminders using exact alarms and notifications
- Alarms are restored automatically after a reboot, app update, or time zone change
- Background sync with WorkManager

### 👩‍🏫 Teacher Marketplace
- Teachers upload **short notes, tutes, model papers, notes and video links**
- Each upload is either **Free** (all students) or **Paid** (only students who paid that teacher)
- Uploads go through **admin review** (pending, approved, declined) before students can see them
- Students can browse teachers, view materials, pay, and leave **reviews**

### 💰 Payments and Payouts
- Student payments to teachers are recorded in Firestore
- A configurable **admin commission** (default 10%) is applied to each payment
- Admins record **payouts** to teachers with a bank reference and receipt upload
- Payout history is permanent and never edited or deleted
- Teachers get their own dashboard, payments tab and notifications

### 🛠️ Admin Panel
- Dashboard with users, content and payments overview
- Manage papers, marking schemes, guide books, quizzes and streams/subjects
- Review teacher uploads
- **Staff management** with role-based access (see below)

### 🔐 Authentication
- Email and password sign-up and login
- **Google Sign-In** through Credential Manager
- Forgot and reset password flow, with deep links that open the app
- Guests can browse limited content without an account

---

## 👥 User Roles

| Role | What they can do |
|---|---|
| **Guest** | Browse papers, marking schemes, guide books and quizzes without logging in. Locked features ask the user to sign in. |
| **Student** | Everything a guest can do, plus the study planner, reminders, downloads, progress analytics, teacher content, payments and reviews. |
| **Teacher** | Upload and manage content, see earnings and payouts, and receive notifications. |
| **Admin / Staff** | Manage the platform. Access is controlled by permissions. |

### Admin staff roles

Staff accounts are given permissions (`users`, `payments`, `papers`, `marking_schemes`, `guide_books`, `quizzes`, `teacher_uploads`, `streams`, `staff`). Ready-made roles pre-tick the right boxes:

| Staff role | Access |
|---|---|
| Full Admin | Everything |
| Content Manager | Papers, marking schemes, guide books, quizzes, teacher uploads, streams |
| Finance Manager | Payments and users |
| User Support | Users |
| Custom | Hand-picked permissions |

> Old admin accounts with no `permissions` field are treated as full access.

---

## 🧰 Tech Stack

| Area | Technology |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel, StateFlow, Lifecycle Compose |
| Auth | Firebase Authentication, Credential Manager, Google ID |
| Database | Cloud Firestore |
| File storage | Cloudinary (PDFs and images), Firebase Storage |
| Networking | OkHttp |
| Background work | WorkManager, AlarmManager |
| Local settings | Jetpack DataStore |
| Hosting | Firebase Hosting (password reset page) |
| Build | Gradle (Kotlin DSL), version catalog, Android Gradle Plugin 9.3 |

**Android config:** `minSdk 26`, `targetSdk 35`, `compileSdk 37`, Java 11.

---

## 🏗️ Architecture

The app follows an MVVM style with a repository layer:

```
UI (Compose screens)  ->  ViewModel  ->  Repository  ->  Firebase / Cloudinary
                                  \->  core/ (pure Kotlin logic)
```

- **`ui/`** holds the screens, grouped by role (guest, student, teacher, admin)
- **`data/repository/`** is the only layer that talks to Firestore and Cloudinary
- **`core/`** holds business logic that is easy to unit test (planner, progress analyser, reminders, session)

---

## 📁 Project Structure

```
Nena-Sayura-App/
├── app/
│   ├── google-services.json            # Firebase config (see Firebase Setup)
│   └── src/
│       ├── main/java/lk/nenasayura/app/
│       │   ├── MainActivity.kt
│       │   ├── navigation/             # Routes and AppNavGraph
│       │   ├── screens/                # Login, Register, Splash, Role select, Setup...
│       │   ├── viewmodel/              # Auth and Setup view models
│       │   ├── components/             # Shared components (Google button)
│       │   ├── core/
│       │   │   ├── planner/            # PlanGenerator, WeekReviewer, ResourceAttacher
│       │   │   ├── progress/           # ProgressAnalyzer
│       │   │   ├── reminders/          # Scheduler, receivers, notifications, sync worker
│       │   │   ├── browse/             # Stream and subject selection logic
│       │   │   └── session/            # User session providers
│       │   ├── data/
│       │   │   ├── model/              # Paper, Quiz, Payment, Payout, StudyPlan...
│       │   │   ├── repository/         # Firestore repositories
│       │   │   └── local/              # DataStore (reminder preferences)
│       │   ├── ui/
│       │   │   ├── guest/              # Papers, marking schemes, guide books, quizzes, search
│       │   │   ├── student/            # Home, timetable, reminders, progress, teachers, reviews
│       │   │   ├── teacher/            # Dashboard, upload, my content, payments
│       │   │   ├── admin/              # Dashboard, users, content, payments, staff
│       │   │   ├── common/             # Shared UI (viewers, cards, dialogs)
│       │   │   └── theme/              # Colors, typography, theme
│       │   └── util/                   # Cloudinary uploader, downloads, validators, helpers
│       ├── test/                       # Unit tests (planner)
│       └── androidTest/                # Instrumented tests
├── nena-reset/                         # Firebase Hosting site for password reset
│   ├── public/                         # reset.html, index.html, 404.html, assetlinks.json
│   └── firebase.json
├── gradle/libs.versions.toml           # Version catalog
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (a recent version that supports AGP 9.x)
- **JDK 11** or newer (Gradle can download the toolchain automatically)
- An Android device or emulator running **Android 8.0 (API 26)** or higher
- A **Firebase project** (see below)

### Clone and open

```bash
git clone https://github.com/Janana2006/Nena-Sayura-App.git
cd Nena-Sayura-App
```

Then open the folder in Android Studio and let Gradle sync.

### Build from the command line

```bash
# Debug APK
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug
```

On Windows use `gradlew.bat` instead of `./gradlew`.

> `local.properties` (your Android SDK path) is created by Android Studio and is ignored by Git.

---

## 🔥 Firebase Setup

The app will not run without a Firebase project.

1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an **Android app** with the package name `lk.nenasayura.app`.
3. Add your **SHA-1 and SHA-256** debug fingerprints (needed for Google Sign-In):
   ```bash
   ./gradlew signingReport
   ```
4. Download `google-services.json` and place it in the `app/` folder.
5. Enable these services:
   - **Authentication**: Email/Password and Google
   - **Cloud Firestore**
   - **Storage** (optional, if used)
6. In `app/src/main/res/values/strings.xml`, set `web_client_id` to the **Web client ID** from your Firebase Google sign-in settings.
7. Set up **Cloudinary** for PDF and image uploads:
   - Create an **unsigned upload preset**
   - Update `CLOUD_NAME` and `UPLOAD_PRESET` in `app/src/main/java/lk/nenasayura/app/util/CloudinaryUploader.kt`

### Firestore collections

| Collection | Purpose |
|---|---|
| `users` | Profiles, roles, study settings, admin permissions, plans and reminders |
| `streams` | Streams, subjects and subject groups |
| `papers` | Past, model, provincial and target papers |
| `guideBooks` | Guide books |
| `quizzes` and `questions` | Quizzes and their MCQs |
| `teacher_content` | Teacher uploads and their review status |
| `payments` | Student payments to teachers |
| `payouts` | Admin payments to teachers (permanent history) |
| `settings` | App settings such as the `commission` percentage |

Streams can be seeded from the app using `StreamSeeder`.

### Firestore security rules

Write rules so that students cannot read or edit each other's data, only teachers can create their own content, and only admins with the right permission can change papers, payments and payouts. Do not ship with open test-mode rules.

---

## 🌐 Password Reset Web Page

The `nena-reset/` folder is a small **Firebase Hosting** site. It handles password reset links and hosts `assetlinks.json` so reset links can open the app directly (Android App Links).

```bash
cd nena-reset
npm install -g firebase-tools
firebase login
firebase deploy --only hosting
```

The Firebase project ID is set in `nena-reset/.firebaserc`. Change it to your own project.

---

## 🧪 Running Tests

```bash
# Unit tests (planner, week reviewer, resource attacher)
./gradlew test

# Instrumented tests (needs a device or emulator)
./gradlew connectedAndroidTest
```

---

## 🌿 Git Workflow

Work is done on feature branches and merged into `main`.

| Branch | Purpose |
|---|---|
| `main` | Stable, merged work |
| `guest_module` | Guest browsing features |
| `Teacher_module` | Teacher features |
| `feature/admin-module`, `admin-module2` | Admin panel |
| `feature/cloudinary-papers` | Paper uploads with Cloudinary |

Suggested flow:

```bash
git checkout -b feature/your-feature
# ...make changes...
git add .
git commit -m "feat: short description"
git push origin feature/your-feature
# then open a Pull Request into main
```

---

## 🔒 Security Notes

- Never commit `local.properties`, keystores (`*.jks`, `*.keystore`) or any API secret.
- `google-services.json` identifies your Firebase project. Many teams keep it out of public repos. If this repository is public, make sure your Firestore and Storage **security rules** are strict, and restrict your API keys in Google Cloud Console.
- Use an **unsigned** Cloudinary preset with limits (file type, size, folder) and never put the Cloudinary API secret in the app.
- `DevSessionProvider` is a **temporary test stub** and does not authenticate anyone. Do not use it in release builds.

---

## 🗺️ Roadmap

- [ ] Enable code shrinking (R8) for release builds
- [ ] Add more unit tests for repositories and view models
- [ ] Online payment gateway integration
- [ ] CI pipeline (build and test on every pull request)
- [ ] Play Store release

---

## 🤝 Contributing

1. Fork the repository
2. Create a branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "feat: add my feature"`
4. Push the branch: `git push origin feature/my-feature`
5. Open a Pull Request

Please keep changes focused, follow the existing package structure, and make sure `./gradlew test` passes.

---

## 📄 License

No license has been added yet. Add a `LICENSE` file (for example MIT) before making the project open source.

---

<p align="center">Made with ❤️ for Sri Lankan students</p>
