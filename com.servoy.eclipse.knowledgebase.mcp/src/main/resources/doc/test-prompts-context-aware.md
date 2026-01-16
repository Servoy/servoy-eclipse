# Test Prompts - Context-Aware Multi-Module Development

**Purpose**: Test prompts for validating context-aware write operations across MainSolution and modules (Module_A, Module_B, Module_C).

**Audience**: Written from the perspective of a developer working with modular Servoy solutions.

**Date Created**: December 30, 2025

---

## SETUP INSTRUCTIONS

Before running these prompts, ensure you have:
1. **MainSolution** - Active solution project open
2. **Module_A** - Added as a module to MainSolution
3. **Module_B** - Added as a module to MainSolution
4. **Module_C** - Added as a module to MainSolution
5. A database server named **example_data** configured with standard schema

---

## SECTION 1: CONTEXT MANAGEMENT BASICS (Tests 1-5)

### Test 1.1: Check Initial Context
```
What context am I currently in?
```

**Expected behavior**: AI should call `getContext()` and show current context is "active" (MainSolution), plus list all available contexts: active, Module_A, Module_B, Module_C.

---

### Test 1.2: Switch to Module A
```
Switch to Module_A
```

**Expected behavior**: AI should call `setContext({context: "Module_A"})` and confirm the switch.

---

### Test 1.3: Verify Context Switch
```
What context am I in now?
```

**Expected behavior**: AI should call `getContext()` and show current context is "Module_A" with [CURRENT] marker.

---

### Test 1.4: Switch Back to Active Solution
```
Go back to the main solution
```

**Expected behavior**: AI should call `setContext({context: "active"})` and confirm switch to MainSolution.

---

### Test 1.5: Invalid Context Error
```
Switch to Module_X
```

**Expected behavior**: AI should call `setContext({context: "Module_X"})` and get error: "Context 'Module_X' not found. Available: active, Module_A, Module_B, Module_C"

---

## SECTION 2: RELATIONS IN DIFFERENT CONTEXTS (Tests 6-12)

### Test 2.1: Create Relation in Active Solution
```
Create a relation called "customers_to_orders" from example_data/customers to 
example_data/orders on customerid.
```

**Expected behavior**: AI should create relation in MainSolution (active context). Response should say "Created relation 'customers_to_orders' in MainSolution (active solution)".

---

### Test 2.2: Create Relation in Module A
```
Switch to Module_A and create a relation "products_to_categories" linking 
example_data/products to example_data/categories on categoryid.
```

**Expected behavior**: AI should switch context, then create relation. Response should say "Created relation 'products_to_categories' in Module_A".

---

### Test 2.3: List Relations Shows Origin
```
Show me all relations
```

**Expected behavior**: AI should call `getRelations()` and show:
- customers_to_orders (in: MainSolution)
- products_to_categories (in: Module_A)

---

### Test 2.4: Multiple Relations in Same Module
```
I'm working in Module_B. Create these relations:
1. orders_to_order_details (orders to order_details on orderid)
2. suppliers_to_products (suppliers to products on supplierid)
```

**Expected behavior**: AI should switch to Module_B context, create both relations there. Both should show "in Module_B".

---

### Test 2.5: Create Relation in Module C with Properties
```
In Module_C, create relation "employees_supervisor" from example_data/employees 
to example_data/employees on reportsto (to employeeid). Make it inner join.
```

**Expected behavior**: AI should switch to Module_C, create self-referencing relation with inner join. Response: "Created relation 'employees_supervisor' in Module_C".

---

### Test 2.6: Context Persists Across Operations
```
Create another relation called "orders_to_customers" from orders to 
customers on customerid.
```

**Expected behavior**: AI should use existing Module_C context (no switch needed). Response: "Created relation 'orders_to_customers' in Module_C".

---

### Test 2.7: Delete Relation from Specific Module
```
Delete the products_to_categories relation
```

**Expected behavior**: AI should delete the relation that was created in Module_A. Should work regardless of current context.

