# 📝 TODO: Feature Extensions for WhereMyThings App

This document outlines the next suggested development steps for enhancing the functionality and user experience of the WhereMyThings app.

---

## 🔔 1. Notification Center (Activity)
**Objective**: Show a list of notifications stored in Firebase under `/notifications/{uid}`.

- [ ] Create `NotificationActivity`
- [ ] Fetch all notifications by current user ID
- [ ] Display with timestamp, matched report class/type
- [ ] Add click event → open matched `ReportDetailActivity`

---

## 🔴 2. Notification Badge or Red Dot
**Objective**: Show visual cue when new unseen notifications exist.

- [ ] Check if unseen notifications exist at app launch or in toolbar
- [ ] Display badge (red dot or number) on `Profile`, `AppBar`, or `Notification` icon
- [ ] Clear badge when opened or seen

---

## 📲 3. Firebase Cloud Messaging (Push Notifications)
**Objective**: Enable background push notification delivery when a match is found.

- [ ] Setup Firebase Cloud Messaging (FCM)
- [ ] Generate and store user `FCM tokens`
- [ ] Send push notification when a match is added
- [ ] Handle notification click to open `ReportDetailActivity`

---

## 🖼️ 4. Similar Report Preview UI
**Objective**: Show image preview of the matched report before user opens it.

- [ ] In the AlertDialog, show matched image thumbnail
- [ ] Add "View" or "Ignore" options
- [ ] Support future batch match suggestions

---

## 🛠️ 5. Optional Improvements
- [ ] Allow user to manually match reports from list
- [ ] Add "Mark as resolved" for a report
- [ ] Filter notifications by type (found/lost, matched/unmatched)
- [ ] Archive or delete old notifications

---

## 📌 Notes
This app already supports:
- TFLite model inference
- Similarity calculation via embeddings
- User matching and notification via Firebase Realtime Database
- UI for report creation, comment & reply system

---

_Last updated: 2025-05-14_
