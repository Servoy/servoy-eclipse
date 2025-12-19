# Test Prompts - Relations and ValueLists

**Purpose**: Test prompts for validating relation and valuelist creation, updating, and deletion with various parameter combinations.

**Audience**: Written from the perspective of a beginner programmer with basic understanding of databases.

**Date Created**: December 11, 2025
**Date Updated**: December 11, 2025 (with actual example_data schema)

---

## DATABASE SCHEMA OVERVIEW

These prompts use the **example_data** database server with the following schema:

**Main Tables:**
- **customers** (customerid PK, companyname, contactname, city, country, etc.)
- **orders** (orderid PK, customerid FK, employeeid FK, orderdate, shipvia FK, etc.)
- **order_details** (orderid+productid composite PK, unitprice, quantity, discount)
- **products** (productid PK, productname, supplierid FK, categoryid FK, unitprice, etc.)
- **categories** (categoryid PK, categoryname, description)
- **suppliers** (supplierid PK, companyname, contactname, city, country, etc.)
- **employees** (employeeid PK, lastname, firstname, title, reportsto self-FK)
- **shippers** (shipperid PK, companyname, phone)
- **territories** (territoryid PK, territorydescription, regionid FK)
- **region** (regionid PK, regiondescription)

**Key Relationships:**
- customers --> orders (customerid)
- orders --> order_details (orderid)
- products --> order_details (productid)
- categories --> products (categoryid)
- suppliers --> products (supplierid)
- employees --> orders (employeeid)
- employees --> employees self-reference (reportsto)
- shippers --> orders (shipvia references shipperid)
- region --> territories (regionid)

---

## SETUP INSTRUCTIONS

Before running these prompts, ensure you have:
1. An active Servoy solution project open
2. A database server named **example_data** configured in Servoy Developer
3. The example_data database imported (schema provided in doc/example_data file)

---

## SECTION 1: RELATIONS - BASIC CREATION

### Test 1.1: Simple Relation with Minimal Parameters
```
I need to create a relation called "customers_to_orders" that links the customers 
table to the orders table. Both tables are in the example_data database. 
The link is on customerid column in both tables.
```

**Expected behavior**: AI should create relation: db:/example_data/customers to db:/example_data/orders on customerid with default properties (left outer join, allow creation).

---

### Test 1.2: Relation via Order Details Junction Table
```
Create a relation called "orders_to_order_details" from the orders table to 
order_details table in example_data database. They link on orderid.
```

**Expected behavior**: AI should create relation: db:/example_data/orders to db:/example_data/order_details on orderid.

---

### Test 1.3: Relation Discovery First
```
I have a database server called example_data. Can you show me what relations 
I could create based on the foreign keys?
```

**Expected behavior**: AI should use `discoverDbRelations` and show POTENTIAL relations (note: example_data has no explicit FKs defined, only implicit relationships via column names).

---

### Test 1.4: Products to Categories Relation
```
Create a relation from products to categories. Both tables are in example_data. 
Link them on categoryid.
```

**Expected behavior**: AI should create relation: db:/example_data/products to db:/example_data/categories on categoryid.

---

## SECTION 2: RELATIONS - CREATION WITH PROPERTIES

### Test 2.1: Relation with Inner Join
```
Create a relation called "orders_to_customers_inner" linking example_data/orders 
to example_data/customers on customerid. Make it an inner join because I only 
want orders that have valid customers.
```

**Expected behavior**: AI should create relation with `joinType: "inner"` property.

---

### Test 2.2: Relation with Cascade Delete
```
I need a relation from customers to orders in example_data where if I delete a 
customer, all their orders get deleted too. Call it "customers_to_orders_cascade". 
Link on customerid.
```

**Expected behavior**: AI should create relation with `deleteRelatedRecords: true` property.

---

### Test 2.3: Relation with Sorting
```
Create a relation "customers_to_orders_sorted" from example_data/customers to 
example_data/orders on customerid. I want the orders to always be sorted by 
orderdate descending.
```

**Expected behavior**: AI should create relation with `initialSort: "orderdate desc"` property.