---

## SECTION 3: FORMS IN DIFFERENT CONTEXTS (Tests 13-19)

### Test 3.1: Create Form in Active Solution
```
Go to active solution and create a form called "customersForm"
```

**Expected behavior**: AI should switch to "active" context, create form. Response: "Form 'customersForm' created in MainSolution (active solution)".

---

### Test 3.2: Create Form in Module A
```
Create a form "productsForm" in Module_A.
```

**Expected behavior**: AI should switch context, create form. Response: "Form 'productsForm' created in Module_A".

---

### Test 3.3: List Forms Shows Origin
```
List all forms in the solution
```

**Expected behavior**: AI should call `listForms()` and show:
- customersForm (in: MainSolution)
- productsForm (in: Module_A)

---

### Test 3.4: Create Multiple Forms in Module B
```
I need to work in Module_B. Create two forms:
1. ordersForm
2. orderDetailsForm
```

**Expected behavior**: AI should switch to Module_B, create both forms. Both should show "in Module_B".

---

### Test 3.5: Create Form in Module C with DataSource
```
In Module_C, create a form called "employeesForm" based on example_data/employees table
```

**Expected behavior**: AI should switch to Module_C, create form with dataSource. Response: "Form 'employeesForm' created in Module_C".

---

### Test 3.6: Get Current Form (Context-Aware)
```
Open productsForm and tell me about it
```

**Expected behavior**: AI should open the form from Module_A (where it was created) and show its properties.

---

### Test 3.7: Set Main Form (Active Solution Only)
```
Switch to active and set customersForm as the main form
```

**Expected behavior**: AI should switch to "active" context, then set customersForm as solution's main form.

---

## SECTION 4: VALUELISTS IN DIFFERENT CONTEXTS (Tests 20-25)

### Test 4.1: Create ValueList in Active Solution
```
In the main solution, create a custom valuelist "status_vl" with values: Active, Inactive, Pending
```

**Expected behavior**: AI should switch to "active", create valuelist. Response: "ValueList 'status_vl' created in MainSolution (active solution) (CUSTOM with 3 values)".

---

### Test 4.2: Create Database ValueList in Module A
```
Switch to Module_A and create a valuelist "categories_vl" from example_data/categories 
showing categoryname for both display and storage
```

**Expected behavior**: AI should switch context, create DATABASE valuelist. Response: "ValueList 'categories_vl' created in Module_A (DATABASE)".

---

### Test 4.3: List ValueLists Shows Origin
```
Show me all valuelists
```

**Expected behavior**: AI should call `getValueLists()` and show:
- status_vl (in: MainSolution)
- categories_vl (in: Module_A)

---

### Test 4.4: Create ValueList in Module B
```
In Module_B, create a valuelist "customers_vl" from example_data/customers. 
Show companyname but store customerid.
```

**Expected behavior**: AI should switch to Module_B, create valuelist. Response: "ValueList 'customers_vl' created in Module_B (DATABASE)".

---

### Test 4.5: Create Custom ValueList in Module C
```
Switch to Module_C and create a valuelist "priority_vl" with values: Low, Medium, High, Critical
```

**Expected behavior**: AI should switch context, create CUSTOM valuelist. Response: "ValueList 'priority_vl' created in Module_C (CUSTOM with 4 values)".

---

### Test 4.6: Delete ValueList from Specific Module
```
Delete the categories_vl valuelist
```

**Expected behavior**: AI should delete the valuelist from Module_A. Should work regardless of current context.

---

## SECTION 5: COMPONENTS IN DIFFERENT CONTEXTS (Tests 26-35)

### Test 5.1: Add Button to Form in Active Solution
```
Go to active solution and add a button to customersForm. Name it "btnSave" with text "Save Customer"
```

**Expected behavior**: AI should switch to "active", add button to customersForm (which is in MainSolution). Button should be added successfully.

---

### Test 5.2: Add Button to Form in Module A
```
Switch to Module_A and add a button to productsForm called "btnNew" with text "New Product"
```

