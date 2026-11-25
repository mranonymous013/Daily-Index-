const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- UNAUTHENTICATED DENIAL ---

test("Unauthenticated user: cannot read or write settings, habits, or expenses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      dailyBudget: 1000,
      currencySymbol: "৳",
      defaultsSeeded: true,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("habits").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("expenses").get()
  );
});

// --- USER SETTINGS TESTS ---

test("Authenticated owner: UserSettings create, partial budget update, and shadow/tamper rejections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 2000);

  // Valid Kotlin create payload
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      dailyBudget: 1000,
      currencySymbol: "৳",
      defaultsSeeded: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Valid partial budget update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyBudget: 1500,
      updatedAt: updated,
    })
  );

  // Reject negative budget
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyBudget: -50,
      updatedAt: updated,
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyBudget: 1200,
      createdAt: updated,
      updatedAt: updated,
    })
  );

  // Reject shadow/ghost field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyBudget: 1200,
      isAdmin: true,
      updatedAt: updated,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyBudget: 1200,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject listing /users root collection
  await assertFails(aliceDb.collection("users").get());
});

// --- HABITS TESTS ---

test("Authenticated owner: Habit CRUD, toggle completion, edit, and invalid payload rejections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 2000);
  const habitsCol = aliceDb.collection("users").doc(ALICE_UID).collection("habits");

  // Empty subcollection list query (matches Kotlin observeHabits)
  await assertSucceeds(habitsCol.get());

  // Valid Kotlin create payload (NO @DocumentId "id" field)
  await assertSucceeds(
    habitsCol.doc("habit_1").set({
      userId: ALICE_UID,
      name: "Drink 8 glasses of water",
      icon: "💧",
      completedDates: [],
      sortOrder: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid toggle completion partial update
  await assertSucceeds(
    habitsCol.doc("habit_1").update({
      completedDates: ["2026-11-24"],
      updatedAt: updated,
    })
  );

  // Valid edit habit name/icon partial update
  await assertSucceeds(
    habitsCol.doc("habit_1").update({
      name: "Drink 10 glasses of water",
      icon: "💧",
      updatedAt: updated,
    })
  );

  // Reject @DocumentId "id" key inside create payload
  await assertFails(
    habitsCol.doc("habit_bad_id").set({
      id: "habit_bad_id",
      userId: ALICE_UID,
      name: "Bad Habit",
      icon: "⭐",
      completedDates: [],
      sortOrder: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject oversized habit name (> 60 chars)
  await assertFails(
    habitsCol.doc("habit_long").set({
      userId: ALICE_UID,
      name: "x".repeat(61),
      icon: "⭐",
      completedDates: [],
      sortOrder: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject userId spoofing
  await assertFails(
    habitsCol.doc("habit_spoof").set({
      userId: BOB_UID,
      name: "Spoofed",
      icon: "⭐",
      completedDates: [],
      sortOrder: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid delete
  await assertSucceeds(habitsCol.doc("habit_1").delete());
});

// --- EXPENSES TESTS ---

test("Authenticated owner: Expense CRUD, query, and schema boundary rejections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 2000);
  const expensesCol = aliceDb.collection("users").doc(ALICE_UID).collection("expenses");

  // Empty subcollection list query (matches Kotlin observeExpenses)
  await assertSucceeds(expensesCol.get());

  // Valid Kotlin create payload
  await assertSucceeds(
    expensesCol.doc("exp_1").set({
      userId: ALICE_UID,
      date: "2026-11-24",
      title: "Lunch at cafe",
      amount: 320.5,
      category: "Food",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial edit
  await assertSucceeds(
    expensesCol.doc("exp_1").update({
      title: "Lunch with friends",
      amount: 450,
      category: "Food",
      date: "2026-11-24",
      updatedAt: updated,
    })
  );

  // Reject zero or negative amount
  await assertFails(
    expensesCol.doc("exp_zero").set({
      userId: ALICE_UID,
      date: "2026-11-24",
      title: "Free item",
      amount: 0,
      category: "Other",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid category
  await assertFails(
    expensesCol.doc("exp_bad_cat").set({
      userId: ALICE_UID,
      date: "2026-11-24",
      title: "Coffee",
      amount: 120,
      category: "InvalidCat",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid date format
  await assertFails(
    expensesCol.doc("exp_bad_date").set({
      userId: ALICE_UID,
      date: "24/11/2026",
      title: "Coffee",
      amount: 120,
      category: "Food",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid delete
  await assertSucceeds(expensesCol.doc("exp_1").delete());
});

// --- CROSS-USER ISOLATION ---

test("Cross-user isolation: Alice cannot read, list, update, or delete Bob's data", async () => {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      dailyBudget: 800,
      currencySymbol: "৳",
      defaultsSeeded: true,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(BOB_UID).collection("habits").doc("b_h1").set({
      userId: BOB_UID,
      name: "Bob's Walk",
      icon: "🏃",
      completedDates: ["2026-11-24"],
      sortOrder: 0,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(BOB_UID).collection("expenses").doc("b_e1").set({
      userId: BOB_UID,
      date: "2026-11-24",
      title: "Bob's Bus Fare",
      amount: 40,
      category: "Transport",
      createdAt: past,
      updatedAt: past,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      dailyBudget: 9999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("habits").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("habits").doc("b_h1").delete()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("expenses").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("expenses").doc("b_e1").delete()
  );
});
