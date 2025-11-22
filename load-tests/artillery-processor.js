// Artillery processor functions for dynamic data generation

module.exports = {
  generateRandomString,
  generateUsername,
  generateFolderName
};

/**
 * Generate a random string of specified length
 */
function generateRandomString(length = 10) {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

/**
 * Generate a unique username for load testing
 */
function generateUsername(context, events, done) {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 15);
  context.vars.username = `loadtest_${timestamp}_${random}`;
  return done();
}

/**
 * Generate a unique folder name
 */
function generateFolderName(context, events, done) {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 9);
  context.vars.folderName = `LoadTestFolder_${timestamp}_${random}`;
  return done();
}