**Expected behavior**: AI should switch to Module_A context, add button to productsForm file in Module_A project.

---

### Test 5.3: List Buttons from Module Form
```
Show me all buttons in productsForm
```

**Expected behavior**: AI should list buttons including btnNew (form is in Module_A).

---

### Test 5.4: Add Label to Form in Module B
```
In Module_B, add a label to ordersForm named "lblTitle" with text "Order Management"
```

**Expected behavior**: AI should switch to Module_B, add label to ordersForm file.

---

### Test 5.5: Add Multiple Components in Same Module
```
I'm working in Module_C. Add these to employeesForm:
1. A button "btnEdit" with text "Edit Employee"
2. A label "lblStatus" with text "Status:"
```

**Expected behavior**: AI should switch to Module_C, add both components to employeesForm. Context persists for both operations.

---

### Test 5.6: Update Button in Module Form
```
Update the btnNew button in productsForm to have text "Create New Product"
```

**Expected behavior**: AI should update button in Module_A's productsForm (where it exists).

---

### Test 5.7: Delete Component from Module Form
```
Delete the lblTitle label from ordersForm
```

**Expected behavior**: AI should delete label from Module_B's ordersForm.

---

### Test 5.8: Add Button to Wrong Context Error
```
Switch to active solution. Now add a button "btnDelete" to productsForm.
```

**Expected behavior**: This should work - context determines project path lookup, and productsForm should be found in Module_A.

---

### Test 5.9: List Labels Shows All Modules
```
Switch to Module_C and show me all labels in employeesForm
```

**Expected behavior**: AI should switch context, list labels including lblStatus from employeesForm.

---

### Test 5.10: Component in Non-Existent Form
```
Add a button to nonExistentForm
```

**Expected behavior**: AI should get error that form doesn't exist.

---

## SECTION 6: STYLES IN DIFFERENT CONTEXTS (Tests 36-40)

### Test 6.1: Create Style in Active Solution
```
In the main solution, create a CSS class "header-title" with font-size: 24px and color: #333
```

**Expected behavior**: AI should switch to "active", create style in MainSolution. Response should show "in MainSolution (active solution)".

---

### Test 6.2: Create Style in Module A
```
Switch to Module_A and create a style "product-card" with background: #f5f5f5 and padding: 10px
```

**Expected behavior**: AI should switch to Module_A, create style in Module_A's LESS files.

---

### Test 6.3: List Styles Shows Origin
```
Show me all CSS classes
```

**Expected behavior**: AI should list styles and indicate which project they're in (though styles might not show explicit origin like other tools).

---

### Test 6.4: Create Style in Module B
```
In Module_B, create a style "order-status" with color: #28a745 and font-weight: bold
```

**Expected behavior**: AI should switch to Module_B, create style.

---

### Test 6.5: Get Style from Specific Module
```
Show me the product-card style
```

**Expected behavior**: AI should retrieve style from Module_A where it was created.

---

## SECTION 7: CROSS-MODULE WORKFLOWS (Tests 41-45)

### Test 7.1: Work Across Multiple Modules in Sequence
```
I need to set up a product catalog system:
1. Switch to Module_A and create form "catalogForm"
2. Add a button "btnFilter" with text "Filter Products"
3. Switch to Module_B and create a relation "categories_to_products" from categories to products on categoryid
4. Go back to Module_A and add a label "lblCount" to catalogForm
```

**Expected behavior**: AI should perform all operations in sequence, switching contexts as needed. Each should report correct module.

---

### Test 7.2: Create Related Items in Different Modules
```
I'm building an order system:
- In Module_A: Create form "orderEntryForm"
- In Module_B: Create relation "customers_to_orders_active" (customers to orders on customerid)
- In Module_C: Create valuelist "payment_methods_vl" with values: Cash, Credit Card, PayPal
- In active solution: Create style "order-header" with font-size: 18px
```