---

### Test 2.4: Relation with Multiple Properties
```
Create relation "products_to_suppliers" from example_data/products to 
example_data/suppliers on supplierid. Make it:
- Inner join (only products with suppliers)
- Allow creating new supplier records
- Sort by companyname ascending
- Add a comment "Links products to their suppliers"
```

**Expected behavior**: AI should create relation with multiple properties:
- `joinType: "inner"`
- `allowCreationRelatedRecords: true`
- `initialSort: "companyname asc"`
- `comment: "Links products to their suppliers"`

---

### Test 2.5: Relation with Encapsulation
```
Create a relation called "employees_supervisor" from example_data/employees to 
example_data/employees on reportsto (linking to employeeid). This should be 
module scope only because it's internal HR functionality.
```

**Expected behavior**: AI should create self-referencing relation with `encapsulation: "module"` property.

---

## SECTION 3: RELATIONS - UPDATING

### Test 3.1: Update Single Property
```
I have a relation called "customers_to_orders". Can you change it to be an 
inner join instead of the default left outer join?
```

**Expected behavior**: AI should call `openRelation` with just name and properties map containing `joinType: "inner"`.

---

### Test 3.2: Update Multiple Properties
```
Update the "products_to_categories" relation:
- Make it allow creation of related records
- Add sorting by category_name ascending
- Add comment "Product categorization relation"
```

**Expected behavior**: AI should update the relation with multiple properties in one call.

---

### Test 3.3: Deprecate a Relation
```
The relation "old_customer_relation" is outdated. Mark it as deprecated with 
a message saying "Use customers_to_orders_v2 instead".
```

**Expected behavior**: AI should update relation with `deprecated` property.

---

### Test 3.4: Update Encapsulation
```
Change the "products_to_suppliers" relation to be hidden from scripting because 
we want to force developers to use our API instead.
```

**Expected behavior**: AI should update relation with `encapsulation: "hide"` property.

---

## SECTION 4: RELATIONS - DELETION

### Test 4.1: Delete Single Relation
```
Delete the relation called "temp_test_relation".
```

**Expected behavior**: AI should call `deleteRelations` with array containing one name.

---

### Test 4.2: Delete Multiple Relations
```
I need to delete these old test relations: "test_rel1", "test_rel2", and "temp_relation".
```

**Expected behavior**: AI should call `deleteRelations` with array of all three names.

---

### Test 4.3: Delete Non-Existent Relation
```
Delete the relation "this_does_not_exist".
```

**Expected behavior**: AI should attempt deletion and report "not found" for this relation.

---

## SECTION 5: RELATIONS - LISTING AND DISCOVERY

### Test 5.1: List All Relations
```
Show me all the relations in my project.
```

**Expected behavior**: AI should call `getRelations` and display the list.

---

### Test 5.2: Open Existing Relation
```
Open the relation called "customers_to_orders" so I can see its details.
```

**Expected behavior**: AI should call `openRelation` with just the name parameter.

---

## SECTION 6: VALUELISTS - CUSTOM TYPE (CREATE)

### Test 6.1: Simple Custom ValueList
```
Create a valuelist called "vl_status" with these values: New, Active, Inactive, Archived
```

**Expected behavior**: AI should create CUSTOM valuelist with the four values.

---

### Test 6.2: Custom ValueList with More Values
```
I need a valuelist for shirt sizes: XS, S, M, L, XL, XXL, XXXL
Name it "vl_shirt_sizes".
```

**Expected behavior**: AI should create CUSTOM valuelist preserving order and casing.

---

### Test 6.3: Custom ValueList - Priority Levels
```
Make a valuelist called "vl_priority" with these values: Low, Medium, High, Critical
```

**Expected behavior**: AI should parse the values and create CUSTOM valuelist.

---

### Test 6.4: Custom ValueList with Special Characters
```
Create a valuelist "vl_payment_methods" with: Cash, Credit Card, Debit Card, PayPal, Bank Transfer
```

**Expected behavior**: AI should handle values with spaces correctly.

---

