import { describe, it, expect } from 'vitest';
import { Deferred } from './deferred';

describe('Deferred', () => {
    it('should expose a promise', () => {
        const d = new Deferred<number>();
        expect(d.promise).toBeInstanceOf(Promise);
    });

    it('should resolve the promise with the given value', async () => {
        const d = new Deferred<string>();
        d.resolve('done');
        await expect(d.promise).resolves.toBe('done');
    });

    it('should reject the promise with the given reason', async () => {
        const d = new Deferred<string>();
        const reason = new Error('nope');
        d.reject(reason);
        await expect(d.promise).rejects.toBe(reason);
    });
});
