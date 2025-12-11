# Test Prompts - ValueList Properties (All 11+ Properties)

**Purpose**: Comprehensive test prompts for validating all valuelist properties with various combinations and types. Tests follow CREATE -> READ -> UPDATE -> DELETE flow to ensure valuelists exist before operations.

**Audience**: Written from the perspective of a beginner programmer with basic understanding of databases.

**Date Created**: December 11, 2025
**Date Updated**: December 11, 2025 - Fixed to use logical testing order with existing data

**Database**: Uses example_data schema with tables: categories, customers, employees, order_details, orders, products, region, shippers, suppliers, territories

---

## TESTING APPROACH

These prompts follow a logical order:
1. **CREATE** valuelists first (SECTION 1-2)
2. **READ/LIST** valuelists to verify (SECTION 3)
3. **UPDATE** properties on existing valuelists (SECTION 4-11)
4. **DELETE** valuelists at the end (SECTION 12)

---

## SECTION 1: CREATE CUSTOM VALUELISTS (Foundation for Property Testing)

### Test 1.1: Simple Custom ValueList
```
Create a valuelist called "order_status_vl" with these values: New, Processing, Shipped, Delivered, Cancelled
```

**Expected behavior**: AI should create CUSTOM valuelist with the five values.

---

### Test 1.2: Custom ValueList with Encapsulation Property
```
Create a valuelist called "priority_levels_vl" with values: Low, Medium, High, Critical. 
Set it to public encapsulation.
```

**Expected behavior**: AI should create CUSTOM valuelist with `encapsulation: "public"`.

---

### Test 1.3: Custom ValueList with Comment Property
```
Create a valuelist "shirt_sizes_vl" with values: XS, S, M, L, XL, XXL. 
Add a comment "Standard clothing sizes for product catalog".
```

**Expected behavior**: AI should create CUSTOM valuelist with `comment` property.

---

### Test 1.4: Custom ValueList with Deprecated Property
```
Create a valuelist "legacy_status_vl" with values: Active, Inactive. 
Mark it as deprecated with message "Use order_status_vl instead".
```

**Expected behavior**: AI should create CUSTOM valuelist with `deprecated` property.

---

## SECTION 2: CREATE DATABASE VALUELISTS (Foundation for Property Testing)

### Test 2.1: Database ValueList - Simple
```
Create a valuelist "categories_vl" from example_data/categories table showing 
categoryname column for both display and storage.
```

**Expected behavior**: AI should create DATABASE valuelist with dataSource.

---

### Test 2.2: Database ValueList - Display vs Return Different
```
Create a valuelist "customers_vl" from example_data/customers table. 
Show companyname but store customerid.
```

**Expected behavior**: AI should create DATABASE valuelist with displayColumn=companyname, returnColumn=customerid.

---

### Test 2.3: Database ValueList - With Sort Options Property
```
Create a valuelist "sorted_products_vl" from example_data/products table. 
Show productname, store productid. Sort by productname ascending.
```

**Expected behavior**: AI should create DATABASE valuelist with `sortOptions: "productname asc"`.

---

### Test 2.4: Database ValueList - With Lazy Loading Property
```
Create a valuelist "all_customers_vl" from example_data/customers table. 
Show companyname, store customerid. Enable lazy loading.
```

**Expected behavior**: AI should create DATABASE valuelist with `lazyLoading: true`.

---

### Test 2.5: Database ValueList - With Add Empty Value Property
```
Create a valuelist "optional_categories_vl" from example_data/categories table. 
Show categoryname. Always allow empty value selection.
```

**Expected behavior**: AI should create DATABASE valuelist with `addEmptyValue: true`.

---

### Test 2.6: Database ValueList - With Multiple Properties
```
Create a valuelist "suppliers_vl" from example_data/suppliers table. 
Show companyname, store supplierid. Sort by companyname ascending, 
enable lazy loading, and add a comment "Supplier selection dropdown".
```

