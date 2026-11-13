import { describe, it, expect } from 'vitest';
import { ServoyApiTesting } from './servoyapi_testing';

describe('ServoyApiTesting', () => {
    const api = new ServoyApiTesting();

    it('formWillShow resolves true', async () => {
        await expect(api.formWillShow('f')).resolves.toBe(true);
    });

    it('hideForm resolves true', async () => {
        await expect(api.hideForm('f')).resolves.toBe(true);
    });

    it('callServerSideApi resolves null', async () => {
        await expect(api.callServerSideApi('m', [])).resolves.toBeNull();
    });

    it('returns the fixed designer / layout / markup / form values', () => {
        expect(api.isInDesigner()).toBe(false);
        expect(api.hasSampleData()).toBe(false);
        expect(api.trustAsHtml()).toBe(false);
        expect(api.isInAbsoluteLayout()).toBe(true);
        expect(api.getMarkupId()).toBe('testid');
        expect(api.getFormName()).toBe('testform');
    });

    it('getClientProperty echoes the key', () => {
        expect(api.getClientProperty('foo')).toBe('foo');
    });

    it('startEdit / apply / register / unregister are inert and do not throw', () => {
        expect(() => {
            api.startEdit('dp');
            api.apply('dp', 1);
            api.registerComponent({} as any);
            api.unRegisterComponent({} as any);
        }).not.toThrow();
    });
});
