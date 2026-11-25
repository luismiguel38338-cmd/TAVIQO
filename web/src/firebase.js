import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "gen-lang-client-0174699991.firebaseapp.com",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "gen-lang-client-0174699991",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "",
};

const DATABASE_ID = "ai-studio-android-loboclip-f031fd05-ba55-4b71-8396-2e4f1629a0fa";

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app, DATABASE_ID);
export const googleProvider = new GoogleAuthProvider();

export const signInGoogle = () => signInWithPopup(auth, googleProvider);
export const signOutWeb = () => signOut(auth);
