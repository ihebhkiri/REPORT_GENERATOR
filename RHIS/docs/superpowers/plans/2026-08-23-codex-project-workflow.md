# Codex Project Workflow Implementation Plan

> **For agentic workers:** Execute this plan inline and in order. Do not delegate unless the user explicitly requests subagents. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add repository-specific Codex instructions and lightweight research, planning, progress, and repository-coordination guidance without changing application behavior.

**Architecture:** The backend repository owns cross-repository workflow documents and a backend-specific root `AGENTS.md`. The separate Angular repository owns its own root `AGENTS.md`; both instruction files share task classification and Git-safety principles while retaining stack-specific rules.

**Tech Stack:** Markdown, Git, Java 17/Spring Boot/Maven, Angular 20/TypeScript/RxJS/PrimeNG

## Global Constraints

- Do not modify application source, tests, runtime configuration, or dependencies.
- Preserve all pre-existing dirty files.
- Do not commit, push, merge, reset, or open a pull request.
- Keep the workflow proportional to task complexity.
- Do not introduce unattended agent loops or require subagents.

---

### Task 1: Backend root instructions

**Files:**
- Create: `AGENTS.md`

**Interfaces:**
- Consumes: existing backend modules, `pom.xml`, `docs/flows`, and `docs/superpowers`
- Produces: repository-wide operating rules inherited by Codex tasks in the backend tree

- [x] **Step 1: Add project map and source-of-truth documentation paths**
- [x] **Step 2: Add proportional research-plan-implement rules**
- [x] **Step 3: Add Java, Spring, PostgreSQL, security, and export constraints**
- [x] **Step 4: Add targeted and full Maven validation commands**
- [x] **Step 5: Add Git safety, human review, context handoff, and frontend coordination rules**
- [x] **Step 6: Verify that all commands and referenced paths exist**

### Task 2: Reusable workflow documents

**Files:**
- Create: `docs/superpowers/research/README.md`
- Create: `docs/superpowers/plans/README.md`
- Create: `docs/superpowers/progress/README.md`

**Interfaces:**
- Consumes: `docs/superpowers/specs`, `docs/superpowers/plans`, and root `AGENTS.md`
- Produces: copyable templates and repository-coordination guidance referenced by `AGENTS.md`

- [x] **Step 1: Add the evidence-based research template**
- [x] **Step 2: Add the implementation-plan template**
- [x] **Step 3: Add the context-handoff progress template**
- [x] **Step 4: Add multi-repository coordination and Git safety boundaries**
- [x] **Step 5: Scan the documents for placeholders that are not explicitly template fields**

### Task 3: Frontend root instructions

**Files:**
- Create: `AGENTS.md` at the root of the separate Angular repository

**Interfaces:**
- Consumes: frontend `package.json`, Angular workspace configuration, and existing frontend feature layout
- Produces: repository-wide operating rules inherited by Codex tasks in the frontend tree

- [x] **Step 1: Add frontend project map and backend coordination rules**
- [x] **Step 2: Add Angular, TypeScript, Signals, RxJS, forms, accessibility, and styling constraints**
- [x] **Step 3: Add targeted tests, full tests, and build commands verified from `package.json`**
- [x] **Step 4: Add the shared proportional workflow, Git safety, and context handoff rules**
- [x] **Step 5: Verify the primary frontend checkout contains only the new instruction file in addition to its prior state**

### Task 4: Final verification

**Files:**
- Verify: all files created by Tasks 1-3

**Interfaces:**
- Consumes: Git status snapshots captured before implementation
- Produces: evidence that the setup is documentation-only and preserves user work

- [x] **Step 1: Search for broken local documentation references and unintended placeholders**
- [x] **Step 2: Review the complete documentation diff**
- [x] **Step 3: Compare backend and frontend Git status with their initial snapshots**
- [x] **Step 4: Report created files, known test baseline, and repository status**