**Expected behavior**: AI should create DATABASE valuelist with multiple properties: sortOptions, lazyLoading, comment.

---

### Test 2.7: Database ValueList - Employees with Sort
```
Create a valuelist "employees_vl" from example_data/employees table. 
Show lastname, store employeeid. Sort by lastname ascending then firstname ascending.
```

**Expected behavior**: AI should create DATABASE valuelist with `sortOptions: "lastname asc, firstname asc"`.

---

### Test 2.8: Database ValueList - Shippers
```
Create a valuelist "shippers_vl" from example_data/shippers table. 
Show companyname, store shipperid.
```

**Expected behavior**: AI should create DATABASE valuelist.

---

## SECTION 3: LIST AND VERIFY VALUELISTS

### Test 3.1: List All ValueLists
```
Show me all the valuelists in my project.
```

**Expected behavior**: AI should call `getValueLists` and display list showing: order_status_vl, priority_levels_vl, shirt_sizes_vl, legacy_status_vl, categories_vl, customers_vl, sorted_products_vl, all_customers_vl, optional_categories_vl, suppliers_vl, employees_vl, shippers_vl.

---

### Test 3.2: Open Existing Custom ValueList
```
Open the valuelist "order_status_vl" to see its details.
```

**Expected behavior**: AI should call `openValueList` with just name, show the custom values.

---

### Test 3.3: Open Existing Database ValueList
```
Show me the details of the "customers_vl" valuelist.
```

**Expected behavior**: AI should call `openValueList` with just name, show database configuration.

---

## SECTION 4: UPDATE LAZY LOADING PROPERTY

### Test 4.1: Enable Lazy Loading on Existing ValueList
```
Update the "categories_vl" valuelist to enable lazy loading.
```

**Expected behavior**: AI should call `openValueList` with name + properties map containing `lazyLoading: true`.

---

### Test 4.2: Disable Lazy Loading
```
The "all_customers_vl" valuelist has lazy loading. Disable it so values load immediately.
```

**Expected behavior**: AI should update with `lazyLoading: false`.

---

## SECTION 5: UPDATE SORT OPTIONS PROPERTY

### Test 5.1: Add Sort to Existing ValueList
```
Update the "categories_vl" valuelist to sort by categoryname descending.
```

**Expected behavior**: AI should update with `sortOptions: "categoryname desc"`.

---

### Test 5.2: Change Existing Sort
```
The "sorted_products_vl" is sorted ascending. Change it to sort by productname descending.
```

**Expected behavior**: AI should update with `sortOptions: "productname desc"`.

---

### Test 5.3: Multi-Column Sort Update
```
Update "employees_vl" to sort by lastname descending, then firstname ascending.
```

**Expected behavior**: AI should update with `sortOptions: "lastname desc, firstname asc"`.

---

## SECTION 6: UPDATE ADD EMPTY VALUE PROPERTY

### Test 6.1: Add Empty Value Option
```
Update "customers_vl" to allow empty values so users can clear their selection.
```

**Expected behavior**: AI should update with `addEmptyValue: true`.

---

### Test 6.2: Remove Empty Value Option
```
The "optional_categories_vl" allows empty values. Remove this option because 
category should always be set.
```

**Expected behavior**: AI should update with `addEmptyValue: false`.

---

### Test 6.3: Set to "always" Mode
```
Update "shippers_vl" to always show an empty value option.
```

**Expected behavior**: AI should update with `addEmptyValue: "always"` or `addEmptyValue: true`.

---

## SECTION 7: UPDATE ENCAPSULATION PROPERTY

### Test 7.1: Change to Module Scope
```
Change the "customers_vl" valuelist to module scope only.
```

**Expected behavior**: AI should update with `encapsulation: "module"`.

---

### Test 7.2: Change to Hide Scope
```
Update "priority_levels_vl" to hide it from scripting.
```

**Expected behavior**: AI should update with `encapsulation: "hide"`.

---

