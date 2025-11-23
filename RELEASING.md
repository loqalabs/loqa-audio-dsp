# Release Process

This document describes the process for releasing new versions of `@loqalabs/loqa-audio-dsp`.

## Overview

Releases are automated via GitHub Actions. When a version tag is pushed, the CI pipeline automatically builds, tests, and publishes the package to npm.

## Prerequisites

Before releasing, ensure you have:

1. **NPM_TOKEN Secret**: Configured in GitHub repository settings

   - Go to: Settings → Secrets and variables → Actions
   - Add secret named `NPM_TOKEN` with your npm access token
   - Token must have publish permissions for `@loqalabs` scope

2. **Write Access**: Push access to the repository and permission to create tags

3. **npm Account**: Member of `@loqalabs` organization on npmjs.com

## Release Steps

### 1. Pre-Release Checklist

Before creating a release, verify:

- [ ] All tests pass locally: `npm test`
- [ ] TypeScript compiles without errors: `npm run typecheck`
- [ ] Linting passes: `npm run lint`
- [ ] Security audit passes: `npm audit`
- [ ] CI pipeline is passing on `main` branch
- [ ] Documentation is up to date (README.md, API.md)
- [ ] Example app works on both iOS and Android

### 2. Update CHANGELOG.md

Edit `CHANGELOG.md` to document changes in the new version:

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added

- New features

### Changed

- Changes to existing functionality

### Fixed

- Bug fixes

### Removed

- Removed features
```

Follow [Conventional Commits](https://www.conventionalcommits.org/) format for consistency.

### 3. Version Bump

Use npm version command to update version and create git tag:

```bash
# For patch releases (bug fixes): 0.1.0 → 0.1.1
npm version patch

# For minor releases (new features): 0.1.0 → 0.2.0
npm version minor

# For major releases (breaking changes): 0.1.0 → 1.0.0
npm version major
```

This command will:

- Update `package.json` version
- Create a git commit with the version change
- Create a git tag (e.g., `v0.1.1`)

### 4. Push Tag to GitHub

Push the version tag to trigger automated publishing:

```bash
git push origin main --tags
```

**Important**: Both the commit AND the tag must be pushed. The `--tags` flag ensures tags are pushed.

### 5. GitHub Actions Workflow

Once the tag is pushed, GitHub Actions automatically:

1. **Checkout code** at the tagged version
2. **Install dependencies**: `npm ci`
3. **Run build**: `npm run build`
4. **Run tests**: `npm test`
5. **Publish to npm**: `npm publish --access public`

Monitor the workflow at: https://github.com/loqalabs/loqa-audio-dsp/actions

### 6. Verify npm Publication

After the workflow succeeds, verify the package on npm:

```bash
npm view @loqalabs/loqa-audio-dsp
```

Check that:

- Version matches your release
- Published date is recent
- Files are correctly included

Test installation in a clean project:

```bash
npx create-expo-app test-project
cd test-project
npx expo install @loqalabs/loqa-audio-dsp
```

### 7. Create GitHub Release

After npm publication, create a GitHub Release:

1. Go to: https://github.com/loqalabs/loqa-audio-dsp/releases/new
2. Select the tag you just created
3. Title: `v0.1.0` (or appropriate version)
4. Description: Copy relevant section from CHANGELOG.md
5. Include:
   - Summary of changes
   - Breaking changes (if any)
   - Installation instructions
   - Link to documentation
6. Click "Publish release"

### 8. Post-Release Verification

After release:

- [ ] Package appears on npm: https://www.npmjs.com/package/@loqalabs/loqa-audio-dsp
- [ ] GitHub release is created: https://github.com/loqalabs/loqa-audio-dsp/releases
- [ ] Installation works: `npx expo install @loqalabs/loqa-audio-dsp`
- [ ] Example app works with published version

## Troubleshooting

### Publishing Fails

**Issue**: GitHub Actions publish step fails

**Solution**:

1. Check NPM_TOKEN secret is configured correctly
2. Verify token has publish permissions
3. Ensure package name is available on npm
4. Check for network issues or npm registry outages

### Version Tag Already Exists

**Issue**: Cannot create tag because it already exists

**Solution**:

```bash
# Delete local tag
git tag -d v0.1.0

# Delete remote tag
git push origin :refs/tags/v0.1.0

# Create new tag
npm version patch
git push origin main --tags
```

### Tests Fail in CI

**Issue**: Tests pass locally but fail in GitHub Actions

**Solution**:

1. Check Node.js version matches CI (18+)
2. Ensure all dependencies are in `package.json` (not global)
3. Review CI logs for environment differences
4. Test with `npm ci` instead of `npm install` locally

### Package Not Found After Publishing

**Issue**: Package published but `npm install` fails

**Solution**:

1. Wait a few minutes for npm CDN propagation
2. Try installing with full version: `npm install @loqalabs/loqa-audio-dsp@0.1.0`
3. Check package is public: `npm access public @loqalabs/loqa-audio-dsp`

## Rollback

If a release has critical issues:

### 1. Deprecate the Version

```bash
npm deprecate @loqalabs/loqa-audio-dsp@0.1.0 "This version has critical issues, please use 0.1.1"
```

### 2. Release a Patch

Follow the release process to publish a fixed version immediately.

### 3. Update Documentation

- Update CHANGELOG.md with the fix
- Update GitHub Release notes if necessary

## Semantic Versioning

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR** (X.0.0): Breaking API changes
- **MINOR** (0.X.0): New features, backward compatible
- **PATCH** (0.0.X): Bug fixes, backward compatible

For pre-1.0.0 releases:

- Breaking changes are allowed in MINOR versions
- We're currently in MVP phase (0.1.x)

## Questions?

For issues with the release process:

- File an issue: https://github.com/loqalabs/loqa-audio-dsp/issues
- Contact maintainers: Loqa Labs

---

**Last Updated**: 2025-11-21
**Process Version**: 1.0