### Test 6.5: Custom ValueList - Order Status
```
Create a valuelist "vl_order_status" with values: Pending, Processing, Shipped, Delivered, Cancelled
```

**Expected behavior**: AI should create CUSTOM valuelist with five values.

---

## SECTION 7: VALUELISTS - DATABASE TYPE (TABLE) - CREATE

### Test 7.1: Database ValueList - Single Column
```
Create a valuelist called "vl_categories" from the categories table in example_data. 
Use the categoryname column for both display and storage.
```

**Expected behavior**: AI should create DATABASE (table) valuelist with dataSource=db:/example_data/categories, displayColumn=categoryname.

---

### Test 7.2: Database ValueList - Display and Return Different
```
I need a valuelist from the customers table in example_data showing company names 
but storing customer IDs. Call it "vl_customers". Use companyname for display 
and customerid for storage.
```

**Expected behavior**: AI should create DATABASE valuelist with displayColumn=companyname, returnColumn=customerid.

---

### Test 7.3: Database ValueList - Products
```
Create a valuelist "vl_products" from example_data/products table. 
Show productname, store productid.
```

**Expected behavior**: AI should create DATABASE valuelist from products table.

---

### Test 7.4: Database ValueList - With Missing Server Name (Error Test)
```
Create a valuelist from the products table, showing productname.
```

**Expected behavior**: AI should ask for the database server name.

---

### Test 7.5: Database ValueList - With Table Discovery
```
I want to create a valuelist from my database server example_data, but I'm 
not sure which table to use. Can you show me what tables are available?
```

**Expected behavior**: AI should use `listTables` to show available tables: categories, customers, employees, orders, order_details, products, suppliers, shippers, region, territories.

---

### Test 7.6: Database ValueList - With Column Discovery
```
Create a valuelist from the employees table in example_data. I'm not sure 
which columns are available though.
```

**Expected behavior**: AI should use `getTableInfo` to show available columns (employeeid, lastname, firstname, title, etc.), then ask which to use for display/return.

---

### Test 7.7: Database ValueList - Suppliers
```
Create a valuelist "vl_suppliers" from example_data/suppliers table. 
Show companyname, store supplierid.
```

**Expected behavior**: AI should create DATABASE valuelist from suppliers table.

---

### Test 7.8: Database ValueList - Employees
```
Create a valuelist "vl_employees" from example_data/employees table. 
Show lastname, store employeeid.
```

**Expected behavior**: AI should create DATABASE valuelist from employees table.

---

### Test 7.9: Database ValueList - Shippers
```
Create a valuelist "vl_shippers" from example_data/shippers table. 
Show companyname, store shipperid.
```

**Expected behavior**: AI should create DATABASE valuelist from shippers table.

---

### Test 7.10: Database ValueList - With Properties
```
Create a valuelist "vl_products_sorted" from example_data/products table. 
Show productname, store productid. Sort it by productname ascending 
and use lazy loading because it's a big table.
```

**Expected behavior**: AI should create DATABASE valuelist with sortOptions and lazyLoading properties.

---

## SECTION 8: VALUELISTS - DATABASE TYPE (RELATED) - CREATE

### Test 8.1: Related ValueList - Check Prerequisites First
```
Can you list all the relations in my project? I want to create a related valuelist.
```

**Expected behavior**: AI should call `getRelations` to show available relations (likely empty unless relations were created in SECTION 1-5).

---

### Test 8.2: Related ValueList - Create Relation First, Then ValueList
```
First, create a relation "rel_customers_to_orders" from example_data/customers 
to example_data/orders on customerid. Then create a valuelist "vl_customer_orders" 
that uses this relation to show orderid.
```

**Expected behavior**: AI should create relation first, then create DATABASE (related) valuelist using that relation.

---

### Test 8.3: Related ValueList - Using Existing Relation
```
If the relation "rel_products_to_categories" exists, create a valuelist 
"vl_product_categories" that uses it to display categoryname and store categoryid.
```

**Expected behavior**: AI should check if relation exists first using `getRelations`, then create valuelist if relation is found, or inform user if not found.

---

