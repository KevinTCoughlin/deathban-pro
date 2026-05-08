# Contributing to DeathBan Pro

Thank you for your interest in contributing to DeathBan Pro! This document provides guidelines for contributing to the project.

## Getting Started

### Prerequisites

- Java 21 or higher
- Gradle (wrapper included)
- A Minecraft server (Spigot or Paper 1.21+) for testing

### Setting Up Development Environment

1. Fork the repository
2. Clone your fork:

   ```bash
   git clone https://github.com/YOUR_USERNAME/deathban-pro.git
   cd deathban-pro
   ```

3. Build the project:

   ```bash
   ./gradlew build
   ```

4. Run tests:

   ```bash
   ./gradlew test
   ```

## Development Workflow

### Branching

- Create feature branches from `main`
- Use descriptive branch names: `feat/add-placeholder-api`, `fix/ban-duration-parsing`

### Commit Messages

We use conventional commits:

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation only
- `refactor:` Code change that neither fixes a bug nor adds a feature
- `test:` Adding or updating tests
- `chore:` Maintenance tasks

Examples:

```
feat: add PlaceholderAPI integration
fix: correct ban duration calculation for edge cases
docs: update README with new commands
```

### Code Style

- Follow Kotlin coding conventions
- Use 4 spaces for indentation
- Keep lines under 120 characters when practical
- Add KDoc comments for public APIs

### Testing

- Write unit tests for new functionality
- Ensure all tests pass before submitting PR
- Manual test on a Minecraft server when possible

## Submitting Changes

### Pull Request Process

1. Update documentation if needed
2. Add tests for new functionality
3. Ensure CI passes
4. Request review from maintainers

### Pull Request Guidelines

- Keep PRs focused on a single concern
- Provide a clear description of changes
- Reference related issues
- Be responsive to feedback

## Reporting Bugs

Use the GitHub issue template for bug reports. Include:

- Server software and version
- Plugin version
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs

## Feature Requests

Open an issue using the feature request template. Describe:

- The use case
- Proposed solution
- Any alternatives considered

## Code of Conduct

Be respectful and constructive in all interactions. We're all here to make a great plugin together.

## Questions?

Open a discussion on GitHub or reach out to the maintainers.

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
