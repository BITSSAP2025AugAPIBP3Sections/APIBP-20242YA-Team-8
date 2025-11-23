import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Landing = () => {
  const { user } = useAuth();

  const features = [
    {
      icon: '🔐',
      title: 'Secure Authentication',
      description: 'JWT-based authentication with secure token management and password hashing.',
    },
    {
      icon: '📁',
      title: 'Folder Management',
      description: 'Organize your files with nested folder structures and intuitive navigation.',
    },
    {
      icon: '📤',
      title: 'File Upload & Storage',
      description: 'Upload, download, and manage files with support for multiple file types.',
    },
    {
      icon: '🔗',
      title: 'File Sharing',
      description: 'Share files and folders with granular permission controls (READ, WRITE, OWNER).',
    },
    {
      icon: '🌐',
      title: 'REST & GraphQL APIs',
      description: 'Comprehensive API support with both REST endpoints and GraphQL queries.',
    },
    {
      icon: '📊',
      title: 'API Documentation',
      description: 'Interactive Swagger UI and GraphiQL playground for easy API exploration.',
    },
  ];

  const techStack = [
    { name: 'Spring Boot 3.5.6', category: 'Backend' },
    { name: 'React 19', category: 'Frontend' },
    { name: 'JWT Authentication', category: 'Security' },
    { name: 'GraphQL', category: 'API' },
    { name: 'Tailwind CSS', category: 'Styling' },
    { name: 'H2 Database', category: 'Database' },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50">
      {/* Navigation */}
      <nav className="bg-white/80 backdrop-blur-sm border-b border-gray-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
                Vaultify
              </h1>
              <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-gradient-to-r from-green-500 to-emerald-500 text-white shadow-sm">
                Open Source
              </span>
            </div>
            <div className="flex items-center space-x-4">
              <a
                href="https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YA-Team-8"
                target="_blank"
                rel="noopener noreferrer"
                className="text-gray-600 hover:text-gray-900 transition-colors"
              >
                <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
                </svg>
              </a>
              {user ? (
                <Link
                  to="/folders"
                  className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
                >
                  Go to App
                </Link>
              ) : (
                <div className="flex space-x-3">
                  <Link
                    to="/login"
                    className="px-4 py-2 text-gray-700 hover:text-gray-900 transition-colors"
                  >
                    Sign In
                  </Link>
                  <Link
                    to="/register"
                    className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
                  >
                    Get Started
                  </Link>
                </div>
              )}
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-16">
        <div className="text-center">
          <h1 className="text-5xl md:text-6xl font-bold text-gray-900 mb-6">
            Modern File Storage & Sharing
            <span className="block bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
              Made Simple
            </span>
          </h1>
          <p className="text-xl text-gray-600 max-w-3xl mx-auto mb-8">
            Vaultify is an open-source file management platform built with Spring Boot and React.
            Organize, share, and manage your files with enterprise-grade security and a beautiful interface.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
            {!user && (
              <>
                <Link
                  to="/register"
                  className="px-8 py-3 bg-indigo-600 text-white rounded-lg text-lg font-semibold hover:bg-indigo-700 transition-all transform hover:scale-105 shadow-lg hover:shadow-xl"
                >
                  Get Started Free
                </Link>
                <a
                  href="https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YA-Team-8"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-8 py-3 bg-white text-gray-900 rounded-lg text-lg font-semibold border-2 border-gray-300 hover:border-gray-400 transition-all transform hover:scale-105 shadow-lg hover:shadow-xl"
                >
                  View on GitHub
                </a>
              </>
            )}
            {user && (
              <Link
                to="/folders"
                className="px-8 py-3 bg-indigo-600 text-white rounded-lg text-lg font-semibold hover:bg-indigo-700 transition-all transform hover:scale-105 shadow-lg hover:shadow-xl"
              >
                Continue to App
              </Link>
            )}
          </div>
          <div className="mt-12 flex items-center justify-center space-x-8 text-sm text-gray-500">
            <div className="flex items-center">
              <svg className="w-5 h-5 text-green-500 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
              Open Source
            </div>
            <div className="flex items-center">
              <svg className="w-5 h-5 text-green-500 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
              MIT License
            </div>
            <div className="flex items-center">
              <svg className="w-5 h-5 text-green-500 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
              Self-Hosted
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="text-center mb-16">
          <h2 className="text-4xl font-bold text-gray-900 mb-4">Powerful Features</h2>
          <p className="text-xl text-gray-600 max-w-2xl mx-auto">
            Everything you need for modern file management and sharing
          </p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {features.map((feature, index) => (
            <div
              key={index}
              className="bg-white p-6 rounded-xl shadow-lg hover:shadow-xl transition-shadow border border-gray-100"
            >
              <div className="text-4xl mb-4">{feature.icon}</div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">{feature.title}</h3>
              <p className="text-gray-600">{feature.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Tech Stack Section */}
      <section className="bg-white py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">Built With Modern Technology</h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              Leveraging the best tools and frameworks for performance and developer experience
            </p>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
            {techStack.map((tech, index) => (
              <div
                key={index}
                className="bg-gradient-to-br from-indigo-50 to-purple-50 p-6 rounded-lg text-center border border-indigo-100 hover:shadow-lg transition-shadow"
              >
                <p className="font-semibold text-gray-900">{tech.name}</p>
                <p className="text-sm text-gray-600 mt-1">{tech.category}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Getting Started Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="bg-gradient-to-r from-indigo-600 to-purple-600 rounded-2xl p-12 text-white text-center">
          <h2 className="text-4xl font-bold mb-4">Ready to Get Started?</h2>
          <p className="text-xl text-indigo-100 mb-8 max-w-2xl mx-auto">
            Set up Vaultify in minutes and start managing your files with enterprise-grade security.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            {!user && (
              <>
                <Link
                  to="/register"
                  className="px-8 py-3 bg-white text-indigo-600 rounded-lg text-lg font-semibold hover:bg-gray-100 transition-all transform hover:scale-105 shadow-lg"
                >
                  Create Account
                </Link>
                <a
                  href="https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YA-Team-8#quick-start"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-8 py-3 bg-indigo-700 text-white rounded-lg text-lg font-semibold hover:bg-indigo-800 transition-all transform hover:scale-105 border-2 border-white/20"
                >
                  View Documentation
                </a>
              </>
            )}
            {user && (
              <Link
                to="/folders"
                className="px-8 py-3 bg-white text-indigo-600 rounded-lg text-lg font-semibold hover:bg-gray-100 transition-all transform hover:scale-105 shadow-lg"
              >
                Continue to App
              </Link>
            )}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-gray-400 py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            <div>
              <h3 className="text-white text-lg font-bold mb-4">Vaultify</h3>
              <p className="text-sm">
                Open-source file storage and sharing platform built with Spring Boot and React.
              </p>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">Resources</h4>
              <ul className="space-y-2 text-sm">
                <li>
                  <a href="https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YA-Team-8" className="hover:text-white transition-colors">
                    GitHub
                  </a>
                </li>
                <li>
                  <a href="https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YA-Team-8/blob/main/README.md" className="hover:text-white transition-colors">
                    Documentation
                  </a>
                </li>
                <li>
                  <a href="https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YA-Team-8/blob/main/CONTRIBUTING.md" className="hover:text-white transition-colors">
                    Contributing
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">Legal</h4>
              <ul className="space-y-2 text-sm">
                <li>
                  <a href="https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YA-Team-8/blob/main/LICENSE" className="hover:text-white transition-colors">
                    License
                  </a>
                </li>
                <li>
                  <a href="https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YA-Team-8/blob/main/CODE_OF_CONDUCT.md" className="hover:text-white transition-colors">
                    Code of Conduct
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">Connect</h4>
              <div className="flex space-x-4">
                <a
                  href="https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YA-Team-8"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-white transition-colors"
                >
                  <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
                  </svg>
                </a>
              </div>
            </div>
          </div>
          <div className="mt-8 pt-8 border-t border-gray-800 text-center text-sm">
            <p>© 2025 BITSSAP2025AugAPIBP3Sections. Licensed under MIT License.</p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Landing;

