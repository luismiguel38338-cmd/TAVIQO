const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { initializeApp } = require("firebase-admin/app");

initializeApp();

const DATABASE_ID = "ai-studio-android-loboclip-f031fd05-ba55-4b71-8396-2e4f1629a0fa";
const db = getFirestore(DATABASE_ID);

const COIN_PACKAGES = {
  pack_100: { coinsAmount: 100, priceUsdCents: 199 },
  pack_500: { coinsAmount: 500, priceUsdCents: 799 },
  pack_1200: { coinsAmount: 1200, priceUsdCents: 1699 },
  pack_3000: { coinsAmount: 3000, priceUsdCents: 3999 },
};

/**
 * Verifies a Google Play Billing / Stripe purchase receipt server-side before crediting coins.
 * Prevents duplicate token redemption and client-side balance tampering.
 */
exports.verifyAndCreditCoinPurchase = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Autenticación requerida.");
  }

  const uid = request.auth.uid;
  const { orderId, purchaseToken, provider } = request.data || {};
  if (!orderId || !purchaseToken || !provider) {
    throw new HttpsError("invalid-argument", "Faltan parámetros de verificación.");
  }

  // TODO: Replace provider receipt check with live Google Play Developer API / Stripe webhook verification
  // using server-only secrets stored in Firebase Secret Manager (defineSecret).
  throw new HttpsError(
    "failed-precondition",
    "Conecta las credenciales de Google Play Billing / Stripe en Secret Manager para acreditar compras reales."
  );
});

/**
 * Admin-only approval or rejection of creator payout requests with atomic ledger update.
 */
exports.processCreatorPayout = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Autenticación requerida.");
  }

  const adminDoc = await db.collection("admins").doc(request.auth.uid).get();
  const isBootstrapAdmin =
    request.auth.token.email === "luismiguel30583@gmail.com" &&
    request.auth.token.email_verified === true;

  if (!adminDoc.exists && !isBootstrapAdmin) {
    throw new HttpsError("permission-denied", "Solo administradores autorizados.");
  }

  const { payoutId, action } = request.data || {};
  if (!payoutId || !["paid", "rejected"].includes(action)) {
    throw new HttpsError("invalid-argument", "Acción de retiro inválida.");
  }

  const payoutRef = db.collection("payout_requests").doc(payoutId);

  await db.runTransaction(async (tx) => {
    const payoutSnap = await tx.get(payoutRef);
    if (!payoutSnap.exists) {
      throw new HttpsError("not-found", "Solicitud de retiro no encontrada.");
    }
    const payout = payoutSnap.data();
    if (payout.status !== "pending_review") {
      throw new HttpsError("failed-precondition", "La solicitud ya fue procesada.");
    }

    const walletRef = db
      .collection("users")
      .doc(payout.creatorId)
      .collection("private")
      .doc("wallet");

    if (action === "rejected") {
      tx.update(walletRef, {
        creatorEarningsCoins: FieldValue.increment(payout.coinsAmount),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }

    tx.update(payoutRef, {
      status: action,
      processedBy: request.auth.uid,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });

  return { ok: true, payoutId, status: action };
});
