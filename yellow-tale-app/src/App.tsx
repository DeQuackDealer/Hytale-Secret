import { useState, useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { 
  Home, Settings, Users, Server, Gamepad2, Download, Crown, Loader2, 
  Sparkles, Smile, User as UserIcon, Play, ShoppingBag, BarChart3, 
  Bell, Search, Star, Heart, Zap, Clock, Activity, ChevronRight,
  Monitor, Cpu, HardDrive, Wifi, Volume2, Eye, Palette, Shield,
  Globe, MessageSquare, Gift, TrendingUp, Award, Target, Map
} from 'lucide-react';

interface User {
  id: string;
  username: string;
  display_name?: string;
  avatar_url?: string;
  premium: boolean;
}

interface ServerInfo {
  id: string;
  name: string;
  address: string;
  port: number;
  player_count: number;
  max_players: number;
  ping_ms: number;
  version: string;
  gamemode?: string;
  favorited?: boolean;
}

interface Friend {
  id: string;
  username: string;
  display_name?: string;
  status: 'online' | 'away' | 'offline';
  activity?: string;
}

type View = 'home' | 'play' | 'servers' | 'friends' | 'mods' | 'cosmetics' | 'shop' | 'stats' | 'settings';

interface CosmeticItem {
  id: string;
  name: string;
  description: string;
  category: string;
  thumbnail_url?: string;
  rarity: string;
  equipped: boolean;
}

interface EquippedCosmetics {
  skin?: string;
  emote_1?: string;
  emote_2?: string;
  emote_3?: string;
  emote_4?: string;
  cape?: string;
  wings?: string;
  aura?: string;
}

interface ModProfile {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
  version: string;
  author: string;
}

interface NewsItem {
  id: string;
  title: string;
  summary: string;
  date: string;
}

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [view, setView] = useState<View>('home');
  const [loading, setLoading] = useState(true);
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');

  useEffect(() => {
    checkUser();
  }, []);

  async function checkUser() {
    try {
      const u = await invoke<User | null>('get_user');
      setUser(u);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    try {
      const u = await invoke<User>('login', loginForm);
      setUser(u);
    } catch (e) {
      setError(String(e));
    }
  }

  async function handleLogout() {
    await invoke('logout');
    setUser(null);
  }

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="logo-container">
          <Gamepad2 size={56} />
        </div>
        <p>Loading Yellow Tale...</p>
        <div className="loading-bar" />
      </div>
    );
  }

  if (!user) {
    return (
      <div className="login-screen">
        <div className="login-left">
          <div className="login-branding">
            <div className="login-logo">
              <Gamepad2 size={48} />
            </div>
            <h1>Yellow Tale</h1>
            <p>Your Ultimate Hytale Companion</p>
          </div>
          
          <div className="login-card">
            <h2>Welcome Back</h2>
            
            <form onSubmit={handleLogin}>
              {error && <div className="error">{error}</div>}
              
              <div className="input-group">
                <label>Username or Email</label>
                <input
                  type="text"
                  value={loginForm.username}
                  onChange={(e) => setLoginForm({ ...loginForm, username: e.target.value })}
                  placeholder="Enter your username"
                  required
                />
              </div>
              
              <div className="input-group">
                <label>Password</label>
                <input
                  type="password"
                  value={loginForm.password}
                  onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                  placeholder="Enter your password"
                  required
                />
              </div>
              
              <button type="submit" className="btn-primary">
                Sign In
              </button>
            </form>
            
            <p className="login-footer">
              Don't have an account? <a href="https://yellowtale.com/register">Create Account</a>
            </p>
          </div>
        </div>
        
        <div className="login-right">
          <Gamepad2 size={200} style={{ opacity: 0.1, color: 'var(--duck-yellow)' }} />
        </div>
      </div>
    );
  }

  const viewTitles: Record<View, string> = {
    home: 'Home',
    play: 'Launch Game',
    servers: 'Server Browser',
    friends: 'Friends',
    mods: 'Mod Profiles',
    cosmetics: 'Cosmetics',
    shop: 'Shop',
    stats: 'Statistics',
    settings: 'Settings'
  };

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="sidebar-logo">
            <Gamepad2 size={24} />
          </div>
          <span className="sidebar-logo-text">Yellow Tale</span>
        </div>
        
        <nav className="sidebar-nav">
          <div className="nav-section">
            <div className="nav-section-label">Main</div>
            
            <button 
              className={`nav-item ${view === 'home' ? 'active' : ''}`}
              onClick={() => setView('home')}
            >
              <Home size={22} />
              <span className="nav-label">Home</span>
            </button>
            
            <button 
              className={`nav-item ${view === 'play' ? 'active' : ''}`}
              onClick={() => setView('play')}
            >
              <Play size={22} />
              <span className="nav-label">Play</span>
            </button>
            
            <button 
              className={`nav-item ${view === 'servers' ? 'active' : ''}`}
              onClick={() => setView('servers')}
            >
              <Server size={22} />
              <span className="nav-label">Servers</span>
            </button>
          </div>
          
          <div className="nav-section">
            <div className="nav-section-label">Social</div>
            
            <button 
              className={`nav-item ${view === 'friends' ? 'active' : ''}`}
              onClick={() => setView('friends')}
            >
              <Users size={22} />
              <span className="nav-label">Friends</span>
              <span className="nav-badge">3</span>
            </button>
          </div>
          
          <div className="nav-section">
            <div className="nav-section-label">Customization</div>
            
            <button 
              className={`nav-item ${view === 'mods' ? 'active' : ''}`}
              onClick={() => setView('mods')}
            >
              <Download size={22} />
              <span className="nav-label">Mods</span>
            </button>
            
            <button 
              className={`nav-item ${view === 'cosmetics' ? 'active' : ''}`}
              onClick={() => setView('cosmetics')}
            >
              <Sparkles size={22} />
              <span className="nav-label">Cosmetics</span>
            </button>
            
            <button 
              className={`nav-item ${view === 'shop' ? 'active' : ''}`}
              onClick={() => setView('shop')}
            >
              <ShoppingBag size={22} />
              <span className="nav-label">Shop</span>
            </button>
          </div>
          
          <div className="nav-section">
            <div className="nav-section-label">Profile</div>
            
            <button 
              className={`nav-item ${view === 'stats' ? 'active' : ''}`}
              onClick={() => setView('stats')}
            >
              <BarChart3 size={22} />
              <span className="nav-label">Stats</span>
            </button>
            
            <button 
              className={`nav-item ${view === 'settings' ? 'active' : ''}`}
              onClick={() => setView('settings')}
            >
              <Settings size={22} />
              <span className="nav-label">Settings</span>
            </button>
          </div>
        </nav>
        
        <div className="sidebar-footer">
          <div className="user-card" onClick={handleLogout}>
            <div className="user-avatar">
              {user.username[0].toUpperCase()}
              <span className="status-dot" />
            </div>
            <div className="user-info">
              <div className="user-name">
                {user.display_name || user.username}
                {user.premium && <Crown size={14} className="premium-badge" />}
              </div>
              <div className="user-status">Online</div>
            </div>
          </div>
        </div>
      </aside>
      
      <div className="main-wrapper">
        <header className="top-bar">
          <div className="top-bar-left">
            <h1 className="page-title">{viewTitles[view]}</h1>
          </div>
          <div className="top-bar-right">
            <div className="search-bar">
              <Search size={16} />
              <input type="text" placeholder="Search..." />
            </div>
            <button className="icon-btn">
              <Bell size={18} />
              <span className="notification-dot" />
            </button>
          </div>
        </header>
        
        <main className="main-content">
          {view === 'home' && <HomeView user={user} onNavigate={setView} />}
          {view === 'play' && <PlayView />}
          {view === 'servers' && <ServersView />}
          {view === 'friends' && <FriendsView />}
          {view === 'mods' && <ModsView />}
          {view === 'cosmetics' && <CosmeticsView />}
          {view === 'shop' && <ShopView />}
          {view === 'stats' && <StatsView />}
          {view === 'settings' && <SettingsView />}
        </main>
      </div>
    </div>
  );
}

