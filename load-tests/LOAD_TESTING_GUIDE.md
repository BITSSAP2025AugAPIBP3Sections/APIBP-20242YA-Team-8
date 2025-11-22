# Load Testing Guide for REST APIs

## What is Load Testing?

Load testing is a type of performance testing that simulates real-world usage of your API by generating multiple concurrent requests. It helps you understand how your API behaves under various load conditions and identifies performance bottlenecks before they become problems in production.

### Key Concepts

**Load Testing** = Testing your API's ability to handle expected traffic
- Simulates normal to high user traffic
- Helps identify performance issues
- Validates system capacity
- Ensures reliability under load

## Why Load Test REST APIs?

### 1. **Performance Validation**
- Ensure your API responds within acceptable time limits
- Verify that response times don't degrade under load
- Identify slow endpoints that need optimization

### 2. **Capacity Planning**
- Determine how many users your API can handle simultaneously
- Plan infrastructure scaling needs
- Estimate resource requirements (CPU, memory, database connections)

### 3. **Reliability Assurance**
- Find breaking points before production
- Identify memory leaks or resource exhaustion
- Ensure error handling works correctly under stress

### 4. **Cost Optimization**
- Right-size your infrastructure
- Avoid over-provisioning (wasting money)
- Avoid under-provisioning (poor user experience)

### 5. **User Experience**
- Prevent slow responses that frustrate users
- Avoid downtime during traffic spikes
- Maintain service quality during peak usage

## How Load Testing Works

### The Process

```
1. Define Test Scenarios
   ↓
2. Configure Load Profile (users, duration, ramp-up)
   ↓
3. Execute Test (simulate concurrent users)
   ↓
4. Collect Metrics (response times, errors, throughput)
   ↓
5. Analyze Results (identify bottlenecks)
   ↓
6. Optimize & Re-test
```

### Key Components

#### 1. **Virtual Users (VUs)**
- Simulated users making requests to your API
- Each VU executes a test scenario independently
- More VUs = higher load on your API

#### 2. **Test Scenarios**
- Sequence of API calls that simulate user behavior
- Example: Register → Login → Get Data → Create Resource

#### 3. **Load Profile**
- **Ramp-up**: Gradually increase users (e.g., 0 → 100 users over 2 minutes)
- **Sustained Load**: Maintain constant load for a period
- **Ramp-down**: Gradually decrease users

#### 4. **Metrics Collection**
- Response times (how fast API responds)
- Throughput (requests per second)
- Error rates (failed requests)
- Resource usage (CPU, memory)

## Key Metrics Explained

### 1. **Response Time (Latency)**

**What it measures**: How long it takes for the API to respond to a request

**Key Percentiles**:
- **p50 (Median)**: 50% of requests are faster than this
- **p95**: 95% of requests are faster than this (most important for user experience)
- **p99**: 99% of requests are faster than this (catches outliers)

**Example**:
```
Response Time:
  min: 10ms
  median (p50): 150ms
  p95: 500ms
  p99: 1000ms
  max: 5000ms
```

**What to look for**:
- ✅ Good: p95 < 500ms for most APIs
- ⚠️ Warning: p95 > 1000ms
- ❌ Bad: p95 > 5000ms or timeouts

### 2. **Throughput (Requests per Second - RPS)**

**What it measures**: Number of requests your API can handle per second

**Example**:
```
Throughput: 100 requests/second
```

**What to look for**:
- Compare with expected production traffic
- Ensure API can handle peak load
- Monitor if throughput decreases under load

### 3. **Error Rate**

**What it measures**: Percentage of requests that fail

**Common Error Types**:
- **4xx (Client Errors)**: Bad requests, authentication issues
- **5xx (Server Errors)**: Server failures, timeouts
- **Network Errors**: Connection timeouts, DNS failures

**Example**:
```
HTTP Status Codes:
  200 (Success): 95%
  401 (Unauthorized): 2%
  500 (Server Error): 3%
```

