// Node 25+ enables its own Web Storage globals by default. Node's localStorage is undefined
// without --localstorage-file and its sessionStorage is a Node Storage (not jsdom's), and both
// shadow the jsdom ones, so code reading localStorage (e.g. the palette search history) breaks.
// Point the globals back at the jsdom window's storage when vitest exposes it.
const g = globalThis as any;
const jsdomWindow = g.jsdom?.window;
if (jsdomWindow) {
  for (const name of ['localStorage', 'sessionStorage', 'Storage']) {
    Object.defineProperty(g, name, { value: jsdomWindow[name], configurable: true, writable: true });
  }
}
