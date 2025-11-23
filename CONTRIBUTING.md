# Contributing to Vaultify

Thank you for your interest in contributing to Vaultify! This document provides guidelines and instructions for contributing to the project.

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

## How to Contribute

### Reporting Bugs

If you find a bug, please create an issue with:
- A clear, descriptive title
- Steps to reproduce the issue
- Expected behavior vs actual behavior
- Environment details (OS, Java version, Node version, etc.)
- Screenshots if applicable

### Suggesting Features

Feature suggestions are welcome! Please create an issue with:
- A clear description of the feature
- Use cases and benefits
- Any implementation ideas (optional)

### Pull Requests

1. **Fork the repository** and create a new branch from `main`
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Follow the existing code style
   - Write clear, descriptive commit messages
   - Add tests if applicable
   - Update documentation as needed

3. **Test your changes**
   - Run backend tests: `cd backend && ./mvnw test`
   - Run frontend linting: `cd frontend && npm run lint`
   - Test the application manually

4. **Submit a Pull Request**
   - Provide a clear description of your changes
   - Reference any related issues
   - Ensure all checks pass

## Development Setup

### Prerequisites

- **Backend**: Java 21, Maven
- **Frontend**: Node.js 18+, npm

### Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/vaultify.git
   cd vaultify
   ```

2. Set up the backend:
   ```bash
   cd backend
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   # Edit application.properties with your configuration
   ./mvnw spring-boot:run
   ```

3. Set up the frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## Code Style

### Backend (Java)

- Follow Java naming conventions
- Use meaningful variable and method names
- Add Javadoc comments for public methods
- Keep methods focused and single-purpose

### Frontend (React/JavaScript)

- Use functional components with hooks
- Follow React best practices
- Use meaningful component and variable names
- Keep components small and focused

## Commit Messages

Write clear, descriptive commit messages:
- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters
- Reference issues and pull requests when applicable

Example:
```
Add file sharing functionality

- Implement share dialog component
- Add permission management API
- Update GraphQL schema for sharing

Fixes #123
```

## Testing

- Write tests for new features
- Ensure existing tests pass
- Test edge cases and error handling

## Documentation

- Update README.md if you add new features
- Add JSDoc/Javadoc comments for public APIs
- Update API documentation if endpoints change

## Questions?

Feel free to open an issue with the `question` label if you need help or have questions about contributing.

Thank you for contributing to Vaultify! 🎉