function HomeView({ user, onNavigate }: { user: User; onNavigate: (view: View) => void }) {
  const [launching, setLaunching] = useState(false);
  const [friends] = useState<Friend[]>([
    { id: '1', username: 'GameMaster99', status: 'online', activity: 'Playing on HytaleCraft' },
    { id: '2', username: 'BlockBuilder', status: 'online', activity: 'In Main Menu' },
    { id: '3', username: 'AdventureSeeker', status: 'away', activity: 'AFK' },
    { id: '4', username: 'CraftKing', status: 'offline' },
  ]);
  
  const [news] = useState<NewsItem[]>([
    { id: '1', title: 'Hytale Early Access Launch', summary: 'Join the adventure today!', date: 'Jan 13, 2026' },
    { id: '2', title: 'Yellow Tale 1.0 Released', summary: 'New features and improvements', date: 'Jan 12, 2026' },
  ]);

  async function launchGame() {
    setLaunching(true);
    try {
      await invoke('launch_game', { serverAddress: null });
    } catch (e) {
      console.error(e);
    } finally {
      setLaunching(false);
    }
  }

  return (
    <div className="view home-view">
      <div className="home-main">
        <div className="hero-card">
          <div className="hero-content">
            <h1>Welcome back, <span>{user.display_name || user.username}</span>!</h1>
            <p>Ready to explore Orbis? Your adventure awaits.</p>
            <div className="hero-stats">
              <div className="hero-stat">
                <div className="hero-stat-value">24h</div>
                <div className="hero-stat-label">Play Time</div>
              </div>
              <div className="hero-stat">
                <div className="hero-stat-value">3</div>
                <div className="hero-stat-label">Friends Online</div>
              </div>
              <div className="hero-stat">
                <div className="hero-stat-value">12</div>
                <div className="hero-stat-label">Achievements</div>
              </div>
            </div>
          </div>
          <div className="launch-button">
            <button 
              className="btn-launch" 
              onClick={launchGame}
              disabled={launching}
            >
              {launching ? (
                <Loader2 size={48} className="spin" />
              ) : (
                <Play size={48} />
              )}
              <span>{launching ? 'Launching...' : 'PLAY'}</span>
            </button>
            <span className="launch-version">Hytale Early Access</span>
          </div>
        </div>
        
        <div className="quick-actions">
          <div className="quick-action" onClick={() => onNavigate('servers')}>
            <div className="quick-action-icon">
              <Server size={24} />
            </div>
            <span className="quick-action-label">Browse Servers</span>
          </div>
          <div className="quick-action" onClick={() => onNavigate('friends')}>
            <div className="quick-action-icon">
              <Users size={24} />
            </div>
            <span className="quick-action-label">Friends</span>
          </div>
          <div className="quick-action" onClick={() => onNavigate('mods')}>
            <div className="quick-action-icon">
              <Download size={24} />
            </div>
            <span className="quick-action-label">Mod Profiles</span>
          </div>
          <div className="quick-action" onClick={() => onNavigate('shop')}>
            <div className="quick-action-icon">
              <ShoppingBag size={24} />
            </div>
            <span className="quick-action-label">Shop</span>
          </div>
        </div>
        
        <div className="news-section">
          <div className="section-header">
            <h2 className="section-title"><Zap size={18} /> Latest News</h2>
            <a href="#" className="section-link">View All <ChevronRight size={14} /></a>
          </div>
          <div className="news-grid">
            {news.map(item => (
              <div key={item.id} className="news-card">
                <div className="news-image">
                  <Gamepad2 size={32} />
                </div>
                <div className="news-content">
                  <h3>{item.title}</h3>
                  <p>{item.summary}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
      
      <div className="home-sidebar">
        <div className="friends-card">
          <div className="friends-header">
            <h3><Users size={16} /> Friends</h3>
            <span className="online-count">{friends.filter(f => f.status === 'online').length} Online</span>
          </div>
          <div className="friends-list">
            {friends.filter(f => f.status !== 'offline').map(friend => (
              <div key={friend.id} className="friend-item">
                <div className="friend-avatar">
                  {friend.username[0].toUpperCase()}
                  <span className={`status-dot ${friend.status}`} />
                </div>
                <div className="friend-info">
                  <div className="friend-name">{friend.username}</div>
                  <div className={`friend-status ${friend.activity?.includes('Playing') ? 'playing' : ''}`}>
                    {friend.activity || 'Offline'}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
        
        <div className="activity-card">
          <div className="activity-header">
            <h3><Activity size={16} /> Recent Activity</h3>
          </div>
          <div className="activity-list">
            <div className="activity-item">
              <div className="activity-icon"><Award size={16} /></div>
              <div className="activity-content">
                <div className="activity-text">Unlocked <strong>First Steps</strong> achievement</div>
                <div className="activity-time">2 hours ago</div>
              </div>
            </div>
            <div className="activity-item">
              <div className="activity-icon"><Server size={16} /></div>
              <div className="activity-content">
                <div className="activity-text">Joined <strong>HytaleCraft</strong> server</div>
                <div className="activity-time">5 hours ago</div>
              </div>
            </div>
            <div className="activity-item">
              <div className="activity-icon"><Gift size={16} /></div>
              <div className="activity-content">
                <div className="activity-text">Received <strong>Daily Reward</strong></div>
                <div className="activity-time">1 day ago</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function PlayView() {
  const [launching, setLaunching] = useState(false);
  const [ram, setRam] = useState(4096);
  const [fpsLimit, setFpsLimit] = useState(0);
  const [profile, setProfile] = useState('default');

  async function launchGame() {
    setLaunching(true);
    try {
      await invoke('launch_game', { 
        serverAddress: null,
        ramMb: ram,
        fpsLimit: fpsLimit 
      });
    } catch (e) {
      console.error(e);
    } finally {
      setLaunching(false);
    }
  }

  return (
    <div className="view play-view">
      <div className="hero-card" style={{ marginBottom: '1.5rem' }}>
        <div className="hero-content">
          <h1>Launch <span>Hytale</span></h1>
          <p>Configure your game settings and start playing</p>
        </div>
        <div className="launch-button">
          <button 
            className="btn-launch" 
            onClick={launchGame}
            disabled={launching}
          >
            {launching ? (
              <Loader2 size={48} className="spin" />
            ) : (
              <Play size={48} />
            )}
            <span>{launching ? 'Launching...' : 'PLAY'}</span>
          </button>
        </div>
      </div>

      <div className="settings-section">
        <h2><Cpu size={18} /> Performance Settings</h2>
        
        <div className="setting-row">
          <div className="setting-label">
            <span>RAM Allocation</span>
            <span>Allocate memory for the game</span>
          </div>
          <div className="ram-slider">
            <input 
              type="range" 
              min={2048} 
              max={16384} 
              step={1024}
              value={ram}
              onChange={(e) => setRam(parseInt(e.target.value))}
            />
            <span>{ram / 1024} GB</span>
          </div>
        </div>

        <div className="setting-row">
          <div className="setting-label">
            <span>FPS Limit</span>
            <span>Cap your framerate</span>
          </div>
          <select value={fpsLimit} onChange={(e) => setFpsLimit(parseInt(e.target.value))}>
            <option value={0}>Unlimited</option>
            <option value={60}>60 FPS</option>
            <option value={120}>120 FPS</option>
            <option value={144}>144 FPS</option>
            <option value={240}>240 FPS</option>
          </select>
        </div>

        <div className="setting-row">
          <div className="setting-label">
            <span>Mod Profile</span>
            <span>Select which mods to load</span>
          </div>
          <select value={profile} onChange={(e) => setProfile(e.target.value)}>
            <option value="default">Default (No Mods)</option>
            <option value="performance">Performance</option>
            <option value="full">Full Experience</option>
          </select>
        </div>
      </div>

      <div className="stats-grid" style={{ marginTop: '1.5rem' }}>
        <div className="stat-card">
          <div className="stat-card-icon"><Clock size={24} /></div>
          <div className="stat-value">24h 32m</div>
          <div className="stat-label">Total Playtime</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-icon"><TrendingUp size={24} /></div>
          <div className="stat-value">142</div>
          <div className="stat-label">FPS Average</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-icon"><Server size={24} /></div>
          <div className="stat-value">8</div>
          <div className="stat-label">Servers Joined</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-icon"><Award size={24} /></div>
          <div className="stat-value">12</div>
          <div className="stat-label">Achievements</div>
        </div>
      </div>
    </div>
  );
}

function ServersView() {
  const [servers, setServers] = useState<ServerInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');

  useEffect(() => {
    loadServers();
  }, []);

  async function loadServers() {
    try {
      const s = await invoke<ServerInfo[]>('get_servers');
      setServers(s);
    } catch (e) {
      console.error(e);
      setServers([
        { id: '1', name: 'HytaleCraft Official', address: 'play.hytalecraft.com', port: 25565, player_count: 1234, max_players: 5000, ping_ms: 24, version: '1.0', gamemode: 'Survival' },
        { id: '2', name: 'Orbis Adventures', address: 'orbis.gg', port: 25565, player_count: 567, max_players: 1000, ping_ms: 45, version: '1.0', gamemode: 'Adventure' },
        { id: '3', name: 'Creative Builders', address: 'build.hytale.io', port: 25565, player_count: 89, max_players: 200, ping_ms: 120, version: '1.0', gamemode: 'Creative' },
      ]);
    } finally {
      setLoading(false);
    }
  }

  const getPingClass = (ping: number) => {
    if (ping < 50) return 'good';
    if (ping < 100) return 'medium';
    return 'bad';
  };

  return (
    <div className="view servers-view">
      <div className="filters-bar">
        <div className="filter-group">
          <label>Filter:</label>
          <select value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="all">All Servers</option>
            <option value="favorites">Favorites</option>
            <option value="friends">Friends Playing</option>
          </select>
        </div>
        <div className="filter-group">
          <label>Sort by:</label>
          <select defaultValue="players">
            <option value="players">Players</option>
            <option value="ping">Ping</option>
            <option value="name">Name</option>
          </select>
        </div>
      </div>
      
      {loading ? (
        <div className="loading">
          <Loader2 size={32} className="spin" />
          <p>Loading servers...</p>
        </div>
      ) : servers.length === 0 ? (
        <div className="empty-state">
          <Server size={64} />
          <h2>No Servers Available</h2>
          <p>Servers will appear here once Hytale launches.</p>
        </div>
      ) : (
        <div className="server-grid">
          {servers.map((server) => (
            <div key={server.id} className="server-card">
              <div className="server-icon">
                <Globe size={24} />
              </div>
              <div className="server-info">
                <div className="server-name">{server.name}</div>
                <div className="server-address">{server.address}:{server.port}</div>
              </div>
              <div className="server-meta">
                <div className="server-players">
                  <Users size={16} />
                  {server.player_count}/{server.max_players}
                </div>
                <div className={`server-ping ${getPingClass(server.ping_ms)}`}>
                  <Wifi size={16} />
                  {server.ping_ms}ms
                </div>
              </div>
              <div className="server-actions">
                <button className="btn-favorite">
                  <Star size={16} />
                </button>
                <button className="btn-join">Join</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function FriendsView() {
  const [friends] = useState<Friend[]>([
    { id: '1', username: 'GameMaster99', display_name: 'GameMaster', status: 'online', activity: 'Playing on HytaleCraft' },
    { id: '2', username: 'BlockBuilder', status: 'online', activity: 'In Main Menu' },
    { id: '3', username: 'AdventureSeeker', status: 'away', activity: 'AFK - 15 minutes' },
    { id: '4', username: 'CraftKing', status: 'offline' },
    { id: '5', username: 'PixelMaster', status: 'offline' },
  ]);
  
  const [tab, setTab] = useState<'all' | 'online' | 'pending'>('all');

  const filteredFriends = friends.filter(f => {
    if (tab === 'online') return f.status === 'online';
    if (tab === 'pending') return false;
    return true;
  });

  return (
    <div className="view friends-view">
      <div className="view-header">
        <div className="view-tabs">
          <button className={`view-tab ${tab === 'all' ? 'active' : ''}`} onClick={() => setTab('all')}>
            All ({friends.length})
          </button>
          <button className={`view-tab ${tab === 'online' ? 'active' : ''}`} onClick={() => setTab('online')}>
            Online ({friends.filter(f => f.status === 'online').length})
          </button>
          <button className={`view-tab ${tab === 'pending' ? 'active' : ''}`} onClick={() => setTab('pending')}>
            Pending (0)
          </button>
        </div>
        <button className="btn-primary" style={{ width: 'auto', padding: '0.625rem 1.25rem' }}>
          Add Friend
        </button>
      </div>

      {filteredFriends.length === 0 ? (
        <div className="empty-state">
          <Users size={64} />
          <h2>No Friends Yet</h2>
          <p>Add friends to see them here and play together.</p>
          <button className="btn-primary" style={{ width: 'auto' }}>Add Friend</button>
        </div>
      ) : (
        <div className="server-grid">
          {filteredFriends.map(friend => (
            <div key={friend.id} className="server-card">
              <div className="friend-avatar" style={{ width: 48, height: 48, borderRadius: 12 }}>
                {friend.username[0].toUpperCase()}
                <span className={`status-dot ${friend.status}`} />
              </div>
              <div className="server-info">
                <div className="server-name">{friend.display_name || friend.username}</div>
                <div className={`server-address ${friend.activity?.includes('Playing') ? 'playing' : ''}`} style={{ fontFamily: 'inherit' }}>
                  {friend.activity || 'Offline'}
                </div>
              </div>
              <div className="server-actions">
                <button className="icon-btn"><MessageSquare size={18} /></button>
                {friend.status === 'online' && friend.activity?.includes('Playing') && (
                  <button className="btn-join">Join</button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function ModsView() {
  const [mods] = useState<ModProfile[]>([
    { id: '1', name: 'Minimap', description: 'Shows a minimap in the corner of your screen', enabled: true, version: '1.0.0', author: 'Yellow Tale' },
    { id: '2', name: 'Waypoints', description: 'Mark locations and navigate easily', enabled: true, version: '1.0.0', author: 'Yellow Tale' },
    { id: '3', name: 'Replay System', description: 'Record and replay your gameplay', enabled: false, version: '1.0.0', author: 'Yellow Tale' },
    { id: '4', name: 'Cinema Camera', description: 'Cinematic camera controls for screenshots', enabled: false, version: '1.0.0', author: 'Yellow Tale' },
    { id: '5', name: 'Performance Boost', description: 'Optimizations for better FPS', enabled: true, version: '1.2.0', author: 'Yellow Tale' },
    { id: '6', name: 'Social Features', description: 'Enhanced social and party features', enabled: true, version: '1.0.0', author: 'Yellow Tale' },
  ]);

  return (
    <div className="view mods-view">
      <div className="view-header">
        <p style={{ color: 'var(--text-secondary)' }}>
          Enable or disable features for your gameplay experience
        </p>
        <button className="btn-primary" style={{ width: 'auto', padding: '0.625rem 1.25rem' }}>
          Create Profile
        </button>
      </div>

      <div className="mod-grid">
        {mods.map(mod => (
          <div key={mod.id} className={`mod-card ${mod.enabled ? 'active' : ''}`}>
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
            <div className="mod-meta">
              <span>By {mod.author}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function CosmeticsView() {
  const [cosmetics, setCosmetics] = useState<CosmeticItem[]>([]);
  const [equipped, setEquipped] = useState<EquippedCosmetics>({});
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'emotes' | 'skins' | 'effects'>('emotes');

  useEffect(() => {
    loadCosmetics();
  }, []);

  async function loadCosmetics() {
    try {
      const [items, equippedItems] = await Promise.all([
        invoke<CosmeticItem[]>('get_cosmetics'),
        invoke<EquippedCosmetics>('get_equipped_cosmetics')
      ]);
      setCosmetics(items);
      setEquipped(equippedItems);
    } catch (e) {
      console.error(e);
      setCosmetics([
        { id: '1', name: 'Victory Dance', description: 'Celebrate your wins!', category: 'emote', rarity: 'rare', equipped: false },
        { id: '2', name: 'Wave Hello', description: 'Friendly greeting', category: 'emote', rarity: 'common', equipped: false },
        { id: '3', name: 'Thumbs Up', description: 'Show approval', category: 'emote', rarity: 'common', equipped: true },
        { id: '4', name: 'Dance Party', description: 'Get the party started', category: 'emote', rarity: 'epic', equipped: false },
        { id: '5', name: 'Duck Knight', description: 'Glowing duck armor', category: 'skin', rarity: 'legendary', equipped: false },
        { id: '6', name: 'Golden Aura', description: 'Shimmering gold particles', category: 'aura', rarity: 'epic', equipped: false },
        { id: '7', name: 'Quack Cape', description: 'A majestic duck cape', category: 'cape', rarity: 'rare', equipped: false },
      ]);
    }
    setLoading(false);
  }

  async function handleEquip(item: CosmeticItem, slot: string) {
    try {
      await invoke('equip_cosmetic', { item_id: item.id, slot });
      setEquipped(prev => ({ ...prev, [slot]: item.id }));
      setCosmetics(prev => prev.map(c => 
        c.id === item.id ? { ...c, equipped: true } : c
      ));
    } catch (e) {
      console.error(e);
    }
  }

  async function handleUnequip(slot: string) {
    try {
      await invoke('unequip_cosmetic', { slot });
      const itemId = equipped[slot as keyof EquippedCosmetics];
      setEquipped(prev => ({ ...prev, [slot]: undefined }));
      setCosmetics(prev => prev.map(c => 
        c.id === itemId ? { ...c, equipped: false } : c
      ));
    } catch (e) {
      console.error(e);
    }
  }

  const filteredCosmetics = cosmetics.filter(c => {
    if (activeTab === 'emotes') return c.category === 'emote';
    if (activeTab === 'skins') return c.category === 'skin';
    return c.category === 'aura' || c.category === 'cape' || c.category === 'wings';
  });

  const emoteSlots = ['emote_1', 'emote_2', 'emote_3', 'emote_4'];

  return (
    <div className="view cosmetics-view">
      <p className="cosmetics-notice">
        <Sparkles size={16} /> Your cosmetics are visible to all Yellow Tale users!
      </p>

      <div className="equipped-section">
        <h2><Heart size={18} /> Equipped Emotes</h2>
        <div className="emote-slots">
          {emoteSlots.map((slot, i) => {
            const equippedId = equipped[slot as keyof EquippedCosmetics];
            const equippedItem = cosmetics.find(c => c.id === equippedId);
            return (
              <div key={slot} className={`emote-slot ${equippedItem ? 'filled' : ''}`}>
                <span className="slot-number">{i + 1}</span>
                {equippedItem ? (
                  <>
                    <Smile size={24} />
                    <span className="slot-name">{equippedItem.name}</span>
                    <button className="btn-unequip" onClick={() => handleUnequip(slot)}>X</button>
                  </>
                ) : (
                  <span className="slot-empty">Empty</span>
                )}
              </div>
            );
          })}
        </div>
      </div>

      <div className="cosmetics-tabs">
        <button className={activeTab === 'emotes' ? 'active' : ''} onClick={() => setActiveTab('emotes')}>
          <Smile size={18} /> Emotes
        </button>
        <button className={activeTab === 'skins' ? 'active' : ''} onClick={() => setActiveTab('skins')}>
          <UserIcon size={18} /> Skins
        </button>
        <button className={activeTab === 'effects' ? 'active' : ''} onClick={() => setActiveTab('effects')}>
          <Sparkles size={18} /> Effects
        </button>
      </div>

      {loading ? (
        <div className="loading">
          <Loader2 size={32} className="spin" />
          <p>Loading cosmetics...</p>
        </div>
      ) : filteredCosmetics.length === 0 ? (
        <div className="empty-state">
          <Sparkles size={64} />
          <h2>No {activeTab} owned</h2>
          <p>Visit the Shop to get new {activeTab}!</p>
        </div>
      ) : (
        <div className="cosmetics-grid">
          {filteredCosmetics.map(item => (
            <div 
              key={item.id} 
              className={`cosmetic-card ${item.equipped ? 'equipped' : ''}`}
            >
              <div className="cosmetic-preview">
                {item.category === 'emote' ? <Smile size={32} /> : 
                 item.category === 'skin' ? <UserIcon size={32} /> : <Sparkles size={32} />}
              </div>
              <div className="cosmetic-name">{item.name}</div>
              <div className={`cosmetic-rarity ${item.rarity}`}>{item.rarity}</div>
              {item.category === 'emote' && !item.equipped && (
                <div className="equip-buttons">
                  {emoteSlots.map((slot, i) => (
                    <button 
                      key={slot} 
                      className="btn-equip-slot"
                      onClick={() => handleEquip(item, slot)}
                      title={`Equip to slot ${i + 1}`}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
              )}
              {item.equipped && (
                <span className="equipped-badge">Equipped</span>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function ShopView() {
  const [items] = useState([
    { id: '1', name: 'Golden Duck Cape', price: 500, rarity: 'legendary' },
    { id: '2', name: 'Quack Emote Pack', price: 300, rarity: 'epic' },
    { id: '3', name: 'Duck Wings', price: 450, rarity: 'epic' },
    { id: '4', name: 'Waddle Dance', price: 200, rarity: 'rare' },
    { id: '5', name: 'Pond Aura', price: 350, rarity: 'rare' },
    { id: '6', name: 'Feather Trail', price: 250, rarity: 'rare' },
  ]);

  return (
    <div className="view shop-view">
      <div className="view-header">
        <p style={{ color: 'var(--text-secondary)' }}>
          Get new cosmetics and show off your style!
        </p>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <span style={{ color: 'var(--duck-yellow)', fontWeight: 600 }}>
            <Crown size={18} style={{ marginRight: '0.5rem' }} />
            1,250 Coins
          </span>
        </div>
      </div>

      <div className="shop-grid">
        {items.map(item => (
          <div key={item.id} className="shop-item">
            <div className="shop-item-image">
              <Sparkles size={48} />
            </div>
            <div className="shop-item-content">
              <div className="shop-item-name">{item.name}</div>
              <div className={`cosmetic-rarity ${item.rarity}`}>{item.rarity}</div>
              <div className="shop-item-price">
                <Crown size={16} />
                {item.price} Coins
              </div>
              <button className="btn-buy">Purchase</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function StatsView() {
  return (
    <div className="view stats-view">
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-card-icon"><Clock size={24} /></div>
          <div className="stat-value">24h 32m</div>
          <div className="stat-label">Total Playtime</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-icon"><Target size={24} /></div>
          <div className="stat-value">1,247</div>
          <div className="stat-label">Blocks Placed</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-icon"><Award size={24} /></div>
          <div className="stat-value">12</div>
          <div className="stat-label">Achievements</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-icon"><Server size={24} /></div>
          <div className="stat-value">8</div>
          <div className="stat-label">Servers Joined</div>
        </div>
      </div>

      <div className="settings-section">
        <h2><TrendingUp size={18} /> Recent Activity</h2>
        <div className="activity-list">
          <div className="activity-item">
            <div className="activity-icon"><Award size={16} /></div>
            <div className="activity-content">
              <div className="activity-text">Unlocked <strong>First Steps</strong> achievement</div>
              <div className="activity-time">2 hours ago</div>
            </div>
          </div>
          <div className="activity-item">
            <div className="activity-icon"><Server size={16} /></div>
            <div className="activity-content">
              <div className="activity-text">Joined <strong>HytaleCraft</strong> server</div>
              <div className="activity-time">5 hours ago</div>
            </div>
          </div>
          <div className="activity-item">
            <div className="activity-icon"><Target size={16} /></div>
            <div className="activity-content">
              <div className="activity-text">Placed 500 blocks milestone!</div>
              <div className="activity-time">1 day ago</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function SettingsView() {
  return (
    <div className="view settings-view">
      <div className="settings-section">
        <h2><Monitor size={18} /> Display</h2>
        <div className="setting-row">
          <div className="setting-label">
            <span>Window Mode</span>
            <span>Choose how the game is displayed</span>
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
            <span>Synchronize framerate with monitor</span>
          </div>
          <input type="checkbox" defaultChecked />
        </div>
        <div className="setting-row">
          <div className="setting-label">
            <span>Show FPS Counter</span>
            <span>Display FPS in corner of screen</span>
          </div>
          <input type="checkbox" />
        </div>
      </div>
      
      <div className="settings-section">
        <h2><Volume2 size={18} /> Audio</h2>
        <div className="setting-row">
          <div className="setting-label">
            <span>Master Volume</span>
          </div>
          <div className="ram-slider">
            <input type="range" min={0} max={100} defaultValue={80} />
            <span>80%</span>
          </div>
        </div>
        <div className="setting-row">
          <div className="setting-label">
            <span>Music Volume</span>
          </div>
          <div className="ram-slider">
            <input type="range" min={0} max={100} defaultValue={60} />
            <span>60%</span>
          </div>
        </div>
      </div>

      <div className="settings-section">
        <h2><HardDrive size={18} /> Game</h2>
        <div className="setting-row">
          <div className="setting-label">
            <span>Game Installation Path</span>
            <span>Where Hytale is installed</span>
          </div>
          <div className="path-input">
            <input type="text" placeholder="Not set" readOnly />
            <button className="btn-secondary">Browse</button>
          </div>
        </div>
        <div className="setting-row">
          <div className="setting-label">
            <span>Auto-update Game</span>
            <span>Automatically download game updates</span>
          </div>
          <input type="checkbox" defaultChecked />
        </div>
      </div>
      
      <div className="settings-section">
        <h2><Shield size={18} /> Privacy</h2>
        <div className="setting-row">
          <div className="setting-label">
            <span>Show Online Status</span>
            <span>Let friends see when you're online</span>
          </div>
          <input type="checkbox" defaultChecked />
        </div>
        <div className="setting-row">
          <div className="setting-label">
            <span>Show Current Server</span>
            <span>Let friends see what server you're on</span>
          </div>
          <input type="checkbox" defaultChecked />
        </div>
      </div>
      
      <div className="settings-section">
        <h2><Palette size={18} /> About</h2>
        <div className="setting-row">
          <div className="setting-label">
            <span>Yellow Tale</span>
            <span>Version 0.1.0</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
