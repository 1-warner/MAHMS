# Azure DevOps setup

How to set up the Azure DevOps side of MAHMS: the project, the Kanban board, the work items
and the build pipeline. The rubric asks for the board, the work items linked to commits, and
an automated build, so this covers all three.

Everything that can be prepared in the repository is already here:

- `azure-pipelines.yml` in the repository root: the build definition.
- `docs/azure-work-items.csv`: 62 work items ready to import into the board.

What is left is clicking through the Azure DevOps website, which is what this file describes.
Take a screenshot at each step marked **[screenshot]** for Part F section 8 of the report.

---

## 1. Create the organisation and the project

1. Go to <https://dev.azure.com> and sign in with a Microsoft account.
2. Create an organisation if there is not one already, for example `doodoobops`.
3. **New project**:
   - Name: `MAHMS`
   - Visibility: Private
   - Advanced > Version control: **Git**
   - Advanced > Work item process: **Basic**

The process matters. The CSV uses the **Basic** process, whose work item types are Epic,
Issue and Task and whose states are **To Do**, **Doing** and **Done**. If the project is
created with Agile or Scrum instead, the import will be rejected, because those use different
types and states. Section 6 below says what to change if Agile is wanted.

Keep the project name exactly `MAHMS`, because the iteration paths in the CSV start with it.

**[screenshot]** the project overview page.

## 2. Bring the code in

The code lives on GitHub. There are two ways to connect it, and either is accepted.

**Option A, which keeps GitHub as the home (recommended).** The repository stays on GitHub and
Azure DevOps reads it. Boards still link to commits.

1. Project settings > GitHub connections > **Connect your GitHub account**.
2. Choose the `1-warner/MAHMS` repository and approve the access request.

**Option B, a copy inside Azure Repos.**

1. Repos > Files > **Import a repository**.
2. Clone URL: `https://github.com/1-warner/MAHMS.git`, then **Import**.
3. Remember that this copy does not update by itself. To keep both, add it as a second remote
   locally and push to both:

```bash
git remote add azure https://dev.azure.com/<organisation>/MAHMS/_git/MAHMS
git push azure main
```

**[screenshot]** the repository file list.

## 3. Create the iterations (the project weeks)

The CSV puts every work item into a week, so the weeks must exist first or the import will
fail.

1. Project settings > **Project configuration** > Iterations.
2. Under the `MAHMS` node, add eleven child iterations named exactly `Week 1` to `Week 11`.
3. Set the dates on each one. Week 1 starts Monday 31 August 2026 and each week runs Monday to
   Friday:

| Iteration | Start | End |
|---|---|---|
| Week 1 | Mon 31 Aug 2026 | Fri 04 Sep 2026 |
| Week 2 | Mon 07 Sep 2026 | Fri 11 Sep 2026 |
| Week 3 | Mon 14 Sep 2026 | Fri 18 Sep 2026 |
| Week 4 | Mon 21 Sep 2026 | Fri 25 Sep 2026 |
| Week 5 | Mon 28 Sep 2026 | Fri 02 Oct 2026 |
| Week 6 | Mon 05 Oct 2026 | Fri 09 Oct 2026 |
| Week 7 | Mon 12 Oct 2026 | Fri 16 Oct 2026 |
| Week 8 | Mon 19 Oct 2026 | Fri 23 Oct 2026 |
| Week 9 | Mon 26 Oct 2026 | Fri 30 Oct 2026 |
| Week 10 | Mon 02 Nov 2026 | Fri 06 Nov 2026 |
| Week 11 | Mon 09 Nov 2026 | Fri 13 Nov 2026 |

These are the same dates as the Gantt chart in Part B section 6.1 of the report.

4. Boards > Project settings > Team configuration > Iterations: tick all eleven so they show
   on the board.

## 4. Import the work items

1. Boards > **Work items**.
2. The **Import Work Items** button is behind the three-dot menu at the top right.
3. Upload `docs/azure-work-items.csv` and confirm.

