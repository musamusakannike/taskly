# Taskly - AI-Powered Offline-First Task Management

Taskly is a production-grade Android task and project management application built with Kotlin and Jetpack Compose. Inspired by modern productivity suites like Todoist, it combines an offline-first architecture (local Room database as the immediate source of truth with Firebase Firestore cloud synchronization) with Gemini AI assistance: natural-language quick capture, recursive task breakdowns, smart daily agenda generation, and an interactive task assistant.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX choices have been aligned based on your preferences:

- **Confirmed Decision 1 (Storage & Sync)**: Full Firebase synchronization with Google Sign-In and an offline-first Room SQLite cache. The local database acts as the single source of truth for instant zero-latency UI updates, while a bi-directional Sync Engine pushes and pulls changes to Firestore with Last-Write-Wins (LWW) field timestamps and deletion tombstones.
- **Confirmed Decision 2 (Visual Aesthetic)**: Modern Todoist red (`#DC4C3E`) accent with warm charcoal surfaces (`#1F1F1F`, `#121212` in dark mode; clean off-white `#FBFBFB` in light mode), crisp typography, clear priority tags (P1 `#DE4C4A`, P2 `#F49C18`, P3 `#4073D6`, P4 `#808080`), and fluid gestures (swipe-to-complete, swipe-to-reschedule, drag-to-reorder).
- **Confirmed Decision 3 (AI Interaction Priority)**: Prominent quick natural-language task entry modal on the primary floating action button (FAB) with live token preview chips (detecting dates, times, recurrence, priorities `#p1`, labels `@phone`, and projects `#Work`) before saving, accompanied by dedicated AI Daily Planner and Task Breakdown tools.

---

## 1. Overview & Core Concept

- **What It Does**: Taskly organizes your work and personal life through deeply nested projects and subtasks, rich task metadata (markdown descriptions, recurrence rules, custom reminders, labels, comments, and attachments), and multiple dynamic views (Inbox, Today with Overdue, Upcoming 7-day agenda, Kanban/Board and List project views, Labels, and Saved Filter queries).
- **Target Audience / Persona**: Busy professionals, students, and power users who need instant offline task capture, robust project organization, and AI assistance to organize clutter into actionable schedules.
- **Key Value**: Instant offline responsiveness without spinner lag; seamless cross-device synchronization when connectivity is available; and AI tools that automate tedious scheduling, subtask generation, and natural-language organization.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Onboarding & Quick Setup**:
   - Welcome carousel with 3 informative cards highlighting nested projects, Gemini natural language parsing, and offline-first cloud sync.
   - Option to sign in with Google or start immediately with bundled seed data in local guest mode, with an unobtrusive sync banner to connect an account anytime.
2. **Natural Language Task Entry**:
   - Tapping the floating "+" button expands a bottom sheet with an interactive text field.
   - Typing `"Submit Q3 tax report tomorrow 4pm p1 #Finance @work every quarter"` dynamically parses the sentence into live chips (`Tomorrow 4:00 PM`, `P1 Priority`, `Project: Finance`, `Label: @work`, `Repeats: Quarterly`).
   - Tapping any chip allows manual adjustment before one-tap saving.
3. **Project & Subtask Organization**:
   - Navigation drawer displays an expandable project tree (e.g. *Work > Client A > Redesign*).
   - Long-press or drag handles permit moving projects or tasks between hierarchies.
   - Project detail screen supports switching between **List View** and **Kanban Board View** (grouped by section or priority).
   - Tasks display circular progress bars indicating subtask completion (e.g., `3/5 subtasks`).
4. **AI-Powered Workflows**:
   - **Auto-Breakdown**: Tapping "Break down with AI" on any complex task queries Gemini 3.5 Flash to generate 3–7 actionable subtasks with estimated durations, selectable with checkmarks to add all or individually.
   - **Plan My Day**: On the Today screen, tapping the magic wand header analyzes overdue and today's tasks, priority levels, and times to generate an optimal time-blocked agenda with suggestions to reschedule lower-priority items.
   - **AI Chat Assistant**: A dedicated conversational sheet where users can ask questions ("What high priority tasks are due before Friday?") and trigger batch actions ("Reschedule overdue chores to Saturday") with clear confirmation action cards.