**What to look for**:
- ✅ Good: < 1% error rate
- ⚠️ Warning: 1-5% error rate
- ❌ Bad: > 5% error rate

### 4. **Concurrent Users**

**What it measures**: Number of simultaneous users making requests

**Example**:
```
Virtual Users:
  Created: 1000
  Completed: 950
  Failed: 50
```

**What to look for**:
- How many users can your API handle?
- At what point do errors start increasing?
- When does performance degrade?

## Load Testing Scenarios

### 1. **Baseline Test**
- Low, steady load
- Establishes performance baseline
- Example: 10 users for 5 minutes

### 2. **Load Test**
- Expected production load
- Validates normal operation
- Example: 100 users for 10 minutes

### 3. **Stress Test**
- Beyond normal capacity
- Finds breaking point
- Example: Gradually increase from 100 to 500 users

### 4. **Spike Test**
- Sudden traffic increase
- Tests system resilience
- Example: 0 → 200 users in 30 seconds

### 5. **Endurance Test**
- Long duration, steady load
- Finds memory leaks, resource exhaustion
- Example: 50 users for 1 hour

## REST API Load Testing Best Practices

### 1. **Start Small, Scale Up**
```
Phase 1: 10 users → Baseline
Phase 2: 50 users → Light load
Phase 3: 100 users → Normal load
Phase 4: 200+ users → Stress test
```

### 2. **Test Realistic Scenarios**
- Test actual user workflows
- Include authentication flows
- Mix read and write operations
- Simulate real data patterns

### 3. **Monitor Multiple Metrics**
- Don't just look at response time
- Watch error rates
- Monitor resource usage (CPU, memory, database)
- Track throughput

### 4. **Test Different Endpoints**
- Not all endpoints are equal
- Some are read-heavy (GET)
- Some are write-heavy (POST, PUT, DELETE)
- Test each type appropriately

### 5. **Use Realistic Data**
- Test with production-like data sizes
- Include edge cases
- Test with various payload sizes

### 6. **Test Authentication**
- JWT token generation/validation
- Session management
- Rate limiting behavior

### 7. **Test Error Handling**
- How API handles invalid requests
- Error response times
- Graceful degradation

## Interpreting Results

### Good Performance Indicators ✅

```
✅ Response Time (p95) < 500ms
✅ Error Rate < 1%
✅ Throughput meets expected load
✅ No memory leaks (stable over time)
✅ CPU usage reasonable (< 80%)
✅ Database connections stable
```

### Warning Signs ⚠️

```
⚠️ Response time increasing with load
⚠️ Error rate > 1% but < 5%
⚠️ Throughput decreasing under load
⚠️ Memory usage slowly increasing
⚠️ Some endpoints much slower than others
```

### Critical Issues ❌

```
❌ Response time (p95) > 2000ms
❌ Error rate > 5%
❌ API crashes or becomes unresponsive
❌ Memory leaks (continuous growth)
❌ Database connection pool exhaustion
❌ Timeouts increasing
```

## Common Performance Issues Found

### 1. **Slow Database Queries**
- **Symptom**: High response times, especially on GET endpoints
- **Solution**: Add database indexes, optimize queries, use caching

### 2. **Memory Leaks**
- **Symptom**: Memory usage continuously increases over time
- **Solution**: Review code for unclosed resources, fix memory leaks

### 3. **Connection Pool Exhaustion**
- **Symptom**: Errors increase, "too many connections" errors
- **Solution**: Increase connection pool size, optimize connection usage

### 4. **Inefficient Algorithms**
- **Symptom**: Response time increases exponentially with data size
- **Solution**: Optimize algorithms, add pagination, limit result sets

### 5. **Lack of Caching**
- **Symptom**: Repeated requests take same time
- **Solution**: Implement caching for frequently accessed data

