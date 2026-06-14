---
name: chinese-docx-report-format
description: Format and generate Chinese course project or database assignment DOCX reports. Use when creating or revising .docx reports that must follow Chinese academic report formatting, including title hierarchy, first-line indentation, figure/table captions, table borders, SQL/code table styling, ER diagrams, and report content organized from a project-specific framework.
---

# Chinese DOCX Report Format

## Overview

Use this skill to produce Chinese course design, database assignment, or role-specific project reports as `.docx` files with consistent formatting. The chapter structure should come from the user's current framework or assignment requirements; this skill mainly controls typography, layout, captions, tables, code blocks, and diagram handling.

## Workflow

1. Read the user's report framework, existing report, and any generation script before editing or generating the DOCX.
2. Separate content structure from formatting. Preserve or adapt the framework headings for the current task instead of forcing a fixed chapter list.
3. Apply the report formatting rules in `references/report-format.md`.
4. For generated DOCX, inspect the output by checking the DOCX XML or opening/exporting screenshots when visual layout is risky.
5. If the target DOCX is locked by Word or preview software, report the lock clearly and avoid generating numbered variants unless the user asks for a temporary file.

## Content Rules

- Write the report as a standalone project artifact. Do not mention internal generation actions such as "extracted from the main report", "based on the previous document", or "this section was copied".
- For role-specific reports, keep shared conceptual or logical design only when it is necessary background, and make feature design, ER diagrams, physical design, and evaluation focus on that role.
- Do not wrap technical names in Markdown backticks in final report text unless the report explicitly requires Markdown syntax. In Word body text, write names plainly.
- Use concrete technical evidence when useful: database creation, table creation, trigger, view, query, backend route, permission check, and cloud database deployment details.
- ER diagrams should be split when dense. Prefer overview plus focused sub-diagrams. Avoid line crossings, entity overlap, unclear cardinality labels, and labels that repeat entity names.

## Resources

Read `references/report-format.md` whenever generating or modifying the DOCX formatting, table style, image sizing, SQL/code block layout, or report generator script.