**Expected behavior**: AI should switch contexts for each operation and create items in correct modules.

---

### Test 7.3: Verify All Created Items
```
Show me:
1. All forms
2. All relations
3. All valuelists
Each with their origin/location
```

**Expected behavior**: AI should list all created items with their origin indicators showing correct modules.

---

### Test 7.4: Update Items in Their Original Modules
```
Update the catalogForm by adding a button "btnRefresh" with text "Refresh List"
```

**Expected behavior**: AI should add button to catalogForm in Module_A (where it was created), regardless of current context.

---

### Test 7.5: Clean Up Specific Module
```
Switch to Module_B and delete:
1. The customers_to_orders_active relation
2. The order-status style
```

**Expected behavior**: AI should switch to Module_B context and delete both items from that module.

---

## SECTION 8: CONTEXT RESET ON SOLUTION ACTIVATION (Tests 46-48)

### Test 8.1: Set Context Then Check After Simulated Reload
```
Switch to Module_C and verify the context
```

**Expected behavior**: AI should switch to Module_C and confirm current context.

---

### Test 8.2: Simulate Solution Activation (Manual Test)
```
[MANUAL STEP: User activates a different solution in Eclipse, then re-activates MainSolution]

What context am I in now?
```

**Expected behavior**: After solution re-activation, context should have auto-reset to "active" (MainSolution).

---

### Test 8.3: Verify Context Reset Works
```
Check my current context
```

**Expected behavior**: Should show "active" (not Module_C from before solution switch).

---

## SECTION 9: ERROR HANDLING AND EDGE CASES (Tests 49-50)

### Test 9.1: Create in Non-Existent Module Context
```
Switch to Module_D
```

**Expected behavior**: Should get error: "Context 'Module_D' not found. Available: active, Module_A, Module_B, Module_C"

---

### Test 9.2: Complex Multi-Step with Context Persistence
```
I'm going to do several operations in Module_A:
1. Create form "testForm1"
2. Create relation "test_rel1" (customers to orders on customerid)
3. Create valuelist "test_vl1" with values: One, Two, Three
4. Add button "btnTest" to testForm1

Then switch to active and create form "mainTestForm"
```

**Expected behavior**: AI should:
- Switch to Module_A once at the start
- Create all 4 items in Module_A (context persists)
- Switch to "active" for last form
- Report correct module for each operation

---

## TESTING SUMMARY

**Total Test Prompts**: 50

**Coverage by Category**:
- Context Management: 5 tests (10%)
- Relations: 7 tests (14%)
- Forms: 7 tests (14%)
- ValueLists: 6 tests (12%)
- Components (Buttons/Labels): 10 tests (20%)
- Styles: 5 tests (10%)
- Cross-Module Workflows: 5 tests (10%)
- Context Reset: 3 tests (6%)
- Error Handling: 2 tests (4%)

**Key Scenarios Tested**:
- Basic context switching and verification
- Creating items in active solution vs modules
- Context persistence across multiple operations
- Origin display in list operations
- Cross-module workflows
- Component additions to module forms
- Style creation in different modules
- Context auto-reset on solution activation
- Error handling for invalid contexts
- Complex multi-step operations

**Expected Outcomes**:
- All write operations respect current context
- List operations show item origins
- Context persists until explicitly changed or solution activated
- Clear error messages for invalid contexts
- Components can be added to forms in any module
- Styles created in correct module LESS files

---

## NOTES FOR TESTERS

1. **Run tests in order** - Some tests depend on items created in earlier tests
2. **Check console output** - Verify operation messages show correct module names
3. **Verify file locations** - Check that files are created in correct project folders
4. **Test context persistence** - Multiple operations should not require repeated context switches
5. **Manual solution activation test** - Test 8.2 requires manually switching solutions in Eclipse

**Success Criteria**:
- All 50 prompts execute without errors
- Items created in correct modules
- Context switches work reliably
- Origin information displayed correctly
- Context persists appropriately
- Auto-reset works on solution activation
