const g = typeof globalThis !== 'undefined' ? globalThis : (typeof window !== 'undefined' ? window : global);

if (typeof g.ResizeObserver === 'undefined') {
  (g as any).ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}
