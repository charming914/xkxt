# 中文大作业 DOCX 报告格式规范

Use these rules when generating Word `.docx` reports for Chinese course projects and database assignments.

## Page And Paragraph

- Use A4 page size with normal margins unless the assignment gives a stricter template.
- Body Chinese font: 宋体. Western font: Times New Roman.
- Body font size: 小四 when following the framework body style; use 小五 inside tables and SQL/code tables when the report has dense database content.
- Body paragraph alignment: left.
- Body first-line indent: 2 Chinese characters. Prefer Word character indentation such as `w:firstLineChars="200"` over approximate centimeter values like `0.74cm`.
- Headings, table captions, image paragraphs, code cells, and table cells should not inherit body first-line indentation.
- Line spacing: approximately 1.18x or a visually close Word line spacing. Keep paragraph spacing stable and avoid crowded captions.

## Heading Hierarchy

- Heading 1: main report title. 黑体, 三号, bold, centered, no first-line indent.
- Heading 2: chapter titles such as `一、项目背景`. 黑体, 四号, bold, left aligned, no first-line indent.
- Heading 3: section titles such as `1.1 ...`. 黑体, 小四, bold, left aligned, no first-line indent.
- Heading 4: subsection titles such as `1.1.1 ...`. 黑体, bold, left aligned, no first-line indent.
- Keep all headings flush to the left except the document title. Do not add body paragraph indentation to headings.
- Use fourth-level headings for detailed functional design when the user asks for expanded module descriptions.

## Figures And ER Diagrams

- Center every figure image. The image paragraph must be zero-indent and centered, not body-indented.
- Put figure number and figure name below the image, centered, using body-style font.
- Do not include the figure number or title inside generated diagram images when a Word caption already exists below the image.
- Use image widths around 14 cm or 15 cm. Avoid full-page oversized images unless required.
- If a wide overview diagram is slightly clipped, reduce width or add diagram-side padding before embedding.
- Split complex ER diagrams into overview and focused sub-diagrams. For example, show a top-level conceptual ER diagram first, then role-specific or workflow-specific diagrams.
- ER relationship lines should connect cleanly to entity boxes, avoid crossing through entities, and avoid text overlap.
- Relationship labels should usually be cardinality only, such as `1:N`, `N:1`, or `1:1`; do not repeat entity names on the line unless necessary.

## Normal Tables

- Center tables on the page.
- Use a fixed width of about 14 cm. In OOXML this can be represented as about `7938` dxa.
- Put table number and table name above the table, centered.
- Keep table caption closer to its own table than to the previous table. Add enough spacing after each table.
- Use full thin borders for normal data tables.
- Do not use fill color unless the assignment explicitly requires it.
- Table text should generally be centered vertically and horizontally.
- Table text should be 小五 when content is dense. Header cells may use 黑体 and bold.
- Set stable column widths. Avoid auto-fit behavior that causes swallowed first characters, uneven alignment, or layout shifts.
- If several tables appear consecutively, ensure every caption has spacing before it and is visually attached to the following table.

## SQL And Code Tables

- Put SQL and code snippets in tables matching the report's code-block style.
- SQL/code tables should be about 14 cm wide and centered.
- Use only the outer border for SQL/code tables. Do not use full internal row borders.
- Code text should be left aligned, not centered.
- Code font should be 小五, using 宋体 or a consistent readable font already used by the report generator.
- Do not add Markdown fences or backticks around SQL inside the Word report.
- Prefer actual SQL from project files over illustrative pseudo-SQL. If the report says it lists triggers, list all relevant real trigger statements for the described scope.
- When extracting triggers from SQL files, cut each trigger from `CREATE TRIGGER ...` through its own `END$$`, not through a global `DELIMITER ;`, because unrelated triggers may exist between them.
- Include `DELIMITER $$` and `DELIMITER ;` around trigger blocks when needed to show executable MySQL trigger creation syntax.

## Generator Implementation Notes

- If generating DOCX directly with OOXML, define reusable helpers for paragraphs, headings, images, normal tables, and code tables.
- Use explicit paragraph indentation control for non-body elements:
  - body: first-line character indent
  - heading/caption/image/table/code: zero first-line indent
- Use explicit table width and grid columns rather than relying on Word auto layout.
- For code table rows, set top border only on the first row and bottom border only on the last row; left and right borders remain visible.
- Validate by inspecting `word/document.xml` for expected snippets and style properties when a GUI check is not possible.
- If Word has the output `.docx` open and the file is locked, do not overwrite it. Ask the user to close the document, then regenerate the intended target path.
