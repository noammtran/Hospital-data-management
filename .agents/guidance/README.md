# Codex project guidance index

This directory is the complete Codex-oriented successor to the former
`.claude/CLAUDE.md`. The repository and nested `AGENTS.md` files contain the
small, always-loaded rules and route each task to the detailed guidance that
applies to it. This split avoids Codex's default combined `AGENTS.md` size limit
without discarding the original project notes.

## Loading rules

The root `AGENTS.md` is always authoritative for repository-wide policy. Before
making an implementation decision, editing files, or reporting completion,
read every detailed guide whose scope matches the task:

| Task scope | Required detailed guidance |
|---|---|
| Product behavior, proposal compliance, patient workflows, or cross-stack CRUD | `01-project-and-requirements.md` |
| Java, Spring Boot, JPA, Oracle, Flyway, schema, API, validation, history, or statistics | `02-backend-database-api.md` and `hospital/AGENTS.md` |
| Angular, UI, forms, routing, Material/Matero, frontend HTTP, or frontend tests | `03-frontend.md` and `hospital-frontend/AGENTS.md` |
| Local setup, commands, troubleshooting, seed/reference data, privacy, or external datasets | `04-development-and-data.md` | 
| LaTeX report, citations, chapter content, figures, Mermaid, ERD, UML, or report verification | `05-report-and-diagrams.md` and `docs/report/AGENTS.md` |
| Auditing the Claude-to-Codex migration itself | `06-claude-migration-map.md` |

Read only the matching guides, but do not skip a matching guide merely to save
context. A task spanning multiple areas must load all applicable guides. For
example, a patient validation change normally needs guides 01, 02, 03, and 05
because it changes behavior across both applications and the report.

## Source precedence

When any detailed guide disagrees with a more authoritative source, use this
order:

1. The binding internship proposal at
   `docs/ref/1.3. Annex 3 - Internship proposal 2026.pdf`.
2. Running code, Flyway migrations, tests, and the actual LaTeX source.
3. The nearest applicable `AGENTS.md`.
4. The detailed guides in this directory.
5. Historical plans and the backed-up Claude configuration.

Do not silently resolve a proposal-versus-code conflict. Identify the conflict,
preserve required proposal behavior, and verify the intended change before
editing.

## Original and migration record

- Exact original: `.legacy-ai/claude-source/CLAUDE.md`
- Exact backup archive:
  `.legacy-ai/backups/claude-original-before-codex-20260727.zip`
- Section-by-section disposition: `06-claude-migration-map.md`
- Last full reconciliation: 2026-07-28

The original file is retained for provenance, not used as current operational
guidance. It contains stale framework versions, paths, endpoint snapshots,
machine-specific database details, and report instructions that conflict with
the current repository.
