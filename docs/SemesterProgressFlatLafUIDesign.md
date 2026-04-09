# Semester Progress UI Design (FlatLaf + NetBeans)

This document describes the registrar and student semester-progress UI design for NetBeans Swing using FlatLaf.

## 1. Design Goals

- Provide clear semester progression visibility: NOT_STARTED, IN_PROGRESS, COMPLETED.
- Keep the interaction fast for registrar review and student self-check.
- Use FlatLaf visual tokens for consistent spacing, typography, and states.
- Keep all views compatible with NetBeans GUI Builder forms.

## 2. Target Screens

### 2.1 Registrar Panel

Panel name: RegistrarSemesterProgressPanel

- Student selector (search + table)
- Curriculum header card
- Semester timeline card list
- Subject completion details table
- Recompute button and last sync timestamp

### 2.2 Student Panel

Panel name: StudentSemesterProgressPanel

- Current curriculum summary
- Current semester status card
- Vertical semester timeline
- Per-semester completion ratio (ex: 5/7 subjects)

## 3. FlatLaf Styling System

### 3.1 Base Theme

Use FlatLightLaf for registrar and student modules.

```java
FlatLightLaf.setup();
UIManager.put("Component.arc", 14);
UIManager.put("Button.arc", 16);
UIManager.put("TextComponent.arc", 14);
UIManager.put("ScrollBar.width", 12);
```

### 3.2 Semantic Colors

- Primary: #2563EB
- Success: #16A34A
- Warning: #D97706
- Muted text: #64748B
- Surface: #FFFFFF
- Surface alt: #F8FAFC
- Border: #E2E8F0

### 3.3 Status Chips

- NOT_STARTED
  - Background: #EEF2FF
  - Foreground: #3730A3
- IN_PROGRESS
  - Background: #FFEDD5
  - Foreground: #9A3412
- COMPLETED
  - Background: #DCFCE7
  - Foreground: #166534

## 4. NetBeans Component Layout

### 4.1 Main Container

Use BorderLayout in root panel.

- NORTH: Header block
- CENTER: Split pane
- SOUTH: Footer status strip

### 4.2 Header Block

Use GridBagLayout.

Components:

- JLabel lblTitle: Semester Progress
- JLabel lblSubtitle: Progress is computed from enrollment and completion records.
- JButton btnRefreshProgress: Recompute Progress
- JLabel lblLastSyncAt: Last synced timestamp

### 4.3 Body Split

Use JSplitPane (horizontal).

Left pane:

- JTable tblStudents
- JTextField txtStudentSearch
- JComboBox cmbCourseFilter

Right pane:

- JPanel pnlCurriculumCard
- JPanel pnlTimelineContainer (scrollable)
- JTable tblSemesterSubjects

### 4.4 Semester Timeline Card

Per semester card (JPanel + FlatLaf border style):

- JLabel lblSemesterName (ex: Year 1 - First Semester)
- JLabel lblStatusChip
- JProgressBar pbCompletion
- JLabel lblCompletionText (ex: 6/8 completed)
- JLabel lblStartedAt
- JLabel lblCompletedAt

## 5. Table Design Rules

### 5.1 Student Table

Columns:

- Student ID
- Name
- Curriculum
- Current Semester Status

Rules:

- Non-editable cells
- Row height: 34
- Alternate row striping enabled
- Highlight selected row with primary tone

### 5.2 Subject Completion Table

Columns:

- Subject Code
- Subject Name
- Units
- Required In Semester
- Student Status

Rules:

- Status column uses color chips
- Units right-aligned
- Subject name supports tooltip for long text

## 6. Component Naming Convention

Use this naming style in NetBeans forms:

- pnl... for panel
- lbl... for label
- btn... for button
- tbl... for table
- txt... for text field
- cmb... for combo box
- pb... for progress bar

Example IDs:

- pnlSemesterTimeline
- lblSemesterStatus
- btnRefreshProgress
- tblSemesterSubjects

## 7. Interaction States

### 7.1 Load State

- Disable right pane until a student is selected.
- Show placeholder card: Select a student to view progress.

### 7.2 Sync State

- Disable btnRefreshProgress while recomputing.
- Show indeterminate progress indicator in footer.

### 7.3 Error State

- Show inline alert panel below header.
- Keep last successful data visible.

## 8. Accessibility and Readability

- Minimum text size: 13px equivalent
- Header text: 18px to 22px
- Maintain contrast ratio >= 4.5 for all status chips
- Support keyboard focus order: search -> table -> timeline -> subject table -> refresh

## 9. Suggested NetBeans Form Breakdown

- RegistrarSemesterProgressPanel.form/.java
- StudentSemesterProgressPanel.form/.java
- SemesterProgressCard.form/.java
- SemesterSubjectStatusRenderer.java

## 10. FlatLaf Properties Snippet

Add to your FlatLaf properties resource for consistent controls.

```properties
Component.arc = 14
Button.arc = 16
TextComponent.arc = 14
Table.showHorizontalLines = false
Table.showVerticalLines = false
Table.rowHeight = 34
ScrollBar.width = 12
```

## 11. Service-to-UI Data Contract

The UI should read from StudentSemesterProgressService.

Expected fields per semester row:

- semesterId
- status
- startedAt
- completedAt
- requiredSubjectCount
- completedSubjectCount
- completionRatio

For subject rows, combine semester_subjects and student_enrolled_subjects status for rendering.