### 6. **Synchronous Blocking Operations**
- **Symptom**: High response times, low throughput
- **Solution**: Use async operations, background processing

## Load Testing Workflow

### Step 1: Plan Your Test
- Define objectives (what are you testing?)
- Identify critical endpoints
- Set performance targets (e.g., p95 < 500ms)
- Determine test scenarios

### Step 2: Set Up Test Environment
- Use staging environment (similar to production)
- Ensure test data is available
- Set up monitoring (logs, metrics, alerts)

### Step 3: Create Test Scenarios
- Define user workflows
- Create test scripts
- Configure load profiles

### Step 4: Run Tests
- Start with baseline
- Gradually increase load
- Monitor in real-time
- Document observations

### Step 5: Analyze Results
- Review metrics
- Identify bottlenecks
- Compare against targets
- Document findings

### Step 6: Optimize
- Fix identified issues
- Optimize slow endpoints
- Improve error handling

### Step 7: Re-test
- Verify improvements
- Confirm issues are resolved
- Validate performance targets

## Example: Load Testing a REST API

### Scenario: User Authentication Flow

```
1. Register new user (POST /auth/register)
   ↓
2. Login (POST /auth/login) → Get token
   ↓
3. Get user profile (GET /auth/me)
   ↓
4. Create folder (POST /api/folders)
   ↓
5. List folders (GET /api/folders)
   ↓
6. Upload file (POST /api/files/upload)
```

### Load Profile

```yaml
phases:
  - duration: 60s    # Warm up
    arrivalRate: 5    # 5 new users per second
    
  - duration: 300s    # Ramp up
    arrivalRate: 10   # 10 new users per second
    
  - duration: 600s    # Sustained load
    arrivalRate: 20   # 20 new users per second
    
  - duration: 60s    # Ramp down
    arrivalRate: 5    # 5 new users per second
```

### Expected Results

```
✅ All endpoints respond in < 500ms (p95)
✅ Error rate < 1%
✅ Can handle 20 concurrent users/second
✅ No memory leaks over 15 minutes
✅ Database connections stable
```

## Tools for REST API Load Testing

### Artillery (What we're using)
- **Pros**: Easy YAML configuration, good for REST APIs, cloud integration
- **Best for**: Quick setup, team collaboration, cloud reporting

### Other Popular Tools
- **k6**: JavaScript-based, flexible, good for complex scenarios
- **JMeter**: GUI-based, feature-rich, good for beginners
- **Gatling**: Scala-based, high performance, detailed reports
- **Locust**: Python-based, code-driven, good for developers

## Key Takeaways

1. **Load testing is essential** for production-ready APIs
2. **Start early** - don't wait until production
3. **Test incrementally** - start small, scale up
4. **Monitor everything** - response times, errors, resources
5. **Set realistic targets** - based on user expectations
6. **Test regularly** - performance can degrade over time
7. **Document results** - track improvements and regressions
8. **Optimize iteratively** - fix issues, re-test, repeat

## Real-World Example

### Before Optimization
```
Response Time (p95): 2000ms
Error Rate: 8%
Throughput: 50 req/s
Issues: Slow database queries, no caching
```

### After Optimization
```
Response Time (p95): 300ms ✅
Error Rate: 0.5% ✅
Throughput: 200 req/s ✅
Improvements: Added indexes, implemented caching, optimized queries
```

## Conclusion

Load testing REST APIs helps you:
- **Ensure reliability** under real-world conditions
- **Identify bottlenecks** before they impact users
- **Plan capacity** for scaling
- **Optimize performance** based on data
- **Build confidence** in your API's ability to handle traffic

Remember: **It's better to find performance issues in testing than in production!**

---

For practical examples using Artillery, see:
- [ARTILLERY_README.md](./ARTILLERY_README.md) - How to run load tests
- [API_CURL_COMMANDS.md](./API_CURL_COMMANDS.md) - API endpoints reference

