import { Injectable } from '@angular/core';
import { WebStorage } from './webstorage.interface';

@Injectable()
export class LocalStorageService implements WebStorage {

    hasLocalStorage: boolean;
    prefix = '';

    constructor() {
        this.hasLocalStorage = this.isSupported();
    }

    isSupported(): boolean {
        try {
			localStorage.setItem(this.prefix + 'key', 'value');
			localStorage.removeItem(this.prefix + 'key');
			return true;
		} catch (e) {
			return false;
		}
    }

    set(key: string, value: any): boolean {
        if (this.hasLocalStorage) {
			try {
				localStorage.setItem(this.prefix + key, JSON.stringify(value));
			} catch (e) {
                console.log(e);
				return false;
			}
			return true;
		}
		return false;
    }

    get(key: string) {
        if (this.hasLocalStorage) {
			try {
				const value = localStorage.getItem(this.prefix + key);
				return value && JSON.parse(value);
			} catch (e) {
				console.log(e);
				return null;
			}
		}
		return null;
    }

    has(key: string): boolean {
        return null !== this.get(key);
    }

    remove(key: string): boolean {
        if (this.hasLocalStorage) {
			try {
				localStorage.removeItem(this.prefix + key);
			} catch (e) {
                console.log(e);
				return false;
			}
			return true;
		}
		return false;
    }

    clear(): boolean {
        if (!this.hasLocalStorage) return false;
		if (!!this.prefix) {
			const prefixLength = this.prefix.length;
			try {
				for (const key in localStorage) {
					if (key.substr(0, prefixLength) === this.prefix) {
						localStorage.removeItem(key);
					}
				}
			} catch (e) {
                console.log(e);
				return false;
			}
			return true;
		}

		try {
			localStorage.clear();
		} catch (e) {
            console.log(e);
            return false;
		}

		return true;
    }

    key(index: number) {
        if (this.hasLocalStorage) {
			return localStorage.key(index);
		}
		return null;
    }

    changePrefix(newPrefix: string) {
        this.prefix = newPrefix;
    }

    length(): number {
        if (this.hasLocalStorage) {
			return localStorage.length;
		}
		return 0;
    }
}