## SECTION 9: VALUELISTS - GLOBAL METHOD TYPE (CREATE)

### Test 9.1: Global Method ValueList
```
Create a valuelist called "vl_dynamic_countries" that gets its values from the 
global method "scopes.globals.getCountries".
```

**Expected behavior**: AI should create GLOBAL_METHOD valuelist with the method name.

---

### Test 9.2: Global Method ValueList - With Scope
```
I have a method in my globals scope called "getActiveUsers". Create a valuelist 
"vl_active_users" that calls this method.
```

**Expected behavior**: AI should create GLOBAL_METHOD valuelist with "scopes.globals.getActiveUsers".

---

## SECTION 10: VALUELISTS - LISTING (READ)

### Test 10.1: List All ValueLists
```
Show me all the valuelists in my project.
```

**Expected behavior**: AI should call `getValueLists` and display list showing all valuelists created so far: vl_status, vl_shirt_sizes, vl_priority, vl_payment_methods, vl_order_status, vl_categories, vl_customers, vl_products, vl_suppliers, vl_employees, vl_shippers, vl_products_sorted, and any related/global method valuelists.

---

### Test 10.2: Open Existing Custom ValueList
```
Open the valuelist "vl_status" to see its details.
```

**Expected behavior**: AI should call `openValueList` with just the name parameter, show custom values.

---

### Test 10.3: Open Existing Database ValueList
```
Show me the details of the "vl_customers" valuelist.
```

**Expected behavior**: AI should call `openValueList` with just the name parameter, show database configuration.

---

## SECTION 11: VALUELISTS - UPDATING (UPDATE)

### Test 11.1: Update Custom Values - Add Value
```
The valuelist "vl_status" currently has New, Active, Inactive, Archived. 
Can you add "Pending" to it?
```

**Expected behavior**: AI should understand this requires recreating with new customValues array including all five values.

---

### Test 11.2: Update Properties - Add Empty Value
```
Update the "vl_categories" valuelist to allow empty values so users can clear 
their selection.
```

**Expected behavior**: AI should call `openValueList` with properties containing `addEmptyValue: true`.

---

### Test 11.3: Update Properties - Change Sorting
```
The "vl_products_sorted" valuelist needs to be sorted by productname descending instead 
of ascending. Can you change that?
```

**Expected behavior**: AI should update with `sortOptions: "productname desc"` property.

---

### Test 11.4: Update Multiple Properties
```
Update "vl_customers":
- Enable lazy loading
- Add empty value option
- Sort by companyname ascending
- Set comment "Customer selection dropdown"
```

**Expected behavior**: AI should update with multiple properties in one call.

---

### Test 11.5: Deprecate ValueList
```
Mark the "vl_shirt_sizes" as deprecated with message "Use vl_clothing_sizes instead".
```

**Expected behavior**: AI should update with `deprecated` property.

---

### Test 11.6: Change Encapsulation
```
Make the "vl_payment_methods" valuelist module scope only.
```

**Expected behavior**: AI should update with `encapsulation: "module"` property.

---

### Test 11.7: Update Comment
```
Add documentation to "vl_products" explaining it shows "All active products for order entry".
```

**Expected behavior**: AI should update with `comment` property.

---

## SECTION 12: VALUELISTS - DELETION (DELETE)

### Test 12.1: Delete Single ValueList
```
Delete the valuelist "vl_shirt_sizes".
```

**Expected behavior**: AI should call `deleteValueLists` with array containing one name, report success.

---

### Test 12.2: Delete Multiple ValueLists
```
Remove these valuelists: "vl_priority", "vl_payment_methods", "vl_order_status".
```

**Expected behavior**: AI should call `deleteValueLists` with array of three names, report success for each.

---

### Test 12.3: Delete Non-Existent ValueList (Error Test)
```
Delete the valuelist "vl_does_not_exist".
```

**Expected behavior**: AI should attempt deletion and report "not found".

---

### Test 12.4: Cleanup Remaining Test ValueLists
```
Delete all remaining test valuelists: "vl_status", "vl_categories", "vl_customers", 
"vl_products", "vl_suppliers", "vl_employees", "vl_shippers", "vl_products_sorted".
```

