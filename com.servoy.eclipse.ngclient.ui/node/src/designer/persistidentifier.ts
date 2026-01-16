/**
 * Note that this class has a Java side equivalent; meant to identify persists in designer, be it simple components,
 * components nested inside form component components or custom types nested or not inside form component components.
 * 
 * @author acostescu
 */
export class PersistIdentifier {
    
    private constructor(
        /**
         * something like ["D9884DBA_C5E7_4395_A934_52030EB8F1F0", "containedForm", "button_1"] if it's a persist from inside a
         * form component container or ["Z9884DBA_C5E7_4395_A934_52030EB8F1F0"] for a simple persist
         */
        public persistUUIDAndFCPropAndComponentPath: Array<string>,
        
        /** in case it wants to identify a "ghost" (form editor) persist that might be inside form components) */
        public customTypeOrComponentTypePropertyUUIDInsidePersist: string) {}

    /**
     * Writes it as a String in JSON format - that will probably be used client side in svy-id attributes,
     */
    public static fromJSONString(jsonContent: string): PersistIdentifier {
        if (jsonContent == null) return null;

        const firstChar = jsonContent.charAt(0);

        if (firstChar == '{') {
            // const GHOST_IDENTIFIER_INSIDE_COMPONENT = 'g';
            // const COMPONENT_LOCATOR_KEY = 'p';

            const parsed = JSON.parse(jsonContent) as { p: string | Array<string>, g: string };
            const componentLocator: string | Array<string> = parsed.p;
            const ghostIdentifierInsideComp: string = parsed.g;

            if (typeof componentLocator === 'string')
                return new PersistIdentifier([componentLocator], ghostIdentifierInsideComp);

            // if componentLocator is not a String, it can only be a JSONArray of Strings
            return new PersistIdentifier(componentLocator, ghostIdentifierInsideComp);
        } else if (firstChar == '[') {
            return new PersistIdentifier(JSON.parse(jsonContent) as Array<string>, undefined);
        } else return new PersistIdentifier([ jsonContent ], undefined);
    }
    
    /**
     * Returns true if this PersistIdentifier is of a persist that is a direct child of a form component identified by "parentPersistIdentifier".
     * 
     * For example if th identifier is in form component component fCC1 -> containedForm -> fCC2 -> containedForm -> myPersist
     * then this methods should only return true when "parentPersistIdentifier" is fCC1 -> containedForm -> fCC2 but
     * it should return false when "parentPersistIdentifier" is fCC1 or fCC1 -> containedForm -> otherRandomPersist or anotherRandomPersist... 
     */
    public isDirectlyNestedInside(parentPersistIdentifier: PersistIdentifier): boolean {
        if (parentPersistIdentifier.persistUUIDAndFCPropAndComponentPath.length + 2 !== this.persistUUIDAndFCPropAndComponentPath.length) return false;
        
        for (let i = parentPersistIdentifier.persistUUIDAndFCPropAndComponentPath.length - 1; i >= 0; i--) {
            if (parentPersistIdentifier.persistUUIDAndFCPropAndComponentPath[i] !== this.persistUUIDAndFCPropAndComponentPath[i]) return false;
        }
        return true;
    }
    
}