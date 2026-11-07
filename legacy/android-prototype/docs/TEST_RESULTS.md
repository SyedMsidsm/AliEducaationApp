# Test-results log

Date: 30 October 2026 (Asia/Calcutta).
No historical assertions in supplied plans are counted as tests run in this chat.
Phone serial numbers and credentials are omitted from this shareable log.

| ID | Build/device/fixture | Expected | Actual | Status / evidence |
| --- | --- | --- | --- | --- |
| ENV-01 | Workspace / supplied ZIP | Current plan/UI/theme match ZIP | All three SHA256 comparisons matched | PASS: PowerShell ZIP stream hash comparison |
| ENV-02 | Windows / ADB | Detect authorized phone | SM-S928B listed as device; Android 16, SDK 36 | PASS: adb devices -l and getprop |
| ENV-03 | SM-S928B primary profile | Check existing Ali packages without mutation | No matching package returned for user 0 | PASS: pm list packages --user 0 com.alieducational |
| ENV-04 | SM-S928B all profiles | Inspect existing packages | Android denied protected user 150 access | BLOCKED: SecurityException; no bypass attempted |
| BUILD-01 | Prototype / sandbox | Assemble debug APK | Gradle native-platform.dll could not load | FAILED: environment restriction; retry outside sandbox |
| BUILD-02 | 0.0.1-prototype / host JBR 21 | Assemble debug APK | BUILD SUCCESSFUL; 38 tasks executed | PASS: Gradle :app:assembleDebug, 1m34s |
| BUILD-03 | Gradle 8.14.3 wrapper | Generate reproducible build entry point | BUILD SUCCESSFUL | PASS: wrapper task |
| INSTALL-01 | 0.0.1-prototype / SM-S928B user 0 | Install isolated TEST app | adb install returned Success; start intent accepted | PASS: separate com.alieducational.inventory.dev; no replacement flag |
| AUTH-01 | 0.1.0 / target phone | Fingerprint success/failure, device credential, timeout, no bypass | Fingerprint, restart lock and coarse foreground-idle lock verified; credential corroboration underway | PARTIAL: failure/cancel, background timing and absent enrollment still pending |
| STORE-01 | 0.1.0 / target phone | Full app offline persistence/restarts | Saved test draft survived force-stop/reopen, including an edit made in airplane mode with Wi-Fi off | PASS for settings/test-draft scope; interrupted-save and future stock/account transactions pending |
| CORE / COST / FINAL | Final-plan acceptance cases | Correct linked stock, money and costs | Prototype uses fixed illustrative text, not domain calculations | NOT RUN |
| UI-01 | 0.0.1-prototype / target phone | Visible prototype and English/Urdu navigation | First capture blank; UI dump could not get idle state; later foreground activity was Chrome | INCONCLUSIVE: evidence/phase0-home.png; awaiting user opening TEST app |
| UI-02 | 0.0.1 / SM-S928B / fixed sample previews | Navigate receiving, discount/credit, repayment, daily scopes, stock/accounts, language/themes | All 13 assertions passed on actual phone | PASS: evidence/v0.0.1/prototype-smoke-results.txt and corresponding XML/screenshots |
| UI-03 | 0.0.1 / foreground safety | Avoid tapping other apps | Initial smoke attempt detected another package and stopped before tapping; user-authorized TEST app then reopened | PASS: guard in scripts/prototype-smoke.ps1 |
| UI-04 | 0.0.1 screenshots | Match theme and preserve currency order in RTL | Found purple Material defaults and reversed rupee placement | FAILED: corrected in v0.0.2; DEF-001/002 |
| BUILD-04 | 0.0.2 / JBR21 | Assemble and lint | BUILD SUCCESSFUL; lint zero errors, seven warnings | PASS with warnings: app/build/reports/lint-results-debug.html |
| INSTALL-02 | 0.0.1 to 0.0.2 / TEST app | In-place debug update | adb install -r returned Success | PASS for installation only; no database preservation claim |
| UI-05 | 0.0.2 / SM-S928B | Retest changed themes/RTL | Four language/theme assertions passed; visual review confirms corrected palette and rupee order | PASS: evidence/v0.0.2; DEF-001/002 resolved |
| UI-06 | 0.0.2 / light theme | Readable status icons | White status icons on light background | FAILED: DEF-003; v0.0.3 correction pending device verification |
| UI-07 | 0.0.3 / SM-S928B | Verify corrected light status icons | Dark icons visible on light background; four language/theme checks passed again | PASS: evidence/v0.0.3; DEF-003 resolved |
| REVIEW-01 | User / Phase 0 | Layout review before foundation | User said layout makes sense and instructed continuing to foundation | ACCEPTED: chat response; independent brother pilot still not run |
| BUILD-05 | 0.1.0 foundation / JBR21 | Build foundation | Initial Kotlin setter/method name collision | FAILED then fixed: method renamed; next build passed |
| BUILD-06 | 0.1.0 / JBR21 | Build app/test APKs and lint | BUILD SUCCESSFUL; lint no errors, 16 warnings and one hint | PASS with warnings: assembleDebug, assembleDebugAndroidTest, lintDebug; dependency updates, API30 backup declaration and logo density remain review items |
| LOCK-UNIT | 0.1.0 / JVM | Exact timeout, late input, brief/long switch, activity extension, clock anomaly, manual lock | 6 tests, 0 failures/errors | PASS: evidence/phase1-lock-unit-tests.xml |
| DB-PHONE | 0.1.0 / SM-S928B / unique instrument-* DBs | Encrypted reopen with Urdu/settings, rollback, encrypted bytes/wrong-key rejection, draft replacement | OK (4 tests) | PASS: evidence/phase1-database-tests.txt; fixtures separate from shop.db |
| AUTH-START | 0.1.0 / SM-S928B | Launch locked before accessing local records | Locked screen shown; Unlock and device-credential actions present | PASS: evidence/phase1-locked.xml |
| AUTH-FINGERPRINT | 0.1.0 / SM-S928B | Authenticate using actual fingerprint and open encrypted data | User confirmed fingerprint success; UI shows Shop settings and Last unlock: Biometric | PASS: user report plus evidence/phase1-after-fingerprint.xml |
| STORE-SAVE | 0.1.0 / SM-S928B / TEST-PHASE1-20261030 | Save settings and disposable draft | UI reports Saved; fixture visible; existing nonempty draft would have stopped automation | PASS: P1-settings-saved.xml, P1-draft-saved.xml, P1-draft-fixture.xml |
| AUTH-RESTART | 0.1.0 / SM-S928B | Force-stop/relaunch must require authentication | Returned to locked screen | PASS: phase1-force-stop-restart-locked.xml; no app data cleared |
| AUTH-CREDENTIAL | 0.1.0 / SM-S928B | Device credential fallback opens protected data | User confirmed device credential opens Home; automated follow-up was blocked by approval-review usage limit | PASS (user-observed); automated corroboration pending |
| RESUME-01 | 0.1.0 / SM-S928B | Resume interrupted draft check | Approval service works again; app currently locked | Draft/controlled timeout not yet verified; renewed unlock requested |
| STORE-RESTART | 0.1.0 / SM-S928B / TEST-PHASE1-20261030 | Draft survives force-stop and encrypted DB reopen | Exact draft text and Saved visible after user unlock | PASS: P1-draft-reopened.xml, P1-draft-reopened-saved.xml; airplane-mode test still pending |
| AUTH-IDLE | 0.1.0 / SM-S928B / default 60 seconds | Lock after foreground inactivity | Unlocked draft observed; no further input sent; locked screen verified 64 seconds after completed draft inspection | PASS: P1-timeout-locked.xml; coarse interval check, not exact deadline measurement |
| AUTH-CREDENTIAL-CHECK | 0.1.0 / SM-S928B | Corroborate credential authentication type | Latest reopened draft screen reports Last unlock: Biometric | INCONCLUSIVE for PIN: earlier user confirmation retained separately; credential-only prompt retest requested |
| AUTH-CREDENTIAL-RETEST | 0.1.0 / SM-S928B | Credential-only prompt opens encrypted database | User entered device credential; UI reports Last unlock: Device credential; draft still Saved | PASS: phase1-device-credential-confirmed.xml and P1-draft-reopened.xml |
| AUTH-BRIEF-SWITCH | 0.1.0 / SM-S928B | Brief background switch retains session | Returned after five-second Home-screen interval without authentication; original Device credential result retained | PASS: phase1-brief-switch.xml |
| AUTH-BACKGROUND | 0.1.0 / SM-S928B / default 60 seconds | Longer background interval requires authentication | App backgrounded at 04:19:52 IST; returned and verified locked at 04:21:17 IST | PASS: phase1-background-timeout.xml; coarse threshold check |
| AUTH-CANCEL | 0.1.0 / SM-S928B | Cancelling credential prompt must not unlock | Back cancelled prompt; Locked and Authentication not completed visible | PASS: phase1-auth-cancel.xml |
| OFFLINE-READ | 0.1.0 / SM-S928B / disposable draft | Read saved draft offline | airplane_mode_on=1, wifi_on=0; draft visible and Saved | PASS: phase1-offline-draft.xml |
| OFFLINE-WRITE | 0.1.0 / SM-S928B / TEST-PHASE1-20261030 | Save an offline edit | Appended -OFFLINE only after exact fixture match; Saved shown and navigation retained edit | PASS: P1-offline-write.xml, P1-offline-write-saved.xml, P1-offline-navigation-retained.xml |
| OFFLINE-GUARD | 0.1.0 / SM-S928B | Avoid edits outside TEST app | First write attempt stopped because app was not foreground; reopened TEST app and rechecked fixture before editing | SAFE STOP then retry; no outside-app edit |
| OFFLINE-RESTART-OBSERVATION | 0.1.0 / SM-S928B | Observe locked state after restart | Locked assertion failed because Home already visible; user confirmed they had authenticated again with fingerprint | INCONCLUSIVE for this launch's locked-screen observation; not an observed bypass |
| OFFLINE-RESTART | 0.1.0 / SM-S928B / exact offline fixture | Offline edit survives force-stop/restart and protected reopen | Exact TEST-PHASE1-20261030-OFFLINE and Saved retained; airplane_mode_on=1, wifi_on=0 after check | PASS: phase1-offline-restart.xml; user corroborated fingerprint authentication |
| BACKUP-01 | Target phone / isolated data | Export/restore, corruption, interruption, replacement installation | Not implemented | NOT RUN |
| UPDATE-01 | Target phone / earlier build with fixtures | Signed in-place upgrade and migration preserve records | Not implemented | NOT RUN |
| PILOT-01 | Brother / isolated data | Independent workflow completion | Awaiting review | NOT RUN |

