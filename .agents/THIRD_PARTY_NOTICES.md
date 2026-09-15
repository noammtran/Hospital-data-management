# Third-Party Skill Notices

This repository vendors third-party Agent Skills under `.agents/skills`.
Pinned repositories, paths, commits, tree identifiers, and local Codex overlays
are recorded in `.agents/skills.sources.json`.

## Angular Developer

`angular-developer` is distributed by the Angular team from `angular/skills`
and authored in `angular/angular`. It is licensed under MIT and identifies
Copyright 2026 Google LLC in its upstream frontmatter. The complete license is
preserved at `.agents/licenses/angular-MIT.txt`.

Project changes add `agents/openai.yaml` and a Codex CLI registration example
to `references/mcp.md`.

## Database Designer

`database-designer` comes from `alirezarezvani/claude-skills` and is licensed
under MIT, Copyright 2025 Alireza Rezvani. The complete license is preserved at
`.agents/licenses/alirezarezvani-MIT.txt`.

Project changes add `agents/openai.yaml`, neutralize one Claude-branded
collection sentence in the upstream README, and add a repository-specific
Oracle/Flyway guard before the generic database guidance. The source repository
name remains recorded in `.agents/skills.sources.json`.

## Java and Spring Skills

`code-quality`, `jpa-patterns`, and `spring-boot` come from
`piomin/claude-ai-spring-boot` and are distributed under Apache License 2.0.
The complete license is preserved at
`.agents/licenses/piomin-Apache-2.0.txt`.

Those upstream directories target Claude. Their upstream files are kept intact
except for the documented adapter corrections in `.agents/skills.sources.json`,
with separate `agents/openai.yaml` files added for Codex. Repository policy
remains in `AGENTS.md` and `hospital/AGENTS.md`. The Spring adapter makes
security and operations capabilities opt-in, fixes invalid annotation
placement in the data example, and uses Oracle/Flyway migration syntax.

The `jpa-patterns` lineage includes work from `decebals/claude-code-java`.
Its MIT notice, Copyright 2026 Decebal Suiu, is additionally preserved at
`.agents/licenses/decebals-MIT.txt`. The project adapter uses that verified
lineage to complete a Java example truncated by the direct upstream.

## Academic Writing Skills

`bib-search-citation`, `cover-letter`, `latex-paper-en`, `latex-thesis-zh`,
`paper-audit`, and `typst-paper` come from
`bahayonghang/academic-writing-skills`. That upstream repository provides no
standalone license file and states: "Academic Use Only - Not for commercial
use." Do not use or redistribute these skills commercially without
permission from the upstream author.

The `bib-search-citation`, `cover-letter`, `latex-thesis-zh`, and `typst-paper`
directories are byte-for-byte upstream copies with no local content changes.
They retain upstream frontmatter and command conventions, including metadata
that the current Codex validator does not accept.

The `latex-paper-en` and `paper-audit` copies were modified on 2026-07-27 to
normalize their frontmatter and OpenAI metadata for Codex, replace the
`$ARGUMENTS` convention, and make `$SKILL_DIR` resolution explicit. The LaTeX
skill's MCP setup example was changed to the Codex CLI, and the paper-audit
ScholarEval stage label was made product-correct.

## Academic Research Suite

`academic-research-suite` comes from
`Imbad0202/academic-research-skills-codex`, the Codex-native sibling
distribution of `Imbad0202/academic-research-skills`. It is licensed under
Creative Commons Attribution-NonCommercial 4.0 International, Copyright 2026
Cheng-I Wu. The complete license is preserved at
`.agents/licenses/Imbad0202-CC-BY-NC-4.0.txt`.

The installed directory is a byte-for-byte upstream copy with no local content
changes. It exposes one Codex router skill and vendors the `deep-research`,
`academic-paper`, `academic-paper-reviewer`, `academic-pipeline`, and
`experiment-agent` workflows beneath it. Do not use or redistribute this suite
commercially without separate permission from the upstream author.

## Taste Skill

`design-taste-frontend` comes from `leonxlnx/taste-skill`, upstream path
`skills/taste-skill`, and is licensed under the MIT License, Copyright 2026
Leonxlnx. The complete license is preserved at
`.agents/licenses/leonxlnx-MIT.txt`.

The installed skill content is byte-for-byte upstream with no local content
changes. Its project directory uses the frontmatter install name
`design-taste-frontend`; the upstream source folder is named `taste-skill`.