62 items are created: 8 Epics and 54 Issues, already set to To Do, Doing or Done to match the
status column in Part B section 6.2 of the report.

If the import is refused, it is almost always one of these:

- The project was not created with the **Basic** process, so `Issue` and `To Do` are not valid.
- The `Week 1` to `Week 11` iterations do not exist yet, so the iteration paths do not resolve.
- The project is not called `MAHMS`, so the paths do not resolve. Open the CSV and replace
  `MAHMS\` with the real project name.

The CSV has no parent links, because Azure needs its own generated ids for those. After the
import, drag each Issue onto its Epic on the Boards > Backlog view if the hierarchy is wanted.
It is not required for the marks.

**[screenshot]** the imported backlog.

## 5. Set up the Kanban board

1. Boards > **Boards**.
2. The Basic process gives three columns: To Do, Doing, Done. The report describes five
   stages, so add the two missing ones with the gear icon > Columns > Add column:
   - To Do, **In Progress**, **In Review/Testing**, Done
     (keep Doing or rename it to In Progress, whichever is tidier)
3. Turn on the **Definition of done** on each column if you want it to look complete.
4. Drag a few items so the board is not all in one column when you screenshot it.

**[screenshot]** the Kanban board with items spread across the columns.

## 6. If the project uses the Agile process instead

The marks do not depend on the process, but if the project was already created as Agile,
convert the CSV before importing. In a text editor, replace:

| Basic | Agile |
|---|---|
| `Issue` | `User Story` |
| `To Do` | `New` |
| `Doing` | `Active` |
| `Done` | `Closed` |

`Epic` exists in both and does not change.

## 7. Create the build pipeline

1. Pipelines > **New pipeline**.
2. Where is your code: **GitHub** (option A above) or **Azure Repos Git** (option B).
3. Choose the MAHMS repository.
4. Configure your pipeline: **Existing Azure Pipelines YAML file**.
5. Branch `main`, path `/azure-pipelines.yml`, then **Continue** and **Run**.

The pipeline has two jobs that run side by side:

- **android**: JDK 17, `./gradlew testDebugUnitTest`, then `./gradlew assembleDebug`. It
  publishes the JUnit results so they appear on the Tests tab, and the debug APK and HTML test
  report as artifacts.
- **api**: Node.js 20 against a PostgreSQL 16 service container, running the API tests.

The first run takes a while because nothing is cached yet.

If the Android job cannot find `app/google-services.json`, that is handled: the pipeline copies
`.github/ci/google-services.placeholder.json` when no real file is committed, exactly as the
GitHub Actions workflow does.

**[screenshot]** a green pipeline run, and the Tests tab showing the unit tests passing.

## 8. Link commits to work items

This is the part the rubric specifically asks for. Put `#<id>` in the commit message, where
`<id>` is the work item number Azure gave on import:

```bash
git commit -m "Add payments tab and numbered receipt #42"
```

Notes:

- Azure DevOps picks this up automatically for commits in **Azure Repos**.
- For a **GitHub** repository, the link works once the GitHub connection in step 2 is in place.
  Mention the item as `AB#42` instead of `#42` in that case, because plain `#42` means a GitHub
  issue.
- You can also link by hand: open a work item > Links > Add link > Commit, and paste the commit
  URL. Doing two or three by hand is enough for the screenshot if the automatic link is
  fiddly.

**[screenshot]** a work item showing its linked commits, and the commit history.

## 9. What to put in the report

Part F section 8 of the combined report has a placeholder for the Azure DevOps link and
screenshots. Fill in:

- The project URL, `https://dev.azure.com/<organisation>/MAHMS`.
- The board screenshot from step 5.
- The green pipeline run from step 7.
- The work item with linked commits from step 8.

## 10. What cannot be scripted

For the record, these steps need a person signed in to the website and are not automated by
anything in this repository: creating the organisation and project, connecting or importing the
repository, creating the iterations, running the CSV import, creating the pipeline from the
YAML file, and taking the screenshots.