### Test 7.3: Change Back to Public
```
The "customers_vl" is currently module scope. Change it back to public.
```

**Expected behavior**: AI should update with `encapsulation: "public"`.

---

## SECTION 8: UPDATE DEPRECATED PROPERTY

### Test 8.1: Mark as Deprecated
```
Mark the "shirt_sizes_vl" valuelist as deprecated with message 
"Use clothing_sizes_vl instead - will be removed in v3.0".
```

**Expected behavior**: AI should update with `deprecated` property.

---

### Test 8.2: Update Deprecated Message
```
The "legacy_status_vl" is already deprecated. Change the message to 
"Deprecated - use order_status_vl for all new code".
```

**Expected behavior**: AI should update the `deprecated` property.

---

### Test 8.3: Remove Deprecated Status
```
The "shirt_sizes_vl" was marked deprecated but we're keeping it. 
Remove the deprecation warning.
```

**Expected behavior**: AI should update to clear/remove `deprecated` property.

---

## SECTION 9: UPDATE COMMENT PROPERTY

### Test 9.1: Add Documentation Comment
```
Add documentation to "categories_vl" explaining it shows "Product categories for filtering".
```

**Expected behavior**: AI should update with `comment` property.

---

### Test 9.2: Update Existing Comment
```
The "suppliers_vl" has a comment. Change it to "List of active suppliers only".
```

**Expected behavior**: AI should update with new `comment`.

---

### Test 9.3: Multi-Line Comment
```
Update the "sorted_products_vl" comment to:
"Product selection valuelist
- Sorted alphabetically by name
- Includes all active products
- Used in order entry forms"
```

**Expected behavior**: AI should update with multi-line `comment`.

---

## SECTION 10: UPDATE MULTIPLE PROPERTIES AT ONCE

### Test 10.1: Update Three Properties
```
Update the "employees_vl" valuelist:
- Enable lazy loading
- Add empty value option
- Add comment "Employee selection with optional empty choice"
```

**Expected behavior**: AI should update with three properties in one call.

---

### Test 10.2: Update Four Properties
```
Update "suppliers_vl":
- Sort by companyname descending
- Disable lazy loading
- Set to module encapsulation
- Change comment to "Internal supplier list - module access only"
```

**Expected behavior**: AI should update with four properties in one call.

---

### Test 10.3: Complete Property Overhaul
```
Update "categories_vl" with these changes:
- Sort by categoryname ascending
- Enable lazy loading
- Allow empty values
- Set to public encapsulation
- Add comment "Main product category selector"
```

**Expected behavior**: AI should update with five properties in one call.

---

## SECTION 11: CREATE VALUELISTS WITH MULTIPLE PROPERTIES

### Test 11.1: Database ValueList with All Properties
```
Create a valuelist "regions_vl" from example_data/region table. 
Show regiondescription, store regionid. Sort by regiondescription ascending, 
enable lazy loading, allow empty values, set to public, and add comment 
"Geographic regions for territory management".
```

**Expected behavior**: AI should create DATABASE valuelist with five properties in creation call.

---

### Test 11.2: Custom ValueList with Multiple Properties
```
Create a valuelist "payment_methods_vl" with values: Cash, Credit Card, Debit Card, PayPal, Bank Transfer. 
Set to module scope, add comment "Payment options for orders", and mark as public.
```

**Expected behavior**: AI should create CUSTOM valuelist with multiple properties.

---

## SECTION 12: DELETE VALUELISTS

### Test 12.1: Delete Single ValueList
```
Delete the valuelist "legacy_status_vl".
```

**Expected behavior**: AI should call `deleteValueLists` with array containing one name.

---

### Test 12.2: Delete Multiple ValueLists
```
Delete these valuelists: "shirt_sizes_vl", "priority_levels_vl", "payment_methods_vl".
```

**Expected behavior**: AI should call `deleteValueLists` with array of three names.

---