5. **Offline & Sync State Feedback**:
   - App bar displays a subtle sync status icon (`Synced`, `Syncing...`, `Offline`, `Error`).
   - Manual "Sync Now" button in settings or drawer header.
   - Immediate undo snackbars for all task completions, moves, and deletions.

### Visual Identity & Theme
- **Theme Archetype**: Modern Todoist Minimalist — clean, energetic, distraction-free.
- **Color Palette**:
  - `Primary / Accent`: `#DC4C3E` (Signature Taskly Crimson Red)
  - `Primary Container`: Light `#FDECEB` / Dark `#3D1B18`
  - `Background`: Light `#FAFAFA` / Dark `#121212`
  - `Surface`: Light `#FFFFFF` / Dark `#1E1E1E`
  - `Surface Variant`: Light `#F3F3F3` / Dark `#2A2A2A`
  - `On Surface`: Light `#1F1F1F` / Dark `#E6E6E6`
  - `Priority Colors`: P1 Crimson `#DE4C4A`, P2 Tangerine `#F49C18`, P3 Royal Blue `#4073D6`, P4 Slate Grey `#8F9499`
- **Typography**: Clean humanist sans-serif scale with crisp headline hierarchies, legible body text for markdown notes, and monospace tags for natural-language syntax tokens.
- **Transitions & Gestures**:
  - Horizontal swipe right to complete (green checkmark reveal).
  - Horizontal swipe left to reschedule (amber calendar reveal).
  - Spring-animated bottom sheets and collapsible project branches.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Room-First Database Architecture with Asynchronous Cloud Sync
- **Chosen Approach**: All UI reads and writes hit local SQLite tables (via Room DAOs) synchronously in the domain layer. A background `SyncManager` monitors connectivity, reads a local `sync_queue` table, pushes dirty entities to Firestore, pulls remote delta updates by timestamp, and resolves conflicts using field-level Last-Write-Wins (LWW) and tombstone flags (`isDeleted = true`).
- **Why**: Ensures 0ms latency for all UI interactions, 100% offline functionality, and complete resilience against network drops.
- **Trade-off**: Requires local sync queue management and conflict resolution logic, but delivers the authentic native productivity app feel expected from apps like Todoist and Things.

### Decision 2: Multi-Pass Gemini Natural Language Task Parser
- **Chosen Approach**: Hybrid parsing pipeline — local regex tokenization for explicit tags (`#project`, `@label`, `p1-p4`, `today`, `tomorrow`) provides immediate 0ms latency feedback in the UI chip row, while an asynchronous Gemini 3.5 Flash prompt processes complex date expressions, recurring schedules, and semantic task titles when needed.
- **Why**: Zero typing lag while typing, with deep natural-language comprehension for complex recurrence phrases ("every second Tuesday of the month").

### Decision 3: Local Offline Reminders via Android AlarmManager & Notification Channels
- **Chosen Approach**: Local notifications scheduled using `AlarmManager.setExactAndAllowWhileIdle()` and broadcast receivers, with notification action buttons ("Complete" and "Snooze 15m") that invoke local repositories directly.
- **Why**: Reliable reminders even when the device is completely offline or in Doze mode, with boot receiver support (`RECEIVE_BOOT_COMPLETED`) to reschedule alarms after a reboot.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Jetpack Compose UI                            │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌───────────────┐ │
│  │  Inbox/Today │ │ Project View │ │ Quick Add NL │ │ AI Chat/Plan  │ │
│  │ (List/Agenda)│ │ (List/Board) │ │ BottomSheet  │ │   Assistant   │ │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └───────┬───────┘ │
└─────────┼────────────────┼────────────────┼─────────────────┼──────────┘
          ▼                ▼                ▼                 ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        ViewModels & Domain Layer                       │
│      TaskViewModel / ProjectViewModel / AiViewModel / SyncViewModel    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                            Repository Layer                            │
│      TaskRepository / ProjectRepository / LabelRepository             │
└─────────┬───────────────────────────────────────────────────┬──────────┘
          ▼                                                   ▼
