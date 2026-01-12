use crate::bridge::GameEvent;
use parking_lot::RwLock;
use std::collections::HashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};
use tokio::sync::broadcast;
use tracing::debug;

pub type EventHandler = Arc<dyn Fn(GameEvent) + Send + Sync>;

pub struct EventBus {
    sender: broadcast::Sender<GameEvent>,
    handlers: RwLock<HashMap<String, Vec<(u64, EventHandler)>>>,
    handler_counter: AtomicU64,
    event_count: AtomicU64,
}

impl EventBus {
    pub fn new() -> Self {
        Self::with_capacity(10000)
    }

    pub fn with_capacity(capacity: usize) -> Self {
        let (sender, _) = broadcast::channel(capacity);
        Self {
            sender,
            handlers: RwLock::new(HashMap::new()),
            handler_counter: AtomicU64::new(0),
            event_count: AtomicU64::new(0),
        }
    }

    pub async fn emit(&self, event: GameEvent) {
        self.event_count.fetch_add(1, Ordering::Relaxed);
        
        let event_name = event.event_name();
        debug!("Event emitted: {}", event_name);
        
        let handlers = self.handlers.read();
        if let Some(handler_list) = handlers.get(event_name) {
            for (_, handler) in handler_list {
                let event_clone = event.clone();
                let handler_clone = handler.clone();
                tokio::spawn(async move {
                    handler_clone(event_clone);
                });
            }
        }
        
        if let Some(wildcard_handlers) = handlers.get("*") {
            for (_, handler) in wildcard_handlers {
                let event_clone = event.clone();
                let handler_clone = handler.clone();
                tokio::spawn(async move {
                    handler_clone(event_clone);
                });
            }
        }
        
        let _ = self.sender.send(event);
    }

    pub fn subscribe(&self) -> broadcast::Receiver<GameEvent> {
        self.sender.subscribe()
    }

    pub fn on<F>(&self, event_name: &str, handler: F) -> u64
    where
        F: Fn(GameEvent) + Send + Sync + 'static,
    {
        let id = self.handler_counter.fetch_add(1, Ordering::Relaxed);
        let handler_arc: EventHandler = Arc::new(handler);
        
        let mut handlers = self.handlers.write();
        handlers.entry(event_name.to_string())
            .or_insert_with(Vec::new)
            .push((id, handler_arc));
        
        id
    }

    pub fn on_all<F>(&self, handler: F) -> u64
    where
        F: Fn(GameEvent) + Send + Sync + 'static,
    {
        self.on("*", handler)
    }

    pub fn off(&self, handler_id: u64) -> bool {
        let mut handlers = self.handlers.write();
        for (_, handler_list) in handlers.iter_mut() {
            if let Some(pos) = handler_list.iter().position(|(id, _)| *id == handler_id) {
                handler_list.remove(pos);
                return true;
            }
        }
        false
    }

    pub fn off_all(&self, event_name: &str) {
        let mut handlers = self.handlers.write();
        handlers.remove(event_name);
    }

    pub fn clear(&self) {
        self.handlers.write().clear();
    }

    pub fn event_count(&self) -> u64 {
        self.event_count.load(Ordering::Relaxed)
    }

    pub fn handler_count(&self) -> usize {
        self.handlers.read().values()
            .map(|v| v.len())
            .sum()
    }
}

impl Default for EventBus {
    fn default() -> Self {
        Self::new()
    }
}
