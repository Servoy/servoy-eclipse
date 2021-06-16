
export interface WebStorage {

    prefix: string;

    /**
     * Test the client's support for storing values in the web store.
     *
     * @return {boolean} True if the client has support for the web store, else false.
     */
    isSupported(): boolean;

    /**
     * Add or update the specified key/value pair in the web store.
     *
     * @param {string} key The name to store the value under.
     * @param {mixed} value The value to set (all values are stored as JSON.)
     * @return {boolean} True on success, else false.
     */
    set(key: string, value: any): boolean;

   /**
    * Getter for the key/value web store.
    *
    * @param {string} key Name of the value to retrieve.
    * @return {mixed} The value previously added under the specified key, else null.
    */
    get(key: string): any;

    /**
     * Check if a key exists.
     *
     * @param {string} key Name of the key to test.
     * @return {boolean} True if the key exists, else false.
     */
    has(key: string): boolean;

    /**
     * Remove a specified value from the key/value web store.
     *
     * @param {string} key Name of the value to remove.
     * @return {boolean} True on success, else false.
     */
    remove(key: string): boolean;

    clear(): boolean;

    /**
     * Returns an integer representing the number of items stored
     * in the key/value web store.
     *
     * @return {number} The number of items currently stored in
     *   the key/value web store.
     */
    length(): number;

	/**
	 * Return the name of the nth key in the key/value web store.
	 *
	 * @param {number} index An integer representing the number of the key to
	 *   the return the name of.
	 * @return {string|null} The name of the key if available or null otherwise.
	 */
    key(index: number): string;

}