### Test 12.3: Delete Non-Existent ValueList
```
Delete the valuelist "this_does_not_exist_vl".
```

**Expected behavior**: AI should attempt deletion and report "not found".

---

### Test 12.4: Cleanup All Test ValueLists
```
Delete all these test valuelists: "order_status_vl", "categories_vl", "customers_vl", 
"sorted_products_vl", "all_customers_vl", "optional_categories_vl", "suppliers_vl", 
"employees_vl", "shippers_vl", "regions_vl".
```

**Expected behavior**: AI should call `deleteValueLists` with array of all names.

---

## SECTION 9: FALLBACK VALUELIST PROPERTY

### Test 9.1: Set Fallback ValueList
```
Create a valuelist "preferred_suppliers" from example_data/suppliers filtered 
to preferred ones. Set "all_suppliers" as fallback in case preferred list is empty.
```

**Expected behavior**: AI should create with `fallbackValueListID: "all_suppliers"`.

---

### Test 9.2: Update to Add Fallback
```
The valuelist "active_customers" might be empty sometimes. Set "all_customers" 
as a fallback valuelist.
```

**Expected behavior**: AI should update with `fallbackValueListID: "all_customers"`.

---

### Test 9.3: Remove Fallback
```
Remove the fallback valuelist from "preferred_suppliers" because we always have 
preferred suppliers now.
```

**Expected behavior**: AI should update to remove/clear `fallbackValueListID`.

---

## SECTION 10: VALUE TYPE PROPERTIES (displayValueType & realValueType)

### Test 10.1: Specify Display and Real Value Types
```
Create a valuelist "numeric_codes" from example_data/products showing productname 
(text) but storing productid (integer). Set the types appropriately.
```

**Expected behavior**: AI should create with appropriate `displayValueType` and `realValueType` (TEXT and INTEGER type codes).

**Note**: Type codes are Servoy-specific integers. This tests if AI can handle type properties.

---

### Test 10.2: Both Same Type
```
Make a valuelist "category_codes" storing and displaying categoryid (both integers).
```

**Expected behavior**: AI should create with both types set to INTEGER type code.

---

## SECTION 11: COMPLEX COMBINATIONS - Multiple Properties

### Test 11.1: Comprehensive Database ValueList
```
Create a valuelist "advanced_customers" with these settings:
- Table: example_data/customers
- Display: companyname
- Store: customerid
- Sort by: companyname ascending
- Enable lazy loading
- Allow empty value
- Add comment: "Customer selection with all features enabled"
- Keep public encapsulation
```

**Expected behavior**: AI should create valuelist with all specified properties in one call.

---

### Test 11.2: Update Multiple Properties at Once
```
Update the "product_list" valuelist with these changes:
- Enable lazy loading
- Sort by productname descending
- Allow empty values
- Add comment: "Updated to improve performance"
```

**Expected behavior**: AI should update with multiple properties in properties map.

---

### Test 11.3: Module-Scoped Documented ValueList
```
Create a valuelist "internal_statuses" with values: Pending, Approved, Rejected. 
Set it to module scope, add comment "Internal approval workflow statuses", 
and mark as deprecated with message "Migrate to new approval system by Q2 2026".
```

**Expected behavior**: AI should create CUSTOM valuelist with encapsulation, comment, and deprecated properties.

---

### Test 11.4: Related ValueList with Properties
```
Create a valuelist "customer_orders" using the relation "customers_to_orders". 
Display orderid, enable lazy loading, sort by orderdate descending, 
and add comment "Orders for selected customer".
```

**Expected behavior**: AI should create DATABASE (related) valuelist with properties.

---

### Test 11.5: Global Method ValueList with Metadata
```
Create a valuelist "dynamic_countries" that calls "scopes.globals.getCountries". 
Add comment "Dynamically loaded country list from global method", 
set to public scope.
```

**Expected behavior**: AI should create GLOBAL_METHOD valuelist with comment property.

---

