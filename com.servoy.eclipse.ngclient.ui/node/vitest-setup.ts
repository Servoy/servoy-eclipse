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
  console.error('[vitest-setup] FATAL: document.querySelector is not a function. document type:', typeof document, 'constructor:', document?.constructor?.name);
} else if (!document.body) {
  console.error('[vitest-setup] FATAL: document.body is null — DOM not fully initialized');
} else {
  console.log('[vitest-setup] DOM environment OK: document.body exists, querySelector available');
}
