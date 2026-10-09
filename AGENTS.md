# AGENTS.md

This file documents the workspace configuration for AI agents working with this repository.

## ZAPP Configuration

**File:** `zapp.yaml`
**Reference:** https://ibm.github.io/zopeneditor-about/Docs/zapp.html

**Property groups defined:**
- cobol-local (COBOL)
- pli-local (PL/I)
- asm-local (HLASM)
- jcl-local (JCL)
- remote-libraries (COBOL - MVS)

**Syslib paths (copybook/include resolution):**
- COBOL: `src/base/cics/copy`, `src/base/ims/copy`
- PL/I: *No include directories found* (TODO: add when directory exists)
- HLASM: *No macro directories found* (TODO: add when directory exists)
- JCL: `src/base/batch/jcl`

**Library paths:**
- JCL proclib: *No proclib directories found* (TODO: add when directory exists)

**Proclib paths (JCL only, if present):**
- `src/base/batch/jcl` (also serves as syslib)

**MVS datasets (if using Zowe/remote):**
- BANKZ.DBB.BMS.COPY
- CICSTS63.CICS.SDFHCOB

**Zowe profiles (if present):**
- taz-test (type: test)
- zcodescan (type: zcodescan)
- zBuilder-userbuild (type: dbb)

**Custom variables used (if any):**
- `${dbbHlq}` - referenced in zBuilder-userbuild profile
- `${errPrefix}` - referenced in zBuilder-userbuild profile

**Analysis profile settings:**
- containsCics: true (CICS calls detected in COBOL programs)
- imsRuntime: IMS_BATCH (DL/I calls detected in COBOL and PL/I programs)
- analysisScope:
  - cobol-scope: `src/base/cics/cobol`, `src/base/ims/cobol`
  - pl1-scope: `src/base/ims/pli`, `src/base/batch/pli`
  - hlasm-scope: `src/base/ims/PSB`, `src/base/ims/DBD`
  - jcl-scope: `src/base/batch/jcl`