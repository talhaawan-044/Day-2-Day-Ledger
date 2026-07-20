git add .
git commit -m "Refactor settings UI, optimize scroll performance, and patch data restoration bugs

- UI & Layouts: Merged 'Data Management' into 'Cloud Account' (now 'Backups & Sync') to consolidate tools and eliminate empty screens. Fixed text wrapping and squishing issues in SettingsRow subtitles.
- Performance: Disabled NavHost transition animations in MainActivity to prevent choppy page navigation.
- Scroll Optimization: Wrapped O(N) running balance calculations inside a 'remember' block in PartiesScreen.kt, completely fixing the soggy/laggy scrolling issue for large lists.
- Bug Fixes: Added .orEmpty() null-safety checks in DataExchangeUtils and LedgerRepository to prevent crashes when restoring older database structures.
- Sync Safety: Audited sync and delete functions to ensure all live database deletions strictly follow safe soft-delete (isDeleted = true) synchronization logic."

git push
