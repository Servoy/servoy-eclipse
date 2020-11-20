import { Injectable } from '@angular/core';
import { WebStorage } from './webstorage.interface';

@Injectable()
export class SessionStorageService implements WebStorage {

    hasSessionStorage: boolean;
    prefix = '';

    constructor() {
        this.hasSessionStorage = this.isSupported();
    }

    isSupported(): boolean {
        try {
			sessionStorage.setItem(this.prefix + 'key', 'value');
			sessionStorage.removeItem(this.prefix + 'key');
			return true;
		} catch (e) {
			return false;
		}
    }

    set(key: string, value: any): boolean {
        if (this.hasSessionStorage) {
			try {
				sessionStorage.setItem(this.prefix + key, JSON.stringify(value));
			} catch (e) {
                console.log(e);
                return false;
			}
			return true;
		}
		return false;
    }

    get(key: string) {
        if (this.hasSessionStorage) {
			try {
				const value = sessionStorage.getItem(this.prefix + key);
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
        if (this.hasSessionStorage) {
			try {
				sessionStorage.removeItem(this.prefix + key);
			} catch (e) {
                console.log(e);
                return false;
			}
			return true;
		}
		return false;
    }

    clear(): boolean {
        if (!this.hasSessionStorage) return false;
		if (!!this.prefix) {
			const prefixLength = this.prefix.length;
			try {
				for (const key in sessionStorage) {
					if (key.substr(0, prefixLength) === this.prefix) {
						sessionStorage.removeItem(key);
					}
				}
			} catch (e) {
                console.log(e);
                return false;
			}
			return true;
		}

		try {
			sessionStorage.clear();
		} catch (e) {
            console.log(e);
            return false;
		}

		return true;
    }

    key(index: number) {
        if (this.hasSessionStorage) {
			return localStorage.key(index);
		}
		return null;
    }

    length(): number {
        if (this.hasSessionStorage) {
			return sessionStorage.length;
		}
		return 0;
    }

    changePrefix(newPrefix: string) {
        this.prefix = newPrefix;
    }
}
