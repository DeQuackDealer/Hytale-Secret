import { BrowserRouter, Routes, Route, Link, useNavigate, useLocation } from 'react-router-dom';
import { useState, useEffect, createContext, useContext } from 'react';
import { Download, Users, Gamepad2, Shield, Zap, Menu, X, LogIn, LogOut, User, Settings, Crown, Clock, BarChart3, Layers, Check, Eye, EyeOff, Mail, Lock, UserPlus, Search, Tag, Star, Upload, DollarSign, Package, Palette, Sparkles, Server, Cpu, Activity, Heart, Filter, Grid, List, Smile, Trash2, ShieldCheck, Wallet, Home as HomeIcon, Play, ShoppingBag, Map, Target, Award, TrendingUp, Monitor, Volume2, HardDrive, Bell, ChevronLeft, ChevronRight, MessageSquare, Wifi } from 'lucide-react';
import './App.css';

const API_URL = import.meta.env.VITE_API_URL || '';

interface UserData {
  id: string;
  username: string;
  display_name?: string;
  avatar_url?: string;
}

interface AuthContextType {
  user: UserData | null;
  token: string | null;
  login: (username: string, password: string) => Promise<void>;
  signup: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserData | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));

  useEffect(() => {
    if (token) {
      fetch(`${API_URL}/api/v1/auth/me`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token }),
      })
        .then(r => r.json())
        .then(data => {
          if (data.success) setUser(data.data);
          else { localStorage.removeItem('token'); setToken(null); }
        })
        .catch(() => {});
    }
  }, [token]);

  const login = async (username: string, password: string) => {
    const res = await fetch(`${API_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const data = await res.json();
    if (!data.success) throw new Error(data.error);
    localStorage.setItem('token', data.data.token);
    setToken(data.data.token);
    setUser(data.data.user);
  };

  const signup = async (username: string, email: string, password: string) => {
    const res = await fetch(`${API_URL}/api/v1/auth/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email, password }),
    });
    const data = await res.json();
    if (!data.success) throw new Error(data.error);
    localStorage.setItem('token', data.data.token);
    setToken(data.data.token);
    setUser(data.data.user);
  };

  const logout = () => {
    if (token) {
      fetch(`${API_URL}/api/v1/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token }),
      }).catch(() => {});
    }
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

function Navbar() {
  const [isOpen, setIsOpen] = useState(false);
  const { user, logout } = useAuth();

  return (
    <nav className="navbar">
      <div className="nav-container">
        <Link to="/" className="nav-logo">
          <Cpu size={32} />
          <span>Rubidium</span>
        </Link>
        
        <button className="nav-toggle" onClick={() => setIsOpen(!isOpen)}>
          {isOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
        
        <div className={`nav-links ${isOpen ? 'open' : ''}`}>
          <Link to="/" onClick={() => setIsOpen(false)}>Home</Link>
          <Link to="/rubidium/documentation" onClick={() => setIsOpen(false)}>Docs</Link>
          <Link to="/marketplace" onClick={() => setIsOpen(false)}>Marketplace</Link>
          <Link to="/download" onClick={() => setIsOpen(false)}>Download</Link>
          <Link to="/premium" onClick={() => setIsOpen(false)}>Premium</Link>
          {user ? (
            <>
              <Link to="/dashboard" onClick={() => setIsOpen(false)}>Dashboard</Link>
              <button className="nav-btn" onClick={() => { logout(); setIsOpen(false); }}>
                <LogOut size={18} /> Logout
              </button>
            </>
          ) : (
            <Link to="/login" className="nav-btn" onClick={() => setIsOpen(false)}>
              <LogIn size={18} /> Login
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
}

function Home() {
  return (
    <div className="page">
      <section className="hero rubidium-focus-hero">
        <div className="hero-content">
          <div className="hero-badge">
            <Cpu size={20} /> Server Framework for Hytale
          </div>
          <h1>Rubidium</h1>
          <p className="hero-subtitle">Build Amazing Hytale Servers</p>
          <p className="hero-description">
            The production-ready server framework for Hytale. Modular architecture, 
            easy-to-use APIs, runtime hot-reload, and built-in performance optimization.
            Start building your dream server in minutes.
          </p>
          <div className="hero-buttons">
            <Link to="/rubidium/documentation" className="btn btn-primary">
              <Layers size={20} /> Read the Docs
            </Link>
            <Link to="/download" className="btn btn-secondary">
              <Download size={20} /> Download SDK
            </Link>
          </div>
        </div>
        <div className="hero-visual">
          <div className="hero-graphic rubidium-graphic">
            <Server size={180} strokeWidth={1} />
            <div className="orbit orbit-1"><Cpu size={32} /></div>
            <div className="orbit orbit-2"><Activity size={28} /></div>
            <div className="orbit orbit-3"><Layers size={24} /></div>
          </div>
        </div>
      </section>

      <section className="rubidium-highlight">
        <h2><Cpu size={32} /> Rubidium Server Framework</h2>
        <p className="section-subtitle">The backbone of high-performance Hytale servers</p>
        <div className="feature-grid">
          <div className="feature-card rubidium-card">
            <Layers size={48} />
            <h3>Modular Architecture</h3>
            <p>Runtime-reloadable modules with dependency resolution and isolated classloaders.</p>
          </div>
          <div className="feature-card rubidium-card">
            <Activity size={48} />
            <h3>Performance Budget</h3>
            <p>Per-module execution time tracking with soft limits and automatic task deferral.</p>
          </div>
          <div className="feature-card rubidium-card">
            <Clock size={48} />
            <h3>Tick Scheduler</h3>
            <p>Priority-based task scheduling with async support and deferred execution queues.</p>
          </div>
          <div className="feature-card rubidium-card">
            <BarChart3 size={48} />
            <h3>Metrics & Profiling</h3>
            <p>Real-time tick timing, memory sampling, and exportable metrics interface.</p>
          </div>
          <div className="feature-card rubidium-card">
            <Wifi size={48} />
            <h3>Network Layer</h3>
            <p>Packet batching, priority queues, and bandwidth limiting without protocol assumptions.</p>
          </div>
          <div className="feature-card rubidium-card">
            <Settings size={48} />
            <h3>Hot-Reload Config</h3>
            <p>Typed configurations with validation, file watching, and automatic reload support.</p>
          </div>
        </div>
        <div className="cta-buttons" style={{ marginTop: '2rem', display: 'flex', justifyContent: 'center', gap: '1rem' }}>
          <Link to="/rubidium" className="btn btn-primary">
            <Cpu size={20} /> Learn More About Rubidium
          </Link>
        </div>
      </section>

      <section className="features-preview">
        <h2>Easy-to-Use APIs</h2>
        <p className="section-subtitle">Build plugins in minutes with clean, intuitive interfaces</p>
        <div className="feature-grid">
          <div className="feature-card">
            <Volume2 size={48} />
            <h3>Voice Chat API</h3>
            <p>Add proximity voice, groups, and whisper mode with just a few lines of code.</p>
          </div>
          <div className="feature-card">
            <Map size={48} />
            <h3>Map Integration</h3>
            <p>Sync markers, waypoints, and objectives with Hytale's built-in world map.</p>
          </div>
          <div className="feature-card">
            <Layers size={48} />
            <h3>UI Framework</h3>
            <p>Declarative UI components that render beautifully in-game.</p>
          </div>
          <div className="feature-card">
            <Zap size={48} />
            <h3>Performance Tools</h3>
            <p>Entity culling, chunk optimization, and automatic performance tuning.</p>
          </div>
          <div className="feature-card">
            <Shield size={48} />
            <h3>Permissions</h3>
            <p>Fine-grained permission system with groups and inheritance.</p>
          </div>
          <div className="feature-card">
            <Target size={48} />
            <h3>Teleportation</h3>
            <p>Complete /tp, /tpa, homes, and warps - ready to use.</p>
          </div>
        </div>
        <div className="cta-buttons" style={{ marginTop: '2rem', display: 'flex', justifyContent: 'center', gap: '1rem' }}>
          <Link to="/rubidium/documentation/apis" className="btn btn-primary">
            <Layers size={20} /> Explore All APIs
          </Link>
        </div>
      </section>

      <section className="marketplace-preview">
        <h2><Sparkles size={32} /> Marketplace</h2>
        <p className="section-subtitle">Discover and share mods, skins, and cosmetics with the community</p>
        <div className="feature-grid">
          <div className="feature-card">
            <Package size={48} />
            <h3>Mods & Plugins</h3>
            <p>Browse and install community-made mods and plugins.</p>
          </div>
          <div className="feature-card">
            <Palette size={48} />
            <h3>Skins & Cosmetics</h3>
            <p>Express yourself with unique skins visible to all players.</p>
          </div>
          <div className="feature-card">
            <Smile size={48} />
            <h3>Emotes & Animations</h3>
            <p>Celebrate victories and express yourself with custom emotes.</p>
          </div>
          <div className="feature-card">
            <DollarSign size={48} />
            <h3>Creator Economy</h3>
            <p>Creators can sell their work or share it for free.</p>
          </div>
        </div>
        <div className="cta-buttons" style={{ marginTop: '2rem', display: 'flex', justifyContent: 'center', gap: '1rem' }}>
          <Link to="/marketplace" className="btn btn-primary">
            <Sparkles size={20} /> Browse Marketplace
          </Link>
        </div>
      </section>

      <section className="cta-section">
        <div className="cta-content">
          <h2>Ready to Build?</h2>
          <p>Start creating amazing Hytale server experiences with Rubidium today.</p>
          <div className="cta-buttons">
            <Link to="/rubidium/documentation/getting-started" className="btn btn-primary">
              <Layers size={20} /> Getting Started Guide
            </Link>
            <Link to="/download" className="btn btn-secondary">
              <Download size={20} /> Download SDK
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}

function RubidiumPage() {
  return (
    <div className="page rubidium-page">
      <section className="rubidium-hero">
        <div className="rubidium-header">
          <div className="rubidium-logo">
            <Cpu size={80} />
          </div>
          <h1>Rubidium</h1>
          <p className="rubidium-tagline">Production-Ready Server Framework for Hytale</p>
          <p className="rubidium-description">
            Rubidium is a modular, runtime-reloadable, performance-focused framework for 
            Hytale servers. Built with Java 25, it provides the foundation for building 
            scalable server infrastructure while remaining safe for official modding APIs.
          </p>
          <div className="hero-buttons">
            <a href="#core-systems" className="btn btn-primary">
              <Layers size={20} /> Explore Core Systems
            </a>
            <a href="#get-started" className="btn btn-secondary">
              <Download size={20} /> Get Started
            </a>
          </div>
        </div>
      </section>

      <section className="design-principles">
        <h2>Design Principles</h2>
        <div className="principles-grid">
          <div className="principle-card">
            <Layers size={36} />
            <h3>Modular</h3>
            <p>Load, unload, and reload modules at runtime without server restarts</p>
          </div>
          <div className="principle-card">
            <Activity size={36} />
            <h3>Performance-Focused</h3>
            <p>Built-in budget management and metrics to maintain consistent tick rates</p>
          </div>
          <div className="principle-card">
            <Shield size={36} />
            <h3>API-Safe</h3>
            <p>No game internals access - ready for official modding APIs when available</p>
          </div>
          <div className="principle-card">
            <Settings size={36} />
            <h3>Extensible</h3>
            <p>Clean interfaces with default implementations and clear extension points</p>
          </div>
        </div>
      </section>

      <section id="core-systems" className="core-systems">
        <h2>Core Systems</h2>
        <p className="section-subtitle">Production-quality building blocks for your server infrastructure</p>
        
        <div className="system-cards">
          <div className="system-card">
            <div className="system-header">
              <Layers size={32} />
              <h3>Module System</h3>
            </div>
            <ul className="system-features">
              <li><Check size={16} /> Runtime load/unload with isolated classloaders</li>
              <li><Check size={16} /> Dependency resolution with topological sorting</li>
              <li><Check size={16} /> Safe enable/disable with rollback on failure</li>
              <li><Check size={16} /> JAR manifest-based module discovery</li>
            </ul>
            <code className="system-code">ModuleManager.loadModule(descriptor)</code>
          </div>

          <div className="system-card">
            <div className="system-header">
              <Play size={32} />
              <h3>Lifecycle Manager</h3>
            </div>
            <ul className="system-features">
              <li><Check size={16} /> onLoad, onEnable, onDisable, onReload hooks</li>
              <li><Check size={16} /> Phase-based lifecycle transitions</li>
              <li><Check size={16} /> Shutdown hooks in reverse order</li>
              <li><Check size={16} /> Event listeners for phase changes</li>
            </ul>
            <code className="system-code">LifecycleManager.addShutdownHook(name, action)</code>
          </div>

          <div className="system-card">
            <div className="system-header">
              <Settings size={32} />
              <h3>Config System</h3>
            </div>
            <ul className="system-features">
              <li><Check size={16} /> Typed configurations with validation</li>
              <li><Check size={16} /> Hot-reload with file watching</li>
              <li><Check size={16} /> Schema migration support</li>
              <li><Check size={16} /> Reload listeners for dynamic updates</li>
            </ul>
            <code className="system-code">ConfigManager.register(id, type, defaultValue)</code>
          </div>

          <div className="system-card">
            <div className="system-header">
              <Clock size={32} />
              <h3>Scheduler</h3>
            </div>
            <ul className="system-features">
              <li><Check size={16} /> Tick-based synchronous task execution</li>
              <li><Check size={16} /> Async tasks with CompletableFuture</li>
              <li><Check size={16} /> Priority-based ordering (LOW to CRITICAL)</li>
              <li><Check size={16} /> Deferred execution for non-critical work</li>
            </ul>
            <code className="system-code">scheduler.runTaskTimer(owner, task, delay, period)</code>
          </div>

          <div className="system-card">
            <div className="system-header">
              <Activity size={32} />
              <h3>Performance Budget</h3>
            </div>
            <ul className="system-features">
              <li><Check size={16} /> Per-module execution time tracking</li>
              <li><Check size={16} /> Soft budget enforcement with deferral</li>
              <li><Check size={16} /> Tick overrun detection and reporting</li>
              <li><Check size={16} /> Timing contexts for precise measurement</li>
            </ul>
            <code className="system-code">performanceManager.startTiming(moduleId)</code>
          </div>

          <div className="system-card">
            <div className="system-header">
              <BarChart3 size={32} />
              <h3>Metrics & Profiling</h3>
            </div>
            <ul className="system-features">
              <li><Check size={16} /> Counters, Gauges, Histograms, Timers</li>
              <li><Check size={16} /> Tick duration statistics with P99</li>
              <li><Check size={16} /> Memory usage sampling</li>
              <li><Check size={16} /> Exportable metrics interface</li>
            </ul>
            <code className="system-code">metricsRegistry.histogram("tick.duration").record(ms)</code>
          </div>

          <div className="system-card">
            <div className="system-header">
              <MessageSquare size={32} />
              <h3>Logging System</h3>
            </div>
            <ul className="system-features">
              <li><Check size={16} /> Structured logs with {} formatting</li>
              <li><Check size={16} /> Per-module log levels</li>
              <li><Check size={16} /> Async file writing with rotation</li>
              <li><Check size={16} /> Hierarchical logger names</li>
            </ul>
            <code className="system-code">logger.info("Module {} loaded in {}ms", id, time)</code>
          </div>

          <div className="system-card">
            <div className="system-header">
              <Wifi size={32} />
              <h3>Network Abstraction</h3>
            </div>
            <ul className="system-features">
              <li><Check size={16} /> Packet batching for efficiency</li>
              <li><Check size={16} /> Priority queues for traffic shaping</li>
              <li><Check size={16} /> Bandwidth limiting</li>
              <li><Check size={16} /> Packet interceptors for inspection</li>
            </ul>
            <code className="system-code">networkManager.send(connectionId, packet, priority)</code>
          </div>
        </div>
      </section>

      <section className="rubidium-integration">
        <h2>Yellow Tale Integration</h2>
        <p className="section-subtitle">Seamless connection between client and server</p>
        <div className="feature-grid">
          <div className="feature-card">
            <Users size={48} />
            <h3>Friend Activity Sync</h3>
            <p>See which Rubidium-powered servers your friends are playing on.</p>
          </div>
          <div className="feature-card">
            <Clock size={48} />
            <h3>Queue Management</h3>
            <p>Skip the queue with Premium, or see accurate wait times.</p>
          </div>
          <div className="feature-card">
            <Shield size={48} />
            <h3>Session Transfer</h3>
            <p>Seamlessly transfer between servers without re-authenticating.</p>
          </div>
        </div>
      </section>

      <section id="get-started" className="rubidium-download">
        <h2>Get Started</h2>
        <div className="download-options">
          <div className="download-card">
            <h3>Java SDK</h3>
            <p>Add to your Gradle or Maven project</p>
            <pre className="code-block">
{`implementation("com.yellowtale:rubidium-sdk:1.0.0")`}
            </pre>
            <a href="#" className="btn btn-primary">
              <Download size={18} /> View on Maven Central
            </a>
          </div>
          <div className="download-card">
            <h3>Example Module</h3>
            <p>Quick start template</p>
            <pre className="code-block">
{`public class MyModule extends AbstractModule {
  @Override public String getId() { return "my_module"; }
  @Override protected void doEnable() { 
    logger.info("Hello Rubidium!"); 
  }
}`}
            </pre>
            <a href="#" className="btn btn-secondary">
              <Download size={18} /> Download Template
            </a>
          </div>
          <div className="download-card">
            <h3>Documentation</h3>
            <p>Complete API reference and guides</p>
            <a href="#" className="btn btn-secondary">
              View Docs
            </a>
          </div>
        </div>
      </section>

      <section className="rubidium-legal">
        <div className="legal-links">
          <Link to="/rubidium/license" className="btn btn-secondary">
            <Shield size={18} /> View License
          </Link>
        </div>
      </section>
    </div>
  );
}

function RubidiumLicensePage() {
  return (
    <div className="page rubidium-license-page">
      <section className="license-hero">
        <div className="license-header">
          <Cpu size={64} />
          <h1>Rubidium</h1>
          <p className="license-tagline">Comprehensive Server Framework for Hytale</p>
        </div>
      </section>

      <section className="rubidium-overview">
        <h2>What is Rubidium?</h2>
        <div className="overview-content">
          <p>
            <strong>Rubidium</strong> is a production-ready, modular server framework designed specifically for 
            Hytale servers. Built with Java 25 and modern software engineering principles, Rubidium provides 
            server operators and developers with a comprehensive foundation for building high-performance, 
            scalable multiplayer experiences.
          </p>
          <p>
            Unlike traditional server modifications that rely on reverse engineering or memory injection, 
            Rubidium is designed to be <strong>API-safe</strong> from day one. This means it will seamlessly 
            integrate with official Hytale modding APIs when they become available, ensuring your server 
            infrastructure remains stable and compliant with Hypixel Studios' guidelines.
          </p>
        </div>
      </section>

      <section className="rubidium-features-comprehensive">
        <h2>Core Features</h2>
        
        <div className="feature-section">
          <h3><Layers size={24} /> Module System</h3>
          <p>
            The heart of Rubidium is its powerful module system. Modules can be loaded, unloaded, and reloaded 
            at runtime without requiring a server restart. Each module runs in an isolated classloader, preventing 
            conflicts between different plugins and allowing for safe hot-swapping during development.
          </p>
          <ul>
            <li><strong>Runtime Loading:</strong> Load new modules while the server is running</li>
            <li><strong>Hot Reloading:</strong> Update module code without server downtime</li>
            <li><strong>Dependency Resolution:</strong> Automatic topological sorting ensures modules load in correct order</li>
            <li><strong>Isolated Classloaders:</strong> Each module gets its own classloader for clean separation</li>
            <li><strong>Safe Unloading:</strong> Proper cleanup with onDisable hooks and GC hints</li>
          </ul>
        </div>

        <div className="feature-section">
          <h3><Play size={24} /> Lifecycle Manager</h3>
          <p>
            Rubidium provides a comprehensive lifecycle management system that coordinates startup, 
            shutdown, and reload operations across all subsystems.
          </p>
          <ul>
            <li><strong>Phase Transitions:</strong> STOPPED, STARTING, RUNNING, STOPPING, RELOADING</li>
            <li><strong>Lifecycle Hooks:</strong> onLoad, onEnable, onDisable, onReload for each module</li>
            <li><strong>Shutdown Hooks:</strong> Register cleanup actions that execute in reverse order</li>
            <li><strong>Rollback on Failure:</strong> If startup fails, already-started subsystems are cleanly stopped</li>
            <li><strong>Event Listeners:</strong> Subscribe to lifecycle phase changes</li>
          </ul>
        </div>

        <div className="feature-section">
          <h3><Settings size={24} /> Configuration System</h3>
          <p>
            A type-safe configuration system that supports hot-reloading, validation, and schema migration.
          </p>
          <ul>
            <li><strong>Typed Configs:</strong> Define configurations as Java classes with getters/setters</li>
            <li><strong>Validation:</strong> Built-in validators for ranges, required fields, patterns</li>
            <li><strong>Hot Reload:</strong> File watching automatically reloads configs when changed</li>
            <li><strong>Schema Migration:</strong> Upgrade old config formats to new versions</li>
            <li><strong>Reload Listeners:</strong> Get notified when configs change</li>
          </ul>
        </div>

        <div className="feature-section">
          <h3><Clock size={24} /> Tick-Based Scheduler</h3>
          <p>
            A powerful scheduler that integrates with the game's tick loop, supporting both synchronous 
            and asynchronous task execution.
          </p>
          <ul>
            <li><strong>Tick Synchronization:</strong> Tasks execute at precise tick intervals (20 TPS target)</li>
            <li><strong>Async Support:</strong> Run heavy operations on worker threads with CompletableFuture</li>
            <li><strong>Priority Queues:</strong> LOW, NORMAL, HIGH, CRITICAL task priorities</li>
            <li><strong>Deferred Execution:</strong> Non-critical tasks can be deferred when tick budget is exceeded</li>
            <li><strong>Repeating Tasks:</strong> Schedule tasks to run at fixed intervals</li>
          </ul>
        </div>

        <div className="feature-section">
          <h3><Activity size={24} /> Performance Budget Manager</h3>
          <p>
            Keep your server running at a consistent tick rate with per-module performance budgets 
            and automatic task deferral.
          </p>
          <ul>
            <li><strong>Per-Module Budgets:</strong> Each module gets allocated time per tick</li>
            <li><strong>Time Tracking:</strong> Measure exactly how long each module takes</li>
            <li><strong>Soft Limits:</strong> Warn or defer when modules exceed their budget</li>
            <li><strong>Tick Overrun Detection:</strong> Automatic reporting when ticks take too long</li>
            <li><strong>Budget Reset:</strong> Fresh allocation each tick for fair scheduling</li>
          </ul>
        </div>

        <div className="feature-section">
          <h3><BarChart3 size={24} /> Metrics & Profiling</h3>
          <p>
            Comprehensive metrics collection for monitoring server health and performance.
          </p>
          <ul>
            <li><strong>Counters:</strong> Track event counts (packets sent, tasks executed)</li>
            <li><strong>Gauges:</strong> Monitor current values (player count, memory usage)</li>
            <li><strong>Histograms:</strong> Record distributions with percentiles (P50, P95, P99)</li>
            <li><strong>Timers:</strong> Measure operation durations</li>
            <li><strong>Tick Statistics:</strong> Detailed tick timing with rolling averages</li>
            <li><strong>Memory Sampling:</strong> Track heap usage over time</li>
          </ul>
        </div>

        <div className="feature-section">
          <h3><MessageSquare size={24} /> Structured Logging</h3>
          <p>
            A logging system designed for high-performance servers with async writing and structured formatting.
          </p>
          <ul>
            <li><strong>Async File Writing:</strong> Logs are written on a background thread</li>
            <li><strong>Structured Format:</strong> Use {} placeholders for efficient string formatting</li>
            <li><strong>Per-Module Loggers:</strong> Each module gets a named logger</li>
            <li><strong>Log Levels:</strong> TRACE, DEBUG, INFO, WARN, ERROR</li>
            <li><strong>Graceful Shutdown:</strong> Queue is drained before closing</li>
          </ul>
        </div>

        <div className="feature-section">
          <h3><Wifi size={24} /> Network Abstraction</h3>
          <p>
            A network layer that abstracts away protocol details while providing powerful traffic shaping.
          </p>
          <ul>
            <li><strong>Packet Batching:</strong> Group multiple packets for efficient transmission</li>
            <li><strong>Priority Queues:</strong> Critical packets (keepalive) go first</li>
            <li><strong>Bandwidth Limiting:</strong> Cap total bytes per second</li>
            <li><strong>Packet Interceptors:</strong> Inspect or modify packets before sending</li>
            <li><strong>Protocol Agnostic:</strong> Ready for official Hytale protocols</li>
          </ul>
        </div>
      </section>

      <section className="rubidium-architecture">
        <h2>Architecture Highlights</h2>
        <div className="architecture-grid">
          <div className="arch-card">
            <Shield size={32} />
            <h3>API-Safe Design</h3>
            <p>No game internals access, no memory injection. Ready for official modding APIs.</p>
          </div>
          <div className="arch-card">
            <Layers size={32} />
            <h3>Clean Interfaces</h3>
            <p>All systems use interfaces with default implementations for easy extension.</p>
          </div>
          <div className="arch-card">
            <Zap size={32} />
            <h3>Zero-Downtime Updates</h3>
            <p>Reload modules and configs without restarting the server.</p>
          </div>
          <div className="arch-card">
            <Target size={32} />
            <h3>Production Ready</h3>
            <p>Error handling, rollback, and graceful degradation built in.</p>
          </div>
        </div>
      </section>

      <section className="license-section">
        <h2><Shield size={28} /> License Agreement</h2>
        <div className="license-content">
          <div className="license-header-block">
            <h3>Rubidium Proprietary License</h3>
            <p>Version 1.0 - 2026 Riley Liang, operating under the alias "DeQuackDealer"</p>
          </div>

          <div className="license-block">
            <h4>1. Definitions</h4>
            <p><strong>"Software"</strong> refers exclusively to Rubidium, including but not limited to its server binaries (e.g., JAR files), plugins, libraries, modules, configuration systems, assets, cosmetic handling systems, performance optimizations, backend integrations, and accompanying documentation.</p>
            <p><strong>"Author"</strong> refers to Riley Liang, operating under the alias DeQuackDealer, and any contributors explicitly authorized by the Author.</p>
            <p><strong>"User"</strong> refers to any individual or legal entity installing, running, or interacting with the Software.</p>
            <p><strong>"Server Operator"</strong> refers to any User running the Software in a server, hosting, or multiplayer environment.</p>
            <p><strong>"Cosmetics"</strong> refers to any skins, accessories, visual overrides, metadata, or cosmetic-related assets made available through Rubidium or its official integrations.</p>
          </div>

          <div className="license-block">
            <h4>2. License Grant (Limited Use Only)</h4>
            <p>The Author grants you a limited, non-exclusive, non-transferable, revocable license to:</p>
            <ul>
              <li>Install and run the Software for personal or server use</li>
              <li>Use the Software only in its original, unmodified form</li>
              <li>Access features provided through official distribution channels</li>
            </ul>
            <p>This license does not grant ownership of the Software or any component thereof.</p>
          </div>

          <div className="license-block">
            <h4>3. Restrictions (Strict)</h4>
            <p>You may not, under any circumstances:</p>
            <ul>
              <li><strong>Redistribute the Software</strong>, in whole or in part (including re-uploading JARs, binaries, installers, or mirrors)</li>
              <li><strong>Sell, sublicense, rent, lease, host as a service, bundle, or otherwise monetize</strong> the Software or access to it (including paid hosting plans, modpacks, or service offerings) unless explicitly authorized in writing by the Author</li>
              <li><strong>Modify, fork, reverse-engineer, decompile, disassemble, or create derivative works</strong> of the Software</li>
              <li><strong>Remove, bypass, or interfere with technical protection measures</strong>, including but not limited to: authentication systems, license or entitlement checks, cosmetic validation, feature gating, server-side or client-side integrity checks</li>
              <li><strong>Rebrand, misrepresent, or claim ownership</strong> of the Software or any component</li>
              <li><strong>Use the Software in violation</strong> of the Hytale End User License Agreement, Terms of Service, or Community Guidelines</li>
            </ul>
            <p className="warning-text">Any violation of this section results in immediate and automatic termination of this license.</p>
          </div>

          <div className="license-block">
            <h4>4. Server Usage Clarification</h4>
            <p>Server Operators are permitted to run Rubidium on their servers.</p>
            <p>Server Operators do not gain the right to:</p>
            <ul>
              <li>Redistribute Rubidium or its components</li>
              <li>Bundle Rubidium with modpacks, installers, or launchers</li>
              <li>Include Rubidium as part of paid hosting services or subscription offerings</li>
            </ul>
            <p>Any server using Rubidium must:</p>
            <ul>
              <li>Use an official, unmodified build</li>
              <li>Refrain from altering core binaries or modules</li>
              <li>Not impersonate or claim affiliation with official Rubidium infrastructure</li>
            </ul>
          </div>

          <div className="license-block">
            <h4>5. Cosmetics & Digital Items</h4>
            <p>All Cosmetics are licensed, not sold. Cosmetics remain the intellectual property of the Author and/or their respective creators.</p>
            <p>Users are granted a revocable, non-transferable right to display Cosmetics:</p>
            <ul>
              <li>On servers running Rubidium</li>
              <li>In environments explicitly supported by Rubidium</li>
            </ul>
            <p>Users may not:</p>
            <ul>
              <li>Extract, rip, or datamine cosmetic assets</li>
              <li>Resell, trade, or transfer cosmetics</li>
              <li>Recreate, clone, or port cosmetics to other platforms or systems</li>
            </ul>
            <p>The Author reserves the right to remove or disable Cosmetics, revoke cosmetic access, and modify cosmetic behavior, availability, or validation at any time.</p>
          </div>

          <div className="license-block">
            <h4>6. Online Services & Connectivity</h4>
            <p>Certain features of the Software may require online connectivity, authentication, or backend/verification services.</p>
            <p>The Author may, at their discretion:</p>
            <ul>
              <li>Modify, suspend, or discontinue online services</li>
              <li>Enforce rate limits or access restrictions</li>
              <li>Disable features in response to abuse or license violations</li>
            </ul>
            <p>There is no guarantee of uptime, continuity, or feature permanence.</p>
          </div>

          <div className="license-block">
            <h4>7. Termination</h4>
            <p>This license remains in effect until terminated. It terminates immediately and automatically if:</p>
            <ul>
              <li>Any provision of this license is violated</li>
              <li>Redistribution or monetization is attempted</li>
              <li>Technical protection or integrity systems are interfered with</li>
            </ul>
            <p>Upon termination, you must immediately cease all use of the Software and delete all copies in your possession or control.</p>
          </div>

          <div className="license-block">
            <h4>8. Disclaimer of Warranty</h4>
            <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. The Author expressly disclaims all warranties, including but not limited to merchantability, fitness for a particular purpose, and non-infringement. Use of the Software is entirely at your own risk.</p>
          </div>

          <div className="license-block">
            <h4>9. Limitation of Liability</h4>
            <p>To the maximum extent permitted by applicable law, the Author shall not be liable for data loss, server instability or downtime, account issues, or direct, indirect, incidental, or consequential damages arising from the use or inability to use the Software.</p>
          </div>

          <div className="license-block">
            <h4>10. Governing Law</h4>
            <p>This license shall be governed by and interpreted under the laws of the jurisdiction determined by the Author, without regard to conflict of law principles.</p>
          </div>

          <div className="license-block">
            <h4>11. No Implied Rights</h4>
            <p>All rights not explicitly granted in this license are reserved by the Author.</p>
          </div>

          <div className="license-block">
            <h4>12. Contact & Permissions</h4>
            <p>For inquiries regarding redistribution rights, commercial use, integrations, or partnerships, explicit written permission from the Author is required.</p>
          </div>

          <div className="license-summary">
            <h4>TL;DR (Non-binding Summary)</h4>
            <ul>
              <li><Check size={16} /> You can run Rubidium</li>
              <li><X size={16} /> You cannot redistribute it</li>
              <li><X size={16} /> You cannot fork or modify it</li>
              <li><X size={16} /> You cannot resell it or bundle it</li>
              <li><Shield size={16} /> Cosmetics are licensed, not owned</li>
            </ul>
          </div>
        </div>
      </section>

      <section className="back-link">
        <Link to="/rubidium" className="btn btn-secondary">
          <ChevronLeft size={18} /> Back to Rubidium
        </Link>
      </section>
    </div>
  );
}

interface MarketplaceItem {
  id: string;
  name: string;
  description: string;
  category: 'mod' | 'plugin' | 'skin' | 'cosmetic' | 'texture' | 'emote';
  author: { id: string; username: string; display_name?: string };
  price: number;
  downloads: number;
  likes: number;
  tags: string[];
  thumbnail_url?: string;
  created_at: string;
  is_featured?: boolean;
}

function Marketplace() {
  const [items, setItems] = useState<MarketplaceItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [category, setCategory] = useState<string>('all');
  const [priceFilter, setPriceFilter] = useState<string>('all');
  const [sortBy, setSortBy] = useState<string>('popular');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const { user } = useAuth();

  useEffect(() => {
    fetchItems();
  }, [category, priceFilter, sortBy]);

  const fetchItems = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (category !== 'all') params.append('category', category);
      if (priceFilter === 'free') params.append('price', 'free');
      if (priceFilter === 'paid') params.append('price', 'paid');
      params.append('sort', sortBy);
      if (searchQuery) params.append('q', searchQuery);

      const res = await fetch(`${API_URL}/api/v1/marketplace/items?${params}`);
      const data = await res.json();
      if (data.success) {
        setItems(data.data.items);
      }
    } catch (e) {
      setItems(getMockItems());
    }
    setLoading(false);
  };

  const getMockItems = (): MarketplaceItem[] => [
    { id: '1', name: 'Crystal Armor Set', description: 'Stunning crystal armor cosmetic with animated glow effects', category: 'cosmetic', author: { id: '1', username: 'CrystalCreator', display_name: 'Crystal Creator' }, price: 0, downloads: 15420, likes: 892, tags: ['armor', 'crystal', 'glow', 'animated'], is_featured: true, created_at: '2025-12-01' },
    { id: '2', name: 'Dragon Wings', description: 'Majestic dragon wings that flutter as you move', category: 'cosmetic', author: { id: '2', username: 'WingMaster' }, price: 2.99, downloads: 8340, likes: 654, tags: ['wings', 'dragon', 'animated', 'premium'], is_featured: true, created_at: '2025-11-28' },
    { id: '3', name: 'Performance Plus', description: 'Client-side optimization mod for better FPS', category: 'mod', author: { id: '3', username: 'OptimizeGuy' }, price: 0, downloads: 45600, likes: 2341, tags: ['performance', 'fps', 'optimization'], created_at: '2025-11-15' },
    { id: '4', name: 'Neon Skin Pack', description: 'Collection of 10 vibrant neon-themed character skins', category: 'skin', author: { id: '4', username: 'NeonArtist' }, price: 1.99, downloads: 12300, likes: 876, tags: ['neon', 'skin', 'pack', 'colorful'], created_at: '2025-11-20' },
    { id: '5', name: 'Server Tools Pro', description: 'Essential admin tools for Rubidium servers', category: 'plugin', author: { id: '5', username: 'ServerDev' }, price: 4.99, downloads: 3200, likes: 234, tags: ['admin', 'tools', 'server', 'management'], created_at: '2025-11-10' },
    { id: '6', name: 'Fantasy Texture Pack', description: 'High-res fantasy-themed textures for immersive gameplay', category: 'texture', author: { id: '6', username: 'TextureWizard' }, price: 0, downloads: 28900, likes: 1567, tags: ['texture', 'fantasy', 'hd', 'immersive'], created_at: '2025-10-25' },
    { id: '7', name: 'Particle Effects Plus', description: 'Beautiful custom particle effects for spells and abilities', category: 'cosmetic', author: { id: '7', username: 'ParticleKing' }, price: 1.49, downloads: 9870, likes: 723, tags: ['particles', 'effects', 'magic', 'spells'], created_at: '2025-11-05' },
    { id: '8', name: 'Medieval Knight Skin', description: 'Detailed medieval knight armor skin with variants', category: 'skin', author: { id: '8', username: 'HistorySkin' }, price: 0, downloads: 18700, likes: 1234, tags: ['medieval', 'knight', 'armor', 'historical'], created_at: '2025-10-30' },
    { id: '9', name: 'Victory Dance', description: 'Celebrate your wins with this flashy victory dance emote', category: 'emote', author: { id: '9', username: 'DanceMaster' }, price: 0.99, downloads: 22450, likes: 1876, tags: ['dance', 'victory', 'celebration', 'animated'], is_featured: true, created_at: '2025-12-05' },
    { id: '10', name: 'Wave Hello', description: 'Friendly wave greeting emote for making new friends', category: 'emote', author: { id: '10', username: 'FriendlyGamer' }, price: 0, downloads: 31200, likes: 2543, tags: ['wave', 'greeting', 'friendly', 'social'], created_at: '2025-11-22' },
  ];

  const filteredItems = items.filter(item => {
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      return item.name.toLowerCase().includes(query) || 
             item.description.toLowerCase().includes(query) ||
             item.tags.some(t => t.toLowerCase().includes(query));
    }
    return true;
  });

  return (
    <div className="page marketplace-page">
      <section className="marketplace-header">
        <h1><Sparkles size={36} /> Marketplace</h1>
        <p className="marketplace-subtitle">Discover mods, plugins, skins, cosmetics, and emotes created by the community</p>
        <p className="cosmetic-notice">
          <Eye size={16} /> Your cosmetics are visible to all Yellow Tale users!
        </p>
      </section>

      <section className="marketplace-controls">
        <div className="search-bar">
          <Search size={20} />
          <input 
            type="text" 
            placeholder="Search mods, skins, emotes..." 
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && fetchItems()}
          />
        </div>

        <div className="filter-controls">
          <div className="filter-group">
            <label><Filter size={16} /> Category</label>
            <select value={category} onChange={e => setCategory(e.target.value)}>
              <option value="all">All Categories</option>
              <option value="mod">Mods</option>
              <option value="plugin">Plugins</option>
              <option value="skin">Skins</option>
              <option value="cosmetic">Cosmetics</option>
              <option value="texture">Textures</option>
              <option value="emote">Emotes</option>
            </select>
          </div>

          <div className="filter-group">
            <label><DollarSign size={16} /> Price</label>
            <select value={priceFilter} onChange={e => setPriceFilter(e.target.value)}>
              <option value="all">All Prices</option>
              <option value="free">Free</option>
              <option value="paid">Paid</option>
            </select>
          </div>

          <div className="filter-group">
            <label><BarChart3 size={16} /> Sort By</label>
            <select value={sortBy} onChange={e => setSortBy(e.target.value)}>
              <option value="popular">Most Popular</option>
              <option value="downloads">Most Downloads</option>
              <option value="newest">Newest</option>
              <option value="price_low">Price: Low to High</option>
              <option value="price_high">Price: High to Low</option>
            </select>
          </div>

          <div className="view-toggle">
            <button className={viewMode === 'grid' ? 'active' : ''} onClick={() => setViewMode('grid')}>
              <Grid size={18} />
            </button>
            <button className={viewMode === 'list' ? 'active' : ''} onClick={() => setViewMode('list')}>
              <List size={18} />
            </button>
          </div>
        </div>

        {user && (
          <Link to="/marketplace/upload" className="btn btn-primary upload-btn">
            <Upload size={18} /> Upload Content
          </Link>
        )}
      </section>

      {loading ? (
        <div className="loading-state">
          <div className="loading-spinner large"></div>
          <p>Loading marketplace...</p>
        </div>
      ) : (
        <>
          {filteredItems.some(i => i.is_featured) && (
            <section className="featured-section">
              <h2><Star size={24} /> Featured</h2>
              <div className="featured-grid">
                {filteredItems.filter(i => i.is_featured).map(item => (
                  <MarketplaceCard key={item.id} item={item} featured />
                ))}
              </div>
            </section>
          )}

          <section className="marketplace-grid-section">
            <h2>All Items ({filteredItems.length})</h2>
            <div className={`marketplace-items ${viewMode}`}>
              {filteredItems.map(item => (
                <MarketplaceCard key={item.id} item={item} listView={viewMode === 'list'} />
              ))}
            </div>
          </section>

          {filteredItems.length === 0 && (
            <div className="empty-marketplace">
              <Package size={64} />
              <h3>No items found</h3>
              <p>Try adjusting your filters or search query</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function MarketplaceCard({ item, featured = false, listView = false }: { item: MarketplaceItem, featured?: boolean, listView?: boolean }) {
  const getCategoryIcon = (cat: string) => {
    switch(cat) {
      case 'mod': return <Package size={14} />;
      case 'plugin': return <Server size={14} />;
      case 'skin': return <User size={14} />;
      case 'cosmetic': return <Sparkles size={14} />;
      case 'texture': return <Palette size={14} />;
      case 'emote': return <Smile size={14} />;
      default: return <Package size={14} />;
    }
  };

  const getCategoryColor = (cat: string) => {
    switch(cat) {
      case 'mod': return '#4ade80';
      case 'plugin': return '#60a5fa';
      case 'skin': return '#f472b6';
      case 'cosmetic': return '#c084fc';
      case 'texture': return '#fbbf24';
      case 'emote': return '#fb923c';
      default: return '#a0a0a0';
    }
  };

  return (
    <div className={`marketplace-card ${featured ? 'featured' : ''} ${listView ? 'list-view' : ''}`}>
      <div className="card-thumbnail">
        {item.thumbnail_url ? (
          <img src={item.thumbnail_url} alt={item.name} />
        ) : (
          <div className="placeholder-thumbnail">
            {getCategoryIcon(item.category)}
          </div>
        )}
        {featured && <span className="featured-badge"><Star size={12} /> Featured</span>}
      </div>
      <div className="card-content">
        <div className="card-header">
          <h3>{item.name}</h3>
          <span className="category-badge" style={{ background: getCategoryColor(item.category) }}>
            {getCategoryIcon(item.category)} {item.category}
          </span>
        </div>
        <p className="card-description">{item.description}</p>
        <div className="card-author">
          by <span>{item.author.display_name || item.author.username}</span>
        </div>
        <div className="card-tags">
          {item.tags.slice(0, 3).map(tag => (
            <span key={tag} className="tag"><Tag size={10} /> {tag}</span>
          ))}
        </div>
        <div className="card-footer">
          <div className="card-stats">
            <span><Download size={14} /> {item.downloads.toLocaleString()}</span>
            <span><Heart size={14} /> {item.likes.toLocaleString()}</span>
          </div>
          <div className="card-price">
            {item.price === 0 ? (
              <span className="free">Free</span>
            ) : (
              <span className="paid">${item.price.toFixed(2)}</span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function UploadPage() {
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    category: 'mod',
    price: 0,
    tags: '',
    is_free: true,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!user) navigate('/login');
  }, [user, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await fetch(`${API_URL}/api/v1/marketplace/items`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          token,
          name: formData.name,
          description: formData.description,
          category: formData.category,
          price: formData.is_free ? 0 : formData.price,
          tags: formData.tags.split(',').map(t => t.trim()).filter(Boolean),
        }),
      });
      const data = await res.json();
      if (data.success) {
        navigate('/marketplace');
      } else {
        setError(data.error || 'Failed to upload');
      }
    } catch (e) {
      setError('Failed to upload item');
    }
    setLoading(false);
  };

  if (!user) return null;

  return (
    <div className="page">
      <section className="upload-section">
        <h1><Upload size={32} /> Upload to Marketplace</h1>
        <p className="upload-subtitle">Share your creation with the Yellow Tale community</p>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit} className="upload-form">
          <div className="form-group">
            <label>Name</label>
            <input
              type="text"
              value={formData.name}
              onChange={e => setFormData({ ...formData, name: e.target.value })}
              placeholder="My Amazing Creation"
              required
              maxLength={100}
            />
          </div>

          <div className="form-group">
            <label>Description</label>
            <textarea
              value={formData.description}
              onChange={e => setFormData({ ...formData, description: e.target.value })}
              placeholder="Describe what your creation does..."
              required
              maxLength={2000}
              rows={4}
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Category</label>
              <select
                value={formData.category}
                onChange={e => setFormData({ ...formData, category: e.target.value })}
              >
                <option value="mod">Mod</option>
                <option value="plugin">Plugin</option>
                <option value="skin">Skin</option>
                <option value="cosmetic">Cosmetic</option>
                <option value="texture">Texture Pack</option>
                <option value="emote">Emote</option>
              </select>
            </div>

            <div className="form-group">
              <label>Pricing</label>
              <div className="pricing-toggle">
                <button
                  type="button"
                  className={formData.is_free ? 'active' : ''}
                  onClick={() => setFormData({ ...formData, is_free: true, price: 0 })}
                >
                  Free
                </button>
                <button
                  type="button"
                  className={!formData.is_free ? 'active' : ''}
                  onClick={() => setFormData({ ...formData, is_free: false })}
                >
                  Paid
                </button>
              </div>
            </div>
          </div>

          {!formData.is_free && (
            <div className="form-group">
              <label>Price (USD)</label>
              <input
                type="number"
                value={formData.price}
                onChange={e => setFormData({ ...formData, price: parseFloat(e.target.value) || 0 })}
                min={0.99}
                max={99.99}
                step={0.01}
                placeholder="2.99"
              />
              <p className="form-hint">Minimum $0.99, you receive 70% of sales</p>
            </div>
          )}

          <div className="form-group">
            <label>Tags (comma separated)</label>
            <input
              type="text"
              value={formData.tags}
              onChange={e => setFormData({ ...formData, tags: e.target.value })}
              placeholder="armor, cosmetic, animated, fantasy"
              maxLength={200}
            />
          </div>

          <div className="form-group">
            <label>Upload Files</label>
            <div className="file-upload-zone">
              <Upload size={32} />
              <p>Drag & drop your files here or click to browse</p>
              <p className="form-hint">Supported: .zip, .jar, .png, .json (max 50MB)</p>
            </div>
          </div>

          <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
            {loading ? <span className="loading-spinner"></span> : <><Upload size={18} /> Publish</>}
          </button>
        </form>
      </section>
    </div>
  );
}

function DownloadPage() {
  return (
    <div className="page">
      <section className="download-section">
        <h1>Download Yellow Tale</h1>
        <p className="download-subtitle">Get the launcher for your platform</p>
        
        <div className="download-grid">
          <div className="download-card">
            <div className="download-icon windows">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="currentColor">
                <path d="M0 3.449L9.75 2.1v9.451H0m10.949-9.602L24 0v11.4H10.949M0 12.6h9.75v9.451L0 20.699M10.949 12.6H24V24l-12.9-1.801"/>
              </svg>
            </div>
            <h3>Windows</h3>
            <p>Windows 10/11 (64-bit)</p>
            <div className="download-buttons">
              <a href="#" className="btn btn-primary">
                <Download size={18} /> Download .exe
              </a>
              <a href="#" className="btn btn-outline btn-small">MSI Installer</a>
            </div>
          </div>
          <div className="download-card">
            <div className="download-icon linux">
              <Server size={48} />
            </div>
            <h3>Linux</h3>
            <p>Ubuntu 22.04+, Fedora 38+</p>
            <div className="download-buttons">
              <a href="#" className="btn btn-primary">
                <Download size={18} /> Download .AppImage
              </a>
              <a href="#" className="btn btn-outline btn-small">.deb Package</a>
            </div>
          </div>
          <div className="download-card">
            <div className="download-icon macos">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="currentColor">
                <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
              </svg>
            </div>
            <h3>macOS</h3>
            <p>macOS 12+ (Apple Silicon & Intel)</p>
            <div className="download-buttons">
              <a href="#" className="btn btn-primary">
                <Download size={18} /> Download .dmg
              </a>
            </div>
          </div>
        </div>

        <div className="download-info">
          <h2>System Requirements</h2>
          <div className="requirements-grid">
            <div className="requirement-card">
              <h4>Minimum</h4>
              <ul>
                <li>64-bit OS</li>
                <li>4 GB RAM</li>
                <li>500 MB disk space</li>
                <li>Internet connection</li>
              </ul>
            </div>
            <div className="requirement-card">
              <h4>Recommended</h4>
              <ul>
                <li>Windows 11 / Ubuntu 22.04 / macOS 13</li>
                <li>8 GB RAM</li>
                <li>SSD with 2 GB free space</li>
                <li>Stable broadband connection</li>
              </ul>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

function FeaturesPage() {
  return (
    <div className="page">
      <section className="features-page">
        <h1>Features</h1>
        
        <div className="feature-section">
          <h2>User Accounts & Friends</h2>
          <ul>
            <li><Check size={16} /> Create your account and personalize your profile</li>
            <li><Check size={16} /> Add friends and see their online status</li>
            <li><Check size={16} /> Send game invites with one click</li>
            <li><Check size={16} /> Block unwanted users</li>
          </ul>
        </div>

        <div className="feature-section">
          <h2>Mod Profiles</h2>
          <ul>
            <li><Check size={16} /> Create unlimited mod configurations</li>
            <li><Check size={16} /> Switch between profiles with one click</li>
            <li><Check size={16} /> Share profiles with friends</li>
            <li><Check size={16} /> Automatic mod dependency resolution</li>
          </ul>
        </div>

        <div className="feature-section">
          <h2>Performance Optimization</h2>
          <ul>
            <li><Check size={16} /> Customize RAM allocation for optimal performance</li>
            <li><Check size={16} /> Fine-tune Java arguments for your system</li>
            <li><Check size={16} /> GC preset selection (Low Latency, High Throughput, etc.)</li>
            <li><Check size={16} /> VSync and FPS limit controls</li>
          </ul>
        </div>

        <div className="feature-section">
          <h2>Game Statistics</h2>
          <ul>
            <li><Check size={16} /> Track your total playtime across sessions</li>
            <li><Check size={16} /> See your session history</li>
            <li><Check size={16} /> Achievement tracking (when available)</li>
            <li><Check size={16} /> Exportable stats for sharing</li>
          </ul>
        </div>

        <div className="feature-section">
          <h2>Rubidium Server Integration</h2>
          <ul>
            <li><Check size={16} /> Enhanced server-side plugin system</li>
            <li><Check size={16} /> Advanced tick optimization via RPAL</li>
            <li><Check size={16} /> Real-time performance monitoring</li>
            <li><Check size={16} /> Hot-reloadable plugins (Premium)</li>
          </ul>
        </div>

        <div className="feature-section">
          <h2>Marketplace</h2>
          <ul>
            <li><Check size={16} /> Browse community-created mods and plugins</li>
            <li><Check size={16} /> Download and apply skins and cosmetics</li>
            <li><Check size={16} /> Your cosmetics visible to all Yellow Tale users</li>
            <li><Check size={16} /> Sell your creations or share them for free</li>
          </ul>
        </div>
      </section>
    </div>
  );
}

function PremiumPage() {
  return (
    <div className="page">
      <section className="premium-section">
        <h1><Crown size={32} /> Yellow Tale Premium</h1>
        <p className="premium-subtitle">Unlock the full potential of your Hytale experience</p>

        <div className="pricing-grid">
          <div className="pricing-card">
            <h2>Free</h2>
            <div className="price">$0<span>/forever</span></div>
            <ul className="feature-list">
              <li><Check size={16} /> Core launcher functionality</li>
              <li><Check size={16} /> 100 friends limit</li>
              <li><Check size={16} /> 3 mod profiles</li>
              <li><Check size={16} /> Basic performance settings</li>
              <li><Check size={16} /> Standard relay servers</li>
              <li><Check size={16} /> Marketplace access</li>
            </ul>
            <Link to="/login" className="btn btn-secondary">Get Started</Link>
          </div>

          <div className="pricing-card featured">
            <div className="featured-badge">Most Popular</div>
            <h2>Premium Launcher</h2>
            <div className="price">$0.99<span>/month</span></div>
            <ul className="feature-list">
              <li><Check size={16} /> Everything in Free</li>
              <li><Check size={16} /> 500 friends limit</li>
              <li><Check size={16} /> Unlimited mod profiles</li>
              <li><Check size={16} /> Advanced diagnostics</li>
              <li><Check size={16} /> Smart JVM tuning</li>
              <li><Check size={16} /> Profile sync across devices</li>
              <li><Check size={16} /> Priority relay servers</li>
              <li><Check size={16} /> Early access features</li>
              <li><Check size={16} /> Premium marketplace badge</li>
            </ul>
            <Link to="/login" className="btn btn-primary">Upgrade Now</Link>
          </div>

          <div className="pricing-card">
            <h2>Premium Rubidium</h2>
            <div className="price">$1.99<span>/year</span></div>
            <ul className="feature-list">
              <li><Check size={16} /> Advanced tick optimization</li>
              <li><Check size={16} /> Plugin CPU budgets</li>
              <li><Check size={16} /> Hot-reload safety nets</li>
              <li><Check size={16} /> Priority task scheduling</li>
              <li><Check size={16} /> Server analytics dashboard</li>
              <li><Check size={16} /> Priority support</li>
            </ul>
            <Link to="/login" className="btn btn-secondary">For Server Owners</Link>
          </div>
        </div>

        <div className="premium-note">
          <p>No pay-to-win features. All premium benefits are infrastructure and convenience only.</p>
        </div>
      </section>
    </div>
  );
}

function LoginPage() {
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, signup, user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (user) navigate('/dashboard');
  }, [user, navigate]);

  const validatePassword = (pwd: string): string[] => {
    const errors: string[] = [];
    if (pwd.length < 8) errors.push('At least 8 characters');
    if (!/[A-Z]/.test(pwd)) errors.push('One uppercase letter');
    if (!/[a-z]/.test(pwd)) errors.push('One lowercase letter');
    if (!/[0-9]/.test(pwd)) errors.push('One number');
    return errors;
  };

  const passwordErrors = mode === 'signup' ? validatePassword(password) : [];
  const passwordsMatch = password === confirmPassword;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (mode === 'signup') {
      if (passwordErrors.length > 0) {
        setError('Password does not meet requirements');
        return;
      }
      if (!passwordsMatch) {
        setError('Passwords do not match');
        return;
      }
    }

    setLoading(true);
    try {
      if (mode === 'login') {
        await login(username, password);
      } else {
        await signup(username, email, password);
      }
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.message || 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page auth-page">
      <section className="auth-section">
        <div className="auth-card">
          <div className="auth-header">
            <Gamepad2 size={48} />
            <h1>{mode === 'login' ? 'Welcome Back' : 'Join Yellow Tale'}</h1>
            <p>{mode === 'login' ? 'Sign in to your account' : 'Create your free account'}</p>
          </div>
          
          {error && <div className="error-message">{error}</div>}
          
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label><User size={16} /> Username</label>
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                placeholder="Enter your username"
                required
                minLength={3}
                maxLength={32}
              />
            </div>
            
            {mode === 'signup' && (
              <div className="form-group">
                <label><Mail size={16} /> Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  placeholder="Enter your email"
                  required
                />
              </div>
            )}
            
            <div className="form-group">
              <label><Lock size={16} /> Password</label>
              <div className="password-input">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  required
                  minLength={8}
                />
                <button 
                  type="button" 
                  className="password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {mode === 'signup' && password && (
                <div className="password-requirements">
                  {['At least 8 characters', 'One uppercase letter', 'One lowercase letter', 'One number'].map(req => (
                    <span key={req} className={passwordErrors.includes(req) ? 'invalid' : 'valid'}>
                      <Check size={12} /> {req}
                    </span>
                  ))}
                </div>
              )}
            </div>

            {mode === 'signup' && (
              <div className="form-group">
                <label><Lock size={16} /> Confirm Password</label>
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={e => setConfirmPassword(e.target.value)}
                  placeholder="Confirm your password"
                  required
                />
                {confirmPassword && !passwordsMatch && (
                  <span className="field-error">Passwords do not match</span>
                )}
              </div>
            )}
            
            <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
              {loading ? (
                <span className="loading-spinner"></span>
              ) : mode === 'login' ? (
                <>
                  <LogIn size={18} /> Sign In
                </>
              ) : (
                <>
                  <UserPlus size={18} /> Create Account
                </>
              )}
            </button>
          </form>
          
          <div className="auth-divider">
            <span>or</span>
          </div>

          <p className="auth-switch">
            {mode === 'login' ? (
              <>Don't have an account? <button onClick={() => setMode('signup')}>Sign up for free</button></>
            ) : (
              <>Already have an account? <button onClick={() => setMode('login')}>Sign in</button></>
            )}
          </p>
        </div>
      </section>
    </div>
  );
}

function Dashboard() {
  const { user, token, logout } = useAuth();
  const [friends, setFriends] = useState<any[]>([]);
  const [pending, setPending] = useState<{ incoming: any[], outgoing: any[] }>({ incoming: [], outgoing: [] });
  const [stats, setStats] = useState<any>(null);
  const [subscription, setSubscription] = useState<any>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }

    fetch(`${API_URL}/api/v1/friends`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token }),
    })
      .then(r => r.json())
      .then(data => { if (data.success) setFriends(data.data.friends); })
      .catch(() => {});

    fetch(`${API_URL}/api/v1/friends/pending`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token }),
    })
      .then(r => r.json())
      .then(data => { if (data.success) setPending(data.data); })
      .catch(() => {});

    fetch(`${API_URL}/api/v1/stats`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token }),
    })
      .then(r => r.json())
      .then(data => { if (data.success) setStats(data.data); })
      .catch(() => {});

    fetch(`${API_URL}/api/v1/subscription`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token }),
    })
      .then(r => r.json())
      .then(data => { if (data.success) setSubscription(data.data); })
      .catch(() => {});
  }, [user, token, navigate]);

  if (!user) return null;

  const formatPlaytime = (minutes: number) => {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return hours > 0 ? `${hours}h ${mins}m` : `${mins}m`;
  };

  return (
    <div className="page">
      <section className="dashboard">
        <div className="dashboard-header">
          <div className="user-info">
            <div className="user-avatar">
              {user.username[0].toUpperCase()}
            </div>
            <div>
              <h1>{user.display_name || user.username}</h1>
              <p>@{user.username}</p>
              {subscription && (
                <span className={`tier-badge ${subscription.tier}`}>
                  {subscription.tier === 'premium' ? <Crown size={14} /> : null}
                  {subscription.tier.charAt(0).toUpperCase() + subscription.tier.slice(1)}
                </span>
              )}
            </div>
          </div>
        </div>

        <div className="dashboard-grid">
          <div className="dashboard-card">
            <h2><BarChart3 size={20} /> Game Statistics</h2>
            {stats ? (
              <div className="stats-grid">
                <div className="stat-item">
                  <Clock size={24} />
                  <div>
                    <span className="stat-value">{formatPlaytime(stats.total_playtime_minutes)}</span>
                    <span className="stat-label">Total Playtime</span>
                  </div>
                </div>
                <div className="stat-item">
                  <Gamepad2 size={24} />
                  <div>
                    <span className="stat-value">{stats.total_sessions}</span>
                    <span className="stat-label">Sessions</span>
                  </div>
                </div>
              </div>
            ) : (
              <p className="empty-state">No stats yet. Start playing to track your progress!</p>
            )}
          </div>

          <div className="dashboard-card">
            <h2><Users size={20} /> Friends ({friends.length})</h2>
            {friends.length > 0 ? (
              <ul className="friends-list">
                {friends.map((f: any) => (
                  <li key={f.id}>
                    <div className="friend-avatar">{(f.display_name || f.username)[0].toUpperCase()}</div>
                    <span>{f.display_name || f.username}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="empty-state">No friends yet. Add some from the launcher!</p>
            )}
          </div>

          <div className="dashboard-card">
            <h2>Friend Requests</h2>
            {pending.incoming.length > 0 ? (
              <ul className="friends-list">
                {pending.incoming.map((f: any) => (
                  <li key={f.id}>
                    <div className="friend-avatar">{f.username[0].toUpperCase()}</div>
                    <span>{f.username}</span>
                    <span className="badge">Pending</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="empty-state">No pending requests</p>
            )}
          </div>

          <div className="dashboard-card">
            <h2>Quick Actions</h2>
            <div className="action-buttons">
              <Link to="/download" className="btn btn-primary">
                <Download size={18} /> Download Launcher
              </Link>
              <Link to="/marketplace" className="btn btn-secondary">
                <Sparkles size={18} /> Browse Marketplace
              </Link>
              {subscription?.tier === 'free' && (
                <Link to="/premium" className="btn btn-premium">
                  <Crown size={18} /> Upgrade to Premium
                </Link>
              )}
              <button className="btn btn-outline" onClick={logout}>
                <LogOut size={18} /> Logout
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

function AdminDashboard() {
  const [adminToken, setAdminToken] = useState<string | null>(localStorage.getItem('admin_token'));
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [items, setItems] = useState<any[]>([]);
  const [escrows, setEscrows] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState<'items' | 'escrow' | 'create'>('items');
  const [newItem, setNewItem] = useState({
    name: '', description: '', category: 'cosmetic', price: 0,
    tags: '', thumbnail_url: '', file_url: '', is_featured: false
  });

  const adminLogin = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${API_URL}/api/v1/admin/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      const data = await res.json();
      if (!data.success) throw new Error(data.error);
      localStorage.setItem('admin_token', data.data.admin_token);
      setAdminToken(data.data.admin_token);
    } catch (e: any) {
      setError(e.message || 'Login failed');
    }
    setLoading(false);
  };

  const adminLogout = () => {
    localStorage.removeItem('admin_token');
    setAdminToken(null);
  };

  const fetchItems = async () => {
    if (!adminToken) return;
    try {
      const res = await fetch(`${API_URL}/api/v1/admin/marketplace/items`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ admin_token: adminToken }),
      });
      const data = await res.json();
      if (data.success) setItems(data.data.items);
    } catch (e) {}
  };

  const fetchEscrows = async () => {
    if (!adminToken) return;
    try {
      const res = await fetch(`${API_URL}/api/v1/admin/escrow`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ admin_token: adminToken }),
      });
      const data = await res.json();
      if (data.success) setEscrows(data.data.escrows);
    } catch (e) {}
  };

  const createItem = async () => {
    if (!adminToken) return;
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/v1/admin/marketplace/items`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          admin_token: adminToken,
          name: newItem.name,
          description: newItem.description,
          category: newItem.category,
          price: newItem.price,
          tags: newItem.tags.split(',').map(t => t.trim()).filter(t => t),
          thumbnail_url: newItem.thumbnail_url || null,
          file_url: newItem.file_url || null,
          is_featured: newItem.is_featured,
        }),
      });
      const data = await res.json();
      if (!data.success) throw new Error(data.error);
      setNewItem({ name: '', description: '', category: 'cosmetic', price: 0, tags: '', thumbnail_url: '', file_url: '', is_featured: false });
      fetchItems();
      setActiveTab('items');
    } catch (e: any) {
      setError(e.message);
    }
    setLoading(false);
  };

  const deleteItem = async (id: string) => {
    if (!adminToken || !confirm('Delete this item?')) return;
    try {
      await fetch(`${API_URL}/api/v1/admin/marketplace/items/${id}`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ admin_token: adminToken }),
      });
      fetchItems();
    } catch (e) {}
  };

  const releaseEscrow = async (escrowId: string) => {
    if (!adminToken || !confirm('Release escrow funds to seller?')) return;
    try {
      await fetch(`${API_URL}/api/v1/admin/escrow/release`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ admin_token: adminToken, escrow_id: escrowId }),
      });
      fetchEscrows();
    } catch (e) {}
  };

  useEffect(() => {
    if (adminToken) {
      fetchItems();
      fetchEscrows();
    }
  }, [adminToken]);

  if (!adminToken) {
    return (
      <div className="page admin-page">
        <section className="admin-login">
          <div className="login-card">
            <div className="login-header">
              <ShieldCheck size={48} />
              <h1>Admin Portal</h1>
              <p>Marketplace Administration</p>
            </div>
            {error && <div className="error-message">{error}</div>}
            <div className="form-group">
              <label><User size={16} /> Username</label>
              <input type="text" value={username} onChange={e => setUsername(e.target.value)} placeholder="Admin username" />
            </div>
            <div className="form-group">
              <label><Lock size={16} /> Password</label>
              <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Admin password" onKeyDown={e => e.key === 'Enter' && adminLogin()} />
            </div>
            <button className="btn btn-primary" onClick={adminLogin} disabled={loading}>
              {loading ? 'Logging in...' : 'Login'}
            </button>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="page admin-page">
      <section className="admin-header">
        <div className="admin-title">
          <ShieldCheck size={32} />
          <h1>Admin Dashboard</h1>
        </div>
        <button className="btn btn-outline" onClick={adminLogout}>
          <LogOut size={18} /> Logout
        </button>
      </section>

      <section className="admin-tabs">
        <button className={activeTab === 'items' ? 'active' : ''} onClick={() => setActiveTab('items')}>
          <Package size={18} /> Items ({items.length})
        </button>
        <button className={activeTab === 'escrow' ? 'active' : ''} onClick={() => setActiveTab('escrow')}>
          <Wallet size={18} /> Escrow ({escrows.filter(e => e.status === 'completed').length} pending)
        </button>
        <button className={activeTab === 'create' ? 'active' : ''} onClick={() => setActiveTab('create')}>
          <Upload size={18} /> Create Item
        </button>
      </section>

      {error && <div className="error-message">{error}</div>}

      {activeTab === 'items' && (
        <section className="admin-items">
          <h2>Marketplace Items</h2>
          <div className="admin-table">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Category</th>
                  <th>Price</th>
                  <th>Downloads</th>
                  <th>Featured</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map(item => (
                  <tr key={item.id}>
                    <td>{item.name}</td>
                    <td><span className="badge">{item.category}</span></td>
                    <td>{item.price === 0 ? 'Free' : `$${item.price.toFixed(2)}`}</td>
                    <td>{item.downloads}</td>
                    <td>{item.is_featured ? <Star size={16} /> : '-'}</td>
                    <td>
                      <button className="btn-icon" title="Delete" onClick={() => deleteItem(item.id)}>
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {items.length === 0 && <p className="empty-state">No items yet. Create your first item!</p>}
          </div>
        </section>
      )}

      {activeTab === 'escrow' && (
        <section className="admin-escrow">
          <h2>Escrow Transactions</h2>
          <div className="admin-table">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {escrows.map(escrow => (
                  <tr key={escrow.id}>
                    <td>{escrow.id.slice(0, 8)}...</td>
                    <td>${escrow.amount.toFixed(2)}</td>
                    <td><span className={`badge ${escrow.status}`}>{escrow.status}</span></td>
                    <td>{new Date(escrow.created_at).toLocaleDateString()}</td>
                    <td>
                      {escrow.status === 'completed' && (
                        <button className="btn btn-small" onClick={() => releaseEscrow(escrow.id)}>
                          Release Funds
                        </button>
                      )}
                      {escrow.status === 'released' && <span className="badge success">Released</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {escrows.length === 0 && <p className="empty-state">No escrow transactions yet.</p>}
          </div>
        </section>
      )}

      {activeTab === 'create' && (
        <section className="admin-create">
          <h2>Create New Item</h2>
          <div className="create-form">
            <div className="form-row">
              <div className="form-group">
                <label>Name</label>
                <input type="text" value={newItem.name} onChange={e => setNewItem({...newItem, name: e.target.value})} placeholder="Item name" />
              </div>
              <div className="form-group">
                <label>Category</label>
                <select value={newItem.category} onChange={e => setNewItem({...newItem, category: e.target.value})}>
                  <option value="cosmetic">Cosmetic</option>
                  <option value="skin">Skin</option>
                  <option value="emote">Emote</option>
                  <option value="mod">Mod</option>
                  <option value="plugin">Plugin</option>
                  <option value="texture">Texture</option>
                </select>
              </div>
            </div>
            <div className="form-group">
              <label>Description</label>
              <textarea value={newItem.description} onChange={e => setNewItem({...newItem, description: e.target.value})} placeholder="Item description" rows={3} />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Price (USD, 0 = Free)</label>
                <input type="number" value={newItem.price} onChange={e => setNewItem({...newItem, price: parseFloat(e.target.value) || 0})} min="0" step="0.01" />
              </div>
              <div className="form-group">
                <label>Tags (comma separated)</label>
                <input type="text" value={newItem.tags} onChange={e => setNewItem({...newItem, tags: e.target.value})} placeholder="tag1, tag2, tag3" />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Thumbnail URL</label>
                <input type="text" value={newItem.thumbnail_url} onChange={e => setNewItem({...newItem, thumbnail_url: e.target.value})} placeholder="https://..." />
              </div>
              <div className="form-group">
                <label>File URL (Download)</label>
                <input type="text" value={newItem.file_url} onChange={e => setNewItem({...newItem, file_url: e.target.value})} placeholder="https://..." />
              </div>
            </div>
            <div className="form-group checkbox">
              <input type="checkbox" id="featured" checked={newItem.is_featured} onChange={e => setNewItem({...newItem, is_featured: e.target.checked})} />
              <label htmlFor="featured"><Star size={16} /> Featured Item</label>
            </div>
            <button className="btn btn-primary" onClick={createItem} disabled={loading || !newItem.name || !newItem.description}>
              {loading ? 'Creating...' : 'Create Item'}
            </button>
          </div>
        </section>
      )}
    </div>
  );
}

type LauncherView = 'home' | 'play' | 'servers' | 'friends' | 'mods' | 'cosmetics' | 'shop' | 'stats' | 'settings';

function LauncherPreview() {
  const [view, setView] = useState<LauncherView>('home');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const navigate = useNavigate();

  const viewTitles: Record<LauncherView, string> = {
    home: 'Home',
    play: 'Play',
    servers: 'Server Browser',
    friends: 'Friends',
    mods: 'Mods',
    cosmetics: 'Cosmetics',
    shop: 'Shop',
    stats: 'Statistics',
    settings: 'Settings',
  };

  const mockUser = {
    username: 'DuckPlayer',
    display_name: 'Duck Player',
    premium: true,
  };

  const mockServers = [
    { id: '1', name: 'HytaleCraft', address: 'play.hytalecraft.com', players: 1247, maxPlayers: 2000, ping: 24, gamemode: 'Survival' },
    { id: '2', name: 'Adventure Realm', address: 'adventure.hytale.net', players: 892, maxPlayers: 1500, ping: 45, gamemode: 'Adventure' },
    { id: '3', name: 'Creative World', address: 'creative.hytale.io', players: 523, maxPlayers: 1000, ping: 18, gamemode: 'Creative' },
    { id: '4', name: 'PvP Arena', address: 'pvp.hytale.gg', players: 1893, maxPlayers: 3000, ping: 32, gamemode: 'PvP' },
  ];

  const mockFriends = [
    { id: '1', username: 'QuackMaster', status: 'online', activity: 'Playing on HytaleCraft' },
    { id: '2', username: 'DuckLord', status: 'online', activity: 'In Menu' },
    { id: '3', username: 'FeatherFriend', status: 'away', activity: 'AFK' },
    { id: '4', username: 'PondExplorer', status: 'offline', activity: 'Last seen 2h ago' },
  ];

  const mockMods = [
    { id: '1', name: 'Minimap', description: 'Real-time minimap with waypoints', version: '1.2.0', author: 'YellowTale', enabled: true },
    { id: '2', name: 'Waypoints', description: 'Mark and share locations', version: '1.1.0', author: 'YellowTale', enabled: true },
    { id: '3', name: 'Replay System', description: 'Record and replay gameplay', version: '2.0.0', author: 'YellowTale', enabled: false },
    { id: '4', name: 'Cinema Camera', description: 'Cinematic camera paths', version: '1.0.5', author: 'YellowTale', enabled: false },
    { id: '5', name: 'Performance Boost', description: 'Optimize game performance', version: '1.3.2', author: 'YellowTale', enabled: true },
    { id: '6', name: 'Social Features', description: 'Enhanced party and chat', version: '1.4.0', author: 'YellowTale', enabled: true },
  ];

  const mockCosmetics = [
    { id: '1', name: 'Victory Dance', category: 'emote', rarity: 'rare', equipped: false },
    { id: '2', name: 'Wave Hello', category: 'emote', rarity: 'common', equipped: true },
    { id: '3', name: 'Duck Knight', category: 'skin', rarity: 'legendary', equipped: false },
    { id: '4', name: 'Golden Aura', category: 'aura', rarity: 'epic', equipped: false },
    { id: '5', name: 'Quack Cape', category: 'cape', rarity: 'rare', equipped: true },
  ];

  const mockNews = [
    { id: '1', title: 'Yellow Tale 1.0 Launch!', summary: 'The ultimate Hytale launcher is here with mod support and more.', date: 'Jan 10, 2026' },
    { id: '2', title: 'Rubidium Server Platform', summary: 'New server-side features including plugins and anticheat.', date: 'Jan 8, 2026' },
    { id: '3', title: 'Cosmetics Store Now Open', summary: 'Get exclusive duck-themed cosmetics for your character!', date: 'Jan 5, 2026' },
  ];

  return (
    <div className="launcher">
      <aside className={`launcher-sidebar ${sidebarCollapsed ? 'collapsed' : ''}`}>
        <div className="launcher-logo" onClick={() => navigate('/')}>
          <Gamepad2 size={28} />
          {!sidebarCollapsed && <span>Yellow Tale</span>}
        </div>

        <button className="sidebar-toggle" onClick={() => setSidebarCollapsed(!sidebarCollapsed)}>
          {sidebarCollapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>

        <nav className="launcher-nav">
          <div className="nav-section">
            {!sidebarCollapsed && <div className="nav-section-label">Main</div>}
            <button className={`launcher-nav-item ${view === 'home' ? 'active' : ''}`} onClick={() => setView('home')}>
              <HomeIcon size={22} />
              {!sidebarCollapsed && <span>Home</span>}
            </button>
            <button className={`launcher-nav-item ${view === 'play' ? 'active' : ''}`} onClick={() => setView('play')}>
              <Play size={22} />
              {!sidebarCollapsed && <span>Play</span>}
            </button>
            <button className={`launcher-nav-item ${view === 'servers' ? 'active' : ''}`} onClick={() => setView('servers')}>
              <Server size={22} />
              {!sidebarCollapsed && <span>Servers</span>}
            </button>
          </div>

          <div className="nav-section">
            {!sidebarCollapsed && <div className="nav-section-label">Social</div>}
            <button className={`launcher-nav-item ${view === 'friends' ? 'active' : ''}`} onClick={() => setView('friends')}>
              <Users size={22} />
              {!sidebarCollapsed && <span>Friends</span>}
              <span className="nav-badge">2</span>
            </button>
          </div>

          <div className="nav-section">
            {!sidebarCollapsed && <div className="nav-section-label">Customization</div>}
            <button className={`launcher-nav-item ${view === 'mods' ? 'active' : ''}`} onClick={() => setView('mods')}>
              <Download size={22} />
              {!sidebarCollapsed && <span>Mods</span>}
            </button>
            <button className={`launcher-nav-item ${view === 'cosmetics' ? 'active' : ''}`} onClick={() => setView('cosmetics')}>
              <Sparkles size={22} />
              {!sidebarCollapsed && <span>Cosmetics</span>}
            </button>
            <button className={`launcher-nav-item ${view === 'shop' ? 'active' : ''}`} onClick={() => setView('shop')}>
              <ShoppingBag size={22} />
              {!sidebarCollapsed && <span>Shop</span>}
            </button>
          </div>

          <div className="nav-section">
            {!sidebarCollapsed && <div className="nav-section-label">Profile</div>}
            <button className={`launcher-nav-item ${view === 'stats' ? 'active' : ''}`} onClick={() => setView('stats')}>
              <BarChart3 size={22} />
              {!sidebarCollapsed && <span>Stats</span>}
            </button>
            <button className={`launcher-nav-item ${view === 'settings' ? 'active' : ''}`} onClick={() => setView('settings')}>
              <Settings size={22} />
              {!sidebarCollapsed && <span>Settings</span>}
            </button>
          </div>
        </nav>

        <div className="launcher-user-card">
          <div className="launcher-user-avatar">
            {mockUser.username[0]}
            <span className="status-indicator online" />
          </div>
          {!sidebarCollapsed && (
            <div className="launcher-user-info">
              <div className="launcher-user-name">
                {mockUser.display_name}
                {mockUser.premium && <Crown size={14} className="premium-icon" />}
              </div>
              <div className="launcher-user-status">Online</div>
            </div>
          )}
        </div>
      </aside>

      <div className="launcher-main">
        <header className="launcher-topbar">
          <h1 className="launcher-page-title">{viewTitles[view]}</h1>
          <div className="launcher-topbar-actions">
            <div className="launcher-search">
              <Search size={16} />
              <input type="text" placeholder="Search..." />
            </div>
            <button className="launcher-icon-btn">
              <Bell size={18} />
              <span className="notification-dot" />
            </button>
          </div>
        </header>

        <main className="launcher-content">
          {view === 'home' && (
            <div className="launcher-view home-view">
              <div className="home-hero">
                <div className="hero-content">
                  <h2>Welcome back, {mockUser.display_name}!</h2>
                  <p>Ready for another adventure in Hytale?</p>
                  <button className="btn-launch" onClick={() => setView('play')}>
                    <Play size={20} /> Launch Game
                  </button>
                </div>
                <div className="hero-stats">
                  <div className="stat-item">
                    <Clock size={16} />
                    <span>24h 32m</span>
                    <small>Playtime</small>
                  </div>
                  <div className="stat-item">
                    <Users size={16} />
                    <span>2</span>
                    <small>Online Friends</small>
                  </div>
                </div>
              </div>

              <div className="home-grid">
                <div className="home-section news-section">
                  <h3><Zap size={18} /> Latest News</h3>
                  <div className="news-list">
                    {mockNews.map(news => (
                      <div key={news.id} className="news-item">
                        <div className="news-date">{news.date}</div>
                        <div className="news-title">{news.title}</div>
                        <div className="news-summary">{news.summary}</div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="home-section friends-section">
                  <h3><Users size={18} /> Friends Online</h3>
                  <div className="friends-list">
                    {mockFriends.filter(f => f.status === 'online').map(friend => (
                      <div key={friend.id} className="friend-item">
                        <div className="friend-avatar">{friend.username[0]}</div>
                        <div className="friend-info">
                          <div className="friend-name">{friend.username}</div>
                          <div className="friend-activity">{friend.activity}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}

          {view === 'play' && (
            <div className="launcher-view play-view">
              <div className="play-main">
                <div className="game-preview">
                  <Gamepad2 size={120} />
                  <h2>Hytale</h2>
                  <p>Ready to launch</p>
                </div>
                <button className="btn-launch-large">
                  <Play size={32} /> PLAY
                </button>
              </div>

              <div className="play-settings">
                <div className="setting-group">
                  <h3><Cpu size={18} /> Performance</h3>
                  <div className="setting-row">
                    <label>Allocated RAM</label>
                    <div className="ram-slider">
                      <input type="range" min={2} max={16} defaultValue={8} />
                      <span>8 GB</span>
                    </div>
                  </div>
                  <div className="setting-row">
                    <label>Target FPS</label>
                    <select defaultValue="unlimited">
                      <option value="60">60 FPS</option>
                      <option value="120">120 FPS</option>
                      <option value="144">144 FPS</option>
                      <option value="unlimited">Unlimited</option>
                    </select>
                  </div>
                </div>

                <div className="setting-group">
                  <h3><Layers size={18} /> Mod Profile</h3>
                  <select defaultValue="default">
                    <option value="default">Default Profile</option>
                    <option value="performance">Performance Mode</option>
                    <option value="custom">Custom</option>
                  </select>
                </div>
              </div>
            </div>
          )}

          {view === 'servers' && (
            <div className="launcher-view servers-view">
              <div className="servers-filters">
                <div className="filter-group">
                  <Filter size={16} />
                  <select defaultValue="all">
                    <option value="all">All Gamemodes</option>
                    <option value="survival">Survival</option>
                    <option value="adventure">Adventure</option>
                    <option value="creative">Creative</option>
                    <option value="pvp">PvP</option>
                  </select>
                </div>
                <div className="filter-search">
                  <Search size={16} />
                  <input type="text" placeholder="Search servers..." />
                </div>
              </div>

              <div className="servers-list">
                {mockServers.map(server => (
                  <div key={server.id} className="server-card">
                    <div className="server-icon">
                      <Server size={24} />
                    </div>
                    <div className="server-info">
                      <div className="server-name">{server.name}</div>
                      <div className="server-address">{server.address}</div>
                    </div>
                    <div className="server-gamemode">{server.gamemode}</div>
                    <div className="server-players">
                      <Users size={14} />
                      {server.players}/{server.maxPlayers}
                    </div>
                    <div className={`server-ping ${server.ping < 50 ? 'good' : server.ping < 100 ? 'medium' : 'bad'}`}>
                      <Wifi size={14} />
                      {server.ping}ms
                    </div>
                    <button className="btn-join">Join</button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {view === 'friends' && (
            <div className="launcher-view friends-view">
              <div className="friends-header">
                <div className="friends-tabs">
                  <button className="active">All Friends</button>
                  <button>Online</button>
                  <button>Pending</button>
                </div>
                <button className="btn-add-friend">
                  <UserPlus size={16} /> Add Friend
                </button>
              </div>

              <div className="friends-grid">
                {mockFriends.map(friend => (
                  <div key={friend.id} className={`friend-card ${friend.status}`}>
                    <div className="friend-avatar-large">
                      {friend.username[0]}
                      <span className={`status-dot ${friend.status}`} />
                    </div>
                    <div className="friend-details">
                      <div className="friend-name">{friend.username}</div>
                      <div className="friend-activity">{friend.activity}</div>
                    </div>
                    <div className="friend-actions">
                      <button className="icon-btn"><MessageSquare size={16} /></button>
                      <button className="icon-btn"><Gamepad2 size={16} /></button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {view === 'mods' && (
            <div className="launcher-view mods-view">
              <div className="mods-header">
                <p>Toggle Yellow Tale features to customize your experience</p>
              </div>

              <div className="mods-grid">
                {mockMods.map(mod => (
                  <div key={mod.id} className={`mod-card ${mod.enabled ? 'enabled' : ''}`}>
                    <div className="mod-header">
                      <div className="mod-icon">
                        {mod.name === 'Minimap' && <Map size={20} />}
                        {mod.name === 'Waypoints' && <Target size={20} />}
                        {mod.name === 'Replay System' && <Play size={20} />}
                        {mod.name === 'Cinema Camera' && <Eye size={20} />}
                        {mod.name === 'Performance Boost' && <Zap size={20} />}
                        {mod.name === 'Social Features' && <Users size={20} />}
                      </div>
                      <div className="mod-title">
                        <h3>{mod.name}</h3>
                        <span>v{mod.version}</span>
                      </div>
                      <button className={`mod-toggle ${mod.enabled ? 'active' : ''}`} />
                    </div>
                    <div className="mod-description">{mod.description}</div>
                    <div className="mod-meta">By {mod.author}</div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {view === 'cosmetics' && (
            <div className="launcher-view cosmetics-view">
              <div className="cosmetics-notice">
                <Sparkles size={16} /> Your cosmetics are visible to all Yellow Tale users!
              </div>

              <div className="equipped-section">
                <h3><Heart size={18} /> Equipped Items</h3>
                <div className="equipped-slots">
                  {['Emote 1', 'Emote 2', 'Cape', 'Aura'].map((slot, i) => (
                    <div key={i} className={`equip-slot ${i < 2 ? 'filled' : ''}`}>
                      <span className="slot-label">{slot}</span>
                      {i < 2 ? <Smile size={20} /> : <span className="slot-empty">Empty</span>}
                    </div>
                  ))}
                </div>
              </div>

              <div className="cosmetics-tabs">
                <button className="active"><Smile size={16} /> Emotes</button>
                <button><User size={16} /> Skins</button>
                <button><Sparkles size={16} /> Effects</button>
              </div>

              <div className="cosmetics-grid">
                {mockCosmetics.map(item => (
                  <div key={item.id} className={`cosmetic-card ${item.rarity} ${item.equipped ? 'equipped' : ''}`}>
                    <div className="cosmetic-preview">
                      {item.category === 'emote' ? <Smile size={32} /> : 
                       item.category === 'skin' ? <User size={32} /> : <Sparkles size={32} />}
                    </div>
                    <div className="cosmetic-name">{item.name}</div>
                    <div className={`cosmetic-rarity ${item.rarity}`}>{item.rarity}</div>
                    {item.equipped && <span className="equipped-badge">Equipped</span>}
                  </div>
                ))}
              </div>
            </div>
          )}

          {view === 'shop' && (
            <div className="launcher-view shop-view">
              <div className="shop-header">
                <p>Get new cosmetics and show off your style!</p>
                <div className="coin-balance">
                  <Crown size={18} />
                  <span>1,250 Coins</span>
                </div>
              </div>

              <div className="shop-grid">
                {[
                  { id: '1', name: 'Golden Duck Cape', price: 500, rarity: 'legendary' },
                  { id: '2', name: 'Quack Emote Pack', price: 300, rarity: 'epic' },
                  { id: '3', name: 'Duck Wings', price: 450, rarity: 'epic' },
                  { id: '4', name: 'Waddle Dance', price: 200, rarity: 'rare' },
                  { id: '5', name: 'Pond Aura', price: 350, rarity: 'rare' },
                  { id: '6', name: 'Feather Trail', price: 250, rarity: 'rare' },
                ].map(item => (
                  <div key={item.id} className={`shop-item ${item.rarity}`}>
                    <div className="shop-item-preview">
                      <Sparkles size={48} />
                    </div>
                    <div className="shop-item-name">{item.name}</div>
                    <div className={`cosmetic-rarity ${item.rarity}`}>{item.rarity}</div>
                    <div className="shop-item-price">
                      <Crown size={14} /> {item.price}
                    </div>
                    <button className="btn-purchase">Purchase</button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {view === 'stats' && (
            <div className="launcher-view stats-view">
              <div className="stats-grid">
                <div className="stat-card">
                  <Clock size={24} />
                  <div className="stat-value">24h 32m</div>
                  <div className="stat-label">Total Playtime</div>
                </div>
                <div className="stat-card">
                  <Target size={24} />
                  <div className="stat-value">1,247</div>
                  <div className="stat-label">Blocks Placed</div>
                </div>
                <div className="stat-card">
                  <Award size={24} />
                  <div className="stat-value">12</div>
                  <div className="stat-label">Achievements</div>
                </div>
                <div className="stat-card">
                  <Server size={24} />
                  <div className="stat-value">8</div>
                  <div className="stat-label">Servers Joined</div>
                </div>
              </div>

              <div className="activity-section">
                <h3><TrendingUp size={18} /> Recent Activity</h3>
                <div className="activity-list">
                  <div className="activity-item">
                    <Award size={16} />
                    <span>Unlocked <strong>First Steps</strong> achievement</span>
                    <small>2 hours ago</small>
                  </div>
                  <div className="activity-item">
                    <Server size={16} />
                    <span>Joined <strong>HytaleCraft</strong> server</span>
                    <small>5 hours ago</small>
                  </div>
                  <div className="activity-item">
                    <Target size={16} />
                    <span>Placed 500 blocks milestone!</span>
                    <small>1 day ago</small>
                  </div>
                </div>
              </div>
            </div>
          )}

          {view === 'settings' && (
            <div className="launcher-view settings-view">
              <div className="settings-section">
                <h3><Monitor size={18} /> Display</h3>
                <div className="setting-row">
                  <div className="setting-label">
                    <span>Window Mode</span>
                    <small>Choose how the game is displayed</small>
                  </div>
                  <select defaultValue="windowed">
                    <option value="windowed">Windowed</option>
                    <option value="fullscreen">Fullscreen</option>
                    <option value="borderless">Borderless</option>
                  </select>
                </div>
                <div className="setting-row">
                  <div className="setting-label">
                    <span>VSync</span>
                    <small>Synchronize framerate with monitor</small>
                  </div>
                  <input type="checkbox" defaultChecked />
                </div>
              </div>

              <div className="settings-section">
                <h3><Volume2 size={18} /> Audio</h3>
                <div className="setting-row">
                  <div className="setting-label"><span>Master Volume</span></div>
                  <div className="volume-slider">
                    <input type="range" min={0} max={100} defaultValue={80} />
                    <span>80%</span>
                  </div>
                </div>
              </div>

              <div className="settings-section">
                <h3><HardDrive size={18} /> Game</h3>
                <div className="setting-row">
                  <div className="setting-label">
                    <span>Game Installation Path</span>
                    <small>Where Hytale is installed</small>
                  </div>
                  <div className="path-input">
                    <input type="text" placeholder="Not set" readOnly />
                    <button>Browse</button>
                  </div>
                </div>
              </div>

              <div className="settings-section">
                <h3><Shield size={18} /> Privacy</h3>
                <div className="setting-row">
                  <div className="setting-label">
                    <span>Show Online Status</span>
                    <small>Let friends see when you're online</small>
                  </div>
                  <input type="checkbox" defaultChecked />
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

function DocsLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="docs-page">
      <aside className="docs-sidebar">
        <div className="docs-nav">
          <h3>Documentation</h3>
          <div className="docs-nav-section">
            <h4>Getting Started</h4>
            <Link to="/rubidium/documentation">Overview</Link>
            <Link to="/rubidium/documentation/getting-started">Quick Start</Link>
            <Link to="/rubidium/documentation/installation">Installation</Link>
          </div>
          <div className="docs-nav-section">
            <h4>Core Concepts</h4>
            <Link to="/rubidium/documentation/modules">Modules</Link>
            <Link to="/rubidium/documentation/lifecycle">Lifecycle</Link>
            <Link to="/rubidium/documentation/config">Configuration</Link>
          </div>
          <div className="docs-nav-section">
            <h4>APIs</h4>
            <Link to="/rubidium/documentation/apis">API Overview</Link>
            <Link to="/rubidium/documentation/apis/voice">Voice Chat</Link>
            <Link to="/rubidium/documentation/apis/map">Map Integration</Link>
            <Link to="/rubidium/documentation/apis/ui">UI Framework</Link>
            <Link to="/rubidium/documentation/apis/teleport">Teleportation</Link>
          </div>
          <div className="docs-nav-section">
            <h4>Examples</h4>
            <Link to="/rubidium/documentation/examples">Code Examples</Link>
          </div>
        </div>
      </aside>
      <main className="docs-content">
        {children}
      </main>
    </div>
  );
}

function DocsOverview() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>Rubidium Documentation</h1>
        <p className="docs-intro">
          Welcome to the Rubidium documentation. Rubidium is a production-ready server framework 
          for Hytale that makes building powerful plugins incredibly easy.
        </p>

        <div className="docs-cards">
          <Link to="/rubidium/documentation/getting-started" className="docs-card">
            <Play size={32} />
            <h3>Quick Start</h3>
            <p>Get up and running in 5 minutes</p>
          </Link>
          <Link to="/rubidium/documentation/apis" className="docs-card">
            <Layers size={32} />
            <h3>API Reference</h3>
            <p>Explore all available APIs</p>
          </Link>
          <Link to="/rubidium/documentation/examples" className="docs-card">
            <Package size={32} />
            <h3>Examples</h3>
            <p>Learn from working code</p>
          </Link>
        </div>

        <h2>Why Rubidium?</h2>
        <ul className="docs-list">
          <li><strong>Easy APIs</strong> - Simple, intuitive interfaces that just work</li>
          <li><strong>Hot Reload</strong> - Change code without restarting the server</li>
          <li><strong>Performance</strong> - Built-in optimization and monitoring</li>
          <li><strong>Type Safe</strong> - Full Java 19+ with modern patterns</li>
        </ul>

        <h2>Quick Example</h2>
        <pre className="docs-code">{`public class MyPlugin extends RubidiumModule {
    @Override
    public void onEnable() {
        // Register a simple command
        commands().register("hello", (sender, args) -> {
            sender.sendMessage("Hello from Rubidium!");
        });
        
        // Add voice chat to your server
        VoiceAPI.setPlayerMuted(playerId, false);
        
        // Create a map marker
        MapAPI.addMarker(playerId, "Shop", x, y, z);
    }
}`}</pre>
      </div>
    </DocsLayout>
  );
}

function DocsGettingStarted() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>Getting Started</h1>
        <p className="docs-intro">
          This guide will help you create your first Rubidium plugin in just a few minutes.
        </p>

        <h2>Step 1: Add the Dependency</h2>
        <p>Add Rubidium to your project:</p>
        <h3>Gradle (Kotlin DSL)</h3>
        <pre className="docs-code">{`dependencies {
    implementation("com.rubidium:rubidium-api:1.0.0")
}`}</pre>

        <h3>Gradle (Groovy)</h3>
        <pre className="docs-code">{`dependencies {
    implementation 'com.rubidium:rubidium-api:1.0.0'
}`}</pre>

        <h3>Maven</h3>
        <pre className="docs-code">{`<dependency>
    <groupId>com.rubidium</groupId>
    <artifactId>rubidium-api</artifactId>
    <version>1.0.0</version>
</dependency>`}</pre>

        <h2>Step 2: Create Your Module</h2>
        <p>Create a new class that extends <code>RubidiumModule</code>:</p>
        <pre className="docs-code">{`package com.example.myplugin;

import rubidium.core.module.RubidiumModule;

public class MyPlugin extends RubidiumModule {
    
    @Override
    public String getId() {
        return "my_plugin";
    }
    
    @Override
    public String getName() {
        return "My Plugin";
    }
    
    @Override
    public void onEnable() {
        getLogger().info("My plugin is enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("My plugin is disabled!");
    }
}`}</pre>

        <h2>Step 3: Create module.json</h2>
        <p>Create a <code>module.json</code> file in your resources folder:</p>
        <pre className="docs-code">{`{
    "id": "my_plugin",
    "name": "My Plugin",
    "version": "1.0.0",
    "main": "com.example.myplugin.MyPlugin",
    "description": "My first Rubidium plugin",
    "authors": ["Your Name"]
}`}</pre>

        <h2>Step 4: Build and Install</h2>
        <p>Build your JAR and place it in the <code>modules/</code> folder of your Rubidium server.</p>
        <pre className="docs-code">{`./gradlew shadowJar
cp build/libs/myplugin-1.0.0.jar /path/to/server/modules/`}</pre>

        <h2>That's It!</h2>
        <p>Your plugin will load automatically when the server starts. Check the console for your log message!</p>
        
        <div className="docs-next">
          <Link to="/rubidium/documentation/apis" className="btn btn-primary">
            Explore APIs <ChevronRight size={18} />
          </Link>
        </div>
      </div>
    </DocsLayout>
  );
}

function DocsInstallation() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>Installation</h1>
        <p className="docs-intro">
          Learn how to install Rubidium on your server.
        </p>

        <h2>Requirements</h2>
        <ul className="docs-list">
          <li>Java 19 or higher</li>
          <li>4GB+ RAM recommended</li>
          <li>Hytale server installation</li>
        </ul>

        <h2>Download</h2>
        <p>Download the latest Rubidium JAR from the releases page:</p>
        <pre className="docs-code">{`wget https://releases.rubidium.dev/latest/Rubidium-1.0.0.jar`}</pre>

        <h2>Server Setup</h2>
        <ol className="docs-list">
          <li>Place <code>Rubidium-1.0.0.jar</code> in your server's <code>plugins/</code> folder</li>
          <li>Start your server</li>
          <li>Rubidium will create a <code>modules/</code> folder for your plugins</li>
        </ol>

        <h2>Verify Installation</h2>
        <p>Check the server console for:</p>
        <pre className="docs-code">{`[Rubidium] Framework v1.0.0 loaded
[Rubidium] Loaded 0 modules`}</pre>
      </div>
    </DocsLayout>
  );
}

function DocsModules() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>Modules</h1>
        <p className="docs-intro">
          Modules are the building blocks of Rubidium. Each plugin is a module.
        </p>

        <h2>Module Structure</h2>
        <pre className="docs-code">{`my-plugin/
  src/
    main/
      java/
        com/example/
          MyPlugin.java
      resources/
        module.json
        config.yml
  build.gradle`}</pre>

        <h2>Hot Reload</h2>
        <p>Rubidium supports hot-reloading modules without restarting the server:</p>
        <pre className="docs-code">{`/rubidium reload my_plugin`}</pre>

        <h2>Dependencies</h2>
        <p>Declare dependencies on other modules:</p>
        <pre className="docs-code">{`{
    "id": "my_plugin",
    "dependencies": ["economy", "permissions"],
    "softDependencies": ["voice_chat"]
}`}</pre>
      </div>
    </DocsLayout>
  );
}

function DocsLifecycle() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>Lifecycle</h1>
        <p className="docs-intro">
          Understanding the module lifecycle helps you write better plugins.
        </p>

        <h2>Lifecycle Methods</h2>
        <pre className="docs-code">{`public class MyPlugin extends RubidiumModule {
    
    @Override
    public void onLoad() {
        // Called when JAR is loaded, before enable
        // Good for: loading configs, registering services
    }
    
    @Override
    public void onEnable() {
        // Called when module is enabled
        // Good for: registering commands, listeners, tasks
    }
    
    @Override
    public void onDisable() {
        // Called when module is disabled
        // Good for: cleanup, saving data
    }
    
    @Override
    public void onReload() {
        // Called during hot-reload
        // Good for: refreshing configs without full restart
    }
}`}</pre>

        <h2>Lifecycle Order</h2>
        <ol className="docs-list">
          <li><code>onLoad()</code> - All modules loaded in dependency order</li>
          <li><code>onEnable()</code> - All modules enabled in dependency order</li>
          <li><code>onDisable()</code> - Modules disabled in reverse order</li>
        </ol>
      </div>
    </DocsLayout>
  );
}

function DocsConfig() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>Configuration</h1>
        <p className="docs-intro">
          Rubidium provides a powerful configuration system with hot-reload support.
        </p>

        <h2>Simple Config</h2>
        <pre className="docs-code">{`// Access your config
String message = getConfig().getString("welcome_message", "Hello!");
int maxPlayers = getConfig().getInt("max_players", 100);

// Save changes
getConfig().set("welcome_message", "Welcome!");
getConfig().save();`}</pre>

        <h2>Typed Configs</h2>
        <pre className="docs-code">{`// Define a config class
public record MyConfig(
    String welcomeMessage,
    int maxPlayers,
    List<String> bannedWords
) {}

// Load it automatically
MyConfig config = getTypedConfig(MyConfig.class);
System.out.println(config.welcomeMessage());`}</pre>

        <h2>Hot Reload</h2>
        <p>Configs reload automatically when files change:</p>
        <pre className="docs-code">{`// Listen for config changes
onConfigReload(() -> {
    refreshSettings();
});`}</pre>
      </div>
    </DocsLayout>
  );
}

function DocsAPIs() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>API Reference</h1>
        <p className="docs-intro">
          Rubidium provides clean, easy-to-use APIs for common server features.
        </p>

        <div className="docs-api-grid">
          <Link to="/rubidium/documentation/apis/voice" className="docs-api-card">
            <Volume2 size={40} />
            <h3>Voice Chat API</h3>
            <p>Proximity voice, groups, whisper mode, push-to-talk</p>
          </Link>
          
          <Link to="/rubidium/documentation/apis/map" className="docs-api-card">
            <Map size={40} />
            <h3>Map API</h3>
            <p>Markers, waypoints, quest objectives, minimap integration</p>
          </Link>
          
          <Link to="/rubidium/documentation/apis/ui" className="docs-api-card">
            <Monitor size={40} />
            <h3>UI Framework</h3>
            <p>Declarative UI components, menus, HUD elements</p>
          </Link>
          
          <Link to="/rubidium/documentation/apis/teleport" className="docs-api-card">
            <Target size={40} />
            <h3>Teleportation</h3>
            <p>/tp, /tpa, homes, warps, spawn management</p>
          </Link>
        </div>

        <h2>API Design Principles</h2>
        <ul className="docs-list">
          <li><strong>Simple by Default</strong> - Common tasks take one line of code</li>
          <li><strong>Powerful When Needed</strong> - Full control available via builders</li>
          <li><strong>Type Safe</strong> - Compile-time error checking</li>
          <li><strong>Well Documented</strong> - Every method has clear JavaDocs</li>
        </ul>
      </div>
    </DocsLayout>
  );
}

function DocsVoiceAPI() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>Voice Chat API</h1>
        <p className="docs-intro">
          Add voice chat to your server with Simple Voice Chat quality features.
        </p>

        <h2>Quick Start</h2>
        <pre className="docs-code">{`import rubidium.voice.VoiceAPI;

// Check if voice is available
if (VoiceAPI.isAvailable()) {
    // Mute/unmute a player
    VoiceAPI.setPlayerMuted(playerId, true);
    
    // Enable whisper mode (8 block range)
    VoiceAPI.setWhisperMode(playerId, true);
    
    // Set activation mode
    VoiceAPI.setActivationMode(playerId, ActivationMode.PUSH_TO_TALK);
}`}</pre>

        <h2>Voice Groups</h2>
        <p>Create private voice channels for teams or parties:</p>
        <pre className="docs-code">{`// Create a group
VoiceGroup group = VoiceAPI.createGroup(ownerId, "Party Chat");

// Password protect it
group.setPassword("secret123");

// Players can join
VoiceAPI.joinGroup(playerId, group.getId(), "secret123");

// Leave the group
VoiceAPI.leaveGroup(playerId, group.getId());`}</pre>

        <h2>Per-Player Volume</h2>
        <pre className="docs-code">{`// Adjust how loud another player sounds to you
VoiceAPI.setPlayerVolume(listenerId, speakerId, 0.5f); // 50% volume`}</pre>

        <h2>Events</h2>
        <pre className="docs-code">{`// Listen for voice state changes
VoiceAPI.onStateChange((playerId, state) -> {
    if (state.isSpeaking()) {
        // Player started talking
    }
});

// Listen for group events
VoiceAPI.onGroupJoin((playerId, group) -> {
    broadcast(player.getName() + " joined " + group.getName());
});`}</pre>

        <h2>Features</h2>
        <ul className="docs-list">
          <li>Opus codec for high-quality, low-bandwidth audio</li>
          <li>RNNoise-style noise suppression</li>
          <li>Automatic gain control</li>
          <li>Voice activity detection (VAD)</li>
          <li>Push-to-talk support</li>
          <li>Whisper mode (8 block range)</li>
          <li>Password-protected groups</li>
        </ul>
      </div>
    </DocsLayout>
  );
}

function DocsMapAPI() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>Map Integration API</h1>
        <p className="docs-intro">
          Integrate with Hytale's built-in world map (M key) for markers, waypoints, and objectives.
        </p>

        <h2>Quick Start</h2>
        <pre className="docs-code">{`import rubidium.map.MapAPI;

// Add a marker visible to a player
MapAPI.addMarker(playerId, "Shop", x, y, z, MarkerIcon.SHOP);

// Create a waypoint
Waypoint wp = MapAPI.createWaypoint(playerId, "My Base", x, y, z);

// Add a quest objective marker
MapAPI.addQuestMarker(playerId, "Find the artifact", x, y, z);`}</pre>

        <h2>Minimap Integration</h2>
        <pre className="docs-code">{`// Show/hide minimap elements
MapAPI.setMinimapVisible(playerId, true);
MapAPI.setMinimapZoom(playerId, 2.0f);

// Track player positions on minimap
MapAPI.setPlayerVisible(playerId, targetId, true);`}</pre>

        <h2>Waypoints</h2>
        <pre className="docs-code">{`// Create personal waypoints
Waypoint home = MapAPI.createWaypoint(playerId, "Home", x, y, z);

// List player's waypoints
List<Waypoint> waypoints = MapAPI.getWaypoints(playerId);

// Delete a waypoint
MapAPI.deleteWaypoint(playerId, waypoint.getId());`}</pre>

        <h2>Hytale World Map Sync</h2>
        <p>All markers automatically sync with the native Hytale world map when players press M:</p>
        <pre className="docs-code">{`// Markers appear on both minimap and world map
MapAPI.addMarker(playerId, "Town", x, y, z, MarkerIcon.TOWN);

// Quest objectives show on world map with tracking
MapAPI.addQuestObjective(playerId, "Defeat the Dragon", x, y, z);`}</pre>
      </div>
    </DocsLayout>
  );
}

function DocsUIAPI() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>UI Framework</h1>
        <p className="docs-intro">
          Create beautiful in-game UIs with a declarative component system.
        </p>

        <h2>Quick Start</h2>
        <pre className="docs-code">{`import rubidium.ui.*;

// Create a simple menu
UIMenu menu = UI.menu("Shop")
    .addItem(UI.button("Buy Sword", () -> buySword()))
    .addItem(UI.button("Buy Shield", () -> buyShield()))
    .build();

// Show to player
UI.show(playerId, menu);`}</pre>

        <h2>Components</h2>
        <pre className="docs-code">{`// Text
UI.text("Hello World").color(Color.GOLD)

// Buttons
UI.button("Click Me", () -> doSomething())
    .width(200).height(40)

// Progress bars
UI.progressBar(0.75f).color(Color.GREEN)

// Icons
UI.icon(ItemType.DIAMOND_SWORD)

// Layouts
UI.row(component1, component2, component3)
UI.column(component1, component2, component3)
UI.grid(3, components)`}</pre>

        <h2>HUD Elements</h2>
        <pre className="docs-code">{`// Add to player's HUD
HudElement healthBar = UI.hud("health")
    .position(HudPosition.TOP_LEFT)
    .add(UI.progressBar(player.getHealthPercent()))
    .build();

UI.addHud(playerId, healthBar);

// Update dynamically
healthBar.update(UI.progressBar(newHealth));`}</pre>

        <h2>Forms & Input</h2>
        <pre className="docs-code">{`// Create an input form
UIForm form = UI.form("Settings")
    .addField("username", UI.textInput("Username"))
    .addField("volume", UI.slider(0, 100, 80))
    .addField("enabled", UI.checkbox("Enable Feature"))
    .onSubmit(data -> {
        String username = data.getString("username");
        int volume = data.getInt("volume");
    })
    .build();`}</pre>
      </div>
    </DocsLayout>
  );
}

function DocsTeleportAPI() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>Teleportation API</h1>
        <p className="docs-intro">
          Complete teleportation system with /tp, /tpa, homes, and warps built-in.
        </p>

        <h2>Built-in Commands</h2>
        <p>Rubidium provides these commands out of the box:</p>
        <pre className="docs-code">{`/tp <player>           - Teleport to player (admin)
/tp <player> <target>  - Teleport player to target (admin)
/tp <x> <y> <z>        - Teleport to coordinates (admin)
/tpa <player>          - Request to teleport to player
/tpahere <player>      - Request player to teleport to you
/tpaccept              - Accept teleport request
/tpdeny                - Deny teleport request
/home                  - Teleport to your home
/home <name>           - Teleport to named home
/sethome [name]        - Set your home location
/delhome <name>        - Delete a home
/homes                 - List your homes
/warp <name>           - Teleport to a warp
/setwarp <name>        - Create a warp (admin)
/delwarp <name>        - Delete a warp (admin)
/warps                 - List available warps
/spawn                 - Teleport to spawn
/setspawn              - Set spawn location (admin)`}</pre>

        <h2>API Usage</h2>
        <pre className="docs-code">{`import rubidium.teleport.TeleportAPI;

// Teleport a player
TeleportAPI.teleport(playerId, x, y, z);

// Teleport with options
TeleportAPI.teleport(playerId, location)
    .withDelay(3)          // 3 second delay
    .cancelOnMove()        // Cancel if player moves
    .withMessage("Teleporting in 3 seconds...")
    .execute();

// Manage homes
TeleportAPI.setHome(playerId, "base", location);
Location home = TeleportAPI.getHome(playerId, "base");
List<Home> homes = TeleportAPI.getHomes(playerId);

// Manage warps
TeleportAPI.createWarp("spawn", location);
TeleportAPI.teleportToWarp(playerId, "spawn");`}</pre>

        <h2>TPA Requests</h2>
        <pre className="docs-code">{`// Send a TPA request
TeleportAPI.sendTpaRequest(senderId, targetId);

// Handle requests programmatically
TeleportAPI.onTpaRequest((sender, target) -> {
    // Custom logic before showing request
    return true; // Allow the request
});

// Auto-accept for friends
TeleportAPI.setAutoAccept(playerId, true);`}</pre>

        <h2>Permissions</h2>
        <pre className="docs-code">{`rubidium.teleport.tp      - Use /tp command
rubidium.teleport.tpa     - Use /tpa command
rubidium.teleport.home    - Use /home command
rubidium.teleport.homes.5 - Allow 5 homes
rubidium.teleport.warp    - Use /warp command
rubidium.teleport.spawn   - Use /spawn command`}</pre>
      </div>
    </DocsLayout>
  );
}

function DocsExamples() {
  return (
    <DocsLayout>
      <div className="docs-article">
        <h1>Code Examples</h1>
        <p className="docs-intro">
          Learn from complete, working examples.
        </p>

        <h2>Simple Plugin</h2>
        <pre className="docs-code">{`public class WelcomePlugin extends RubidiumModule {
    
    @Override
    public void onEnable() {
        // Listen for player join
        events().on(PlayerJoinEvent.class, event -> {
            Player player = event.getPlayer();
            player.sendMessage("Welcome, " + player.getName() + "!");
            
            // Play a sound
            player.playSound(Sounds.LEVEL_UP);
        });
    }
}`}</pre>

        <h2>Economy Plugin</h2>
        <pre className="docs-code">{`public class EconomyPlugin extends RubidiumModule {
    private Map<UUID, Double> balances = new HashMap<>();
    
    @Override
    public void onEnable() {
        // Load balances from config
        balances = getConfig().getMap("balances", UUID.class, Double.class);
        
        // Register commands
        commands().register("balance", (sender, args) -> {
            double bal = balances.getOrDefault(sender.getId(), 100.0);
            sender.sendMessage("Balance: $" + bal);
        });
        
        commands().register("pay", (sender, args) -> {
            if (args.length < 2) {
                sender.sendMessage("Usage: /pay <player> <amount>");
                return;
            }
            Player target = players().find(args[0]);
            double amount = Double.parseDouble(args[1]);
            
            transfer(sender.getId(), target.getId(), amount);
        });
    }
    
    public void transfer(UUID from, UUID to, double amount) {
        double fromBal = balances.getOrDefault(from, 0.0);
        if (fromBal < amount) throw new InsufficientFundsException();
        
        balances.put(from, fromBal - amount);
        balances.put(to, balances.getOrDefault(to, 0.0) + amount);
        save();
    }
}`}</pre>

        <h2>Voice Chat Zone</h2>
        <pre className="docs-code">{`public class VoiceZonesPlugin extends RubidiumModule {
    
    @Override
    public void onEnable() {
        // Create a "quiet zone" where voice is disabled
        Region quietZone = regions().create("library", pos1, pos2);
        
        events().on(PlayerEnterRegionEvent.class, event -> {
            if (event.getRegion().equals(quietZone)) {
                VoiceAPI.setPlayerMuted(event.getPlayer().getId(), true);
                event.getPlayer().sendMessage("Entering quiet zone - voice disabled");
            }
        });
        
        events().on(PlayerLeaveRegionEvent.class, event -> {
            if (event.getRegion().equals(quietZone)) {
                VoiceAPI.setPlayerMuted(event.getPlayer().getId(), false);
                event.getPlayer().sendMessage("Leaving quiet zone - voice enabled");
            }
        });
    }
}`}</pre>

        <h2>Custom Map Markers</h2>
        <pre className="docs-code">{`public class ShopMarkersPlugin extends RubidiumModule {
    
    @Override
    public void onEnable() {
        // Add shop markers for all players
        List<Shop> shops = loadShops();
        
        for (Shop shop : shops) {
            // Marker visible to everyone
            MapAPI.addGlobalMarker(
                shop.getName(),
                shop.getLocation(),
                MarkerIcon.SHOP
            );
        }
        
        // Dynamic markers based on player
        events().on(PlayerJoinEvent.class, event -> {
            Player player = event.getPlayer();
            
            // Show personal quest markers
            for (Quest quest : getActiveQuests(player)) {
                MapAPI.addQuestMarker(
                    player.getId(),
                    quest.getObjective(),
                    quest.getTargetLocation()
                );
            }
        });
    }
}`}</pre>
      </div>
    </DocsLayout>
  );
}

function Footer() {
  return (
    <footer className="footer">
      <div className="footer-content">
        <div className="footer-brand">
          <Cpu size={24} />
          <span>Rubidium</span>
        </div>
        <p>The production-ready server framework for Hytale. Not affiliated with Hypixel Studios.</p>
        <div className="footer-links">
          <Link to="/rubidium/documentation">Documentation</Link>
          <Link to="/rubidium/documentation/apis">API Reference</Link>
          <Link to="/marketplace">Marketplace</Link>
          <Link to="/rubidium/license">License</Link>
          <a href="#">Privacy Policy</a>
          <a href="#">Contact</a>
        </div>
        <p className="copyright">&copy; 2026 Rubidium. All rights reserved.</p>
      </div>
    </footer>
  );
}

function AppLayout() {
  const location = useLocation();
  const isPreview = location.pathname === '/preview';

  if (isPreview) {
    return <LauncherPreview />;
  }

  return (
    <div className="app">
      <Navbar />
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/download" element={<DownloadPage />} />
          <Route path="/features" element={<FeaturesPage />} />
          <Route path="/rubidium" element={<RubidiumPage />} />
          <Route path="/rubidium/license" element={<RubidiumLicensePage />} />
          <Route path="/rubidium/documentation" element={<DocsOverview />} />
          <Route path="/rubidium/documentation/getting-started" element={<DocsGettingStarted />} />
          <Route path="/rubidium/documentation/installation" element={<DocsInstallation />} />
          <Route path="/rubidium/documentation/modules" element={<DocsModules />} />
          <Route path="/rubidium/documentation/lifecycle" element={<DocsLifecycle />} />
          <Route path="/rubidium/documentation/config" element={<DocsConfig />} />
          <Route path="/rubidium/documentation/apis" element={<DocsAPIs />} />
          <Route path="/rubidium/documentation/apis/voice" element={<DocsVoiceAPI />} />
          <Route path="/rubidium/documentation/apis/map" element={<DocsMapAPI />} />
          <Route path="/rubidium/documentation/apis/ui" element={<DocsUIAPI />} />
          <Route path="/rubidium/documentation/apis/teleport" element={<DocsTeleportAPI />} />
          <Route path="/rubidium/documentation/examples" element={<DocsExamples />} />
          <Route path="/marketplace" element={<Marketplace />} />
          <Route path="/marketplace/upload" element={<UploadPage />} />
          <Route path="/premium" element={<PremiumPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/preview" element={<LauncherPreview />} />
        </Routes>
      </main>
      <Footer />
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppLayout />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
