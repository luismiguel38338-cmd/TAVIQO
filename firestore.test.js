const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const ADMIN_UID = "admin_uid_999";
const ADMIN_EMAIL = "luismiguel30583@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("clips").where("status", "==", "active").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("live_streams").where("status", "==", "live").get());
  await assertFails(unauth.collection("conversations").get());
});

test("User profile creation, PII wallet isolation, and privilege escalation prevention", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = pastTime();

  // Valid user profile creation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_taviqo",
      displayName: "Alice Taviqo",
      bio: "Creadora real",
      verified: false,
      suspended: false,
      followersCount: 0,
      followingCount: 0,
      clipsCount: 0,
      likesCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject self-verification on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_taviqo",
      displayName: "Bob",
      verified: true,
      suspended: false,
      followersCount: 0,
      followingCount: 0,
      clipsCount: 0,
      likesCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid private wallet creation with 0 coins & user role
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("wallet").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      role: "user",
      coinBalance: 0,
      creatorEarningsCoins: 0,
      totalSpentCoins: 0,
      blockedUserIds: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's private wallet (PII Isolation)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("wallet").get());

  // Alice cannot self-credit coins or escalate role to admin
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("wallet").update({
      coinBalance: 99999,
      updatedAt: pastTime(500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("wallet").update({
      role: "admin",
      updatedAt: pastTime(500),
    })
  );
});

test("Clips publish, filtered query alignment, engagement counters, and rejection of extra @DocumentId field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastTime();

  // Reject creating a clip with extra "id" field
  await assertFails(
    aliceDb.collection("clips").doc("clip_bad").set({
      id: "clip_bad",
      authorId: ALICE_UID,
      authorUsername: "alice_taviqo",
      authorVerified: false,
      caption: "Invalid id field",
      hashtags: ["#taviqo"],
      videoUrl: "https://example.com/v.mp4",
      durationMs: 12000,
      likesCount: 0,
      commentsCount: 0,
      viewsCount: 0,
      sharesCount: 0,
      giftsCoinsTotal: 0,
      status: "active",
      aiModerationStatus: "approved",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid clip create
  await assertSucceeds(
    aliceDb.collection("clips").doc("clip_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice_taviqo",
      authorVerified: false,
      caption: "Primer clip real en TAVIQO",
      hashtags: ["#taviqo", "#musica"],
      videoUrl: "https://storage.googleapis.com/demo/clip1.mp4",
      durationMs: 15000,
      likesCount: 0,
      commentsCount: 0,
      viewsCount: 0,
      sharesCount: 0,
      giftsCoinsTotal: 0,
      status: "active",
      aiModerationStatus: "approved",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered feed query succeeds; unfiltered query fails
  await assertSucceeds(bobDb.collection("clips").where("status", "==", "active").get());
  await assertFails(bobDb.collection("clips").get());

  // Bob can increment likesCount by +1, cannot hijack caption
  await assertSucceeds(
    bobDb.collection("clips").doc("clip_1").update({
      likesCount: 1,
      updatedAt: pastTime(500),
    })
  );
  await assertFails(
    bobDb.collection("clips").doc("clip_1").update({
      caption: "Hijacked",
      updatedAt: pastTime(500),
    })
  );
});

test("Atomic virtual gift transaction enforces coin balance, prevents negative balance & duplicate tx", async () => {
  const now = pastTime(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).collection("private").doc("wallet").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      role: "user",
      coinBalance: 500,
      creatorEarningsCoins: 0,
      totalSpentCoins: 0,
      blockedUserIds: [],
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(BOB_UID).collection("private").doc("wallet").set({
      userId: BOB_UID,
      email: "bob@example.com",
      role: "user",
      coinBalance: 0,
      creatorEarningsCoins: 0,
      totalSpentCoins: 0,
      blockedUserIds: [],
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txTime = pastTime(500);
  const txId = "tx_gift_1001";

  // Batch write mirroring Kotlin sendGift transaction (100 coins: 70 creator + 30 platform)
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("gift_transactions").doc(txId), {
    senderId: ALICE_UID,
    senderUsername: "alice_taviqo",
    recipientId: BOB_UID,
    recipientUsername: "bob_taviqo",
    giftId: "rocket",
    giftName: "Cohete",
    coinCost: 100,
    creatorNetCoins: 70,
    platformFeeCoins: 30,
    contextType: "clip",
    contextId: "clip_1",
    createdAt: txTime,
  });
  batch.update(aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("wallet"), {
    coinBalance: 400,
    totalSpentCoins: 100,
    lastTxId: txId,
    updatedAt: txTime,
  });
  batch.update(aliceDb.collection("users").doc(BOB_UID).collection("private").doc("wallet"), {
    creatorEarningsCoins: 70,
    lastTxId: txId,
    updatedAt: txTime,
  });
  await assertSucceeds(batch.commit());

  // Duplicate txId must fail
  await assertFails(
    aliceDb.collection("gift_transactions").doc(txId).set({
      senderId: ALICE_UID,
      senderUsername: "alice_taviqo",
      recipientId: BOB_UID,
      recipientUsername: "bob_taviqo",
      giftId: "rocket",
      giftName: "Cohete",
      coinCost: 100,
      creatorNetCoins: 70,
      platformFeeCoins: 30,
      contextType: "clip",
      contextId: "clip_1",
      createdAt: pastTime(200),
    })
  );

  // Overspending into negative balance must fail
  const badBatch = aliceDb.batch();
  badBatch.set(aliceDb.collection("gift_transactions").doc("tx_gift_overdraft"), {
    senderId: ALICE_UID,
    senderUsername: "alice_taviqo",
    recipientId: BOB_UID,
    recipientUsername: "bob_taviqo",
    giftId: "crown",
    giftName: "Corona Imperial",
    coinCost: 1000,
    creatorNetCoins: 700,
    platformFeeCoins: 300,
    contextType: "clip",
    contextId: "clip_1",
    createdAt: pastTime(100),
  });
  badBatch.update(aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("wallet"), {
    coinBalance: -600,
    totalSpentCoins: 1100,
    lastTxId: "tx_gift_overdraft",
    updatedAt: pastTime(100),
  });
  await assertFails(badBatch.commit());
});

test("Private conversations isolate participants; Admin panel honors verified admin email", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_303").firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const now = pastTime();

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_ab").set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "alice_taviqo", [BOB_UID]: "bob_taviqo" },
      lastMessage: "Hola!",
      lastSenderId: ALICE_UID,
      unreadUserIds: [BOB_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can query his conversations; Charlie cannot read Alice-Bob conversation
  await assertSucceeds(bobDb.collection("conversations").where("participantIds", "array-contains", BOB_UID).get());
  await assertFails(charlieDb.collection("conversations").doc("conv_ab").get());

  // Spoofed unverified admin email cannot query reports; verified admin can
  await assertFails(spoofedAdminDb.collection("reports").get());
  await assertSucceeds(realAdminDb.collection("reports").get());
});