**Expected behavior**: AI should call `deleteValueLists` with array of all names, report results.

---

## SECTION 13: COMPLEX SCENARIOS

### Test 13.1: Discover Relations and Create ValueList from One
```
I have a database server called example_data. Can you show me what relations 
I could create based on foreign keys? Then I want to make a valuelist using one of those relations.
```

**Expected behavior**: AI should use `discoverDbRelations` with serverName=example_data, show potential relations, then help create a related valuelist (requires creating relation first).

---

### Test 13.2: Create Relation Then Use It in ValueList
```
First, create a relation "rel_products_to_suppliers" linking example_data/products 
to example_data/suppliers on supplierid. Then create a valuelist called 
"vl_product_suppliers" that uses this relation to show companyname and store supplierid.
```

**Expected behavior**: AI should create relation first, then create related valuelist using it.

---

### Test 13.3: Update Both Relation and ValueList
```
If the relation "rel_customers_to_orders" and valuelist "vl_customer_orders" exist, 
update the relation to sort by orderdate desc, and update the valuelist 
to enable lazy loading.
```

**Expected behavior**: AI should check existence first, then update both the relation and valuelist with their respective properties.

---

### Test 13.4: Partial Information - AI Should Ask
```
Create a valuelist for me that shows product information.
```

**Expected behavior**: AI should ask what type (custom/database/related/global method), what specific data, which table/columns, etc.

---

### Test 13.5: Ambiguous Request - AI Should Clarify
```
I need a dropdown for regions.
```

**Expected behavior**: AI should ask for server name (example_data), confirm table name (region), ask about columns (regionid, regiondescription).

---

### Test 13.6: Complete Workflow - Discover, Create Relation, Create ValueList
```
I want to create a dropdown showing supplier information. Can you help me set this up 
using the example_data database?
```

**Expected behavior**: AI should:
1. Ask if user wants a direct database valuelist or related valuelist
2. If direct: Create DATABASE valuelist from suppliers table
3. If related: Ask which table to relate from, create relation, then create related valuelist
4. Ask about display/return columns
5. Ask about properties (sorting, lazy loading, etc.)

---

## SECTION 14: ERROR HANDLING

### Test 14.1: Invalid DataSource Format
```
Create a valuelist from "countries" table showing country_name.
```

**Expected behavior**: AI should ask for server name to construct proper dataSource format.

---

### Test 14.2: Missing Required Parameters
```
Create a relation called "test_relation".
```

**Expected behavior**: AI should ask for primary datasource, foreign datasource, and columns.

---

### Test 14.3: Conflicting Parameters
```
Create a valuelist with both custom values (Active, Inactive) and from the 
status table in my database.
```

**Expected behavior**: AI should recognize conflict and ask which type user wants.

---

## SECTION 15: PROPERTIES COMBINATIONS

### Test 15.1: Relation with All Properties
```
Create a relation "rel_orders_to_customers_full" from example_data/orders to 
example_data/customers on customerid with these settings:
- Inner join
- Allow creating related records
- Don't allow parent delete when having related records
- Enable cascade delete of related records
- Sort by companyname ascending, then contactname ascending
- Module scope only
- Deprecated: "For testing only - use rel_orders_to_customers instead"
- Comment: "Test relation with all properties configured for comprehensive testing"
```

**Expected behavior**: AI should create relation with all 8 properties set: joinType="inner", allowCreationRelatedRecords=true, allowParentDeleteWhenHavingRelatedRecords=false, deleteRelatedRecords=true, initialSort="companyname asc, contactname asc", encapsulation="module", deprecated message, comment.

---

### Test 15.2: ValueList with All Properties
```
Create a valuelist "vl_products_complete" from example_data/products showing 
productname and storing productid with these settings:
- Lazy loading enabled
- Sort by productname ascending
- Allow empty value
- Add comment "Complete product list for testing all valuelist properties"
- Public encapsulation
```

**Expected behavior**: AI should create valuelist with multiple properties set: lazyLoading=true, sortOptions="productname asc", addEmptyValue=true, comment, encapsulation="public".

