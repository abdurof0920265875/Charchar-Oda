const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function makeProjectPayload(uid, overrides = {}) {
  return {
    userId: uid,
    title: "Oak Bookshelf",
    category: "furniture",
    location: "Living Room",
    status: "planning",
    budgetCents: 35000,
    notes: "White oak plywood with brass brackets",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

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

test("Unauthenticated user: cannot list or read projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/projects`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/projects/proj_1`).get());
});

test("Authenticated user: can create, read, update, and delete own project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projRef = aliceDb.doc(`users/${ALICE_UID}/projects/proj_1`);

  await assertSucceeds(projRef.set(makeProjectPayload(ALICE_UID)));
  await assertSucceeds(projRef.get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/projects`).get());

  await assertSucceeds(
    projRef.update({
      status: "in_progress",
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(projRef.delete());
});

test("Cross-user isolation: Bob cannot read, write, or delete Alice's project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_1`).set(makeProjectPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/projects`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/projects/proj_1`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/projects/proj_1`).update({
      status: "completed",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/projects/proj_1`).delete());
});

test("Shadow update & identity spoofing: rejects ghost fields and mismatched userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Ghost field on create
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_ghost`).set(
      makeProjectPayload(ALICE_UID, { isAdmin: true })
    )
  );

  // Mismatched userId
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_spoof`).set(
      makeProjectPayload(BOB_UID)
    )
  );

  // Invalid category enum
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_enum`).set(
      makeProjectPayload(ALICE_UID, { category: "invalid_cat" })
    )
  );
});

test("Parent Gate & subcollections: steps, materials, measurements require existing parent project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Orphan step without parent project must fail
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/projects/missing_proj/steps/step_1`).set({
      userId: ALICE_UID,
      projectId: "missing_proj",
      title: "Cut timber",
      phase: "build",
      completed: false,
      sortOrder: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Create parent project first
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_1`).set(makeProjectPayload(ALICE_UID))
  );

  // Valid step, material, and measurement succeed
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_1/steps/step_1`).set({
      userId: ALICE_UID,
      projectId: "proj_1",
      title: "Cut 2x4 framing",
      phase: "build",
      completed: false,
      sortOrder: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_1/materials/mat_1`).set({
      userId: ALICE_UID,
      projectId: "proj_1",
      name: "White Oak Board",
      quantity: 4,
      unit: "pcs",
      unitCostCents: 3250,
      purchased: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_1/measurements/meas_1`).set({
      userId: ALICE_UID,
      projectId: "proj_1",
      label: "Alcove Width",
      dimensionValue: "74.5 in",
      note: "Check baseboard clearance",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob cannot read Alice's subcollections
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/projects/proj_1/steps`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/projects/proj_1/materials`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/projects/proj_1/measurements`).get());
});
