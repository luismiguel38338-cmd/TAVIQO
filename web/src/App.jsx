import React, { useEffect, useState } from "react";
import { onAuthStateChanged } from "firebase/auth";
import { collection, onSnapshot, orderBy, query, where } from "firebase/firestore";
import { auth, db, signInGoogle, signOutWeb } from "./firebase";
import "./styles.css";

export default function App() {
  const [user, setUser] = useState(null);
  const [clips, setClips] = useState([]);
  const [streams, setStreams] = useState([]);

  useEffect(() => onAuthStateChanged(auth, setUser), []);

  useEffect(() => {
    if (!user) return;
    const clipsQ = query(
      collection(db, "clips"),
      where("status", "==", "active"),
      orderBy("createdAt", "desc")
    );
    const streamsQ = query(
      collection(db, "live_streams"),
      where("status", "==", "live")
    );
    const unsubClips = onSnapshot(clipsQ, (snap) => {
      setClips(snap.docs.map((d) => ({ id: d.id, ...d.data() })));
    });
    const unsubStreams = onSnapshot(streamsQ, (snap) => {
      setStreams(snap.docs.map((d) => ({ id: d.id, ...d.data() })));
    });
    return () => {
      unsubClips();
      unsubStreams();
    };
  }, [user]);

  return (
    <div className="lobo-shell">
      <header className="lobo-header">
        <div>
          <span className="lobo-brand">LOBO CLIPS AI</span>
          <span className="lobo-sub">Web Multiplataforma • Usuarios 100% Reales</span>
        </div>
        {user ? (
          <button className="lobo-btn-outline" onClick={signOutWeb}>
            Salir ({user.displayName || user.email})
          </button>
        ) : (
          <button className="lobo-btn-cyan" onClick={signInGoogle}>
            Iniciar sesión con Google
          </button>
        )}
      </header>

      {!user ? (
        <main className="lobo-hero">
          <h1>Red social de vídeos 9:16 con regalos y directos reales</h1>
          <p>
            Conectada al mismo Cloud Firestore de Android e iOS. Cero perfiles falsos, cero
            interacciones simuladas.
          </p>
        </main>
      ) : (
        <main className="lobo-grid">
          <section>
            <h2>Feed Vertical Activo ({clips.length})</h2>
            {clips.length === 0 ? (
              <p className="lobo-muted">Aún no hay clips publicados por usuarios reales.</p>
            ) : (
              clips.map((clip) => (
                <article key={clip.id} className="lobo-card">
                  <div className="lobo-author">@{clip.authorUsername}</div>
                  {clip.videoUrl?.startsWith("https://") && (
                    <video src={clip.videoUrl} controls playsInline className="lobo-video-9-16" />
                  )}
                  <p>{clip.caption}</p>
                  <div className="lobo-stats">
                    <span>❤️ {clip.likesCount || 0}</span>
                    <span>💬 {clip.commentsCount || 0}</span>
                    <span>👁️ {clip.viewsCount || 0}</span>
                    <span>🎁 {clip.giftsCoinsTotal || 0}</span>
                  </div>
                </article>
              ))
            )}
          </section>

          <aside>
            <h2>Directos en Vivo ({streams.length})</h2>
            {streams.length === 0 ? (
              <p className="lobo-muted">No hay transmisiones activas en este momento.</p>
            ) : (
              streams.map((s) => (
                <div key={s.id} className="lobo-card">
                  <strong>{s.title}</strong>
                  <p className="lobo-muted">@{s.hostUsername} • {s.viewerCount || 0} viendo</p>
                </div>
              ))
            )}
          </aside>
        </main>
      )}
    </div>
  );
}