---

### Test 15.3: Custom ValueList with Multiple Properties
```
Create a custom valuelist "vl_status_complete" with values: Draft, Active, Pending, Completed, Cancelled, Archived. 
Set these properties:
- Module encapsulation (internal use only)
- Add comment "Complete status workflow - all possible states"
- Deprecated: "Phase out in v3.0 - use vl_workflow_status instead"
```

**Expected behavior**: AI should create CUSTOM valuelist with encapsulation="module", comment, and deprecated properties all set during creation.

---

## TESTING CHECKLIST

Use this checklist to track which prompts have been tested:

**Relations - Basic Creation**: [ ] 1.1, [ ] 1.2, [ ] 1.3, [ ] 1.4

**Relations - Creation With Properties**: [ ] 2.1, [ ] 2.2, [ ] 2.3, [ ] 2.4, [ ] 2.5

**Relations - Updating**: [ ] 3.1, [ ] 3.2, [ ] 3.3, [ ] 3.4

**Relations - Deletion**: [ ] 4.1, [ ] 4.2, [ ] 4.3

**Relations - Listing**: [ ] 5.1, [ ] 5.2

**ValueLists - Custom (CREATE)**: [ ] 6.1, [ ] 6.2, [ ] 6.3, [ ] 6.4, [ ] 6.5

**ValueLists - Database Table (CREATE)**: [ ] 7.1, [ ] 7.2, [ ] 7.3, [ ] 7.4, [ ] 7.5, [ ] 7.6, [ ] 7.7, [ ] 7.8, [ ] 7.9, [ ] 7.10

**ValueLists - Database Related (CREATE)**: [ ] 8.1, [ ] 8.2, [ ] 8.3

**ValueLists - Global Method (CREATE)**: [ ] 9.1, [ ] 9.2

**ValueLists - Listing (READ)**: [ ] 10.1, [ ] 10.2, [ ] 10.3

**ValueLists - Updating (UPDATE)**: [ ] 11.1, [ ] 11.2, [ ] 11.3, [ ] 11.4, [ ] 11.5, [ ] 11.6, [ ] 11.7

**ValueLists - Deletion (DELETE)**: [ ] 12.1, [ ] 12.2, [ ] 12.3, [ ] 12.4

**Complex Scenarios**: [ ] 13.1, [ ] 13.2, [ ] 13.3, [ ] 13.4, [ ] 13.5, [ ] 13.6

**Error Handling**: [ ] 14.1, [ ] 14.2, [ ] 14.3

**Properties Combinations**: [ ] 15.1, [ ] 15.2, [ ] 15.3

---

## NOTES FOR TESTERS

1. **Testing Order**: IMPORTANT - Follow the section order for valuelists:
   - SECTION 6-9: CREATE valuelists first
   - SECTION 10: List and verify they exist
   - SECTION 11: UPDATE properties on existing valuelists
   - SECTION 12: DELETE valuelists at the end
   - Do NOT try to update/delete valuelists before creating them

2. **Database Schema**: All tests use the example_data database server with real tables: categories, customers, employees, order_details, orders, products, region, shippers, suppliers, territories.

3. **Expected Behavior**: The AI should follow the 6 CRITICAL RULES from copilot-instructions.md:
   - RULE 1: Respect cancellations immediately
   - RULE 2: Ask for missing parameters, never guess
   - RULE 3: Respect user intent
   - RULE 4: Announce tools before calling
   - RULE 5: Never edit .frm files directly
   - RULE 6: Only use specified tools, no file system tools

4. **Parameter Guessing**: The AI should NEVER guess serverName, tableName, or columnName. It should always ASK.

5. **Tool Announcements**: Before each tool call, AI should clearly state which tool it's calling and with what parameters.

6. **Success Criteria**: 
   - Relation/valuelist created successfully
   - Auto-save completed
   - Properties applied correctly
   - User informed of success

7. **Failure Cases**: Test that AI handles errors gracefully and provides helpful error messages.

---

**End of Test Prompts Document**
