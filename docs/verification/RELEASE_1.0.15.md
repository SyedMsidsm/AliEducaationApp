# Version 1.0.15

Repairs cover inventory, pack checkout, linked accounts, backups, TLS synchronization, receipts and layout. Schema remains version 8. The signed 1.0.15 APK was installed as an in-place update on the owner phone; no uninstall or data clearing was performed.

Repair verification: 58 JVM tests and 42 emulator tests passed; lint had 0 errors and 84 warnings. Subsequently added explicitly provisioned, one-time Owner recovery requiring device authentication; release build and unit tests passed. Recovery itself requires the user to authenticate on the phone and was not completed by automation.

Signing credentials, recovery proof, shop databases, backups and APKs are deliberately excluded. The embedded recovery proof digest is not the secret proof; this repository alone does not provision recovery on a device.

Read REPAIRS_2026-11-08.md for limitations and outstanding release/device tests. No claim is made that every requested feature is complete.