## SECTION 12: PROPERTY VALIDATION & EDGE CASES

### Test 12.1: Invalid Sort Column
```
Create a valuelist from example_data/products sorted by "nonexistent_column asc".
```

**Expected behavior**: AI might warn or create anyway (Servoy validation handles this at runtime).

---

### Test 12.2: Empty Comment
```
Update "status_list" to have an empty comment.
```

**Expected behavior**: AI should update with empty or cleared comment.

---

### Test 12.3: Very Long Comment
```
Add this documentation to "customer_list": [insert 500+ character description]
```

**Expected behavior**: AI should handle long strings appropriately.

---

### Test 12.4: Conflicting Properties
```
Create a custom valuelist with lazy loading enabled.
```

**Expected behavior**: AI should recognize custom valuelists don't use lazy loading (only DATABASE types do) and either skip the property or inform user.

---

## SECTION 13: PROPERTY DISCOVERY

### Test 13.1: Ask About Available Properties
```
What properties can I set on a valuelist?
```

**Expected behavior**: AI should list the 11+ available properties with brief descriptions.

---

### Test 13.2: Get Current Properties
```
Show me all the properties currently set on the "customer_dropdown" valuelist.
```

**Expected behavior**: AI should open the valuelist and display its current property values.

---

### Test 13.3: Compare Properties
```
Compare the properties of "old_status_list" and "status_list_v2" valuelists.
```

**Expected behavior**: AI should retrieve both and show property differences.

---

## TESTING CHECKLIST

Use this checklist to track which property tests have been completed:

**Lazy Loading**: [ ] 1.1, [ ] 1.2, [ ] 1.3

**Sort Options**: [ ] 2.1, [ ] 2.2, [ ] 2.3, [ ] 2.4

**Add Empty Value**: [ ] 3.1, [ ] 3.2, [ ] 3.3, [ ] 3.4

**Use Table Filter**: [ ] 4.1, [ ] 4.2

**Separator**: [ ] 5.1, [ ] 5.2

**Encapsulation**: [ ] 6.1, [ ] 6.2, [ ] 6.3, [ ] 6.4

**Deprecated**: [ ] 7.1, [ ] 7.2, [ ] 7.3

**Comment**: [ ] 8.1, [ ] 8.2, [ ] 8.3

**Fallback**: [ ] 9.1, [ ] 9.2, [ ] 9.3

**Value Types**: [ ] 10.1, [ ] 10.2

**Complex Combinations**: [ ] 11.1, [ ] 11.2, [ ] 11.3, [ ] 11.4, [ ] 11.5

**Validation & Edge Cases**: [ ] 12.1, [ ] 12.2, [ ] 12.3, [ ] 12.4

**Property Discovery**: [ ] 13.1, [ ] 13.2, [ ] 13.3

---

## NOTES FOR TESTERS

**Property Support by Type:**
- **CUSTOM valuelists**: encapsulation, deprecated, comment (NO lazy loading, sort, etc.)
- **DATABASE (table) valuelists**: ALL properties supported
- **DATABASE (related) valuelists**: ALL properties supported
- **GLOBAL_METHOD valuelists**: encapsulation, deprecated, comment, displayValueType, realValueType

**Boolean vs String for addEmptyValue:**
- Accepts: `true`, `false`, `"always"`, `"never"`
- `true` = `"always"`, `false` = `"never"`

**Type Codes (for advanced testing):**
- TEXT: typically 12 or similar
- INTEGER: typically 4
- (Exact codes are Servoy-specific, AI should handle or ask)

**Multiple Properties:**
- Test single property updates
- Test multiple property updates in one call
- Test creating with multiple properties from start

**Error Scenarios:**
- Invalid fallback valuelist name (doesn't exist)
- Invalid encapsulation value
- Invalid sort column name

---

**End of ValueList Properties Test Prompts**

*Total Tests: 40+ comprehensive property tests*
*Coverage: All 11+ valuelist properties + combinations + edge cases*
