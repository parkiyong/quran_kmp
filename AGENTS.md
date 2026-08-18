## Agent skills

### Issue tracker

Issues and specs live in this repo's GitHub Issues; skills use the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical roles (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`) map 1:1 to tracker labels. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` at the repo root plus `docs/adr/` for decisions. See `docs/agents/domain.md`.

### Reference Project & Porting Guidelines

- Legacy Android repo source location: `C:\Users\wraja\GitHub\quran_android`
- **Follow legacy implementation as much as possible (code copy)**: Do not reinvent the wheel. Copy/adapt existing domain logic, algorithms, math, data structures, SQL queries, and utility classes directly from `quran_android`, replacing only Android-specific framework types with KMP/Okio/Compose equivalents.


