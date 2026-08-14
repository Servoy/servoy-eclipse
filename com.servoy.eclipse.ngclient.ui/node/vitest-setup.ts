const g = typeof globalThis !== 'undefined' ? globalThis : (typeof window !== 'undefined' ? window : global);

if (typeof g.ResizeObserver === 'undefined') {
  (g as any).ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}

// Diagnostic: verify DOM environment is properly initialized
if (typeof document === 'undefined') {
  console.error('[vitest-setup] FATAL: document is undefined — jsdom environment not loaded');
} else if (typeof document.querySelector !== 'function') {
  const keys = Object.keys(document).slice(0, 20);
  const proto = Object.getPrototypeOf(document);
  const protoName = proto?.constructor?.name ?? 'null';
  const protoKeys = proto ? Object.getOwnPropertyNames(proto).slice(0, 10) : [];
  console.error(
    '[vitest-setup] FATAL: document.querySelector is not a function.',
    '\n  typeof document:', typeof document,
    '\n  constructor:', document?.constructor?.name,
    '\n  prototype constructor:', protoName,
    '\n  own keys (first 20):', JSON.stringify(keys),
    '\n  proto keys (first 10):', JSON.stringify(protoKeys),
    '\n  JSON.stringify (first 200):', JSON.stringify(document)?.slice(0, 200),
    '\n  typeof window:', typeof window,
    '\n  window === globalThis:', (typeof window !== 'undefined' && window === globalThis),
    '\n  window.document === document:', (typeof window !== 'undefined' && (window as any).document === document)
  );
} else if (!document.body) {
  console.error('[vitest-setup] FATAL: document.body is null — DOM not fully initialized');
} else {
  console.log('[vitest-setup] DOM environment OK: document.body exists, querySelector available');
}