## Phase 2 increment — 0.2.0-stock

| ID | Build/device/fixture | Expected | Actual | Status / evidence |
| --- | --- | --- | --- | --- |
| P2-BUILD | 0.2.0 / JBR21 | Build app/test APKs, unit tests, lint | Final build successful in 34 seconds | PASS; lint warnings remain |
| P2-DB | 0.2.0 / SM-S928B / isolated instrument-* databases | Pack/damage arithmetic, retries, duplicate SKU rollback, shortage confirmation, unknown cost, v1→v2 migration plus prior encrypted database tests | OK (10 tests) | PASS: evidence/phase2-database-tests.txt |
| P2-INSTALL | 0.1.0→0.2.0 / TEST identity | In-place update without clearing data | adb install -r Success; user authenticated normally | PASS for debug identity only |
| P2-UPGRADE | 0.2.0 / actual TEST database | Preserve prior draft through migration | Exact TEST-PHASE1-20261030-OFFLINE and Saved retained | PASS: evidence/phase2-upgrade-retained-draft.xml |
| P2-UI-CREATE | 0.2.0 / TEST-STOCK-PEN / TEST-P2-PEN-001 | Save 30 opening pieces in packs of ten through phone UI | Product created and 3 packs + 0 pieces (30 pieces) displayed | PASS: P2-product-created.xml, P2-opening-packs.xml; recovered completed test output after interruption |
| P2-RESUME | Phone connection | Resume stock movement checks | Initially no device detected; later Samsung phone detected as device/authorised | Connection restored; no data changes while disconnected |
| P2-021-BUILD | 0.2.1 / JBR21 | Build app/test APKs, unit tests and lint | BUILD SUCCESSFUL; six lock-policy tests, zero failures/errors | PASS; lint warnings remain |
| P2-021-DB | 0.2.1 / SM-S928B / isolated fixtures | Prior stock/encryption tests plus counted-total idempotency, edit history/pack snapshots and archive safeguards | OK (13 tests) | PASS: evidence/phase2-021-database-tests.txt |
| P2-021-INSTALL | 0.2.0→0.2.1 / TEST identity | Update without uninstall/data clearing | adb install -r returned Success | PASS for installation; preserved-product UI check pending unlock |
| P2-021-RETAIN | 0.2.1 / TEST-P2-PEN-001 | Preserve existing stock after update | 30 pieces / 3 packs retained | PASS: P2-021-stock-retained.xml |
| P2-UI-NAV | 0.2.1 / UI automation | Locate existing test product | First capture remained Home; later exact-label lookup failed on trailing name whitespace; subsequent stock assertion passed | Automation failures only; no duplicate product created |
| P2-UI-PACK | 0.2.1 / TEST-P2-PEN-001 | Remove 2 packs of 10 from 30 pieces | 10 sellable pieces remain | PASS: P2-two-pack-removal.xml; shop-use movement, not a sale |
| P2-UI-DAMAGE | 0.2.1 / TEST-P2-PEN-001 | Move 3 of 10 to damaged | 7 sellable, 3 damaged | PASS: P2-damage-sellable.xml, P2-damage-separated.xml |
| P2-UI-COUNT | 0.2.1 / TEST-P2-PEN-001 | Set counted sellable total to 23 | 2 packs + 3 pieces; damaged remains 3 | PASS: P2-counted-total-ui.xml, P2-count-preserves-damaged.xml |
| P2-UI-EDIT | 0.2.1 / TEST-P2-PEN-001 | Change pack size 10→12 without changing stock | 1 pack + 11 pieces (23), damaged remains 3 | PASS: P2-pack-edit-ui.xml, P2-edit-preserves-damaged.xml |
| P2-UI-ARCHIVE | 0.2.1 / TEST-P2-PEN-001 | Hide archived product from active list and retain stock | Hidden from active filter; 23 pieces retained in archived filter | PASS: P2-archived-hidden.xml, P2-archive-retains-stock.xml |
| P2-UI-UNARCHIVE | 0.2.1 / TEST-P2-PEN-001 | Restore active listing without changing quantities | 23 sellable, 3 damaged retained | PASS: P2-unarchive-restores-active-view.xml, P2-unarchive-damaged-retained.xml |
| SOURCE-XLS | Bundled Python + xlrd / original supplied XLS | Read and reconcile without mutation | 171 transactions, 29 dates, income 447241, expense 501315, closing -54074; no balance discrepancy; summary controls match; SHA256 unchanged | PASS: evidence/ledger-source-audit.json; source audit only, not Android import |
| SOURCE-STOCK-SCOPE | Supplied Transactions sheet | Determine whether product opening stock can be imported | Headers are Date, Notes, Description / Category, Income, Expense, Balance; no product quantities/pack sizes/costs | Not a stock source; no records imported |
| P2-022-BUILD | 0.2.2 / JBR21 | Build app/test APKs, run unit tests/lint | BUILD SUCCESSFUL; six unit tests, zero failures/errors; lint warnings remain | PASS with warnings |
| P2-022-DB | 0.2.2 / SM-S928B / isolated fixtures | Earlier stock/encryption tests plus independent pack price and v2→v3 preservation | OK (15 tests) | PASS: evidence/phase2-022-database-tests.txt |
| P2-022-INSTALL | 0.2.1→0.2.2 / TEST identity | In-place update | adb install -r Success | PASS for debug update installation; no uninstall or clearing |
| P2-022-UI-LOOKUP | 0.2.2 / product edit | Locate pack price | Initial assertion ran before scrolling and missed off-screen field; subsequent scroll located it | Automation lookup failure, resolved; no lost record |
| P2-022-UI-PRICE | 0.2.2 / TEST-P2-PEN-001 | Save independent pack price without changing piece price/stock | Piece ₹15, pack ₹170; 23 sellable and 3 damaged retained | PASS: P2-022-prices.xml, P2-022-stock-retained.xml, P2-022-damaged-retained.xml |

Calculation groundwork: six CostingTest JVM cases passed (zero failures/errors), covering proportional delivery, the agreed moving-average/sale example, reconciled rounding, zero-value rejection, shortage cost rejection and negative delivery rejection. Evidence: costing-unit-tests.xml. This is not a purchase/sale posting or phone workflow test. Calculation changes are not yet installed in an APK.

No release APK is ready. Debug stock increment v0.2.2 installed only. No app data has been cleared or existing app uninstalled.
Latest debug APK (0.2.2-stock) SHA256: D25177262F7B8E915089A61866C4C057D115EC4C35D75DBED7095FF7AF0309C5.
For every subsequent run append build/version, device, fixture, expected/actual, status,
evidence file and defect reference. Never convert simulated previews into passed functional tests.