┌───────────────────────────────┐           ┌────────────────────────────┐
│      Local Room Database      │           │      Cloud & AI APIs       │
│  (Single Source of Truth)     │           │                            │
│  - projects (hierarchical)    │           │  - Gemini 3.5 Flash REST   │
│  - tasks & subtasks           │           │    (Parsing, Plan, Chat)   │
│  - labels & task_labels       │           │  - Firebase Firestore      │
│  - comments & attachments     │◄─────────►│    (Remote Cross-Device)   │
│  - sync_queue & tombstones    │   Sync    │  - Firebase Auth           │
│  - reminders                  │  Manager  │    (Google Sign-In)        │
└───────────────────────────────┘           └────────────────────────────┘
```

### Data Models & Relationships

1. **`ProjectEntity`**:
   - `id: String` (UUID)
   - `parentId: String?` (null for root projects, enables unlimited tree nesting)
   - `name: String`
   - `colorHex: String`
   - `iconName: String`
   - `isFavorite: Boolean`
   - `isArchived: Boolean`
   - `isInbox: Boolean`
   - `sortOrder: Int`
   - `updatedAt: Long`, `createdAt: Long`, `isDeleted: Boolean`, `syncState: Int`

2. **`TaskEntity`**:
   - `id: String` (UUID)
   - `projectId: String` (Foreign key to `ProjectEntity.id`)
   - `parentTaskId: String?` (enables recursive subtasks)
   - `title: String`
   - `description: String` (Markdown format)
   - `priority: Int` (1 = P1 Urgent Red, 2 = P2 High Orange, 3 = P3 Normal Blue, 4 = P4 None Grey)
   - `dueDate: Long?`, `dueTimeMinutes: Int?`, `isAllDay: Boolean`
   - `recurrenceRule: String?` (e.g. `DAILY`, `WEEKLY:1,4`, `MONTHLY:1`)
   - `isCompleted: Boolean`, `completedAt: Long?`
   - `estimatedMinutes: Int?`
   - `orderIndex: Int`
   - `updatedAt: Long`, `createdAt: Long`, `isDeleted: Boolean`, `syncState: Int`

3. **`LabelEntity` & `TaskLabelCrossRef`**:
   - Many-to-many relationship supporting colored tags (`@work`, `@urgent`, `@errands`).

4. **`ReminderEntity`**:
   - `id: String`, `taskId: String`, `triggerTime: Long`, `isFired: Boolean`.

5. **`CommentEntity` & `AttachmentEntity`**:
   - Timestamped comments on tasks and local URI / metadata attachments.

6. **`SyncQueueEntity`**:
   - `entityType: String`, `entityId: String`, `operation: String` (INSERT, UPDATE, DELETE), `timestamp: Long`.

---

## 5. Execution Steps

1. **Project Configuration & Gradle Dependencies**:
   - Ensure Room KSP compiler, Retrofit, Kotlinx Serialization, and Firebase dependencies are active in `app/build.gradle.kts`.
   - Update `app_name` to "Taskly" in `res/values/strings.xml` and synchronize `metadata.json`.
   - Configure permissions for `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, and `INTERNET` in `AndroidManifest.xml`.
2. **Local Persistence Layer (Room Database)**:
   - Create Room Entities, TypeConverters, DAOs, and `TasklyDatabase`.
   - Implement Database Seeder for initial first-run projects, sample tasks, labels, and subtasks.
3. **Repository & Sync Layer**:
   - Build `TaskRepository`, `ProjectRepository`, `LabelRepository`, and `SyncManager`.
   - Implement offline-first CRUD operations with local sync queue logging.
   - Configure Firestore schema and Google Sign-In with Jetpack `CredentialManager`.
4. **Notification & Reminder Engine**:
   - Implement `ReminderBroadcastReceiver`, `BootReceiver`, and notification helper with "Complete" and "Snooze" action intents.
5. **AI Service Integration (Gemini 3.5 Flash)**:
   - Implement `GeminiApiService` with Retrofit for natural-language parsing, auto-breakdown, smart daily planning, and conversational task assistance.
6. **UI Components & Navigation**:
   - Define custom M3 Theme with Taskly red, dark/light color schemes, and shape tokens.
   - Build Bottom Navigation (Inbox, Today, Upcoming, Browse/Projects) and Sidebar Drawer with expandable hierarchical project tree.
   - Build Quick-Add Natural Language Bottom Sheet with real-time token preview chips.
   - Build Task List, Kanban Board toggle, Subtask checklist with circular progress, and Task Detail editor with markdown support.
   - Build AI Daily Plan modal and AI Assistant Chat screen.
   - Build Filter Builder, Search screen, and Settings with data export (JSON).
7. **Verification & Build**:
   - Compile with `compile_applet` and verify all flows and state machines.
